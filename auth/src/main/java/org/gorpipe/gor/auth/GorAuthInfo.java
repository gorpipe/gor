package org.gorpipe.gor.auth;

import java.util.List;

// TODO:  Currently this is more of session info object than auth info object, as the getUser and getProject (and the corresponding id methods) give you info on the current session.
public interface GorAuthInfo {

    String getUsername();

    default String getProject() {
        return null;
    }

    List<String> getUserRoles();

    default String getUserRole() {
        return !getUserRoles().isEmpty() ? getUserRoles().get(0) : "";
    }

    /**
     * Get the project id. Project id < 1 means the project id is not set.
     *
     * @return the project id.
     */
    // TODO:  Outdated, should be removed.
    int getProjectId();

    /**
     * Get the user id. User id < 1 means the user id is not set.
     *
     * @return the user id.
     */
    // TODO:  Outdated, should be removed.
    String getUserId();

    /**
     * Get the organization id. Organization id < 1 means the project id is not set.
     *
     * @return the project id.
     */
    // TODO:  Outdated, replaced with Roles.  Organization acceess/mapping should be deefined in JWT.
    //        Need redefine how this is used in db queries.  Probably best to change the views to
    //        only need user and project.
    int getOrganizationId();

    /**
     * Get the expiration timestamp. Expiration = 0 means there is no expiration on this object.
     *
     * @return
     */
    long getExpiration();

    static String getDBScopeString(int projectId, int organizationId) {
        if (projectId <= 0 && organizationId <= 0) {
            return "";
        }
        // There is only a project Id
        else if (organizationId <= 0) {
            return "dbscope=project_id#int#" + projectId;
        }
        // There is only a organization Id
        else if (projectId <= 0) {
            return "dbscope=organization_id#int#" + organizationId;
        }
        // There are both project and organization Ids
        else {
            return "dbscope=project_id#int#" + projectId + ",organization_id#int#" + organizationId;
        }
    }

}
