package org.gorpipe.s3.driver;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.SdkMetric;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Maps AWS SDK per-call metrics to Prometheus, keeping each call's operation attached to its
 * attempts so throttling can be attributed per operation (ENGKNOW-3723).
 * <p>
 * Labels are deliberately low-cardinality: no bucket or key.
 */
public class PrometheusMetricPublisher implements MetricPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricPublisher.class);

    static final String ATTEMPT_COLLECTION = "ApiCallAttempt";
    static final String NO_ERROR = "none";
    static final String NO_STATUS = "-1";
    static final String UNKNOWN_OPERATION = "unknown";

    // prometheus-metrics appends _total to counters on exposition.
    static final Counter API_CALLS = Counter.builder()
            .name("gor_driver_s3_api_calls")
            .help("S3 API calls by operation and outcome")
            .labelNames("operation", "outcome")
            .register();

    static final Counter API_ATTEMPTS = Counter.builder()
            .name("gor_driver_s3_api_attempts")
            .help("S3 API call attempts, including SDK retries, by operation, HTTP status and SDK error type")
            .labelNames("operation", "http_status", "error_type")
            .register();

    static final Histogram API_CALL_DURATION = Histogram.builder()
            .name("gor_driver_s3_api_call_duration_seconds")
            .help("S3 API call duration including SDK retries")
            .labelNames("operation")
            .register();

    static final Histogram BACKOFF = Histogram.builder()
            .name("gor_driver_s3_backoff_seconds")
            .help("SDK backoff delay before an S3 retry attempt")
            .labelNames("operation")
            .register();

    static final Counter METRIC_ERRORS = Counter.builder()
            .name("gor_driver_s3_metric_errors")
            .help("S3 SDK metric collections that could not be mapped")
            .register();

    @Override
    public void publish(MetricCollection call) {
        try {
            record(call);
        } catch (Exception e) {
            // Metrics must never fail an S3 call.
            METRIC_ERRORS.inc();
            logger.debug("Error publishing S3 metrics", e);
        }
    }

    private void record(MetricCollection call) {
        String operation = first(call, CoreMetric.OPERATION_NAME).orElse(UNKNOWN_OPERATION);

        first(call, CoreMetric.API_CALL_SUCCESSFUL)
                .ifPresent(ok -> API_CALLS.labelValues(operation, ok ? "success" : "failure").inc());
        first(call, CoreMetric.API_CALL_DURATION)
                .ifPresent(d -> API_CALL_DURATION.labelValues(operation).observe(seconds(d)));

        for (MetricCollection attempt : call.children()) {
            if (!ATTEMPT_COLLECTION.equals(attempt.name())) continue;

            String status = first(attempt, HttpMetric.HTTP_STATUS_CODE).map(String::valueOf).orElse(NO_STATUS);
            String errorType = first(attempt, CoreMetric.ERROR_TYPE).orElse(NO_ERROR);
            API_ATTEMPTS.labelValues(operation, status, errorType).inc();

            first(attempt, CoreMetric.BACKOFF_DELAY_DURATION)
                    .filter(d -> !d.isZero())
                    .ifPresent(d -> BACKOFF.labelValues(operation).observe(seconds(d)));
        }
    }

    private static <T> Optional<T> first(MetricCollection collection, SdkMetric<T> metric) {
        List<T> values = collection.metricValues(metric);
        return values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
    }

    private static double seconds(Duration d) {
        return d.toNanos() / 1e9;
    }

    @Override
    public void close() {
        // Nothing to do.
    }
}
