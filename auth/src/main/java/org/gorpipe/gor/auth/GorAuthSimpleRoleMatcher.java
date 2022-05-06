package org.gorpipe.gor.auth;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GorAuthSimpleRoleMatcher {
    public static final String PROJECT_REGEX = "prj:";
    public static final String DELIMITER = ":";
    public static final String WILDCARD = "*";
    public static final String SYSTEM_ADMIN_ROLE = "system_admin";

    public static boolean hasRolebasedSystemAdminAccess(GorAuthInfo authInfo) {
        return matchRoles(authInfo.getUserRoles(), Arrays.asList(SYSTEM_ADMIN_ROLE));
    }

    public static boolean hasRolebasedAccess(GorAuthInfo authInfo, String subject, AuthorizationAction... authorizationActions) {
        return hasRolebasedAccess(authInfo.getUserRoles(), authInfo.getProject(), subject, authorizationActions);
    }

    public static boolean hasRolebasedAccess(List<String> userRoles, String project, String subject, AuthorizationAction... authorizationActions) {

        return matchRoles(userRoles, getRolesThatGiveAccessPatterns(project, subject,  authorizationActions));
    }

    // Includes roles with *
    private static List<String> getRolesThatGiveAccessPatterns(String project, String subject, AuthorizationAction... actions) {
        List<String> allowedRoles = new ArrayList<>();

        // Action based.

        for (AuthorizationAction action : actions) {
            if (!Strings.isNullOrEmpty(project)) {
                allowedRoles.add(PROJECT_REGEX + project + DELIMITER + action.value);

                if (!Strings.isNullOrEmpty(subject)) {
                    allowedRoles.add(PROJECT_REGEX + project + DELIMITER + action.value + DELIMITER + subject);
                    allowedRoles.add(PROJECT_REGEX + project + DELIMITER + action.value + DELIMITER + WILDCARD);
                }

                // Only add those if we have project (even though they work for all projects),
                allowedRoles.add(PROJECT_REGEX + WILDCARD + DELIMITER + action.value);
                allowedRoles.add(PROJECT_REGEX + WILDCARD + DELIMITER + action.value + DELIMITER + subject);
                allowedRoles.add(PROJECT_REGEX + WILDCARD + DELIMITER + action.value + DELIMITER + WILDCARD);
            }
        }

        // Admin and all ops allowed

        if (!Strings.isNullOrEmpty(project)) {
            allowedRoles.add(PROJECT_REGEX + project + DELIMITER + AuthorizationAction.PROJECT_ADMIN.value);
            allowedRoles.add(PROJECT_REGEX + project + DELIMITER + WILDCARD);
        }

        allowedRoles.add(PROJECT_REGEX + WILDCARD + DELIMITER + AuthorizationAction.PROJECT_ADMIN.value);
        allowedRoles.add(PROJECT_REGEX + WILDCARD + DELIMITER + WILDCARD);

        allowedRoles.add(SYSTEM_ADMIN_ROLE);

        return allowedRoles;
    }

    private static boolean matchRoles(List<String> userRoles, List<String> allowAccessRoles) {
        if (userRoles != null && !userRoles.isEmpty() && allowAccessRoles != null && !allowAccessRoles.isEmpty()) {
            for (String role : allowAccessRoles) {
                for (String userRole : userRoles) {
                    if (matchRoles(role, userRole)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchRoles(String role, String authRole) {
        if (Strings.isNullOrEmpty(role) || Strings.isNullOrEmpty(authRole)) {
            return false;
        }
        return role.toLowerCase().equals(authRole.toLowerCase());
    }
}
