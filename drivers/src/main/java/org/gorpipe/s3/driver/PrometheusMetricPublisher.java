package org.gorpipe.s3.driver;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PrometheusMetricPublisher implements MetricPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricPublisher.class);
    static final String SUBSYSTEM_LABEL_NAME = "subsystem";
    static final String CATEGORIES_LABEL_NAME = "categories";
    static final String LEVEL_LABEL_NAME = "level";
    static final String TYPE_LABEL_NAME = "type";
    static final String NAMESPACE_LABEL = "s3";
    static final String METRIC_PREFIX = "gor_driver_s3_";

    static final String[] BLACKLISTED_METRIC_WORDS = new String[]{"RequestId"};

    private static final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private static final Map<String, Gauge> gauges = new ConcurrentHashMap<>();
    private static final Map<String, Histogram> histograms = new ConcurrentHashMap<>();

    @Override
    public void publish(MetricCollection metricCollection) {
        try {
            mapMetrics(metricCollection);
        } catch (Exception e) {
            // We should not fail even if we can not collect metrics.
            logger.warn("Error publishing metrics", e);
        }
    }

    private void mapMetrics(MetricCollection metricCollection) {
        metricCollection.stream().forEach(metricRecord -> {
            try {
                String metricName = METRIC_PREFIX + metricRecord.metric().name();
                var value = metricRecord.value();
                var levelLabel = metricRecord.metric().level().name();
                var categoriesLabel = new HashSet<>(metricRecord.metric().categories().stream().map(Enum::name).toList()).toString();

                if (value instanceof Number || value instanceof Duration) {
                    var labelNames = new String[]{SUBSYSTEM_LABEL_NAME, CATEGORIES_LABEL_NAME, LEVEL_LABEL_NAME};
                    var labelsArray = new String[]{NAMESPACE_LABEL, categoriesLabel, levelLabel};

                    if (metricName.contains("Count")) {
                        double doubleValue = ((Number) value).doubleValue();
                        Counter counter = counters.computeIfAbsent(metricName, name -> Counter.builder()
                                .name(name)
                                .help(name)
                                .labelNames(labelNames)
                                .register());
                        counter.labelValues(labelsArray).inc(doubleValue);
                    } else if (metricName.contains("Duration") || metricName.contains("Time")) {
                        double doubleValue = (value instanceof Duration) ? ((Duration) value).toMillis() : ((Number) value).doubleValue();
                        Histogram histogram = histograms.computeIfAbsent(metricName, name -> Histogram.builder()
                                .name(name)
                                .help(name)
                                .labelNames(labelNames)
                                .register());
                        histogram.labelValues(labelsArray).observe(doubleValue);
                    } else {
                        double doubleValue = ((Number) value).doubleValue();
                        Gauge gauge = gauges.computeIfAbsent(metricName, name -> Gauge.builder()
                                .name(name)
                                .help(name)
                                .labelNames(labelNames)
                                .register());
                        gauge.labelValues(labelsArray).set(doubleValue);
                    }
                } else if (Arrays.stream(BLACKLISTED_METRIC_WORDS).noneMatch(metricName::contains)) {
                    var typeLabel = value.toString();
                    var labelsArray = new String[]{NAMESPACE_LABEL, categoriesLabel, levelLabel, typeLabel};

                    Counter counter = counters.computeIfAbsent(metricName, name -> Counter.builder()
                            .name(name)
                            .help(name)
                            .labelNames(SUBSYSTEM_LABEL_NAME, CATEGORIES_LABEL_NAME, LEVEL_LABEL_NAME, TYPE_LABEL_NAME)
                            .register());

                    counter.labelValues(labelsArray).inc();
                }
            } catch (Exception e) {
                // Ignore errors, map what we can.
                logger.warn("Error mapping metric", e);
            }
        });

        for (MetricCollection childCollection : metricCollection.children()) {
            mapMetrics(childCollection);
        }
    }

    @Override
    public void close() {
        // Nothing to do.
    }
}
