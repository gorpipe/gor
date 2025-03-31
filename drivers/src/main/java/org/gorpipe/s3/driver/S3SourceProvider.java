/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.s3.driver;

import com.google.auto.service.AutoService;
import org.apache.commons.lang3.StringUtils;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.StreamSourceProvider;
import org.gorpipe.gor.driver.utils.CredentialClientCache;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.gorpipe.gor.util.StringUtil;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.crt.ProxyConfiguration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3BaseClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.services.s3.crt.S3CrtProxyConfiguration;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

@AutoService(SourceProvider.class)
public class S3SourceProvider extends StreamSourceProvider {

    // Beginning with version 2.30.0 of the AWS SDK for Java 2.x, the SDK provides default integrity protections
    // by automatically calculating a CRC32 checksum for uploads.
    // This is NOT compatible with the OCI S3 compatibility layer so we either need to:
    // 1. Turn off the checksums in the SDK by setting the system property aws.requestChecksumValidation to "false".
    //    See: https://docs.aws.amazon.com/sdkref/latest/guide/feature-dataintegrity.html
    // 2. Use .checksumAlgorithm(ChecksumAlgorithm.SHA256), on the put object (not tested).
    //    See: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/s3-checksums.html
    // So for now turn off the checksums.
    // NOTE:  We also turn it off for AWS S3 buckets.
    static {
        System.setProperty("aws.requestChecksumCalculation", "WHEN_REQUIRED");
        System.setProperty("aws.responseChecksumValidation", "WHEN_REQUIRED");
    }

    private static final boolean USE_CRT_CLIENT = Boolean.parseBoolean(System.getProperty("gor.s3.client.crt", "true"));
    private static final boolean USE_ASYNC_CLIENT = Boolean.parseBoolean(System.getProperty("gor.s3.client.async", "false"));
    private static final boolean FORCE_PATH_STYLE = Boolean.parseBoolean(System.getProperty("gor.s3.forcePathStyle", "false"));

    private final CredentialClientCache<S3Client> clientCredCache = new CredentialClientCache<>(S3SourceType.S3.getName(), this::createClient);
    private final CredentialClientCache<S3AsyncClient> asyncClientCredCache = new CredentialClientCache<>(S3SourceType.S3.getName(), this::createAsyncClient);

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

    private final S3Configuration s3Config;


    public S3SourceProvider() {
        s3Config = ConfigManager.getPrefixConfig("gor.s3", S3Configuration.class);
    }

