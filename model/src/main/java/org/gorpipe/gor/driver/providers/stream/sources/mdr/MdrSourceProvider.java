
package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.StreamSourceProvider;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.SourceRef;
import org.gorpipe.gor.session.GorSession;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * MDR Source Provider
 *
 * Uses the MDR service to resolve document ids to presigned or direct urls.
 *
 * The MDR url is formatted as:
 *
 * mdr://<document_id>[/<filename>][?url_type={direct|presigned}&include_grouped={true|false}&env=<env_name>]
 *
 * <document_id> = The MDR document id (UUID format).
 * <filename> = Optional filename to use when the document contains multiple files.
 *
 * Query parameters:
 * url_type = Optional url type to request.  Default is direct
 * include_grouped = Optional flag to include grouped files when requesting presigned urls.  Default is false.
 * env = Optional MDR environment name/alias to use when resolving the document.  The env parameter must match the
 *       configured MDR environments.  Default is "default".
 *
 */
@AutoService(SourceProvider.class)
public class MdrSourceProvider extends StreamSourceProvider {

    private static final MdrConfiguration config = ConfigManager.createPrefixConfig("gor.mdr", MdrConfiguration.class);

    public MdrSourceProvider() {

    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{MdrSourceType.MDR};
    }

    @Override
    public StreamSource resolveDataSource(SourceReference sourceReference) throws IOException {
        var session = GorSession.currentSession.get();

        if (session == null) {
            throw new GorResourceException("No session found", sourceReference.getUrl());
        }

        String documentUrl = MdrServer.resolveUrl(sourceReference.getUrl());

        return (StreamSource)session.getProjectContext().getFileReader().resolveUrl(documentUrl);
    }

    @Override
    protected RetryHandlerBase getRetryHandler() {
        if (retryHandler == null) {
            retryHandler = new MdrSourceRetryHandler(
                    config.retryInitialWait().toMillis(),
                    config.retryMaxWait().toMillis());
        }
        return retryHandler;
    }

    @Override
    public Stream<SourceRef> prepareSources(Stream<SourceRef> sources) {
        var sourcesList = sources.toList();
        MdrServer.cacheUrls(sourcesList);
        return sourcesList.stream();
    }

}
