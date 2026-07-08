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

package org.gorpipe.test.memory;

import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives a mixed read+write load against a set of Gor dictionaries using a thread pool.
 * Reads: tag-filtered {@code filter().tags(..).get()}. Writes: insert a new data line + save.
 */
public class TableServiceLoadDriver {
    private final List<Path> tablePaths;
    private final DictionaryFixture fixture;
    private final MemoryLoadConfig config;

    public TableServiceLoadDriver(List<Path> tablePaths, DictionaryFixture fixture, MemoryLoadConfig config) {
        this.tablePaths = tablePaths;
        this.fixture = fixture;
        this.config = config;
    }

    public LoadResult run() throws InterruptedException {
        AtomicLong readOps = new AtomicLong();
        AtomicLong writeOps = new AtomicLong();
        AtomicLong errors = new AtomicLong();
        long deadline = System.nanoTime() + config.durationSeconds * 1_000_000_000L;

        ExecutorService pool = Executors.newFixedThreadPool(config.threads);
        for (int i = 0; i < config.threads; i++) {
            pool.submit(() -> {
                while (System.nanoTime() < deadline) {
                    Path gord = tablePaths.get(ThreadLocalRandom.current().nextInt(tablePaths.size()));
                    try {
                        if (ThreadLocalRandom.current().nextInt(100) < config.readWritePercent) {
                            doRead(gord);
                            readOps.incrementAndGet();
                        } else {
                            doWrite(gord);
                            writeOps.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(config.durationSeconds + 30, TimeUnit.SECONDS);
        return new LoadResult(readOps.get(), writeOps.get(), errors.get());
    }

    private void doRead(Path gord) {
        GorDictionaryTable table = fixture.openTable(gord);
        String tag = "PN" + ThreadLocalRandom.current().nextInt(config.fileCount);
        table.filter().tags(tag).get();
    }

    private void doWrite(Path gord) throws IOException {
        GorDictionaryTable table = fixture.openTable(gord);
        int id = ThreadLocalRandom.current().nextInt(1_000_000);
        // insert() validates that the referenced file exists and its header column
        // count matches the dictionary's existing entries, so write a minimal real
        // data file (matching DictionaryFixture/GorDictionarySetup's 6-column layout)
        // before registering it.
        Path dataFile = gord.getParent().resolve("memtest_extra_" + id + ".gor");
        try (PrintWriter out = new PrintWriter(dataFile.toFile())) {
            out.println("Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData");
            out.println("chr1\t1\tEXTRA" + id + "\tinfo\tconst\t1");
        }
        // insert(String...) accepts raw dictionary lines: "file\talias".
        table.insert(dataFile + "\tEXTRA" + id);
        table.save();
    }

    public static class LoadResult {
        public final long readOps;
        public final long writeOps;
        public final long errors;
        public LoadResult(long readOps, long writeOps, long errors) {
            this.readOps = readOps; this.writeOps = writeOps; this.errors = errors;
        }
        @Override public String toString() {
            return "reads=" + readOps + " writes=" + writeOps + " errors=" + errors;
        }
    }
}
