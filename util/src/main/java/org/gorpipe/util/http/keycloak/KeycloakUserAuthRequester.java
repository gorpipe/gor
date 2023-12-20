package org.gorpipe.util.http.keycloak;

import org.gorpipe.util.http.client.AuthRequester;
import org.gorpipe.util.http.utils.KeyAndValue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class KeycloakUserAuthRequester extends KeycloakAuthRequester implements AuthRequester {

    private final String clientId;
    private final String username;
    private final String password;

    public KeycloakUserAuthRequester(String server, Duration timeout, String clientId, String username, String password) {
        super(server, timeout);
        this.clientId = clientId;
        this.username = username;
        this.password = password;
    }

    @Override
    protected List<KeyAndValue> getInitialData() {
        var postData = new ArrayList<KeyAndValue>();
        postData.add(new KeyAndValue("client_id", clientId));
        postData.add(new KeyAndValue("username", this.username));
        postData.add(new KeyAndValue("password", this.password));
        postData.add(new KeyAndValue("grant_type", "password"));
        postData.add(new KeyAndValue("scope", "offline_access"));
        return postData;
    }
}
