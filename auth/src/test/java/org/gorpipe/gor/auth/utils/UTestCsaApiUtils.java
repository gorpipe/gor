package org.gorpipe.gor.auth.utils;

import org.gorpipe.gor.auth.GeneralAuthInfo;
import org.gorpipe.gor.auth.GorAuthInfo;
import org.gorpipe.security.cred.CsaApiService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
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

    private static GorAuthInfo authInfo(String username) {
        return new GeneralAuthInfo(0, PROJECT, username, "", null, 0, Long.MAX_VALUE);
    }
}
