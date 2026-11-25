package org.gorpipe.gor.driver.utils;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorSystemException;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;

public abstract class RetryHandlerBase {

    public <T> T perform(Action<T> action) {
        return perform(action, null);
    }

    public  void perform(ActionVoid action) {
        perform(action, null);
    }

    public abstract <T> T perform(Action<T> action, ActionVoid preRetryOp);

    public abstract void perform(ActionVoid action, ActionVoid preRetryOp);

    public static final String UNKNOWN_OPERATION = "unknown";

    public <T> T perform(String operation, Action<T> action) {
        return perform(operation, action, null);
    }

    public void perform(String operation, ActionVoid action) {
        perform(operation, action, null);
    }

    /**
     * Like {@link #perform(Action, ActionVoid)}, labelled with the operation being retried (for logs
     * and metrics). Handlers that do not report operations fall back to the unlabelled variant.
     */
    public <T> T perform(String operation, Action<T> action, ActionVoid preRetryOp) {
        return perform(action, preRetryOp);
    }

    public void perform(String operation, ActionVoid action, ActionVoid preRetryOp) {
        perform(action, preRetryOp);
    }

    public interface Action<T> {
        T perform();
    }

    public interface ActionVoid {
        void perform();
    }

    protected void threadSleep(long sleepMs, int tries, Throwable orginalException) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            // If interrupted waiting to retry, throw original exception
            Thread.currentThread().interrupt();
            throw new GorSystemException("Retry thread interrupted after " + tries + " retries", e);
        }
    }

}

