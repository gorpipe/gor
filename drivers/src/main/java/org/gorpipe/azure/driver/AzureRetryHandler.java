package org.gorpipe.azure.driver;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;

public class AzureRetryHandler extends RetryHandlerWithFixedWait {
    public AzureRetryHandler(long initialDuration, long totalDuration) {
        super(initialDuration, totalDuration);
    }

    @Override
    protected void checkIfShouldRetryException(GorException e) {

    }
}
