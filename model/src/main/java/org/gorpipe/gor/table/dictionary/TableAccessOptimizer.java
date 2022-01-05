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

package org.gorpipe.gor.table.dictionary;

import org.gorpipe.gor.util.IntHashMap;
import org.gorpipe.util.collection.IntArray;
import org.gorpipe.util.collection.IntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Optimize file access
 * <p>
 * Created by gisli on 25/07/16.
 */
public class TableAccessOptimizer {

    private static final Logger log = LoggerFactory.getLogger(TableAccessOptimizer.class);

    private final BaseDictionaryTable table;
    private final ArrayList<DictionaryEntry> lines = new ArrayList<>();
    public boolean hasBuckets = false;

    private boolean removeFirstIfMoreThan1 = false; // True if first element of files list should be removed if anything other is added
    private final HashMap<String, BucketEntry> mapBucketIndex = new HashMap<>(); // Map of found buckets to allocated indices
    private final ArrayList<BucketEntry> mapIndexBuckets = new ArrayList<>(); // The bucket files
    private final IntArray bucketTotalFileCounts = new IntArray(); // Count for each found bucket, the total number of files in the bucket
    private final IntHashMap mapBucketUsedFileCounts = new IntHashMap(); // Count for each used bucket, the number of files from the bucket used
    private final IntHashMap mapFileBucket = new IntHashMap(); // Map index of file to the bucket the file is alternatively stored in

    /**
     *
     */
    public TableAccessOptimizer(BaseDictionaryTable table) {
        this.table = table;
    }

    public void update(Collection<? extends DictionaryEntry> inputLines, Map<Integer, Set<String>> columnTags, boolean allowBucketAccess) {
        this.lines.clear();
        this.hasBuckets = false;

        final boolean hasTags = columnTags.containsKey(3); // Does the qeury specify tag filtering.

        for (DictionaryEntry dictLine : inputLines) {
            int bucketId = -1;
            String alias = dictLine.getAlias();
            if (dictLine.hasBucket()) {
                if (dictLine.isDeleted()) {
                    addTagForDeletedFile(dictLine.getBucket(), alias);
                } else if (dictLine.hasBucket()) {
                    bucketId = addFileBucket(dictLine.getBucket(), alias);
                }
            }

            if (!dictLine.isDeleted()) {
                // Optimization, reduce the maximum number of files needed to be opened by applying tag filter ASAP, if tag filter is used.
                // For Gor Server this allows more accurate number of files to be opened by a query
                if (alias == null || !hasTags || match(dictLine.getTags(), columnTags.get(3), alias)) { // Do not include files that will not be used
                    addSource(dictLine, bucketId);
                } else if (this.lines.size() == 0) {
                    addFirstSource(dictLine, bucketId);
                }
            }
        }

        optimizeFileList(allowBucketAccess);
    }

    public static boolean match(Collection<String> lineTags, Set<String> queryTags, String alias) {
        if (lineTags == null || lineTags.size() == 0) {
            if (alias != null) return queryTags.contains(alias) || queryTags.isEmpty();
            return queryTags.isEmpty();
        }
        if (queryTags.size() >= lineTags.size()) {
            for (String tag : lineTags) {
                if (queryTags.contains(tag)) return true;
            }
        } else {
            for (String tag : queryTags) {
                if (lineTags.contains(tag)) return true;
            }
        }
        return false;
    }

    private static boolean match(String[] lineTags, Set<String> queryTags, String alias) {
        return match(Arrays.asList(lineTags), queryTags, alias);
    }

    public boolean hasBuckets() {
        return hasBuckets;
    }

    public List<? extends DictionaryEntry> getLines() {
        return this.lines;
    }

    private int addFileBucket(final String bucket, final String filetag) {
        BucketEntry entry = getEntryEnsureExits(bucket);
        entry.tags.add(filetag);
        bucketTotalFileCounts.increment(entry.id);
        return entry.id;
    }

    private void addTagForDeletedFile(final String bucket, final String filetag) {
        BucketEntry entry = getEntryEnsureExits(bucket);
        entry.tags.add(filetag);
        bucketTotalFileCounts.increment(entry.id);
    }

