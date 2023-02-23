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

import gorsat.TestUtils;
import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.table.dictionary.BaseDictionaryTable;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.lock.NoTableLock;
import org.gorpipe.gor.table.lock.TableLock;
import org.gorpipe.test.GorDictionarySetup;
import org.gorpipe.test.SlowTests;
import org.junit.*;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.gorpipe.gor.table.util.PathUtils.formatUri;
import static org.gorpipe.gor.table.util.PathUtils.resolve;

/**
 *
 */
@Category(SlowTests.class)
public class UTestBucketManager {

    private static final Logger log = LoggerFactory.getLogger(UTestBucketManager.class);

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @BeforeClass
    public static void setUp() {

    }

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    @Test
    public void testLockTimeOutParameter() throws Exception {
        String name = "LockTimeOutParameter";
        Path testWorkDir = workDir.newFolder(name).toPath();
        Path dictFile = testWorkDir.resolve(name + ".gord");

        BaseDictionaryTable<DictionaryEntry> table = createTable(dictFile);
        BucketManager man = BucketManager.newBuilder(table).lockTimeout(Duration.ofDays(13)).build();

        Assert.assertEquals("Manager should have builder lock timeout", Duration.ofDays(13), man.getLockTimeout());
    }

    @Test
    public void testSettingBucketsize() throws Exception {
        BaseDictionaryTable<DictionaryEntry> table = createTable(Paths.get("../../testing/misc_data/1m/1m.gord"));

        BucketManager buc = new BucketManager(table);
        buc.setMinBucketSize(20);
        buc.setBucketSize(100);
        Assert.assertEquals("Wrong min bucketsize", 20, buc.getEffectiveMinBucketSize());
        Assert.assertEquals("Wrong bucketsize", 100, buc.getBucketSize());

        buc.setMinBucketSize(200);
        buc.setBucketSize(300);
        Assert.assertEquals("Wrong min bucketsize", 200, buc.getEffectiveMinBucketSize());
        Assert.assertEquals("Wrong bucketsize", 300, buc.getBucketSize());

        buc.setBucketSize(300);
        buc.setMinBucketSize(200);
        Assert.assertEquals("Wrong min bucketsize", 200, buc.getEffectiveMinBucketSize());
        Assert.assertEquals("Wrong bucketsize", 300, buc.getBucketSize());

        buc = BucketManager.newBuilder(table).minBucketSize(20).bucketSize(100).build();
        Assert.assertEquals("Wrong min bucketsize", 20, buc.getEffectiveMinBucketSize());
        Assert.assertEquals("Wrong bucketsize", 100, buc.getBucketSize());

        buc = BucketManager.newBuilder(table).minBucketSize(100).bucketSize(300).build();
        Assert.assertEquals("Wrong min bucketsize", 100, buc.getEffectiveMinBucketSize());
        Assert.assertEquals("Wrong bucketsize", 300, buc.getBucketSize());

        buc = BucketManager.newBuilder(table).minBucketSize(400).bucketSize(300).build();
        Assert.assertEquals("Wrong min bucketsize", 300, buc.getEffectiveMinBucketSize());
        Assert.assertEquals("Wrong bucketsize", 300, buc.getBucketSize());

        buc = BucketManager.newBuilder(table).minBucketSize(1).bucketSize(3).build();
        Assert.assertEquals("Wrong min bucketsize", 1, buc.getEffectiveMinBucketSize());
        Assert.assertEquals("Wrong bucketsize", 3, buc.getBucketSize());

        buc = BucketManager.newBuilder(table).minBucketSize(4).bucketSize(3).build();
        Assert.assertEquals("Wrong min bucketsize", 3, buc.getEffectiveMinBucketSize());
        Assert.assertEquals("Wrong bucketsize", 3, buc.getBucketSize());
    }

    @Test
    public void testSimpleBucketize() throws Exception {
        String name = "testSimpleBucketize";

        Path dataDir = workDirPath.resolve("data");
        Files.createDirectory(dataDir);
        String[] sources = IntStream.range(1, 150).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, dataDir, 200, new int[]{1, 2, 3}, 10, "PN", true, sources);

        DictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);

        BucketManager buc = new BucketManager(table);
        buc.setMinBucketSize(10);
        buc.setBucketSize(50);
        
        // Initial bucketize.

        int bucketsCreated = buc.bucketize(BucketManager.DEFAULT_BUCKET_PACK_LEVEL, 1000);

        Assert.assertEquals("Wrong number of buckets created", 4, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        // Test default bucket added files.

        dataFiles = GorDictionarySetup.createDataFilesMap(name + "_2", workDirPath, 70, new int[]{1, 2, 3}, 10, "PN", true, sources);
        table.insert(dataFiles);
        bucketsCreated = buc.bucketize(BucketManager.DEFAULT_BUCKET_PACK_LEVEL, -1);

        Assert.assertEquals("Wrong number of buckets created", 2, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        // Add small buckets.
        int idIndex = 0;
        for (int count : new int[]{20, 10, 10, 10, 10, 10}) {
            dataFiles = GorDictionarySetup.createDataFilesMap(name + "_3" + idIndex++, workDirPath, count, new int[]{1, 2, 3}, 10, "PN", true, sources);
            table.insert(dataFiles);
            buc.bucketize(BucketManager.BucketPackLevel.NO_PACKING, -1);
        }
        // 5 : 50, 2 : 20, 5 : 10

        long bucketCount = table.selectAll().stream().map(l -> l.getBucket()).distinct().count();

        Assert.assertEquals("Wrong number of buckets", 12, bucketCount);

        // Test no packing.

        bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.NO_PACKING, -1);
        bucketCount = table.selectAll().stream().map(l -> l.getBucket()).distinct().count();

        Assert.assertEquals("Wrong number of buckets", 12, bucketCount);
        Assert.assertEquals("Wrong number of buckets created", 0, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        // Test consolidate packing

        bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.CONSOLIDATE, -1);
        List<String> ml = table.selectAll().stream().map(l -> l.getBucket()).distinct().collect(Collectors.toList());
        bucketCount = table.selectAll().stream().map(l -> l.getBucket()).distinct().count();
        // 6 : 50, 2 : 20, 0 : 10

        Assert.assertEquals("Wrong number of buckets", 8, bucketCount);
        Assert.assertEquals("Wrong number of buckets created", 1, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        // Test full packing.

        bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.FULL_PACKING, -1);
        bucketCount = table.selectAll().stream().map(l -> l.getBucket()).distinct().count();
        // 6 : 50, 1: 40, 0 : 20 : 0 : 10

        Assert.assertEquals("Wrong number of buckets", 7, bucketCount);
        Assert.assertEquals("Wrong number of buckets created", 1, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        // Test deleted files packing (standard)

        Map<String, Integer> bucketCounts = new HashMap<>();
        table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).forEach(l -> bucketCounts.put(l.getBucket(), bucketCounts.getOrDefault(l.getBucket(), 0) + 1));

        // Delete 45 entries from one full.
        String bucketToDeleteFrom = bucketCounts.keySet().stream().filter(l -> bucketCounts.get(l) == 50).findFirst().get();
        table.delete(table.filter().buckets(bucketToDeleteFrom).get().subList(0, 45));
        // 5 : 50, 1: 40, 0 : 20 : 0 : 10  1 : 5

        bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.CONSOLIDATE, -1);
        bucketCount = table.selectAll().stream().map(l -> l.getBucket()).distinct().count();

        Assert.assertEquals("Wrong number of buckets", 7, bucketCount);
        Assert.assertEquals("Wrong number of buckets created", 0, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        // Add 5 files.
        dataFiles = GorDictionarySetup.createDataFilesMap(name + "_4", workDirPath, 5, new int[]{1, 2, 3}, 10, "PN", true, sources);
        table.insert(dataFiles);

        bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.CONSOLIDATE, -1);
        bucketCount = table.selectAll().stream().map(l -> l.getBucket()).distinct().count();
        // 6 : 50, 0: 40, 0 : 20 : 0 : 10  0 : 5

        Assert.assertEquals("Wrong number of buckets", 6, bucketCount);
        Assert.assertEquals("Wrong number of buckets created", 1, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        // Test deleted files packing (full)

        // Delete from the two buckets.
        bucketCounts.clear();
        table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).forEach(l -> bucketCounts.put(l.getBucket(), bucketCounts.getOrDefault(l.getBucket(), 0) + 1));
        List<String> bucketsDeletedList = bucketCounts.keySet().stream().filter(l -> bucketCounts.get(l) == 50).collect(Collectors.toList()).subList(0, 2);

        for (String bucket : bucketsDeletedList) {
            table.delete(table.filter().buckets(bucket).get().subList(0, 10));
        }

        bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.FULL_PACKING, -1);
        bucketCount = table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(l -> l.getBucket()).distinct().count();

        Assert.assertEquals("Wrong number of buckets", 6, bucketCount);
        Assert.assertEquals("Wrong number of buckets created", 2, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        // Delete ALL from the a bucket (should have one with 30 others full with 50).
        bucketCounts.clear();
        table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).forEach(l -> bucketCounts.put(l.getBucket(), bucketCounts.getOrDefault(l.getBucket(), 0) + 1));
        bucketsDeletedList = bucketCounts.keySet().stream().filter(l -> bucketCounts.get(l) == 30).collect(Collectors.toList()).subList(0, 1);

        for (String bucket : bucketsDeletedList) {
            table.delete(table.filter().buckets(bucket).get());
        }

        bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.FULL_PACKING, -1);
        bucketCount = table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(l -> l.getBucket()).distinct().count();
        long bucketCountWithDeleted = table.selectAll().stream().filter(l -> l.hasBucket()).map(l -> l.getBucket()).distinct().count();

        Assert.assertEquals("Wrong number of buckets", 5, bucketCount);
        Assert.assertEquals("Wrong number of buckets created", 0, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        Assert.assertEquals("Wrong number of buckets", 5, bucketCountWithDeleted);
        Assert.assertEquals("Pns should removed from dict", table.selectAll().size(), 250);  // Hove totally deleted 50.

        Assert.assertTrue("Bucket files should be removed",  bucketsDeletedList.stream().allMatch(b -> !Files.exists(Paths.get(b))));

        // Test maxBucketCount

        // Delete the buckets
        List<String> buckets = table.getBuckets();
        buc.deleteBuckets(buckets.toArray(new String[buckets.size()]));
        bucketsCreated = buc.bucketize(BucketManager.DEFAULT_BUCKET_PACK_LEVEL, 1);
        Assert.assertEquals("Only one bucket should be created", 1, bucketsCreated);

        buckets = table.getBuckets();
        Assert.assertEquals("Only on bucket should be created", 1, buckets.size());
    }

    @Test
    public void testBucketizeExternal() throws Exception {
        try {
            String name = "testBucketizeExternal";
            int fileCount = 1000;
            String[] sources = IntStream.range(1, 150).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
            Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                    name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

            DictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
            BucketManager buc = new BucketManager(table);
            buc.setMinBucketSize(20);
            buc.setBucketSize(100);

            String bucketDir = resolve(table.getRootUri(), buc.pickBucketDir()).toString();

            Path dictPath = Paths.get(workDir.getRoot().toString(), name + ".gord");
            Assert.assertEquals("Dictionary file not created", dictPath.toString(), table.getPath());
            Assert.assertTrue("Dictionary file not created", Files.exists(dictPath));

            String dictContent = FileUtils.readFileToString(dictPath.toFile(), Charset.defaultCharset());
            Assert.assertEquals("Incorrect line count in dictionary", fileCount, Arrays.stream(dictContent.split("\n")).filter(l -> !l.startsWith("#")).count());

            buc.bucketize(BucketManager.BucketPackLevel.NO_PACKING, 1000);

            Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
            Assert.assertEquals("Not correct number of buckets created", 10, Files.list(Path.of(formatUri(resolve(table.getRootUri(), bucketDir)))).filter(p -> p.toString().endsWith(".gorz")).count());

            // Add more - exact bucket size
            dataFiles = GorDictionarySetup.createDataFilesMap(
                    name + "_2", workDirPath, 100, new int[]{1, 2, 3}, 10, "PN", true, sources);
            table.insert(dataFiles);
            table.save();
            buc.bucketize(BucketManager.BucketPackLevel.NO_PACKING, -1);
            Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
            Assert.assertEquals("Not correct number of buckets created", 11, Files.list(Path.of(bucketDir)).filter(p -> p.toString().endsWith(".gorz")).count());

            // Add more files - with non existing sources.
            sources = IntStream.range(1, 180).mapToObj(i -> String.format("PN2x%d", i)).toArray(size -> new String[size]);
            dataFiles = GorDictionarySetup.createDataFilesMap(
                    name, workDirPath, 180, new int[]{1, 2, 3}, 10, "PN", true, sources);
            table.insert(dataFiles);
            table.save();
            buc.bucketize(BucketManager.BucketPackLevel.NO_PACKING, -1);
            Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
            Assert.assertEquals("Not correct number of buckets created", 13, Files.list(Path.of(bucketDir)).filter(p -> p.toString().endsWith(".gorz")).count());

            // Add more files - with existing sources
            dataFiles = GorDictionarySetup.createDataFilesMap(
                    name + "_3", workDirPath, 110, new int[]{1, 2, 3}, 10, "PN", true, sources);
            table.insert(dataFiles);
            table.save();
            buc.bucketize(BucketManager.BucketPackLevel.NO_PACKING, -1);
            Assert.assertEquals("Not all lines bucketized", 10, table.needsBucketizing().size());
            Assert.assertEquals("Not correct number of buckets created", 14, Files.list(Path.of(bucketDir)).filter(p -> p.toString().endsWith(".gorz")).count());

        } catch (Throwable t) {
            throw t;
        }
    }

    @Test
    public void testCleaningOfOldTempFiles() throws Exception {
        String name = "testCleaningOfDeletedBuckets";

        // Setup small table

        int fileCount = 2;
        String[] sources = IntStream.range(1, fileCount).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        DictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
        BucketManager buc = new BucketManager(table);
        buc.setMinBucketSize(2);
        buc.setBucketSize(10);

        // Create artificial temp file
        Path tempFilePath = workDirPath.resolve(buc.getTempTablePrefix() + "111.gord");
        Files.copy(Path.of(table.getPath()), tempFilePath);
        Assert.assertTrue(Files.exists(tempFilePath));

        // Bucketize

        buc.bucketize(BucketManager.BucketPackLevel.FULL_PACKING, 100);

        String[] buckets = table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(e -> e.getBucket()).distinct().toArray(String[]::new);
        Assert.assertEquals("Should have ten buckets", 1, buckets.length);

        Assert.assertFalse(Files.exists(tempFilePath));
    }

    @Test
    public void testCleaningOfDeletedBuckets() throws Exception {
        String name = "testCleaningOfDeletedBuckets";

        // Setup small table and bucketize it.

        int fileCount = 100;
        String[] sources = IntStream.range(1, 100).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        DictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
        BucketManager buc = new BucketManager(table);
        buc.setMinBucketSize(10);
        buc.setBucketSize(10);

        buc.bucketize(BucketManager.BucketPackLevel.FULL_PACKING, 100);

        String[] buckets = table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(e -> e.getBucket()).distinct().toArray(String[]::new);
        Assert.assertEquals("Should have ten buckets", 10, buckets.length);

        // Delete one bucket, normal grace (use TableManager).
        buc.deleteBuckets(buckets[0]);
        Assert.assertTrue("Bucket file should not be deleted", Files.exists(Path.of(table.getRootPath()).resolve(buckets[0])));
        TableLock lock = new NoTableLock(table, table.getName());
        lock.lock(false, Duration.ofMillis(10000));
        buc.cleanOldBucketFiles(lock, true);
        Assert.assertFalse("Bucket file should be deleted", Files.exists(Path.of(table.getRootPath()).resolve(buckets[0])));

        // Delete one bucket, no grace period (use BucketManager).
        buc.gracePeriodForDeletingBuckets = Duration.ofMillis(0);
        buc.deleteBuckets(buckets[1]);
        Assert.assertFalse("Bucket file should be deleted", Files.exists(Path.of(table.getRootPath()).resolve(buckets[1])));

        // Delete one bucket, short grace period (lastAccessTime is measured in secs, we must use quite large delays)
        buc.gracePeriodForDeletingBuckets = Duration.ofMillis(2000);
        buc.deleteBuckets(buckets[2]);
        Assert.assertTrue("Bucket file should not be deleted", Files.exists(Path.of(table.getRootPath()).resolve(buckets[2])));
        Thread.sleep(5000);
        buc.cleanOldBucketFiles(lock, false);
        Assert.assertFalse("Bucket file should be deleted", Files.exists(Path.of(table.getRootPath()).resolve(buckets[2])));
    }

    @Test
    public void testCleaningOfDeletedBucketsExtraFiles() throws Exception {
        String name = "testCleaningOfDeletedBucketsExtraFiles";

        // Setup small table and move one bucket to be linked, adding meta and gori files.

        int fileCount = 10;
        GorDictionarySetup dictionarySetup = new GorDictionarySetup(workDirPath, name, fileCount, 5, new int[]{1,2,3}, 10, false);
        DictionaryTable table = new DictionaryTable(dictionarySetup.dictionary);

        String[] buckets = table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(e -> e.getBucket()).distinct().toArray(String[]::new);
        Assert.assertEquals("Should have 2 buckets", 2, buckets.length);

        String bucketFileName = name + "_bucketfile_Bucket0.gor";
        Path relativeBucketDirPath = Path.of("." + name + "/buckets");
        Path bucketDirPath = workDirPath.resolve(relativeBucketDirPath);
        Files.writeString(bucketDirPath.resolve(bucketFileName + DataType.META.suffix), "## Dummy meta");
        Files.writeString(bucketDirPath.resolve(bucketFileName + DataType.GORI.suffix), "## Dummy gori");

        // Delete bucket with extra files.

        BucketManager<DictionaryEntry> bucketManager = new BucketManager<>(table);
        bucketManager.gracePeriodForDeletingBuckets = Duration.ofMillis(0);
        bucketManager.deleteBuckets(relativeBucketDirPath.resolve(bucketFileName).toString());
        buckets = table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(e -> e.getBucket()).distinct().toArray(String[]::new);
        Assert.assertEquals("Should have 1 bucket", 1, buckets.length);

        Assert.assertFalse("Bucket should be deleted", Files.exists(bucketDirPath.resolve(bucketFileName)));
        Assert.assertFalse("Meta should be deleted", Files.exists(bucketDirPath.resolve(bucketFileName +  DataType.META.suffix)));
        Assert.assertFalse("gori should be deleted", Files.exists(bucketDirPath.resolve(bucketFileName + DataType.GORI.suffix)));
    }

    @Test
    public void testCleaningOfDeletedLinkedBuckets() throws Exception {
        String name = "testCleaningOfDeletedLinkedBuckets";

        // Setup small table and move one bucket to be linked, adding meta and gori files.

        int fileCount = 10;
        GorDictionarySetup dictionarySetup = new GorDictionarySetup(workDirPath, name, fileCount, 5, new int[]{1,2,3}, 10, false);
        DictionaryTable table = new DictionaryTable(dictionarySetup.dictionary);

        String[] buckets = table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(e -> e.getBucket()).distinct().toArray(String[]::new);
        Assert.assertEquals("Should have 2 buckets", 2, buckets.length);

        Path linkFolder = workDirPath.resolve("linkfolder");
        Files.createDirectory(linkFolder);
        Path relativeBucketDirPath = Path.of("." + name + "/buckets");
        Path bucketDirPath = workDirPath.resolve(relativeBucketDirPath);
        String bucketFileName = name + "_bucketfile_Bucket0.gor";

        Files.move(bucketDirPath.resolve(bucketFileName), linkFolder.resolve(bucketFileName));
        Files.writeString(linkFolder.resolve(bucketFileName + DataType.META.suffix), "## Dummy meta");
        Files.writeString(linkFolder.resolve(bucketFileName + DataType.GORI.suffix), "## Dummy gori");
        Files.writeString(bucketDirPath.resolve(bucketFileName + DataType.LINK.suffix), linkFolder.resolve(bucketFileName).toString());

        // Delete the linked bucket.

        BucketManager<DictionaryEntry> bucketManager = new BucketManager<>(table);
        bucketManager.gracePeriodForDeletingBuckets = Duration.ofMillis(0);
        bucketManager.deleteBuckets(relativeBucketDirPath.resolve(bucketFileName).toString());
        buckets = table.selectAll().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(e -> e.getBucket()).distinct().toArray(String[]::new);
        Assert.assertEquals("Should have 1 bucket", 1, buckets.length);

        Assert.assertFalse("Link should be deleted", Files.exists(bucketDirPath.resolve(bucketFileName + DataType.LINK.suffix)));

        Assert.assertFalse("Bucket should be deleted", Files.exists(linkFolder.resolve(bucketFileName)));
        Assert.assertFalse("Meta should be deleted", Files.exists(linkFolder.resolve(bucketFileName + DataType.META.suffix)));
        Assert.assertFalse("gori should be deleted", Files.exists(linkFolder.resolve(bucketFileName + DataType.GORI.suffix)));
    }

    @Test
    public void testBucketFolders() throws Exception {
        String name = "testBucketFolders";
        int fileCount = 400;
        String[] sources = IntStream.range(1, 400).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        DictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
        BucketManager buc = new BucketManager(table);
        buc.setMinBucketSize(10);
        buc.setBucketSize(10);  // Keep the bucketsize small as we use random (makes very unlikely that we get all the buckets in the same folder)

        for (String strategy : new String[]{"random", "least_used"}) {

            System.setProperty("gor.table.buckets.directory.strategy", strategy);

            int bucketsCreated = 0;
            buc.setBucketDirs(null);

            // Not specified

            List<String> buckets = table.filter().get().stream().map(l -> l.getBucket()).distinct().filter(b -> b != null).collect(Collectors.toList());
            buc.deleteBuckets(buckets.toArray(new String[buckets.size()]));
            bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.NO_PACKING, 1000);
            Assert.assertEquals("Wrong number of buckets", fileCount / buc.getBucketSize(), bucketsCreated);
            Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
            buckets = table.filter().get().stream().map(l -> l.getBucket()).distinct().collect(Collectors.toList());
            List<String> bucketFolders = buckets.stream().map(l -> PathUtils.getParent(l)).distinct().collect(Collectors.toList());
            Assert.assertEquals("Incorrect number of bucket folders", 1, bucketFolders.size());
            Assert.assertEquals("Incorrect bucket folder(s)", buc.pickBucketDir(), bucketFolders.get(0));
            for (String bucket : buckets) {
                Assert.assertTrue("Bucket does not exists", Files.exists(Path.of(PathUtils.resolve(table.getRootPath(), bucket))));
            }

            List<String> bucketDirs = new ArrayList();

            // Single directory

            bucketDirs.clear();
            bucketDirs.add("someCustomDir");
            testBucketDirsHelper(buc, table, bucketDirs, fileCount);

            // Two top level

            bucketDirs.clear();
            bucketDirs.add("someCustomDir1");
            bucketDirs.add("someCustomDir2");
            testBucketDirsHelper(buc, table, bucketDirs, fileCount);


            // Two lower level

            bucketDirs.clear();
            bucketDirs.add("toplevel/someCustomDir1");
            bucketDirs.add("toplevel/someCustomDir2");
            testBucketDirsHelper(buc, table, bucketDirs, fileCount);

            // Absolute path

            Path currentDir = Paths.get("").toAbsolutePath();
            bucketDirs.clear();
            bucketDirs.add(currentDir.resolve("absCustomDir1").toString());
            bucketDirs.add(currentDir.resolve("absCustomDir2").toString());
            try {
                testBucketDirsHelper(buc, table, bucketDirs, fileCount);
            } finally {
                FileUtils.deleteDirectory(currentDir.resolve("absCustomDir1").toFile());
                FileUtils.deleteDirectory(currentDir.resolve("absCustomDir2").toFile());
            }
        }
    }

    @Test
    public void testSharedBucketFolders() throws Exception {
        String name = "testSharedBucketFolders";
        int fileCount = 100;

        String[] sourcesA = IntStream.range(1, fileCount).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFilesA = GorDictionarySetup.createDataFilesMap(
                name + 'A', workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sourcesA);

        String[] sourcesB = IntStream.range(1, fileCount).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFilesB = GorDictionarySetup.createDataFilesMap(
                name + 'B', workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sourcesB);

        DictionaryTable tableA = TestUtils.createDictionaryWithData(name + 'A', workDirPath, dataFilesA);
        DictionaryTable tableB = TestUtils.createDictionaryWithData(name + 'B', workDirPath, dataFilesB);

        List<String> bucketDirs = new ArrayList();
        bucketDirs.add("someCustomDir");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            try {
                testBucketDirsHelper(BucketManager.newBuilder(tableA).bucketSize(10).build(), tableA, bucketDirs, fileCount);
            } catch (Exception e) {
                throw new GorSystemException(e);
            }
        }, "TestThreadA"));

        Thread.sleep(200);
        futures.add(executor.submit(() -> {
            try {
                testBucketDirsHelper(BucketManager.newBuilder(tableB).bucketSize(10).build(), tableB, bucketDirs, fileCount);
            } catch (Exception e) {
                throw new GorSystemException(e);
            }
        }, "TestThreadB"));

        for (Future future : futures) {
            // Throws an exception if an exception was thrown by the task.
            future.get();
        }

        log.debug("Test {} done", name);
    }

    @Test
    public void testBucketizeSetFalse() throws Exception {
        String name = "testBucketizeNotSet";

        Path dataDir = workDirPath.resolve("data");
        Files.createDirectory(dataDir);
        String[] sources = IntStream.range(1, 150).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, dataDir, 200, new int[]{1, 2, 3}, 10, "PN", true, sources);

        DictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
        table.setBucketize(false);

        BucketManager buc = new BucketManager(table);
        buc.setMinBucketSize(10);
        buc.setBucketSize(50);

        int bucketsCreated = buc.bucketize(BucketManager.DEFAULT_BUCKET_PACK_LEVEL,1000);

        Assert.assertEquals("Wrong number of buckets created", 0, bucketsCreated);
    }

    @Test
    public void testRepeatedBucketize() throws Exception {
        String name = "testSimpleBucketize";

        Path dataDir = workDirPath.resolve("data");
        Files.createDirectory(dataDir);
        String[] sources = IntStream.range(1, 150).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, dataDir, 200, new int[]{1, 2, 3}, 10, "PN", true, sources);

        DictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);

        BucketManager buc = new BucketManager(table);
        buc.setMinBucketSize(10);
        buc.setBucketSize(50);

        int bucketsCreated = 0;
        int iterations = 0;
        do {
            int iterBucketsCreated = buc.bucketize(BucketManager.DEFAULT_BUCKET_PACK_LEVEL, 1);
            Assert.assertEquals("Wrong number of buckets created", 1, iterBucketsCreated);
            bucketsCreated += iterBucketsCreated;
            iterations++;
        } while (table.needsBucketizing().size() >= buc.getMinBucketSize());

        Assert.assertEquals("Wrong number of buckets created", 4, bucketsCreated);
        Assert.assertEquals("Wrong number of iterations", 4, iterations);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
    }

    private BaseDictionaryTable<DictionaryEntry> createTable(Path path) {
        return new DictionaryTable.Builder<>(path).useHistory(true).validateFiles(false).build();
    }

    private void testBucketDirsHelper(BucketManager buc, BaseDictionaryTable<DictionaryEntry> table, List<String> bucketDirs, int fileCount) throws IOException {
        log.trace("Calling buckets dir helper with {}", bucketDirs);
        for (String bucketDir : bucketDirs) {
            Path bucketDirFull = Path.of(resolve(table.getRootPath(), bucketDir));
            if (!Files.exists(bucketDirFull)) {
                Files.createDirectories(bucketDirFull);
                bucketDirFull.toFile().deleteOnExit();
            }
        }
        List<String> buckets = table.filter().get().stream().map(l -> l.getBucket()).filter(p -> p != null).distinct().collect(Collectors.toList());
        buc.deleteBuckets(buckets.toArray(new String[buckets.size()]));
        buc.setBucketDirs(bucketDirs);
        int bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.NO_PACKING, 1000);
        Assert.assertEquals("Wrong number of buckets", fileCount / buc.getBucketSize(), bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
        buckets = table.filter().get().stream().map(l -> l.getBucket()).distinct().collect(Collectors.toList());
        List<String> createdBucketFolders = buckets.stream().map(p -> PathUtils.getParent(p)).distinct().collect(Collectors.toList());
        Assert.assertEquals("Incorrect number of bucket folders", bucketDirs.size(), createdBucketFolders.size());
        Assert.assertEquals("Incorrect bucket folder(s)", new TreeSet(bucketDirs), new TreeSet(createdBucketFolders));
        for (String bucket : buckets) {
            Assert.assertTrue(String.format("Bucket %s does not exists", PathUtils.resolve(table.getRootPath(), bucket)),
                    Files.exists(Path.of(PathUtils.resolve(table.getRootPath(), bucket))));
        }
    }

}
    
