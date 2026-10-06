package org.gorpipe.security.cred;

import org.gorpipe.gor.auth.AuthConfig;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UTestCsaApiService {

    private CsaApiService service() {
        return spy(new CsaApiService(mock(CsaAuthConfiguration.class), mock(AuthConfig.class)));
    }

    @Test
    public void notFoundIsNotRetried() throws IOException {
        CsaApiService service = service();
        doThrow(new HttpStatusException(404, "Not Found", null)).when(service).jsonGet(anyString());

        HttpStatusException e = Assert.assertThrows(HttpStatusException.class, () -> service.getUserByEmail("sequenceminer"));

        Assert.assertEquals(404, e.getStatusCode());
        verify(service, times(1)).jsonGet(anyString());
        verify(service, never()).initializeAndRetry(anyString());
    }

    @Test
    public void otherErrorsAreRetriedWithNewAuth() throws IOException {
        CsaApiService service = service();
        doThrow(new HttpStatusException(401, "Unauthorized", null)).when(service).jsonGet(anyString());
        Map<String, Object> user = Collections.singletonMap("id", 10);
        doReturn(Collections.singletonMap("user", user)).when(service).initializeAndRetry(anyString());

        Assert.assertEquals(user, service.getUserByEmail("user@email.com"));
        verify(service, times(1)).initializeAndRetry(anyString());
    }
}
