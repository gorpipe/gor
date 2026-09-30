package org.gorpipe.gor.auth;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.ParseException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.gorpipe.gor.auth.utils.OAuthHandler;
import org.gorpipe.security.cred.CsaApiService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Username resolution for platform JWTs. Service-account tokens (Keycloak client_credentials) carry no email claim,
 * so the configured user key must fall back to preferred_username, then azp/client_id, then sub.
 */
public class UTestPlatformAuthUsername {

    private static final String ISSUER = "https://keycloak.test/auth/realms/test";
    private static final String PROJECT = "test-proj";
    private static final String EMAIL = "user@example.com";
    private static final String SERVICE_ACCOUNT = "service-account-cope-oscar";
    private static final String CLIENT_ID = "cope-oscar";
    private static final String SUB = "0b5a2f6e-6a4e-4d8b-9d1e-4a2c3f7e8b90";

    private static KeyPair keyPair;
    private static KeyPair otherKeyPair;

    private AuthConfig config;
    private ListAppender<ILoggingEvent> auditAppender;
    private Logger auditLogger;
    private Level auditLevel;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            otherKeyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Before
    public void setUp() {
        config = mock(AuthConfig.class);
        doReturn("email").when(config).getPlatformUserKey();
        doReturn("NONE").when(config).updateAuthInfoPolicy();
        doReturn(true).when(config).userRolesFromToken();

        auditLogger = (Logger) LoggerFactory.getLogger("audit." + PlatformAuth.class.getName());
        auditLevel = auditLogger.getLevel();
        auditLogger.setLevel(Level.INFO);
        auditAppender = new ListAppender<>();
        auditAppender.start();
        auditLogger.addAppender(auditAppender);
    }

    @After
    public void tearDown() {
        auditLogger.detachAppender(auditAppender);
        auditLogger.setLevel(auditLevel);
    }

    // --- PlatformJWTAuth ---

    @Test
    public void jwtAuthUsesEmailWhenPresent() throws Exception {
        JsonWebToken jwt = parse(token().withClaim("email", EMAIL).withClaim("preferred_username", SERVICE_ACCOUNT));
        Assert.assertEquals(EMAIL, new PlatformJWTAuth(config, null).getGorAuthInfo(PROJECT, jwt).getUsername());
    }

    @Test
    public void jwtAuthFallsBackToPreferredUsernameWithoutEmail() throws Exception {
        JsonWebToken jwt = parse(token().withClaim("preferred_username", SERVICE_ACCOUNT));
        Assert.assertEquals(SERVICE_ACCOUNT, new PlatformJWTAuth(config, null).getGorAuthInfo(PROJECT, jwt).getUsername());
    }

    @Test
    public void jwtAuthFallsBackToPreferredUsernameWhenEmailIsEmpty() throws Exception {
        JsonWebToken jwt = parse(token().withClaim("email", "").withClaim("preferred_username", SERVICE_ACCOUNT));
        Assert.assertEquals(SERVICE_ACCOUNT, new PlatformJWTAuth(config, null).getGorAuthInfo(PROJECT, jwt).getUsername());
    }

    @Test
    public void jwtAuthFallsBackToAzpWithoutEmailOrPreferredUsername() throws Exception {
        JsonWebToken jwt = parse(token().withClaim("azp", CLIENT_ID));
        Assert.assertEquals(CLIENT_ID, new PlatformJWTAuth(config, null).getGorAuthInfo(PROJECT, jwt).getUsername());
    }

    @Test
    public void jwtAuthFallsBackToClientIdWithoutAzp() throws Exception {
        JsonWebToken jwt = parse(token().withClaim("client_id", CLIENT_ID));
        Assert.assertEquals(CLIENT_ID, new PlatformJWTAuth(config, null).getGorAuthInfo(PROJECT, jwt).getUsername());
    }

    @Test
    public void jwtAuthFallsBackToSub() throws Exception {
        JsonWebToken jwt = parse(token());
        Assert.assertEquals(SUB, new PlatformJWTAuth(config, null).getGorAuthInfo(PROJECT, jwt).getUsername());
    }

    @Test
    public void jwtAuthHandlesMissingRealmAccess() throws Exception {
        JsonWebToken jwt = parse(JWT.create()
                .withIssuer(ISSUER)
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(SUB)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .withClaim("preferred_username", SERVICE_ACCOUNT));

        GorAuthInfo info = new PlatformJWTAuth(config, null).getGorAuthInfo(PROJECT, jwt);
        Assert.assertEquals(SERVICE_ACCOUNT, info.getUsername());
        Assert.assertTrue(info.getUserRoles().isEmpty());
    }

