package org.gorpipe.gor.auth;

import org.gorpipe.security.cred.CsaCredentialService;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.base.security.BundledCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CredentialsHandler {

    private static final Logger log = LoggerFactory.getLogger(CredentialsHandler.class);

    private CredentialsHandler() {
        //not called
    }

    public static String makeSecurityContextString(String securityContext, GorAuthFactory gorAuthFactory, GorAuthInfo info,
                                                   CsaCredentialService csaSecurityService, BundledCredentials bundledCredentialsFromRequest) {
        BundledCredentials bundledCredentials = getBundledCredentials(gorAuthFactory, info, csaSecurityService, securityContext, bundledCredentialsFromRequest);
        return " -Z '" + bundledCredentials.addToSecurityContext(securityContext) + "'";
    }

    private static BundledCredentials getBundledCredentials(GorAuthFactory gorAuthFactory, GorAuthInfo info, CsaCredentialService csaSecurityService, String securityContext, BundledCredentials bundledCredentialsFromRequest) {
        BundledCredentials bundledCredentialsFromCredService = null;
        if (!gorAuthFactory.isNoneSecurityPolicy() && csaSecurityService != null) {
            try {
                bundledCredentialsFromCredService = csaSecurityService.getCredentials(info);
            } catch (GorSystemException e) {
                log.error("Error getting credentials from credentials service.  Will continue with credentials from security context.", e);
            }
        }

        // The credentials from the request take precedence over the ones from the Credential Service
        BundledCredentials bundledCredentials = BundledCredentials.mergeBundledCredentials(bundledCredentialsFromCredService, bundledCredentialsFromRequest);

        if (bundledCredentials == null) {
            bundledCredentials = BundledCredentials.fromSecurityContext(securityContext);
        }

        return bundledCredentials;
    }
}