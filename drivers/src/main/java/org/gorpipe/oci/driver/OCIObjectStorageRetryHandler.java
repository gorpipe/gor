package org.gorpipe.oci.driver;

import com.oracle.bmc.auth.exception.AuthClientException;
import com.oracle.bmc.model.BmcException;
import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;

import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;

public class OCIObjectStorageRetryHandler extends RetryHandlerWithFixedWait {
    public OCIObjectStorageRetryHandler(long initialDuration, long totalDuration) {
        super(initialDuration, totalDuration);
    }

    @Override
    protected void checkIfShouldRetryException(GorException e) {
        var path = "";

        if (e instanceof GorResourceException gre) {
            path = gre.getUri();
        }

        var cause = ExceptionUtilities.getUnderlyingCause(e);

        if (cause instanceof FileNotFoundException
                || cause instanceof FileSystemException) {
            throw e;
        // Don't retry and improve error messages.
        } else if (cause instanceof AuthClientException) {
            throw new GorResourceException("Client exception", path, e);
        } else if (cause instanceof BmcException bmcException) {
            // See: https://docs.oracle.com/en-us/iaas/Content/API/References/apierrors.htm
            var detail = bmcException.getMessage();
            if (bmcException.getStatusCode() == 400) {
                throw new GorResourceException(String.format("Bad request for resource. Detail: %s. Original message: %s", detail, cause.getMessage()), path, e);
            } else if (bmcException.getStatusCode() == 401) {
                throw new GorResourceException(String.format("Unauthorized. Detail: %s. Original message: %s", detail, cause.getMessage()), path, e);
            } else if (bmcException.getStatusCode() == 403) {
                throw new GorResourceException(String.format("Access Denied. Detail: %s. Original message: %s", detail, cause.getMessage()), path, e);
            } else if (bmcException.getStatusCode() == 404) {
                throw new GorResourceException(String.format("Not Found. Detail: %s. Original message: %s", detail, cause.getMessage()), path, e);
            } else if (bmcException.getStatusCode() == 416) {
                // Deterministic: the range asked for does not exist in the object, so retrying the same
                // range cannot help.  OCIObjectStorageSource self-heals the stale-cached-length case
                // before we get here, so anything reaching this point is a genuinely unsatisfiable range.
                throw new GorResourceException(String.format("Requested byte range not satisfiable. Detail: %s. Original message: %s", detail, cause.getMessage()), path, e);
            }
        }
    }
}
