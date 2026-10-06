package org.gorpipe.gor.auth.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.gorpipe.gor.auth.GeneralAuthInfo;
import org.gorpipe.gor.auth.GorAuthInfo;
import org.gorpipe.security.cred.CsaApiService;
import org.gorpipe.security.cred.HttpStatusException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CSA user lookups for users that don't exist in CSA (e.g. service accounts such as sequenceminer) must not flood the
 * logs or hit CSA on every request.
 */
public class UTestCsaApiUtils {

    private static final String PROJECT = "test-proj";
    private static final String SERVICE_ACCOUNT = "sequenceminer";

    private Logger logger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;
    private CsaApiService csaApiService;

    @Before
    public void setUp() throws IOException {
        CsaApiUtils.clearUsersNotInCsa();
        logger = (Logger) LoggerFactory.getLogger(CsaApiUtils.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        csaApiService = mock(CsaApiService.class);
        Map<String, Object> projectMap = new LinkedHashMap<>();
        projectMap.put("id", 5);
        projectMap.put("organization_id", 7);
        doReturn(projectMap).when(csaApiService).getProject(PROJECT);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
        CsaApiUtils.clearUsersNotInCsa();
    }

    @Test
    public void userNotInCsaKeepsEmptyUserIdAndRoles() throws IOException {
        doThrow(notFound()).when(csaApiService).getUserByEmail(SERVICE_ACCOUNT);
        doThrow(notFound()).when(csaApiService).getUserRoleList(anyString(), anyString());

        GorAuthInfo info = CsaApiUtils.updateWithCsaApi(csaApiService, authInfo(SERVICE_ACCOUNT));

        Assert.assertEquals(5, info.getProjectId());
        Assert.assertEquals(SERVICE_ACCOUNT, info.getUsername());
        Assert.assertEquals("", info.getUserId());
        Assert.assertTrue(info.getUserRoles().isEmpty());
    }

    @Test
    public void userNotInCsaIsLookedUpOnlyOnce() throws IOException {
        doThrow(notFound()).when(csaApiService).getUserByEmail(SERVICE_ACCOUNT);
        doThrow(notFound()).when(csaApiService).getUserRoleList(anyString(), anyString());

        for (int i = 0; i < 5; i++) {
            CsaApiUtils.updateWithCsaApi(csaApiService, authInfo(SERVICE_ACCOUNT));
        }

        verify(csaApiService, times(1)).getUserByEmail(SERVICE_ACCOUNT);
        verify(csaApiService, never()).getUserRoleList(anyString(), anyString());
    }

    @Test
    public void userNotInCsaLogsOnceWithoutWarnOrStackTrace() throws IOException {
        doThrow(notFound()).when(csaApiService).getUserByEmail(SERVICE_ACCOUNT);
        doThrow(notFound()).when(csaApiService).getUserRoleList(anyString(), anyString());

        for (int i = 0; i < 5; i++) {
            CsaApiUtils.updateWithCsaApi(csaApiService, authInfo(SERVICE_ACCOUNT));
        }

        List<ILoggingEvent> warnings = appender.list.stream().filter(e -> e.getLevel().isGreaterOrEqual(Level.WARN)).toList();
        Assert.assertEquals("Unexpected warnings: " + warnings, 0, warnings.size());

        List<ILoggingEvent> infos = appender.list.stream().filter(e -> e.getLevel() == Level.INFO).toList();
        Assert.assertEquals(1, infos.size());
        Assert.assertNull(infos.get(0).getThrowableProxy());
        Assert.assertTrue(infos.get(0).getFormattedMessage().contains(SERVICE_ACCOUNT));
    }

    @Test
    public void otherCsaErrorsStillWarnAndAreNotCached() throws IOException {
        doThrow(new HttpStatusException(500, "Internal Server Error", null)).when(csaApiService).getUserByEmail(SERVICE_ACCOUNT);

        CsaApiUtils.updateWithCsaApi(csaApiService, authInfo(SERVICE_ACCOUNT));
        CsaApiUtils.updateWithCsaApi(csaApiService, authInfo(SERVICE_ACCOUNT));

        verify(csaApiService, times(2)).getUserByEmail(SERVICE_ACCOUNT);
        Assert.assertTrue(appender.list.stream().anyMatch(e -> e.getLevel() == Level.WARN));
    }

    @Test
    public void existingCsaUserGetsIdAndRoles() throws IOException {
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

    private static HttpStatusException notFound() {
        return new HttpStatusException(404, "Not Found: {\"error\":{\"full_message\":\"Couldn't find user\"}}", null);
    }
}
