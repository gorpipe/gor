package org.gorpipe.gor.auth;

import org.gorpipe.base.config.annotations.Documentation;
import org.aeonbits.owner.Config;

public interface AuthConfig extends Config {

    String SECURITY_POLICY = "GOR_SECURITY_POLICY";

    @Documentation("")
    @Key(SECURITY_POLICY)
    @DefaultValue("CSA")
    String[] securityPolicies();

    String SESSIONCHECKER_DBURL = "RDA_SESSION_CHECKER_DBURL";

    @Documentation("")
    @Key(SESSIONCHECKER_DBURL)
    String sessioncheckerDbUrl();

    String SESSIONCHECKER_USERNAME = "RDA_SESSION_CHECKER_USERNAME";

    @Documentation("")
    @Key(SESSIONCHECKER_USERNAME)
    @DefaultValue("session_checker")
    String sessioncheckerUsername();

    String SESSIONCHECKER_USERROLE = "RDA_SESSION_CHECKER_USERROLE";

    @Documentation("")
    @Key(SESSIONCHECKER_USERROLE)
    @DefaultValue("")
    String sessioncheckerUserrole();

    String SESSIONCHECKER_PASSWORD = "RDA_SESSION_CHECKER_PASSWORD";

    @Documentation("")
    @Key(SESSIONCHECKER_PASSWORD)
    String sessioncheckerPassword();

    String PUBLIC_AUTHORIZATION_KEY = "PUBLIC_AUTHORIZATION_KEY";

    @Documentation("")
    @Key(PUBLIC_AUTHORIZATION_KEY)
    @DefaultValue("")
    String publicAuthorizationKey();

    String PLATFORM_USER_KEY = "PLATFORM_USER_KEY";

    @Documentation("")
    @Key(PLATFORM_USER_KEY)
    @DefaultValue("preferred_username")
    String getPlatformUserKey();

    String UPDATE_AUTH_INFO_POLICY = "UPDATE_AUTH_INFO_POLICY";

    @Documentation("")
    @Key(UPDATE_AUTH_INFO_POLICY)
    @DefaultValue("CSA")
    String updateAuthInfoPolicy();

    String USE_ROLES_FROM_TOKEN = "USE_ROLES_FROM_TOKEN";

    @Documentation("")
    @Key(USE_ROLES_FROM_TOKEN)
    @DefaultValue("false")
    boolean userRolesFromToken();

    String GOR_PROJECTS_PATH = "GOR_PROJECTS_PATH";

    @Documentation("Root for all the project directories.")
    @Key(GOR_PROJECTS_PATH)
    @DefaultValue("")
    String projectRoot();

}
