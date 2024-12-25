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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.crt.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@AutoService(SourceProvider.class)
public class S3SourceProvider extends StreamSourceProvider {
    private final Cache<String, S3Client> clientCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    private final CredentialClientCache<S3Client> clientCredCache = new CredentialClientCache<>(S3SourceType.S3.getName(), this::createClient);
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
        return new S3Source(client, sourceReference);
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
        var builder = S3Client.builder();

        var endpoint = getEndpoint(cred);
        if (!StringUtil.isEmpty(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        builder.region(getRegion(cred, endpoint));

        builder.credentialsProvider(getCredentialsProvider(cred));

        AwsCrtHttpClient.Builder httpClientBuilder = AwsCrtHttpClient.builder()
                .connectionTimeout(s3Config.connectionTimeout())  // Default was 2s
                //.connectionMaxIdleTime(Duration.ofMillis(s3Config.socketTimeout().toMillis() * 2))  // Default was 60s
                .maxConcurrency(s3Config.connectionPoolSize())  // Default was 50
                .tcpKeepAliveConfiguration(b -> b
                        .keepAliveInterval(Duration.ofMillis(s3Config.socketTimeout().toMillis()/2))
                        .keepAliveTimeout(s3Config.connectionTimeout()))
                //.connectionHealthConfiguration()
                //.readBufferSizeInBytes()

                ;

        // Note: See defaults values at https://github.com/aws/aws-sdk-java-v2/blob/master/http-client-spi/src/main/java/software/amazon/awssdk/http/SdkHttpConfigurationOption.java

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



        var metricsPub = new PrometheusMetricPublisher();
        builder.overrideConfiguration(c -> c.addMetricPublisher(metricsPub));

        builder.overrideConfiguration(o -> o.retryStrategy(b -> b.maxAttempts(s3Config.connectionRetries()))
                //.apiCallAttemptTimeout(Duration.ofMillis(s3Config.socketTimeout().toMillis()/(s3Config.connectionRetries() * 3)))
                //.apiCallTimeout(s3Config.socketTimeout())
        );

        // OCI compat layer needs path style access.
        builder.forcePathStyle(true);

        // Cross region access.  One use it to create client with emtpy creds and apply creds/region/endpoint later.
        //builder.crossRegionAccessEnabled(true);

        return builder.build();
    }

    AwsCredentialsProvider getCredentialsProvider(Credentials cred) {
        if (cred == null || cred.isNull()) {
            log.info("CredentialsProvider: DefaultCredentialsProvider for null creds");
            return DefaultCredentialsProvider.create();
        } else {
            var awsKey = cred.getOrDefault(Credentials.Attr.KEY, "");
            var awsSecret = cred.getOrDefault(Credentials.Attr.SECRET, "");
            var sessionToken = cred.getOrDefault(Credentials.Attr.SESSION_TOKEN, "");

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
                log.info(String.format("CredentialsProvider: DefaultCredentialsProvider for %s:%s", cred.getService(), cred.getLookupKey()));
                return DefaultCredentialsProvider.create();
            }
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

        return endpoint;
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

