package org.gorpipe.s3.driver;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    /** Records sleeps instead of sleeping. */
    static class RecordingS3RetryHandler extends S3RetryHandler {
        final List<Long> sleeps = new ArrayList<>();

        RecordingS3RetryHandler(long initial, long total, long maxSingle) {
            super(initial, total, maxSingle, 1);
        }

        @Override
        protected void threadSleep(long sleepMs, int tries, Throwable e) {
            sleeps.add(sleepMs);
        }
    }

    private static GorResourceException throttled(String retryAfter) {
        var http = SdkHttpResponse.builder().statusCode(429);
        if (retryAfter != null) http.putHeader("Retry-After", retryAfter);
        var s3e = (S3Exception) S3Exception.builder().statusCode(429)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("TooManyRequests").sdkHttpResponse(http.build()).build())
                .build();
        return (GorResourceException) new GorResourceException(
                "Failed to open S3 object: s3://bucket/dir/sample.gorz", "s3://bucket/dir/sample.gorz", s3e).retry();
    }

    @Test
    public void fullJitterStaysWithinCeiling() {
        var handler = new S3RetryHandler(1000, 1_000_000, 4000, 1);
        boolean sawVariation = false;
        for (int tries = 1; tries <= 6; tries++) {
            long ceiling = Math.min(4000, 1000L << (tries - 1));
            long first = handler.calculateDuration(tries, 1000);
            for (int i = 0; i < 500; i++) {
                long d = handler.calculateDuration(tries, 1000);
                assertTrue("sleep " + d + " outside [0," + ceiling + "]", d >= 0 && d <= ceiling);
                if (d != first) sawVariation = true;
            }
        }
        assertTrue("full jitter must vary the sleep", sawVariation);
    }

    @Test
    public void retryAfterHeaderIsHonoured() {
        var handler = new RecordingS3RetryHandler(100, 60_000, 1000);
        var attempts = new AtomicInteger();

        handler.perform("open", () -> {
            if (attempts.incrementAndGet() == 1) throw throttled("5");
            return "ok";
        });

        assertEquals(1, handler.sleeps.size());
        assertTrue(handler.sleeps.get(0) >= 5000);
    }

    @Test
    public void parseRetryAfterForms() {
        assertEquals(5000, S3RetryHandler.parseRetryAfter("5"));
        assertEquals(0, S3RetryHandler.parseRetryAfter("garbage"));
        assertEquals(0, S3RetryHandler.parseRetryAfter("-3"));
        String inTenSeconds = ZonedDateTime.now().plusSeconds(10).format(DateTimeFormatter.RFC_1123_DATE_TIME);
        long ms = S3RetryHandler.parseRetryAfter(inTenSeconds);
        assertTrue("HTTP-date Retry-After, got " + ms, ms > 8000 && ms <= 10_000);
    }

    @Test
    public void throttleRetryIsCounted() {
        String op = "open";
        double retriedBefore = S3RetryHandler.RETRIES.labelValues("s3", op, "429", "retried").get();
        double sleepBefore = S3RetryHandler.RETRY_SLEEP.labelValues("s3", op).get();
        var handler = new RecordingS3RetryHandler(100, 60_000, 1000);
        var attempts = new AtomicInteger();

        handler.perform(op, () -> {
            if (attempts.incrementAndGet() == 1) throw throttled(null);
            return "ok";
        });

        assertEquals(retriedBefore + 1, S3RetryHandler.RETRIES.labelValues("s3", op, "429", "retried").get(), 0.0);
        assertEquals(sleepBefore + handler.sleeps.get(0) / 1000.0,
                S3RetryHandler.RETRY_SLEEP.labelValues("s3", op).get(), 1e-9);
    }

    @Test
    public void giveUpIsCounted() {
        double before = S3RetryHandler.RETRIES.labelValues("s3", "metadata", "429", "gave_up").get();
        var handler = new RecordingS3RetryHandler(100, 300, 100);

        assertThrows(Exception.class, () -> handler.perform("metadata", (RetryHandlerBase.Action<String>) () -> { throw throttled(null); }));

        assertEquals(before + 1, S3RetryHandler.RETRIES.labelValues("s3", "metadata", "429", "gave_up").get(), 0.0);
    }

    @Test
    public void retryLogIsOneLineWithoutStackTraceOrFileName() {
        Logger logger = (Logger) LoggerFactory.getLogger(RecordingS3RetryHandler.class);
        Level previous = logger.getLevel();
        logger.setLevel(Level.WARN); // tests/config/logback-test.xml may filter WARN for org.gorpipe
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var handler = new RecordingS3RetryHandler(100, 60_000, 1000);
            var attempts = new AtomicInteger();
            handler.perform("open", () -> {
                if (attempts.incrementAndGet() == 1) throw throttled(null);
                return "ok";
            });

            assertEquals(1, appender.list.size());
            ILoggingEvent event = appender.list.get(0);
            String msg = event.getFormattedMessage();
            assertNull("retry lines must not carry a stack trace", event.getThrowableProxy());
            assertTrue(msg, msg.startsWith("S3 retry attempt=1 op=open bucket=bucket keyPrefix=dir/ status=429 errorCode=TooManyRequests"));
            assertFalse("file name must not be logged on retry lines", msg.contains("sample.gorz"));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }
    }

    @Test
    public void giveUpLogCarriesException() {
        Logger logger = (Logger) LoggerFactory.getLogger(RecordingS3RetryHandler.class);
        Level previous = logger.getLevel();
        logger.setLevel(Level.WARN);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var handler = new RecordingS3RetryHandler(100, 150, 100);
            assertThrows(Exception.class, () -> handler.perform("open", (RetryHandlerBase.Action<String>) () -> { throw throttled(null); }));

            ILoggingEvent last = appender.list.get(appender.list.size() - 1);
            assertTrue(last.getFormattedMessage().startsWith("S3 giving up op=open"));
            assertNotNull(last.getThrowableProxy());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }
    }
}
