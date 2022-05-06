package org.gorpipe.gor.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.gorpipe.gor.auth.utils.PlainGorAuthCache;
import org.gorpipe.security.cred.CsaApiService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;


public class PlainAuth extends GorAuth {

    private static final Logger log = LoggerFactory.getLogger(PlainAuth.class);

    private ObjectMapper objectMapper;
    private PlainGorAuthCache gorAuthInfoCache;

    public PlainAuth(AuthConfig config, CsaApiService csaApiService) {
        super(config, csaApiService);
        this.securityPolicy = SecurityPolicy.PLAIN;
        this.objectMapper = new ObjectMapper();
        this.gorAuthInfoCache = new PlainGorAuthCache();
    }

    @Override
    public GorAuthInfo getGorAuthInfo(String sessionKey) {
        if (Strings.isNullOrEmpty(sessionKey) || "NO_SESSION".equals(sessionKey)) {
            log.debug("sessionKey is empty");
            return new GeneralAuthInfo(0, "", "", "", null, 0, 0);
        } else {
            GorAuthInfo gorAuthInfo = gorAuthInfoCache.get(sessionKey);
            if (gorAuthInfo == null) {
                try {
                    log.debug("Parsing plain token {}", sessionKey);
                    gorAuthInfo = updateGorAuthInfo(objectMapper.readValue(sessionKey, GeneralAuthInfo.class));
                    gorAuthInfoCache.add(sessionKey, gorAuthInfo);
                } catch (IOException e) {
                    log.warn("Error parsing sessionKey {}", sessionKey);
                    throw new GorSystemException("Error reading GorAuthInfo from session key!", e);
                }
            }
            return gorAuthInfo;
        }
    }

    @Override
    public GorAuthInfo getGorAuthInfo(String project, JsonWebToken jwt) {
        return getGorAuthInfo(jwt.getRawToken());
    }

    @Override
    public boolean hasBasicAccess(GorAuthInfo authInfo, String project, String user) {
        return true;
    }

    @Override
    public boolean hasAccessBasedOnRoles(List<String> authRoles, AuthorizationAction authorizationAction, String project) {
        return true;
    }
}


