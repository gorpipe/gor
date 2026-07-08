package org.gorpipe.gor.table.dictionary;

import org.gorpipe.gor.table.dictionary.gor.GorDictionaryEntry;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stress test for concurrent reads on a single shared {@link GorDictionaryTable} — the access
 * pattern the table service uses when many selects share one cached dictionary. Guards against
 * the lazy-load safe-publication race (transient NPE / torn read) and against
 * ConcurrentModificationException on the shared backing list.
 */
public class UTestDictionaryEntriesConcurrency {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private static final int ENTRIES = 500;

    private GorDictionaryTable buildTable() throws Exception {
        Path dir = workDir.getRoot().toPath();
        for (int i = 0; i < ENTRIES; i++) {
            try (PrintWriter out = new PrintWriter(dir.resolve("f" + i + ".gor").toFile())) {
                out.println("chrom\tpos");
                out.println("chr1\t" + (i + 1));
            }
        }
        Path gord = dir.resolve("big.gord");
        try (PrintWriter out = new PrintWriter(gord.toFile())) {
            for (int i = 0; i < ENTRIES; i++) out.println("f" + i + ".gor\tpn" + i);
        }
        return new GorDictionaryTable.Builder<>(gord.toString()).build();
    }

    @Test
    public void concurrentReadsOnSharedInstance_noRaceNoCorruption() throws Exception {
        GorDictionaryTable table = buildTable();

        int threads = 16;
        int opsPerThread = 200;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        AtomicInteger tagHits = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final int base = t;
            Thread th = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        // full scan read (exercises getEntries()/rawLines lazy publication)
                        List<GorDictionaryEntry> all = table.filter().get();
                        Assert.assertEquals(ENTRIES, all.size());
                        // tag-filtered read (exercises tagHashToLines lazy build)
                        int idx = (base * 7 + i) % ENTRIES;
                        List<GorDictionaryEntry> hit = table.filter().tags("pn" + idx).get();
                        if (hit.size() == 1) tagHits.incrementAndGet();
                    }
                } catch (Throwable e) {
                    failures.add(e);
                } finally {
                    done.countDown();
                }
            });
            th.start();
        }

        start.countDown();
        Assert.assertTrue("threads did not finish", done.await(60, TimeUnit.SECONDS));
        Assert.assertTrue("concurrent reads threw: " + failures, failures.isEmpty());
        Assert.assertEquals("every tag lookup should match exactly one entry",
                threads * opsPerThread, tagHits.get());
    }

    @Test
    public void concurrentGetOptimizedLines_singleSafeOptimizer() throws Exception {
        GorDictionaryTable table = buildTable();

        int threads = 16;
        int opsPerThread = 200;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int base = t;
            Thread th = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        Set<String> tags = Collections.singleton("pn" + ((base * 7 + i) % ENTRIES));
                        // getOptimizedLines lazily builds the shared table access optimizer.
                        List<GorDictionaryEntry> res = table.getOptimizedLines(tags, true, false);
                        Assert.assertEquals(1, res.size());
                    }
                } catch (Throwable e) {
                    failures.add(e);
                } finally {
                    done.countDown();
                }
            });
            th.start();
        }

        start.countDown();
        Assert.assertTrue("threads did not finish", done.await(60, TimeUnit.SECONDS));
        Assert.assertTrue("concurrent getOptimizedLines threw: " + failures, failures.isEmpty());
    }
}
