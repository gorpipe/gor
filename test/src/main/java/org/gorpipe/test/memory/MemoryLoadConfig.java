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

/** Immutable set of load knobs for the table-service memory harness. */
public final class MemoryLoadConfig {
    public final int fileCount;      // data files per dictionary (~ dictionary lines)
    public final int rowsPerChr;     // rows per chromosome per data file
    public final int bucketSize;     // files per bucket
    public final int tableCount;     // dictionaries held concurrently
    public final int threads;        // load driver concurrency
    public final int readWritePercent; // percent of ops that are reads (0-100)
    public final int durationSeconds;

    public MemoryLoadConfig(int fileCount, int rowsPerChr, int bucketSize, int tableCount,
                            int threads, int readWritePercent, int durationSeconds) {
        this.fileCount = fileCount;
        this.rowsPerChr = rowsPerChr;
        this.bucketSize = bucketSize;
        this.tableCount = tableCount;
        this.threads = threads;
        this.readWritePercent = readWritePercent;
        this.durationSeconds = durationSeconds;
    }

    public static MemoryLoadConfig fromSystemProperties() {
        return new MemoryLoadConfig(
                intProp("gor.memtest.fileCount", 2000),
                intProp("gor.memtest.rowsPerChr", 100),
                intProp("gor.memtest.bucketSize", 100),
                intProp("gor.memtest.tableCount", 8),
                intProp("gor.memtest.threads", Runtime.getRuntime().availableProcessors()),
                intProp("gor.memtest.readWritePercent", 70),
                intProp("gor.memtest.durationSeconds", 60));
    }

    private static int intProp(String key, int def) {
        return Integer.parseInt(System.getProperty(key, String.valueOf(def)));
    }

    @Override
    public String toString() {
        return "MemoryLoadConfig{fileCount=" + fileCount + ", rowsPerChr=" + rowsPerChr
                + ", bucketSize=" + bucketSize + ", tableCount=" + tableCount
                + ", threads=" + threads + ", readWritePercent=" + readWritePercent
                + ", durationSeconds=" + durationSeconds + "}";
    }
}
