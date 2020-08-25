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

package gorsat.process;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import org.gorpipe.base.config.converters.DurationConverter;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.session.GorSessionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Class that stores the session cache on a requestId key. By default the cache is kept alive for 3600 seconds. When a
 * new session is created this class should be used to acquire the cache object.
 */
public class GorSessionCacheManager {
    private static final Logger log = LoggerFactory.getLogger(GorSessionCacheManager.class);
    private static final Cache<String, GorSessionCache> gorCache = createCache();

    private GorSessionCacheManager() {}

    private static Cache<String, GorSessionCache> createCache() {
        DurationConverter converter = new DurationConverter();
        RemovalListener<String, GorSessionCache> removalNotifier;
        removalNotifier = notification -> log.info("Removing from gor session cache, key: {}, cause: {}.  Remaining entries: {}",
                notification.getKey(), notification.getCause(), gorCache.size());

        CacheBuilder builder =  CacheBuilder.newBuilder()
                .removalListener(removalNotifier)
                .weakValues();
        return builder.build();
    }

    public static synchronized GorSessionCache getCache(String requestId) {
        try {
            log.info("Acquiring gor session cache for request id: {}", requestId);
            return gorCache.get(requestId, () -> createCache(requestId));
        } catch (ExecutionException e) {
            throw new GorSystemException("Failed to create got session cache for request id: " + requestId, e);
        }
    }

    public static void invalidateCache(String requestId) {
        gorCache.invalidate(requestId);
    }

    private static GorSessionCache createCache(String requestId) {
        log.info("Creating gor session cache for request id: {}", requestId);
        return new GorSessionCache();
    }
}
