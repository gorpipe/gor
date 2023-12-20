package org.gorpipe.util.http;

import org.gorpipe.util.http.keycloak.KeycloakAuthRequester;
import org.gorpipe.util.http.utils.KeyAndValue;

import java.time.Duration;
import java.util.List;

public class KeycloakMockUserAuthRequester extends KeycloakAuthRequester {

    public boolean initialDataUsed = false;
    public boolean refreshDataUsed = false;

    private String payload;

    public KeycloakMockUserAuthRequester(String server, Duration timeout, String payload) {
        super(server, timeout);
        this.payload = payload;
    }

    @Override
    protected List<KeyAndValue> getRefreshPostData() {
        refreshDataUsed = true;
        initialDataUsed = false;
        return List.of();
    }


    @Override
    protected List<KeyAndValue> getInitialData() {
        initialDataUsed = true;
        refreshDataUsed = false;
        return List.of();
    }

    @Override
    protected String post(String url, List<KeyAndValue> data) {
        return payload;
    }
}
