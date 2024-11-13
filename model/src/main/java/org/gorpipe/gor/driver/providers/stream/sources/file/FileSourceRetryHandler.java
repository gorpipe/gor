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
        if (e.getMessage().equals("Stale file handle")) {
            // Stale file handle errors generally require retries on a higher level as the
            // file needs to be reopened.
            throw new GorResourceException("Stale file handle", "", e);
        } else if (e.getCause() instanceof FileNotFoundException fe) {
            throw GorResourceException.fromIOException(fe, "");
        } else if (e.getCause() instanceof FileSystemException fe) {
            throw GorResourceException.fromIOException(fe, "");
        }
    }
}
