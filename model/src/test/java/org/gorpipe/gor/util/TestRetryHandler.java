package org.gorpipe.gor.util;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorRetryException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;

public class TestRetryHandler extends RetryHandlerWithFixedWait {

    int counter = 0;

    public TestRetryHandler(long initialDuration, long totalDuration) {
        super(initialDuration, totalDuration);
    }

    @Override
    protected void checkIfShouldRetryException(GorException e) {

        counter ++;

        if (e instanceof GorRetryException gre && gre.isRetry()) {
            return;
        }

        throw new GorSystemException("Test", e);
    }

    public int getCounter() { return counter; };


}
