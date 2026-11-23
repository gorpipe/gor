package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorRetryException;
import org.gorpipe.exceptions.GorSourceChangedException;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSourceRetryHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ENGKNOW-4312. A dictionary save writes a temp file and moves it over the original, so a reader on another NFS
 * client gets "Stale file handle" on its next read. The retry wrapper then reopens the path and resumes at the old
 * byte offset. When the file was replaced with different content, that read silently returns the old prefix
 * followed by the new file from the old offset. These tests go through the production pieces (a real
 * {@link FileSource} and {@link FileSourceRetryHandler}) and only simulate the ESTALE itself.
 */
public class UTestRetrySourceChanged {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @After
    public void clearProperty() {
        System.clearProperty(RetryStreamSourceWrapper.FAIL_ON_SOURCE_CHANGED_PROPERTY);
    }

    public static String lines(int from, int to) {
        var sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append("source/var/file_").append(i).append(".gorz\tPN").append(i).append('\n');
        }
        return sb.toString();
    }

    public static String reversedLines(int from, int to) {
        List<String> l = new ArrayList<>(List.of(lines(from, to).split("\n")));
        Collections.reverse(l);
        return String.join("\n", l) + "\n";
    }

    /** Replace {@code target} the way a dictionary save does: write a temp file, then move it over the original. */
    public static void replace(Path target, String content) {
        try {
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    static RetryStreamSourceWrapper wrap(StreamSourceWithStale source) {
        return new RetryStreamSourceWrapper(new FileSourceRetryHandler(10, 1000), source);
    }

    static String readAll(InputStream in) throws IOException {
        // Small reads, so the failure lands mid-file rather than in the first buffer.
        var out = new StringBuilder();
        var buf = new byte[256];
        int n;
        while ((n = in.read(buf, 0, buf.length)) > 0) {
            out.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        }
        return out.toString();
    }

    private Path write(String name, String content) throws IOException {
        Path p = workDir.getRoot().toPath().resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p;
    }

    private static void assertSourceChanged(GorException e) {
        Assert.assertTrue("expected a source-changed error, got: " + e.getMessage(),
                e.getMessage().contains("changed during read"));
    }

    @Test
    public void replacedFileWithDifferentPrefixIsNotResumedAtOldOffset() throws IOException {
        String before = lines(0, 200);
        String after = lines(50, 200);  // first 50 entries deleted: the prefix changes
        Path p = write("dict.gord", before);

        var source = new StreamSourceWithStale(p, 3, () -> replace(p, after));
        try (InputStream in = wrap(source).open()) {
            String read = readAll(in);
            Assert.fail("a replaced file must not be resumed at the old offset; read "
                    + (read.equals(before) ? "the old file" : read.equals(after) ? "the new file" : "a mix of both"));
        } catch (GorException e) {
            assertSourceChanged(e);
            Assert.assertTrue("source changed should ask for a full retry",
                    e instanceof GorRetryException && ((GorRetryException) e).isFullRetry());
        }
    }

    @Test
    public void sameLengthReorderedFileIsDetected() throws IOException {
        String before = lines(0, 200);
        String after = reversedLines(0, 200);
        Assert.assertEquals(before.length(), after.length());
        Path p = write("dict.gord", before);

        var source = new StreamSourceWithStale(p, 3, () -> replace(p, after));
        try (InputStream in = wrap(source).open()) {
            readAll(in);
            Assert.fail("a same-length reordered file must not be resumed at the old offset");
        } catch (GorException e) {
            assertSourceChanged(e);
        }
    }

    @Test
    public void streamOpenedMidFileIsCheckedOnFirstRead() throws IOException {
        String before = lines(0, 200);
        Path p = write("data.gor", before);

        // Fails on the very first read: position 0 in the stream, but offset 1000 in the file.
        var source = new StreamSourceWithStale(p, 1, () -> replace(p, lines(50, 200)));
        try (InputStream in = wrap(source).open(1000)) {
            readAll(in);
            Assert.fail("a stream opened at an offset must not resume into a replaced file");
        } catch (GorException e) {
            assertSourceChanged(e);
        }
    }

    @Test
    public void replacedBeforeAnyByteIsReadGivesTheNewFile() throws IOException {
        String after = lines(50, 200);
        Path p = write("dict.gord", lines(0, 200));

        var source = new StreamSourceWithStale(p, 1, () -> replace(p, after));
        try (InputStream in = wrap(source).open()) {
            Assert.assertEquals("nothing was read yet, so reading the new file from the start is correct",
                    after, readAll(in));
        }
    }

    @Test
    public void staleHandleOnUnchangedFileStillResumes() throws IOException {
        String content = lines(0, 200);
        Path p = write("dict.gord", content);

        var source = new StreamSourceWithStale(p, 3, () -> { });
        try (InputStream in = wrap(source).open()) {
            Assert.assertEquals(content, readAll(in));
        }
        Assert.assertEquals("the stale handle must have been hit and retried", 1, source.failures.get());
    }

    @Test
    public void propertyRestoresResumeBehaviour() throws IOException {
        System.setProperty(RetryStreamSourceWrapper.FAIL_ON_SOURCE_CHANGED_PROPERTY, "false");
        String before = lines(0, 200);
        String after = lines(50, 200);
        Path p = write("dict.gord", before);

        var source = new StreamSourceWithStale(p, 3, () -> replace(p, after));
        try (InputStream in = wrap(source).open()) {
            String read = readAll(in);
            Assert.assertNotEquals("log-only mode resumes at the old offset, as before ENGKNOW-4312", before, read);
            Assert.assertNotEquals(after, read);
        }
    }

    @Test
    public void readStringThroughWrapperDetectsChange() throws IOException {
        Path p = write("dict.gord", lines(0, 200));
        var source = new StreamSourceWithStale(p, 2, () -> replace(p, lines(50, 200)));
        try (InputStream in = wrap(source).open()) {
            var e = Assert.assertThrows(GorSourceChangedException.class, () -> StreamUtils.readString(in, 1_000_000));
            Assert.assertTrue(e.isFullRetry());
            Assert.assertFalse("the request-level retry must not retry it", e.isRetry());
            Assert.assertFalse("the cause must stay out of the message", e.getMessage().contains("Stale file handle"));
        }
    }

    @Test
    public void streamStaysFailedAfterSourceChanged() throws IOException {
        Path p = write("dict.gord", lines(0, 200));
        var source = new StreamSourceWithStale(p, 3, () -> replace(p, lines(50, 200)));
        try (InputStream in = wrap(source).open()) {
            Assert.assertThrows(GorSourceChangedException.class, () -> readAll(in));
            Assert.assertThrows("reading on would give the new file at the old offset",
                    GorSourceChangedException.class, () -> in.read(new byte[10], 0, 10));
        }
    }

    @Test
    public void reopenAtStartThenStaleMidFileResumesInNewFile() throws IOException {
        String after = lines(50, 200);
        Path p = write("dict.gord", lines(0, 200));

        // First failure before any byte: replace, read the new file from 0. Second failure mid-file, nothing
        // replaced: the stream is on the new file, so resuming is correct.
        var source = new StreamSourceWithStale(p, () -> replace(p, after), 1, 4);
        try (InputStream in = wrap(source).open()) {
            Assert.assertEquals(after, readAll(in));
        }
        Assert.assertEquals(2, source.failures.get());
    }

    @Test
    public void inPlaceAppendStillResumes() throws IOException {
        String content = lines(0, 200);
        String appended = lines(200, 250);
        Path p = write("dict.gord", content);

        // Same inode, more bytes: the prefix is unchanged, so resuming is correct (as before ENGKNOW-4312).
        var source = new StreamSourceWithStale(p, 3, () -> {
            try {
                Files.writeString(p, appended, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        });
        try (InputStream in = wrap(source).open()) {
            Assert.assertEquals(content + appended, readAll(in));
        }
    }

    /**
     * A real {@link FileSource} whose streams throw "Stale file handle" once, on the {@code failOnRead}th read call
     * across all streams of this source, after running {@code beforeFailure} (typically: replace the file).
     */
    public static class StreamSourceWithStale extends FileSource {
        private final Set<Integer> failOnReads;
        private final Runnable beforeFailure;
        private final AtomicInteger reads = new AtomicInteger();
        private final AtomicBoolean fired = new AtomicBoolean();
        final AtomicInteger failures = new AtomicInteger();

        public StreamSourceWithStale(Path path, int failOnRead, Runnable beforeFailure) {
            this(path, beforeFailure, failOnRead);
        }

        /** Fails on each of the given read calls; {@code beforeFailure} runs before the first failure only. */
        public StreamSourceWithStale(Path path, Runnable beforeFailure, Integer... failOnReads) {
            super(path);
            this.failOnReads = Set.of(failOnReads);
            this.beforeFailure = beforeFailure;
        }

        @Override
        public InputStream open(long start) {
            return inject(super.open(start));
        }

        @Override
        public InputStream openClosable() {
            return inject(super.openClosable());
        }

        private InputStream inject(InputStream in) {
            return new FilterInputStream(in) {
                @Override
                public int read() throws IOException {
                    maybeFail();
                    return super.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    maybeFail();
                    return super.read(b, off, len);
                }
            };
        }

        private void maybeFail() throws IOException {
            if (failOnReads.contains(reads.incrementAndGet())) {
                if (fired.compareAndSet(false, true)) {
                    beforeFailure.run();
                }
                failures.incrementAndGet();
                throw new IOException("Stale file handle");
            }
        }
    }
}
