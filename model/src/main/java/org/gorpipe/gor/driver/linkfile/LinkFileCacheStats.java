package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.base.config.PropsHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Observe-only tally of link file resolutions (ENGKNOW-3770).
 *
 * <p>Off unless {@code gor.driver.link.cache.stats} is set, and it never influences what a caller is
 * served.  It answers two questions that decide the shape of the link content cache:
 *
 * <ul>
 *   <li><b>Is caching worth anything</b> -- resolutions against distinct paths, plus the hits the
 *       cache actually serves.</li>
 *   <li><b>Would caching versioned links be safe</b> -- how often the content behind one path actually
 *       changes between two resolutions.  Versioned links are never cached, so every resolution of one
 *       is a read and this number is complete for them.  Simple links are cached, so once an entry is
 *       warm their changes are not observed -- which is the accepted trade for that kind of link.</li>
 * </ul>
 *
 * <p>The counters are approximate under concurrency: two threads resolving the same path at the same
 * moment can each see the other's content as a change.  That is acceptable for a measurement tally and
 * is not worth locking a read path over.
 */
public final class LinkFileCacheStats {

    static final String STATS_ENABLED_KEY = "gor.driver.link.cache.stats";
    static final String MAX_PATHS_KEY = "gor.driver.link.cache.stats.maxpaths";
    private static final int DEFAULT_MAX_PATHS = 50000;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LinkFileCacheStats.class);

    // Per version totals.  Always counted, even for paths the cap stopped us tracking individually.
    private static final Map<String, Counters> counters = new ConcurrentHashMap<>();
    // Per path, enough to spot a content change.  Capped.
    private static final Map<String, TrackedPath> trackedPaths = new ConcurrentHashMap<>();

    private static final AtomicBoolean dumpRegistered = new AtomicBoolean();

    private LinkFileCacheStats() {}

    /** Tally for one link file version. */
    public record Summary(long reads, long hits, long contentChanges, int distinctPaths) {}

    private static final class Counters {
        final AtomicLong reads = new AtomicLong();
        final AtomicLong hits = new AtomicLong();
        final AtomicLong contentChanges = new AtomicLong();
    }

    private static final class TrackedPath {
        final String version;
        volatile String contentFingerprint;

        TrackedPath(String version, String contentFingerprint) {
            this.version = version;
            this.contentFingerprint = contentFingerprint;
        }
    }

    private static boolean enabled() {
        return PropsHelper.getBoolean(STATS_ENABLED_KEY, false);
    }

    private static int maxPaths() {
        return PropsHelper.getInt(MAX_PATHS_KEY, DEFAULT_MAX_PATHS);
    }

    private static Counters counters(String version) {
        return counters.computeIfAbsent(version, v -> new Counters());
    }

    /** Length plus hash: cheap, and far less collision prone for change detection than the hash alone. */
    private static String fingerprint(String content) {
        return content.length() + ":" + content.hashCode();
    }

    static String versionOf(String content) {
        return LinkFileMeta.createOrLoad(content, null, false).getVersion();
    }

    /**
     * Record a resolution that went to storage.
     */
    public static void recordRead(String path, String content) {
        if (!enabled() || content == null) {
            return;
        }
        registerDump();

        var version = versionOf(content);
        var counters = counters(version);
        counters.reads.incrementAndGet();

        var fingerprint = fingerprint(content);
        var tracked = trackedPaths.get(path);
        if (tracked != null) {
            if (!tracked.contentFingerprint.equals(fingerprint)) {
                tracked.contentFingerprint = fingerprint;
                counters.contentChanges.incrementAndGet();
            }
        } else if (trackedPaths.size() < maxPaths()) {
            trackedPaths.put(path, new TrackedPath(version, fingerprint));
        }
    }

    /**
     * Record a resolution served from the cache.  Only simple link files are cached, so a hit against a
     * path we are not tracking is counted against them.
     */
    public static void recordCacheHit(String path) {
        if (!enabled()) {
            return;
        }
        registerDump();

        var tracked = trackedPaths.get(path);
        counters(tracked != null ? tracked.version : LinkFileV0.VERSION).hits.incrementAndGet();
    }

    public static Summary summary(String version) {
        var c = counters.get(version);
        var distinct = (int) trackedPaths.values().stream().filter(t -> t.version.equals(version)).count();
        if (c == null) {
            return new Summary(0, 0, 0, distinct);
        }
        return new Summary(c.reads.get(), c.hits.get(), c.contentChanges.get(), distinct);
    }

    public static void reset() {
        counters.clear();
        trackedPaths.clear();
    }

    public static void logSummary() {
        for (var version : counters.keySet().stream().sorted().toList()) {
            var s = summary(version);
            var resolutions = s.reads() + s.hits();
            log.info("Link file cache stats, version {}: {} resolutions ({} reads, {} cache hits) over {} distinct paths, {} content changes seen",
                    version, resolutions, s.reads(), s.hits(), s.distinctPaths(), s.contentChanges());
        }
    }

    private static void registerDump() {
        if (dumpRegistered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(LinkFileCacheStats::logSummary,
                    "link-cache-stats-dump"));
        }
    }
}
