package org.gorpipe.gor.auth;

public enum AuthorizationAction {

    /**
     * Read all project data
     */
    READ("file:read"),
    /**
     * Query project
     */
    QUERY("query"),

    /**
     * Write all project data
     */
    WRITE("file:write"),

    /**
     * Write link files
     */
    WRITE_LINK("file:write_link"),

    /**
     * Write to user data
     *
     * TODO:  Should remove this action and instead of creating prj:amelia:file:write:user_data in Keycloak we should
     *        create prj:amelia:file:write:user_data/* which allow write access in any file in user_data.
     */
    WRITE_TO_USER_DATA("file:write:user_data"),

    /**
     * Submit Lord jobs
     */
    SUBMIT_TO_LORD("lord:submit"),

    /**
     * Project admin
     */
    PROJECT_ADMIN("project_admin");

    public final String value;

    AuthorizationAction(String value) {
        this.value = value;
    }
}