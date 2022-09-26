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

package org.gorpipe.gor.manager;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.parquet.Strings;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.table.dictionary.BaseDictionaryTable;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.lock.ExclusiveFileTableLock;
import org.gorpipe.gor.table.lock.TableLock;
import org.gorpipe.gor.table.lock.TableTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BucketManager<T extends DictionaryEntry> {

    private static final Logger log = LoggerFactory.getLogger(BucketManager.class);

    // Constants
    public enum BucketPackLevel {
        NO_PACKING,     // No packing.
        CONSOLIDATE,    // Consolidate small buckets into larger one (as needed).
        FULL_PACKING    // Full packing. Rebucket all small buckets and all buckets with deleted files.
    }

    public static final int DEFAULT_MIN_BUCKET_SIZE = 20;
    public static final int DEFAULT_BUCKET_SIZE = 100;
    public static final int DEFAULT_MAX_BUCKET_COUNT = 3;
    public static final BucketPackLevel DEFAULT_BUCKET_PACK_LEVEL = BucketPackLevel.CONSOLIDATE;
    static final String BUCKET_FILE_PREFIX = "bucket"; // Use to identify which files are bucket files.

    public static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofMinutes(30);
    public static final Class<? extends TableLock> DEFAULT_LOCK_TYPE = ExclusiveFileTableLock.class;

    public static final String HEADER_MIN_BUCKET_SIZE_KEY = "MIN_BUCKET_SIZE";
    public static final String HEADER_BUCKET_SIZE_KEY = "BUCKET_SIZE";

    // Comma seperated list of bucket dirs.  Absolute or relative to the table dir.
    public static final String HEADER_BUCKET_DIRS_KEY = "BUCKET_DIRS";

    // Bucket dir root.  Used to find the absolute bucket path if bucket dir is relative.  The path
    // is <bucket location>/<project relative path to bucket>/<bucket dir>
    public static final String HEADER_BUCKET_DIRS_LOCATION_KEY = "BUCKET_DIRS_LOCATION";
    public static final String HEADER_BUCKET_MAX_BUCKETS = "BUCKET_MAX_BUCKETS";

    protected Duration gracePeriodForDeletingBuckets = Duration.ofHours(24);

    // Location of the bucket files we create (absolute or relative to rootPath).
    private final List<String> bucketDirs = new ArrayList<>();
    private Map<String, Long> bucketDirCount;  // Number of files per bucket

    private Class<? extends TableLock> lockType = DEFAULT_LOCK_TYPE;
    private Duration lockTimeout = DEFAULT_LOCK_TIMEOUT;

    private final BaseDictionaryTable<T> table;

    private int bucketSize;
    private int minBucketSize;

    private BucketCreator<T> bucketCreator;

    /**
     * Default constructor.
     */
    public BucketManager(BaseDictionaryTable<T> table) {
        this.table = table;
        this.bucketCreator = new BucketCreatorGorPipe<>();

        setBucketSize(Integer.parseInt(table.getConfigTableProperty(HEADER_BUCKET_SIZE_KEY, Integer.toString(DEFAULT_BUCKET_SIZE))));
        setMinBucketSize(Integer.parseInt(table.getConfigTableProperty(HEADER_MIN_BUCKET_SIZE_KEY, Integer.toString(DEFAULT_MIN_BUCKET_SIZE))));

        setBucketDirs(parseBucketDirString(table.getConfigTableProperty(HEADER_BUCKET_DIRS_KEY, null)));
    }

    private BucketManager(Builder builder) {
        this(builder.table);
        this.lockType = builder.lockType != null ? builder.lockType : DEFAULT_LOCK_TYPE;
        this.lockTimeout = builder.lockTimeout != null ? builder.lockTimeout : DEFAULT_LOCK_TIMEOUT;

        // Must set bucket size first as it is the main size value..
        if (builder.bucketSize > 0) {
            setBucketSize(builder.bucketSize);
        }
        if (builder.minBucketSize > 0) {
            setMinBucketSize(builder.minBucketSize);
        }
        if (builder.bucketCreator != null) {
            this.bucketCreator = builder.bucketCreator;
        }
    }

    public static Builder newBuilder(BaseDictionaryTable table) {
        return new Builder(table);
    }

    public void bucketize() {
        bucketize(DEFAULT_BUCKET_PACK_LEVEL, -1, null, false);
    }

    public int bucketize(BucketPackLevel packLevel, int maxBucketCount) {
        return bucketize(packLevel, maxBucketCount, null, false);
    }

    /**
     * Bucketize the given table.
     *
     * @param packLevel      pack level to use (see BucketPackLevel).
     * @param maxBucketCount Maximum number of buckets to generate on this call, 0 or less means not set in which
     *                       case we read it from the table, if its not set on the table we use default.
     * @param bucketDirs     array of directories to bucketize to, ignored if null.  The dirs are absolute
     *                       or relative to the table dir.
     * @param forceClean     Should we force clean bucket files (if force clean we ignoring grace periods).
     * @return buckets created.
     */
    public int bucketize(BucketPackLevel packLevel, int maxBucketCount, List<String> bucketDirs, boolean forceClean) {
        if (!table.isBucketize()) {
            log.info("Bucketize - Bucketize called on {} but as the table is marked not bucketize so nothing was done.",
                    table.getPath());
            return 0;
        }

        if (bucketDirs != null && !bucketDirs.isEmpty()) {
            setBucketDirs(bucketDirs);
        }
        try (TableLock bucketizeLock = TableLock.acquireWrite(this.lockType, table, "bucketize", Duration.ofMillis(1000))) {
            if (!bucketizeLock.isValid()) {
                long millisRunning = System.currentTimeMillis() - bucketizeLock.lastModified();
                log.debug("Bucketize - Bucketization already in progress on {} (has been running for {} seconds)",
                        table.getName(), millisRunning / 1000);
                return 0;
            }

            if (maxBucketCount <= 0) {
                maxBucketCount = Integer.parseInt(table.getConfigTableProperty(HEADER_BUCKET_MAX_BUCKETS, Integer.toString(DEFAULT_MAX_BUCKET_COUNT)));
                maxBucketCount = maxBucketCount > 0 ? maxBucketCount : 10000;
            }

            cleanTempBucketData(bucketizeLock);
            cleanOldBucketFiles(bucketizeLock, forceClean);

            return doBucketize(bucketizeLock, packLevel, maxBucketCount);
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
    }

    /**
     * Delete the given buckets.
     *
     * @param buckets   list of buckets to be deleted.
     */
    public void deleteBuckets(String... buckets) {
        deleteBuckets(false, buckets);
    }
    
    /**
     * Delete the given buckets.
     *
     * @param force         should grace period be ignored.
     * @param buckets   list of buckets to be deleted.
     */
    public void deleteBuckets(boolean force, String... buckets) {
        try (TableTransaction trans = TableTransaction.openWriteTransaction(this.lockType, table, table.getName(), this.lockTimeout)) {
            deleteBuckets(trans.getLock(), force, buckets);
            trans.commit();
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
    }

    /**
     * Get effective min bucket size.
     *
     * @return effective min bucket size.
     */
    protected int getEffectiveMinBucketSize() {
        return Math.min(this.getMinBucketSize(), this.getBucketSize());
    }

    public int getBucketSize() {
        return this.bucketSize;
    }

    public void setBucketSize(int bucketSize) {
        this.bucketSize = bucketSize;
    }

    public int getMinBucketSize() {
        return this.minBucketSize;
    }

    public void setMinBucketSize(int minBucketSize) {
        this.minBucketSize = minBucketSize;
    }

    protected String getDefaultBucketDir() {
        return "." + table.getName() + "/buckets";
    }

    public Duration getLockTimeout() {
        return lockTimeout;
    }

    /**
     * Get bucket dirs.
     *
     * @return the list of bucketDirs for this table (relative to root or absolute).
     */
    private List<String> getBucketDirs() {
        return bucketDirs;
    }

    /**
     * Set the bucket dir list.
     *
     * @param newBucketDirs the new list of bucket dirs, absolute or relative to table dir.
     */
    public void setBucketDirs(List<String> newBucketDirs) {
        this.bucketDirs.clear();
        if (newBucketDirs != null && newBucketDirs.size() > 0) {
            table.setProperty(HEADER_BUCKET_DIRS_KEY, newBucketDirs.stream().map(p -> PathUtils.formatUri(p)).collect(Collectors.joining(",")));
            
            for (String p : newBucketDirs) {
                this.bucketDirs.add(PathUtils.relativize(table.getRootUri(), p));
            }
        } else {
            table.setProperty(HEADER_BUCKET_DIRS_KEY, "");
            this.bucketDirs.add(getDefaultBucketDir());
        }
    }

    private List<String> parseBucketDirString(String bucketDirs) {
        if (bucketDirs == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(bucketDirs.split(","))
                .filter(s -> !s.trim().isEmpty())
//                .map(s -> PathUtils.toURIFolder(s))
                .collect(Collectors.toList());
    }

    private void checkBucketDirExistance(URI bucketDir) {
        // Create the default bucket dir (if it is to be used and is missing)
        if (!table.getFileReader().exists(bucketDir.toString())) {
            URI fullPathDefaultBucketDir = table.getRootUri().resolve(getDefaultBucketDir().toString());
            if (bucketDir.equals(fullPathDefaultBucketDir)) {
                try {
                    table.getFileReader().createDirectoryIfNotExists(fullPathDefaultBucketDir.toString());
                } catch (IOException e) {
                    throw new GorSystemException(String.format("Could not create bucket directory %s", fullPathDefaultBucketDir), e);
                }
            } else {
                throw new GorSystemException(String.format("Bucket dirs must exists, directory %s is not found!", bucketDir), null);
            }
        }
    }

    /**
     * Pick a bucket dir from a list.
     * There couple of strategies to choose from (using the gor.table.buckets.directory.strategy option.)
     * 'random':  pick a directory by random.
     * 'least_used':  pick the directory that is least used by the dictionary.  Default.
     *
     * @return path to the chosen bucket dir, relative to path or absolute.
     */
    protected final String pickBucketDir() {
        if (bucketDirs.size() == 0) {
            throw new GorSystemException("Can not pick bucket, the list of bucket dirs is empty!", null);
        }
        String strategy = table.getConfigTableProperty("gor.table.buckets.directory.strategy", "least_used");
        if ("random".equals(strategy)) {
            // Use randomly chosen directory from the given list of bucket dirs or the default bucket dir if no bucket
            // dirs are specified.
            return bucketDirs.get(new Random().nextInt(bucketDirs.size()));
        } else {
            // 'least_used' :  Use the least used folder.
            // Note:  We are not worried if another process changing the dictionary so these numbers are off.
            //        That could happen if two processes take turns calling bucketize (without reloading the dictinary),
            //        but that problem would rectify itself on future calls to bucketize.
            if (bucketDirCount == null) {
                bucketDirCount = table.getEntries().stream()
                        .filter(l -> l.getBucket() != null)
                        .map(l -> PathUtils.parent(l.getBucket()))
                        .filter(p -> bucketDirs.contains(p))
                        .collect(Collectors.groupingByConcurrent(Function.identity(), Collectors.counting()));
                // Make sure all the bucketDirs are represented in the map.
                for (String bucketDir : bucketDirs) {
                    if (!bucketDirCount.containsKey(bucketDir)) {
                        bucketDirCount.put(bucketDir, 0L);
                    }
                }
            }

            Map.Entry<String, Long> minEntry = null;
            for (Map.Entry<String, Long> entry : bucketDirCount.entrySet()) {
                if (minEntry == null || entry.getValue().compareTo(minEntry.getValue()) < 0) {
                    minEntry = entry;
                }
            }

            // Found the entry, updated and return the path.
            minEntry.setValue(minEntry.getValue() + 1);
            return minEntry.getKey();
        }
    }

    /**
     * Do bucketization.
     *
     * @param bucketizeLock  the bucketize lock to use.
     * @param packLevel      pack level to use (see BucketPackLevel).
     * @param maxBucketCount Maximum number of buckets to generate on this call (positive integer).
     * @return number of buckets created.
     * @throws IOException
     */
    private int doBucketize(TableLock bucketizeLock, BucketPackLevel packLevel, int maxBucketCount) throws IOException {
        if (!bucketizeLock.isValid()) {
            log.debug("Bucketize - Bucketization already in progress");
            return 0;
        }

        Map<String, List<T>> newBucketsMap;
        Collection<String> bucketsToDelete;
        // We do the bucketization by goring the dictionary file, do be safe we create a copy of dictionary so we
        // are not affected by any changes made to it.   This copy is only used by the gor command we use to create
        // the buckets for anything else we use the original table object.
        BaseDictionaryTable tempTable;
        try (TableTransaction trans = TableTransaction.openReadTransaction(this.lockType, table, table.getName(), this.lockTimeout)) {

            // Check if we have enough data to start bucketizing.

            int unbucketizedCount = table.needsBucketizing().size();
            if (packLevel == BucketPackLevel.NO_PACKING && unbucketizedCount < getEffectiveMinBucketSize()) {
                log.debug("Bucketize - Nothing to bucketize, aborting {} unbucketized but {} is minimum.",
                        unbucketizedCount, getEffectiveMinBucketSize());
                return 0;
            }

            // Find which buckets to delete and which buckets to create.

            bucketsToDelete = findBucketsToDelete(trans.getLock(), packLevel, unbucketizedCount, maxBucketCount);
            newBucketsMap = findBucketsToCreate(trans.getLock(), bucketsToDelete, maxBucketCount);

            if (log.isDebugEnabled()) {
                log.debug("Bucketize - Bucketizing {} files into {} buckets",
                        newBucketsMap.values().stream().map(List::size).mapToInt(Integer::intValue).sum(), newBucketsMap.keySet().size());
            }
            tempTable = createTempTable(trans.getLock());
        }

        // Run separate bucketization run for each bucket dir (for space and for fast move of the results).
        // TODO:  This can be run in parallel if running on cluster, if local it might be better do just run sequeneally
        //        Reason to run this per bucket dir is having tmp files on that dir so the move is fast.
        for (String bucketDir : getBucketDirs()) {
            doBucketizeForBucketDir(tempTable, bucketDir, newBucketsMap);
        }

        // Clean up
        log.trace("Deleting temp table {}", tempTable.getPath());
        tempTable.delete();

        if (bucketsToDelete.size() > 0) {
            try (TableTransaction trans = TableTransaction.openWriteTransaction(this.lockType, table, table.getName(), this.lockTimeout)) {
                // Delete buckets that are to be deleted.
                deleteBuckets(trans.getLock(), false, bucketsToDelete.toArray(new String[bucketsToDelete.size()]));
                trans.commit();
            }
        }

        return newBucketsMap.size();
    }

    private void doBucketizeForBucketDir(BaseDictionaryTable tempTable, String bucketDir, Map<String, List<T>> newBucketsMap) throws IOException {
        Map<String, List<T>> newBucketsMapForBucketDir =
                newBucketsMap.keySet().stream()
                        .filter(p -> PathUtils.parent(Path.of(p)).equals(Path.of(bucketDir)))
                        .collect(Collectors.toMap(Function.identity(), newBucketsMap::get));

        //  Create the bucket files
        createBucketFilesForBucketDir(tempTable, newBucketsMapForBucketDir, getAbsoluteBucketDir(bucketDir));

        // Move files and update dictionary.
        for (String bucket : newBucketsMapForBucketDir.keySet()) {
            List<T> bucketEntries = newBucketsMapForBucketDir.get(bucket);
            updateTableWithNewBucket(table, bucket, bucketEntries);
        }
    }

    private URI getAbsoluteBucketDir(String bucketDir) throws IOException {

       String bucketDirsLocation = table.getConfigTableProperty(HEADER_BUCKET_DIRS_LOCATION_KEY, "");

        URI absBucketDir;
        if (!Strings.isNullOrEmpty(bucketDirsLocation)) {
            URI tableRoot = PathUtils.relativize(URI.create(table.getFileReader().getCommonRoot()), table.getRootUri());
            absBucketDir = PathUtils.resolve(URI.create(bucketDirsLocation), PathUtils.resolve(tableRoot, bucketDir).toString());
        } else {
            absBucketDir = PathUtils.resolve(table.getRootUri(), bucketDir);
        }

        table.getFileReader().createDirectoryIfNotExists(absBucketDir.toString());

        return absBucketDir;
    }

    private void updateTableWithNewBucket(BaseDictionaryTable table, String bucket, List<T> bucketEntries) {
        try (TableTransaction trans = TableTransaction.openWriteTransaction(this.lockType, table, table.getName(), this.lockTimeout)) {
            // Update the lines we bucketized.
            table.removeFromBucket(bucketEntries);
            table.addToBucket(bucket, bucketEntries);
            // TODO: Keep track of added files, use for clean up to guarantee that we are only deleting what we have created.

            table.setProperty(HEADER_BUCKET_SIZE_KEY, Integer.toString(this.getBucketSize()));
            table.setProperty(HEADER_MIN_BUCKET_SIZE_KEY, Integer.toString(this.getMinBucketSize()));
            trans.commit();
        }
    }

    private BaseDictionaryTable createTempTable(TableLock lock) throws IOException {
        lock.assertValid();

        // Create copy of the dictionary (so we are shielded from external changes to the file).  Must in the same dir
        // as the main file for all the relative paths to work.
        URI tempTablePath;
        String ext = FilenameUtils.getExtension(table.getPath().toString());
        tempTablePath = table.getRootUri().resolve(
        getTempTablePrefix()
            + RandomStringUtils.random(8, true, true)
            + (ext.length() > 0 ? "." + ext : ""));

        log.trace("Creating temp table {}", tempTablePath);
        table.getFileReader().copy(table.getPathUri().toString(), tempTablePath.toString());

        return initTempTable(tempTablePath.toString());
    }

    String getTempTablePrefix() {
        return "." + table.getName() + ".temp.bucketizing.";
    }

    /**
     * Initialize the temp table given by path.  It will be based on the master original table.
     *
     * @param path path to the table.
     * @return the table given by path.
     */
    private BaseDictionaryTable initTempTable(String path) {
        if (path.toString().toLowerCase().endsWith(".gord")) {
            return new DictionaryTable.Builder<>(path)
                    .useHistory(table.isUseHistory())
                    .sourceColumn(table.getSourceColumn())
                    .fileReader(table.getFileReader())
                    .validateFiles(table.isValidateFiles())
                    .build();
        } else {
            throw new GorSystemException("BaseTable of type " + path.toString() + " are not supported!", null);
        }
    }

    private void deleteBuckets(TableLock lock, boolean force, String... buckets) throws IOException {
        lock.assertValid();

        // Delete bucket files.
        deleteBucketFiles(force, buckets);

        // Remove the files from the bucket.
        table.removeFromBucket(table.filter().buckets(buckets).includeDeleted().get());
    }

    /**
     * Delete the given bucket files.  Bucketfiles accessed within the grace period are not deleted
     * unless the force=true.
     *
     * @param force         should grace period be ignored.
     * @param buckets       array of bucket paths to delete.
     * @throws IOException
     */
    private void deleteBucketFiles(boolean force, String... buckets) throws IOException {
        for (String bucket : buckets) {
            URI bucketFile = PathUtils.resolve(table.getRootUri(), bucket);
            DataSource source = table.getFileReader().resolveUrl(bucketFile.toString());

            if (source.exists()) {
                long lastAccessTime = source.getSourceMetadata().getLastModified();
                log.trace("Checking bucket file CTM {} LAT {} GPFDB {}", System.currentTimeMillis(),
                        lastAccessTime, gracePeriodForDeletingBuckets.toMillis());
                if (force || System.currentTimeMillis() - lastAccessTime > gracePeriodForDeletingBuckets.toMillis()) {
                    log.debug("Deleting bucket file {}", bucketFile);
                    source.delete();
                    deleteFileIfExists(source.getFullPath() + ".gori");
                    deleteFileIfExists(source.getFullPath() + ".meta");
                    deleteLinkFileIfExists(bucketFile.toString());
                }
            }
        }
    }

    private void deleteFileIfExists(String path) {                      
        try {
            DataSource source = table.getFileReader().resolveUrl(path);
            if (source != null && source.exists()) {
                source.delete();
            }
        } catch (IOException e) {
            //Ignore, file does not exists.
        }
    }

    private void deleteLinkFileIfExists(String path) {
        try {
            String linkFile = path + (path.endsWith(".link") ? "" : ".link");
            DataSource linkSource = table.getFileReader().resolveDataSource(table.getFileReader().createSourceReference(linkFile, false));
            if (linkSource != null && linkSource.exists()) {
                linkSource.delete();
            }
        } catch (Exception e) {
            //Ignore, assume file does not exists.
        }
    }

    /**
     * Cleans up bucket files/folders.
     *  1. Temp files from old bucketization runs that might have been left behind.
     *
     *  Note:  Temp bucket files that might be left from bucketization will be cleaned as part of standard
     *         unused bucket cleaning.
     */
    void cleanTempBucketData(TableLock bucketizeLock) {
        if (!bucketizeLock.isValid()) {
            log.debug("Bucketization in progress, will skip cleaning bucket files.");
            return;
        }

        // If get a valid write lock no temp files should be here, so go ahead and clean ALL the temp files for the table
        try (Stream<String> candStream = table.getFileReader().list(table.getRootUri().toString())) {
            String prefix = getTempTablePrefix();

            for (String candTempFile : candStream.collect(Collectors.toList())) {
                if (candTempFile.contains(prefix)) {
                    table.getFileReader().resolveUrl(table.getRootUri().resolve(candTempFile).toString()).delete();
                }
            }
        } catch (IOException ioe) {
            log.warn("Got exception when trying to clean up temp folders.  Just logging out the exception", ioe);
        }
    }

    /**
     * Cleans up bucketFiles that are not in use and have not been accessed for a given period of time.
     * <p>
     *
     * Notes:
     *  1. We only manage bucketdirs that are listed in the table meta data and are in use.  So if we stop using a
     *     folder and there are no buckets there listed in the table then we need to manage it manually.
     *
     * @param bucketizeLock  the bucketize lock to use.
     * @param force force cleaning, ignoring grace periods.
     */
    protected void cleanOldBucketFiles(TableLock bucketizeLock, boolean force) throws IOException {
        if (!bucketizeLock.isValid()) {
            log.debug("Bucketization in progress, will skip cleaning bucket files.");
            return;
        }

        Map<String, List<String>> bucketsToCleanMap = new HashMap<>();

        // Collect buckets to clean.
        try (TableTransaction trans = TableTransaction.openReadTransaction(this.lockType, table, table.getName(), this.lockTimeout)) {

            Set<String> allBucketDirs = new HashSet<>(getBucketDirs());
            allBucketDirs.addAll(table.getBuckets().stream().map(b -> PathUtils.parent(b)).collect(Collectors.toSet()));

            for (String bucketDir : allBucketDirs) {
                URI fullPathBucketDir = table.getRootUri().resolve(bucketDir);

                if (!table.getFileReader().exists(fullPathBucketDir.toString())) {
                    log.debug("Bucket folder {} never been used, nothing to clean.", fullPathBucketDir);
                    continue;
                }
                List<String> bucketsToClean = collectBucketsToClean(fullPathBucketDir, force);
                if (bucketsToClean.size() > 0) {
                    bucketsToCleanMap.put(bucketDir, bucketsToClean);
                }
            }
        }

        // Do the cleaning.
        for (String bucketDir : bucketsToCleanMap.keySet()) {
            List<String> bucketsToClean = bucketsToCleanMap.get(bucketDir);
            // Perform the deletion., safest to use the deleteBuckets table methods.
            //deleteBucketFiles(force, bucketsToClean.toArray(new Path[bucketsToClean.size()]));
            deleteBuckets(force, bucketsToClean.toArray(new String[0]));
            for (String bucket : bucketsToClean) {
                log.warn("Bucket '{}' removed as it is not used", bucket);
            }
        }
    }

    private List<String> collectBucketsToClean(URI fullPathBucketDir, boolean force) throws IOException {
        // Collect buckets to be deleted. Note:  We don't need to get table read lock as we have bucket lock so the
        // bucket part of the table will not change in away that will affect us.
        List<String> bucketsToDelete = new ArrayList<>();
        try (Stream<String> pathList = table.getFileReader().list(fullPathBucketDir.toString())) {
            for (String f : pathList.collect(Collectors.toList())) {
                String fileName = Path.of(f).getFileName().toString();
                String bucketFile = PathUtils.relativize(table.getRootUri(), fullPathBucketDir.resolve(f).toString());
                // !GM last access time.  Setting as 0 for now which basically disables the graceperiod.
                long lastAccessTime = 0; //Files.readAttributes(bucketFile, BasicFileAttributes.class).lastAccessTime().toMillis();
                log.trace("Checking bucket file CTM {} LAT {} GPFDB {}",
                        System.currentTimeMillis(), lastAccessTime, gracePeriodForDeletingBuckets.toMillis());
                if (fileName.startsWith(getBucketFilePrefix(table))
                        && fileName.endsWith(".gorz")
                        && (System.currentTimeMillis() - lastAccessTime > gracePeriodForDeletingBuckets.toMillis()
                            || force)) {
                    // This bucket file has not been accessed for some time.
                    if (table.filter().buckets(bucketFile).get().size() == 0) {
                        // This bucket is not used in the table so mark it for deletion.
                        bucketsToDelete.add(bucketFile);
                    }
                }
            }
        }
        return bucketsToDelete;
    }

    private String getBucketFilePrefix(BaseDictionaryTable table) {
        return table.getName() + "_" + BUCKET_FILE_PREFIX;
    }

    /**
     * Finds buckets to delete.
     * These are buckets that we are deleting to force rebucketization.
     *
     * @param lock              table lock
     * @param packLevel         pack level (see BucketPackLevel).
     * @param unbucketizedCount how many files are not bucketized at all.
     * @param maxBucketCount    Maximum number of buckets to generate on this call (positive integer).
     * @return buckets to be deleted.
     */
    private Collection<String> findBucketsToDelete(TableLock lock, BucketPackLevel packLevel, int unbucketizedCount, int maxBucketCount) {
        lock.assertValid();

        // Count active files per bucket
        Map<String, Integer> bucketCounts = new HashMap<>();
        table.selectAll().stream().filter(l -> l.hasBucket())
                .forEach(l -> {
                    bucketCounts.put(l.getBucket(), bucketCounts.getOrDefault(l.getBucket(), 0) + (!l.isDeleted() ? 1 : 0));
                });

        // Handle buckets were all files have beeen deleted.

        Set<String> bucketsToDelete = new HashSet(bucketCounts.keySet().stream()
                .filter(k -> bucketCounts.get(k) == 0).collect(Collectors.toSet()));


        // Handle packing.

        if (packLevel == BucketPackLevel.NO_PACKING) {
            return bucketsToDelete;
        }

        if (packLevel == BucketPackLevel.FULL_PACKING) {
            // Rebucketize all partial buckets.
            bucketsToDelete.addAll(bucketCounts.keySet().stream()
                    .filter(k -> bucketCounts.get(k) < getBucketSize()).collect(Collectors.toSet()));
        } else if (packLevel == BucketPackLevel.CONSOLIDATE) {
            // Create new full buckets from partial files, starting with the smallest.
            int totalFilesNeeding = unbucketizedCount
                    + bucketCounts.values().stream().filter(i -> i < getBucketSize()).mapToInt(Integer::intValue).sum();
            int totalNeededNewBuckets = totalFilesNeeding / getBucketSize();
            int totalNewBuckets = Math.min(totalNeededNewBuckets, maxBucketCount);

            if (totalNewBuckets == totalNeededNewBuckets) {
                // We are creating all the needed buckets, the last bucket might have some space left in it.  Remove
                // the smallest existing bucket to fill the new one up.
                int totalSpaceLeftInNewBuckets = totalNewBuckets * getBucketSize() - unbucketizedCount;

                for (Map.Entry<String, Integer> entry : bucketCounts.entrySet().stream()
                        .filter(e -> e.getValue() < getBucketSize())
                        .sorted(Map.Entry.comparingByValue()).collect(Collectors.toList())) {
                    if (totalSpaceLeftInNewBuckets <= 0) {
                        break;
                    }
                    bucketsToDelete.add(entry.getKey());
                    totalSpaceLeftInNewBuckets -= entry.getValue();
                }
            }
        }

        return bucketsToDelete;
    }

    /**
     * Find which buckets to create.
     * @param lock
     * @param bucketsToDelete
     * @param maxBucketCount    Maximum number of buckets to generate on this call (positive integer).
     * @return
     */
    private Map<String, List<T>> findBucketsToCreate(TableLock lock, Collection<String> bucketsToDelete, int maxBucketCount) {
        lock.assertValid();

        List<Path> relBucketsToDelete = bucketsToDelete != null
                ? bucketsToDelete.stream().map(b -> PathUtils.resolve(table.getRootPath(), b)).collect(Collectors.toList())
                : null;

        List<T> lines2bucketize = table.selectAll().stream()
                .filter(l -> !l.hasBucket()
                        || (!l.isDeleted()
                            && relBucketsToDelete != null
                            && relBucketsToDelete.contains(Paths.get(l.getBucketReal()))))
                .collect(Collectors.toList());

        int bucketCreateCount = (int) Math.ceil((double) lines2bucketize.size() / getBucketSize());
        if ((lines2bucketize.size() - (bucketCreateCount - 1) * getBucketSize()) < getEffectiveMinBucketSize()) {
            // The last bucket will be too small so skip it.
            bucketCreateCount--;
        }

        String bucketNamePrefix =
                String.format("%s_%s_%s_",
                        getBucketFilePrefix(table),
                        new SimpleDateFormat("yyyy_MMdd_HHmmss").format(new Date()),
                        RandomStringUtils.random(8, true, true));


        Map<String, List<T>> bucketsToCreate = new HashMap<>();
        bucketDirCount = null;
        for (int i = 1; i <= Math.min(bucketCreateCount, maxBucketCount); i++) {
            String bucketDir = pickBucketDir();
            int nextToBeAddedIndex = (i - 1) * getBucketSize();
            int nextBucketSize = Math.min(getBucketSize(), lines2bucketize.size() - nextToBeAddedIndex);
            bucketsToCreate.put(
                    PathUtils.resolve(bucketDir, bucketNamePrefix) + i + ".gorz",
                    lines2bucketize.subList(nextToBeAddedIndex, nextToBeAddedIndex + nextBucketSize));
        }
        return bucketsToCreate;
    }

    /**
     * Create bucket files using gorpipe.
     *
     * NOTE:  We run gorpipe and a temp folder on the same drive as the final destination so the move to that
     *        destination is fast.
     *
     * @param table
     * @param bucketsToCreate map with bucket name to table entries, representing the buckets to be created.
     * @param absBucketDir
     * @throws IOException
     */
    private void createBucketFilesForBucketDir(BaseDictionaryTable table, Map<String, List<T>> bucketsToCreate, URI absBucketDir) throws IOException {
        checkBucketDirExistance(absBucketDir);
        bucketCreator.createBucketsForBucketDir(table, bucketsToCreate, absBucketDir);
    }

    public static final class Builder<T extends DictionaryEntry> {
        private final BaseDictionaryTable<T> table;
        private Duration lockTimeout = null;
        private Class<? extends TableLock> lockType = null;
        private int minBucketSize = -1;
        private int bucketSize = -1;
        private BucketCreator<T> bucketCreator = null;

        private Builder(BaseDictionaryTable<T> table) {
            this.table = table;
        }

        public Builder lockType(Class val) {
            lockType = val;
            return this;
        }

        public Builder lockTimeout(Duration val) {
            lockTimeout = val;
            return this;
        }

        public Builder minBucketSize(int val) {
            minBucketSize = val;
            return this;
        }

        public Builder bucketSize(int val) {
            bucketSize = val;
            return this;
        }

        public Builder bucketCreator(BucketCreator<T> val) {
            bucketCreator = val;
            return this;
        }

        public BucketManager build() {
            return new BucketManager(this);
        }
    }

}
