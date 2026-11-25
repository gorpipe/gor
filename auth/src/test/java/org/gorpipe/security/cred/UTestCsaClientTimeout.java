package org.gorpipe.security.cred;

import org.gorpipe.gor.auth.AuthConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * CSA HTTP calls must not wait forever on a slow CSA: every connection has connect and read timeouts, and a timed-out
 * call is not retried (a retry would double the time a request thread is pinned).
 */
public class UTestCsaClientTimeout {

    private ServerSocket server;
    private final List<Socket> accepted = new CopyOnWriteArrayList<>();
    private Thread acceptor;

    @Before
    public void startHungServer() throws IOException {
        // Accepts connections and never answers.
        server = new ServerSocket(0);
        acceptor = new Thread(() -> {
            while (!server.isClosed()) {
                try {
                    accepted.add(server.accept());
                } catch (IOException e) {
                    return;
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    @After
    public void stopServer() throws Exception {
        server.close();
        for (Socket s : accepted) {
            s.close();
        }
        acceptor.join(1000);
    }

    @Test
    public void connectionsHaveDefaultTimeouts() throws IOException {
        HttpURLConnection conn = new HttpJsonServiceClient("http://127.0.0.1:" + server.getLocalPort() + "/")
                .createUrlConnection("api/projects/p");

        Assert.assertEquals(HttpJsonServiceClient.DEFAULT_CONNECT_TIMEOUT_MS, conn.getConnectTimeout());
        Assert.assertEquals(HttpJsonServiceClient.DEFAULT_READ_TIMEOUT_MS, conn.getReadTimeout());
        Assert.assertTrue(conn.getConnectTimeout() > 0);
        Assert.assertTrue(conn.getReadTimeout() > 0);
    }

    @Test
    public void csaServiceUsesConfiguredTimeouts() throws IOException {
        CsaApiService service = new CsaApiService(csaConfig(2000, 3000), mock(AuthConfig.class));
        service.initAuth();

        HttpURLConnection conn = service.createUrlConnection("api/projects/p");

        Assert.assertEquals(2000, conn.getConnectTimeout());
        Assert.assertEquals(3000, conn.getReadTimeout());
    }

    @Test
    public void csaServiceFallsBackToDefaultsForNonPositiveTimeouts() throws IOException {
        // 0 would mean "wait forever" to HttpURLConnection.
        CsaApiService service = new CsaApiService(csaConfig(0, -1), mock(AuthConfig.class));
        service.initAuth();

        HttpURLConnection conn = service.createUrlConnection("api/projects/p");

        Assert.assertEquals(HttpJsonServiceClient.DEFAULT_CONNECT_TIMEOUT_MS, conn.getConnectTimeout());
        Assert.assertEquals(HttpJsonServiceClient.DEFAULT_READ_TIMEOUT_MS, conn.getReadTimeout());
    }

    @Test(timeout = 10_000)
    public void hungCsaTimesOutOnceWithoutRetry() throws Exception {
        int readTimeoutMs = 500;
        CsaApiService service = new CsaApiService(csaConfig(1000, readTimeoutMs), mock(AuthConfig.class));

        long start = System.currentTimeMillis();
        Assert.assertThrows(SocketTimeoutException.class, () -> service.getProject("p"));
        long elapsed = System.currentTimeMillis() - start;

        // One connection proves there was no retry; the time bound only guards against waiting forever.
        Assert.assertTrue("took " + elapsed + " ms", elapsed < 6L * readTimeoutMs);
        Assert.assertEquals(1, accepted.size());
    }

    private CsaAuthConfiguration csaConfig(int connectTimeoutMs, int readTimeoutMs) {
        CsaAuthConfiguration config = mock(CsaAuthConfiguration.class);
        doReturn("http://127.0.0.1:" + server.getLocalPort() + "/").when(config).getAuthApiEndpoint();
        doReturn("csa-user").when(config).getUser();
        doReturn("csa-pass").when(config).getPassword();
        doReturn(connectTimeoutMs).when(config).getConnectTimeoutMs();
        doReturn(readTimeoutMs).when(config).getReadTimeoutMs();
        return config;
    }
}
