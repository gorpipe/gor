package org.gorpipe.gor.table.dictionary;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.table.TableInfo;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.util.collection.IntArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Based on the optimizer from Dictionary.
 */
public class DefaultTableAccessOptimizer implements TableAccessOptimizer {

    private static final Logger log = LoggerFactory.getLogger(DefaultTableAccessOptimizer.class);

    private final Cache<String, List<DictionaryEntry>> tagsToListCache = CacheBuilder.newBuilder().maximumSize(100).build();   //A map from dictionaries to the cache objects.

    private static final boolean USE_CACHE = true;

    private final ITableEntries<DictionaryEntry> tableEntries;
    private final TableInfo table;

    private final List<Set<String>> bucketTagsList = new ArrayList<>();
    private final List<String> resetBucketNames = new ArrayList<>();
    private final IntArray bucketTotalCount = new IntArray();
    private final IntArray bucketActiveCount = new IntArray();
    private final Map<String, Integer> bucketToIdx = new HashMap<>();

    boolean isStatsUpdated = false;

    final Multimap<String, String> bucketHasDeletedFile = ArrayListMultimap.create(); //This is changed if we find a deleted line with bucket.

    /**
     *
     */
    public DefaultTableAccessOptimizer(TableInfo table, ITableEntries<DictionaryEntry> tableEntries) {
        this.table = table;
        this.tableEntries = tableEntries;
    }

    @Override
    synchronized public List<DictionaryEntry> getOptimizedEntries(Set<String> tags, boolean allowBucketAccess, boolean isSilentTagFilter) {
        if (!isStatsUpdated && !tableEntries.getEntries().isEmpty()) {
            updateStats();
        }

        List<DictionaryEntry> result;
        final Set<String> badTags = new HashSet<>();
        if (USE_CACHE) {
            String key = findKey(tags, allowBucketAccess, isSilentTagFilter);
            result = tagsToListCache.getIfPresent(key);
            if (result == null) {
                result = calcOptimizeFileList(tags, allowBucketAccess, badTags);
                tagsToListCache.put(key, result);
            }

            // Removing the entry from cache due to invalid tags should be separate from throwing the data exception
            if (!badTags.isEmpty()) {
                tagsToListCache.invalidate(key);
            }
        } else {
            result = calcOptimizeFileList(tags, allowBucketAccess, badTags);
        }

        // Here we throw the data exception if the call is not silent
        if (!badTags.isEmpty() && !isSilentTagFilter) {
            throwBadTagException(badTags);
        }

        if (result.size() == 0) {
            //Must return a dummy line.
            if (tableEntries.getActiveLines().size() > 0) {
                return List.of(tableEntries.getActiveLines().get(0));
            } else {
                return new ArrayList<>();
            }
        } else {
            return result;
        }
    }

    @Override
    synchronized public Collection<String> getBucketDeletedFiles(String bucket) {
        return this.bucketHasDeletedFile.get(bucket);
    }

    private static String findKey(Collection<String> tags, boolean allowBucketAccess, boolean isSilentTagFilter) {
        if (tags == null) return ":" + allowBucketAccess + ":" + isSilentTagFilter;
        final String[] stringsAsArray = tags.toArray(new String[0]);
        Arrays.sort(stringsAsArray);
        return String.join(",", stringsAsArray) + ":" + allowBucketAccess + ":" + isSilentTagFilter;
    }

    private void throwBadTagException(Set<String> badTags) {
        String message = "Invalid Source Filter for dictionary file: " + table.getPath() + ". ";
        if (badTags.contains("")) {
            message += "Empty tag is not allowed";
        } else {
            message += "Following are not in dictionary " + String.join(",", badTags);
        }
        throw new GorDataException(message);
    }

    /**
     *
     */
    private void updateStats() {
        for (DictionaryEntry entry : tableEntries.getEntries()) {
            processEntry(entry);
        }
        isStatsUpdated = true;
    }

    private void processEntry(DictionaryEntry entry) {
        if (entry.hasBucket()) {
            final int bucketIdx = bucketToIdx.computeIfAbsent(entry.getBucket(), bucket -> {
                resetBucketNames.add(entry.bucketLogical);
                bucketTagsList.add(new HashSet<>());
                bucketActiveCount.add(0);
                bucketTotalCount.add(0);
                return bucketToIdx.size();
            });
            bucketTotalCount.increment(bucketIdx);
            if (entry.isDeleted()) {
                bucketHasDeletedFile.put(PathUtils.getFileName(entry.getBucket()), entry.getAlias());
                bucketTagsList.get(bucketIdx).add(entry.getAlias());
            } else {
                bucketActiveCount.increment(bucketIdx);
                bucketTagsList.get(bucketIdx).addAll(Arrays.asList(entry.getFilterTags()));
            }
        }
    }

