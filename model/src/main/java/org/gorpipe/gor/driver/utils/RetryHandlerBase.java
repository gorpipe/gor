package org.gorpipe.gor.driver.utils;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorSystemException;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;

public abstract class RetryHandlerBase {

    public abstract <T> T perform(Action<T> action);

    public abstract void perform(ActionVoid action);

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

