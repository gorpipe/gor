package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.session.GorSessionCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

/**
 * Tests which link files {@link LinkFile#loadContentFromSource} caches (ENGKNOW-3770).
 *
 * <p>The cache is keyed on the link file path and expires after 5 minutes. That is safe for **simple**
 * link files -- a bare data path, rewritten so rarely that serving one up to 5 minutes stale is an
 * accepted trade -- and it is what makes the cache useful at all, since simple links are by far the
 * common case.
 *
 * <p>It is *not* safe for **versioned** link files, which are rewritten in normal operation: a stale
 * entry could resolve to a generation that versioned-link GC has already deleted, silently. Those are
 * always re-read. See the ticket for why keying on cached metadata instead does not fix this.
 *
 * <p>Before this change the cache was keyed on the {@link org.gorpipe.gor.driver.providers.stream.sources.StreamSource}
 * object identity and could never hit at all, so nothing was cached in practice.
 */
public class UTestLinkFileCache {

    private static final String SIMPLE = "source/data.gorz\n";
    private static final String SIMPLE_REWRITTEN = "source/rewritten.gorz\n";

    private static final String VERSIONED = """
            ## SERIAL = 1
            ## VERSION = 1
            #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
            source/versions/generation_1.gorz\t2026-01-01T00:00:00.000Z\tMD5SUM1\t1\t
            """;

    private static final String VERSIONED_REWRITTEN = """
            ## SERIAL = 2
            ## VERSION = 1
            #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
            source/versions/generation_2.gorz\t2026-02-01T00:00:00.000Z\tMD5SUM2\t2\t
            """;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private Path linkPath;

    @Before
    public void setUp() {
        linkPath = workDir.getRoot().toPath().toAbsolutePath().resolve("test.gorz.link");
    }

    @After
    public void tearDown() {
        // Set by the session tests below. It is an InheritableThreadLocal that nothing clears, so
        // leaving it set would hand this thread's session to every later test in the same JVM.
        GorSession.currentSession.remove();
    }

    private String read() throws Exception {
        return LinkFile.loadContentFromSource(new FileSource(linkPath.toString()));
    }

    private void saveLinkFile(String link) {
        try {
            var linkFile = LinkFile.load(new FileSource(linkPath.toString()));
            linkFile.appendEntry(link, "NEWMD5SUM");
            linkFile.save(new DriverBackedFileReader(
                    null, workDir.getRoot().toPath().toAbsolutePath().toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A session with its own link cache, bound to the calling thread the way a real one is. */
    private static GorSession newSessionOnThisThread(String requestId) {
        var cache = new GorSessionCache();
        var session = new GorSession(requestId) {
            @Override
            public GorSessionCache getCache() {
                return cache;
            }
        };
        GorSession.currentSession.set(session);
        return session;
    }

    @Test
    public void simpleLinkFileIsServedFromCache() throws Exception {
        Files.writeString(linkPath, SIMPLE);
        var firstRead = read();
        assertEquals(SIMPLE, firstRead);

        Files.writeString(linkPath, SIMPLE_REWRITTEN);

        assertEquals("a simple link file must be served from cache within the expiry window",
                firstRead, read());
    }

    @Test
    public void versionedLinkFileIsNeverCached() throws Exception {
        Files.writeString(linkPath, VERSIONED);
        assertEquals(VERSIONED, read());

        Files.writeString(linkPath, VERSIONED_REWRITTEN);

        assertEquals("a versioned link file must be re-read, never served from cache",
                VERSIONED_REWRITTEN, read());
    }

    /**
     * The cache tolerates a link file rewritten by another process going unnoticed until it expires.
     * It must never do that for one rewritten here: the caller would read back content it just replaced.
     */
    @Test
    public void savingALinkFileInvalidatesItsCachedContent() throws Exception {
        Files.writeString(linkPath, SIMPLE);
        assertEquals(SIMPLE, read());

        var linkFile = LinkFile.load(new FileSource(linkPath.toString()));
        linkFile.appendEntry("source/appended.gorz", "NEWMD5SUM");
        linkFile.save(new DriverBackedFileReader(null, workDir.getRoot().toPath().toAbsolutePath().toString()));

        assertEquals("content written in this process must not be served from cache",
                Files.readString(linkPath), read());
    }


    /**
     * A resolution that is already in flight when a save lands must not leave its content behind. The
     * stream it read from was opened before the save replaced the file, so what it holds is the
     * pre-write content -- and it caches that only after the save has dropped everything it could reach.
     */
    @Test
    public void contentReadWhileASaveWasRunningIsNotLeftInTheCache() throws Exception {
        Files.writeString(linkPath, SIMPLE);

        var saved = new AtomicBoolean();
        var source = new FileSource(linkPath.toString()) {
            @Override
            public InputStream open() {
                var is = super.open();
                if (saved.compareAndSet(false, true)) {
                    saveLinkFile("source/appended.gorz");
                }
                return is;
            }
        };

        assertEquals("the in-flight read holds the content the file had when it opened the stream",
                SIMPLE, LinkFile.loadContentFromSource(source));

        assertEquals("content read while a save was in flight must not outlive the save",
                Files.readString(linkPath), read());
    }

    /**
     * {@link GorSession#currentSession} is an InheritableThreadLocal that nothing clears, so a read and
     * a save of the same link file can land on different session caches: the save invalidates the cache
     * of the session bound to its own thread, which is not the one holding the entry.
     */
    @Test
    public void savingOnAnotherSessionsThreadInvalidatesCachedContent() throws Exception {
        Files.writeString(linkPath, SIMPLE);

        var readingSession = newSessionOnThisThread("reader");
        assertEquals(SIMPLE, read());

        var writer = new Thread(() -> {
            newSessionOnThisThread("writer");
            saveLinkFile("source/appended.gorz");
        });
        writer.start();
        writer.join();

        GorSession.currentSession.set(readingSession);
        assertEquals("a save in this process must not leave stale content in another session's cache",
                Files.readString(linkPath), read());
    }

    /** An empty link file carries no link and is typically a placeholder about to be written. */
    @Test
    public void emptyLinkFileIsNotCached() throws Exception {
        Files.writeString(linkPath, "");
        assertEquals("", read());

        Files.writeString(linkPath, SIMPLE);

        assertEquals("an empty link file must not be cached", SIMPLE, read());
    }

    @Test
    public void cachedSimpleContentIsKeyedOnThePathNotTheSourceObject() throws Exception {
        var otherPath = workDir.getRoot().toPath().toAbsolutePath().resolve("other.gorz.link");
        Files.writeString(linkPath, SIMPLE);
        Files.writeString(otherPath, SIMPLE_REWRITTEN);

        assertEquals(SIMPLE, read());
        assertEquals("a different link file must not be served another one's content",
                SIMPLE_REWRITTEN, LinkFile.loadContentFromSource(new FileSource(otherPath.toString())));
    }
}
