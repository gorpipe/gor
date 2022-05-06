package org.gorpipe.gor.auth.utils;

import org.gorpipe.gor.auth.GorAuthInfo;

public class PlainGorAuthCache extends PlatformGorAuthCache {

    public PlainGorAuthCache() {
        super();
    }

    public void add(String key, GorAuthInfo gorAuthInfo) {
        gorAuthInfoCache.put(key, gorAuthInfo);
    }

    @Override
    public GorAuthInfo get(String key) {
        if (key == null) return null;
        return gorAuthInfoCache.getIfPresent(key);
    }
}
