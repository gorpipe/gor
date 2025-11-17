package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.SourceRef;
import org.gorpipe.util.http.keycloak.KeycloakClientAuthRequester;
import org.gorpipe.util.http.utils.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MdrServer {

    public static final String DEFAULT_MDR_SERVER_NAME = "default";

    private static final String MDR_PATH = "/api/v1/documents/urls";
    private static final String URL_TYPE = "url_type";
    private static final String URL_TYPE_DIRECT = "direct";
    private static final String URL_TYPE_PRESIGNED = "presigned";
    private static final String INCLUDE_GROUPED = "include_grouped";

    private static final MdrConfiguration config = ConfigManager.createPrefixConfig("gor.mdr", MdrConfiguration.class);

    private static HashMap<String, MdrServer> mdrServers;

    private static final Cache<MdrDocumentCacheKey, MdrUrlsResultItem> documentCache =
            CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterAccess(config.mdrCacheDuration(), TimeUnit.MINUTES).build();

    static {
        loadCredentials();
    }

    /**
     * Parse MDR credentials from a string.
     *
     * The credentials are in the format:
     *   #name\tMdrUrl\tKeycloakUrl\tKeycloakClientId\tKeycloakClientSecret
     *   <name>\t<mdr url>\t<keycloakUrl>\t<clientId>\t<clientSecret>
     *
     *   # Lines starting with '#' are treated as comments and ignored.
     *
     * @param credentialsData The credential data
     * @return An MdrCredentials object containing the parsed credentials.
     * @throws IllegalArgumentException if the credential string is not in the expected format.
     */
    public static List<MdrServer> parse(String credentialsData, MdrConfiguration config) {
        List<MdrServer> credentialsList = new java.util.ArrayList<>();
        for (String credLine : credentialsData.split("\n")) {
            credLine = credLine.trim();
            if (credLine.isEmpty() || credLine.startsWith("#")) {
                continue;
            }

            String[] parts = credLine.split("\t");
            if (parts.length != 5) {
                throw new IllegalArgumentException("Invalid credential line format. Expected format: <mdr url>\\t<keycloakUrl></>\\t<clientId>\\t<clientSecret>");
            }

            credentialsList.add(new MdrServer(parts[0], parts[1], parts[2], parts[3], parts[4]));
        }

        return credentialsList;
    }

    public static HashMap<String, MdrServer> loadCredentials() {
        mdrServers = new HashMap<>();
        mdrServers.put(DEFAULT_MDR_SERVER_NAME, new MdrServer(
                DEFAULT_MDR_SERVER_NAME,
                config.mdrServer(),
                config.keycloakAuthServer(),
                config.keycloakClientId(), config.keycloakClientSecret()));

        final String MDR_CREDENTIALS_PATH = System.getProperty("gor.mdr.credentials");

        if (MDR_CREDENTIALS_PATH != null && !MDR_CREDENTIALS_PATH.isEmpty()) {
            try {
                String credentialsData = Files.readString(Path.of(MDR_CREDENTIALS_PATH));
                for (MdrServer server : parse(credentialsData, config)) {
                    mdrServers.put(server.getMdrServerName(), server);
                }
            } catch (Exception e) {
                throw new GorParsingException("Failed to read MDR credentials from path: " + MDR_CREDENTIALS_PATH, e);
            }
        }
        return mdrServers;
    }

    public static String resolveUrl(String url) {
        URI uri = URI.create(url);
        return mdrServers.get(extractMdrServerName(uri)).resolveMdrUrl(uri);
    }

    public static void cacheUrls(List<SourceRef> sources) {
        HashMap<String, List<SourceRef>> sourcesByMdrServer = new HashMap<>();
        for (SourceRef source : sources) {
            var mdrServerName  = extractMdrServerName(URI.create(source.file));
            sourcesByMdrServer.computeIfAbsent(mdrServerName, k -> new java.util.ArrayList<>()).add(source);
        }
        for (var entry : sourcesByMdrServer.keySet()) {
            mdrServers.get(entry).cacheMdrUrls(sourcesByMdrServer.get(entry));
        }
    }

    private final String mdrServerName;
    private final String mdrServerUrl;
    private final String keycloakAuthServer;
    private final String keycloakClientId;
    private final String keycloakClientSecret;
    private MdrAuthorizedClient authorizedClient;

    public MdrServer(String mdrServerName, String mdrServerUrl, String keycloakAuthServer, String keycloakClientId, String keycloakClientSecret) {
        this.mdrServerName = mdrServerName;
        this.mdrServerUrl = mdrServerUrl;
        this.keycloakAuthServer = keycloakAuthServer;
        this.keycloakClientId = keycloakClientId;
        this.keycloakClientSecret = keycloakClientSecret;
    }

    public String getMdrServerName() {
        return mdrServerName;
    }

    public String getMdrServerUrl() {
        return mdrServerUrl;
    }

    public MdrAuthorizedClient getAuthorizedClient() {
        if (this.authorizedClient == null) {
            this.authorizedClient = new MdrAuthorizedClient(
                    new KeycloakClientAuthRequester(keycloakAuthServer,
                            Duration.ofSeconds(config.keycloakAuthTimeout()),
                            keycloakClientId, keycloakClientSecret),
                    Duration.ofSeconds(config.mdrTimeout()));
        }
        return this.authorizedClient;
    }

    public String constructPayload(URI mdrUrl) {
        return "{\"document_ids\": [\"%s\"]}".formatted(extractDocumentId(mdrUrl));
    }

    public String constructPayload(List<SourceRef> sources) {
        var sb = new StringBuilder();
        sources.forEach(s -> {
            try {
                if (MdrSourceType.MDR.match(s.file)) {
                    var url = new URI(s.file);
                    sb.append("\"").append(extractDocumentId(url)).append("\",");
                }
            } catch (URISyntaxException e) {
                throw new GorResourceException("Invalid uri: " + s.file, s.file, e);
            }
        });

        if (sb.isEmpty()) {
            return null;
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append("]}");
        return "{\"document_ids\": [" + sb;
    }

    public URI constructUrl(URI mdrUrl) throws IOException, URISyntaxException {
        var mdrQueryMap = new LinkedHashMap<String, String>();

        if (mdrUrl != null) {
            var mdrQueryString = mdrUrl.getQuery();

            if (mdrQueryString != null) {
                mdrQueryMap.putAll(HttpUtils.parseQuery(mdrQueryString));
            }
        }

        if (!mdrQueryMap.containsKey(URL_TYPE)) {
            mdrQueryMap.put(URL_TYPE, config.mdrDefaultLinkType());
        }

        if (!mdrQueryMap.containsKey(INCLUDE_GROUPED)) {
            mdrQueryMap.put(INCLUDE_GROUPED, config.mdrIncludeGrouped() ? "true" : "false");
        }

        var baseUri = URI.create(this.mdrServerUrl);

        return new URI( baseUri.getScheme(), baseUri.getHost(), baseUri.getPath() +
                MDR_PATH, HttpUtils.constructQuery(mdrQueryMap), null);
    }

    public String resolveMdrUrl(URI uri) {
        var query = uri.getQuery() != null ? uri.getQuery() : "";

        var mdrDocument = documentCache.getIfPresent(new MdrDocumentCacheKey(extractDocumentId(uri),
                query.contains(URL_TYPE_PRESIGNED) ? URL_TYPE_PRESIGNED : URL_TYPE_DIRECT));

        if (mdrDocument == null) {
            var mdrResult = getMdrDocument(uri);
            documentCache.put(new MdrDocumentCacheKey(extractDocumentId(uri), mdrResult.url_type()), mdrResult.urls().get(0));
            mdrDocument = mdrResult.urls().get(0);
        }

        return mdrDocument.url();
    }

    public void cacheMdrUrls(List<SourceRef> sources) {
        // Create a list of all sources
        var payload = constructPayload(sources);

        if (payload == null) {
            return;
        }

        // call the mdr service and get the bulk urls
        try {

            var mdrUri = constructUrl(null);
            var result = getAuthorizedClient().post(mdrUri, payload);
            var mdrResult = MdrUrlsResult.fromJSON(result);

            if (mdrResult == null) {
                throw new GorResourceException("Invalid response from MDR", mdrUri.toString());
            }

            // Cache all entries
            mdrResult.urls().forEach(u -> {
                documentCache.put(new MdrDocumentCacheKey(u.document_id(), mdrResult.url_type()), u);
                for (var s : sources) {
                    if (s.file.startsWith("mdr://" + u.document_id())) {
                        s.file = u.url();
                    }
                }
            });
        } catch (Throwable e) {
            // Ignore errors and leave mdr:// urls as is, i.e. not cached.
        }
    }

    private MdrUrlsResult getMdrDocument(URI url) {
        try {
            var mdrUri = constructUrl(url);
            var payload = constructPayload(url);

            var result = getAuthorizedClient().post(mdrUri, payload);

            var mdrResult = MdrUrlsResult.fromJSON(result);

            if (mdrResult == null) {
                throw new GorResourceException("Invalid response from MDR: " + result, url.toString());
            } else if (mdrResult.urls().size() != 1) {
                throw new GorResourceException("Invalid response from MDR, only one source allowed per request, got " + mdrResult.urls().size(), url.toString());
            }
            return mdrResult;
        } catch (URISyntaxException e) {
            throw new GorResourceException("Invalid uri: " + url, url.toString(), e);
        } catch (IOException e) {
            throw new GorResourceException("Error connecting to MDR: " + url, url.toString(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException("MDR call interrupted: " + url, e);
        }
    }

    public static String extractDocumentId(URI mdrUrl) {
        if (mdrServers.containsKey(mdrUrl.getHost())) {
            if (mdrUrl.getPath() == null || mdrUrl.getPath().isEmpty() || mdrUrl.getPath().equals("/")) {
                throw new GorResourceException("No document id specified in MDR url: " + mdrUrl, mdrUrl.toString());
            }
            return mdrUrl.getPath().split("/", 3)[1];
        } else {
            return mdrUrl.getHost();
        }
    }

    public static String extractMdrServerName(URI mdrUrl) {
        if (mdrServers.containsKey(mdrUrl.getHost())) {
            return mdrUrl.getHost();
        } else {
            return DEFAULT_MDR_SERVER_NAME;
        }
    }

}
