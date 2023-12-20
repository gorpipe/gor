package org.gorpipe.util.http.keycloak;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gorpipe.exceptions.GorSystemException;

import java.time.LocalDateTime;
import java.util.HashMap;


public class KeycloakTokenResult {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String EXPIRES_IN = "expires_in";
    private static final String REFRESH_EXPIRES_IN = "refresh_expires_in";
    private static final String TOKEN_TYPE = "token_type";
    private static final String SESSION_STATE = "session_state";
    private static final String SCOPE = "scope";


    private final String accessToken;
    private final String refreshToken;
    private final LocalDateTime expiresAt;
    private final LocalDateTime refreshExpiresAt;
    private final String tokenType;
    private final String sessionState;
    private final String scope;

    private KeycloakTokenResult(String accessToken, String refreshToken, LocalDateTime expiresAt,
                                LocalDateTime refreshExpiresAt, String tokenType, String sessionState, String scope) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.tokenType = tokenType;
        this.sessionState = sessionState;
        this.scope = scope;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getSessionState() {
        return sessionState;
    }

    public String getScope() {
        return scope;
    }

    public static KeycloakTokenResult fromJson(String json, LocalDateTime now) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            var sessionMap = mapper.readValue(json, new TypeReference<HashMap<String,String>>(){});
            String accessToken = sessionMap.getOrDefault(ACCESS_TOKEN, "");
            if (accessToken.isEmpty()) {
                throw new GorSystemException("Access token from keycloak is empty", null);
            }
            var refreshToken = sessionMap.getOrDefault(REFRESH_TOKEN, "");
            var expiresAt = now.plusSeconds(Integer.parseInt(sessionMap.getOrDefault(EXPIRES_IN, "0")));
            var refreshExpiresAt = now.plusSeconds(Integer.parseInt(sessionMap.getOrDefault(REFRESH_EXPIRES_IN, "0")));
            var tokenType = sessionMap.getOrDefault(TOKEN_TYPE, "");
            var sessionState = sessionMap.getOrDefault(SESSION_STATE, "");
            var scope = sessionMap.getOrDefault(SCOPE, "");


            return new KeycloakTokenResult(accessToken, refreshToken, expiresAt,
                    refreshExpiresAt, tokenType, sessionState, scope);
        } catch (JsonProcessingException e) {
            throw new GorSystemException("Failed to parse Keycloak token result", e);
        }
    }
}
