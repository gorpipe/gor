package org.gorpipe.s3.driver;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;
import java.util.concurrent.ExecutionException;

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

        var cause = getCause(e);

        if (cause instanceof FileNotFoundException
        || cause instanceof FileSystemException) {
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
            }
        } else if (cause instanceof SdkClientException) {
            throw new GorResourceException("Amazon SDK client exception", path, e);
        }
    }
}
