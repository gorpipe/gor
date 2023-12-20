package org.gorpipe.util.http.keycloak;

import org.gorpipe.util.http.client.AuthRequester;
import org.gorpipe.util.http.utils.KeyAndValue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class KeycloakClientAuthRequester extends KeycloakAuthRequester implements AuthRequester {

    private final String clientId;
    private final String clientSecret;

    public KeycloakClientAuthRequester(String server, Duration timeout, String clientId, String clientSecret) {
        super(server, timeout);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    protected List<KeyAndValue> getInitialData() {
        var postData = new ArrayList<KeyAndValue>();
        postData.add(new KeyAndValue("client_id", clientId));
        postData.add(new KeyAndValue("client_secret", this.clientSecret));
        postData.add(new KeyAndValue("grant_type", "client_credentials"));
        postData.add(new KeyAndValue("scope", "offline_access"));
        return postData;
    }
}
