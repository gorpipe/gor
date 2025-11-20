package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.SourceRef;
import org.gorpipe.util.http.keycloak.KeycloakClientAuthRequester;
import org.gorpipe.util.http.utils.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
    private static final String MDR_ENV = "env";

    private static final MdrConfiguration defaultConfig = ConfigManager.createPrefixConfig("gor.mdr", MdrConfiguration.class);

    private static HashMap<String, MdrServer> mdrServers;

    private static final Cache<MdrDocumentCacheKey, MdrUrlsResultItem> documentCache =
            CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterAccess(defaultConfig.mdrCacheDuration(), TimeUnit.MINUTES).build();

    static {
        loadMdrServers(defaultConfig);
    }

    public static void loadMdrServers(MdrConfiguration defaultConfig) {
        mdrServers = MdrConfiguration.loadMdrConfigurations(defaultConfig).entrySet().stream()
                .collect(HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), new MdrServer(entry.getValue())),
                        HashMap::putAll);
    }

    public static String resolveUrl(String url) {
        URI uri = URI.create(url);
        return mdrServers.get(extractMdrEnvName(uri)).resolveMdrUrl(uri);
    }

    public static void cacheUrls(List<SourceRef> sources) {
        HashMap<String, List<SourceRef>> sourcesByMdrServer = new HashMap<>();
        for (SourceRef source : sources) {
            var mdrServerName  = extractMdrEnvName(URI.create(source.file));
            sourcesByMdrServer.computeIfAbsent(mdrServerName, k -> new java.util.ArrayList<>()).add(source);
        }
        for (var entry : sourcesByMdrServer.keySet()) {
            mdrServers.get(entry).cacheMdrUrls(sourcesByMdrServer.get(entry));
        }
    }

    private MdrConfiguration config;
    private MdrAuthorizedClient authorizedClient;

    public MdrServer(MdrConfiguration config) {
        this.config = config;
    }

    public MdrAuthorizedClient getAuthorizedClient() {
        if (this.authorizedClient == null) {
            this.authorizedClient = new MdrAuthorizedClient(
                    new KeycloakClientAuthRequester(config.keycloakAuthServer(),
                            Duration.ofSeconds(defaultConfig.keycloakAuthTimeout()),
                            config.keycloakClientId(), config.keycloakClientSecret()),
                    Duration.ofSeconds(defaultConfig.mdrTimeout()));
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
            mdrQueryMap.put(URL_TYPE, defaultConfig.mdrDefaultLinkType());
        }

        if (!mdrQueryMap.containsKey(INCLUDE_GROUPED)) {
            mdrQueryMap.put(INCLUDE_GROUPED, defaultConfig.mdrIncludeGrouped() ? "true" : "false");
        }

        var baseUri = URI.create(config.mdrServer());

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
        return mdrUrl.getHost();
    }

    public static String extractMdrEnvName(URI mdrUrl) {
        if (mdrUrl != null) {
            var queryString = mdrUrl.getQuery();
            if (queryString != null) {
                return HttpUtils.parseQuery(queryString).getOrDefault(MDR_ENV, DEFAULT_MDR_SERVER_NAME);
            }
        }

        return DEFAULT_MDR_SERVER_NAME;
    }

}
