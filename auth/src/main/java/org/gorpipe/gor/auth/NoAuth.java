package org.gorpipe.gor.auth;

// TODO:  Are we still supporting NONE.  It would makes things easier to remove it.

import java.util.List;

public class NoAuth extends GorAuth {

    private GorAuthInfo generalAuthInfo;

    protected NoAuth(AuthConfig config, String username, String project, int projectId, String userId, List<String> userRoles, int organizationId) {
        super(config, null);
        this.generalAuthInfo = new GeneralAuthInfo(projectId, project, username, userId, userRoles, organizationId, 0);
        this.securityPolicy = SecurityPolicy.NONE;
    }

    @Override
    public GorAuthInfo getGorAuthInfo(String id) {
        // Id strings are ignored for NoAuth
        return this.generalAuthInfo;
    }

    @Override
    public boolean hasBasicAccess(GorAuthInfo authInfo, String project, String user) {
        return true;
    }

    @Override
    public boolean hasAccessBasedOnRoles(List<String> authRoles, AuthorizationAction authorizationAction, String project) {
        return true;
    }
}
