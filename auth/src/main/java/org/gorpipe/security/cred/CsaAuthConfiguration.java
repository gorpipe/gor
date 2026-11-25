package org.gorpipe.security.cred;

import org.gorpipe.base.config.annotations.Documentation;
import org.aeonbits.owner.Config;

/**
 * Configuration for CSA auth service
 * <p>
 * Created by villi on 02/04/16.
 */
public interface CsaAuthConfiguration extends Config {
    String ENDPOINT_KEY = "csa.auth.service.endpoint";
    String USER_KEY = "csa.auth.service.user";
    String PASSWORD_KEY = "csa.auth.service.password";
    String CONNECT_TIMEOUT_KEY = "csa.auth.service.connect.timeout.ms";
    String READ_TIMEOUT_KEY = "csa.auth.service.read.timeout.ms";

    String[] KEYS = {ENDPOINT_KEY, USER_KEY, PASSWORD_KEY};

    /**
     * Api endpoint prefix - including protocol and trailing slash (e.g. https://dev.nextcode.com/csa/)
     */
    @Key(ENDPOINT_KEY)
    @Documentation("Api endpoint for credential service including protocol and trailing slash - e.g. https://dev.nextcode.com/csa/")
    String getAuthApiEndpoint();

    @Key(USER_KEY)
    @Documentation("Username to authenticate against credential service. Not needed if system can authenticate itself.")
    String getUser();

    @Key(PASSWORD_KEY)
    @Documentation("Password to authenticate  against credential service. Not needed if system can authenticate itself.")
    String getPassword();

    @Key(CONNECT_TIMEOUT_KEY)
    @DefaultValue("5000")
    @Documentation("Connect timeout in milliseconds for calls to the credential service. Non-positive values use the default.")
    int getConnectTimeoutMs();

    @Key(READ_TIMEOUT_KEY)
    @DefaultValue("10000")
    @Documentation("Read timeout in milliseconds for calls to the credential service. Non-positive values use the default.")
    int getReadTimeoutMs();
}
