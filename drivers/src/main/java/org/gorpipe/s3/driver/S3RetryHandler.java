package org.gorpipe.s3.driver;

import io.prometheus.metrics.core.metrics.Counter;
import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.utils.RetryHandlerWithFixedWait;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class S3RetryHandler extends RetryHandlerWithFixedWait {
    static final String HANDLER = "s3";
    static final long DEFAULT_MAX_SINGLE_SLEEP_MS = 30_000;
    static final int DEFAULT_MAX_ATTEMPTS = 6;

    // prometheus-metrics appends _total to counters on exposition.
    static final Counter RETRIES = Counter.builder()
            .name("gor_driver_retry")
            .help("GOR-level driver retries by handler, operation, status and outcome")
            .labelNames("handler", "operation", "status", "outcome")
            .register();

    static final Counter RETRY_SLEEP = Counter.builder()
            .name("gor_driver_retry_sleep_seconds")
            .help("Seconds worker threads spent sleeping between GOR-level driver retries")
            .labelNames("handler", "operation")
            .register();

    private final long maxSingleSleep;
    private final int keyPrefixSegments;
    private final int maxAttempts;

    public S3RetryHandler(long initialDuration, long totalDuration) {
        this(initialDuration, totalDuration, DEFAULT_MAX_SINGLE_SLEEP_MS, 1, DEFAULT_MAX_ATTEMPTS);
    }

    public S3RetryHandler(long initialDuration, long totalDuration, long maxSingleSleep, int keyPrefixSegments) {
        this(initialDuration, totalDuration, maxSingleSleep, keyPrefixSegments, DEFAULT_MAX_ATTEMPTS);
    }

    public S3RetryHandler(long initialDuration, long totalDuration, long maxSingleSleep, int keyPrefixSegments, int maxAttempts) {
        super(initialDuration, totalDuration);
        this.maxSingleSleep = maxSingleSleep;
        this.keyPrefixSegments = keyPrefixSegments;
        this.maxAttempts = maxAttempts;
    }

    @Override
    protected int maxAttempts() {
        return maxAttempts;
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

    /**
     * Full jitter over an exponential ceiling. The inherited quadratic backoff with +/-10% jitter
     * re-fired a throttled burst as a near-synchronised wave (ENGKNOW-3723).
     */
    @Override
    protected long calculateDuration(int tries, long initialDuration) {
        long ceiling = Math.min(maxSingleSleep, initialDuration << Math.min(tries - 1, 20));
        return (long) (rand.nextDouble() * ceiling);
    }

    @Override
    protected long retryAfterMillis(GorException e) {
        if (!(ExceptionUtilities.getUnderlyingCause(e) instanceof S3Exception s3e)
                || s3e.awsErrorDetails() == null || s3e.awsErrorDetails().sdkHttpResponse() == null) {
            return 0;
        }
        return s3e.awsErrorDetails().sdkHttpResponse().firstMatchingHeader("Retry-After")
                .map(S3RetryHandler::parseRetryAfter)
                .orElse(0L);
    }

    /** Retry-After is either delay-seconds or an HTTP-date; anything else counts as no hint. */
    static long parseRetryAfter(String value) {
        String v = value.trim();
        try {
            return Math.max(0, Long.parseLong(v) * 1000);
        } catch (NumberFormatException ignored) {
            // Not delay-seconds; try HTTP-date.
        }
        try {
            var at = ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            return Math.max(0, Duration.between(Instant.now(), at).toMillis());
        } catch (DateTimeParseException ignored) {
            return 0;
        }
    }

    @Override
    protected void onRetry(String operation, GorException e, int attempt, long sleepMs) {
        var info = S3FailureInfo.of(e, keyPrefixSegments);
        RETRIES.labelValues(HANDLER, operation, info.status(), "retried").inc();
        RETRY_SLEEP.labelValues(HANDLER, operation).inc(sleepMs / 1000.0);
        log.warn("S3 retry attempt={} op={} bucket={} keyPrefix={} status={} errorCode={} sleepMs={}",
                attempt, operation, info.bucket(), info.keyPrefix(), info.status(), info.errorCode(), sleepMs);
    }

    @Override
    protected void onGiveUp(String operation, GorException e, int attempts, long totalSleepMs) {
        var info = S3FailureInfo.of(e, keyPrefixSegments);
        RETRIES.labelValues(HANDLER, operation, info.status(), "gave_up").inc();
        log.warn("S3 giving up op={} bucket={} keyPrefix={} status={} errorCode={} attempts={} totalSleepMs={}",
                operation, info.bucket(), info.keyPrefix(), info.status(), info.errorCode(), attempts, totalSleepMs, e);
    }
}
