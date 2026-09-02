package org.gorpipe.s3.driver;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for how {@link S3RetryHandler} classifies S3 status codes as retryable or not.
 *
 * <p>Regression coverage for ENGKNOW-3722: a stale cached object length makes GOR request a byte
 * range beyond the object's end, which S3 answers with a deterministic
 * {@code 416 Range Not Satisfiable}. That status was missing from the handler's non-retryable list,
 * so every occurrence burned the full retry ladder (~167 s in production) before failing anyway.
 */
public class UTestS3RetryHandler {

    private static GorResourceException s3Failure(int statusCode, String detail) {
        return (GorResourceException) new GorResourceException(
                "Failed to open S3 object: s3://bucket/some/file.gor.link",
                "s3://bucket/some/file.gor.link",
                S3Exception.builder().statusCode(statusCode).message(detail).build()).retry();
    }

    @Test
    public void rangeNotSatisfiableFailsWithoutRetrying() {
        var handler = new S3RetryHandler(100, 1000);
        var attempts = new AtomicInteger();

        assertThrows(GorResourceException.class, () ->
                handler.perform((RetryHandlerBase.ActionVoid) () -> {
                    attempts.incrementAndGet();
                    throw s3Failure(416, "byte range bytes=98674-98824 cannot be satisfied "
                            + "from object with content length 98674");
                }));

        assertEquals("a 416 is deterministic and must fail on the first attempt", 1, attempts.get());
    }

    @Test
    public void serverErrorIsStillRetried() {
        var handler = new S3RetryHandler(100, 1000);
        var attempts = new AtomicInteger();

        assertThrows(Exception.class, () ->
                handler.perform((RetryHandlerBase.ActionVoid) () -> {
                    attempts.incrementAndGet();
                    throw s3Failure(500, "Internal Error");
                }));

        assertTrue("transient server errors must keep retrying", attempts.get() > 1);
    }
}
