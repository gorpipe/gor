
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
 * mdr://[<mdr host alias>/]<document_id>[/<filename>][?url_type={direct|presigned}&include_grouped={true|false}]
 *
 * <mdr host alias = MDR server to use.   'default' means use the default MDR server configured in the system.
 * <document_id> = The MDR document id (UUID format).
 * <filename> = Optional filename to use when the document contains multiple files.
 *
 * Note:  The first iteration allowed mdr://<document_id> only.  This is still supported for backward compatibility,
 * but it is recommended to use mdr://default/<document_id> going forward to avoid confusion.
 *
 * The backward compatibility makes it a little tricky to parse the url, so the following rule is use for parsing
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
