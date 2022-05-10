package org.gorpipe.gor.auth;

import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gorpipe.gor.auth.utils.OAuthHandler;
import org.gorpipe.gor.auth.utils.PlatformGorAuthCache;
import org.gorpipe.security.cred.CsaApiService;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.value;


public class PlatformAuth extends GorAuth {

    private final static Logger log = LoggerFactory.getLogger(PlatformAuth.class);
    private final static Logger auditLog = LoggerFactory.getLogger("audit." + PlatformAuth.class.getName());

    private PlatformGorAuthCache gorAuthInfoCache;

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";
    private static final String SUB = "sub";

    private ObjectMapper objectMapper;
    private String userKey;
    private OAuthHandler oAuthHandler;
    private boolean useRolesFromToken;

    protected PlatformAuth(AuthConfig config, CsaApiService csaApiService, OAuthHandler oAuthHandler) throws GorSystemException {
        super(config, csaApiService);
        this.securityPolicy = SecurityPolicy.PLATFORM;
        this.objectMapper = new ObjectMapper();
        this.userKey = config.getPlatformUserKey();
        this.gorAuthInfoCache = new PlatformGorAuthCache();
        this.oAuthHandler = oAuthHandler;
        this.useRolesFromToken = config.userRolesFromToken();
    }

    @Override
    public GorAuthInfo getGorAuthInfo(String securityKey) {
        GorAuthInfo gorAuthInfo = gorAuthInfoCache.get(securityKey);
        if (gorAuthInfo == null) {
            try {
                log.debug("Parsing platform token {}", securityKey);

                String sessionKey = getSessionKey(securityKey);

                PlatformSessionKey platformSessionKey = objectMapper.readValue(sessionKey, PlatformSessionKey.class);

                String accessToken = platformSessionKey.getAccessToken();
                String project = platformSessionKey.getProject();

                if (accessToken == null) {
                    log.error("ERROR: Access Token is null in PLATFORM security policy");
                    return null;
                }

                if (project == null) {
                    log.error("ERROR: Project is null in PLATFORM security policy");
                    return null;
                }

                DecodedJWT jwt = verifyAccessToken(accessToken, platformSessionKey);
                if (jwt == null) {
                    return null;
                }

                // Get attributes from the verified JWT
                String username = getUsername(jwt);
                long expiration = getExpiration(jwt);
                String userId = getSub(jwt);
                List<String> userRoles = getUserRoles(jwt);
                gorAuthInfo = updateGorAuthInfo(new GeneralAuthInfo(0, project, username, userId, userRoles, 0, expiration));
                gorAuthInfoCache.add(securityKey, gorAuthInfo, expiration);
            } catch (Exception e) {
                throw new GorSystemException(e);
            }
        }
        return gorAuthInfo;
    }

    private String getSub(DecodedJWT jwt) {
        Claim claim = jwt.getClaim(SUB);
        return claim != null ? claim.asString() : null;
    }

    private String getUsername(DecodedJWT jwt) {
        Claim claim = jwt.getClaim(userKey);
        return claim != null ? claim.asString() : null;
    }

    private long getExpiration(DecodedJWT jwt) {
        return jwt.getExpiresAt().getTime();
    }

    private List<String> getUserRoles(DecodedJWT jwt) {
        return (List<String>)jwt.getClaim(REALM_ACCESS).asMap().get(ROLES);
    }

    /**
     * Check the session-key which should be on JSON format from the security key
     *
     * @param securityKey
     * @return
     */

    private String getSessionKey(String securityKey) {
        final int sessionKeyEnd = securityKey.indexOf("|||");
        return sessionKeyEnd > 0 ? securityKey.substring(0, sessionKeyEnd) : securityKey;
    }

    private DecodedJWT verifyAccessToken(String accessToken, PlatformSessionKey platformSessionKey) {
        // Verify Access Token Signature
        try {
            return oAuthHandler.verifyAccessToken(accessToken);
        } catch (SignatureVerificationException e) {
            DecodedJWT jwtDecoded = oAuthHandler.decodeToken(accessToken);
            String username = jwtDecoded.getClaim(userKey).asString();
            String message = "ERROR: Unable to verify the signature of the access token";
            log.error(message, e);
            auditLog.info(message,
                    value("username", username),
                    value("project", platformSessionKey.getProject()),
                    value("source", platformSessionKey.getSource()),
                    value("security-policy", platformSessionKey.getSecurityPolicy()));
            return null;
        } catch (TokenExpiredException e) {
            DecodedJWT jwtDecoded = oAuthHandler.decodeToken(accessToken);
            String username = jwtDecoded.getClaim(userKey).asString();
            log.error(e.getMessage(), e);
            auditLog.info(e.getMessage(),
                    value("username", username),
                    value("project", platformSessionKey.getProject()),
                    value("source", platformSessionKey.getSource()),
                    value("security-policy", platformSessionKey.getSecurityPolicy()));
            return null;
        }
    }

}
