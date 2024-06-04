package org.gorpipe.googlecloudstorage.driver;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;

public class GoogleCloudStorageRetryHandler extends RetryHandlerWithFixedWait {
    public GoogleCloudStorageRetryHandler(long initialDuration, long totalDuration) {
        super(initialDuration, totalDuration);
    }

    @Override
    protected void onHandleError(GorException e, long delay, int tries) {

    }
}
