package org.gorpipe.exceptions;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression test: {@link ExceptionUtilities#gorExceptionToJson} must surface the underlying cause
 * chain (e.g. the real S3/SDK error) even when stack traces are disabled.  Previously the JSON carried
 * only the top-level {@code getMessage()}, so with {@code gor.server.stacktrace.enabled=false} the root
 * cause was completely absent from the error report.
 */
public class UTestExceptionUtilities {

    @After
    public void tearDown() {
        ExceptionUtilities.setShowStackTrace(true);
    }

    @Test
    public void jsonIncludesRootCauseWhenStackTraceDisabled() {
        // Mirror the production chain: exists() -> load metadata -> underlying SDK error.
        Throwable root = new RuntimeException("S3 SlowDown 503 - please reduce your request rate");
        GorResourceException metadata =
                new GorResourceException("Failed to load metadata for thebucket/the/key", "s3://thebucket/the/key", root);
        GorResourceException top =
                new GorResourceException("Exists failed for s3://thebucket/the/key", "s3://thebucket/the/key", metadata);

        // Reproduce the prod deployment: stack traces off.
        ExceptionUtilities.setShowStackTrace(false);
        String json = ExceptionUtilities.gorExceptionToJson(top);

        // stackTrace field must be absent (it was in prod) ...
        assertFalse("stackTrace should be suppressed when disabled", json.contains("stackTrace"));
        // ... but the cause chain must still carry the underlying error, both as a separate field ...
        assertTrue("json must contain the cause field", json.contains("cause"));
        assertTrue("json must contain the root cause message",
                json.contains("S3 SlowDown 503 - please reduce your request rate"));
        // Note: forward slashes are JSON-escaped (thebucket\/the\/key), so match a slash-free fragment.
        assertTrue("json must contain the intermediate metadata cause",
                json.contains("Failed to load metadata for thebucket"));
    }

    @Test
    public void noCauseFieldWhenNoCause() {
        ExceptionUtilities.setShowStackTrace(false);
        GorResourceException e = new GorResourceException("Standalone error", "s3://thebucket/the/key");

        String json = ExceptionUtilities.gorExceptionToJson(e);

        assertFalse("no cause field expected when there is no cause", json.contains("\"cause\""));
    }
}
