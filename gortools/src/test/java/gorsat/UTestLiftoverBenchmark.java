/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package gorsat;

import org.gorpipe.test.SlowTests;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Benchmark of LIFTOVER (query string expansion) against LIFTOVER_ANNOTATE on a synthetic chain.
 * Records wall time and peak heap per query. There are no timing assertions, only row counts.
 */
public class UTestLiftoverBenchmark {

    private static final int VAR_ROWS = 1_000_000;
    private static final int SEG_ROWS = 100_000;
    private static final int REPEATS = 3;

    private static final String VAR_INPUT = "gorrows -p chr1:1-1000001 | calc Ref 'A' | calc Alt 'G'";
    private static final String SEG_INPUT = "gorrows -p chr1:1-1000001 -segment 50 -step 10";

    private static Path tmpdir;
    private static String chain;

    @BeforeClass
    public static void setUp() throws IOException {
        tmpdir = Files.createTempDirectory("liftover-benchmark");
        StringBuilder sb = new StringBuilder("Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore\n");
        for (int i = 0; i < 2000; i++) {
            sb.append("chr1\t").append(i * 1000).append('\t').append(i * 1000 + 990)
                    .append("\tchr1\t").append(i * 1000 + 5).append('\t').append(i * 1000 + 995)
                    .append('\t').append(i % 2 == 0 ? '+' : '-').append("\t1000\n");
        }
        Path file = tmpdir.resolve("benchmark_chain.gor");
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        chain = file.toAbsolutePath().toString();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (tmpdir != null) {
            try (Stream<Path> walk = Files.walk(tmpdir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Category(SlowTests.class)
    @Test
    public void benchmarkVar() {
        run("VAR", VAR_ROWS, List.of(
                new Variant("old", VAR_INPUT + " | liftover " + chain + " -var"),
                new Variant("new", VAR_INPUT + " | liftover_annotate " + chain + " -var"),
                new Variant("new+sort", VAR_INPUT + " | liftover_annotate " + chain + " -var | sort genome")));
    }

    @Category(SlowTests.class)
    @Test
    public void benchmarkSeg() {
        run("SEG", SEG_ROWS, List.of(
                new Variant("old", SEG_INPUT + " | liftover " + chain + " -seg"),
                new Variant("new", SEG_INPUT + " | liftover_annotate " + chain + " -seg"),
                new Variant("new+sort", SEG_INPUT + " | liftover_annotate " + chain + " -seg | sort genome")));
    }

    /** A labelled query to benchmark. */
    private record Variant(String label, String query) { }

    /**
     * Runs every variant once untimed (warm-up), then times each variant {@link #REPEATS} times, rotating the
     * start variant per round. Prints each timed run as it is measured, then the median ms and max peak heap
     * per variant. Every run asserts the row count.
     */
    private static void run(String mode, int expectedRows, List<Variant> variants) {
        int n = variants.size();
        for (Variant v : variants) {
            Assert.assertEquals(mode + " " + v.label() + " warm-up", expectedRows, TestUtils.runGorPipeCount(v.query()));
        }
        List<List<Long>> msRuns = new ArrayList<>();
        long[] maxPeakMb = new long[n];
        for (int i = 0; i < n; i++) {
            msRuns.add(new ArrayList<>());
        }
        System.out.println("LIFTOVER benchmark " + mode + " (peakHeapMB = sum of heap-pool peaks)");
        System.out.println("name\trun\trows\tms\tpeakHeapMB");
        for (int round = 0; round < REPEATS; round++) {
            for (int k = 0; k < n; k++) {
                int idx = (round + k) % n;
                Variant v = variants.get(idx);
                System.gc();
                resetPeakHeap();
                long start = System.nanoTime();
                int rows = TestUtils.runGorPipeCount(v.query());
                long ms = (System.nanoTime() - start) / 1_000_000;
                long peakMb = peakHeapBytes() / (1024 * 1024);
                msRuns.get(idx).add(ms);
                maxPeakMb[idx] = Math.max(maxPeakMb[idx], peakMb);
                System.out.println(mode + " " + v.label() + "\t" + (round + 1) + "\t" + rows + "\t" + ms + "\t" + peakMb);
                Assert.assertEquals(mode + " " + v.label(), expectedRows, rows);
            }
        }
        System.out.println("LIFTOVER benchmark " + mode + " summary (median of " + REPEATS
                + " runs, peakHeapMB = max of sum of heap-pool peaks)");
        System.out.println("name\trows\tmedianMs\tmaxPeakHeapMB");
        for (int i = 0; i < n; i++) {
            List<Long> sorted = new ArrayList<>(msRuns.get(i));
            Collections.sort(sorted);
            System.out.println(mode + " " + variants.get(i).label() + "\t" + expectedRows + "\t"
                    + sorted.get(sorted.size() / 2) + "\t" + maxPeakMb[i]);
        }
    }

    private static void resetPeakHeap() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                pool.resetPeakUsage();
            }
        }
    }

    private static long peakHeapBytes() {
        long total = 0;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                total += pool.getPeakUsage().getUsed();
            }
        }
        return total;
    }
}
