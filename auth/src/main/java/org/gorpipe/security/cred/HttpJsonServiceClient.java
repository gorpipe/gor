package org.gorpipe.security.cred;

import org.gorpipe.util.JsonUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Common json/http service client
 * <p>
 * Created by villi on 30/03/16.
 */
public class HttpJsonServiceClient {
    private static final Logger log = LoggerFactory.getLogger(HttpJsonServiceClient.class);

    private String httpPath;
    private int resultLimit = DEFAULT_RESULT_LIMIT;
    private String user;
    private String password;
    private String bearerToken;
    protected static final int DEFAULT_RESULT_LIMIT = 128 * 1024 * 1024; // 128MB
    private String authPar;
    private String authVal;


    public HttpJsonServiceClient() {

    }

    public HttpJsonServiceClient(String httpPath) {
        this.httpPath = httpPath;
    }

    /**
     * Set http path
     */
    public void setHttpPath(String httpPath) {
        this.httpPath = httpPath;
    }


    public Map<String, Object> jsonGet(String path) throws IOException {
        log.debug("jsonGet on path {}", path);
        return readJsonInput(createUrlConnection(path));
    }

    public Map<String, Object> jsonPost(String path, Map<String, Object> data) throws IOException {
        return jsonPost(path, JsonUtils.toJson(data));
    }

    public Map<String, Object> jsonPost(String path, String json) throws IOException {
        log.debug("jsonPost on path {} with payload {}", path, json);
        HttpURLConnection conn = createUrlConnection(path);
        try {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }

            return readJsonInput(conn);
        } finally {
            conn.disconnect();
        }
    }

    protected Map<String, Object> readJsonInput(HttpURLConnection conn) throws IOException {
        String data = readInput(conn);
        log.debug("Response data: {}", data);
        try {
            return JsonUtils.parseJson(data);
        } catch (IllegalArgumentException e) {
            throw new IOException("Cannot parse as json - response body is:" + data);
        } finally {
            conn.disconnect();
        }
    }

    protected HttpURLConnection createUrlConnection(String path) throws IOException {
        final HttpURLConnection http = (HttpURLConnection) new URL(makeUrl(path)).openConnection();
        http.setInstanceFollowRedirects(true);
        if (user != null) {
            final String userPassword = user + ":" + password;
            final String encoding = Base64.getEncoder().encodeToString(userPassword.getBytes());
            http.setRequestProperty("Authorization", "Basic " + encoding);
        } else if (bearerToken != null) {
            http.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        return http;
    }

    protected String readInput(HttpURLConnection conn) throws IOException {
        try (InputStream stream = conn.getInputStream()) {
            String result = readString(stream, resultLimit);
            if (result.length() >= resultLimit) {
                throw new GorSystemException("Result exceeded max result size of " + resultLimit, null);
            }
            return result;
        } catch (IOException e) {
            InputStream ie = conn.getErrorStream();
            String headerinfo = conn.getHeaderFields().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining("\n"));
            String str = ie == null ? headerinfo : headerinfo + "\n" + new BufferedReader(new InputStreamReader(ie)).lines().collect(Collectors.joining());
            throw new IOException(conn.getResponseMessage() + ": " + str, e);
        }
    }

    private static String readString(InputStream stream, int maxLength) throws IOException {
        byte[] buf = new byte[maxLength];
        int read = readToBuffer(stream, buf, 0, maxLength);
        return new String(buf, 0, read);
    }

    private static int readToBuffer(InputStream stream, byte[] buf, int offset, int length) throws IOException {
        int totalread = 0;
        int read;
        do {
            read = stream.read(buf, offset + totalread, length - totalread);
            if (read > 0) {
                totalread += read;
            }
        } while (read > 0);
        if (totalread > 0) {
            return totalread;
        }
        // Return same value as from underlying stream - 0 or -1
        return read;
    }

    protected String makeUrl(String path) {
        if (authPar != null) {
            String authArg = authPar + "=" + authVal;
            if (path.contains("?")) {
                path = path + "&" + authArg;
            } else {
                path = path + "?" + authArg;
            }
        }
        return httpPath + path;
    }

    public void setBearerToken(String token) {
        if (token != null) {
            log.debug("Using bearer token {}", token);
        }
        this.bearerToken = token;
    }

    public void setBasicAuthentication(String user, String password) {
        if (user != null) {
            log.debug("Using basic authentication with user {}, password {}", user, password);
        }
        this.user = user;
        this.password = password;
    }

    public void setParameterAuthentication(String par, String val) {
        if (par != null) {
            log.debug("Using parameter authentication with par {}, val {}", par, val);
        }
        this.authPar = par;
        this.authVal = val;
    }
}
