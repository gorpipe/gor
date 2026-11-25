package org.gorpipe.gor.auth.utils;

import org.gorpipe.util.Strings;
import org.gorpipe.gor.auth.GeneralAuthInfo;
import org.gorpipe.gor.auth.GorAuthInfo;
import org.gorpipe.security.cred.CsaApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsaApiUtils {

    private static final Logger log = LoggerFactory.getLogger(CsaApiUtils.class);

    /**
     * Result of a CSA lookup: the updated auth info, and whether CSA answered every lookup it was asked. "Not found"
     * is an answer; a timeout or any other error is not.
     */
    public record CsaLookup(GorAuthInfo info, boolean complete) {
    }

    /**
     * Add ids from CSA API to the given gor auth, but only if missing and if found.
     * <p>
     * A failed lookup is logged and skipped, except a timeout, which ends the update with an
     * {@link UncheckedIOException}.
     *
     * @param info
     * @return a new auth object with the db id appended.
     */
    public static GorAuthInfo updateWithCsaApi(CsaApiService csaApiService, GorAuthInfo info) {
        return lookupWithCsaApi(csaApiService, info).info();
    }

    /**
     * Same as {@link #updateWithCsaApi}, and also says whether any lookup failed, so a caller can keep a partial
     * result for a shorter time.
     */
    public static CsaLookup lookupWithCsaApi(CsaApiService csaApiService, GorAuthInfo info) {
        int projectId = info.getProjectId();
        String project = info.getProject();
        int organizationId = info.getOrganizationId();
        String userId = info.getUserId();
        String userName = info.getUsername();
        List<String> userRoles = new ArrayList<>(info.getUserRoles());
        Failures failures = new Failures();

        if (projectId < 1 && !Strings.isNullOrEmpty(project)) {
            Map<String, Object> projectMap = getProjectMapOrStopOnTimeout(csaApiService, project, failures);
            projectId = updateProjectId(projectId, projectMap);
            organizationId = updateOrganizationId(projectId, projectMap);
        }

        // CSA looks users up by email, so skip non-email usernames (e.g. service accounts) that it can't know.
        if (Strings.isNullOrEmpty(userId) && Strings.isEmail(userName)) {
            Map<String, Object> userMap = getUserMapByEmail(csaApiService, userName, failures);
            userId = updateUserId(userId, userMap);

            if (userRoles.isEmpty() && !Strings.isNullOrEmpty(project)) {
                List csaUserRoles = getUserRoleList(csaApiService, project, userName, failures);
                updateUserRoles(userRoles, csaUserRoles);
            }
        }

        return new CsaLookup(new GeneralAuthInfo(projectId, project, userName, userId, userRoles,
                organizationId, info.getExpiration()), !failures.any);
    }

    public static int getProjectId(Map projectMap) {
        if (projectMap != null && projectMap.containsKey("id")) {
            return (int) projectMap.get("id");
        } else {
            return -1;
        }
    }

    public static int getOrganizationId(Map projectMap) {
        if (projectMap != null && projectMap.containsKey("organization_id")) {
            return (int) projectMap.get("organization_id");
        } else {
            return -1;
        }
    }

    public static Map<String, Object> getProjectMap(CsaApiService csaApiService, String project) {
        try {
            return getProjectMapOrStopOnTimeout(csaApiService, project, new Failures());
        } catch (UncheckedIOException e) {
            log.warn("Unable to get project from CSA API", e);
            return null;
        }
    }

    private static Map<String, Object> getProjectMapOrStopOnTimeout(CsaApiService csaApiService, String project,
                                                                    Failures failures) {
        Map<String, Object> projectMap = null;
        try {
            projectMap = csaApiService != null ? csaApiService.getProject(project) : null;
        } catch (SocketTimeoutException e) {
            // Stop here: CSA is slow, and the next lookup would wait another full timeout.
            throw new UncheckedIOException(e);
        } catch (IOException e) {
            failures.add(e);
            log.warn("Unable to get project from CSA API", e);
        }
        return projectMap;
    }

    private static Map<String, Object> getUserMapByEmail(CsaApiService csaApiService, String userEmail,
                                                         Failures failures) {
        Map<String, Object> userMap = null;
        try {
            userMap = csaApiService != null ? csaApiService.getUserByEmail(userEmail) : null;
        } catch (SocketTimeoutException e) {
            // Stop here: CSA is slow, and the next lookup would wait another full timeout.
            throw new UncheckedIOException(e);
        } catch (IOException e) {
            failures.add(e);
            log.warn("Unable to get user id from CSA API", e);
        }
        return userMap;
    }

    private static List getUserRoleList(CsaApiService csaApiService, String project, String userEmail,
                                        Failures failures) {
        List userRoleList = null;
        try {
            userRoleList = csaApiService != null ? csaApiService.getUserRoleList(project, userEmail) : null;
        } catch (SocketTimeoutException e) {
            // Stop here: CSA is slow, and the next lookup would wait another full timeout.
            throw new UncheckedIOException(e);
        } catch (IOException e) {
            failures.add(e);
            log.warn("Unable to get user roles from CSA API", e);
        }
        return userRoleList;
    }

    private static int updateProjectId(int projectId, Map<String, Object> projectMap) {
        if (projectId < 1 && projectMap != null && projectMap.containsKey("id")) {
            return (int) projectMap.get("id");
        } else {
            return projectId;
        }
    }

    private static int updateOrganizationId(int organizationId, Map<String, Object> projectMap) {
        if (organizationId < 1 && projectMap != null && projectMap.containsKey("organization_id")) {
            return (int) projectMap.get("organization_id");
        } else {
            return organizationId;
        }
    }

    private static String updateUserId(String userId, Map<String, Object> userMap) {
        if (Strings.isNullOrEmpty(userId) && userMap != null && userMap.containsKey("id")) {
            return String.valueOf(userMap.get("id"));
        } else {
            return userId;
        }
    }

    private static void updateUserRoles(List<String> userRoles, List csaUserRoles) {
        if (userRoles.isEmpty() && hasUserRole(csaUserRoles)) {
            for (Object csaRole : csaUserRoles) {
                LinkedHashMap csaRoleMap = (LinkedHashMap) csaRole;
                userRoles.add((String)csaRoleMap.get("role"));
            }
        }
    }

    private static boolean hasUserRole(List userRoles) {
        return userRoles != null && !userRoles.isEmpty() && userRoles.get(0) instanceof LinkedHashMap && ((LinkedHashMap) userRoles.get(0)).containsKey("role");
    }

    /**
     * Records whether a lookup failed. CSA answering 404 (unknown project or user) is an answer, not a failure.
     */
    private static final class Failures {
        private boolean any;

        void add(IOException e) {
            if (!isNotFound(e)) {
                any = true;
            }
        }

        // HttpURLConnection throws FileNotFoundException for a 404; HttpJsonServiceClient.readInput wraps it.
        private static boolean isNotFound(Throwable e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                if (t instanceof FileNotFoundException) {
                    return true;
                }
            }
            return false;
        }
    }
}
