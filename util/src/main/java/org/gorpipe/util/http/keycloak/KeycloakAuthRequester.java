package org.gorpipe.util.http.keycloak;

import org.gorpipe.util.http.client.AuthRequester;
import org.gorpipe.util.http.utils.KeyAndValue;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class KeycloakAuthRequester implements AuthRequester {

    private final static int MAX_RETRIES = 3;
    private final String server;

    protected KeycloakTokenResult tokenResult = null;

    private final HttpClient httpClient;


    public KeycloakAuthRequester(String server, Duration timeout) {
        this.server = server;
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build();
    }

    @Override
    public String getJWT() throws IOException {
        var now = LocalDateTime.now();
        var postData = new ArrayList<KeyAndValue>();

        if (accessTokenIsValid(now)) {
            return tokenResult.getAccessToken();
        } else if (refreshTokenIsValid(now)) {
            postData.addAll(getRefreshPostData());
        } else {
            postData.addAll(getInitialData());
        }

        var result = post(getUrl().toString(), postData);
        tokenResult = KeycloakTokenResult.fromJson(result, now);

        return  tokenResult.getAccessToken();
    }

    @Override
    public void invalidateJWT() {
        tokenResult = null;
    }

    public KeycloakTokenResult getTokenResult() {
        return tokenResult;
    }

    private URL getUrl() throws MalformedURLException {
        return new URL(server);
    }

    protected abstract List<KeyAndValue> getInitialData();

    protected List<KeyAndValue> getRefreshPostData() {
        return List.of(
                new KeyAndValue("refresh_token", tokenResult.getRefreshToken()),
                new KeyAndValue("grant_type", "refresh_token"),
                new KeyAndValue("scope", "offline_access")
        );
    }

    private boolean accessTokenIsValid(LocalDateTime now) {
        return tokenResult != null && tokenResult.getExpiresAt().isAfter(now);
    }

    private boolean refreshTokenIsValid(LocalDateTime now) {
        return tokenResult != null && tokenResult.getRefreshExpiresAt().isAfter(now);
    }

    protected String post(String url, List<KeyAndValue> data) {
        return internalPost(URI.create(url), data, 0);
    }

    private String getPostData(List<KeyAndValue> data) {
        var result = new StringBuilder();

        for (var item : data) {
            if (!result.isEmpty()) {
                result.append("&");
            }

            result.append(item.key());
            result.append("=");
            result.append(item.value());
        }

        return result.toString();
    }

    private String internalPost(URI url, List<KeyAndValue> data, int retries) {
        if (retries > MAX_RETRIES)
            throw new GorSystemException("Error calling " + url +  ", too many retries", null);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(url)
                    .POST(HttpRequest.BodyPublishers.ofString(getPostData(data)))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .build();

            var result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(result, retries, (r) -> internalPost(url, data, r));
        } catch(IOException e) {
            throw new GorResourceException("Failed to call", url.toString(), e);
        } catch (InterruptedException e) {
            throw new GorSystemException("Error getting JWT, Operation cancelled", e);
        }
    }

    private String handleResponse(HttpResponse<String> response, Integer retries, Function<Integer, String> callback) {
        if(response.statusCode() == HttpURLConnection.HTTP_CLIENT_TIMEOUT) {
            return callback.apply(retries+1);
        } else if (response.statusCode() >= HttpURLConnection.HTTP_MULT_CHOICE) {
            throw new GorSystemException("Calling " + response.uri() + " returned with status code: " + response.statusCode() + "\n" + response.body(), null);
        }

        return response.body();
    }
}
