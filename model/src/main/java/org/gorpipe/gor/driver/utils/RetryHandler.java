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

import org.gorpipe.gor.driver.GorDriverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utility class that implements retry logic around arbitrary IO operations.
 * <p>
 * Created by villi on 17/09/15.
 * <p>
 * TODO: Change to interface with implementations for different strategies (exponential backoff vs ...) ?
 */
public class RetryHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final long retryInitialSleepMs;
    private final long retryMaxSleepMs;

    public RetryHandler(GorDriverConfig config) {
        this(config.retryInitialSleep().toMillis(), config.retryMaxSleep().toMillis());
    }

    public RetryHandler(long retryInitialSleepMs, long retryMaxSleepMs) {
        this.retryInitialSleepMs = retryInitialSleepMs;
        this.retryMaxSleepMs = retryMaxSleepMs;
    }

    /**
     * Try an IO operation.  If it fails with any IOException - it will be retried up to a maximum.
     * Before retrying, it will sleep - doing an exponential back-off - sleeping originally for retryInitialSleepMs up to a maximum of retryMaxSleepMs.
     *
     * @param op      IO Operation
     * @param retries Maximum number of retries
     * @param onRetry Optional operation to execute before retry - e.g. to close/reopen underlying stream etc.
     *                It can also be used to determine if operation is really retryable - e.g. based on exception thrown.
     * @param <T>     Data type returned by IO operation.
     * @return Object returned by successful IO operation.
     * @throws IOException - exception thrown on last retry or  execption thrown by any call to onRetry
     */
    public <T> T tryOp(IoOp<T> op, int retries, OnRetryOp onRetry) throws IOException {
        int tries = 0;
        long sleepMs = retryInitialSleepMs;
        IOException lastException = null;
        while (tries <= retries) {
            try {
                return op.perform();
            } catch (IOException e) {
                if (e.getMessage().equals("Stale file handle")) {
                    // Stale file handle errors generally require retries on a higher level as the
                    // file needs to be reopened.
                    throw e;
                }
                lastException = e;
                log.debug("Retry number " + tries + " of " + retries + " and waiting " + sleepMs + "ms of " + retryMaxSleepMs + "ms", e);
                try {
                    Thread.sleep(sleepMs);
                    if (onRetry != null) {
                        onRetry.onRetry(e);
                    }
                } catch (InterruptedException e1) {
                    // If interrupted waiting to retry, throw original exception
                    throw e;
                }
                tries++;
                if (sleepMs < retryMaxSleepMs) {
                    sleepMs *= 2;
                }
            }
        }
        throw new IOException("Giving up after " + tries + " retries", lastException);
    }

    /**
     * Try and retry an io operation. Same as above but without onRetry callback.
     */
    public <T> T tryOp(IoOp<T> op, int retries) throws IOException {
        return tryOp(op, retries, null);
    }

    /**
     * Interface for on retry callback
     */
    public interface OnRetryOp {
        /**
         * Called before io operation is retried.
         *
         * @param e Exception thrown by last io operation
         * @throws IOException - if retry operation should be aborted.
         */
        void onRetry(IOException e) throws IOException;
    }

    /**
     * Interface for an arbitrary IO operation
     */
    public interface IoOp<T> {
        T perform() throws IOException;
    }
}
