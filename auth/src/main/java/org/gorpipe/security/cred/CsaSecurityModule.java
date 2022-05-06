package org.gorpipe.security.cred;

import com.google.inject.*;
import org.gorpipe.gor.auth.AuthConfig;
import org.gorpipe.base.config.ConfigManager;

/**
 * Created by villi on 20/04/16.
 */
public class CsaSecurityModule extends AbstractModule {

    private static CsaCredentialService service;
    private static CsaApiService apiService;

    public CsaSecurityModule() {
    }

    @Override
    protected void configure() {
    }

    /**
     * Get CSA Auth configuration
     */
    @Provides
    @Singleton
    public CsaAuthConfiguration config() {
        return ConfigManager.createPrefixConfig("gor", CsaAuthConfiguration.class);
    }

    @Provides
    @Singleton
    public AuthConfig sessionAuthConfig() {
        return ConfigManager.createPrefixConfig("gor", AuthConfig.class, System.getenv());
    }

    /**
     * Static entry point for code outside the scope of this module
     * Hopefully this can be removed once all components live inside Guice
     * Uses shared validation cache
     */
    public synchronized static CsaCredentialService service() {
        if (service == null) {
            Injector injector = Guice.createInjector(new CsaSecurityModule());
            if (injector.getInstance(CsaAuthConfiguration.class).getAuthApiEndpoint() != null) {
                service = injector.getInstance(CsaCredentialService.class);
            }
        }
        return service;
    }

    public synchronized static CsaApiService apiService() {
        if (apiService == null) {
            Injector injector = Guice.createInjector(new CsaSecurityModule());
            if (injector.getInstance(CsaAuthConfiguration.class).getAuthApiEndpoint() != null) {
                apiService = injector.getInstance(CsaApiService.class);
            }
        }
        return apiService;
    }
}
