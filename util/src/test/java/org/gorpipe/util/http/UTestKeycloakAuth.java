package org.gorpipe.util.http;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.test.IntegrationTests;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.time.Duration;

import static java.lang.Thread.sleep;

public class UTestKeycloakAuth {
    @Test
    @Ignore("Fails spor adically")
    public void testFullAuthCycle() throws IOException, InterruptedException {
        var payload = "{\"access_token\":\"mocked\",\"expires_in\":1,\"refresh_expires_in\":2,\"refresh_token\":\"mocked\",\"token_type\":\"bearer\",\"not-before-policy\":0,\"session_state\":\"mocked\",\"scope\":\"mocked\"}";

        var requester = new KeycloakMockUserAuthRequester(
            "https://gdb-demo.genedx.com",
                Duration.ofMinutes(10),
                payload);

        var jwt = requester.getJWT();

        Assert.assertEquals("mocked", jwt);
        Assert.assertTrue(requester.initialDataUsed);
        Assert.assertFalse(requester.refreshDataUsed);

        sleep(1000);

        jwt = requester.getJWT();
        Assert.assertEquals("mocked", jwt);
        Assert.assertFalse(requester.initialDataUsed);
        Assert.assertTrue(requester.refreshDataUsed);

        sleep(3000);
        jwt = requester.getJWT();
        Assert.assertTrue(requester.initialDataUsed);
        Assert.assertFalse(requester.refreshDataUsed);
    }

    @Test
    public void testNoRefreshToken() throws IOException, InterruptedException {
        var payload = "{\"access_token\":\"mocked\",\"expires_in\":1,\"token_type\":\"bearer\",\"not-before-policy\":0,\"session_state\":\"mocked\",\"scope\":\"mocked\"}";

        var requester = new KeycloakMockUserAuthRequester(
                "https://gdb-demo.genedx.com",
                Duration.ofMinutes(10),
                payload);

        var jwt = requester.getJWT();
        Assert.assertTrue(requester.initialDataUsed);
        sleep(2000);
        jwt = requester.getJWT();
        Assert.assertTrue(requester.initialDataUsed);
    }

    @Test
    public void testInvalidTokenPayload() {
        var payload = "{\"token_type\":\"bearer\",\"not-before-policy\":0,\"session_state\":\"mocked\",\"scope\":\"mocked\"}";

        var requester = new KeycloakMockUserAuthRequester(
                "https://gdb-demo.genedx.com",
                Duration.ofMinutes(10),
                payload);

        Assert.assertThrows(GorSystemException.class, requester::getJWT);
    }
}