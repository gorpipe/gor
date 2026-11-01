package org.gorpipe.gor.auth;

import jakarta.json.JsonString;
import org.gorpipe.gor.auth.utils.PlatformGorAuthCache;
import org.gorpipe.security.cred.CsaApiService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class PlatformJWTAuth extends GorAuth {

    private final static Logger log = LoggerFactory.getLogger(PlatformJWTAuth.class);
    private final static Logger auditLog = LoggerFactory.getLogger("audit." + PlatformJWTAuth.class.getName());

    private PlatformGorAuthCache gorAuthInfoCache;


    private String userKey;

    public PlatformJWTAuth(AuthConfig config, CsaApiService csaApiService) throws GorSystemException {
        super(config, csaApiService);
        this.securityPolicy = SecurityPolicy.JWT;
        this.userKey = config.getPlatformUserKey();
        this.gorAuthInfoCache = new PlatformGorAuthCache();
    }

    @Override
    public GorAuthInfo getGorAuthInfo(String sessionKey) {
        throw new GorSystemException("Not Supported", null);
    }

    @Override
    public GorAuthInfo getGorAuthInfo(String project, JsonWebToken jwt) {
        if (jwt == null || jwt.getTokenID() == null) {
            log.error("Access Token is null in PLATFORM security policy");
            return new GeneralAuthInfo(-1, project, null, null, null, -1, -1);
        }

        long expiration = jwt.getExpirationTime();
        String username = getUsername(jwt);
        String cacheId = project + ":" + username;
        List<String> roles = getUserRoles(jwt);

        // We cache a basic gorauthtoken with all the lookups, then we always add the roles.
        GorAuthInfo gorAuthInfo = gorAuthInfoCache.get(cacheId);
        if (gorAuthInfo == null) {
            try {
                // Pass the token roles in so CSA isn't asked for roles that would be replaced by them anyway.
                gorAuthInfo = updateGorAuthInfo(new GeneralAuthInfo(0, project, username, "", roles, 0, Long.MAX_VALUE));
                // If we got project id we keep this for ever, otherwise just 1 min.  The cache expires entries by
                // the expiration of the stored auth info, so it must carry the 1 min too.
                long cacheExpiration = gorAuthInfo.getProjectId() > 0 ? Long.MAX_VALUE : System.currentTimeMillis() + 60000;
                gorAuthInfo = new GeneralAuthInfo(gorAuthInfo.getProjectId(), project, username, gorAuthInfo.getUserId(),
                        gorAuthInfo.getUserRoles(), gorAuthInfo.getOrganizationId(), cacheExpiration);
                gorAuthInfoCache.add(cacheId, gorAuthInfo, cacheExpiration);
            } catch (Exception e) {
                throw new GorSystemException(e);
            }
        }

        gorAuthInfo = new GeneralAuthInfo(gorAuthInfo.getProjectId(), project, username, gorAuthInfo.getUserId(),
                roles, gorAuthInfo.getOrganizationId(), expiration);
        return gorAuthInfo;
    }

    private String getUsername(JsonWebToken jwt) {
        return resolveUsername(userKey, jwt);
    }

    private List<String> getUserRoles(JsonWebToken jwt) {
        Map<String, List<JsonString>> realmAccess = jwt.getClaim(REALM_ACCESS);
        if (realmAccess == null || realmAccess.get(ROLES) == null) {
            return Collections.emptyList();
        }
        return realmAccess.get(ROLES).stream().map(js -> js.getString()).collect(Collectors.toList());
    }
}
