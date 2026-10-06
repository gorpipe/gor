package org.gorpipe.security.cred;

import java.io.IOException;

/**
 * Thrown by {@link HttpJsonServiceClient} when the server responds with an error status.
 */
public class HttpStatusException extends IOException {

    public static final int NOT_FOUND = 404;

    private final int statusCode;

    public HttpStatusException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isNotFound() {
        return statusCode == NOT_FOUND;
    }
}
