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

package org.gorpipe.model.genome.files.binsearch;

import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * @author hjaltii
 */
public class PositionCache {
    private static final Logger log = LoggerFactory.getLogger(PositionCache.class);

    static final int DEFAULT_MAX_NUMBER_OF_FILES_IN_CACHE = 100_000;
    static final int DEFAULT_MAX_NUMBER_OF_POS_PER_GB = 256;

    static int MAX_NUMBER_OF_FILES_IN_CACHE = DEFAULT_MAX_NUMBER_OF_FILES_IN_CACHE;
    static int MAX_NUMBER_OF_POS_PER_GB = DEFAULT_MAX_NUMBER_OF_POS_PER_GB;
    static final int GB = 1024 * 1024 * 1024;

    private static final Map<String, PositionCache> GLOBAL_CACHE =
            new LinkedHashMap<String, PositionCache>(16, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, PositionCache> e) {
                    return size() > getMaxNumberOfFilesInCache();
                }
            };

    static synchronized PositionCache getFilePositionCache(Object user, String filePath, String uniqueId, long fileOffset, long fileSize) {
        final PositionCache candidate = GLOBAL_CACHE.get(filePath);
        final PositionCache toReturn;
        if (candidate == null || !candidate.uniqueId.equals(uniqueId)) {
            final int maxNumberOfPos = getMaxNumberOfPos(fileOffset, fileSize);
            toReturn = new PositionCache(uniqueId, fileOffset, fileSize, maxNumberOfPos);
            if (uniqueId == null || uniqueId.equals("")) {
                GLOBAL_CACHE.remove(filePath);
            } else {
                GLOBAL_CACHE.put(filePath, toReturn);
            }
        } else {
            toReturn = candidate;
        }
        return toReturn;
    }

    public static void clearGlobalCache() {
        GLOBAL_CACHE.clear();
    }

    static void setMaxNumberOfFilesInCache(int n) {
        MAX_NUMBER_OF_FILES_IN_CACHE = n;
    }

    static int getMaxNumberOfFilesInCache() {
        return MAX_NUMBER_OF_FILES_IN_CACHE;
    }

    public static synchronized int getNumFilesInCache() {
        return GLOBAL_CACHE.size();
    }

    public static synchronized int getTotalNumKeysInCache() {
        int n = 0;
        for (PositionCache pc: GLOBAL_CACHE.values()) {
            n += pc.getSize();
        }
        return n;
    }

    static void setMaxNumberOfPosPerGb(int n) {
        MAX_NUMBER_OF_POS_PER_GB = n;
    }

    private PositionCacheEntry[] entries = new PositionCacheEntry[16];
    private int numEntries;

    private final long fileOffset;
    private final long fileSize;
    private int maxNumberOfPos;
    private final String uniqueId;
    private boolean isIndexLoaded;

    PositionCache(long fileOffset, long fileSize, int maxNumberOfPos) {
        this(null, fileOffset, fileSize, maxNumberOfPos);
    }

    PositionCache(String uniqueId, long fileOffset, long fileSize, int maxNumberOfPos) {
        this.uniqueId = uniqueId;
        this.fileOffset = fileOffset;
        this.fileSize = fileSize;
        this.maxNumberOfPos = maxNumberOfPos;
        this.isIndexLoaded = false;
    }

    synchronized Position getLowerBound(StringIntKey key) {
        PositionCacheEntry entry = new PositionCacheEntry(key, 0);
        int index = Arrays.binarySearch(entries, 0, numEntries, entry);

        if (index < 0) {
            index = -index - 1;
        }
        if (index > 0) {
            PositionCacheEntry candidate = entries[index - 1];
            return new Position(candidate.key, candidate.filePosition);
        } else {
            return new Position(null, this.fileOffset);
        }
    }

    synchronized Position getUpperBound(StringIntKey key) {
        PositionCacheEntry entry = new PositionCacheEntry(key, 0);
        int index = Arrays.binarySearch(entries, 0, numEntries, entry);

        if (index < 0) {
            index = -index - 1;
        }
        if (index < numEntries) {
            PositionCacheEntry candidate = entries[index];
            return new Position(candidate.key, candidate.filePosition);
        } else {
            return new Position(null, this.fileSize);
        }
    }

    synchronized void putFilePosition(StringIntKey keyToPut, long posToPut) {
        if (numEntries == entries.length) {
            int newLength =  entries.length*2;
            if (newLength > maxNumberOfPos) {
                newLength = maxNumberOfPos + 1;
            }
            entries = Arrays.copyOf(entries, newLength);
        }

        PositionCacheEntry entry = new PositionCacheEntry(keyToPut, posToPut);
        int index = Arrays.binarySearch(entries, 0, numEntries, entry);
        if (index < 0) {
            int insertionPoint = -index - 1;
            int entriesToShift = numEntries - insertionPoint;
            if (entriesToShift > 0) {
                System.arraycopy(entries, insertionPoint, entries, insertionPoint + 1, entriesToShift);
            }
            entries[insertionPoint] = entry;
            numEntries++;
        }

        if (numEntries > maxNumberOfPos) {
            removeLeastUsefulKey();
        }
    }

    synchronized void removeLeastUsefulKey() {
        int indexOfSmallest = -1;
        long smallestValue = Long.MAX_VALUE;

        for (int i = 1; i < numEntries - 1; i++) {
            PositionCacheEntry left = entries[i-1];
            PositionCacheEntry me = entries[i];
            PositionCacheEntry right = entries[i+1];

            if (left.chromHash != me.chromHash) {
                // First position in chromosome - key is useful
                continue;
            }

            if (right.chromHash != me.chromHash) {
                // Last position in chromosome - key is useful
                continue;
            }

            long value = (right.filePosition - me.filePosition) * (me.filePosition - left.filePosition);
            if (value < smallestValue) {
                smallestValue = value;
                indexOfSmallest = i;
            }
        }

        if (indexOfSmallest >= 0) {
            System.arraycopy(entries, indexOfSmallest + 1, entries, indexOfSmallest, numEntries - indexOfSmallest - 1);
            numEntries--;
            entries[numEntries] = null;
        }
    }

    synchronized int getSize() {
        return numEntries;
    }

    synchronized StringIntKey[] getKeysInCache() {
        StringIntKey[] keys = new StringIntKey[numEntries];
        for (int i = 0; i < numEntries; i++) {
            keys[i] = entries[i].key;
        }
        return keys;
    }

    synchronized long[] getFilePositionsInCache() {
        long[] positions = new long[numEntries];
        for (int i = 0; i < numEntries; i++) {
            positions[i] = entries[i].filePosition;
        }
        return positions;
    }

    synchronized void loadIndex(StreamSourceSeekableFile indexFile) throws IOException {
        if (!this.isIndexLoaded) {
            try (StreamSource dataSource = indexFile.getDataSource();
                 InputStream inputStream = dataSource.open()) {
                GorIndexFile.load(inputStream, this);
            }
            this.isIndexLoaded = true;
        }
    }

    static int getMaxNumberOfPos(long fileOffset, long fileSize) {
        final long size = fileSize - fileOffset;
        final int q = (int) (size / GB);
        if (q * GB == size) return q * MAX_NUMBER_OF_POS_PER_GB;
        else return (q + 1) * MAX_NUMBER_OF_POS_PER_GB;
    }
}
