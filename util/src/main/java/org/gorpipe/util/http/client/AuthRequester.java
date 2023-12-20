package org.gorpipe.util.http.client;

import java.io.IOException;


public interface AuthRequester {
    String getJWT() throws IOException;

    void invalidateJWT();
}
