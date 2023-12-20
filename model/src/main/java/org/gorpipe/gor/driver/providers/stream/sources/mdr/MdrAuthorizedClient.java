package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import org.gorpipe.util.http.client.AuthRequester;
import org.gorpipe.util.http.client.AuthorizedHttpClient;
import org.gorpipe.util.http.utils.KeyAndValue;

import java.time.Duration;
import java.util.List;

public class MdrAuthorizedClient extends AuthorizedHttpClient {
    public MdrAuthorizedClient(AuthRequester authRequester, Duration timeout) {
        super(authRequester, timeout);
    }

    @Override
    protected List<KeyAndValue> getHeaderValues() {
        return List.of(new KeyAndValue("Accept", "application/json"), new KeyAndValue("Content-Type", "application/json"));
    }
}
