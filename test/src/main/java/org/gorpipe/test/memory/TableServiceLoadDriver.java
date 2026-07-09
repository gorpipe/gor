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

import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.gorpipe.gor.table.lock.ExclusiveFileTableLock;
import org.gorpipe.gor.table.lock.TableTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives a mixed read+write load against a set of Gor dictionaries using a thread pool.
 * Reads: tag-filtered {@code filter().tags(..).get()}. Writes: insert a new data line + save.
 *
 * <p>Read-side table instances are cached (keyed by dictionary path) so their in-memory
 * {@code DictionaryEntries} stay retained for the lifetime of the run, modeling a
 * session/table cache in the real Table Service. Writes deliberately bypass the cache and
 * always go through the faithful locked-transaction path on a fresh instance.
 */
public class TableServiceLoadDriver {
    private static final Logger log = LoggerFactory.getLogger(TableServiceLoadDriver.class);

    private final List<Path> tablePaths;
    private final DictionaryFixture fixture;
    private final MemoryLoadConfig config;
    private final ConcurrentHashMap<Path, GorDictionaryTable> tableCache = new ConcurrentHashMap<>();

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

        // Pre-open all tables into the cache. NOTE: this builds the table shells only;
        // each table's DictionaryEntries (the retained heap payload) loads lazily on its
        // first read, so full retention assumes every table gets at least one read during
        // the run (guaranteed at the validation config: tableCount=4, threads=4, 3s).
        for (Path p : tablePaths) tableCache.computeIfAbsent(p, fixture::openTable);

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
                        log.debug("load op failed: {}: {}", e.getClass().getSimpleName(), e.getMessage());
                    }
                }
            });
        }
        pool.shutdown();
        boolean terminated = pool.awaitTermination(config.durationSeconds + 30, TimeUnit.SECONDS);
        if (!terminated) {
            // Workers are still alive well past the expected run time; force them down so
            // they don't leak and keep mutating state after run() has returned.
            pool.shutdownNow();
        }
        return new LoadResult(readOps.get(), writeOps.get(), errors.get());
    }

    private void doRead(Path gord) {
        // Reads use the shared cached instance so its DictionaryEntries stay retained.
        // NOTE: concurrent reads share DictionaryEntries.getEntries(), whose lazy-load
        // guard (non-volatile 'dataLoaded' double-checked outside the synchronized
        // loadLinesAndUpdateIndices()) is a pre-existing JMM data race in product code.
        // loadLinesAndUpdateIndices() is idempotent+synchronized so the worst case here
        // is a redundant reload, not corruption. See ENGKNOW-3657 findings.
        GorDictionaryTable table = tableCache.get(gord);
        String tag = "PN" + ThreadLocalRandom.current().nextInt(config.fileCount);
        table.filter().tags(tag).get();
    }

    private void doWrite(Path gord) throws IOException {
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

        // Write through the same exclusive-lock write path the real Table Service uses
        // (see TableManager#insert): opening a write transaction acquires the exclusive
        // file lock, reloads the table's fresh on-disk state under that lock, and (on
        // commit + close) saves under the same lock. This serializes concurrent writers
        // to the same dictionary instead of racing on independent last-writer-wins temp
        // files, which was silently dropping inserts under contention.
        GorDictionaryTable table = fixture.openTable(gord);
        try (TableTransaction trans = TableTransaction.openWriteTransaction(
                ExclusiveFileTableLock.class, table, table.getName(), TableManager.DEFAULT_LOCK_TIMEOUT)) {
            // insert(String...) accepts raw dictionary lines: "file\talias".
            table.insert(dataFile + "\tEXTRA" + id);
            trans.commit();
        }
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