    // TODO: Check if need to normalize the bucket.
    private BucketEntry getEntryEnsureExits(final String bucket) {
        BucketEntry entry = mapBucketIndex.get(bucket);
        if (entry == null) {
            entry = new BucketEntry(mapBucketIndex.size(), bucket);
            mapBucketIndex.put(bucket, entry);
            mapIndexBuckets.add(entry);
            bucketTotalFileCounts.add(0);
        }
        return entry;
    }

    private void addSource(DictionaryEntry srf, int bucketId) {
        if (removeFirstIfMoreThan1 && this.lines.size() == 1) { // First file was added to ensure at least one file was found, but is not to be used
            removeFirstIfMoreThan1 = false;
            if (mapFileBucket.size() > 0) { // Only remove from bucket info, if a bucket is available
                final int bucket = mapFileBucket.get(0);
                mapBucketUsedFileCounts.decrement(bucket); // Must update the bucket statistics
            }
            this.lines.remove(0); // Remove the file
            // TODO if this was an remote reference, more is needed, i.e. remove from filelist in remote options
        }

        this.lines.add(srf);

        if (bucketId >= 0) {
            mapBucketUsedFileCounts.increment(bucketId);
            mapFileBucket.put(this.lines.size() - 1, bucketId);
        }
    }

    private void addFirstSource(DictionaryEntry srf, int bucketId) {
        addSource(srf, bucketId);
        removeFirstIfMoreThan1 = true; // Always include at least one file to ensure empty queries do not resolve with an error
    }

