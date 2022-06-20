package org.gorpipe.security.cred;

import org.gorpipe.base.security.CredentialsParser;
import org.gorpipe.gor.auth.AuthConfig;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.gor.auth.GorAuthFactory;

/**
 * Created by villi on 20/04/16.
 */
public class CsaSecurityModule  {

    private static CsaSecurityModule  csaSecurityModule;

    public static CsaSecurityModule get() {
        if (csaSecurityModule == null) {
            csaSecurityModule = new CsaSecurityModule();
        }
        return csaSecurityModule;
    }

    private  CsaCredentialService service;
    private  CsaApiService apiService;

    private  CsaAuthConfiguration csaAuthConfiguration;

    private  AuthConfig authConfig;

    CsaSecurityModule() {
    }

    /**
     * Get CSA Auth configuration
     */

    public synchronized CsaAuthConfiguration config() {
        if (csaAuthConfiguration == null) {
            csaAuthConfiguration = ConfigManager.createPrefixConfig("gor", CsaAuthConfiguration.class);
        }
        return csaAuthConfiguration;
    }

    public synchronized AuthConfig sessionAuthConfig() {
        if (authConfig == null) {
            authConfig = ConfigManager.createPrefixConfig("gor", AuthConfig.class, System.getenv());
        }
        return authConfig;
    }

    public synchronized AppSessionUtility appSessionUtility(GorAuthFactory gorAuthFactory) {
        return new AppSessionUtility(gorAuthFactory);
    }

    /**
     * Static entry point for code outside the scope of this module
     * Hopefully this can be removed once all components live inside Guice
     * Uses shared validation cache
     */
    public synchronized CsaCredentialService service() {
        if (service == null) {
            if (config().getAuthApiEndpoint() != null) {
                service = new CsaCredentialService(config(), sessionAuthConfig(), new CredentialsParser(),
                        appSessionUtility(new GorAuthFactory(sessionAuthConfig())));
            }
        }
        return service;
    }

    public synchronized CsaApiService apiService() {
        if (apiService == null) {
            if (config().getAuthApiEndpoint() != null) {
                apiService = new CsaApiService(config(), sessionAuthConfig());
            }
        }
        return apiService;
    }
}
