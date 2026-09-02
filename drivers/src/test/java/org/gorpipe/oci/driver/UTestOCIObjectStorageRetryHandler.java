package org.gorpipe.oci.driver;

import com.oracle.bmc.model.BmcException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for how {@link OCIObjectStorageRetryHandler} classifies object store status codes.
 *
 * <p>The S3 counterpart of this, {@link org.gorpipe.s3.driver.UTestS3RetryHandler}, covers the same
 * ground for ENGKNOW-3722: a stale cached object length produces a deterministic
 * {@code 416 Range Not Satisfiable}, which must not be run through the retry ladder.
 */
public class UTestOCIObjectStorageRetryHandler {

    private static GorResourceException ociFailure(int statusCode, String detail) {
        return (GorResourceException) new GorResourceException(
                "Failed to open OCI object: oci://bucket/some/file.gor.link",
                "oci://bucket/some/file.gor.link",
                new BmcException(statusCode, "TestServiceCode", detail, "test-opc-request-id")).retry();
    }

    @Test
    public void rangeNotSatisfiableFailsWithoutRetrying() {
        var handler = new OCIObjectStorageRetryHandler(100, 1000);
        var attempts = new AtomicInteger();

        assertThrows(GorResourceException.class, () ->
                handler.perform((RetryHandlerBase.ActionVoid) () -> {
                    attempts.incrementAndGet();
                    throw ociFailure(416, "The requested range is not satisfiable");
                }));

        assertEquals("a 416 is deterministic and must fail on the first attempt", 1, attempts.get());
    }

    @Test
    public void serverErrorIsStillRetried() {
        var handler = new OCIObjectStorageRetryHandler(100, 1000);
        var attempts = new AtomicInteger();

        assertThrows(Exception.class, () ->
                handler.perform((RetryHandlerBase.ActionVoid) () -> {
                    attempts.incrementAndGet();
                    throw ociFailure(500, "Internal Server Error");
                }));

        assertTrue("transient server errors must keep retrying", attempts.get() > 1);
    }
}
