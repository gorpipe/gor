package org.gorpipe.gor.auth.utils;

import org.gorpipe.gor.auth.GeneralAuthInfo;
import org.gorpipe.gor.auth.GorAuthInfo;
import com.sun.net.httpserver.HttpServer;
import org.gorpipe.gor.auth.AuthConfig;
import org.gorpipe.security.cred.CsaApiService;
import org.gorpipe.security.cred.CsaAuthConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CSA looks users up by email. Usernames that aren't emails (e.g. service accounts such as sequenceminer) must not be
 * looked up, as CSA answers 404 for them and every request logged a warning.
 */
public class UTestCsaApiUtils {

    private static final String PROJECT = "test-proj";

    private CsaApiService csaApiService;

    @Before
    public void setUp() throws IOException {
        csaApiService = mock(CsaApiService.class);
        Map<String, Object> projectMap = new LinkedHashMap<>();
        projectMap.put("id", 5);
        projectMap.put("organization_id", 7);
        doReturn(projectMap).when(csaApiService).getProject(PROJECT);
    }

    @Test
    public void nonEmailUserIsNotLookedUpInCsa() throws IOException {
        GorAuthInfo info = CsaApiUtils.updateWithCsaApi(csaApiService, authInfo("sequenceminer"));

        verify(csaApiService, never()).getUserByEmail(anyString());
        verify(csaApiService, never()).getUserRoleList(anyString(), anyString());
        Assert.assertEquals(5, info.getProjectId());
        Assert.assertEquals("sequenceminer", info.getUsername());
        Assert.assertEquals("", info.getUserId());
        Assert.assertTrue(info.getUserRoles().isEmpty());
    }

    @Test
    public void emailUserGetsIdAndRolesFromCsa() throws IOException {
        String user = "user@email.com";
        doReturn(Collections.singletonMap("id", 10)).when(csaApiService).getUserByEmail(user);
        Map<String, Object> role = new LinkedHashMap<>();
        role.put("role", "researcher");
        doReturn(List.of(role)).when(csaApiService).getUserRoleList(PROJECT, user);

        GorAuthInfo info = CsaApiUtils.updateWithCsaApi(csaApiService, authInfo(user));

        Assert.assertEquals("10", info.getUserId());
        Assert.assertEquals(List.of("researcher"), info.getUserRoles());
    }

    @Test
    public void timeoutStopsTheUpdate() throws IOException {
        String user = "user@email.com";
        doThrow(new SocketTimeoutException("Read timed out")).when(csaApiService).getUserByEmail(user);

        Assert.assertThrows(UncheckedIOException.class, () -> CsaApiUtils.updateWithCsaApi(csaApiService, authInfo(user)));
        verify(csaApiService, never()).getUserRoleList(anyString(), anyString());
    }

    @Test
    public void getProjectMapReturnsNullOnTimeout() throws IOException {
        // Public, and used outside GOR (gdb-gor-services QueryServiceCache), so it keeps returning null on failure.
        doThrow(new SocketTimeoutException("Read timed out")).when(csaApiService).getProject(PROJECT);

        Assert.assertNull(CsaApiUtils.getProjectMap(csaApiService, PROJECT));
    }

    @Test
    public void csaNotFoundIsAnAnswerButServerErrorIsAFailure() throws Exception {
        // Real HTTP, so the exceptions have the shape HttpURLConnection and HttpJsonServiceClient give them.
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            int status;
            String body;
            if (path.equals("/api/projects/" + PROJECT)) {
                status = 200;
                body = "{\"project\": {\"id\": 5, \"organization_id\": 7}}";
            } else if (path.contains("unknown@")) {
                status = 404;
                body = "{\"error\": \"not found\"}";
            } else {
                status = 500;
                body = "boom";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        try {
            CsaAuthConfiguration csaConfig = mock(CsaAuthConfiguration.class);
            doReturn("http://127.0.0.1:" + server.getAddress().getPort() + "/").when(csaConfig).getAuthApiEndpoint();
            doReturn("csa-user").when(csaConfig).getUser();
            doReturn("csa-pass").when(csaConfig).getPassword();
            doReturn(1000).when(csaConfig).getConnectTimeoutMs();
            doReturn(1000).when(csaConfig).getReadTimeoutMs();
            CsaApiService service = new CsaApiService(csaConfig, mock(AuthConfig.class));

            CsaApiUtils.CsaLookup unknown = CsaApiUtils.lookupWithCsaApi(service, authInfo("unknown@email.com"));
            CsaApiUtils.CsaLookup broken = CsaApiUtils.lookupWithCsaApi(service, authInfo("broken@email.com"));

            Assert.assertEquals(5, unknown.info().getProjectId());
            Assert.assertTrue(unknown.complete());
            Assert.assertEquals(5, broken.info().getProjectId());
            Assert.assertFalse(broken.complete());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void updateWithCsaApiStillReturnsPartialResultOnError() throws IOException {
        // Public, and used outside GOR: an error that is not a timeout is still logged and skipped.
        String user = "user@email.com";
        doThrow(new IOException("Internal Server Error")).when(csaApiService).getUserByEmail(user);

        GorAuthInfo info = CsaApiUtils.updateWithCsaApi(csaApiService, authInfo(user));

        Assert.assertEquals(5, info.getProjectId());
        Assert.assertEquals("", info.getUserId());
    }

    private static GorAuthInfo authInfo(String username) {
        return new GeneralAuthInfo(0, PROJECT, username, "", null, 0, Long.MAX_VALUE);
    }
}
