package org.gorpipe.util;

import retrofit2.Response;

import java.io.IOException;

public class Rest {
    /**
     * Adjust services base url, adds missing /.
     * @param baseUrl  the base url
     * @return adjusted base url.
     */
    public static String adjustBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return baseUrl;
        }

        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        return baseUrl;
    }

    /**
     * Extract error message from response.
     * @param response   the response, can be null.
     * @return formatted error message.
     */
    public static String extractErrorMessage(Response response) {
        String errorBody;
        if (response == null) {
            return "Empty response";
        }

        try {
            errorBody = response.errorBody().string();
        } catch (IOException e) {
            errorBody = "Could not parse error body!";
        }

        return String.format("Error code: %s message: %s %n%s",
                response.code(),
                response.message(),
                errorBody);
    }

}
