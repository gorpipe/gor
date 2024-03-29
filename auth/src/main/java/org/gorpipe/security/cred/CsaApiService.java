package org.gorpipe.security.cred;

import org.gorpipe.gor.auth.AuthConfig;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Call csa api to get resources stored in csa system.
 */
public class CsaApiService extends CsaBaseService {
    /**
     * Create csa api service object
     *
     * @param config configuration
     */
    public CsaApiService(CsaAuthConfiguration config, AuthConfig authConfig) {
        super(config, authConfig);
    }

    public Map<String, Object> getAppSession(String id) throws IOException {
        return getApiResource("app_sessions", "app_session", id);
    }

    public Map<String, Object> getProject(String id) throws IOException {
        return getApiResource("projects", "project", id);
    }

    public Map<String, Object> getOrganization(String id) throws IOException {
        return getApiResource("organizations", "organization", id);
    }

    public Map<String, Object> getUserByEmail(String id) throws IOException {
        return getApiResource("users/by_email", "user", id);
    }

    public List getUserRoleList(String project, String userEmail) throws IOException {
        String path = "api/projects/" + project + "/users/" + userEmail;
        Map<String, Object> result = getApiResults(path);
        return (List) result.get("roles");
    }

    private Map<String, Object> getApiResource(String resource, String field, String id) throws IOException {
        String path = "api/" + resource + "/" + id;
        Map<String, Object> result = getApiResults(path);
        return (Map<String, Object>) result.get(field);
    }

    private Map<String, Object> getApiResults(String path) throws IOException {
        Map<String, Object> result;
        try {
            result = jsonGet(path);
        } catch (IOException ioe) {
            // Retry once with new Auth.
            result = initializeAndRetry(path);
        }
        return result;
    }
}
