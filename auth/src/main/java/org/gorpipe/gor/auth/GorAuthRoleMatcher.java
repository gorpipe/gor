package org.gorpipe.gor.auth;

import com.google.common.base.Strings;
import org.gorpipe.exceptions.GorSecurityException;
import org.gorpipe.util.RegexpUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GorAuthRoleMatcher {
    private static final Logger log = LoggerFactory.getLogger(GorAuthRoleMatcher.class);

    private static final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public static final String PROJECT_REGEX = "prj:";
    public static final String DELIMITER = ":";
    public static final String WILDCARD = "*";
    public static final String SYSTEM_ADMIN_ROLE = "system_admin";

    public static boolean hasRolebasedSystemAdminAccess(GorAuthInfo authInfo) {
        return matchRolePatterns(authInfo.getUserRoles(), List.of(SYSTEM_ADMIN_ROLE));
    }

    /**
     * Check roled based access.  Throws GorSystemException if no access.

     * @param authInfo                  access info
     * @param subject                   subject like, like file for file:write:&lt;subject&gt;
     * @param authorizationActions      the actions we need to have access to, access to any of them will grant access.
     * @throws GorSystemException       if not access.
     */
    public static void needsRolebasedAccess(GorAuthInfo authInfo, String subject, AuthorizationAction... authorizationActions) throws GorSystemException {
        if (!hasRolebasedAccess(authInfo.getUserRoles(), authInfo.getProject(), subject, authorizationActions)) {
            log.warn(String.format("User '%s' in project '%s' does not have access to subject '%s' with any of '%s'",
                    authInfo.getUsername(), authInfo.getProject(), subject,
                    Arrays.stream(authorizationActions).map(a -> a.value).collect(Collectors.joining(","))));
            throw new GorSecurityException(String.format("User '%s' in project '%s' does not have access.",
                    authInfo.getUsername(), authInfo.getProject()), null);
        }
    }

    /**
     * Check roled based access.

     * @param authInfo                  access info
     * @param subject                   subject like, like file for file:write:&lt;subject&gt;
     * @param authorizationActions      the actions we need to have access to, access to any of them will grant access.
     * @return  true if we have access to any of the authorizationActions, otherwise false.
     */
    public static boolean hasRolebasedAccess(GorAuthInfo authInfo, String subject, AuthorizationAction... authorizationActions) {
        return hasRolebasedAccess(authInfo.getUserRoles(), authInfo.getProject(), subject, authorizationActions);
    }

    /**
     * Check roled based access.

     * @param userRoles                 the roles the user has
     * @param project                   the project we are in.
     * @param subject                   subject like, like file for file:write:&lt;subject&gt;
     * @param authorizationActions      the actions we need to have access to, access to any of them will grant access.
     * @return  true if we have access to any of the authorizationActions, otherwise false.
     */
    public static boolean hasRolebasedAccess(List<String> userRoles, String project, String subject, AuthorizationAction... authorizationActions) {
        return matchRolePatterns(userRoles, getRolesThatGiveAccess(project, subject, authorizationActions));
    }

    // Includes exact roles.
    static List<String> getRolesThatGiveAccess(String project, String subject, AuthorizationAction... actions) {
        List<String> allowedRoles = new ArrayList<String>();

        // Action based.

        for (AuthorizationAction action : actions) {
            if (!Strings.isNullOrEmpty(project)) {
                allowedRoles.add(PROJECT_REGEX + project + DELIMITER + action.value);

                if (!Strings.isNullOrEmpty(subject)) {
                    allowedRoles.add(PROJECT_REGEX + project + DELIMITER + action.value + DELIMITER + subject);
                }
            }

            // User data special
            if (action == AuthorizationAction.WRITE && subject != null && subject.startsWith("user_data/")) {
                allowedRoles.add(PROJECT_REGEX + project + DELIMITER + AuthorizationAction.WRITE_TO_USER_DATA.value);
            }
        }

        // TODO:  We might want to skip these admin roles as these particular roles are composite roles and
        // should include the action.  Leaving it in here for now.  We might want to use non-composite admin roles
        // hrere.

        // Admin and all ops allowed

        if (!Strings.isNullOrEmpty(project)) {
            allowedRoles.add(PROJECT_REGEX + project + DELIMITER + AuthorizationAction.PROJECT_ADMIN.value);
        }

        allowedRoles.add(SYSTEM_ADMIN_ROLE);

        return allowedRoles;
    }

    static boolean matchRolePatterns(List<String> userRolesPatterns, List<String> allowAccessRoles) {
        if (userRolesPatterns != null && !userRolesPatterns.isEmpty() && allowAccessRoles != null && !allowAccessRoles.isEmpty()) {
            for (String patternString : userRolesPatterns) {
                if (!Strings.isNullOrEmpty(patternString)) {
                    Pattern pattern = patternCache.computeIfAbsent(patternString, p -> RegexpUtils.compilePattern(p));
                    for (String role : allowAccessRoles) {
                        if (!Strings.isNullOrEmpty(role) && matchRoles(pattern, role)) {
                            return true;
                        }
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