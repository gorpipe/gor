package org.gorpipe.security.cred;

import org.gorpipe.gor.auth.GorAuthFactory;
import org.gorpipe.gor.auth.GorAuthInfo;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by villi on 11/08/16.
 */
public class AppSessionUtility {
    private final static Logger log = LoggerFactory.getLogger(AppSessionUtility.class);
    private final GorAuthFactory gorAuthFactory;

    public AppSessionUtility(GorAuthFactory gorAuthFactory) {
        this.gorAuthFactory = gorAuthFactory;
    }

    /**
     * Get app session token usable against credentials system with system privileges
     *
     * @param project filter by the given project id else ignore.
     */
    public String getSystemAppSession(String project) {
        return gorAuthFactory.getSystemAppSession(project);
    }

    GorAuthInfo getSessionContext(String appSession) {
        GorAuthInfo gorAuthInfo = gorAuthFactory.getGorAuthInfo(appSession);
        if (gorAuthInfo == null) {
            throw new GorSystemException("Invalid app session key was provided", null);
        }
        return gorAuthInfo;
    }

}
