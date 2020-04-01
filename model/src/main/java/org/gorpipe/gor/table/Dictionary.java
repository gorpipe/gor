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

package org.gorpipe.gor.table;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.model.util.Util;
import org.gorpipe.util.Pair;
import org.gorpipe.util.collection.IntArray;
import org.gorpipe.util.string.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Class representing GOR Dictionary
 * <p>
 * Created by gisli on 10/03/16.
 * <p>
 * Format of the gord dictionary file
 * <p>
 * [<meta-information lines>]
 * [<header line>]
 * <data lines>
 * <p>
 * The format of the meta-information lines is:
 * <p>
 * [## <key>=<value>]
 * ...
 * <p>
 * <p>
 * <key>       Attribute name.  Not case sensitive.
 * <value>     Attribute value.
 * <p>
 * Reserved key values are:
 * <p>
 * fileformat      - Version of the file format.
 * created         - Creation date of the file
 * build           - Reference data build version.
 * columns         - Column definition for the gor files in the dictionary.
 * sourceColumn    - Name of the source column.
 * <p>
 * Format of the header line is:
 * <p>
 * [# <column name 1>\t<column name 2>\t<column name 3> ...]
 * <p>
 * <p>
 * Each data line is a tab separated list of columns, described as:
 * <p>
 * <file>[[|<flags>]|<bucket>][\t<alias>[\t<startchrom>\t<startpos>\t<endchrom>\t<endpos>\t[tags]]]
 * <p>
 * where:
 * <p>
 * <file>              Abosolute path or relative (to the location of the dictionary) path to the main data file.
 * <flags>             Comma seprated list of flags applicable to the data file.  Available flags:
 * D - The file is marked as deleted.  This means the file has been deleted and should be
 * ignored when reading from the bucket.
 * <bucket>            Relative path ot the bucket file <file> belongs to.
 * <alias>             Alias for <file>.  The alias specifies "source" value of <file>.  If no tags are specified, <alias> is used
 * as tag for <file>.  Can be empty.
 * <startchrom>        Filter start chromosome, e.g. chr1.  Can be empty, if all range elements are empty.
 * <startpos>          Filter start pos. Can be empty, if all range elements are empty.
 * <endchrom>          Filter stop chromosome, e.g. chr3. Can be empty, if all range elements are empty.
 * <endpos>            Filter stop pos.  Can be empty, if all range elements are empty.
 * <tags>              Comma separated list of tags:  <tagval1>[,<tagval2>...]
 * <p>
 * <p>
 * Notes:
 * 1. The <file> + <filter> is a unique key into the file.  Note, this always exact match, i.e. filter that is a subset of another filter
 * will be treated as different.  This could be improved by banning overlapping filters.
 * 2. If <tags> are specified then they are used for filtering (but the <alias> is not).  If no <tags> are specified <alias> is used as
 * tags for filtering.
 * 3. <file> could be bucket file.
 * 4. Seems in general alias is used for normal files but tags for the bucket files.
 * 5. We assume that either all files have alias or none of them.
 */
public class Dictionary {

    private static final Logger log = LoggerFactory.getLogger(Dictionary.class);
    private boolean isDictionaryWithBuckets = false; // source col from dictionary files can be hiden if no buckets and no -f filters
    private final String commonRoot;
    protected DictionaryLine[] files;
    private DictionaryLine fallbackLineForHeader = null;
    private FileReference dictFileParent = null;
    private Set<String> validTags;    //Set containing all not deleted tags in the dictionary.
    private final boolean hasTags;  //Whether the user has specified any tags to filter the dictionary with.
    private Multimap<String, String> bucketHasDeletedFile;
    final static ConcurrentHashMap<String, DictionaryCacheObject> dictCache = new ConcurrentHashMap<>();   //A map from dictionaries to the cache objects.

    /**
     * When a dictionary is read for the first time we make a hash map mapping tags to its corresponding lines.
     * We also store an array of the bucketEntry objects for the dictionary and an hashmap from the buckets names to their
     * corresponding indices.
     */
    private static class DictionaryCacheObject {
        final LinkedHashMap<String, IntArray> tagsToActiveLines;    //A map from tags to its corresponding lines.
        final String fileSignature;  //The file signature of the dictionary. Usually an md5 sum.
        final FileReference fileReference;  //The dictFileParent
        final DictionaryLine[] activeDictionaryLines;  //All dictionaryLines which are not deleted
        final HashMap<String, Integer> mapBucketIndex; //Map from bucketNames to corresponding indices.
        final int[] bucketTotalCount; //bucketCount[i] = number of files in bucket i.
        final int[] bucketActiveCount;
        final String[] bucketResetNames; //bucketResetNames[i] = resetFilePath(...).physical of bucket i.
        final Set<String>[] bucketTags; //bucketTags[i] = set of tags in bucket i.
        final Multimap<String, String> bucketHasDeletedFile;   //Is there any deleted file in a bucket in the dictionary?
        final Set<String> validTags;  //Set of all tags which are not deleted.
        final ConcurrentHashMap<String, Pair<DictionaryLine[], Boolean>> tagsToListCache = new ConcurrentHashMap<>();

        DictionaryCacheObject(String fileSignature, LinkedHashMap<String, IntArray> tagsToActiveLines, FileReference fileReference, DictionaryLine[] activeDictionaryLines,
                              HashMap<String, Integer> mapBucketIndex, int[] bucketTotalCount, int[] bucketActiveCount, String[] bucketResetNames, Set<String>[] bucketTags,
                              Multimap<String, String>  bucketHasDeletedFile, Set<String> validTags) {
            this.fileSignature = fileSignature;
            this.tagsToActiveLines = tagsToActiveLines;
            this.fileReference = fileReference;
            this.activeDictionaryLines = activeDictionaryLines;
            this.mapBucketIndex = mapBucketIndex;
            this.bucketTotalCount = bucketTotalCount;
            this.bucketResetNames = bucketResetNames;
            this.bucketTags = bucketTags;
            this.bucketHasDeletedFile = bucketHasDeletedFile;
            this.validTags = validTags;
            this.bucketActiveCount = bucketActiveCount;
        }
    }

    public Dictionary(String path, boolean allowBucketAccess, Set<String> queryTags,
                      String commonRoot, String uniqueID) {
        this(path, allowBucketAccess, queryTags, commonRoot, uniqueID, false);
    }

    public Dictionary(String path, boolean allowBucketAccess, Set<String> queryTags,
                      String commonRoot, String uniqueID, boolean isSilentTagFilter) {
        this(path, allowBucketAccess, queryTags, commonRoot, uniqueID, queryTags != null && !queryTags.isEmpty(), isSilentTagFilter, true);
    }

    public Dictionary(String path, boolean allowBucketAccess, Set<String> queryTags, String commonRoot,
                      String uniqueID, boolean hasTags, boolean isSilentTagFilter, boolean useDictionaryCache) throws GorDataException {
        this.hasTags = hasTags;
        this.commonRoot = commonRoot;
        if (useDictionaryCache) {
            if (uniqueID == null || uniqueID.equals("")) {
                dictCache.remove(path);
                parseDictionary(path, allowBucketAccess, queryTags, isSilentTagFilter);
            } else {
                final DictionaryCacheObject cache;
                try {
                    cache = dictCache.compute(path, (ppath, cacheObject) -> {
                        if (cacheObject == null || !cacheObject.fileSignature.equals(uniqueID)) {
                            return generateCache(ppath, uniqueID);
                        } else {
                            return cacheObject;
                        }
                    });
                } catch (Exception e) {
                    dictCache.remove(path);
                    throw e;
                }
                this.validTags = cache.validTags;
                this.bucketHasDeletedFile = cache.bucketHasDeletedFile;
                final String orderedTags = orderTags(queryTags);
                final Set<String> badTags = new HashSet<>();
                final Pair<DictionaryLine[], Boolean> fileListAndMore =
                        cache.tagsToListCache.computeIfAbsent(orderedTags,
                                s -> parseFileListFromCache(cache, allowBucketAccess, queryTags, badTags));

                // See if we should throw an error and if we should remove the key from the cache
                boolean hasInvalidTags = false;

                // Removing the entry from cache due to invalid tags should be separate from throwing the data exception
                if (hasInvalidTags(badTags)) {
                    cache.tagsToListCache.remove(orderedTags);
                    hasInvalidTags = true;
                }

                // Here we throw the data exception if the call is not silent
                if (hasInvalidTags && !isSilentTagFilter) {
                    throwBadTagException(path, badTags);
                    return;
                }
                this.files = fileListAndMore.getFormer();
                if (this.files.length == 0 && cache.activeDictionaryLines.length > 0) this.fallbackLineForHeader = cache.activeDictionaryLines[0];
                this.isDictionaryWithBuckets = fileListAndMore.getLatter();
            }
        } else {
            parseDictionary(path, allowBucketAccess, queryTags, isSilentTagFilter);
        }
    }

    private void throwBadTagException(String path, Set<String> badTags) {
        String message = "Invalid Source Filter for dictionary file: " + path + ". ";
        if (badTags.contains("")) {
            message += "Empty tag is not allowed";
        } else {
            message = "Following are not in dictionary " + String.join(",", badTags);
        }
        throw new GorDataException(message);
    }

    public Set<String> getValidTags() {
        return this.validTags;
    }

    public boolean getAnyBucketHasDeletedFile() {
        return !this.bucketHasDeletedFile.isEmpty();
    }

    public Collection<String> getBucketDeletedFiles(String bucket) {
        return this.bucketHasDeletedFile.get(bucket);
    }

    public DictionaryLine[] getFiles() {
        return files;
    }

    public DictionaryLine[] getFallbackLinesForHeader() {
        return new DictionaryLine[]{this.fallbackLineForHeader};
    }

    public boolean isDictionaryWithBuckets() {
        return isDictionaryWithBuckets;
    }

    private FileReference getBucketsPath(final FileReference dictFileParent) {
        final boolean isAbsolute = Util.isAbsoluteFilePath(dictFileParent.physical);
        return commonRoot == null || isAbsolute || (dictFileParent.physical + '/').startsWith(commonRoot)
                ? dictFileParent
                : new FileReference(dictFileParent.logical, commonRoot + dictFileParent.physical);
    }

    private boolean hasInvalidTags(Set<String> badTags) {
        return this.hasTags && !badTags.isEmpty();
    }

    /**
     * Read filename, alias, range and tags from tab delimited text file
     *
     * @return Cache object with all important info about the dictionary.
     */
    private DictionaryCacheObject generateCache(String path, String uniqueId) {
        final ArrayList<Set<String>> bucketTagsList = new ArrayList<>();
        final ArrayList<String> resetBucketNames = new ArrayList<>();
        final IntArray bucketTotalCounts = new IntArray();
        final IntArray bucketActiveCount = new IntArray();
        final HashMap<String, Integer> bucketToIdx = new HashMap<>();
        final Path gordPath = Paths.get(path);
        final FileReference dictFileParent = getDictionaryFileParent(gordPath, commonRoot);
        this.dictFileParent = dictFileParent;
        final FileReference bucketsParent = getBucketsPath(dictFileParent);
        final ArrayList<DictionaryLine> activeDictionaryLines = new ArrayList<>();
        final LinkedHashMap<String, IntArray> tagsToLines = new LinkedHashMap<>();
        final Set<String> validTags = new HashSet<>();
        final Multimap<String, String> bucketHasDeletedFile = ArrayListMultimap.create(); //This is changed if we find a deleted line with bucket.
        if(Files.exists(gordPath)) {
            try (final Stream<String> stream = Files.newBufferedReader(gordPath).lines()) {
                stream.map(String::trim)
                        .filter(line -> !(line.isEmpty() || line.charAt(0) == '#'))
                        .map(line -> parseDictionaryLine(line, this.dictFileParent))
                        .filter(Objects::nonNull)
                        .forEach(dictLine ->
                                processLineForCache(bucketTagsList, resetBucketNames, bucketTotalCounts, bucketActiveCount, bucketToIdx, bucketsParent, activeDictionaryLines, tagsToLines, validTags, bucketHasDeletedFile, dictLine)
                        );
            } catch (IOException ex) {
                throw new GorResourceException("Error Initializing Query. Can not open file " + path, path, ex);
            }
        }
        return new DictionaryCacheObject(uniqueId, tagsToLines, this.dictFileParent, activeDictionaryLines.toArray(new DictionaryLine[0]),
                bucketToIdx, bucketTotalCounts.toArray(), bucketActiveCount.toArray(), resetBucketNames.toArray(new String[0]),
                bucketTagsList.toArray(new Set[0]), bucketHasDeletedFile, validTags);
    }

    private void processLineForCache(ArrayList<Set<String>> bucketTagsList, ArrayList<String> resetBucketNames, IntArray bucketTotalCounts, IntArray bucketActiveCount, HashMap<String, Integer> bucketToIdx, FileReference bucketsParent, ArrayList<DictionaryLine> activeDictionaryLines, LinkedHashMap<String, IntArray> tagsToLines, Set<String> validTags, Multimap<String, String> bucketHasDeletedFile, DictionaryLine dictLine) {
        if (dictLine.bucket != null) {
            final int bucketIdx = bucketToIdx.computeIfAbsent(dictLine.bucket, bucket -> {
                resetBucketNames.add(resetFilePath(bucket, bucket.contains("://") ? new FileReference("") : bucketsParent).physical);
                bucketTagsList.add(new HashSet<>());
                bucketActiveCount.add(0);
                bucketTotalCounts.add(0);
                return bucketToIdx.size();
            });
            bucketTotalCounts.increment(bucketIdx);
            if (dictLine.isDeleted) {
                bucketHasDeletedFile.put(Paths.get(dictLine.bucket).getFileName().toString(), dictLine.alias);
                bucketTagsList.get(bucketIdx).add(dictLine.alias);
            } else {
                bucketActiveCount.increment(bucketIdx);
                bucketTagsList.get(bucketIdx).addAll(dictLine.tags);
            }
        }
        if (!dictLine.isDeleted) {
            dictLine.tags.forEach(tag -> {
                // Have seen the same tag before? If not, put it and an empty array to the tags-to-lines map.
                tagsToLines.computeIfAbsent(tag, someVariableName -> new IntArray());
                tagsToLines.get(tag).add(activeDictionaryLines.size());    //Put this line to the array with the lines which this tag corresponds to.
                validTags.add(tag);
            });
            activeDictionaryLines.add(dictLine);
        }
    }

    private void parseDictionary(String path, boolean allowBucketAccess, Set<String> queryTags, boolean isSilentTagFilter) {
        this.validTags = new HashSet<>();
        final IntArray fileListToBeOptimized = new IntArray();
        int numberOfLinesWithoutBuckets = 0;
        final ArrayList<String> resetBucketNames = new ArrayList<>();
        final IntArray bucketTotalCounts = new IntArray();
        final IntArray bucketUsedCounts = new IntArray();
        final HashMap<String, Integer> bucketToIdx = new HashMap<>();
        final HashSet<String> badTags = this.hasTags && !isSilentTagFilter ? new HashSet<>(queryTags) : null;
        final Path gordPath = Paths.get(path);
        final FileReference dictFileParent = getDictionaryFileParent(gordPath, commonRoot);
        this.dictFileParent = dictFileParent;
        final FileReference bucketsParent = getBucketsPath(dictFileParent);
        final ArrayList<DictionaryLine> activeDictionaryLines = new ArrayList<>();
        this.bucketHasDeletedFile = ArrayListMultimap.create();
        final ArrayList<Set<String>> bucketTagsList = new ArrayList<>();
        try (final Stream<String> lines = new BufferedReader(new FileReader(gordPath.toFile())).lines()) {
            numberOfLinesWithoutBuckets = (int) lines.map(String::trim)
                    .filter(line -> !(line.isEmpty() || line.charAt(0) == '#')).map(line -> parseDictionaryLine(line, dictFileParent))
                    .peek(dictLine -> {
                        if (dictLine != null) {
                            int bucketIdx = -1;
                            if (dictLine.bucket != null) {
                                bucketIdx = bucketToIdx.computeIfAbsent(dictLine.bucket, bucket -> {
                                    resetBucketNames.add(resetFilePath(bucket, bucket.contains("://") ? new FileReference("") : bucketsParent).physical);
                                    bucketTagsList.add(new HashSet<>());
                                    bucketTotalCounts.add(0);
                                    bucketUsedCounts.add(0);
                                    return bucketToIdx.size();
                                });
                                bucketTotalCounts.increment(bucketIdx);
                                if (dictLine.isDeleted) {
                                    bucketHasDeletedFile.put(Paths.get(dictLine.bucket).getFileName().toString(), dictLine.alias);
                                    bucketTagsList.get(bucketIdx).add(dictLine.alias);
                                } else {
                                    bucketTagsList.get(bucketIdx).addAll(dictLine.tags);
                                }
                            }
                            if (!dictLine.isDeleted) {
                                // Optimization, reduce the maximum number of files needed to be opened by applying tag filter ASAP, if tag filter is used.
                                // For Gor Server this allows more accurate number of files to be opened by a query
                                if (dictLine.alias == null || !this.hasTags || TableAccessOptimizer.match(dictLine.tags, queryTags, dictLine.alias)) { // Do not include files that will not be used
                                    if (dictLine.bucket != null) bucketUsedCounts.increment(bucketIdx);
                                    fileListToBeOptimized.add(activeDictionaryLines.size());
                                    activeDictionaryLines.add(dictLine);
                                } else if (this.fallbackLineForHeader == null) {
                                    this.fallbackLineForHeader = dictLine;
                                }
                                if (this.hasTags && !badTags.isEmpty() && !dictLine.tags.isEmpty()) {
                                    badTags.removeAll(dictLine.tags);
                                }
                                this.validTags.addAll(dictLine.tags);
                            }
                        }
                    }).filter(dictLine -> !dictLine.isDeleted && dictLine.bucket == null).count();
        } catch (IOException ex) {
            throw new GorResourceException("Error Initializing Query. Can not open file " + path, path, ex);
        }

        if (hasInvalidTags(badTags) && !isSilentTagFilter) {
            throwBadTagException(path, badTags);
        }

        final int[] bucketTotalCountArray = bucketTotalCounts.toArray();
        final String[] resetBucketNamesArray = resetBucketNames.toArray(new String[0]);
        final DictionaryLine[] activeDictionaryLinesArray = activeDictionaryLines.toArray(new DictionaryLine[0]);
        if (allowBucketAccess && bucketTotalCounts.size() != 0) {
            this.files = getOptimizedFileList(bucketTotalCountArray, bucketUsedCounts.toArray(), bucketTagsList.toArray(new Set[0]), fileListToBeOptimized.toArray(), numberOfLinesWithoutBuckets,
                    activeDictionaryLinesArray, bucketToIdx, resetBucketNamesArray);
        } else {
            this.files = new DictionaryLine[fileListToBeOptimized.size()];
            for (int i = 0; i < this.files.length; ++i) {
                this.files[i] = activeDictionaryLinesArray[fileListToBeOptimized.get(i)];
            }
        }
        if (bucketTotalCounts.size() > 0) {
            this.isDictionaryWithBuckets = true;
        }
    }

    private Pair<DictionaryLine[], Boolean> parseFileListFromCache(DictionaryCacheObject cache, boolean allowBucketAccess, Set<String> queryTags, Set<String> badTags) {
        final int[] filesToOptimize;
        this.bucketHasDeletedFile = cache.bucketHasDeletedFile;
        final IntArray bucketUsedCounts = new IntArray();
        final IntArray bucketTotalCount = new IntArray();
        final ArrayList<String> resetBucketNames = new ArrayList<>();
        final HashMap<String, Integer> newBucketToIdx;
        final int[] bucketUsedCountsArray;
        final int[] bucketTotalCountArray;
        final String[] resetBucketNamesArray;
        final Set<String>[] bucketTagsArray;
        int numberOfFilesWithoutBucket = 0;
        final ArrayList<Set<String>> bucketTagsList = new ArrayList<>();

        if (this.hasTags) {
            Set<Integer> filesToOptimizeTmp = new LinkedHashSet<>();
            newBucketToIdx = new HashMap<>();
            for (String tag : queryTags) {
                if (cache.validTags.contains(tag)) {
                    for (int i : cache.tagsToActiveLines.get(tag)) {
                        filesToOptimizeTmp.add(i);
                        final String bucket = cache.activeDictionaryLines[i].bucket;
                        if (bucket == null) numberOfFilesWithoutBucket++;
                        else {
                            final int bucketIdx = newBucketToIdx.computeIfAbsent(bucket, bucketbucket -> {
                                bucketUsedCounts.add(0);
                                final int idx = cache.mapBucketIndex.get(bucketbucket);
                                bucketTotalCount.add(cache.bucketTotalCount[idx]);
                                resetBucketNames.add(cache.bucketResetNames[idx]);
                                bucketTagsList.add(cache.bucketTags[idx]);
                                return newBucketToIdx.size();
                            });
                            bucketUsedCounts.increment(bucketIdx);
                        }
                    }
                } else badTags.add(tag);
            }
            filesToOptimize = new int[filesToOptimizeTmp.size()];
            int idx = 0;
            for (int i : filesToOptimizeTmp) filesToOptimize[idx++] = i;
            bucketUsedCountsArray = bucketUsedCounts.toArray();
            bucketTotalCountArray = bucketTotalCount.toArray();
            resetBucketNamesArray = resetBucketNames.toArray(new String[0]);
            bucketTagsArray = bucketTagsList.toArray(new Set[0]);
        } else {
            filesToOptimize = new int[cache.activeDictionaryLines.length];
            for (int i = 0; i < filesToOptimize.length; i++) {
                if (cache.activeDictionaryLines[i].bucket == null) numberOfFilesWithoutBucket++;
                filesToOptimize[i] = i;
            }
            bucketUsedCountsArray = cache.bucketActiveCount;
            bucketTotalCountArray = cache.bucketTotalCount;
            resetBucketNamesArray = cache.bucketResetNames;
            newBucketToIdx = cache.mapBucketIndex;
            bucketTagsArray = cache.bucketTags;
        }
        this.isDictionaryWithBuckets = bucketUsedCountsArray.length != 0;
        final DictionaryLine[] fileList;
        if (allowBucketAccess && cache.bucketTotalCount.length != 0) {
            fileList = getOptimizedFileList(bucketTotalCountArray, bucketUsedCountsArray, bucketTagsArray, filesToOptimize, numberOfFilesWithoutBucket,
                    cache.activeDictionaryLines, newBucketToIdx, resetBucketNamesArray);
        } else {
            fileList = new DictionaryLine[filesToOptimize.length];
            for (int i = 0; i < filesToOptimize.length; ++i) {
                fileList[i] = cache.activeDictionaryLines[filesToOptimize[i]];
            }
        }
        return new Pair<>(fileList, this.isDictionaryWithBuckets);
    }

    private DictionaryLine[] getOptimizedFileList(int[] bucketTotalFileCounts, int[] bucketUsedCounts, Set<String>[] bucketTagsArray, int[] fileListToOptimize, int numberOfFilesWithoutBucket,
                                                  DictionaryLine[] activeDictionaryLines, HashMap<String, Integer> bucketsToIdx, String[] resetBucketNames) {
        final int numberOfBuckets = bucketUsedCounts.length;
        final boolean[] replace = new boolean[numberOfBuckets]; //replace[i] = bucket i will be used
        final boolean[] include = new boolean[numberOfBuckets]; //include[i] = files of bucket i will be accessed directly.
        for (int i = 0; i < numberOfBuckets; ++i) include[i] = true;
        int totalFileReads = fileListToOptimize.length;
        int numberOfBucketsToBeAccessed = 0;
        int numberOfFilesTakenFromBuckets = 0;
        final int FILE_COUNT_THRESHOLD = Integer.parseInt(System.getProperty("gor.bucket.file.count.threshold", "300"));
        float threshold = Integer.parseInt(System.getProperty("gor.bucket.initial.usage.threshold", "80")) / 100f;
        final int singleFilesBucketCountThresholdRatio = 10;
        final int minNumberOfFilesToAccess = numberOfFilesWithoutBucket + numberOfBuckets; //No matter what, we have to open this many files.

        /**
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
        final DictionaryLine[] filesToUse = new DictionaryLine[totalFileReads];
        int filesToUseIdx = 0;
        int cntIndividualFiles = 0;
        String bucket;
        int bucketIdx = -1;
        for (int i = 0; i < fileListToOptimize.length; ++i) {
            bucket = activeDictionaryLines[fileListToOptimize[i]].bucket;
            if (bucket != null && replace[bucketIdx = bucketsToIdx.get(bucket)]) { // All files from this bucket will be replaced with the bucket
                replace[bucketIdx] = false;
                if (log.isTraceEnabled()) {
                    log.trace("Bucket used={}", resetBucketNames[bucketIdx]);
                }
                filesToUse[filesToUseIdx++] = new DictionaryLine(new FileReference(resetBucketNames[bucketIdx]), null, null, null, -1, null, -1, bucketTagsArray[bucketIdx], true, false);
            } else if (bucket == null || include[bucketIdx]) { // all files from this bucket are to be included as they were
                if (log.isTraceEnabled()) {
                    log.trace("Include {}", activeDictionaryLines[fileListToOptimize[i]]);
                }
                filesToUse[filesToUseIdx++] = activeDictionaryLines[fileListToOptimize[i]];
                cntIndividualFiles++;
            } else { // else we skip the file since it is from a bucket that is going to be included
                if (log.isTraceEnabled()) {
                    log.trace("Skipping {} with idx {} from bucket {}", activeDictionaryLines[fileListToOptimize[i]], fileListToOptimize[i], bucket);
                }
            }
        }
        if (log.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numberOfBuckets; ++i) {
                if (replace[i]) {
                    if (i > 0) sb.append(',');
                    sb.append(resetBucketNames[i] + " (" + bucketUsedCounts[i] + ")");
                }
            }
            log.debug("Individual Files={}, Buckets={} for %d tags { {} }", cntIndividualFiles, numberOfBucketsToBeAccessed, numberOfFilesTakenFromBuckets, sb);

            if (log.isTraceEnabled()) {
                log.trace("Files={} -> {}", filesToUse.length, Arrays.toString(filesToUse));
            }
        }
        return filesToUse;
    }

    static public DictionaryLine parseDictionaryLine(String line, FileReference dictFileParent) {
        final ArrayList<String> parts = StringUtil.split(line);
        final int length = parts.size();
        if (length > 0) {
            String file = parts.get(0).replace('\\', '/');
            String bucketFileName = null;
            final int pipeIdx = file.indexOf('|');
            final String alias = length > 1 ? parts.get(1) : null;
            boolean lineDeleted = false;
            if (pipeIdx >= 0) { // If file bucket is encoded in the file name
                if (pipeIdx == 0) {
                    throw new GorDataException("Error Intializing Query. File " + file + " starts with pipe character");
                }
                final String bucket = file.substring(pipeIdx + 1);
                if (bucket.toLowerCase().startsWith("d|")) {
                    bucketFileName = bucket.substring(2);
                    log.debug("Ignoring deleted file: {}", bucketFileName);
                    lineDeleted = true;
                } else {
                    file = file.substring(0, pipeIdx);
                    bucketFileName = bucket;
                }
            }

            if (!lineDeleted) {
                FileReference fileref = file.contains("://") ? resetFilePath(file, null) : resetFilePath(file, dictFileParent);
                final Set<String> tags;
                if (length > 2) {
                    if (length >= 6) {
                        // Support specification of the genomic range of each file
                        final String startChr = parts.get(2);
                        final int startPos = Integer.parseInt(parts.get(3));
                        final String stopChr = parts.get(4);
                        final int stopPos = Integer.parseInt(parts.get(5));
                        // support both comma separated and tab separated tags
                        tags = length < 7 ? tagset(alias)
                                : tagset(parts.get(6).indexOf(',') >= 0 ? StringUtil.split(parts.get(6), ',') : parts.subList(6, parts.size()));

                        return new DictionaryLine(fileref, bucketFileName, alias, startChr, startPos, stopChr, stopPos, tags, false, false);
                    } else {
                        throw new GorDataException("Error Initializing Query. Expected 4 columns for genomic range specification!");
                    }
                } else {
                    // Add alias as tag on the file, if alias is specified
                    tags = tagset(alias);
                    return new DictionaryLine(fileref, bucketFileName, alias, null, -1, null, -1, tags, false, false);
                }
            } else {
                return new DictionaryLine(null, bucketFileName, alias, null, -1, null, -1, null, false, true);
            }
        }
        return null;
    }

    private static Set<String> tagset(String alias) {
        final HashSet<String> tags = new HashSet<>();
        if (alias != null) {
            tags.add(alias);
        }
        return tags;
    }

    private static Set<String> tagset(List<String> t) {
        if (t != null && !t.isEmpty()) {
            return new HashSet<>(t);
        }
        return null;
    }

    private static boolean isHttpRef(String path) {
        final String p = path.toLowerCase();
        return p.startsWith("http://") || p.startsWith("https://");
    }

    private static boolean isTcpRef(String path) {
        final String p = path.toLowerCase();
        return p.startsWith("tcp://");
    }

    public static class FileReference {
        public final String logical;
        public final String physical;
        public final boolean isAcceptedAbsoluteRef;

        FileReference(String identical) {
            this(identical, identical);
        }

        FileReference(String logical, String physical) {
            this(logical, physical, false);
        }

        FileReference(String logical, String physical, boolean isAcceptedAbsoluteRef) {
            this.logical = Dictionary.removeDotDots(logical);
            this.physical = Dictionary.removeDotDots(physical);
            this.isAcceptedAbsoluteRef = isAcceptedAbsoluteRef;
        }
    }

    public static class DictionaryLine {
        public final FileReference fileRef;
        /**
         * The bucket file if this line has bucket, otherwise nul1.
         */
        public final String bucket;
        /**
         * The alias to use for the file
         */
        public final String alias;
        /**
         * The chromosome number of the first data line in the file, or -1 if this is not specified.
         */
        public final String startChr;
        /**
         * The position with in the chromosome of the first data line in the file, or -1 if this is not specified.
         */
        public final int startPos;
        /**
         * The chromosome number of the last data line in the file, or -1 if this is not specified.
         */
        public final String stopChr;
        /**
         * The position with in the chromosome of the last data line in the file, or -1 if this is not specified.
         */
        public final int stopPos;
        /**
         * Set containing all the column tags.
         */
        public final Set<String> tags;
        /**
         * Is source column is already inserted, set for bucketfiles.  Passed on to .
         */
        public final boolean sourceInserted;
        /**
         * Is the source marked as deleted
         */
        final boolean isDeleted;

        DictionaryLine(FileReference fileRef, String bucket, String alias, String startChr, int startPos, String stopChr, int stopPos, Set<String> tags, boolean sourceInserted, boolean isDeleted) {
            this.fileRef = fileRef;
            this.bucket = bucket;
            this.alias = alias;
            this.startChr = startChr;
            this.startPos = startPos;
            this.stopChr = stopChr;
            this.stopPos = stopPos;
            this.tags = tags;
            this.isDeleted = isDeleted;
            this.sourceInserted = sourceInserted;
        }

        @Override
        public String toString() {
            return fileRef.physical + " " + bucket + " " + alias + " " + startChr + " " + startPos + " " + tags.size();
        }

        public String toPreciseString() {
            final String[] tagsArray = tags.toArray(new String[tags.size()]);
            Arrays.sort(tagsArray);
            return fileRef.physical + " " + bucket + " " + alias + " " + startChr + " " + startPos + " " + Arrays.toString(tagsArray);
        }
    }

    private static FileReference resetFilePath(String file, final FileReference dictFileParent) {
        if (file != null) {
            if (file.startsWith("/")) {
                if (!file.startsWith("//")) { // Ensure unix based path will work in linux
                    return new FileReference('/' + file);
                }
            } else if (dictFileParent != null && dictFileParent.physical != null && dictFileParent.physical.length() > 0) { // assume relative files in gord file are relative to that gord file
                if (file.length() >= 2 && file.charAt(1) != ':') {
                    // Filename doesn't start with slash or backslash and second letter is not double colon, so assume this is a relative path
                    return new FileReference(dictFileParent.logical + '/' + file, dictFileParent.physical + '/' + file, dictFileParent.isAcceptedAbsoluteRef);
                }
            }
        }
        return new FileReference(file);
    }

    static String removeDotDots(String ipath) {
        if (ipath == null || !ipath.contains("../")) {
            return ipath;
        }
        // path contains ../, try squashing the path, in effect removing dot dots which will
        // allow better handling of symlinks in parent folders
        final String path = ipath.replace('\\', '/');
        final boolean startsWithSlash = ipath.charAt(0) == '/';
        final ArrayList<String> parts = StringUtil.split(path, '/');
        final String[] outParts = new String[parts.size()];
        int last = 0;
        for (String part : parts) {
            if (part.equals(".")) {
                /* do nothing */
            } else if (part.equals("..")) {
                last -= 1;
                if (last < 0) { // dot dots have traversed above the root, not allowed
                    log.info("Path not constraint within root: {}", ipath);
                    return ipath;
                }
            } else {
                outParts[last] = part;
                last += 1;
            }
        }

        // Path is constrained within the root, lets remove all unneeded x/y/../.. - will allow better handling of symlinks in parent folders
        StringBuilder sb = new StringBuilder(ipath.length());
        for (int i = 0; i < last; i++) {
            if (outParts[i].length() != 0) {
                if (sb.length() > 0 || startsWithSlash) {
                    sb.append('/');
                }
                sb.append(outParts[i]);
            }
        }
        return sb.toString();
    }

    public static FileReference getDictionaryFileParent(Path logicalPath, String commonRoot) {
        // Need to prepare two parent paths, one is logical (use by server for security constraints) and another is physical (used to read the actual file)
        Path physicalPath = logicalPath;
        Path pathFromRoot = Paths.get("");
        if (commonRoot != null) {
            String p = logicalPath.toString();
            if (p.startsWith(commonRoot)) {
                final int idx = p.lastIndexOf('/');
                if (idx > 0) {
                    pathFromRoot = Paths.get(p.substring(commonRoot.length(), idx + 1));
                }
            }
        }
        File physicalFile = null;
        boolean symlinkIsAbsolute = false;
        while (Files.isSymbolicLink(physicalPath)) { // When dealing with symbolic links to dictionary files, use the path of the link target as parent
            try {
                Path l = Files.readSymbolicLink(physicalPath);
                if (!symlinkIsAbsolute && Util.isAbsoluteFilePath(l.toString())) {
                    // TODO GM: Can we refer isAcceptedAbsoluteRef (assigned from symlinkIsAbsolute) directly from resolved physicalFile
                    symlinkIsAbsolute = true;
                } else {
                    l = Paths.get(physicalPath.getParent().toString(), l.toString());
                }
                physicalPath = symlinkIsAbsolute ? l : Paths.get(removeDotDots(pathFromRoot.resolve(l).toString()));
                physicalFile = physicalPath.toFile();
            } catch (IOException e) {
                throw new GorSystemException("Error reading dictionary link. Can't read symbolic link", e);
            }
        }

        // In case of different physical path from the logical provided by the user, keep track of two file paths
        final String lpath = findDictPathParent(Files.exists(logicalPath) ? logicalPath.getParent() : null, commonRoot);
        final String ppath = physicalFile != null ? (physicalFile.exists() ? findDictPathParent(physicalPath.getParent(), commonRoot) : Util.nvlToString(physicalPath.getParent(), "")) : lpath;
        log.trace("logicalFile = {}, root = {}, lpath = {}, ppath = {}\n", logicalPath.toAbsolutePath(), commonRoot, lpath, ppath);
        return new FileReference(lpath, ppath, symlinkIsAbsolute);
    }

    /**
     * @param parentpath absolute path or relative path to the dictionary.
     * @param commonRoot
     * @return if commonRoot and commonroot is http or tcp then null
     * else if commonRoot and parentpath starts with the commonRoot then relative path from the common root.
     * else  parentpath as absolute path.
     */
    private static String findDictPathParent(final Path parentpath, String commonRoot) {
        if (parentpath != null) { // 25.01.2012 gfg+hakon, treat relative files in dictionary files always as relative to said dictionary
            // TODO:  Check this, if parentpaht is relative it is resolved from the current dir, but not the relative to the dictionary.gh
            String dictFileParent = parentpath.toAbsolutePath().toString();
            if (commonRoot != null) {
                if (isHttpRef(commonRoot) || isTcpRef(commonRoot)) {
                    return null; // Do no support relative references of local dictionary files with remote reference in common root
                } else {
                    if (dictFileParent.startsWith(commonRoot)) {
                        dictFileParent = dictFileParent.substring(commonRoot.length());
                        if (dictFileParent.length() > 0 && (dictFileParent.charAt(0) == '/' || dictFileParent.charAt(0) == '\\')) { // ensure there is no starting nor trailing slash
                            dictFileParent = dictFileParent.substring(1);
                        }
                        if (dictFileParent.length() > 0 && (dictFileParent.charAt(dictFileParent.length() - 1) == '/' || dictFileParent.charAt(dictFileParent.length() - 1) == '\\')) {
                            dictFileParent = dictFileParent.substring(0, dictFileParent.length() - 1);
                        }
                    }
                }
            }
            return dictFileParent;
        }
        return null;
    }

    private static String orderTags(Collection<String> strings) {
        if (strings == null) return "";
        final String[] stringsAsArray = strings.toArray(new String[0]);
        Arrays.sort(stringsAsArray);
        return String.join(",", stringsAsArray);
    }
}