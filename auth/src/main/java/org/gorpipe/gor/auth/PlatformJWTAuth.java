package org.gorpipe.gor.auth;

import jakarta.json.JsonString;
import org.gorpipe.gor.auth.utils.PlatformGorAuthCache;
import org.gorpipe.security.cred.CsaApiService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        // We cache a basic gorauthtoken with all the lookups, then we always add the roles.
        GorAuthInfo gorAuthInfo = gorAuthInfoCache.get(cacheId);
        if (gorAuthInfo == null) {
            try {
                gorAuthInfo = updateGorAuthInfo(new GeneralAuthInfo(0, project, username, "", null, 0, Long.MAX_VALUE));
                // If we got project id we keep tihs for ever, otherwise just 1 min.
                gorAuthInfoCache.add(cacheId, gorAuthInfo, gorAuthInfo.getProjectId() > 0 ?  Long.MAX_VALUE : 60000);
            } catch (Exception e) {
                throw new GorSystemException(e);
            }
        }

        List<String> roles = getUserRoles(jwt);
        gorAuthInfo = new GeneralAuthInfo(gorAuthInfo.getProjectId(), project, username, gorAuthInfo.getUserId(),
                roles, gorAuthInfo.getOrganizationId(), expiration);
        return gorAuthInfo;
    }

    private String getUsername(JsonWebToken jwt) {
        return jwt.getClaim(userKey);
    }

    private List<String> getUserRoles(JsonWebToken jwt) {
        List<JsonString> roles = ((Map<String, List<JsonString>>) jwt.getClaim(REALM_ACCESS)).get(ROLES);
        return roles.stream().map(js -> js.getString()).collect(Collectors.toList());
    }
}
