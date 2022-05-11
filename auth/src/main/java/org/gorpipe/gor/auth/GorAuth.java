package org.gorpipe.gor.auth;

import com.google.common.base.Strings;
import org.gorpipe.gor.auth.utils.CsaApiUtils;
import org.gorpipe.security.cred.CsaApiService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class GorAuth implements AutoCloseable {

    public static final String REALM_ACCESS = "realm_access";
    public static final String ROLES = "roles";
    private static final String USER_DATA = "user_data";



    protected AuthConfig config;
    protected SecurityPolicy securityPolicy;
    protected CsaApiService csaApiService;

    private static final Logger log = LoggerFactory.getLogger(PlainAuth.class);


    /**
     * Constructor.
     *
     * @param config the auth config to use.
     */
    public GorAuth(AuthConfig config, CsaApiService csaApiService) {
        this.config = config;
        this.csaApiService = csaApiService;
    }

    /**
     * Create a new auth info object from the session key.
     *
     * @param sessionKey the session key to use.
     * @return new auth info object based on the session key.
     */
    abstract public GorAuthInfo getGorAuthInfo(String sessionKey);

    /**
     * Create a new auth info object from the jwt.
     *
     * @param project the project.
     * @param jwt     the jwt to use.
     * @return new auth info object based on the session key.
     */
    public GorAuthInfo getGorAuthInfo(String project, JsonWebToken jwt) {
        return null;
    }

    /**
     * Get the security policy.
     *
     * @return the security policy.
     */
    public SecurityPolicy getSecurityPolicy() {
        return this.securityPolicy;
    }

    public boolean hasAccessBasedOnRoles(List<String> authRoles, AuthorizationAction authorizationAction, String project) {
        return GorAuthRoleMatcher.hasRolebasedAccess(authRoles, project, null, authorizationAction);
    }

    public void close() {
        // Does nothing
    }

    /**
     * Update GOR Auth Info by trying to get the missing fields from the CSA API if available.
     *
     * @param info
     * @return
     */

    public GorAuthInfo updateGorAuthInfo(GorAuthInfo info) {
        if (csaApiService == null) {
            log.warn("Csa Api Service was null, therefore not updating gor auth info");
            return info;
        } else if (!config.updateAuthInfoPolicy().equals(SecurityPolicy.CSA.name())) {
            log.warn("Update auth info policy was not CSA, therefore not updating gor auth info");
            return info;
        } else {
            try {
                return CsaApiUtils.updateWithCsaApi(csaApiService, info);
            } catch (Exception e) {
                log.warn("Could not update gor auth info from CSA because of an error.", e);
                return info;
            }
        }
    }

    public static boolean validateUserProject(String user, String project) {
        return !Strings.isNullOrEmpty(user) && !Strings.isNullOrEmpty(project);
    }

    boolean hasBasicAccess(GorAuthInfo authInfo, String project, String user) {
        return authInfo.getProject() != null && authInfo.getProject().equals(project) && authInfo.getUsername() != null && authInfo.getUsername().equals(user);
    }

    boolean hasReadAccess(GorAuthInfo gorAuthInfo, String project) {
        if (!config.userRolesFromToken()) {
            throw new GorSystemException("User missing since roles are not retrieved from token", null);
        }
        return hasAccessBasedOnRoles(gorAuthInfo.getUserRoles(), AuthorizationAction.READ, project);
    }

    boolean hasQueryAccess(GorAuthInfo gorAuthInfo, String project) {
        if (!config.userRolesFromToken()) {
            throw new GorSystemException("User missing since roles are not retrieved from token", null);
        }
        return hasAccessBasedOnRoles(gorAuthInfo.getUserRoles(), AuthorizationAction.QUERY, project);
    }

    boolean hasQueryAccess(GorAuthInfo gorAuthInfo, String project, String user) {
        if (!config.userRolesFromToken()) {
            return hasBasicAccess(gorAuthInfo, project, user);
        }
        return hasAccessBasedOnRoles(gorAuthInfo.getUserRoles(), AuthorizationAction.QUERY, project);
    }

    boolean hasWriteAccess(String desiredFileName, GorAuthInfo gorAuthInfo, String project) {
        if (!config.userRolesFromToken()) {
            throw new GorSystemException("User missing since roles are not retrieved from token", null);
        }
        if (!isInProject(desiredFileName, project)) {
            return false;
        }
        if (startsWithUserData(desiredFileName, project)) {
            return hasAccessBasedOnRoles(gorAuthInfo.getUserRoles(), AuthorizationAction.WRITE_TO_USER_DATA, project)
                    || hasAccessBasedOnRoles(gorAuthInfo.getUserRoles(), AuthorizationAction.WRITE, project);
        } else {
            return hasAccessBasedOnRoles(gorAuthInfo.getUserRoles(), AuthorizationAction.WRITE, project);
        }
    }

    boolean hasWriteAccess(String desiredFileName, GorAuthInfo gorAuthInfo, String project, String user) {
        if (!config.userRolesFromToken()) {
            return hasBasicAccess(gorAuthInfo, project, user);
        }
        return hasWriteAccess(desiredFileName, gorAuthInfo, project);
    }

    boolean hasLordSubmitAccess(GorAuthInfo gorAuthInfo, String project, String user) {
        if (!config.userRolesFromToken()) {
            return hasBasicAccess(gorAuthInfo, project, user);
        }
        return hasAccessBasedOnRoles(gorAuthInfo.getUserRoles(), AuthorizationAction.SUBMIT_TO_LORD, project);
    }

    private boolean startsWithUserData(String desiredFileName, String project) {
        Path userDataPath = Paths.get(config.projectRoot()).resolve(project).resolve(USER_DATA).normalize();
        Path desiredPath = Paths.get(config.projectRoot()).resolve(project).resolve(desiredFileName).normalize();
        return desiredPath.startsWith(userDataPath);
    }

    private boolean isInProject(String desiredFileName, String project) {
        Path projectPath = Paths.get(config.projectRoot()).resolve(project).normalize();
        Path desiredPath = Paths.get(config.projectRoot()).resolve(project).resolve(desiredFileName).normalize();
        return desiredPath.startsWith(projectPath);
    }
}
