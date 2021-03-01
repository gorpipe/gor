/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import org.gorpipe.gor.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * S3GenomicIteratorFactory is a factory for GenomicIterators with data in a S3 instance. The credentials for S3 can
 * be configured in two ways: a) properties and b) AWS instance lookup
 * S3 endpoint can be configured using the property gor.s3.endpoint
 *
 * @version $Id$
 */
public class S3GenomicIteratorFactory {
    private static final Logger log = LoggerFactory.getLogger(S3GenomicIteratorFactory.class);
    final ClientConfiguration clientconfig = new ClientConfiguration();
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
                clientconfig.setProxyHost(proxy);
                clientconfig.setProxyPort(Integer.parseInt(port));
            }
            String poolSize = System.getProperty("gor.s3.conn.pool.size");
            if (poolSize != null && poolSize.trim().length() > 0) {
                clientconfig.setMaxConnections(Integer.parseInt(poolSize));
            }
            clientconfig.setConnectionTimeout(120 * 1000);
            clientconfig.setMaxErrorRetry(15);
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
        }
    }

    public Stream<String> newNorInstance(String file) throws IOException {
        AmazonS3 s3;
        try {
            final AWSCredentialsProviderChain gcCredentialsProvider;
            int w = file.indexOf("profile=");
            if (w == -1) {
                w = file.indexOf("aws_key=");
                if (w != -1) {
                    int v = file.indexOf('&', w);
                    final String key = file.substring(w + 8, v);
                    final String secret = file.substring(v + 12);

                    file = file.substring(0, w - 1);

                    final AWSCredentials awsc = new AWSCredentials() {
                        public String getAWSSecretKey() {
                            return secret;
                        }

                        public String getAWSAccessKeyId() {
                            return key;
                        }
                    };

                    ProfileCredentialsProvider pcp = new ProfileCredentialsProvider() {
                        @Override
                        public AWSCredentials getCredentials() {
                            return awsc;
                        }
                    };

                    gcCredentialsProvider = new AWSCredentialsProviderChain(pcp) {
                        @Override
                        public AWSCredentials getCredentials() {
                            return awsc;
                        }
                    };
                } else {
                    gcCredentialsProvider = new AWSCredentialsProviderChain(
                            new RdaAWSCredentialsProvider(), new InstanceProfileCredentialsProvider()) {
                        @Override
                        public AWSCredentials getCredentials() {
                            try {
                                return super.getCredentials();
                            } catch (AmazonClientException ace) {
                                log.warn(ace.getMessage(), ace);
                                // Do nothing
                            }
                            log.warn("No credentials available; falling back to anonymous access");
                            return null;
                        }
                    };
                }
            } else {
                int k = file.indexOf('&', w);
                if (k == -1) k = file.length();
                String profile = file.substring(Math.min(k, w + 8), k);

                int m = file.indexOf("aws_key=");
                if (m != -1) {
                    int v = file.indexOf('&', m);
                    final String key = file.substring(m + 8, v);
                    final String secret = file.substring(v + 12);
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

                file = file.substring(0, w - 1);

                ProfileCredentialsProvider pcp;
                if (profile.length() > 0) pcp = new ProfileCredentialsProvider(profile);
                else pcp = new ProfileCredentialsProvider();

                gcCredentialsProvider = new AWSCredentialsProviderChain(
                        pcp,
                        new RdaAWSCredentialsProvider(), new InstanceProfileCredentialsProvider()) {
                    @Override
                    public AWSCredentials getCredentials() {
                        try {
                            return super.getCredentials();
                        } catch (AmazonClientException ace) {
                            log.warn(ace.getMessage(), ace);
                            // Do nothing
                        }
                        log.warn("No credentials available; falling back to anonymous access");
                        return null;
                    }
                };
                gcCredentialsProvider.setReuseLastProvider(false);
            }

            s3 = new AmazonS3Client(gcCredentialsProvider, clientconfig);
            if (endpoint != null) {
                s3.setEndpoint(endpoint);
            }
            log.debug("S3 endpoint is %s", Util.nvl(endpoint, "default"));
        } catch (Exception ex) {
            s3 = null;
            log.warn("Could not initialize connection to Amazon S3", ex);
        }

        SamReaderFactory srf = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT);

        final long start = System.currentTimeMillis();
        if (s3 == null) {
            throw new IOException("Not connected to Amazon S3, check logs to investigate what is wrong");
        }
        final Stream<String> stream;
        final int idx = file.indexOf('/');
        if (idx < 0) {

            ObjectListing listing = s3.listObjects(file, "");
            List<S3ObjectSummary> summaries = listing.getObjectSummaries();

            while (listing.isTruncated()) {
                listing = s3.listNextBatchOfObjects(listing);
                summaries.addAll(listing.getObjectSummaries());
            }
            stream = Stream.concat(Stream.of(new String[]{"#Filename\tFilesize\tFilepath\tFileowner\tFilemodified"}), summaries.stream().map(x -> x.getKey() + "\t" + x.getSize() + "\ts3://" + x.getBucketName() + "/" + x.getKey() + "\t" + x.getOwner().getDisplayName() + "\t" + x.getLastModified()));

            //throw new GorException("Error: Invalid S3 path", "The path " + file + " is not a valid S3 reference.");
        } else stream = null;
        return stream;
    }

    RdaAWSCredentialsProvider rdaCredProvider;

    private AmazonS3 getS3(Map<String, String> params) {
        AmazonS3 s3;
        if (rdaCredProvider == null) rdaCredProvider = new RdaAWSCredentialsProvider();
        try {
            final AWSCredentialsProviderChain gcCredentialsProvider;
            if (!params.containsKey("profile")) {
                if (params.containsKey("aws_key")) {
                    final String key = params.get("aws_key");
                    final String secret = params.get("aws_secret");

                    final AWSCredentials awsc = new AWSCredentials() {
                        public String getAWSSecretKey() {
                            return secret;
                        }

                        public String getAWSAccessKeyId() {
                            return key;
                        }
                    };

                    ProfileCredentialsProvider pcp = new ProfileCredentialsProvider() {
                        @Override
                        public AWSCredentials getCredentials() {
                            return awsc;
                        }
                    };

                    gcCredentialsProvider = new AWSCredentialsProviderChain(pcp) {
                        @Override
                        public AWSCredentials getCredentials() {
                            return awsc;
                        }
                    };
                } else {
                    gcCredentialsProvider = new AWSCredentialsProviderChain(rdaCredProvider, new InstanceProfileCredentialsProvider()) {
                        @Override
                        public AWSCredentials getCredentials() {
                            try {
                                return super.getCredentials();
                            } catch (AmazonClientException ace) {
                                log.warn(ace.getMessage(), ace);
                                // Do nothing
                            }
                            log.warn("No credentials available; falling back to anonymous access");
                            return null;
                        }
                    };
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
                if (profile.length() > 0) pcp = new ProfileCredentialsProvider(profile);
                else pcp = new ProfileCredentialsProvider();

                gcCredentialsProvider = new AWSCredentialsProviderChain(
                        pcp,
                        rdaCredProvider, new InstanceProfileCredentialsProvider()) {
                    @Override
                    public AWSCredentials getCredentials() {
                        try {
                            return super.getCredentials();
                        } catch (AmazonClientException ace) {
                            log.warn(ace.getMessage(), ace);
                            // Do nothing
                        }
                        log.warn("No credentials available; falling back to anonymous access");
                        return null;
                    }
                };
                gcCredentialsProvider.setReuseLastProvider(false);
            }

            s3 = new AmazonS3Client(gcCredentialsProvider, clientconfig);
            if (endpoint != null) {
                s3.setEndpoint(endpoint);
            }
            log.debug("S3 endpoint is %s", Util.nvl(endpoint, "default"));
        } catch (Exception ex) {
            s3 = null;
            log.warn("Could not initialize connection to Amazon S3", ex);
        }

        return s3;
    }

}

/**
 * AWS Credential Provider implementation that reads account settings from RDA provided property file
 */
class RdaAWSCredentialsProvider implements AWSCredentialsProvider {
    private static final Logger log = LoggerFactory.getLogger(RdaAWSCredentialsProvider.class);
    private BasicAWSCredentials credentials;

    public RdaAWSCredentialsProvider() {
        refresh();
    }

    public AWSCredentials getCredentials() {
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
            credentials = new BasicAWSCredentials(accessKey, secretKey);
        }
    }
}