    private List<DictionaryEntry> calcOptimizeFileList(Set<String> tags, boolean allowBucketAccess, Set<String> badTags) {
        final DictionaryEntry[] filesToOptimize;
        final IntArray bucketUsedCounts = new IntArray();
        final IntArray localBucketTotalCount = new IntArray();
        final List<String> localResetBucketNames = new ArrayList<>();
        final Map<String, Integer> newBucketToIdx;
        final int[] bucketUsedCountsArray;
        final int[] bucketTotalCountArray;
        final String[] resetBucketNamesArray;
        final Set<String>[] bucketTagsArray;
        int numberOfFilesWithoutBucket = 0;
        final List<Set<String>> localBucketTagsList = new ArrayList<>();

        if (tags != null && !tags.isEmpty()) {
            Set<DictionaryEntry> filesToOptimizeTmp = new LinkedHashSet<>();
            newBucketToIdx = new HashMap<>();
            Set<String> goodTags = new HashSet<>();
            for (String tag : tags) {
                if (tableEntries.getAllActiveTags().contains(tag)) {
                    goodTags.add(tag);
                } else {
                    badTags.add(tag);
                }
            }

            for (DictionaryEntry entry : tableEntries.getEntries(goodTags.toArray(String[]::new))) {
                if (!entry.isDeleted) {
                    filesToOptimizeTmp.add(entry);
                    final String bucket = entry.getBucket();
                    if (bucket == null) numberOfFilesWithoutBucket++;
                    else {
                        final int bucketIdx = newBucketToIdx.computeIfAbsent(bucket, bucketbucket -> {
                            bucketUsedCounts.add(0);
                            final int idx = this.bucketToIdx.get(bucketbucket);
                            localBucketTotalCount.add(this.bucketTotalCount.get(idx));
                            localResetBucketNames.add(this.resetBucketNames.get(idx));
                            localBucketTagsList.add(this.bucketTagsList.get(idx));
                            return newBucketToIdx.size();
                        });
                        bucketUsedCounts.increment(bucketIdx);
                    }
                }
            }

            filesToOptimize = new DictionaryEntry[filesToOptimizeTmp.size()];
            int idx = 0;
            for (DictionaryEntry i : filesToOptimizeTmp) filesToOptimize[idx++] = i;
            bucketUsedCountsArray = bucketUsedCounts.toArray();
            bucketTotalCountArray = localBucketTotalCount.toArray();
            resetBucketNamesArray = localResetBucketNames.toArray(new String[0]);
            bucketTagsArray = localBucketTagsList.toArray(new Set[0]);
        } else {
            filesToOptimize = new DictionaryEntry[tableEntries.getActiveLines().size()];
            for (int i = 0; i < filesToOptimize.length; i++) {
                DictionaryEntry entry = tableEntries.getActiveLines().get(i);
                if (entry.getBucket() == null) numberOfFilesWithoutBucket++;
                filesToOptimize[i] = entry;
            }
            bucketUsedCountsArray = this.bucketActiveCount.toArray();
            bucketTotalCountArray = this.bucketTotalCount.toArray();
            resetBucketNamesArray = this.resetBucketNames.toArray(String[]::new);
            newBucketToIdx = this.bucketToIdx;
            bucketTagsArray = this.bucketTagsList.toArray(Set[]::new);
        }

        final List<DictionaryEntry>  fileList;
        if (allowBucketAccess && this.bucketTotalCount.size() != 0) {
            fileList = getOptimizedFileList(bucketTotalCountArray, bucketUsedCountsArray, bucketTagsArray, filesToOptimize,
                    numberOfFilesWithoutBucket, newBucketToIdx, resetBucketNamesArray);
        } else {
            fileList = new ArrayList<>();
            for (int i = 0; i < filesToOptimize.length; ++i) {
                fileList.add(filesToOptimize[i]);
            }
        }
        return fileList;
    }

