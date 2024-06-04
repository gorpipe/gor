package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedRetries;

public class TestRetryHandlerWithFixedRetries extends RetryHandlerWithFixedRetries {

    private int counter = 0;
    public TestRetryHandlerWithFixedRetries(long retryInitialSleepMs, long retryMaxSleepMs, int retryExpBackoff, int retries) {
        super(retryInitialSleepMs, retryMaxSleepMs, retryExpBackoff, retries);
    }

    @Override
    protected void onHandleError(Throwable e, long delay, int retries, int tries) {
        counter++;
    }

    public int getCounter() {
        return counter;
    }
}
