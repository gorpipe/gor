package org.gorpipe.gor.driver.providers.stream.sources.file;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;

import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;

public class FileSourceRetryHandler extends RetryHandlerWithFixedWait {
    public FileSourceRetryHandler(long initialDuration, long totalDuration) {
        super(initialDuration, totalDuration);
    }

    @Override
    protected void checkIfShouldRetryException(GorException e) {
        if (e.getMessage().contains("Stale file handle")) {
            // We generally use RetryInputStream (which reopens the stream on retry),
        } else if (e.getCause() instanceof FileNotFoundException fe) {
            throw e;
        } else if (e.getCause() instanceof FileSystemException fe) {
            throw e;
        }
    }
}
