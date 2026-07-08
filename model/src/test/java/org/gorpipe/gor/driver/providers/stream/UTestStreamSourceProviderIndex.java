package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for {@link StreamSourceProvider#findIndexFileFromFileDriver}.
 *
 * <p>The {@code .gori} index is optional, so a probe failure that survives the retry handler must not
 * fail the whole query (ENGKNOW-3643).  But the fallback must be scoped: a real configuration error in
 * {@code resolveDataSource} still has to propagate rather than be silently swallowed as a missing index.
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

        // Identity wrap: production wrap() adds the RetryStreamSourceWrapper (which retries exists())
        // and needs a GorDriverConfig this bare test provider has none of, so bypass it here. The retry
        // behavior itself is covered by the wrapper's own tests; this test covers the post-retry fallback.
        @Override
        public StreamSource wrap(DataSource input) {
            return (StreamSource) input;
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

    @Test
    public void nonOptionalProbeFailurePropagates() throws IOException {
        StreamSource indexSource = mock(StreamSource.class);
        when(indexSource.exists()).thenThrow(new RuntimeException("transient storage error"));

        StreamSourceFile file = mock(StreamSourceFile.class);
        when(file.possibleIndexNames()).thenReturn(List.of("s3://bucket/proj/data.gorz.gori"));

        ThrowingProvider provider = new ThrowingProvider(indexSource);

        // optional=false (copy/move/delete dependent enumeration): an undetermined index state must NOT be
        // swallowed, or a mutating op could orphan a stale index. The probe error propagates instead.
        assertThrows(RuntimeException.class, () -> provider.findIndexFileFromFileDriver(
                file, new SourceReference("s3://bucket/proj/data.gorz"), false));
    }

    /** A resolveDataSource failure is a configuration error, not a missing index, so it must propagate. */
    private static class ResolveFailingProvider extends StreamSourceProvider {
        @Override
        public StreamSource resolveDataSource(SourceReference sourceReference) {
            throw new RuntimeException("bad security context");
        }

        @Override
        public StreamSource wrap(DataSource input) {
            return (StreamSource) input;
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
    public void resolveConfigErrorPropagates() throws IOException {
        StreamSourceFile file = mock(StreamSourceFile.class);
        when(file.possibleIndexNames()).thenReturn(List.of("s3://bucket/proj/data.gorz.gori"));

        ResolveFailingProvider provider = new ResolveFailingProvider();

        // resolveDataSource failing (misconfiguration) must NOT be swallowed as "optional index missing".
        assertThrows(RuntimeException.class,
                () -> provider.findIndexFileFromFileDriver(file, new SourceReference("s3://bucket/proj/data.gorz")));
    }
}
