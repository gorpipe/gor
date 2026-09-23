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

    @Override
    public <T> T perform(Action<T> action, ActionVoid preRetryOp) {
        return perform(UNKNOWN_OPERATION, action, preRetryOp);
    }

    @Override
    public void perform(ActionVoid action, ActionVoid preRetryOp) {
        perform(UNKNOWN_OPERATION, action, preRetryOp);
    }

    @Override
    public void perform(String operation, ActionVoid action, ActionVoid preRetryOp) {
        perform(operation, () -> {
            action.perform();
            return null;
        }, preRetryOp);
    }

    @Override
    public <T> T perform(String operation, Action<T> action, ActionVoid preRetryOp) {
        assert initialDuration <= totalDuration;

        int tries = 0;
        long accumulatedDuration = 0;

        while (true) {
            try {
                return action.perform();
            } catch (GorRetryException e) {
                if (!e.isRetry()) throw e;
                checkIfShouldRetryException(e);

                tries++;

                // Give up once the attempt cap is reached, rather than sleeping for an attempt that
                // will never be made (ENGKNOW-3723).
                if (tries >= maxAttempts()) {
                    onGiveUp(operation, e, tries, accumulatedDuration);
                    throw new GorSystemException(
                            String.format("Giving up after %s milliseconds and %d retries", accumulatedDuration, tries - 1),
                            e);
                }

                long sleepMs = Math.max(calculateDuration(tries, initialDuration), retryAfterMillis(e));

                // Give up before a sleep that would exceed the budget, rather than sleeping and then
                // giving up without another attempt.
                if (accumulatedDuration + sleepMs > totalDuration) {
                    onGiveUp(operation, e, tries, accumulatedDuration);
                    throw new GorSystemException(
                            String.format("Giving up after %s milliseconds and %d retries", accumulatedDuration, tries - 1),
                            e);
                }

                onRetry(operation, e, tries, sleepMs);
                threadSleep(sleepMs, tries, e);
                accumulatedDuration += sleepMs;

                if (preRetryOp != null) {
                    preRetryOp.perform();
                }
            } catch (Exception e) {
                log.warn("Non-retryable exception caught, will not retry.", e);
                throw e;
            }
        }
    }

    /** Wait the server asked for (e.g. an HTTP Retry-After header), in ms; 0 when none. */
    protected long retryAfterMillis(GorException e) {
        return 0;
    }

    /** Called before sleeping for a retry. One line, no stack trace: retries are frequent and recover. */
    protected void onRetry(String operation, GorException e, int attempt, long sleepMs) {
        log.warn("Retry attempt={} op={} sleepMs={}: {}", attempt, operation, sleepMs, e.getMessage());
    }

    /** Called once when the retry budget is exhausted. Logs the full exception. */
    protected void onGiveUp(String operation, GorException e, int attempts, long totalSleepMs) {
        log.warn("Giving up op={} after {} attempts and {}ms of retry sleep", operation, attempts, totalSleepMs, e);
    }

    /**
     * Check if the exception is a retryable exception, and if not, throw a new GorException.
     *
     * @param e the exception to check
     * @throws GorException if the exception is not retryable.
     */
    protected abstract void checkIfShouldRetryException(GorException e) throws GorException;

    protected long calculateDuration(int tries, long initialDuration) {
        // we allow randomness of the initial delay of up to 10%
        return (long)((initialDuration * (0.9 + 0.1 * rand.nextDouble())) * Math.pow(tries, 2));
    }

    /**
     * Maximum number of attempts (first try + retries) before giving up, regardless of the sleep
     * budget. Unbounded by default; only handlers that opt in (e.g. {@code S3RetryHandler}) cap this.
     */
    protected int maxAttempts() {
        return Integer.MAX_VALUE;
    }
}
