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

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entry point for profiler-attached runs of the table-service memory harness.
 * Run with a prod-like heap and profiling flags, e.g.:
 *   java -Xmx2g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp \
 *        -XX:StartFlightRecording=filename=load.jfr,settings=profile \
 *        -Dgor.memtest.fileCount=20000 -Dgor.memtest.tableCount=16 \
 *        -cp <classpath> org.gorpipe.test.memory.TableServiceLoadMain
 */
public class TableServiceLoadMain {

    public static void main(String[] args) throws Exception {
        MemoryLoadConfig config = MemoryLoadConfig.fromSystemProperties();
        Path root = Files.createTempDirectory("memtest-run");
        System.out.println("Config: " + config);
        System.out.println(runOnce(config, root));
    }

    public static String runOnce(MemoryLoadConfig config, Path root) throws Exception {
        MemorySampler sampler = new MemorySampler(100);
        sampler.start();
        try {
            DictionaryFixture fixture = new DictionaryFixture(root);
            var tables = fixture.createTables(config);
            TableServiceLoadDriver driver = new TableServiceLoadDriver(tables, fixture, config);
            TableServiceLoadDriver.LoadResult result = driver.run();
            System.gc();
            Thread.sleep(200); // let sampler catch post-GC retained heap
            sampler.stop();
            long peakHeapMB = sampler.peakHeapUsedBytes() / (1024 * 1024);
            long peakRssMB = sampler.peakRssBytes() < 0 ? -1 : sampler.peakRssBytes() / (1024 * 1024);
            return "peakHeapMB=" + peakHeapMB + " peakRssMB=" + peakRssMB + " " + result;
        } finally {
            sampler.stop();
        }
    }
}
