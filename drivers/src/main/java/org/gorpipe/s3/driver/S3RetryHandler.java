package org.gorpipe.s3.driver;

import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;

public class S3RetryHandler extends RetryHandlerWithFixedWait {
    public S3RetryHandler(long initialDuration, long totalDuration) {
        super(initialDuration, totalDuration);
    }

    @Override
    protected void checkIfShouldRetryException(GorException e) {

        var path = "";

        if (e instanceof GorResourceException gre) {
            path = gre.getUri();
        }

        var cause = ExceptionUtilities.getUnderlyingCause(e);

        if (cause instanceof FileNotFoundException || cause instanceof FileSystemException) {
            throw e;
        } else if (cause instanceof S3Exception awsException) {
            var detail = awsException.getMessage();
            if (awsException.statusCode() == 400) {
                throw new GorResourceException(String.format("Bad request for resource. Detail: %s. Original message: %s", detail, e.getMessage()), path, e);
            } else if (awsException.statusCode() == 401) {
                throw new GorResourceException(String.format("Unauthorized. Detail: %s. Original message: %s", detail, e.getMessage()), path, e);
            } else if (awsException.statusCode() == 403) {
                throw new GorResourceException(String.format("Access Denied. Detail: %s. Original message: %s", detail, e.getMessage()), path, e);
            } else if (awsException.statusCode() == 404) {
                throw new GorResourceException(String.format("Not Found. Detail: %s. Original message: %s", detail, e.getMessage()), path, e);
            } else if (awsException.statusCode() == 416) {
                // Deterministic: the range asked for does not exist in the object, so retrying the same
                // range cannot help.  S3Source self-heals the stale-cached-length case before we get
                // here, so anything reaching this point is a genuinely unsatisfiable range.
                throw new GorResourceException(String.format("Requested byte range not satisfiable. Detail: %s. Original message: %s", detail, e.getMessage()), path, e);
            }
        } else if (cause instanceof SdkClientException) {
            throw new GorResourceException("Amazon SDK client exception", path, e);
        }
    }
}