    private List<DictionaryEntry> getOptimizedFileList(int[] bucketTotalFileCounts, int[] bucketUsedCounts, Set<String>[] bucketTagsArray,
                                                       DictionaryEntry[] fileListToOptimize, int numberOfFilesWithoutBucket,
                                                       Map<String, Integer> bucketsToIdx, String[] resetBucketNames) {
        final int numberOfBuckets = bucketUsedCounts.length;
        final boolean[] replace = new boolean[numberOfBuckets]; //replace[i] = bucket i will be used
        final boolean[] include = new boolean[numberOfBuckets]; //include[i] = files of bucket i will be accessed directly.
        for (int i = 0; i < numberOfBuckets; ++i) include[i] = true;
        int totalFileReads = fileListToOptimize.length;
        int numberOfBucketsToBeAccessed = 0;
        int numberOfFilesTakenFromBuckets = 0;
        final int FILE_COUNT_THRESHOLD = Integer.parseInt(System.getProperty("gor.bucket.file.count.threshold", "100"));
        float threshold = Integer.parseInt(System.getProperty("gor.bucket.initial.usage.threshold", "40")) / 100f;
        final int singleFilesBucketCountThresholdRatio = 10;
        final int minNumberOfFilesToAccess = numberOfFilesWithoutBucket + numberOfBuckets; //No matter what, we have to open this many files.

        /*
         * We optimize the file list by replacing each file from a bucket with more than @threshold usage ratio (number of files we are going to use / total number of files in the bucket)
         * If we are still accessing more than @FILE_COUNT_THRESHOLD or @minNumberOfFilesToAccess many files or if the number of single files is more than @singleFilesBucketCountThresholdRatio times
         * the number of buckets we keep on with @threshold = half of what it was.
         */
        int count;
        do {
            for (int i = 0; i < numberOfBuckets; ++i) {
                count = bucketUsedCounts[i];
                if (include[i] && (float) count / bucketTotalFileCounts[i] > threshold && count != 1) {
                    include[i] = false;
                    replace[i] = true;
                    totalFileReads -= (count - 1);
                    numberOfFilesTakenFromBuckets += count;
                    numberOfBucketsToBeAccessed++;
                }
            }
            threshold /= 2;
        }
        while (totalFileReads > FILE_COUNT_THRESHOLD && totalFileReads > minNumberOfFilesToAccess && totalFileReads > (singleFilesBucketCountThresholdRatio + 1) * numberOfBucketsToBeAccessed);

        // At this time we know which buckets to use instead of files, so recreate the files list with that in mind
        final List<DictionaryEntry> filesToUse = new ArrayList<>();
        int cntIndividualFiles = 0;
        String bucket;
        int bucketIdx = -1;
        for (DictionaryEntry entry : fileListToOptimize) {
            bucket = entry.getBucket();
            if (bucket != null && replace[bucketIdx = bucketsToIdx.get(bucket)]) { // All files from this bucket will be replaced with the bucket
                replace[bucketIdx] = false;
                if (log.isTraceEnabled()) {
                    log.trace("Bucket used={}", resetBucketNames[bucketIdx]);
                }
                filesToUse.add(new DictionaryEntry(resetBucketNames[bucketIdx], table.getRootPath(), null, bucketTagsArray[bucketIdx].toArray(String[]::new),
                        null, null, false, true));
                
                } else if (bucket == null || include[bucketIdx]) { // all files from this bucket are to be included as they were
                if (log.isTraceEnabled()) {
                    log.trace("Include {}", entry);
                }
                filesToUse.add(entry);
                cntIndividualFiles++;
            } else { // else we skip the file since it is from a bucket that is going to be included
                if (log.isTraceEnabled()) {
                    log.trace("Skipping {} with from bucket {}", entry, bucket);
                }
            }
        }
        if (log.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numberOfBuckets; ++i) {
                if (replace[i]) {
                    if (i > 0) sb.append(',');
                    sb.append(resetBucketNames[i]).append(" (").append(bucketUsedCounts[i]).append(")");
                }
            }
            log.debug("Individual Files={}, Buckets={} for {} tags { {} }", cntIndividualFiles, numberOfBucketsToBeAccessed, numberOfFilesTakenFromBuckets, sb);

            if (log.isTraceEnabled()) {
                log.trace("Files={} -> {}", filesToUse.size(), filesToUse.stream().map(e -> e.getContent()).collect(Collectors.joining(",")));
            }
        }
        return filesToUse;
    }


}
