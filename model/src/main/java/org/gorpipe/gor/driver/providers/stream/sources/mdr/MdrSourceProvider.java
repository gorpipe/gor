
package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import com.google.auto.service.AutoService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.gorpipe.util.http.utils.HttpUtils;
import org.gorpipe.util.http.client.AuthorizedHttpClient;
import org.gorpipe.util.http.keycloak.KeycloakClientAuthRequester;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.StreamSourceProvider;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.SourceRef;
import org.gorpipe.gor.session.GorSession;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@AutoService(SourceProvider.class)
public class MdrSourceProvider extends StreamSourceProvider {

    private static final String MDR_PATH = "/api/v1/documents/urls";
    private static final String URL_TYPE = "url_type";
    private static final String URL_TYPE_DIRECT = "direct";
    private static final String URL_TYPE_PRESIGNED = "presigned";
    private static final String INCLUDE_GROUPED = "include_grouped";

    private static final MdrConfiguration config = ConfigManager.createPrefixConfig("gor.mdr", MdrConfiguration.class);
    private static final Cache<MdrDocumentCacheKey, MdrUrlsResultItem> documentCache =
            CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterAccess(config.mdrCacheDuration(), TimeUnit.MINUTES).build();

    private final AuthorizedHttpClient authorizedClient;

    public MdrSourceProvider() {
        this.authorizedClient = new MdrAuthorizedClient(
                new KeycloakClientAuthRequester(config.keycloakAuthServer(),
                        Duration.ofSeconds(config.keycloakAuthTimeout()),
                        config.keycloakClientId(), config.keycloakClientSecret()),
                Duration.ofSeconds(config.mdrTimeout()));
    }
    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{MdrSourceType.MDR};
    }

    @Override
    public StreamSource resolveDataSource(SourceReference sourceReference) throws IOException {
        try {
            URI url = new URI(sourceReference.getUrl());

            var query = url.getQuery();

            if (query == null)
                query = "";

            var cached =
                    documentCache.getIfPresent(new MdrDocumentCacheKey(url.getHost(), query.contains(URL_TYPE_PRESIGNED) ? URL_TYPE_PRESIGNED : URL_TYPE_DIRECT));

            var session = GorSession.currentSession.get();

            if (session == null) {
                throw new GorResourceException("No session found", sourceReference.getUrl());
            }

            if (cached != null) {
                return (StreamSource)session.getProjectContext().getFileReader().resolveUrl(cached.url());
            }

            var mdrUri = constructUrl(url);
            var payload = constructPayload(url);

            var result = this.authorizedClient.post(mdrUri, payload);

            var mdrResult = MdrUrlsResult.fromJSON(result);

            if (mdrResult == null) {
                throw new GorResourceException("Invalid response from MDR: " + result, sourceReference.getUrl());
            } else if (mdrResult.urls().size() != 1) {
                throw new GorResourceException("Invalid response from MDR, only one source allowed per requerst, got " + mdrResult.urls().size(), sourceReference.getUrl());
            }

            documentCache.put(new MdrDocumentCacheKey(url.getHost(), mdrResult.url_type()), mdrResult.urls().get(0));

            return (StreamSource)session.getProjectContext().getFileReader().resolveUrl(mdrResult.urls().get(0).url());
        } catch (URISyntaxException e) {
            throw new GorResourceException("Invalid uri: " + sourceReference.getUrl(), sourceReference.getUrl(), e);
        } catch (IOException e) {
            throw new GorResourceException("Error connecting to MDR: " + sourceReference.getUrl(), sourceReference.getUrl(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException("MDR call interrupted: " + sourceReference.getUrl(), e);
        }
    }

    private String constructPayload(URI mdrUrl) {
        return "{\"document_ids\": [\"" + mdrUrl.getHost() + "\"]}";
    }

    private String constructPayload(List<SourceRef> sources) {
        var sb = new StringBuilder();
        sources.forEach(s -> {
            try {
                if (MdrSourceType.MDR.match(s.file)) {
                    var url = new URI(s.file);
                    sb.append("\"").append(url.getHost()).append("\",");
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

    private URI constructUrl(URI mdrUrl) throws IOException, URISyntaxException {
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

        var baseUri = URI.create(config.mdrServer());

        return new URI( baseUri.getScheme(), baseUri.getHost(), baseUri.getPath() +
                MDR_PATH, HttpUtils.constructQuery(mdrQueryMap), null);
    }

    @Override
    public Stream<SourceRef> prepareSources(Stream<SourceRef> sources) {

        var sourcesList = sources.toList();

        // Create a list of all sources
        var payload = constructPayload(sourcesList);

        if (payload == null) {
            return sourcesList.stream();
        }

        // call the mdr service and get the bulk urls
        try {
            var mdrUri = constructUrl(null);
            var result = this.authorizedClient.post(mdrUri, payload);
            var mdrResult = MdrUrlsResult.fromJSON(result);

            if (mdrResult == null) {
                throw new GorResourceException("Invalid response from MDR", mdrUri.toString());
            }

            // Cache all entries
            mdrResult.urls().forEach(u -> {
                documentCache.put(new MdrDocumentCacheKey(u.document_id(), mdrResult.url_type()), u);
                for (var s : sourcesList) {
                    if (s.file.startsWith("mdr://" + u.document_id())) {
                        s.file = u.url();
                    }
                }
            });

            return sourcesList.stream();
        } catch (Throwable e) {
            return sourcesList.stream();
        }
    }

}
