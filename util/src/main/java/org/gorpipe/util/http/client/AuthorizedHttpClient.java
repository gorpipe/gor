package org.gorpipe.util.http.client;

import org.gorpipe.util.http.utils.KeyAndValue;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@FunctionalInterface
interface HandlerCallback<T,R> {
    R apply(T t) throws IOException, InterruptedException;
}

public abstract class AuthorizedHttpClient {
    private static final int MAX_RETRIES = 3;
    private final AuthRequester authRequester;

    private final HttpClient httpClient;

    public AuthorizedHttpClient(AuthRequester authRequester, Duration timeout) {
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build();
        this.authRequester = authRequester;

    }

    public String getJWT() throws IOException {
        return this.authRequester.getJWT();
    }

    public String post(URI url, String data) throws IOException, InterruptedException {
        return internalPost(url, data, 0);
    }

    public String get(URI url) throws IOException, InterruptedException {
        return internalGet(url, 0);
    }

    private HttpRequest.Builder initHeader(HttpRequest.Builder builder) throws IOException {
        var b = builder.header("Authorization", "Bearer " + getJWT());

        var headerValues = getHeaderValues();
        if (headerValues != null) {
            for (var headerValue : headerValues) {
                b = b.header(headerValue.key(), headerValue.value());
            }
        }

        return b;
    }

    private String internalPost(URI url, String data, int retries) throws InterruptedException {
        if (retries > MAX_RETRIES)
            throw new GorSystemException("Error calling " + url +  ", too many retries", null);

        try {
            HttpRequest request = initHeader(HttpRequest.newBuilder()
                    .uri(url)
                    .POST(HttpRequest.BodyPublishers.ofString(data)))
                    .build();

            var result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(result, retries, (r) -> internalPost(url, data, r));
        } catch(IOException e) {
            throw new GorResourceException("Failed to call", url.toString(), e);
        }
    }

    private String internalGet(URI url, int retries) throws InterruptedException {
        if (retries > MAX_RETRIES)
            throw new GorSystemException("Error calling " + url +  ", too many retries", null);

        try {
            HttpRequest request = initHeader(HttpRequest.newBuilder()
                    .uri(url)
                    .GET())
                    .build();

            var result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(result, retries, (r) -> internalGet(url, r));
        } catch (IOException e) {
            throw new GorResourceException("Failed to call", url.toString(), e);
        }
    }

    private String handleResponse(HttpResponse<String> response, Integer retries, HandlerCallback<Integer, String> callback) throws IOException, InterruptedException {
        if (response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            this.authRequester.invalidateJWT();
            return callback.apply(retries+1);
        } else if(response.statusCode() == HttpURLConnection.HTTP_CLIENT_TIMEOUT) {
            return callback.apply(retries+1);
        } else if(response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new GorResourceException("Document " + response.uri() + " not found.\n" + response.body(), response.uri().toString());
        } else if (response.statusCode() >= HttpURLConnection.HTTP_MULT_CHOICE) {
            throw new GorSystemException("Calling " + response.uri() + " returned with status code: " + response.statusCode() + "\n" + response.body(), null);
        }

        return response.body();
    }

    protected abstract List<KeyAndValue> getHeaderValues();
}
