package org.gorpipe.gor.auth.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import org.gorpipe.gor.auth.GorAuthInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class PlatformGorAuthCache {
    private final static Logger log = LoggerFactory.getLogger(PlatformGorAuthCache.class);

    private static final int CACHE_TIMEOUT_IN_SECONDS = 600;
    private static final int CACHE_STORE_TIME_LIMIT_IN_SECONDS = 10;

    Cache<String, GorAuthInfo> gorAuthInfoCache;

    public PlatformGorAuthCache() {
        this.gorAuthInfoCache = createGorAuthCache(CACHE_TIMEOUT_IN_SECONDS);
    }

    public void add(String key, GorAuthInfo gorAuthInfo, long expiration) {
        if (System.currentTimeMillis() + CACHE_STORE_TIME_LIMIT_IN_SECONDS * 1000 < expiration) {
            gorAuthInfoCache.put(key, gorAuthInfo);
        }
    }

    public GorAuthInfo get(String key, GorAuthInfo defaultGorAuthInfo) {
        GorAuthInfo authInfo = get(key);
        return authInfo != null ? authInfo : defaultGorAuthInfo;
    }

    public GorAuthInfo get(String key) {
        if (key == null) return null;
        GorAuthInfo gorAuthInfo = gorAuthInfoCache.getIfPresent(key);
        if (gorAuthInfo != null && gorAuthInfo.getExpiration() <= System.currentTimeMillis()) {
            gorAuthInfoCache.invalidate(key);
            gorAuthInfo = null;
        }
        return gorAuthInfo;
    }

    public Cache<String, GorAuthInfo> createGorAuthCache(long timeoutInSeconds) {
        RemovalListener<String, GorAuthInfo> removalNotifier;
        removalNotifier = notification -> {
            notification.getValue();
            log.debug("Removing gor auth info from cache, key: {}, cause: {}", notification.getKey(), notification.getCause());
        };

        return CacheBuilder.newBuilder().removalListener(removalNotifier)
                .expireAfterWrite(timeoutInSeconds, TimeUnit.SECONDS).build();
    }
}
