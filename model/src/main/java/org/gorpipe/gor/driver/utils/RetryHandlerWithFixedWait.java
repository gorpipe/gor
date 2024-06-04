package org.gorpipe.gor.driver.utils;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorRetryException;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static java.lang.Thread.sleep;

public abstract class RetryHandlerWithFixedWait extends RetryHandlerBase {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected long initialDuration;
    protected long totalDuration;

    public RetryHandlerWithFixedWait(long initialDuration, long totalDuration) {
        this.initialDuration = initialDuration;
        this.totalDuration = totalDuration;
    }

    protected Random rand = new Random();

    public <T> T perform(Action<T> action) {
        assert initialDuration <= totalDuration;

        int tries = 0;
        long accumulatedDuration = 0;

        Throwable lastException = null;
        while (accumulatedDuration <= totalDuration) {
            try {
                return action.perform();
            } catch (GorRetryException e) {
                if (!e.getRetry()) throw e;
                tries++;
                lastException = e;
                accumulatedDuration += handleThrowable(e, tries, initialDuration);
            }
        }

        throw new GorSystemException(
            String.format("Giving up after %s milliseconds and %d retries", accumulatedDuration, tries),
            lastException
        );
    }

    public void perform(ActionVoid action) {
        assert initialDuration <= totalDuration;

        int tries = 0;
        long accumulatedDuration = 0;

        Throwable lastException = null;
        while (accumulatedDuration <= totalDuration) {
            try {
                action.perform();
                return;
            } catch (GorRetryException e) {
                if (!e.getRetry()) throw e;
                tries++;
                lastException = e;
                accumulatedDuration += handleThrowable(e, tries, initialDuration);
            }
        }

        throw new GorSystemException(
                String.format("Giving up after %s milliseconds and %d retries", accumulatedDuration, tries),
                lastException
        );
    }

    protected abstract void onHandleError(GorException e, long delay, int tries);

    private long handleThrowable(GorException e, int tries, long initialDuration) {
        var delay = calculateDuration(tries, initialDuration);

        onHandleError(e, delay, tries);

        log.warn("Try number " + tries + " failed. Waiting for " + delay + "ms before retrying.", e);

        try {
            sleep(delay);
        } catch (InterruptedException e1) {
            // If interrupted waiting to retry, throw original exception
            Thread.currentThread().interrupt();
            throw new GorSystemException("Retry thread interrupted after " + tries + " retries", e);
        }

        return delay;
    }

    protected long calculateDuration(int tries, long initialDuration) {
        // we allow randomness of the initial delay of up to 10%
        return (long)((initialDuration * (0.9 + 0.1 * rand.nextDouble())) * Math.pow(tries, 2));
    }
}
