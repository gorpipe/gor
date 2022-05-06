package org.gorpipe.gor.auth;

import com.google.common.base.Strings;
import org.gorpipe.util.RegexpUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GorAuthRoleMatcher {
    private static final Logger log = LoggerFactory.getLogger(GorAuthRoleMatcher.class);

    private static Map<String, Pattern> patternCache = new HashMap<>();

    public static final String PROJECT_REGEX = "prj:";
    public static final String DELIMITER = ":";
    public static final String WILDCARD = "*";
    public static final String SYSTEM_ADMIN_ROLE = "system_admin";

    public static boolean hasRolebasedSystemAdminAccess(GorAuthInfo authInfo) {
        return matchRolePatterns(authInfo.getUserRoles(), Arrays.asList(SYSTEM_ADMIN_ROLE));
    }

    public static void needsRolebasedAccess(GorAuthInfo authInfo, String subject, AuthorizationAction... authorizationActions) {
        if (!hasRolebasedAccess(authInfo.getUserRoles(), authInfo.getProject(), subject, authorizationActions)) {
            log.warn(String.format("User '%s' in project '%s' does not have access to subject '%s' with any of '%s'",
                    authInfo.getUsername(), authInfo.getProject(), subject,
                    Arrays.stream(authorizationActions).map(a -> a.value).collect(Collectors.joining(","))));
            throw new GorSystemException(String.format("User '%s' in project '%s' does not have access.",
                    authInfo.getUsername(), authInfo.getProject()), null);
        }
    }

    public static boolean hasRolebasedAccess(GorAuthInfo authInfo, String subject, AuthorizationAction... authorizationActions) {
        return hasRolebasedAccess(authInfo.getUserRoles(), authInfo.getProject(), subject, authorizationActions);
    }

    public static boolean hasRolebasedAccess(List<String> userRoles, String project, String subject, AuthorizationAction... authorizationActions) {
        return matchRolePatterns(userRoles, getRolesThatGiveAccess(project, subject, authorizationActions));
    }

    // Includes exact roles.
    private static List<String> getRolesThatGiveAccess(String project, String subject, AuthorizationAction... actions) {
        List<String> allowedRoles = new ArrayList<String>();

        // Action based.

        for (AuthorizationAction action : actions) {
            if (!Strings.isNullOrEmpty(project)) {
                allowedRoles.add(PROJECT_REGEX + project + DELIMITER + action.value);

                if (!Strings.isNullOrEmpty(subject)) {
                    allowedRoles.add(PROJECT_REGEX + project + DELIMITER + action.value + DELIMITER + subject);
                }
            }
        }

        // Admin and all ops allowed

        if (!Strings.isNullOrEmpty(project)) {
            allowedRoles.add(PROJECT_REGEX + project + DELIMITER + AuthorizationAction.PROJECT_ADMIN.value);
        }

        allowedRoles.add(SYSTEM_ADMIN_ROLE);

        return allowedRoles;
    }

    private static boolean matchRolePatterns(List<String> userRolesPatterns, List<String> allowAccessRoles) {
        if (userRolesPatterns != null && !userRolesPatterns.isEmpty() && allowAccessRoles != null && !allowAccessRoles.isEmpty()) {
            for (String patternString : userRolesPatterns) {
                Pattern pattern = patternCache.computeIfAbsent(patternString, p -> RegexpUtils.compilePattern(p));
                for (String role : allowAccessRoles) {
                    if (matchRoles(pattern, role)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchRoles(Pattern pattern, String role) {
        if (Strings.isNullOrEmpty(pattern.pattern()) || Strings.isNullOrEmpty(role)) {
            return false;
        }
        return pattern.matcher(role).matches();
    }
}