package org.gorpipe.gor.driver.utils;

import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

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

