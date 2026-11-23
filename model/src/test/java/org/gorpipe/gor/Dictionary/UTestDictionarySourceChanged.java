package org.gorpipe.gor.Dictionary;

import org.gorpipe.exceptions.GorSourceChangedException;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSourceRetryHandler;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryStreamSourceWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.UTestRetrySourceChanged;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.UTestRetrySourceChanged.StreamSourceWithStale;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.table.Dictionary;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ENGKNOW-4312: a dictionary replaced while it is read is read again from the start, so the query sees exactly the
 * new dictionary, never the old prefix plus the new file from the old offset.
 */
public class UTestDictionarySourceChanged {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    /** Reads through the retry wrapper; the first stream it opens hits one "Stale file handle" after the replace. */
    static class StaleOnceReader extends DriverBackedFileReader {
        final AtomicInteger opens = new AtomicInteger();
        private final Runnable replace;

        StaleOnceReader(Runnable replace) {
            super("");
            this.replace = replace;
        }

        @Override
        public InputStream getInputStream(String resource) {
            // Fail on the 2nd read call: the first 8 kb are consumed, the rest comes after the replace.
            int failOnRead = opens.incrementAndGet() == 1 ? 2 : Integer.MAX_VALUE;
            var source = new StreamSourceWithStale(Path.of(resource), failOnRead, replace);
            return new RetryStreamSourceWrapper(new FileSourceRetryHandler(10, 1000), source).openClosable();
        }
    }

    private static Set<String> tags(int from, int to) {
        var s = new HashSet<String>();
        for (int i = from; i < to; i++) s.add("PN" + i);
        return s;
    }

    private Path dictionary() throws IOException {
        Path p = workDir.getRoot().toPath().resolve("var.gord");
        Files.writeString(p, UTestRetrySourceChanged.lines(0, 2000), StandardCharsets.UTF_8);
        return p;
    }

    @Test
    public void dictionaryReplacedDuringReadIsReadAgain() throws IOException {
        Path p = dictionary();
        var reader = new StaleOnceReader(() -> UTestRetrySourceChanged.replace(p, UTestRetrySourceChanged.lines(500, 2000)));

        Dictionary dict = Dictionary.getDictionary(p.toString(), reader, workDir.getRoot().toString(), false);

        Assert.assertEquals("the dictionary must be read again from the start", 2, reader.opens.get());
        Assert.assertEquals(tags(500, 2000), dict.getValidTags());
    }

    @Test
    public void cachedDictionaryReplacedDuringReadIsReadAgain() throws IOException {
        Path p = dictionary();
        // A constant signature: only the re-read's empty signature can force the second read below.
        var reader = new StaleOnceReader(() -> UTestRetrySourceChanged.replace(p, UTestRetrySourceChanged.lines(500, 2000))) {
            @Override
            public String getFileSignature(String file) {
                return "constant";
            }
        };

        Dictionary dict = Dictionary.getDictionary(p.toString(), reader, workDir.getRoot().toString(), true);
        Assert.assertEquals(tags(500, 2000), dict.getValidTags());

        // The signature taken before the read was the old file's, so the entry must not be served for the new one.
        int opens = reader.opens.get();
        Dictionary again = Dictionary.getDictionary(p.toString(), reader, workDir.getRoot().toString(), true);
        Assert.assertEquals("the re-read dictionary must not be cached under the old signature", opens + 1, reader.opens.get());
        Assert.assertEquals(tags(500, 2000), again.getValidTags());
    }

    @Test
    public void dictionaryReplacedOnEveryReadFails() throws IOException {
        Path p = dictionary();
        // Every stream fails after a replace: one restart, then give up.
        var reader = new DriverBackedFileReader("") {
            final AtomicInteger n = new AtomicInteger();

            @Override
            public InputStream getInputStream(String resource) {
                int i = n.incrementAndGet();
                var source = new StreamSourceWithStale(Path.of(resource), 2,
                        () -> UTestRetrySourceChanged.replace(p, UTestRetrySourceChanged.lines(i * 100, 2000)));
                return new RetryStreamSourceWrapper(new FileSourceRetryHandler(10, 1000), source).openClosable();
            }
        };

        var e = Assert.assertThrows(GorSourceChangedException.class,
                () -> Dictionary.getDictionary(p.toString(), reader, workDir.getRoot().toString(), false));
        Assert.assertTrue(e.isFullRetry());
        Assert.assertEquals(2, reader.n.get());
    }
}
