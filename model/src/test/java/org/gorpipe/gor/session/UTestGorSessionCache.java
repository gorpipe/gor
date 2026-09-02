package org.gorpipe.gor.session;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for the caches held by a {@link GorSessionCache}.
 *
 * <p>Regression coverage for ENGKNOW-3722. The session S3 metadata cache had neither an expiry nor
 * a size bound, and {@code gor.s3.meta.cache.session=true} is the default, so a long-lived
 * gor-worker session could hold an object length for the lifetime of the process. When another pod
 * rewrote a {@code .link} file smaller, readers kept clamping ranges to the old, larger length:
 * a range past the new end returns {@code 416}, and a range short of a new, larger end silently
 * truncates the link content. Bounding the staleness window bounds both failures.
 */
public class UTestGorSessionCache {

    @Test
    public void s3MetadataCacheEntriesExpire() {
        var cache = new GorSessionCache().getS3MetadataCache();

        var expiry = cache.policy().expireAfterWrite().orElseThrow(() -> new AssertionError(
                "session S3 metadata cache has no write expiry, so a stale object length can be "
                        + "cached for the whole lifetime of a long-running session"));

        assertTrue("staleness window must be at most 5 minutes, matching the static metadata cache",
                expiry.getExpiresAfter(TimeUnit.MINUTES) <= 5);

        expiry.setExpiresAfter(Duration.ofNanos(1));
        cache.put("thebucket/some/file.gor.link", "stale-metadata");
        cache.cleanUp();

        assertNull("an expired object length must not be served again",
                cache.getIfPresent("thebucket/some/file.gor.link"));
    }

    /**
     * The link cache was {@code static}, so cached link content outlived the session that read it and
     * was visible to every other session in the process. Link files are rewritten, so content held
     * beyond a session is content that may no longer be true -- the same staleness this ticket is about,
     * one level up from the object length.
     */
    @Test
    public void linkContentIsNotSharedBetweenSessions() {
        var source = mock(StreamSource.class);
        var readingSession = new GorSessionCache();
        var otherSession = new GorSessionCache();

        readingSession.getLinkCache().put(source, "source/versions/generation_1200.gorz");

        assertNull("link content cached by one session must not be visible to another",
                otherSession.getLinkCache().getIfPresent(source));
    }

    @Test
    public void linkCacheEntriesExpire() {
        var cache = new GorSessionCache().getLinkCache();

        var expiry = cache.policy().expireAfterWrite().orElseThrow(() -> new AssertionError(
                "session link cache has no write expiry, so link content read once can be served for "
                        + "the whole lifetime of a long-running session"));

        assertTrue("staleness window must be at most 5 minutes, matching the static link cache",
                expiry.getExpiresAfter(TimeUnit.MINUTES) <= 5);
    }

    @Test
    public void s3MetadataCacheIsSizeBounded() {
        var cache = new GorSessionCache().getS3MetadataCache();

        var eviction = cache.policy().eviction().orElseThrow(() -> new AssertionError(
                "session S3 metadata cache is unbounded, so it grows without limit in a "
                        + "long-running session"));

        eviction.setMaximum(1);
        cache.put("thebucket/first.gor.link", "meta-1");
        cache.put("thebucket/second.gor.link", "meta-2");
        cache.cleanUp();

        assertTrue("cache must evict down to its maximum size", cache.estimatedSize() <= 1);
    }
}
