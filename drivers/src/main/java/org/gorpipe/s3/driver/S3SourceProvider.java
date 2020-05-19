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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.StreamSourceProvider;
import org.gorpipe.gor.driver.utils.CredentialClientCache;
import org.gorpipe.security.cred.BundledCredentials;
import org.gorpipe.security.cred.Credentials;

import java.io.IOException;
import java.util.Set;

@Singleton
public class S3SourceProvider extends StreamSourceProvider {
    private final CredentialClientCache<S3Client> clientCache = new CredentialClientCache<>(S3SourceType.S3.getName(), this::createClient);
    private final S3Configuration s3Config;

    @Inject
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
        S3Url url = S3Url.parse(sourceReference.getUrl());
        S3Client client = getClient(sourceReference.getSecurityContext(), url.getLookupKey());
        return new S3Source(client, sourceReference);
    }

    private S3Client getClient(String securityContext, String bucket) throws IOException {
        BundledCredentials creds = BundledCredentials.fromSecurityContext(securityContext);
        return clientCache.getClient(creds, bucket);
    }

    private S3Client createClient(Credentials cred) {
        ClientConfiguration clientconfig = new ClientConfiguration();
        clientconfig.setConnectionTimeout((int) s3Config.connectionTimeout().toMillis());
        clientconfig.setMaxErrorRetry(s3Config.connectionRetries());
        clientconfig.setMaxConnections(s3Config.connectionPoolSize());
        log.debug("Creating S3Client for {}", cred);
        if (cred == null || cred.isNull()) {
            return new S3Client(new DefaultAWSCredentialsProviderChain(), clientconfig);
        } else {
            AWSCredentials awsCredentials;

            if (cred.containsKey(Credentials.Attr.SESSION_TOKEN)) {
                log.debug("Creating temporary session credentials for {}", cred.getLookupKey());
                awsCredentials = new BasicSessionCredentials(
                        cred.get(Credentials.Attr.KEY),
                        cred.get(Credentials.Attr.SECRET),
                        cred.get(Credentials.Attr.SESSION_TOKEN)
                );
            } else {
                awsCredentials = new BasicAWSCredentials(
                        cred.get(Credentials.Attr.KEY),
                        cred.get(Credentials.Attr.SECRET)
                );
            }

            S3Client client = new S3Client(awsCredentials, clientconfig);

            String endpoint = cred.get(Credentials.Attr.API_ENDPOINT);
            if (endpoint != null) {
                client.setEndpoint(endpoint);
            }
            return client;
        }
    }

}
