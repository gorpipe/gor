package org.gorpipe.security.cred;

import com.google.api.client.util.Strings;
import org.gorpipe.gor.auth.AuthConfig;
import org.gorpipe.gor.auth.GorAuthFactory;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.util.db.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Call csa auth api to get credentials and auth details stored in csa system.
 */
abstract class CsaBaseService extends HttpJsonServiceClient {
    private final static Logger log = LoggerFactory.getLogger(CsaBaseService.class);

    private CsaAuthConfiguration config;
    private AuthConfig authConfig;

    boolean initializedAuth = false;

    /**
     * Create csa credentials service object
     *
     * @param config configuration
     */
    public CsaBaseService(CsaAuthConfiguration config, AuthConfig authConfig) {
        super();
        this.config = config;
        this.authConfig = authConfig;
    }

    //todo retry initialization in preemptive fashion
    //todo part of the initialization no requiring remote access should not happen lazily (like isConfigured() test) but fail fast instead.
    synchronized void initAuth() {
        if (!initializedAuth) {
            log.debug("Initializing with config: host: {}, user: {}, pass: {}", config.getAuthApiEndpoint(), config.getUser(), config.getPassword());
            if (isConfigured()) {
                setHttpPath(config.getAuthApiEndpoint());
                if (config.getUser() != null) {
                    setBasicAuthentication(config.getUser(), config.getPassword());
                } else {
                    String token = getSystemAppSession();
                    if (token != null) {
                        setParameterAuthentication("app_session", token);
                    } else {
                        log.warn("CSA Credentialservice active with no authentication information");
                    }
                }
            } else {
                throw new GorSystemException("No configuration - CSA Credentialservice not active", null);
            }
            initializedAuth = true;
        }
    }

    Map<String, Object> initializeAndRetry(String path) throws IOException {
        initializedAuth = false;
        initAuth();
        return jsonGet(path);
    }

    public String getSystemAppSession() {
        try (Connection conn = Db.getPool(authConfig.sessioncheckerDbUrl(), authConfig.sessioncheckerUsername(), authConfig.sessioncheckerPassword()).getConnection()) {
            if (conn == null) {
                throw new SQLException("Unable to get a proper connection to database at " + authConfig.sessioncheckerDbUrl() + " with user " + authConfig.sessioncheckerUsername());
            }
            return GorAuthFactory.getSystemAppSession(conn, null);
        } catch (SQLException e) {
            throw new GorSystemException("Error reading project id from the db!", e);
        }
    }

    boolean isConfigured() {
        return !Strings.isNullOrEmpty(config.getAuthApiEndpoint());
    }


}
