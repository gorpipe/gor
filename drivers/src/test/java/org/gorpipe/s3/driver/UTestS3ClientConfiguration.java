package org.gorpipe.s3.driver;

import org.aeonbits.owner.ConfigFactory;
import org.gorpipe.base.security.Credentials;
import org.junit.Test;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Regression coverage for ENGKNOW-3723. {@code applyBaseClientConfig} called
 * {@code overrideConfiguration(Consumer)} three times; each call replaces the previous
 * {@code ClientOverrideConfiguration}, so the retry strategy and the Prometheus publisher were
 * silently dropped. 429s were then retried by the SDK default strategy and again by GOR, and no
 * {@code gor_driver_s3_*} metrics were ever published.
 */
public class UTestS3ClientConfiguration {

    private static S3Configuration config(Map<String, String> props) {
        return ConfigFactory.create(S3Configuration.class, props);
    }

    private static S3Client build(S3Configuration cfg) {
        var provider = new S3SourceProvider(null, cfg, null, Set.of());
        var cred = new Credentials.Builder()
                .service("s3")
                .lookupKey("thebucket")
                .set(Credentials.Attr.KEY, "key")
                .set(Credentials.Attr.SECRET, "secret")
                .set(Credentials.Attr.REGION, "us-east-1")
                .build();
        var builder = S3Client.builder().httpClientBuilder(ApacheHttpClient.builder());
        provider.applyBaseClientConfig(builder, cred);
        return builder.build();
    }

    @Test
    public void builtClientKeepsMetricPublisher() {
        try (var client = build(config(Map.of()))) {
            var override = client.serviceClientConfiguration().overrideConfiguration();
            assertTrue("the Prometheus publisher must survive client construction",
                    override.metricPublishers().stream().anyMatch(p -> p instanceof PrometheusMetricPublisher));
        }
    }

    @Test
    public void builtClientUsesConfiguredRetryCount() {
        // 1 retry => 2 attempts: distinct from every SDK default, so it only passes if our strategy is applied.
        try (var client = build(config(Map.of("gor.s3.conn.retries", "1")))) {
            var strategy = client.serviceClientConfiguration().overrideConfiguration().retryStrategy().orElseThrow();
            assertEquals(2, strategy.maxAttempts());
        }
    }

    @Test
    public void defaultRetriesGiveFourAttempts() {
        assertEquals(4, S3SourceProvider.buildRetryStrategy(config(Map.of())).maxAttempts());
    }

    @Test
    public void zeroRetriesMeansSingleAttempt() {
        assertEquals(1, S3SourceProvider.buildRetryStrategy(config(Map.of("gor.s3.conn.retries", "0"))).maxAttempts());
    }

    @Test
    public void negativeRetriesAreRejected() {
        assertThrows(RuntimeException.class, () ->
                S3SourceProvider.buildRetryStrategy(config(Map.of("gor.s3.conn.retries", "-1"))));
    }

    @Test
    public void adaptiveModeSelectsAdaptiveStrategy() {
        var strategy = S3SourceProvider.buildRetryStrategy(config(Map.of("gor.s3.retry.mode", "adaptive")));
        assertTrue(strategy instanceof AdaptiveRetryStrategy);
    }

    @Test
    public void unknownModeIsRejected() {
        var e = assertThrows(RuntimeException.class, () ->
                S3SourceProvider.buildRetryStrategy(config(Map.of("gor.s3.retry.mode", "bogus"))));
        assertTrue(e.getMessage().contains("gor.s3.retry.mode"));
    }
}
