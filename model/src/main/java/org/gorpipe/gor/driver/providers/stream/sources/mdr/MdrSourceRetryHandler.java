package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;

public class MdrSourceRetryHandler extends RetryHandlerWithFixedWait {
    public MdrSourceRetryHandler(long initialDuration, long totalDuration) {
        super(initialDuration, totalDuration);
    }

    @Override
    protected void onHandleError(GorException e, long delay, int tries) {

    }
}