    @Test
    public void jwtAuthCachesServiceAccountsSeparately() throws Exception {
        CsaApiService csaApiService = mock(CsaApiService.class);
        doReturn(Collections.singletonMap("id", 11)).when(csaApiService).getUserByEmail("service-account-a");
        doReturn(Collections.singletonMap("id", 22)).when(csaApiService).getUserByEmail("service-account-b");
        doReturn(null).when(csaApiService).getProject(anyString());
        doReturn("CSA").when(config).updateAuthInfoPolicy();

        PlatformJWTAuth auth = new PlatformJWTAuth(config, csaApiService);
        GorAuthInfo a = auth.getGorAuthInfo(PROJECT, parse(token().withClaim("preferred_username", "service-account-a")));
        GorAuthInfo b = auth.getGorAuthInfo(PROJECT, parse(token().withClaim("preferred_username", "service-account-b")));

        Assert.assertEquals("service-account-a", a.getUsername());
        Assert.assertEquals("11", a.getUserId());
        Assert.assertEquals("service-account-b", b.getUsername());
        Assert.assertEquals("22", b.getUserId());
    }

    // --- PlatformAuth ---

    @Test
    public void platformAuthUsesEmailWhenPresent() {
        String sessionKey = sessionKey(sign(token().withClaim("email", EMAIL).withClaim("preferred_username", SERVICE_ACCOUNT)));
        Assert.assertEquals(EMAIL, platformAuth().getGorAuthInfo(sessionKey).getUsername());
    }

    @Test
    public void platformAuthFallsBackToPreferredUsernameWithoutEmail() {
        String sessionKey = sessionKey(sign(token().withClaim("preferred_username", SERVICE_ACCOUNT)));
        Assert.assertEquals(SERVICE_ACCOUNT, platformAuth().getGorAuthInfo(sessionKey).getUsername());
    }

    @Test
    public void platformAuthFallsBackToAzpWithoutEmailOrPreferredUsername() {
        String sessionKey = sessionKey(sign(token().withClaim("azp", CLIENT_ID)));
        Assert.assertEquals(CLIENT_ID, platformAuth().getGorAuthInfo(sessionKey).getUsername());
    }

    @Test
    public void platformAuthFallsBackToSub() {
        String sessionKey = sessionKey(sign(token()));
        Assert.assertEquals(SUB, platformAuth().getGorAuthInfo(sessionKey).getUsername());
    }

    @Test
    public void platformAuthHandlesMissingRealmAccess() {
        String sessionKey = sessionKey(sign(JWT.create()
                .withSubject(SUB)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .withClaim("preferred_username", SERVICE_ACCOUNT)));

        GorAuthInfo info = platformAuth().getGorAuthInfo(sessionKey);
        Assert.assertEquals(SERVICE_ACCOUNT, info.getUsername());
        Assert.assertTrue(info.getUserRoles().isEmpty());
    }

    @Test
    public void platformAuthAuditLogsServiceAccountOnBadSignature() {
        String badToken = token().withClaim("preferred_username", SERVICE_ACCOUNT)
                .sign(Algorithm.RSA256((RSAPublicKey) otherKeyPair.getPublic(), (RSAPrivateKey) otherKeyPair.getPrivate()));

        Assert.assertNull(platformAuth().getGorAuthInfo(sessionKey(badToken)));
        Assert.assertEquals(SERVICE_ACCOUNT, auditUsername());
    }

    @Test
    public void platformAuthAuditLogsServiceAccountOnExpiredToken() {
        String expiredToken = sign(JWT.create()
                .withSubject(SUB)
                .withExpiresAt(new Date(System.currentTimeMillis() - 3600_000))
                .withClaim("preferred_username", SERVICE_ACCOUNT));

        Assert.assertNull(platformAuth().getGorAuthInfo(sessionKey(expiredToken)));
        Assert.assertEquals(SERVICE_ACCOUNT, auditUsername());
    }

    // --- helpers ---

    private static JWTCreator.Builder token() {
        return JWT.create()
                .withIssuer(ISSUER)
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(SUB)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .withClaim(GorAuth.REALM_ACCESS, Collections.singletonMap(GorAuth.ROLES, List.of("prj:test-proj:query")));
    }

    private static String sign(JWTCreator.Builder builder) {
        return builder.sign(Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate()));
    }

    private static JsonWebToken parse(JWTCreator.Builder builder) throws ParseException {
        JWTAuthContextInfo ci = new JWTAuthContextInfo((RSAPublicKey) keyPair.getPublic(), ISSUER);
        return new DefaultJWTParser(ci).parse(sign(builder));
    }

    private static String sessionKey(String accessToken) {
        return "{\"security-policy\": \"PLATFORM\", \"source\": \"test\", \"project\": \"" + PROJECT + "\", "
                + "\"access-token\": \"" + accessToken + "\"}";
    }

    private PlatformAuth platformAuth() {
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        return new PlatformAuth(config, null, new OAuthHandler(publicKey));
    }

    private String auditUsername() {
        Assert.assertEquals(1, auditAppender.list.size());
        return auditAppender.list.get(0).getArgumentArray()[0].toString();
    }
}
