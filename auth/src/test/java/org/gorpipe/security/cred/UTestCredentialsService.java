package org.gorpipe.security.cred;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.auth.GeneralAuthInfo;
import org.gorpipe.gor.auth.GorAuthFactory;
import org.gorpipe.gor.auth.GorAuthInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 *
 */
public class UTestCredentialsService {

    private static final Logger log = LoggerFactory.getLogger(UTestCredentialsService.class);

    @Before
    public void setUp() throws Exception {

    }


    @Test
    public void testGetCredentialsGorAuth() throws IOException {
        String credJson = "{\"default_credentials\":[],\"credentials\":[{\"expires\":null,\"owner_id\":null,\"service\":\"s3\",\"lookup_key\":\"csa-test-data\",\"credential_attributes\":{\"key\":\"key\",\"secret\":\"qwertySecret\"},\"owner_type\":\"System\",\"user_default\":false},{\"expires\":null,\"owner_id\":null,\"service\":\"s3\",\"lookup_key\":\"nextcode-qc-data\",\"credential_attributes\":{\"key\":\"qwerty\",\"secret\":\"qwertySecert\"},\"owner_type\":\"System\",\"user_default\":false}]}";
        Map<String, Object> credMap = new ObjectMapper().readValue(credJson, new TypeReference<HashMap<String,Object>>(){});

        CsaSecurityModule csaSecurityModule = new CsaSecurityModule() {
            public CsaAuthConfiguration config() {
                Map<String, String> overrides = new HashMap<>();
                overrides.put(CsaAuthConfiguration.ENDPOINT_KEY, "dummyServer");
                overrides.put(CsaAuthConfiguration.USER_KEY, "credUser");
                overrides.put(CsaAuthConfiguration.PASSWORD_KEY, "credPass");
                CsaAuthConfiguration config = ConfigManager.createConfig(CsaAuthConfiguration.class, overrides);
                return config;
            }
        };

        CsaCredentialService service = csaSecurityModule.service();

        CsaCredentialService serviceMock = spy(service);
        doReturn(credMap).when(serviceMock).jsonGet(any(String.class));

        GorAuthInfo authInfo = new GeneralAuthInfo(1, "proj1", "user1", "2", null, 2, 0);
        BundledCredentials creds = serviceMock.getCredentials(authInfo);

        Assert.assertEquals(credJson, creds.toJson());

    }

    @Test
    public void testGetCredentialsAppSession() throws IOException {
        String credJson = "{\"default_credentials\":[],\"credentials\":[{\"expires\":null,\"owner_id\":null,\"service\":\"s3\",\"lookup_key\":\"csa-test-data\",\"credential_attributes\":{\"key\":\"key\",\"secret\":\"qwertySecret\"},\"owner_type\":\"System\",\"user_default\":false},{\"expires\":null,\"owner_id\":null,\"service\":\"s3\",\"lookup_key\":\"nextcode-qc-data\",\"credential_attributes\":{\"key\":\"qwerty\",\"secret\":\"qwertySecert\"},\"owner_type\":\"System\",\"user_default\":false}]}";
        Map<String, Object> credMap = new ObjectMapper().readValue(credJson, new TypeReference<HashMap<String,Object>>(){});


        CsaSecurityModule csaSecurityModule = new CsaSecurityModule() {
            public CsaAuthConfiguration config() {
                Map<String, String> overrides = new HashMap<>();
                overrides.put(CsaAuthConfiguration.ENDPOINT_KEY, "dummyServer");
                CsaAuthConfiguration config = ConfigManager.createConfig(CsaAuthConfiguration.class, overrides);
                return config;
            }

            public AppSessionUtility appSessionUtility(GorAuthFactory gorAuthFactory) {
                AppSessionUtility mock = mock(AppSessionUtility.class);
                when(mock.getSessionContext("session1"))
                        .thenReturn(new GeneralAuthInfo(1, "proj1", "user1", "2", null, 2, 0));
                return mock;
            }
        };

        CsaCredentialService service = csaSecurityModule.service();

        CsaCredentialService serviceMock = spy(service);
        doReturn(credMap).when(serviceMock).jsonGet(any(String.class));
        doReturn(null).when(serviceMock).getSystemAppSession();

        BundledCredentials creds = serviceMock.getCredentials("session1");

        Assert.assertEquals(credJson, creds.toJson());

    }

    @Test
    public void testGetCredentialsGorAuthRetrySucceed() throws IOException {
        String credJson = "{\"default_credentials\":[],\"credentials\":[{\"expires\":null,\"owner_id\":null,\"service\":\"s3\",\"lookup_key\":\"csa-test-data\",\"credential_attributes\":{\"key\":\"key\",\"secret\":\"qwertySecret\"},\"owner_type\":\"System\",\"user_default\":false},{\"expires\":null,\"owner_id\":null,\"service\":\"s3\",\"lookup_key\":\"nextcode-qc-data\",\"credential_attributes\":{\"key\":\"qwerty\",\"secret\":\"qwertySecert\"},\"owner_type\":\"System\",\"user_default\":false}]}";
        Map<String, Object> credMap = new ObjectMapper().readValue(credJson, new TypeReference<HashMap<String,Object>>(){});

        CsaSecurityModule csaSecurityModule = new CsaSecurityModule() {
            public CsaAuthConfiguration config() {
                Map<String, String> overrides = new HashMap<>();
                overrides.put(CsaAuthConfiguration.ENDPOINT_KEY, "dummyServer");
                overrides.put(CsaAuthConfiguration.USER_KEY, "credUser");
                overrides.put(CsaAuthConfiguration.PASSWORD_KEY, "credPass");
                CsaAuthConfiguration config = ConfigManager.createConfig(CsaAuthConfiguration.class, overrides);
                return config;
            }
        };

        CsaCredentialService service = csaSecurityModule.service();

        CsaCredentialService serviceMock = spy(service);
        doThrow(new IOException()).doReturn(credMap).when(serviceMock).jsonGet(any(String.class));

        GorAuthInfo authInfo = new GeneralAuthInfo(1, "proj1", "user1", "2", null, 2, 0);
        BundledCredentials creds = serviceMock.getCredentials(authInfo);

        Assert.assertEquals(credJson, creds.toJson());

    }

    @Test
    public void testGetCredentialsUnauthorized() throws IOException {
        CsaSecurityModule csaSecurityModule = new CsaSecurityModule() {
            public CsaAuthConfiguration config() {
                Map<String, String> overrides = new HashMap<>();
                overrides.put(CsaAuthConfiguration.ENDPOINT_KEY, "dummyServer");
                overrides.put(CsaAuthConfiguration.USER_KEY, "credUser");
                overrides.put(CsaAuthConfiguration.PASSWORD_KEY, "credPass");
                CsaAuthConfiguration config = ConfigManager.createConfig(CsaAuthConfiguration.class, overrides);
                return config;
            }
        };

        CsaCredentialService service = csaSecurityModule.service();

        CsaCredentialService serviceMock = spy(service);
        doThrow(new IOException()).when(serviceMock).jsonGet(any(String.class));

        GorAuthInfo authInfo = new GeneralAuthInfo(1, "proj1", "user1", "2", null, 2, 0);
        try {
            BundledCredentials creds = serviceMock.getCredentials(authInfo);
            Assert.fail("Should get exception");
        } catch (GorSystemException gse) {
            Assert.assertTrue(gse.getMessage().startsWith("Error getting credentials"));
        }


    }

}
