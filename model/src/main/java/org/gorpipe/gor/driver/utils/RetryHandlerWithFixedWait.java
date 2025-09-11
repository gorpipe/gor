package org.gorpipe.gor.driver.utils;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorRetryException;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public abstract class RetryHandlerWithFixedWait extends RetryHandlerBase {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final long initialDuration;
    protected final long totalDuration;

    public RetryHandlerWithFixedWait(long initialDuration, long totalDuration) {
        this.initialDuration = initialDuration;
        this.totalDuration = totalDuration;
    }

    protected Random rand = new Random();

    public <T> T perform(Action<T> action, ActionVoid preRetryOp) {
        assert initialDuration <= totalDuration;

        int tries = 0;
        long accumulatedDuration = 0;

        Throwable lastException = null;
        while (accumulatedDuration <= totalDuration) {
            try {
                return action.perform();
            } catch (GorRetryException e) {
                if (!e.isRetry()) throw e;
                checkIfShouldRetryException(e);

                tries++;
                lastException = e;
                accumulatedDuration += sleep(e, tries, initialDuration);

                log.warn("Retrying gor action (return 1) after " + accumulatedDuration + "ms, retry " + tries, e);

                if (preRetryOp != null) {
                    preRetryOp.perform();
                }
            } catch (Exception e) {
                log.warn("Non-retryable exception caught, will not retry.", e);
                throw e;
            }
        }

        throw new GorSystemException(
            String.format("Giving up after %s milliseconds and %d retries", accumulatedDuration, tries),
            lastException
        );
    }

    public void perform(ActionVoid action, ActionVoid preRetryOp) {
        assert initialDuration <= totalDuration;

        int tries = 0;
        long accumulatedDuration = 0;

        Throwable lastException = null;
        while (accumulatedDuration <= totalDuration) {
            try {
                action.perform();
                return;
            } catch (GorRetryException e) {
                if (!e.isRetry()) throw e;
                checkIfShouldRetryException(e);

                tries++;
                lastException = e;
                accumulatedDuration += sleep(e, tries, initialDuration);

                log.warn("Retrying gor action (non return) after " + accumulatedDuration + "ms, retry " + tries, e);

                if (preRetryOp != null) {
                    preRetryOp.perform();
                }
            } catch (Exception e) {
                log.warn("Non-retryable exception caught, will not retry.", e);
                throw e;
            }
        }

        throw new GorSystemException(
                String.format("Giving up after %s milliseconds and %d retries", accumulatedDuration, tries),
                lastException
        );
    }

    /**
     * Check if the exception is a retryable exception, and if not, throw a new GorException.
     *
     * @param e the exception to check
     * @throws GorException if the exception is not retryable.
     */
    protected abstract void checkIfShouldRetryException(GorException e) throws GorException;

    private long sleep(GorException e, int tries, long initialDuration) {
        var sleepMs = calculateDuration(tries, initialDuration);
        log.warn("Try number " + tries + " failed. Waiting for " + sleepMs + "ms before retrying.", e);
        threadSleep(sleepMs, tries, e);
        return sleepMs;
    }

    protected long calculateDuration(int tries, long initialDuration) {
        // we allow randomness of the initial delay of up to 10%
        return (long)((initialDuration * (0.9 + 0.1 * rand.nextDouble())) * Math.pow(tries, 2));
    }
}
