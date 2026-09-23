package org.gorpipe.s3.driver;

import org.junit.Test;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;

import java.time.Duration;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The publisher must keep each API call's operation attached to its attempts, so a 429 can be
 * attributed to GetObject vs HeadObject (ENGKNOW-3723). Operation names are made unique per test
 * because the Prometheus metrics are process-wide.
 */
public class UTestPrometheusMetricPublisher {

    private final PrometheusMetricPublisher publisher = new PrometheusMetricPublisher();

    private static String op() {
        return "GetObject-" + UUID.randomUUID();
    }

    private static void attempt(MetricCollector call, Integer status, String errorType, Duration backoff) {
        MetricCollector a = call.createChild("ApiCallAttempt");
        if (status != null) a.reportMetric(HttpMetric.HTTP_STATUS_CODE, status);
        if (errorType != null) a.reportMetric(CoreMetric.ERROR_TYPE, errorType);
        a.reportMetric(CoreMetric.BACKOFF_DELAY_DURATION, backoff);
    }

    private static MetricCollection throttledTwiceThenOk(String op) {
        MetricCollector call = MetricCollector.create("ApiCall");
        call.reportMetric(CoreMetric.OPERATION_NAME, op);
        call.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, true);
        call.reportMetric(CoreMetric.API_CALL_DURATION, Duration.ofMillis(1500));
        attempt(call, 429, "Throttling", Duration.ZERO);
        attempt(call, 429, "Throttling", Duration.ofMillis(200));
        attempt(call, 200, null, Duration.ofMillis(400));
        return call.collect();
    }

    @Test
    public void throttledAttemptsAreCountedPerOperation() {
        String op = op();
        publisher.publish(throttledTwiceThenOk(op));

        assertEquals(2.0, PrometheusMetricPublisher.API_ATTEMPTS.labelValues(op, "429", "Throttling").get(), 0.0);
        assertEquals(1.0, PrometheusMetricPublisher.API_ATTEMPTS.labelValues(op, "200", "none").get(), 0.0);
        assertEquals(1.0, PrometheusMetricPublisher.API_CALLS.labelValues(op, "success").get(), 0.0);
        assertEquals(0.0, PrometheusMetricPublisher.API_CALLS.labelValues(op, "failure").get(), 0.0);
    }

    @Test
    public void durationAndBackoffAreObservedInSeconds() {
        String op = op();
        publisher.publish(throttledTwiceThenOk(op));

        var duration = PrometheusMetricPublisher.API_CALL_DURATION.labelValues(op);
        assertEquals(1, duration.getCount());
        assertEquals(1.5, duration.getSum(), 1e-9);

        var backoff = PrometheusMetricPublisher.BACKOFF.labelValues(op);
        assertEquals("zero backoff (first attempt) is not observed", 2, backoff.getCount());
        assertEquals(0.6, backoff.getSum(), 1e-9);
    }

    @Test
    public void attemptWithoutHttpResponseUsesMinusOneStatus() {
        String op = op();
        MetricCollector call = MetricCollector.create("ApiCall");
        call.reportMetric(CoreMetric.OPERATION_NAME, op);
        call.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, false);
        attempt(call, null, "IO", Duration.ZERO);
        publisher.publish(call.collect());

        assertEquals(1.0, PrometheusMetricPublisher.API_ATTEMPTS.labelValues(op, "-1", "IO").get(), 0.0);
        assertEquals(1.0, PrometheusMetricPublisher.API_CALLS.labelValues(op, "failure").get(), 0.0);
    }

    @Test
    public void malformedCollectionNeverThrows() {
        MetricCollection broken = mock(MetricCollection.class);
        when(broken.metricValues(any())).thenThrow(new IllegalStateException("broken"));
        double before = PrometheusMetricPublisher.METRIC_ERRORS.get();

        publisher.publish(broken);

        assertEquals(before + 1, PrometheusMetricPublisher.METRIC_ERRORS.get(), 0.0);
    }
}
