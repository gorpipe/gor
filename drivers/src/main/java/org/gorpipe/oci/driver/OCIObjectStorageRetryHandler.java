package org.gorpipe.oci.driver;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.oracle.bmc.auth.exception.AuthClientException;
import com.oracle.bmc.model.BmcException;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;
import java.util.concurrent.ExecutionException;

public class OCIObjectStorageRetryHandler extends RetryHandlerWithFixedWait {
    public OCIObjectStorageRetryHandler(long initialDuration, long totalDuration) {
        super(initialDuration, totalDuration);
    }

    @Override
    protected void checkIfShouldRetryException(GorException e) {
        var realEx = e.getCause() != null ? e.getCause() : e;

        // Find the real exception.
        if (realEx.getCause() instanceof ExecutionException
                || realEx.getCause() instanceof UncheckedExecutionException) {
            realEx = realEx.getCause();
        }

        // Some exception we throw right back.
        if (realEx.getCause() instanceof FileNotFoundException
                || realEx.getCause() instanceof FileSystemException) {
            throw e;
        }

        var path = "";

        if (e instanceof GorResourceException gre) {
            path = gre.getUri();
        }

        // Don't retry and improve error messages.
        if (realEx instanceof AuthClientException) {
            throw new GorResourceException("Client exception", path, realEx);
        } else if (realEx instanceof BmcException bmcException) {
            var detail = bmcException.getMessage();
            if (bmcException.getStatusCode() == 400) {
                throw new GorResourceException(String.format("Bad request for resource. Detail: %s. Original message: %s", detail, realEx.getMessage()), path, realEx);
            } else if (bmcException.getStatusCode() == 401) {
                throw new GorResourceException(String.format("Unauthorized. Detail: %s. Original message: %s", detail, realEx.getMessage()), path, realEx);
            } else if (bmcException.getStatusCode() == 403) {
                throw new GorResourceException(String.format("Access Denied. Detail: %s. Original message: %s", detail, realEx.getMessage()), path, realEx);
            } else if (bmcException.getStatusCode() == 404) {
                throw new GorResourceException(String.format("Not Found. Detail: %s. Original message: %s", detail, realEx.getMessage()), path, realEx);
            }
        }
    }
}
