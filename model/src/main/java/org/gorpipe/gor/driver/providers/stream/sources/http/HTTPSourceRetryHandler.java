package org.gorpipe.gor.driver.providers.stream.sources.http;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;

public class HTTPSourceRetryHandler extends RetryHandlerWithFixedWait {
    public HTTPSourceRetryHandler(long initialDuration, long totalDuration) {
        super(initialDuration, totalDuration);
    }

    @Override
    protected void onHandleError(GorException e, long delay, int tries) {

    }
}