    public void optimizeFileList(boolean allowBucketAccess) {
        if (mapFileBucket.size() == 0) {
            return; // Nothing to optimize since there are no merged bucket files
        }

        this.hasBuckets = true;

        // Optimize access, using bucket files if such access is available and is deemed to be faster.
        // Use bucket instead of file if:
        // 1. Utilization of the bucket is over BUCKET_USAGE_THRESHOLD.
        // 2. We have more than FILE_COUNT_THRESHOLD of unbucketed file reads, then use the bucket with the most files (not utilization) that
        //    has utilization over the BUCKET_REJECT_THRESHOLD.  Repeat untill file reads from unbucketed files goes below FILE_COUNT_THRESHOLD.
        if (allowBucketAccess && mapIndexBuckets.size() > 0) {
            final int[] buckets = mapBucketUsedFileCounts.keysToArray(); // Bucket IDs
            final int[] counts = mapBucketUsedFileCounts.valuesToArray(); // Count of files used for each bucket ID
            final int[] countsOriginal = mapBucketUsedFileCounts.valuesToArray(); // Copy of counts, due to counts being modified
            final IntHashSet replace = new IntHashSet(); // Set of buckets that are to be used instead of the individual files
            final IntHashSet include = new IntHashSet(); // Set of buckets not used, i.e. individual files will be used
            int totalFileReads = this.lines.size();
            final float BUCKET_USAGE_THRESHOLD = Integer.parseInt(System.getProperty("gor.bucket.usage.threshold", "75")) / 100f;
            final float BUCKET_REJECT_THRESHOLD = Integer.parseInt(System.getProperty("gor.bucket.reject.threshold", "20")) / 100f;
            final int FILE_COUNT_THRESHOLD = Integer.parseInt(System.getProperty("gor.bucket.file.count.threshold", "300"));

            // Find bucket replacements for files, based on buckets usages > threshold
            for (int idx = 0; idx < buckets.length; idx++) {
                final int bucket = buckets[idx];
                final float T = bucketTotalFileCounts.get(bucket);
                final float usage = counts[idx] / T;
                if (usage >= BUCKET_USAGE_THRESHOLD) {
                    log.debug("Use bucket {} for {} files (total for bucket is {} or {}%)", mapIndexBuckets.get(bucket), counts[idx], (int) T, (int) (100 * usage));
                    replace.add(bucket);
                    totalFileReads -= counts[idx];
                    counts[idx] = 0;
                } else {
                    include.add(bucket); // This bucket will not be used so all files refering it will be directly included
                }
            }

            // A lot of files are being referenced, try replacing with bucket access to reduce number of files
            if (totalFileReads >= FILE_COUNT_THRESHOLD) {
                // Find bucket replacements for files, based on highest files cnt
                final Integer[] idx = IntArray.toIntegerArray(IntArray.sequence(0, buckets.length));
                Arrays.sort(idx, new Comparator<Integer>() {
                    @Override
                    public int compare(final Integer o1, final Integer o2) {
                        return Integer.compare(counts[o2], counts[o1]);
                    }
                });

                for (int i = 0; i < idx.length && totalFileReads >= FILE_COUNT_THRESHOLD; i++) {
                    final int index = idx[i];
                    final int bucket = buckets[index];
                    final float T = bucketTotalFileCounts.get(bucket);
                    final float usage = counts[index] / T;
                    // Will individual files for this bucket be directly read? AND is enough of the bucket file being read
                    if (include.contains(bucket) && usage >= BUCKET_REJECT_THRESHOLD) {
                        replace.add(bucket);
                        include.remove(bucket);
                        totalFileReads -= counts[index];
                        counts[index] = 0;
                    }
                }
            }

            // At this time we know which buckets to use instead of files, so recreate the files list with that in mind
            final ArrayList<DictionaryEntry> oldFiles = new ArrayList<>(this.lines);
            this.lines.clear();
            final IntHashSet toBeReplaced = new IntHashSet(replace); // Clone to be used in logging
            int cntIndividualFiles = 0;
            for (int idx = 0; idx < oldFiles.size(); idx++) {
                final int bucket = mapFileBucket.get(idx, -1);
                if (replace.contains(bucket)) { // All files from this bucket will be replaced with the bucket
                    final BucketEntry entry = mapIndexBuckets.get(bucket);

                    if (log.isTraceEnabled()) {
                        log.trace("Bucket used={} with tags={}", entry.file, entry.tags.toString());
                    }
                    // Fake dictionary line.
                    this.lines.add((DictionaryEntry)new DictionaryEntry.Builder<>(entry.file, table.getRootUri()).sourceInserted(true).tags(entry.tags).build());
                    replace.remove(bucket);
                } else if (bucket == -1 || include.contains(bucket)) { // all files from this bucket are to be included as they were
                    if (log.isTraceEnabled()) {
                        log.trace("Include {}", oldFiles.get(idx));
                    }
                    this.lines.add(oldFiles.get(idx));
                    cntIndividualFiles++;
                } else { // else we skip the file since it is from a bucket that is going to be included
                    if (log.isTraceEnabled()) {
                        log.trace("Skipping {} with idx {} from bucket {}", oldFiles.get(idx), idx, bucket);
                    }
                }
            }
            if (log.isDebugEnabled()) {
                int totalFilesFromBuckets = 0;
                final StringBuilder sb = new StringBuilder();
                for (int bucket : toBeReplaced.toArray()) {
                    final BucketEntry entry = mapIndexBuckets.get(bucket);
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    if (bucket < countsOriginal.length) {
                        totalFilesFromBuckets += countsOriginal[bucket];
                        sb.append(entry.file).append(" (").append(countsOriginal[bucket]).append(")");
                    } else {
                        sb.append(entry.file).append(" (!!! Error: bucket not found in counts original !!!)");
                    }
                }
                log.debug("Individual Files={}, Buckets={} for {} tags { {} }", cntIndividualFiles, toBeReplaced.size(), totalFilesFromBuckets, sb);

                if (log.isTraceEnabled()) {
                    log.trace("Files={} -> {}", this.lines.size(), Arrays.toString(this.lines.toArray()));
                }
            }
        }
    }

    private static class BucketEntry {
        final int id;
        final String file;
        final ArrayList<String> tags = new ArrayList<String>();

        BucketEntry(int index, String file) {
            this.id = index;
            this.file = file;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            return id == ((BucketEntry) obj).id;
        }

        @Override
        public String toString() {
            return "{" + id + " : " + file + "}";
        }
    }

}
