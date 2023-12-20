package org.gorpipe.gor.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.gorpipe.gor.auth.utils.OAuthHandler;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.util.db.Db;
import org.gorpipe.security.cred.CsaApiService;
import org.gorpipe.security.cred.CsaSecurityModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Note on NONE:  The NONE security policy is ignored unless the it is the only security policy defined.
 */

public class GorAuthFactory {

    private final static Logger log = LoggerFactory.getLogger(GorAuthFactory.class);

    private final Cache<String, GorAuth> authCache;
    private final AuthConfig config;
    private List<String> policesCache;
    private CsaApiService csaApiService;
    private OAuthHandler oAuthHandler;

    public GorAuthFactory(AuthConfig config) {
        this(config, null);
    }

    public GorAuthFactory(AuthConfig config, CsaApiService csaApiService) {
        this.config = config;
        this.authCache = CacheBuilder.newBuilder().concurrencyLevel(4)
                .expireAfterWrite(2, TimeUnit.HOURS).build();
        this.csaApiService = csaApiService;
        if (!Strings.isNullOrEmpty(config.publicAuthorizationKey())) {
            this.oAuthHandler = new OAuthHandler(config.publicAuthorizationKey());
        }
    }

    /**
     * Get and validate policies.
     *
     * Now we support two notations one with the ';' as splitter and we need to use that notation while we have to
     * support mapping all properties to java properties, and the future ',' splitter that we map directly into
     * string array, but is not compatible with java properties.
     *
     * @return list of valid policies (of at lenght least 1).
     * @throws GorSystemException if security policy is not set.
     */
    synchronized public List<String> getAndValidatePolices() {
        if (policesCache == null) {
            final String[] policyConfigRead = config.securityPolicies();
            if (policyConfigRead == null) {
                throw new GorSystemException("Error: Security policy config must be set.", null);
            }
            final String[] policyConfig = policyConfigRead.length == 1 ? policyConfigRead[0].split(";") : policyConfigRead;

            policesCache = Arrays.stream(policyConfig).map(String::trim).map(p -> p.replaceAll("^[\"']|[\"']$", "")).filter(p -> !p.isEmpty()).collect(Collectors.toList());
            if (policesCache.isEmpty()) {
                throw new GorSystemException("Error: Security policy must not be empty.", null);
            }
            log.info("Valid security policies: {} (from {})", policesCache, policyConfigRead);
        }
        return policesCache;
    }

    public String getUpdateAuthInfoPolicy() {
        return config.updateAuthInfoPolicy();
    }

    public boolean getUserRolesFromToken() {
        return config.userRolesFromToken();
    }


    /**
     * Check if NONE is the only security policy defined.
     *
     * @return if NONE is the only security policy defined.
     */
    public boolean isNoneSecurityPolicy() {
        List<String> polices = getAndValidatePolices();
        return polices.size() == 1 && polices.get(0).equals("NONE");
    }

    /**
     * Get the GorAuth appropriate for this session key.
     *
     * @param sessionKey the session key (null, empty and NO_SESSION will all map to the same gorAuth).
     * @return GorAuth appropriate for the &lt;sessionKey&gt;
     */
    public GorAuth getGorAuth(String sessionKey) {
        try {
            final String cacheSessionKey = Strings.isNullOrEmpty(sessionKey) ? "NO_SESSION" : sessionKey;
            return authCache.get(cacheSessionKey, () -> getGorAuthFromPolicy(getPolicyFromSessionKey(cacheSessionKey)));
        }  catch (UncheckedExecutionException | ExecutionException e) {
            throw new GorSystemException("Error getting gorauth from sessionKey!", e.getCause());
        }
    }

    public GorAuthInfo getGorAuthInfo(String sessionKey) {
        return getGorAuth(sessionKey).getGorAuthInfo(sessionKey);
    }

    public GorAuthInfo getGorAuthInfo(String project, JsonWebToken jwt) {
        return getGorAuth(jwt.getRawToken()).getGorAuthInfo(project, jwt);
    }

    public void closeAll() {
        authCache.asMap().values().stream().distinct().forEach(GorAuth::close);

        authCache.invalidateAll();
    }

    private String getPolicyFromSessionKey(String sessionKey) {
        List<String> validPolices = getAndValidatePolices();

        String candPolicy;
        if (validPolices.size() == 1) {
            candPolicy = validPolices.get(0);
        } else {
            candPolicy = inferPolicyFromSessionKey(sessionKey);
        }

        // Validate that we can use the candPolicy.
        if (Strings.isNullOrEmpty(candPolicy) || !validPolices.contains(candPolicy)) {
            throw new GorSystemException(
                    "Error:  Session key (" + sessionKey + ") contains invalid security policy (" + candPolicy + ").", null);
        }

        return candPolicy;
    }

    private GorAuth getGorAuthFromPolicy(String policy) {
        // TODO:  Remove NONE is just a special case of plain?
        if (SecurityPolicy.NONE.toString().equalsIgnoreCase(policy)) {
            return getNoAuth();
        } else if (SecurityPolicy.CSA.toString().equalsIgnoreCase(policy)) {
            if (csaApiService == null) {
                csaApiService = CsaSecurityModule.get().apiService();
            }
            return new CsaAuth(config, csaApiService);
        } else if (SecurityPolicy.PLATFORM.toString().equalsIgnoreCase(policy)) {
            if (oAuthHandler == null) {
                throw new GorSystemException("Error: OAuthHandler not initialized for setting up PlatformAuth", null);
            }
            return new PlatformAuth(config, csaApiService, oAuthHandler);
        } else if (SecurityPolicy.JWT.toString().equalsIgnoreCase(policy)) {
            return new PlatformJWTAuth(config, csaApiService);
        } else if (SecurityPolicy.PLAIN.toString().equalsIgnoreCase(policy)) {
            return new PlainAuth(config, csaApiService);
        } else {
            throw new GorSystemException("Error: Unknown security policy " + policy, null);
        }
    }