    public S3SourceProvider(GorDriverConfig config, S3Configuration s3Config, FileCache cache,
                            Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, cache, initialFactories);
        this.s3Config = s3Config;
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{S3SourceType.S3};
    }

    @Override
    public S3Source resolveDataSource(SourceReference sourceReference)
            throws IOException {
        S3Url url = S3Url.parse(sourceReference);
        S3Client client = getClient(sourceReference.getSecurityContext(), url.getLookupKey());
        S3AsyncClient asyncClient = getAsyncClient(sourceReference.getSecurityContext(), url.getLookupKey());
        return new S3Source(client, asyncClient, sourceReference);
    }

    @Override
    protected RetryHandlerBase getRetryHandler() {
        if (retryHandler == null) {
            retryHandler = new S3RetryHandler(config.retryInitialSleep().toMillis(), config.retryMaxSleep().toMillis());
        }
        return retryHandler;
    }

    protected S3Client getClient(String securityContext, String bucket) throws IOException {
        BundledCredentials creds = BundledCredentials.fromSecurityContext(securityContext);
        return clientCredCache.getClient(creds, bucket);
    }

    private S3Client createClient(Credentials cred) {
        if (USE_CRT_CLIENT) {
            return createSyncCrtClient(cred);
        }
        return createApacheClient(cred);
    }

    private S3Client createSyncCrtClient(Credentials cred) {
        var builder = S3Client.builder();

        AwsCrtHttpClient.Builder httpClientBuilder = AwsCrtHttpClient.builder()
                .connectionTimeout(s3Config.connectionTimeout())  // Default was 2s
                .maxConcurrency(s3Config.connectionPoolSize())
                .tcpKeepAliveConfiguration(b -> b
                        .keepAliveInterval(Duration.ofMillis(s3Config.socketTimeout().toMillis()/2))
                        .keepAliveTimeout(s3Config.connectionTimeout()))
                ;

        final String proxy = System.getProperty("http.proxyHost");
        final String port = System.getProperty("http.proxyPort");
        if (proxy != null && port != null) {
            log.info("RDA AWS connection - Proxy set to {}:{}", proxy, port);
            final ProxyConfiguration.Builder proxyConfig = ProxyConfiguration.builder();
            proxyConfig.host(proxy);
            proxyConfig.port(Integer.parseInt(port));
            httpClientBuilder.proxyConfiguration(proxyConfig.build());
        }

        builder.httpClientBuilder(httpClientBuilder);

        applyBaseClientConfig(builder, cred);

        return builder.build();
    }

    private S3Client createApacheClient(Credentials cred) {
        var builder = S3Client.builder();

        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder()
                .connectionTimeout(s3Config.connectionTimeout())  // Default was 2s
                .socketTimeout(s3Config.socketTimeout())          // Default was 30s
                .maxConnections(s3Config.connectionPoolSize())    // Default was 50
                .tcpKeepAlive(true)
                ;

        // Note: See defaults values at https://github.com/aws/aws-sdk-java-v2/blob/master/http-client-spi/src/main/java/software/amazon/awssdk/http/SdkHttpConfigurationOption.java

        final String proxy = System.getProperty("http.proxyHost");
        final String port = System.getProperty("http.proxyPort");
        if (proxy != null && port != null) {
            log.info("RDA AWS connection - Proxy set to {}:{}", proxy, port);
            final software.amazon.awssdk.http.apache.ProxyConfiguration.Builder proxyConfig = software.amazon.awssdk.http.apache.ProxyConfiguration.builder();
            proxyConfig.endpoint(URI.create(proxy + ":" + port));
            httpClientBuilder.proxyConfiguration(proxyConfig.build());
        }

        builder.httpClientBuilder(httpClientBuilder);

        applyBaseClientConfig(builder, cred);

        return builder.build();
    }

    protected S3AsyncClient getAsyncClient(String securityContext, String bucket) throws IOException {
        if (!USE_ASYNC_CLIENT) {
            return null;
        }
        BundledCredentials creds = BundledCredentials.fromSecurityContext(securityContext);
        return asyncClientCredCache.getClient(creds, bucket);
    }

    private S3AsyncClient createAsyncClient(Credentials cred) {
        if (USE_CRT_CLIENT) {
            return createAsyncCrtClient(cred);
        }
        return createNettyClient(cred);
    }

    private S3AsyncClient createNettyClient(Credentials cred) {
        var builder = S3AsyncClient.builder();

        NettyNioAsyncHttpClient.Builder httpClientBuilder = NettyNioAsyncHttpClient.builder()
                .connectionTimeout(s3Config.connectionTimeout())  // Default was 2s
                .maxConcurrency(s3Config.connectionPoolSize())
                .tcpKeepAlive(true)
                ;

        final String proxy = System.getProperty("http.proxyHost");
        final String port = System.getProperty("http.proxyPort");
        if (proxy != null && port != null) {
            log.info("RDA AWS connection - Proxy set to {}:{}", proxy, port);
            final var proxyConfig = software.amazon.awssdk.http.nio.netty.ProxyConfiguration.builder();
            proxyConfig.scheme("http");
            proxyConfig.host(proxy);
            proxyConfig.port(Integer.parseInt(port));
            httpClientBuilder.proxyConfiguration(proxyConfig.build());
        }

        builder.httpClientBuilder(httpClientBuilder);

        applyBaseClientConfig(builder, cred);

        return builder.build();
    }

    private S3AsyncClient createAsyncCrtClient(Credentials cred) {
        var builder = S3AsyncClient.crtBuilder();

        var httpConfigBuilder = S3CrtHttpConfiguration.builder()
                .connectionTimeout(s3Config.connectionTimeout());

        final String proxy = System.getProperty("http.proxyHost");
        final String port = System.getProperty("http.proxyPort");
        if (proxy != null && port != null) {
            log.info("RDA AWS connection - Proxy set to {}:{}", proxy, port);
            final S3CrtProxyConfiguration.Builder proxyConfig = S3CrtProxyConfiguration.builder();
            proxyConfig.host(proxy);
            proxyConfig.port(Integer.parseInt(port));
            httpConfigBuilder.proxyConfiguration(proxyConfig.build());
        }

        builder.httpConfiguration(httpConfigBuilder.build());

        // See: https://github.com/aws/aws-sdk-java-v2/blob/master/services/s3/src/main/java/software/amazon/awssdk/services/s3/S3CrtAsyncClientBuilder.java
        builder.maxConcurrency(s3Config.connectionPoolSize());
        //builder.accelerate(true)
        //builder.targetThroughputInGbps(5.0);
        //builder.maxNativeMemoryLimitInBytes(1L * 1024 * 1024 * 1024);
        //builder.minimumPartSizeInBytes(5L * 1024 * 1024);
        //builder.initialReadBufferSizeInBytes(1L * 1024 * 1024);

        var endpoint = getEndpoint(cred);
        if (!StringUtil.isEmpty(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        builder.region(getRegion(cred, endpoint));

        builder.credentialsProvider(getCredentialsProvider(cred));

        builder.retryConfiguration(b -> b.numRetries(s3Config.connectionRetries()));

//        var metricsPub = new PrometheusMetricPublisher();
//        builder.overrideConfiguration(c -> c.addMetricPublisher(metricsPub));

        builder.futureCompletionExecutor(scheduledExecutorService);

        // OCI compat layer needs path style access.
        if (isOciEndpoint(endpoint)) {
            builder.forcePathStyle(true);
        }

        builder.crossRegionAccessEnabled(true);

        return builder.build();
    }

    private void applyBaseClientConfig(S3BaseClientBuilder<?, ?> builder, Credentials cred) {
        //builder.accelerate(true);

        var endpoint = getEndpoint(cred);
        if (!StringUtil.isEmpty(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        builder.region(getRegion(cred, endpoint));

        builder.credentialsProvider(getCredentialsProvider(cred));

        builder.overrideConfiguration(o -> o.retryStrategy(b -> b.maxAttempts(s3Config.connectionRetries())));

        var metricsPub = new PrometheusMetricPublisher();
        log.info("Adding metrics publisher: {}", metricsPub);
        builder.overrideConfiguration(c -> c.addMetricPublisher(metricsPub));

        builder.overrideConfiguration(c -> c.scheduledExecutorService(scheduledExecutorService));

        // OCI compat layer needs path style access.
        if (isOciEndpoint(endpoint) || FORCE_PATH_STYLE) {
            builder.forcePathStyle(true);
        }

        builder.crossRegionAccessEnabled(true);
    }

    AwsCredentialsProvider getCredentialsProvider(Credentials cred) {
        var awsKey = System.getProperty("aws.accessKeyId", "");
        var awsSecret = System.getProperty("aws.secretKey", "");
        var sessionToken = System.getProperty("aws.sessionToken", "");

        if (cred != null && !cred.isNull()) {
            awsKey = cred.getOrDefault(Credentials.Attr.KEY, awsKey);
            awsSecret = cred.getOrDefault(Credentials.Attr.SECRET, awsSecret);
            sessionToken = cred.getOrDefault(Credentials.Attr.SESSION_TOKEN, sessionToken);
        }

        if (!awsKey.isEmpty() && !awsSecret.isEmpty()) {
            if (!sessionToken.isEmpty()) {
                return StaticCredentialsProvider.create(
                        AwsSessionCredentials.builder()
                                .accessKeyId(awsKey)
                                .secretAccessKey(awsSecret)
                                .sessionToken(sessionToken)
                                .build());
            } else {
                log.debug("CredentialsProvider: StaticCredentialsProvider for {}:{}", cred.getService(), cred.getLookupKey());
                return StaticCredentialsProvider.create(
                        AwsBasicCredentials.builder()
                                .accessKeyId(awsKey)
                                .secretAccessKey(awsSecret)
                                .build());
            }
        } else {
            log.info("CredentialsProvider: DefaultCredentialsProvider.");
            return DefaultCredentialsProvider.create();
        }
    }

    String getEndpoint(Credentials creds) {
        var endpoint = "";
        if (creds != null && !creds.isNull()) {
            endpoint = creds.get(Credentials.Attr.API_ENDPOINT);
        }

        if (StringUtils.isEmpty(endpoint)) {
            endpoint = s3Config.s3Endpoint();
        }

        if (StringUtils.isEmpty(endpoint)) {
            endpoint = System.getProperty("s3.endpoint");
        }

        if (StringUtils.isEmpty(endpoint)) {
            endpoint = System.getProperty("aws.endpointUrl");
        }

        return endpoint;
    }

    boolean isOciEndpoint(String endpoint) {
        return endpoint != null && endpoint.contains(".objectstorage.");
    }

    Region getRegion(Credentials creds, String endpoint) {
        var regionStr = creds.get(Credentials.Attr.REGION);

        // Extract region from S3 endpoint if not provided
        if (StringUtil.isEmpty(regionStr) && !StringUtil.isEmpty(endpoint)) {
            var m = Pattern.compile(".*?s3-(.*?)\\..*").matcher(endpoint);
            if (m.matches()) {
                regionStr = m.group(1);
            }
        }

        // Extract region from OCI endpoint if not provided
        if (StringUtil.isEmpty(regionStr) && !StringUtil.isEmpty(endpoint)) {
            var m = Pattern.compile(".*?\\.objectstorage\\.(.*?)\\..*").matcher(endpoint);
            if (m.matches()) {
                regionStr = m.group(1);
            }
        }

        if (StringUtil.isEmpty(regionStr)) {
            regionStr = System.getProperty("aws.region");
        }

        if (StringUtil.isEmpty(regionStr)) {
            regionStr = System.getenv("AWS_REGION");
        }

        return StringUtil.isEmpty(regionStr) ? Region.US_EAST_1 : Region.of(regionStr);
    }
}

