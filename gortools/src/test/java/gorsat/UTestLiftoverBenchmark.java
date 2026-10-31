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
        run("VAR", VAR_INPUT, "-var", VAR_ROWS);
    }

    @Category(SlowTests.class)
    @Test
    public void benchmarkSeg() {
        run("SEG", SEG_INPUT, "-seg", SEG_ROWS);
    }

    private static void run(String mode, String input, String flag, int expectedRows) {
        List<String> table = new ArrayList<>();
        table.add("name\trows\tms\tpeakHeapMB");
        String[][] variants = {
                {"old", input + " | liftover " + chain + " " + flag},
                {"new", input + " | liftover_annotate " + chain + " " + flag},
                {"new+sort", input + " | liftover_annotate " + chain + " " + flag + " | sort genome"}};
        for (String[] v : variants) {
            System.gc();
            resetPeakHeap();
            long start = System.nanoTime();
            int rows = TestUtils.runGorPipeCount(v[1]);
            long ms = (System.nanoTime() - start) / 1_000_000;
            long peakMb = peakHeapBytes() / (1024 * 1024);
            table.add(mode + " " + v[0] + "\t" + rows + "\t" + ms + "\t" + peakMb);
            Assert.assertEquals(mode + " " + v[0], expectedRows, rows);
        }
        System.out.println("LIFTOVER benchmark " + mode + "\n" + String.join("\n", table));
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
