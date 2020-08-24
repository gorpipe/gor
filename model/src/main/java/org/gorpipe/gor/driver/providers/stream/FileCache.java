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

package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.model.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Very simple LRU file cache.  Files are stored by unique id (should include timestamp information if important).
 * Cache is swept every few hours, deleting least used files if size is above threshold.
 * <p>
 * Created by villi on 08/10/15.
 */
public class FileCache {
    private static final Logger log = LoggerFactory.getLogger(FileCache.class);

    private final String cacheDir;
    private final long maxBytes;
    private Thread thread;

    public FileCache(String cacheDir, long maxBytes) {
        this.cacheDir = cacheDir;
        this.maxBytes = maxBytes;
    }

    public FileCache(GorDriverConfig config) {
        this(config.cacheDir(), config.maxSize().getBytesAsLong());
    }

    /**
     * Store file in cache by unique id.  Data is read from source.
     * Replaces file data if present.
     */
    public File store(String uniqueId, StreamSource source) throws IOException {
        File cacheDirFile = new File(cacheDir);

        cacheDirFile.mkdirs();
        if (cacheDirFile.exists() && cacheDirFile.canWrite()) {
            File f = new File(cachedFilePath(uniqueId));
            // Download to a temporary file path
            File tempFile = new File(cachedFilePath(uniqueId) + "." + UUID.randomUUID());
            try {
                try (FileOutputStream out = new FileOutputStream(tempFile); InputStream in = source.open()) {
                    StreamUtils.readFullyToStream(in, out, 128 * 1024);
                }
                // Atomic move after successful read
                tempFile.renameTo(f);
            } catch (Throwable e) {
                // Best effort - try to delete temp file if it exists
                StreamUtils.tryDelete(tempFile);
                throw e;
            }
            // Write info file - can be used to track back to the origin
            try (FileOutputStream info = new FileOutputStream(cachedFilePath(uniqueId) + ".info")) {
                info.write(("Origin:" + uniqueId + "\n").getBytes());
            }
            return f;
        }
        log.warn("The cacheDir " + cacheDir + " is not writable, file " + source.getName() + " fetched directly");
        return null;
    }

    /**
     * Get file by unique id.  Will update file's timestamp.
     *
     * @return File or null if not present in cache.
     */
    public File get(String uniqueId) {
        File f = new File(cachedFilePath(uniqueId));
        if (f.exists()) {
            // TODO: this is a bad idea, it screws up the RAM file buffering in Linux.
            f.setLastModified(System.currentTimeMillis());
            return f;
        }
        return null;
    }

    /**
     * Ensure stored file does not exist.
     */
    public void delete(String uniqueId) {
        File f = new File(cachedFilePath(uniqueId));
        if (f.exists()) {
            f.delete();
            StreamUtils.tryDelete(new File(f.getAbsolutePath() + ".info"));
        }
    }

    /**
     * Sweep cache. If file usage is larger than max, least recently accessed files are deleted.
     */
    void sweep() {
        log.debug("Sweeping cache directory: {}", cacheDir);
        File[] files = getCachedFiles();

        // Sort by newest first
        // TODO: remember to change this if the above todo is "fixed". Changing  timestamps messes up linux file buffering.
        Arrays.sort(files, (f1, f2) -> Long.valueOf(f2.lastModified()).compareTo(f1.lastModified()));

        long total = 0;
        long totalCount = 0;
        long deleted = 0;
        long deletedCount = 0;
        for (File f : files) {
            if (f.getName().endsWith(".cached")) {
                total += f.length();
                totalCount++;
                if (total > maxBytes) {
                    log.debug("Deleting cached file: {}", f);
                    if (StreamUtils.tryDelete(f)) {
                        deleted += f.length();
                        deletedCount++;
                    }
                    File infoFile = new File(f.getAbsolutePath() + ".info");
                    if (infoFile.exists()) {
                        StreamUtils.tryDelete(infoFile);
                    }
                }
            }
        }
        log.debug("Deleted {}/{} files ({}/{} bytes)", deletedCount, totalCount, deleted, total);
    }

    /**
     * Clear all files from cache.
     */
    public void clear() {
        log.debug("Clearing cache directory: {}", cacheDir);
        File[] files = new File(cacheDir).listFiles();
        for (File f : files) {
            f.delete();
        }
    }

    public void startSweepingThread() {
        if (thread == null) {
            thread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    try {
                        sweep();
                        // Sleep 1-2 hours
                        long hour = 1000 * 3600;
                        Thread.sleep((long) (hour * (1 + Math.random())));
                    } catch (InterruptedException e) {
                        // End if interrupted
                        break;
                    } catch (Exception e) {
                        log.warn("Error in sweep - will resume sleeping", e);
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void stopSweepingThread() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    public String cachedFilePath(String id) {
        return cacheDir + "/" + sourceKey(id) + ".cached";
    }

    private String sourceKey(String sourceId) {
        return Util.md5(sourceId);
    }


    File[] getCachedFiles() {
        File[] result = new File(cacheDir).listFiles((dir, name) -> name.endsWith(".cached"));
        if (result == null) {
            result = new File[0];
        }
        return result;
    }
}
