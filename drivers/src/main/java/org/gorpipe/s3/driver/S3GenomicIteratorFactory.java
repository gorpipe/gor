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

import org.gorpipe.gor.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * S3GenomicIteratorFactory is a factory for GenomicIterators with data in a S3 instance. The credentials for S3 can
 * be configured in two ways: a) properties and b) AWS instance lookup
 * S3 endpoint can be configured using the property gor.s3.endpoint
 *
 * @version $Id$
 */
public class S3GenomicIteratorFactory {
    private static final Logger log = LoggerFactory.getLogger(S3GenomicIteratorFactory.class);
    final ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
    final ProxyConfiguration.Builder proxyConfig = ProxyConfiguration.builder();
    final String endpoint = System.getProperty("gor.s3.endpoint");

    /**
     * Construct S3GenomicIteratorFactory
     */
    public S3GenomicIteratorFactory() {
        //this.profile = prof;
        // Get configured account and service location

        // Detect proxy settings from java system proxies
        try {
            final String proxy = System.getProperty("http.proxyHost");
            final String port = System.getProperty("http.proxyPort");
            if (proxy != null && port != null) {
                log.info("RDA AWS connection - Proxy set to {}:{}", proxy, port);
                proxyConfig.endpoint(URI.create(proxy + ":" + port));
            }

            httpClientBuilder.proxyConfiguration(proxyConfig.build());
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
        }
        String poolSize = System.getProperty("gor.s3.conn.pool.size");
        if (poolSize != null && poolSize.trim().length() > 0) {
            httpClientBuilder.maxConnections(Integer.parseInt(poolSize));
        }
        httpClientBuilder.connectionTimeout(Duration.ofSeconds(120));
    }

    RdaAWSCredentialsProvider rdaCredProvider;

    private S3Client getS3(Map<String, String> params) {
        S3ClientBuilder s3;
        if (rdaCredProvider == null) {
            rdaCredProvider = new RdaAWSCredentialsProvider();
        }

        try {
            final AwsCredentialsProvider gcCredentialsProvider;
            if (!params.containsKey("profile")) {
                if (params.containsKey("aws_key")) {
                    final String key = params.get("aws_key");
                    final String secret = params.get("aws_secret");
                    gcCredentialsProvider = StaticCredentialsProvider.create(
                            AwsBasicCredentials.builder().accessKeyId(key).secretAccessKey(secret).build());
                } else {
                    gcCredentialsProvider = DefaultCredentialsProvider.create();
                }
            } else {
                String profile = params.get("profile");

                if (params.containsKey("aws_key")) {
                    final String key = params.get("aws_key");
                    final String secret = params.get("aws_secret");
                    String profilebrack = "[" + profile + "]";

                    String userhome = System.getProperty("user.home");
                    Path p = Paths.get(userhome);
                    Path rp = p.resolve(".aws");
                    Path awsdir = Files.createDirectories(rp);
                    Path cred = awsdir.resolve("credentials");
                    if (Files.exists(cred)) {
                        String cont = new String(Files.readAllBytes(cred));
                        if (!cont.contains(profilebrack)) {
                            Files.write(cred, (profilebrack + "\naws_access_key_id = " + key + "\naws_secret_access_key = " + secret + "\n").getBytes(), StandardOpenOption.APPEND);
                        }
                    } else {
                        Files.write(cred, (profilebrack + "\naws_access_key_id = " + key + "\naws_secret_access_key = " + secret + "\n").getBytes(), StandardOpenOption.CREATE_NEW);
                    }
                }

                ProfileCredentialsProvider pcp;
                if (profile.length() > 0) {
                    pcp = ProfileCredentialsProvider.create(profile);
                } else {
                    pcp = ProfileCredentialsProvider.create();
                }

                var credentialProviderChain = AwsCredentialsProviderChain.of(
                        pcp,
                        rdaCredProvider,
                        InstanceProfileCredentialsProvider.create());

                gcCredentialsProvider = () -> {
                    try {
                        return credentialProviderChain.resolveCredentials();
                    } catch (Exception ace) {
                        log.warn(ace.getMessage(), ace);
                        // Do nothing
                    }
                    log.warn("No credentials available; falling back to anonymous access");
                    return null;
                };
            }

            s3 = S3Client.builder()
                    .credentialsProvider(gcCredentialsProvider)
                    .httpClientBuilder(httpClientBuilder)
                    .overrideConfiguration(o -> o.retryStrategy(s -> s.maxAttempts(10)));
            if (endpoint != null) {
                s3.endpointOverride(URI.create(endpoint));
            }
            log.debug("S3 endpoint is %s", Util.nvl(endpoint, "default"));
        } catch (Exception ex) {
            s3 = null;
            log.warn("Could not initialize connection to Amazon S3", ex);
        }

        return s3.build();
    }
}

/**
 * AWS Credential Provider implementation that reads account settings from RDA provided property file
 */
class RdaAWSCredentialsProvider implements AwsCredentialsProvider {
    private static final Logger log = LoggerFactory.getLogger(RdaAWSCredentialsProvider.class);
    private AwsCredentials credentials;

    public RdaAWSCredentialsProvider() {
        refresh();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return credentials;
    }

    public void refresh() {
        // Get configured account and service location
        String accessKey, secretKey;
        final String keystore = System.getProperty("gor.s3.keystore");
        if (keystore != null) {
            log.debug("Loading AWS credentials from RDA keystore %s", keystore);
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(keystore)) {
                p.load(in);
                accessKey = p.getProperty("access.key");
                secretKey = p.getProperty("secret.key");
            } catch (Exception ex) {
                log.warn("Error loading RDA AWS account configuration from " + keystore, ex);
                accessKey = secretKey = null;
            }
        } else {
            accessKey = System.getProperty("gor.s3.access.key");
            secretKey = System.getProperty("gor.s3.secret.key");
        }

        if (accessKey == null || secretKey == null) {
            log.debug("RDA AWS configuration is not provided for either accessKey or secretKey");
        } else {
            log.info("Using RDA AWS configuration");
            credentials = AwsBasicCredentials.create(accessKey, secretKey);
        }
    }
}
