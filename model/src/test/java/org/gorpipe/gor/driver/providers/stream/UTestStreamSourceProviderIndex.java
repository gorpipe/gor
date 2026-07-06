package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression test: the {@code .gori} index is optional, so a failure while probing an index
 * candidate must not fail the whole query.  Previously {@link StreamSourceProvider#findIndexFileFromFileDriver}
 * let a non-"not-found" error from {@code exists()} (e.g. a transient storage error) propagate.
 */
public class UTestStreamSourceProviderIndex {

    /** Minimal concrete provider whose resolveDataSource returns a source that blows up on exists(). */
    private static class ThrowingProvider extends StreamSourceProvider {
        private final StreamSource source;

        ThrowingProvider(StreamSource source) {
            this.source = source;
        }

        @Override
        public StreamSource resolveDataSource(SourceReference sourceReference) {
            return source;
        }

        @Override
        protected RetryHandlerBase getRetryHandler() {
            return null;
        }

        @Override
        public SourceType[] getSupportedSourceTypes() {
            return new SourceType[0];
        }
    }

    @Test
    public void indexProbeFailureIsNotFatal() throws IOException {
        StreamSource indexSource = mock(StreamSource.class);
        // Simulate a transient/non-NoSuchKey storage error while checking the optional index.
        when(indexSource.exists()).thenThrow(new RuntimeException("transient storage error"));

        StreamSourceFile file = mock(StreamSourceFile.class);
        when(file.possibleIndexNames()).thenReturn(List.of("s3://bucket/proj/data.gorz.gori"));

        ThrowingProvider provider = new ThrowingProvider(indexSource);

        // Must swallow the probe error and report "no index found" rather than throwing.
        StreamSource result = provider.findIndexFileFromFileDriver(file, new SourceReference("s3://bucket/proj/data.gorz"));

        assertNull(result);
    }
}
