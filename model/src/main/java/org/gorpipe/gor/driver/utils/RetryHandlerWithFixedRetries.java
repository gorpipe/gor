/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor.driver.utils;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorRetryException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Utility class that implements retry logic around arbitrary operations.
 * <p>
 * Created by villi on 17/09/15.
 * <p>
 * TODO: Change to interface with implementations for different strategies (exponential backoff vs ...) ?
 */
public abstract class RetryHandlerWithFixedRetries extends RetryHandlerBase {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected Random rand = new Random();
    private final long retryInitialSleepMs;
    private final long retryMaxSleepMs;
    private final int retryExpBackoff;
    private final int retries;

    public RetryHandlerWithFixedRetries(long retryInitialSleepMs, long retryMaxSleepMs, int retryExpBackoff, int retries) {
        this.retryInitialSleepMs = retryInitialSleepMs;
        this.retryMaxSleepMs = retryMaxSleepMs;
        this.retryExpBackoff = retryExpBackoff;
        this.retries = retries;
    }

    @Override
    public <T> T perform(Action<T> action) {
        int tries = 0;
        long sleepMs = 0;
        Throwable lastException = null;
        while (tries <= retries) {
            try {
                return action.perform();
            } catch (GorRetryException e) {
                if (!e.getRetry()) throw e;
                tries++;
                lastException = e;
                sleepMs = retryExceptionHandler(retries, tries, sleepMs, e);
            }
        }
        throw new GorSystemException("Giving up after " + tries + " retries", lastException);
    }

    @Override
    public void perform(ActionVoid action) {
        int tries = 0;
        long sleepMs = 0;
        Throwable lastException = null;
        while (tries <= retries) {
            try {
                action.perform();
                return;
            } catch (GorRetryException e) {
                if (!e.getRetry()) throw e;
                tries++;
                lastException = e;
                sleepMs = retryExceptionHandler(retries, tries, sleepMs, e);
            }
        }
        throw new GorSystemException("Giving up after " + tries + " tries.", lastException);
    }

    protected long retryExceptionHandler(int retries, int tries, long sleepMs, Throwable e) {
        onHandleError(e, sleepMs, retries, tries);

        if (sleepMs <= 0) {
            sleepMs = calcInitialSleep();
        }

        log.warn("Try number " + tries + " of " + (retries + 1) + " failed. Waiting for " + sleepMs + "ms before retrying.", e);

        sleep(sleepMs, e);
        sleepMs = calcNextSleep(sleepMs);

        return sleepMs;
    }

    protected long calcInitialSleep() {
        return Math.round(retryInitialSleepMs * (0.5 + rand.nextFloat()/2.0));
    }

    protected void sleep(long sleepMs, Throwable orginalException) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e1) {
            // If interrupted waiting to retry, throw original exception
            Thread.currentThread().interrupt();
            throw new GorSystemException("Retry handler interrupted", orginalException);
        }
    }

    protected long calcNextSleep(long sleepMs) {
        return Math.min(sleepMs * retryExpBackoff, retryMaxSleepMs);
    }

    protected abstract void onHandleError(Throwable e, long delay, int retries, int tries);
}
