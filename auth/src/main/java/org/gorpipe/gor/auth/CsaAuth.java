package org.gorpipe.gor.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.security.cred.CsaApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CsaAuth extends GorAuth {

    private static final Logger log = LoggerFactory.getLogger(CsaAuth.class);

    private Cache<String, GorAuthInfo> gorAuthInfoCache;
    private static final int CACHE_TIMEOUT_IN_SECONDS = 600;

    public CsaAuth(AuthConfig config, CsaApiService csaApiService) {
        super(config, csaApiService);
        gorAuthInfoCache = createGorAuthCacheCache(CACHE_TIMEOUT_IN_SECONDS);
    }

    @Override
    public GorAuthInfo getGorAuthInfo(String sessionKey) {
        GorAuthInfo gorAuthInfo = gorAuthInfoCache.getIfPresent(sessionKey);
        if (gorAuthInfo == null) {
            LinkedHashMap<String, Object> appSessionMap;
            List userroles;

            try {
                appSessionMap = csaApiService.getAppSession(sessionKey);
            } catch (IOException e) {
                throw new GorSystemException("Unable to get app_session from CSA API", e);
            }

            validateAppSession(sessionKey, appSessionMap);

            int organizationId = getOrganizationId(appSessionMap);

            LinkedHashMap<String, Object> projectMap = getSubMap(appSessionMap, "project");
            LinkedHashMap<String, Object> userMap = getSubMap(appSessionMap, "user");

            String userId = getUserId(userMap);
            String username = getUsername(userMap);

            int projectId = getProjectId(projectMap);
            String project = getProject(projectMap);

            try {
                userroles = csaApiService.getUserRoleList(project, username);
            } catch (IOException e) {
                throw new GorSystemException("Unable to get user_role from CSA API", e);
            }
            String userrole = getFirstUserRole(userroles);

            gorAuthInfo = new GeneralAuthInfo(projectId, project, username, userId, Arrays.asList(userrole), organizationId, 0);
            gorAuthInfoCache.put(sessionKey, gorAuthInfo);
        }
        return gorAuthInfo;
    }

    private int getOrganizationId(LinkedHashMap<String, Object> appSessionMap) {
        return (Integer) appSessionMap.get("organization_id");
    }

    private LinkedHashMap<String, Object> getSubMap(LinkedHashMap<String, Object> appSessionMap, String key) {
        return (LinkedHashMap<String, Object>) appSessionMap.get(key);
    }

    private String getUserId(LinkedHashMap<String, Object> userMap) {
        String userIdString = (String) userMap.get("key");
        return userIdString.replace("DCUS", "");
    }

    private static String getFirstUserRole(List userroles) {
        return userroles.isEmpty() ? "" : (String) ((LinkedHashMap) userroles.get(0)).get("role");
    }

    private String getUsername(LinkedHashMap<String, Object> userMap) {
        return (String) userMap.get("email");
    }


    private int getProjectId(LinkedHashMap<String, Object> projectMap) {
        return (Integer) projectMap.get("id");
    }

    private String getProject(LinkedHashMap<String, Object> projectMap) {
        return (String) projectMap.get("key");
    }

    private Cache<String, GorAuthInfo> createGorAuthCacheCache(long timeoutInSeconds) {

        RemovalListener<String, GorAuthInfo> removalNotifier;
        removalNotifier = notification -> {
            notification.getValue();
            log.debug("Removing gor auth info from cache, key: {}, cause: {}", notification.getKey(), notification.getCause());
        };

        return CacheBuilder.newBuilder().removalListener(removalNotifier)
                .expireAfterAccess(timeoutInSeconds, TimeUnit.SECONDS).build();
    }

    private void validateAppSession(String sessionKey, LinkedHashMap<String, Object> appSessionMap) {
        if (!sessionKey.equals(appSessionMap.get("key"))) {
            throw new GorSystemException("App Session details from CSA API are for a different key than requested for", null);
        }
    }
}
