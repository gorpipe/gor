package org.gorpipe.security.cred;

import com.google.api.client.util.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.gorpipe.gor.auth.AuthConfig;
import org.gorpipe.gor.auth.GorAuthInfo;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.base.security.CredentialsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Call csa auth api to get credentials and auth details stored in csa system.
 */
public class CsaCredentialService extends CsaBaseService {
    private final static Logger log = LoggerFactory.getLogger(CsaCredentialService.class);
    private final AppSessionUtility appSessionUtility;

    private CredentialsParser parser;
    private Cache<String, BundledCredentials> credentialsCache;    // Key is "<projectName>:<userName>"

    /**
     * Create csa credentials service object
     *
     * @param config configuration
     */
    public CsaCredentialService(CsaAuthConfiguration config, AuthConfig authConfig, CredentialsParser parser, AppSessionUtility appSessionUtility) {
        super(config, authConfig);
        this.parser = parser;
        this.appSessionUtility = appSessionUtility;
        credentialsCache = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(1, TimeUnit.MINUTES).build();
    }

    public BundledCredentials getCredentials(GorAuthInfo gorAuthInfo) {
        return getCredentialsBundleFromCache(gorAuthInfo.getProject(), gorAuthInfo.getUsername(), gorAuthInfo.getUserId());
    }

    public BundledCredentials getCredentials(String appSession) {
        GorAuthInfo sessionContext = appSessionUtility.getSessionContext(appSession);
        return getCredentialsBundleFromCache(sessionContext.getProject(), sessionContext.getUsername(), sessionContext.getUserId());
    }

    private BundledCredentials getCredentialsBundleFromCache(String projectName, String userName, String userId) {
        String key;
        if (Strings.isNullOrEmpty(userId)) {
            key = projectName;
        } else {
            key = projectName + ":" + userName;
        }
        try {
            return credentialsCache.get(key, () -> getCredentialsBundle(projectName, userId));
        } catch (UncheckedExecutionException | ExecutionException e) {
            throw new GorSystemException("Error getting credentials for user: " + userName + " in project: " + projectName, e.getCause());
        }
    }

    private BundledCredentials getCredentialsBundle(String projectName, String userId) throws IOException {
        return getCredentialsBundle(projectName, userId, null, null);
    }

    public BundledCredentials getCredentialsBundle(String projectName, String userId, String service, String lookupKey) throws IOException {
        log.debug("get credentials for project: {}, user {}", projectName, userId);
        if (!isConfigured()) {
            log.info("No configuration - returning empty credentials list");
            return BundledCredentials.emptyCredentials();
        }
        initAuth();
        String parms = String.format("find[project_key]=%s", projectName);
        if (!Strings.isNullOrEmpty(userId)) {
            parms = parms + String.format("&find[user_id]=%s", userId);
        }
        if (service != null) {
            parms = parms + String.format("&find[service]=%s", service);
        }
        if (lookupKey != null) {
            parms = parms + String.format("&find[lookup_key]=%s", lookupKey);
        }

        Map<String, Object> result = null;
        try {
            result = jsonGet("auth/v1/credentials.json?" + parms);
        } catch (IOException ioe) {
            // Retry once with new Auth.
            result = initializeAndRetry("auth/v1/credentials.json?" + parms);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("credentials");
        BundledCredentials.Builder builder = new BundledCredentials.Builder();
        if (list != null) {
            List<Credentials> credList = parser.parseFromJson(list);
            Collections.sort(credList, (Credentials c1, Credentials c2) -> c2.getOwnerType().compareTo(c1.getOwnerType()));
            for (Credentials cred : credList) {
                if (cred.getLookupKey() != null) {
                    builder.addCredentials(cred);
                } else {
                    builder.addDefaultCredentials(cred);
                }
            }
        }
        return builder.build();
    }

}
