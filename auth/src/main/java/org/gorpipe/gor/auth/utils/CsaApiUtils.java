package org.gorpipe.gor.auth.utils;

import com.google.common.base.Strings;
import org.gorpipe.gor.auth.GeneralAuthInfo;
import org.gorpipe.gor.auth.GorAuthInfo;
import org.gorpipe.security.cred.CsaApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsaApiUtils {

    private static final Logger log = LoggerFactory.getLogger(CsaApiUtils.class);

    /**
     * Add ids from CSA API to the given gor auth, but only if missing and if found.
     *
     * @param info
     * @return a new auth object with the db id appended.
     */
    public static GorAuthInfo updateWithCsaApi(CsaApiService csaApiService, GorAuthInfo info) {
        int projectId = info.getProjectId();
        String project = info.getProject();
        int organizationId = info.getOrganizationId();
        String userId = info.getUserId();
        String userName = info.getUsername();
        List<String> userRoles = new ArrayList<>(info.getUserRoles());

        if (projectId < 1 && !Strings.isNullOrEmpty(project)) {
            Map<String, Object> projectMap = getProjectMap(csaApiService, project);
            projectId = updateProjectId(projectId, projectMap);
            organizationId = updateOrganizationId(projectId, projectMap);
        }

        if (Strings.isNullOrEmpty(userId) && !Strings.isNullOrEmpty(userName)) {
            Map<String, Object> userMap = getUserMapByEmail(csaApiService, userName);
            userId = updateUserId(userId, userMap);

            if (userRoles.isEmpty() && !Strings.isNullOrEmpty(project) && !Strings.isNullOrEmpty(userName)) {
                List csaUserRoles = getUserRoleList(csaApiService, project, userName);
                updateUserRoles(userRoles, csaUserRoles);
            }
        }

        return new GeneralAuthInfo(projectId, project, userName, userId, userRoles,
                organizationId, info.getExpiration());
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
        Map<String, Object> projectMap = null;
        try {
            projectMap = csaApiService != null ? csaApiService.getProject(project) : null;
        } catch (IOException e) {
            log.warn("Unable to get project from CSA API", e);
        }
        return projectMap;
    }

    private static Map<String, Object> getUserMapByEmail(CsaApiService csaApiService, String userEmail) {
        Map<String, Object> userMap = null;
        try {
            userMap = csaApiService != null ? csaApiService.getUserByEmail(userEmail) : null;
        } catch (IOException e) {
            log.warn("Unable to get user id from CSA API", e);
        }
        return userMap;
    }

    private static List getUserRoleList(CsaApiService csaApiService, String project, String userEmail) {
        List userRoleList = null;
        try {
            userRoleList = csaApiService != null ? csaApiService.getUserRoleList(project, userEmail) : null;
        } catch (IOException e) {
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
}