    private String inferPolicyFromSessionKey(String sessionKey) {
        String policy;

        if (sessionKey == null || sessionKey.isEmpty() || "NO_SESSION".equals(sessionKey)) {
            policy = SecurityPolicy.PLAIN.toString();
        } else {
            Map<String, Object> sessionMap;

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                sessionMap = objectMapper.readValue(sessionKey.getBytes(), new TypeReference<HashMap<String,Object>>(){});
                if (sessionMap.containsKey("security-policy")) {
                    // Covers PLATFORM, PLAIN
                    policy = sessionMap.get("security-policy").toString();
                } else {
                    policy = SecurityPolicy.PLAIN.toString();
                }
            } catch (IOException e) {
                if (sessionKey.split("\\.").length == 3) {
                    policy = SecurityPolicy.JWT.toString();
                } else {
                    policy = SecurityPolicy.CSA.toString();
                }
            }
        }
        return policy;
    }

    /**
     * Private method to initialize No Security Policy
     */
    private GorAuth getNoAuth() {
        // No security concerns, allow clients to access any data
        return new NoAuth(config, config.sessioncheckerUsername(), "", 0, "",
                List.of(Strings.nullToEmpty(config.sessioncheckerUserrole())), 0);
    }

    public String getSystemAppSession(String project) {
        try (Connection conn = Db.getPool(config.sessioncheckerDbUrl(), config.sessioncheckerUsername(), config.sessioncheckerPassword()).getConnection()) {
            if (conn == null) {
                throw new SQLException("Unable to get a proper connection to database at " + config.sessioncheckerDbUrl() + " with user " + config.sessioncheckerUsername());
            }
            return getSystemAppSession(conn, project);
        } catch (SQLException e) {
            throw new GorSystemException("Error reading project id from the db!", e);
        }
    }

    /**
     * Until we have better authentication between services - this is a temporary solution to allow gor server/workers to final a session to authenticate against csa api with system privileges
     *
     * @param c       database connection
     * @param project if not null, filter by the given project id else ignore.
     * @return system app session token or null if not found
     */
    public static String getSystemAppSession(Connection c, String project) throws SQLException {
        String sql = "select session_key from rda.app_sessions where session_key like 'SYS%' and user_id in (select user_id from rda.user_roles where role = 'system_admin') ";
        if (project != null) {
            sql += " and project_id in (select id from rda.projects where internal_project_name = ?)";
        }

        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            if (project != null) {
                stmt.setString(1, project);
            }

            try (ResultSet r = stmt.executeQuery()) {
                if (r.next()) {
                    return r.getString(1);
                }
            }
        }
        log.warn("No valid session key found in database with system_admin role");
        return null;
    }

    public String getProjectRoot(String project) {
        return new File(getProjectRoot(), project).getAbsolutePath();
    }

    public String getProjectRoot() {
        return config.projectRoot();
    }

    public boolean hasBasicAccess(String sessionKey, String project, String user) {
        GorAuth gorAuth = getGorAuth(sessionKey);
        GorAuthInfo gorAuthInfo = gorAuth.getGorAuthInfo(sessionKey);
        return gorAuth.hasBasicAccess(gorAuthInfo, project, user);
    }

    public boolean hasReadAccess(JsonWebToken jwt, String project) {
        GorAuth gorAuth = getGorAuth(jwt.getRawToken());
        GorAuthInfo gorAuthInfo = gorAuth.getGorAuthInfo(project, jwt);
        return gorAuth.hasReadAccess(gorAuthInfo, project);
    }

    public boolean hasQueryAccess(JsonWebToken jwt, String project) {
        GorAuth gorAuth = getGorAuth(jwt.getRawToken());
        GorAuthInfo gorAuthInfo = gorAuth.getGorAuthInfo(project, jwt);
        return gorAuth.hasQueryAccess(gorAuthInfo, project);
    }

    public boolean hasQueryAccess(String sessionKey, String project, String user) {
        GorAuth gorAuth = getGorAuth(sessionKey);
        GorAuthInfo gorAuthInfo = gorAuth.getGorAuthInfo(sessionKey);
        return gorAuth.hasQueryAccess(gorAuthInfo, project, user);
    }

    public boolean hasWriteAccess(String desiredFileName, JsonWebToken jwt, String project) {
        GorAuth gorAuth = getGorAuth(jwt.getRawToken());
        GorAuthInfo gorAuthInfo = gorAuth.getGorAuthInfo(project, jwt);
        return gorAuth.hasWriteAccess(desiredFileName, gorAuthInfo, project);
    }

    public boolean hasWriteAccess(String desiredFileName, String sessionKey, String project, String user) {
        GorAuth gorAuth = getGorAuth(sessionKey);
        GorAuthInfo gorAuthInfo = gorAuth.getGorAuthInfo(sessionKey);
        return gorAuth.hasWriteAccess(desiredFileName, gorAuthInfo, project, user);
    }

    public boolean hasLordSubmitAccess(String sessionKey, String project, String user) {
        GorAuth gorAuth = getGorAuth(sessionKey);
        GorAuthInfo gorAuthInfo = gorAuth.getGorAuthInfo(sessionKey);
        return gorAuth.hasLordSubmitAccess(gorAuthInfo, project, user);
    }
}
