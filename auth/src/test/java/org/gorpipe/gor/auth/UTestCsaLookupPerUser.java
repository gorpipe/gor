package org.gorpipe.gor.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.ParseException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.gorpipe.gor.auth.utils.PlatformGorAuthCache;
import org.gorpipe.security.cred.CsaApiService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CSA lookups (project, user, roles) are cached per (project, user), not per access token, so a client that gets a
 * new token for every call does not cost CSA calls on every query. Roles and expiration still come from each token.
 */
public class UTestCsaLookupPerUser {

    private static final String ISSUER = "https://keycloak.test/auth/realms/test";
    private static final String PROJECT = "test-proj";
    private static final String EMAIL = "user@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String SUB = "0b5a2f6e-6a4e-4d8b-9d1e-4a2c3f7e8b90";

    private static KeyPair keyPair;
    private static KeyPair otherKeyPair;

    private AuthConfig config;
    private CsaApiService csa;

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
    public void setUp() throws Exception {
        config = mock(AuthConfig.class);
        doReturn("email").when(config).getPlatformUserKey();
        doReturn("CSA").when(config).updateAuthInfoPolicy();
        doReturn(true).when(config).userRolesFromToken();
        doReturn(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())).when(config).publicAuthorizationKey();

        csa = mock(CsaApiService.class);
        Map<String, Object> project = new HashMap<>();
        project.put("id", 5);
        project.put("organization_id", 3);
        doReturn(project).when(csa).getProject(PROJECT);
        doReturn(Collections.singletonMap("id", 7)).when(csa).getUserByEmail(anyString());
        LinkedHashMap<String, Object> csaRole = new LinkedHashMap<>();
        csaRole.put("role", "csa-role");
        doReturn(List.of(csaRole)).when(csa).getUserRoleList(anyString(), anyString());
    }

    // --- JWT policy (gor-query-server) ---

    @Test
    public void jwtReusesCsaLookupsAcrossTokensForSameUser() throws Exception {
        GorAuthFactory factory = factory("JWT");

        GorAuthInfo first = factory.getGorAuthInfo(PROJECT, parse(token().withClaim("email", EMAIL)));
        GorAuthInfo second = factory.getGorAuthInfo(PROJECT, parse(token().withClaim("email", EMAIL)));

        verify(csa, times(1)).getProject(PROJECT);
        verify(csa, times(1)).getUserByEmail(EMAIL);
        Assert.assertEquals(5, second.getProjectId());
        Assert.assertEquals("7", second.getUserId());
        Assert.assertEquals(first.getUserId(), second.getUserId());
    }

    @Test
    public void jwtTakesRolesAndExpirationFromEachToken() throws Exception {
        GorAuthFactory factory = factory("JWT");
        long laterExpiry = (System.currentTimeMillis() / 1000 + 7200) * 1000;

        factory.getGorAuthInfo(PROJECT, parse(token().withClaim("email", EMAIL)));
        GorAuthInfo second = factory.getGorAuthInfo(PROJECT, parse(token()
                .withClaim("email", EMAIL)
                .withExpiresAt(new Date(laterExpiry))
                .withClaim(GorAuth.REALM_ACCESS, Collections.singletonMap(GorAuth.ROLES, List.of("prj:test-proj:write")))));

        Assert.assertEquals(List.of("prj:test-proj:write"), second.getUserRoles());
        Assert.assertEquals(laterExpiry / 1000, second.getExpiration());
    }

    @Test
    public void jwtLooksUpEachUserSeparately() throws Exception {
        GorAuthFactory factory = factory("JWT");

        factory.getGorAuthInfo(PROJECT, parse(token().withClaim("email", EMAIL)));
        factory.getGorAuthInfo(PROJECT, parse(token().withClaim("email", OTHER_EMAIL)));

        verify(csa, times(1)).getUserByEmail(EMAIL);
        verify(csa, times(1)).getUserByEmail(OTHER_EMAIL);
    }

    @Test
    public void factoryUsesOneGorAuthPerPolicy() throws Exception {
        GorAuthFactory factory = factory("JWT");

        GorAuth a = factory.getGorAuth(parse(token().withClaim("email", EMAIL)).getRawToken());
        GorAuth b = factory.getGorAuth(parse(token().withClaim("email", OTHER_EMAIL)).getRawToken());

        Assert.assertSame(a, b);
    }

    @Test
    public void factoryStillRejectsInvalidPolicy() {
        AuthConfig plainOnly = mock(AuthConfig.class);
        doReturn(new String[]{"PLAIN", "NONE"}).when(plainOnly).securityPolicies();
        GorAuthFactory factory = new GorAuthFactory(plainOnly, csa);

        Assert.assertThrows(org.gorpipe.exceptions.GorSystemException.class,
                () -> factory.getGorAuth("{\"security-policy\": \"PLATFORM\"}"));
    }

    // --- PLATFORM policy (gor-query, lord) ---

    @Test
    public void platformReusesCsaLookupsAcrossTokensForSameUser() throws Exception {
        GorAuthFactory factory = factory("PLATFORM");

        GorAuthInfo first = factory.getGorAuthInfo(sessionKey(sign(token().withClaim("email", EMAIL))));
        GorAuthInfo second = factory.getGorAuthInfo(sessionKey(sign(token().withClaim("email", EMAIL))));

        verify(csa, times(1)).getProject(PROJECT);
        Assert.assertEquals(5, first.getProjectId());
        Assert.assertEquals(5, second.getProjectId());
        Assert.assertEquals(SUB, second.getUserId());
    }

    @Test
    public void platformTakesRolesAndExpirationFromEachToken() {
        GorAuthFactory factory = factory("PLATFORM");
        long laterExpiry = (System.currentTimeMillis() / 1000 + 7200) * 1000;

        factory.getGorAuthInfo(sessionKey(sign(token().withClaim("email", EMAIL))));
        GorAuthInfo second = factory.getGorAuthInfo(sessionKey(sign(token()
                .withClaim("email", EMAIL)
                .withExpiresAt(new Date(laterExpiry))
                .withClaim(GorAuth.REALM_ACCESS, Collections.singletonMap(GorAuth.ROLES, List.of("prj:test-proj:write"))))));

        Assert.assertEquals(List.of("prj:test-proj:write"), second.getUserRoles());
        Assert.assertEquals(laterExpiry, second.getExpiration());
    }

    @Test
    public void platformStillRejectsBadSignatureForCachedUser() {
        GorAuthFactory factory = factory("PLATFORM");

        Assert.assertNotNull(factory.getGorAuthInfo(sessionKey(sign(token().withClaim("email", EMAIL)))));
        String forged = token().withClaim("email", EMAIL)
                .sign(Algorithm.RSA256((RSAPublicKey) otherKeyPair.getPublic(), (RSAPrivateKey) otherKeyPair.getPrivate()));

        Assert.assertNull(factory.getGorAuthInfo(sessionKey(forged)));
    }

    @Test
    public void platformDoesNotGiveCsaRolesToTokenWithRoles() {
        GorAuthFactory factory = factory("PLATFORM");

        // Tokens without sub make CSA look the user (and, without token roles, the roles) up.
        GorAuthInfo withoutRoles = factory.getGorAuthInfo(sessionKey(sign(tokenWithoutSub(List.of()))));
        GorAuthInfo withRoles = factory.getGorAuthInfo(sessionKey(sign(tokenWithoutSub(List.of("prj:test-proj:query")))));

        Assert.assertEquals(List.of("csa-role"), withoutRoles.getUserRoles());
        Assert.assertEquals(List.of("prj:test-proj:query"), withRoles.getUserRoles());
    }

    @Test
    public void platformDoesNotGiveTokenRolesToTokenWithoutRoles() throws Exception {
        GorAuthFactory factory = factory("PLATFORM");

        GorAuthInfo withRoles = factory.getGorAuthInfo(sessionKey(sign(tokenWithoutSub(List.of("prj:test-proj:query")))));
        GorAuthInfo withoutRoles = factory.getGorAuthInfo(sessionKey(sign(tokenWithoutSub(List.of()))));

        Assert.assertEquals(List.of("prj:test-proj:query"), withRoles.getUserRoles());
        Assert.assertEquals(List.of("csa-role"), withoutRoles.getUserRoles());
        verify(csa, times(1)).getUserRoleList(PROJECT, EMAIL);
    }

    @Test
    public void platformTokenWithoutSubGetsCsaUserNotAnotherTokensSub() {
        GorAuthFactory factory = factory("PLATFORM");

        factory.getGorAuthInfo(sessionKey(sign(token().withClaim("email", EMAIL))));
        GorAuthInfo withoutSub = factory.getGorAuthInfo(sessionKey(sign(tokenWithoutSub(List.of()))));

        Assert.assertEquals("7", withoutSub.getUserId());
        Assert.assertEquals(List.of("csa-role"), withoutSub.getUserRoles());
    }

    @Test
    public void platformTokenWithSubGetsNoCsaRoles() {
        GorAuthFactory factory = factory("PLATFORM");

        factory.getGorAuthInfo(sessionKey(sign(tokenWithoutSub(List.of()))));
        GorAuthInfo withSub = factory.getGorAuthInfo(sessionKey(sign(token().withClaim("email", EMAIL)
                .withClaim(GorAuth.REALM_ACCESS, Collections.singletonMap(GorAuth.ROLES, List.of())))));

        Assert.assertEquals(SUB, withSub.getUserId());
        Assert.assertTrue(withSub.getUserRoles().isEmpty());
    }

    @Test
    public void completeCsaLookupIsCachedForCacheLifetime() {
        GorAuthInfo cached = new PlatformJWTAuth(config, csa)
                .getCsaAuthInfo(new PlatformGorAuthCache(), PROJECT, EMAIL, "", List.of());

        Assert.assertEquals(Long.MAX_VALUE, cached.getExpiration());
    }

    @Test
    public void timeoutAfterProjectResolvedIsCachedForOneMinute() throws Exception {
        // The project resolves, then the user lookup times out: don't share the missing user id for the cache
        // lifetime.
        doThrow(new SocketTimeoutException("Read timed out")).when(csa).getUserByEmail(EMAIL);

        long before = System.currentTimeMillis();
        GorAuthInfo cached = new PlatformJWTAuth(config, csa)
                .getCsaAuthInfo(new PlatformGorAuthCache(), PROJECT, EMAIL, "", List.of("prj:test-proj:query"));

        Assert.assertTrue(cached.getExpiration() <= System.currentTimeMillis() + 60_000);
        Assert.assertTrue(cached.getExpiration() >= before + 60_000);
    }

    @Test
    public void userUnknownToCsaIsCachedForCacheLifetime() throws Exception {
        // CSA answering "no such user" is an answer, not a failure: don't ask again every minute.
        doThrow(new IOException("Not Found")).when(csa).getUserByEmail(EMAIL);

        GorAuthInfo cached = new PlatformJWTAuth(config, csa)
                .getCsaAuthInfo(new PlatformGorAuthCache(), PROJECT, EMAIL, "", List.of("prj:test-proj:query"));

        Assert.assertEquals(5, cached.getProjectId());
        Assert.assertEquals(Long.MAX_VALUE, cached.getExpiration());
    }

    @Test
    public void csaTimeoutStopsFurtherLookups() throws Exception {
        doThrow(new SocketTimeoutException("Read timed out")).when(csa).getProject(PROJECT);

        GorAuthInfo info = factory("JWT").getGorAuthInfo(PROJECT, parse(token().withClaim("email", EMAIL)));

        verify(csa, times(1)).getProject(PROJECT);
        verify(csa, never()).getUserByEmail(anyString());
        Assert.assertTrue(info.getProjectId() <= 0);
        Assert.assertEquals(List.of("prj:test-proj:query"), info.getUserRoles());
    }

    // --- helpers ---

    private GorAuthFactory factory(String policy) {
        doReturn(new String[]{policy}).when(config).securityPolicies();
        return new GorAuthFactory(config, csa);
    }

    private static JWTCreator.Builder token() {
        return JWT.create()
                .withIssuer(ISSUER)
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(SUB)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .withClaim(GorAuth.REALM_ACCESS, Collections.singletonMap(GorAuth.ROLES, List.of("prj:test-proj:query")));
    }

    private static JWTCreator.Builder tokenWithoutSub(List<String> roles) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .withClaim("email", EMAIL)
                .withClaim(GorAuth.REALM_ACCESS, Collections.singletonMap(GorAuth.ROLES, roles));
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
}
