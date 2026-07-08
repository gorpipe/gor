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

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Background thread sampling heap-used (via MXBean) and RSS (via /proc on Linux). */
public class MemorySampler {
    private final long intervalMillis;
    private volatile boolean running;
    private volatile long peakHeap;
    private volatile long peakRss = -1;
    private Thread thread;

    public MemorySampler(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    public void start() {
        running = true;
        thread = new Thread(this::loop, "memory-sampler");
        thread.setDaemon(true);
        thread.start();
    }

    private void loop() {
        while (running) {
            sampleOnce();
            try { Thread.sleep(intervalMillis); } catch (InterruptedException e) { return; }
        }
    }

    private void sampleOnce() {
        long heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        if (heap > peakHeap) peakHeap = heap;
        long rss = readRssBytes();
        if (rss > peakRss) peakRss = rss;
    }

    /** Reads VmRSS from /proc/self/status. Returns -1 if unavailable (e.g. macOS). */
    private long readRssBytes() {
        Path status = Path.of("/proc/self/status");
        if (!Files.exists(status)) return -1;
        try {
            List<String> lines = Files.readAllLines(status);
            for (String line : lines) {
                if (line.startsWith("VmRSS:")) {
                    String[] parts = line.trim().split("\\s+"); // "VmRSS:  12345 kB"
                    return Long.parseLong(parts[1]) * 1024;
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
        sampleOnce();
    }

    public long peakHeapUsedBytes() { return peakHeap; }
    public long peakRssBytes() { return peakRss; }
    public long currentHeapUsedBytes() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }
}
