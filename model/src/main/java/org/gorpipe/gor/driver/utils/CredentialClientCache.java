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

package org.gorpipe.gor.driver.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.base.security.CredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Implements common caching pattern to resolve lookup from credentials to client objects accessing remote resources
 * <p>
 * Created by villi on 17/08/16.
 */
public class CredentialClientCache<ClientClass> {
    private final static Logger log = LoggerFactory.getLogger(CredentialClientCache.class);

    // Cache credential to client mapping
    private final Cache<Credentials, ClientClass> credToClient = createCache();

    // Cache bundled credentials to lookup cache.  The lookup cache caches lookup_key (e.g. project/bucker) to credentials for that bundle.
    private final Cache<CredentialsProvider, Cache<Optional<String>, Optional<Credentials>>> bundleToLookupCache = createCache();

    private final String service;
    private final Function<Credentials, ClientClass> createClient;

    /**
     * Create new cache
     *
     * @param service      Service string (s3/dx/...)
     * @param createClient Method used to create a new client from the given credentials
     */
    public CredentialClientCache(String service, Function<Credentials, ClientClass> createClient) {
        this.service = service;
        this.createClient = createClient;
    }

    public ClientClass getClient(CredentialsProvider creds, String lookup) throws IOException {
        log.debug("Looking up client for lookup key: {}", lookup);
        try {
            Cache<Optional<String>, Optional<Credentials>> lookupCache = bundleToLookupCache.get(creds, this::createCache);
            Optional<String> key = Optional.ofNullable(lookup);
            Credentials cred = lookupCache.get(key, () -> computeCred(creds, lookup)).orElse(Credentials.NULL);
            if (cred != null && !isValid(cred)) {
                lookupCache.invalidate(key);
                cred = lookupCache.get(key, () -> computeCred(creds, lookup)).orElse(Credentials.NULL);
            }

            final Credentials finalCred = cred;
            return credToClient.get(cred, () -> createClient.apply(finalCred));
        } catch (ExecutionException e) {
            throw convertException(e);
        }
    }

    private <K1, V1> Cache<K1, V1> createCache() {
        return CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterAccess(1, TimeUnit.HOURS).build();
    }

    private Optional<Credentials> computeCred(CredentialsProvider creds, String lookup) {
        log.debug("Computing credentials for {}", lookup);
        try {
            for (Credentials cred : creds.getCredentials(service, lookup)) {
                if (isValid(cred)) {
                    log.debug("Found valid credentials: {}", cred);
                    return Optional.ofNullable(cred);
                }
            }
            log.debug("No credentials available for {}", lookup);
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isValid(Credentials cred) {
        return cred.isValidFor(1, ChronoUnit.HOURS);
    }

    IOException convertException(ExecutionException e) {
        if (e.getCause() instanceof IOException) {
            return (IOException) e.getCause();
        }
        throw new RuntimeException(e);
    }

}
