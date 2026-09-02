package org.gorpipe.gor.driver.linkfile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the link resolution tally behind ENGKNOW-3770.
 *
 * <p>The numbers this produces decide two things: whether caching link content is worth anything
 * (repeat resolutions of one path), and whether caching *versioned* links would be safe (how often the
 * content of one path actually changes between resolutions). Simple links are cached, so their repeat
 * resolutions show up as hits; versioned links are always re-read, so theirs show up as reads.
 */
public class UTestLinkFileCacheStats {

    private static final String SIMPLE = "source/data.gorz\n";
    private static final String SIMPLE_REWRITTEN = "source/other.gorz\n";

    private static final String VERSIONED = """
            ## SERIAL = 1
            ## VERSION = 1
            #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
            source/versions/generation_1.gorz\t2026-01-01T00:00:00.000Z\tMD5SUM1\t1\t
            """;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Before
    public void setUp() {
        System.setProperty("gor.driver.link.cache.stats", "true");
        LinkFileCacheStats.reset();
    }

    private static void enable(boolean enabled) {
        System.setProperty("gor.driver.link.cache.stats", String.valueOf(enabled));
    }

    /**
     * A link file gets converted from simple to versioned in normal operation (LinkUpdateCommand does
     * exactly this). Reads follow the conversion because they derive the version from the content they
     * just read; hits must follow it too, or the per-version hit rate -- the number this tally exists to
     * produce -- is skewed in both directions.
     */
    @Test
    public void hitsFollowThePathsCurrentVersion() {
        var path = "s3://thebucket/ref/dbsnp.gorz.link";

        LinkFileCacheStats.recordRead(path, SIMPLE);
        LinkFileCacheStats.recordRead(path, VERSIONED);
        LinkFileCacheStats.recordCacheHit(path);

        assertEquals("a hit must count against the version the path holds now",
                1, LinkFileCacheStats.summary("1").hits());
        assertEquals(0, LinkFileCacheStats.summary("0").hits());
        assertEquals(1, LinkFileCacheStats.summary("1").distinctPaths());
        assertEquals(0, LinkFileCacheStats.summary("0").distinctPaths());
    }

    /**
     * The first stats-enabled resolution registers a shutdown hook to dump the tally. If that first
     * resolution happens while the JVM is already shutting down, registering throws -- and an
     * observe-only tally must never be able to fail the read it is counting.
     */
    @Test
    public void readsSurviveAJvmThatIsAlreadyShuttingDown() {
        LinkFileCacheStats.shutdownHookRegistrar = hook -> {
            throw new IllegalStateException("Shutdown in progress");
        };

        LinkFileCacheStats.recordRead("s3://thebucket/ref/dbsnp.gorz.link", SIMPLE);
        LinkFileCacheStats.recordCacheHit("s3://thebucket/ref/dbsnp.gorz.link");

        assertEquals(1, LinkFileCacheStats.summary("0").reads());
        assertEquals(1, LinkFileCacheStats.summary("0").hits());
    }

    @Test
    public void recordsNothingWhenDisabled() {
        enable(false);

        LinkFileCacheStats.recordRead("s3://thebucket/ref/dbsnp.gorz.link", SIMPLE);
        LinkFileCacheStats.recordCacheHit("s3://thebucket/ref/dbsnp.gorz.link");

        assertEquals("must cost nothing unless explicitly switched on",
                0, LinkFileCacheStats.summary("0").reads());
        assertEquals(0, LinkFileCacheStats.summary("0").hits());
    }

    @Test
    public void repeatReadsOfOnePathCountAsOneDistinctPath() {
        var path = "s3://thebucket/ref/dbsnp.gorz.link";

        LinkFileCacheStats.recordRead(path, SIMPLE);
        LinkFileCacheStats.recordRead(path, SIMPLE);
        LinkFileCacheStats.recordRead(path, SIMPLE);

        var simple = LinkFileCacheStats.summary("0");
        assertEquals(3, simple.reads());
        assertEquals("repeat resolutions of one path are what a cache would save", 1, simple.distinctPaths());
        assertEquals("unchanged content is not a change", 0, simple.contentChanges());
    }

    @Test
    public void changedContentOnOnePathIsCountedAsAChange() {
        var path = "s3://thebucket/ref/dbsnp.gorz.link";

        LinkFileCacheStats.recordRead(path, SIMPLE);
        LinkFileCacheStats.recordRead(path, SIMPLE_REWRITTEN);
        LinkFileCacheStats.recordRead(path, SIMPLE_REWRITTEN);

        assertEquals("only the resolution that saw new content counts",
                1, LinkFileCacheStats.summary("0").contentChanges());
    }

    @Test
    public void simpleAndVersionedAreTalliedSeparately() {
        LinkFileCacheStats.recordRead("s3://thebucket/ref/simple.gorz.link", SIMPLE);
        LinkFileCacheStats.recordRead("s3://thebucket/ref/versioned.gorz.link", VERSIONED);
        LinkFileCacheStats.recordRead("s3://thebucket/ref/versioned.gorz.link", VERSIONED);

        assertEquals("a bare link path is a simple link file", 1, LinkFileCacheStats.summary("0").reads());
        assertEquals("a '## VERSION = 1' header is a versioned link file",
                2, LinkFileCacheStats.summary("1").reads());
        assertEquals(1, LinkFileCacheStats.summary("1").distinctPaths());
    }

    @Test
    public void cacheHitsAreCountedAgainstSimpleLinks() {
        var path = "s3://thebucket/ref/dbsnp.gorz.link";

        LinkFileCacheStats.recordRead(path, SIMPLE);
        LinkFileCacheStats.recordCacheHit(path);
        LinkFileCacheStats.recordCacheHit(path);

        var simple = LinkFileCacheStats.summary("0");
        assertEquals(1, simple.reads());
        assertEquals("only simple links are cached, so hits belong to them", 2, simple.hits());
    }

    @Test
    public void distinctPathsAreCappedSoTheTallyCannotGrowWithoutBound() {
        System.setProperty("gor.driver.link.cache.stats.maxpaths", "2");

        for (int i = 0; i < 10; i++) {
            LinkFileCacheStats.recordRead("s3://thebucket/ref/file" + i + ".gorz.link", SIMPLE);
        }

        var simple = LinkFileCacheStats.summary("0");
        assertTrue("tracked paths must stay within the cap, got " + simple.distinctPaths(),
                simple.distinctPaths() <= 2);
        assertEquals("every resolution is still counted", 10, simple.reads());
    }
}
