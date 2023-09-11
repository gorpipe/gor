package org.gorpipe.gor.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.security.cred.CsaApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
            Map<String, Object> appSessionMap;
            List userroles;

            try {
                appSessionMap = csaApiService.getAppSession(sessionKey);
            } catch (IOException e) {
                throw new GorSystemException("Unable to get app_session from CSA API", e);
            }

            validateAppSession(sessionKey, appSessionMap);

            int organizationId = getOrganizationId(appSessionMap);

            Map<String, Object> projectMap = getSubMap(appSessionMap, "project");
            Map<String, Object> userMap = getSubMap(appSessionMap, "user");

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

    private int getOrganizationId(Map<String, Object> appSessionMap) {
        return (Integer) appSessionMap.get("organization_id");
    }

    private Map<String, Object> getSubMap(Map<String, Object> appSessionMap, String key) {
        return (Map<String, Object>) appSessionMap.get(key);
    }

    private String getUserId(Map<String, Object> userMap) {
        String userIdString = (String) userMap.get("key");
        return userIdString.replace("DCUS", "");
    }

    private static String getFirstUserRole(List userroles) {
        return userroles.isEmpty() ? "" : ((Map<String, String>) userroles.get(0)).get("role");
    }

    private String getUsername(Map<String, Object> userMap) {
        return (String) userMap.get("email");
    }


    private int getProjectId(Map<String, Object> projectMap) {
        return (Integer) projectMap.get("id");
    }

    private String getProject(Map<String, Object> projectMap) {
        return (String) projectMap.get("key");
    }

    private Cache<String, GorAuthInfo> createGorAuthCacheCache(long timeoutInSeconds) {

        RemovalListener<String, GorAuthInfo> removalNotifier;
        removalNotifier = (k,v,c) -> {
            log.debug("Removing gor auth info from cache, key: {}, cause: {}", k, c);
        };

        return Caffeine.newBuilder().removalListener(removalNotifier)
                .expireAfterAccess(timeoutInSeconds, TimeUnit.SECONDS).build();
    }

    private void validateAppSession(String sessionKey, Map<String, Object> appSessionMap) {
        if (!sessionKey.equals(appSessionMap.get("key"))) {
            throw new GorSystemException("App Session details from CSA API are for a different key than requested for", null);
        }
    }
}
