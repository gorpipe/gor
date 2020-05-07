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

import org.gorpipe.gor.table.BaseTable;
import org.gorpipe.gor.table.BucketableTableEntry;
import org.gorpipe.gor.table.Dictionary;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.lock.TableLock;
import org.gorpipe.gor.table.lock.TableTransaction;
import org.gorpipe.test.GorDictionarySetup;
import org.gorpipe.test.SlowTests;
import gorsat.TestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by gisli on 18/08/16.
 */
@Category(SlowTests.class)
public class UTestTableManager {

    private static final Logger log = LoggerFactory.getLogger(UTestTableManager.class);
    private final TestTableManagerUtil testTableManagerUtil = new TestTableManagerUtil();

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
    public void testCreateTableManager() {
        TableManager man = new TableManager();
    }

    @Test
    public void testLockTimeOutParameter() throws Exception {
        String name = "LockTimeOutParameter";
        Path testWorkDir = workDir.newFolder(name).toPath();
        Path dictFile = testWorkDir.resolve(name + ".gord");
        
        TableManager man = TableManager.newBuilder().lockTimeout(Duration.ofDays(13)).build();
        BaseTable<BucketableTableEntry> table = man.initTable(dictFile);

        Assert.assertEquals("Manager should have builder lock timeout", Duration.ofDays(13), man.getLockTimeout());
    }

    @Test
    public void testBasicInsertFiles() throws Exception {
        Path testWorkDir = workDir.newFolder("BasicInsertTest").toPath();
        Path dictFile = testWorkDir.resolve("basicInsertFile.gord");
        Path testFile1 = workDir.newFile("basicInsertFile1.gor").toPath();
        Path testFile2 = Files.createFile(testWorkDir.resolve("basicInsertFile2.gor")).normalize();


        TableManager man = new TableManager();
        BaseTable<BucketableTableEntry> table = man.initTable(dictFile);
        man.insert(dictFile, BucketManager.BucketPackLevel.CONSOLIDATE, 4, new DictionaryEntry.Builder<>(testFile1, table.getRootUri()).alias("A").build());
        man.insert(dictFile, BucketManager.BucketPackLevel.CONSOLIDATE, 4, new DictionaryEntry.Builder<>(testFile2, table.getRootUri()).alias("B").build());

        String result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).collect(Collectors.joining());

        Assert.assertEquals("Insert failed", testFile1 + "\tA\nbasicInsertFile2.gor\tB\n", result);
    }

    @Test
    public void testHistory() throws Exception {
        String name = "testHistory";
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, 2, new int[]{1, 2}, 10, "PN", true, new String[]{"A", "B"});
        Path testFile1 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("A").get(0))).normalize();
        Path testFile2 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("B").get(0))).normalize();

        // Test History option false.
        TableManager manNoHist = TableManager.newBuilder().useHistory(false).build();
        String noHistDict = "noHistDict";
        BaseTable<BucketableTableEntry> table = manNoHist.initTable(workDirPath.resolve(noHistDict + ".gord"));
        table.save();
        manNoHist.insert(table.getPath(), BucketManager.BucketPackLevel.CONSOLIDATE, 4, new DictionaryEntry.Builder<>(testFile1, table.getRootUri()).alias("A").build());
        manNoHist.insert(table.getPath(), BucketManager.BucketPackLevel.CONSOLIDATE, 4, new DictionaryEntry.Builder<>(testFile2, table.getRootUri()).alias("B").build());
        Assert.assertTrue(!Files.exists(workDirPath.resolve("." + noHistDict).resolve(BaseTable.HISTORY_DIR_NAME)));

        // Test History option true.
        TableManager manHist = TableManager.newBuilder().useHistory(true).build();
        String histDict = "histDict";
        table = manHist.initTable(workDirPath.resolve(histDict + ".gord"));
        table.save();
        manHist.insert(table.getPath(), BucketManager.BucketPackLevel.CONSOLIDATE, 4, new DictionaryEntry.Builder<>(testFile1, table.getRootUri()).alias("A").build());
        manHist.insert(table.getPath(), BucketManager.BucketPackLevel.CONSOLIDATE, 4, new DictionaryEntry.Builder<>(testFile2, table.getRootUri()).alias("B").build());
        table.reload();
        Assert.assertTrue(Files.exists(workDirPath.resolve("." + histDict).resolve(BaseTable.HISTORY_DIR_NAME)));
        Assert.assertEquals(1, Files.list(workDirPath.resolve("." + histDict).resolve(BaseTable.HISTORY_DIR_NAME)).count()); // 1 action log.
    }

    @Test
    public void testMultiProcessInsert() throws Exception {
        String name = "test_multiprocess_insert";
        int fileCount = 5;
        String[] sources = IntStream.range(1, fileCount + 1).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        Path gordFile = workDirPath.resolve(name + ".gord");

        final StringWriter out_writer = new StringWriter();
        final StringWriter err_writer = new StringWriter();

        List<Process> processes = new ArrayList<>();
        for (String alias : dataFiles.keySet()) {

            for (String file : dataFiles.get(alias)) {
                Process p = testTableManagerUtil.startGorManagerCommand(gordFile.toString(), new String[]{"--history", "true"}, "insert", new String[]{"-a", alias, file.toString()}, workDir.getRoot().toString());
                testTableManagerUtil.startProcessStreamEaters(p, out_writer, err_writer);
                processes.add(p);
                log.debug("Starting processes for (" + p.toString() + ") " + alias + " count " + dataFiles.get(alias).size());
            }
        }

        // Wait for all the processes to finish.
        for (Process p : processes) {
            boolean noTimeout = p.waitFor(30, TimeUnit.SECONDS);
            int errCode = -1;
            if (noTimeout) errCode = p.exitValue();
            if (errCode != 0) {
                log.warn(p.toString());
                log.warn(err_writer.toString());
                if (!noTimeout) {
                    Assert.fail("Insert process timed out.");
                } else {
                    Assert.fail("Insert process failed with exit code " + errCode);
                }
            } else {
                log.debug("Process " + p.toString() + " finished");
            }
        }

        // Check the result.
        TableManager man = new TableManager();
        Collection<? extends BucketableTableEntry> entries = man.selectAll(gordFile);
        Assert.assertEquals("File count incorrect", fileCount, entries.size());
    }

    @Test
    public void testUpdateWhileBucketize() throws Exception {
        String name = "testUpdateWhileBucketize";
        int fileCount = 1000;
        String[] sources = IntStream.range(1, fileCount).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        man.setMinBucketSize(20);
        man.setBucketSize(100);

        DictionaryTable table = DictionaryTable.createDictionaryWithData(name, workDirPath, dataFiles);

        // Bucketize in process.
        Process p = testTableManagerUtil.startGorManagerCommand(table.getPath().toString(), null, "bucketize", new String[]{"-w", "1"}, ".");

        // Wait for the thread to get the bucketize lock (so we are waiting for getting inValid lock).
        testTableManagerUtil.waitForBucketizeToStart(table, p);

        // The bucketizing in the thread (started above) finishes by acquiring write lock to update the dictionary.  We can use write lock
        // here to make sure we can do our changes before the bucketzing in the thread finishes.
        try {
            try (TableTransaction trans = TableTransaction.openWriteTransaction(TableManager.DEFAULT_LOCK_TYPE, table, table.getName(), Duration.ofSeconds(10))) {
                if (!trans.getLock().isValid()) {
                    log.info(testTableManagerUtil.waitForProcessPlus(p));
                    Assert.assertTrue("Test not setup correctly, bucketizing finished to early.", false);
                }

                // Do some changes, add, delete, update.
                String[] sources2 = IntStream.range(1, 10).mapToObj(i -> String.format("PNX%d", i)).toArray(size -> new String[size]);
                Map<String, List<String>> dataFiles2 = GorDictionarySetup.createDataFilesMap(
                        name, workDirPath, 10, new int[]{1, 2, 3}, 10, "PN", true, sources2);
                table.insert(dataFiles2);

                Collection<DictionaryEntry> entriesToDelete = table.filter().tags("PN10", "PN11").get();
                table.delete(entriesToDelete);

                // Validate that we are sill bucketizing.
                try (TableLock bucketizeLock = TableLock.acquireWrite(TableManager.DEFAULT_LOCK_TYPE, table, "bucketize", Duration.ofMillis(100))) {
                    if (bucketizeLock.isValid()) {
                        log.info(testTableManagerUtil.waitForProcessPlus(p));
                        Assert.assertTrue("Test not setup correctly, bucketizing finished to early.", false);
                    }
                }
                trans.commit();
            }
        } finally {
            log.info(testTableManagerUtil.waitForProcessPlus(p));
        }

        // Check that the updates where handled correctly.
        table.reload();
        Assert.assertEquals("Total number of lines does not match", 1010, table.selectAll().size());
        Assert.assertEquals("New lines should not be bucketized", 10, table.needsBucketizing().size());
        Assert.assertEquals("Deleted lines should be reinserted", 2, table.selectAll().stream().filter(f -> f.isDeleted()).count());
    }


    @Test
    public void testMultiprocessBucketize() throws Exception {
        String name = "testMultiprocessBucketize";
        int fileCount = 1000;
        String[] sources = IntStream.range(1, 1000).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        DictionaryTable table = DictionaryTable.createDictionaryWithData(name, workDirPath, dataFiles);
        BucketManager buc = new BucketManager(table);
        buc.setMinBucketSize(20);
        buc.setBucketSize(100);

        // Bucketize in process.
        Process p = testTableManagerUtil.startGorManagerCommand(table.getPath().toString(), null, "bucketize", new String[]{"-w", "1"}, ".");

        // Wait for the thread to get the bucketize lock (so we are waiting for getting inValid lock).
        testTableManagerUtil.waitForBucketizeToStart(table, p);

        // Bucketize in main - should return immediately.
        int bucketsCreated = buc.bucketize(BucketManager.BucketPackLevel.NO_PACKING, -1);

        // Wait for the thread and print out stuff.
        log.debug(testTableManagerUtil.waitForProcessPlus(p));
        table.reload();

        Assert.assertEquals("Should do nothing", 0, bucketsCreated);
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
    }

    @Ignore
    // Will ignore this from automated testing (is seems quite unstable and fails often) - keep the test so we can run it manually.
    @Test
    public void testLoadingLargeDictionary() throws Exception {
        String name = "testLoadingLargeDict";

        int fileCount = 20000;
        String[] sources = IntStream.range(1, 150).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        man.setMinBucketSize(20);
        man.setBucketSize(100);

        DictionaryTable table = DictionaryTable.createDictionaryWithData(name, workDirPath, dataFiles);

        long startTime = System.currentTimeMillis();
        log.info("Time select all: {}", startTime);
        table.reloadForce();
        List<? extends DictionaryEntry> newFiles = table.getOptimizedLines(table.tagmap("ABC1234"), false);

        long newTime = System.currentTimeMillis() - startTime;
        log.info("Time using table manager: {}", newTime);

        startTime = System.currentTimeMillis();
        Dictionary d = new Dictionary(table.getPath().toString(), true, table.tagset("ABC1234"), ".", "", false);
        Dictionary.DictionaryLine[] oldFiles = d.getFiles();
        long oldTime = System.currentTimeMillis() - startTime;
        log.info("Time using old dictionary code: {}", oldTime);

        Assert.assertEquals("Old and new impl dont give same number of files", oldFiles.length, newFiles.size());
        Assert.assertTrue("Much slower than old implementtion", newTime - oldTime < 500);
    }

    @Ignore
    // Will ignore this for has we don't have access to the large dictionary yet.  Update and enable when we have access to 1m line dict.
    @Test
    public void testLoadingLargeDictionaryLocal() throws Exception {
        String name = "testLoadingLargeDictLocal";
        long startTime;

        TableManager man = new TableManager();
        man.setMinBucketSize(20);
        man.setBucketSize(100);

        DictionaryTable table = (DictionaryTable) man.initTable(Paths.get("../../testing/misc_data/1m/1m.gord"));

        startTime = System.currentTimeMillis();
        Dictionary d = new Dictionary(table.getPath().toString(), true, table.tagset("ABC1234"), ".", "", false);
        Dictionary.DictionaryLine[] oldFiles = d.getFiles();
        long oldTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        table.reloadForce();
        List<? extends DictionaryEntry> newFiles = table.getOptimizedLines(table.tagmap("ABC1234"), false);
        long newTime = System.currentTimeMillis() - startTime;

        log.info("Time using table manager: {}", newTime);
        log.info("Time using old dictionary code: {}", oldTime);

        Assert.assertEquals("Old and new impl dont give same number of files", oldFiles.length, newFiles.size());
        Assert.assertTrue("Much slower than old implementtion", newTime - oldTime < 500);
    }

    @Ignore
    // Will ignore this for has we don't have access to the large dictionary yet.  Update and enable when we have access to 1m line dict.
    @Test
    public void testSelectFromLargeDictionary() throws Exception {
        String name = "testSelectFromLargeDictionary";

        long startTime = 0;
        String tags = "PN1000,PN10000,PN500000,PN900000";

        startTime = System.currentTimeMillis();
        String res = TestUtils.runGorPipe("nor -asdict ../../testing/misc_data/1m/1m.gord | where (#2 == 'PN1000' or #2 == 'PN10000' or #2 == 'PN500000' or #2 == 'PN900000')");
        long gorpipeTime = System.currentTimeMillis() - startTime;
        log.info("Filtering lines with gorpipe from large dict: {}", gorpipeTime);

        TableManager man = new TableManager();
        man.setMinBucketSize(20);
        man.setBucketSize(100);

        startTime = System.currentTimeMillis();
        DictionaryTable table = (DictionaryTable) man.initTable(Paths.get("../../testing/misc_data/1m/1m.gord"));
        List<DictionaryEntry> entries = table.filter().tags(tags.split(",")).get();
        long tableLoadAndTagFilterTime = System.currentTimeMillis() - startTime;
        log.info("Filtering entries with dictionary table by tags from large dict: {}", tableLoadAndTagFilterTime);

        startTime = System.currentTimeMillis();
        entries = table.filter().tags(tags.split(",")).get();
        long tableTagFilterTime = System.currentTimeMillis() - startTime;
        log.info("Filtering entries with dictionary table by tags from large dict (already loaded): {}", tableTagFilterTime);

        startTime = System.currentTimeMillis();
        for (String tag : tags.split(",")) {
            table.getEntries(tag);
        }
        long tableEntryFilterTime = System.currentTimeMillis() - startTime;
        log.info("Filtering entries with dictionary table by key from large dict (already loaded): {}", tableEntryFilterTime);
    }

    @Ignore
    // Will ignore this for has we don't have access to the large dictionary yet.  Update and enable when we have access to 1m line dict.
    @Test
    public void testDeleteFromLargeDictionary() throws Exception {
        String name = "testDeleteFromLargeDictionary";

        Path gord = workDirPath.resolve("deleteFrom1m.gord");
        Files.copy(Paths.get("../../testing/misc_data/1m/1m.gord"), gord);

        long startTime = 0;
        String tags = "PN1000,PN10000,PN500000,PN900000";

        TableManager man = new TableManager();
        man.setMinBucketSize(20);
        man.setBucketSize(100);

        DictionaryTable table = (DictionaryTable) man.initTable(gord);
        table.reloadForce();

        startTime = System.currentTimeMillis();
        List<DictionaryEntry> entries = table.filter().tags(tags.split(",")).get();
        long tableTagFilterTime = System.currentTimeMillis() - startTime;
        Assert.assertEquals("Selection failed", tags.split(",").length, entries.size());
        log.info("Finding entries with dictionary table by tags from large dict (already loaded): {}", tableTagFilterTime);

        startTime = System.currentTimeMillis();
        table.delete(entries);
        long tableDeleteTime = System.currentTimeMillis() - startTime;
        log.info("Deleting entries from dictionary from large dict (already loaded): {}", tableDeleteTime);
    }

   /* @Ignore
    // Will ignore this for has we don't have access to the large dictionary yet.  Update and enable when we have access to 1m line dict.
    @Test
    public void testSignatureOfLargeDictionaryFewTags() throws Exception {
        String name = "testSignatureOfLargeDictionaryFewTags";
        String[] tags = new String[]{"PN1000", "PN10000", "PN500000", "PN900000"};

        TableManager man = new TableManager();
        DictionaryTable table = (DictionaryTable) man.initTable(Paths.get("../../testing/misc_data/1m/1m.gord"));
        long startTime = System.currentTimeMillis();
        String s1 = table.getSignatureCand(tags);
        long t1 = System.currentTimeMillis() - startTime;
        log.info("Signature (few tags, table not loaded, directly from bytes): {}", t1);

        startTime = System.currentTimeMillis();
        String s2 = table.getSignature(tags);
        long t2 = System.currentTimeMillis() - startTime;
        log.info("Signature (few tags, table not loaded, normal load): {}", t2);

        startTime = System.currentTimeMillis();
        String s3 = table.getSignature(tags);
        long t3 = System.currentTimeMillis() - startTime;
        log.info("Signature (few tags, table loaded): {}", t3);

        Assert.assertEquals("Signature is diffrerent", s1, s2);
        Assert.assertEquals("Signature is diffrerent", s1, s3);
    }*/

    @Ignore
    // Will ignore this for has we don't have access to the large dictionary yet.  Update and enable when we have access to 1m line dict.
    @Test
    public void testSignatureOfLargeDictionaryManyTags() throws Exception {
        String name = "testSignatureOfLargeDictionaryManyTags";

        TableManager man = new TableManager();
        DictionaryTable table = (DictionaryTable) man.initTable(Paths.get("../../testing/misc_data/1m/1m.gord"));
        Random r = new Random();
        String[] tags = table.getAllActiveTags().stream().filter(t -> r.nextFloat() > 0.9).collect(Collectors.toList()).toArray(new String[0]);  // Randomly pick 10% of the tags..

        long startTime = System.currentTimeMillis();
        table.getSignature(tags);
        long newTime = System.currentTimeMillis() - startTime;
        log.info("Signature (many tags): {}", newTime);
    }

    @Test
    public void testRepeatedInsertDelete() throws Exception {
        String name = "testRepeatedInsertDelete";

        int fileCount = 10;
        String[] sources = IntStream.range(1, fileCount + 1).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        final Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        DictionaryTable dummyTable = new DictionaryTable.Builder<>(workDirPath.resolve("dummy")).build();
        DictionaryEntry[] entries = dataFiles.keySet().stream().map(k -> new DictionaryEntry.Builder(dataFiles.get(k).get(0).toString(), dummyTable.getRootUri()).alias(k).build()).toArray(size -> new DictionaryEntry[size]);
        String[] pns = dataFiles.keySet().toArray(new String[0]);

        DictionaryTable table1 = DictionaryTable.createDictionaryWithData(name + 1, workDirPath, new HashMap<>());
        testRepeatedInsertDeleteHelper(man, entries, pns, table1, false, BucketManager.BucketPackLevel.NO_PACKING);
    }

    @Test
    public void testRepeatedInsertDeleteWithBucketsSmall() throws Exception {
        String name = "testRepeatedInsertDeleteWithBucketsSmall";

        int fileCount = 10;
        String[] sources = IntStream.range(1, fileCount + 1).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        final Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        DictionaryTable dummyTable = new DictionaryTable.Builder<>(workDirPath.resolve("dummy")).build();
        DictionaryEntry[] entries = dataFiles.keySet().stream().map(k -> new DictionaryEntry.Builder(dataFiles.get(k).get(0).toString(), dummyTable.getRootUri()).alias(k).build()).toArray(size -> new DictionaryEntry[size]);
        String[] pns = dataFiles.keySet().toArray(new String[0]);

        man.setBucketSize(1);
        man.setMinBucketSize(1);
        DictionaryTable table2 = DictionaryTable.createDictionaryWithData(name + 2, workDirPath, new HashMap<>());
        testRepeatedInsertDeleteHelper(man, entries, pns, table2, true, BucketManager.BucketPackLevel.NO_PACKING);
        Assert.assertEquals("Number of used buckets wrong", table2.filter().get().size(), table2.filter().get().stream().map(e -> e.getBucket()).filter(b -> b != null).distinct().count());
    }

    @Test
    public void testRepeatedInsertDeleteWithBucketsMedium() throws Exception {
        String name = "testRepeatedInsertDeleteWithBucketsMedium";

        int fileCount = 10;
        String[] sources = IntStream.range(1, fileCount + 1).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        final Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        DictionaryTable dummyTable = new DictionaryTable.Builder<>(workDirPath.resolve("dummy")).build();
        DictionaryEntry[] entries = dataFiles.keySet().stream().map(k -> new DictionaryEntry.Builder(dataFiles.get(k).get(0).toString(), dummyTable.getRootUri()).alias(k).build()).toArray(size -> new DictionaryEntry[size]);
        String[] pns = dataFiles.keySet().toArray(new String[0]);

        man.setBucketSize(4);
        man.setMinBucketSize(2);
        DictionaryTable table3 = DictionaryTable.createDictionaryWithData(name + 3, workDirPath, new HashMap<>());
        testRepeatedInsertDeleteHelper(man, entries, pns, table3, true, BucketManager.BucketPackLevel.NO_PACKING);
        Assert.assertEquals("Number of used buckets wrong", table3.selectAll().size() / 2, table3.filter().get().stream().map(e -> e.getBucket()).filter(b -> b != null).distinct().count());
    }

    @Test
    public void testRepeatedInsertDeleteWithBucketsMediumFullPacking() throws Exception {
        String name = "testRepeatedInsertDeleteWithBucketsMediumFullPacking";

        int fileCount = 10;
        String[] sources = IntStream.range(1, fileCount + 1).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);
        final Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        DictionaryTable dummyTable = new DictionaryTable.Builder<>(workDirPath.resolve("dummy")).build();
        DictionaryEntry[] entries = dataFiles.keySet().stream().map(k -> new DictionaryEntry.Builder(dataFiles.get(k).get(0).toString(), dummyTable.getRootUri()).alias(k).build()).toArray(size -> new DictionaryEntry[size]);
        String[] pns = dataFiles.keySet().toArray(new String[0]);

        man.setBucketSize(4);
        man.setMinBucketSize(2);
        DictionaryTable table4 = DictionaryTable.createDictionaryWithData(name + 4, workDirPath, new HashMap<>());
        testRepeatedInsertDeleteHelper(man, entries, pns, table4, true, BucketManager.BucketPackLevel.FULL_PACKING);
        Assert.assertEquals("Number of used buckets wrong", table4.selectAll().size() / 4 + (table4.selectAll().size() % 4) / 2,
                table4.filter().get().stream().map(e -> e.getBucket()).filter(b -> b != null).distinct().count());
                
    }

    private void testRepeatedInsertDeleteHelper(TableManager man, DictionaryEntry[] entries, String[] pns, BaseTable table, boolean bucketize, BucketManager.BucketPackLevel pack) throws InterruptedException {
        // Turn of the dictionary cache, if the cache is active we need ot wait for 1 second after each insert.
        System.setProperty("gor.dictionary.cache.active", "false");   // The RestoreSystemProperty rule will take care of restore the property.

        table.insert(entries[0]);
        table.save();
        
        if (bucketize) man.bucketize(table.getPath(), pack, 1, -1, null);
        TestUtils.assertTwoGorpipeResults("Unexpected content", "gor " + entries[0].getContentReal().toString(), "gor " + table.getPath().toString() + " | select 1-6");
        TestUtils.assertTwoGorpipeResults("Unexpected content", "gor " + entries[0].getContentReal().toString(), "gor " + table.getPath().toString() + " -f " + pns[0] + " | select 1-6");

        table.reload();
        table.delete(table.filter().tags(pns[0]).get());
        table.save();

        // Add multiple entries, delete some.
        List<DictionaryEntry> activeEntries = new ArrayList<>();
        for (int i = 0; i < pns.length; i++) {
            table.insert(entries[i]);
            System.err.println("Added: " + pns[i]);
            try {
                System.err.println(table.getPath());
                System.err.println(FileUtils.readFileToString(table.getPath().toFile(), Charset.defaultCharset()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            activeEntries.add(entries[i]);

            if (i > 0 && i % 3 == 0) {
                // Remove every third pn.
                table.delete(table.filter().tags(pns[i - 1]).get());
                activeEntries.remove(activeEntries.size() - 2);
            }
            table.save();

            if (bucketize) man.bucketize(table.getPath(), pack, 1, -1, null);

            table.reload();

            TestUtils.assertTwoGorpipeResults("Unexpected tag content", "gor " + entries[i].getContentReal().toString(), "gor " + table.getPath().toString() + " -f " + pns[i] + " | select 1-6");

            TestUtils.assertTwoGorpipeResults("Unexpected all content",
                    "gor " + activeEntries.stream().map(e -> e.getContentReal().toString()).collect(Collectors.joining(" ")),
                    "gor " + table.getPath().toString() + " | select 1-6");
        }
        // Must reload to update from bucketizing (as that is not called on the table).
        table.reload();
    }

    @Test
    public void testBasicCLI() throws Exception {
        String name = "testBasicCLI";
        int fileCount = 4;
        String[] sources = new String[]{"A", "B", "C", "D"};
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        Path dictFile = workDirPath.resolve(name + ".gord");

        String testFile1 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("A").get(0))).normalize().toString();
        String testFile2 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("B").get(0))).normalize().toString();
        String testFile3 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("C").get(0))).normalize().toString();
        String testFile4 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("D").get(0))).normalize().toString();

        testTableManagerUtil.executeGorManagerCommand(dictFile.toString(), new String[]{}, "insert", new String[]{"--alias", "A", testFile1}, workDirPath.toString(), true);
        testTableManagerUtil.executeGorManagerCommand(dictFile.toString(), new String[]{}, "insert", new String[]{"--alias", "B", testFile2}, workDirPath.toString(), true);
        testTableManagerUtil.executeGorManagerCommand(dictFile.toString(), new String[]{}, "insert", new String[]{"--alias", "D", testFile4}, workDirPath.toString(), true);

        TableManager man = new TableManager();
        BaseTable<BucketableTableEntry> table = man.initTable(dictFile);

        String result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFile1 + "\tA\n" + testFile2 + "\tB\n" + testFile4 + "\tD\n", result);

        testTableManagerUtil.executeGorManagerCommand(table.getPath().toString(), null, "delete", new String[]{"--tags", "B"}, workDirPath.toString(), true);
        table.reload();
        result = table.selectUninon(table.filter().tags("B")).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Delete failed", "", result);

        testTableManagerUtil.executeGorManagerCommand(table.getPath().toString(), null, "bucketize", new String[]{"-w", "1", "--min_bucket_size", "1"}, ".", true);
        table.reload();
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        List<String> buckets = table.filter().get().stream().map(l -> l.getBucket().toString()).distinct().collect(Collectors.toList());
        testTableManagerUtil.executeGorManagerCommand(table.getPath().toString(), null, "delete_bucket", buckets.toArray(new String[buckets.size()]), ".", true);
        table.reload();
        Assert.assertEquals("Delete buckets failed", 2, table.needsBucketizing().size());

        testTableManagerUtil.executeGorManagerCommand(table.getPath().toString(), null, "delete", new String[]{"--tags", "A"}, workDirPath.toString(), true);
        table.reload();
        result = table.selectUninon(table.filter().tags("A")).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Delete failed", "", result);
        Assert.assertEquals("Delete failed", 1, table.selectAll().size());

        testTableManagerUtil.executeGorManagerCommand(dictFile.toString(), new String[]{}, "insert", new String[]{"--alias", "C", testFile3}, workDirPath.toString(), true);
        table.reload();
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFile3 + "\tC\n" + testFile4 + "\tD\n", result);
        Assert.assertEquals("Insert failed", 2, table.selectAll().size());

        Files.createDirectories(table.getRootPath().resolve("X"));
        Files.createDirectories(table.getRootPath().resolve("Y"));
        testTableManagerUtil.executeGorManagerCommand(table.getPath().toString(), null, "bucketize", new String[]{"-w", "1", "--min_bucket_size", "1", "--bucket_dirs", "X,Y"}, ".", true);
        table.reload();
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
        List<String> bucketFolders = table.filter().get().stream().map(l -> Paths.get(l.getBucket()).getParent().toString()).distinct().collect(Collectors.toList());
        for (String f : bucketFolders) {
            Assert.assertTrue("Invalid buckets", f.matches("[XY]"));
        }

    }

    @Test
    public void testHistoryOptionCLI() throws Exception {
        String name = "testHistoryOptionCLI";
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, 2, new int[]{1, 2}, 10, "PN", true, new String[]{"A", "B"});
        String testFile1 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("A").get(0))).normalize().toString();
        String testFile2 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("B").get(0))).normalize().toString();

        // Test History option.
        String noHistDict = "noHistDict";
        testTableManagerUtil.executeGorManagerCommand(workDirPath.resolve(noHistDict + ".gord").toString(), new String[]{}, "insert", new String[]{"--alias", "A", testFile1}, workDirPath.toString(), true);
        Assert.assertTrue(Files.exists(workDirPath.resolve("." + noHistDict).resolve(BaseTable.HISTORY_DIR_NAME)));

        String noHistDict2 = "noHistDict2";
        testTableManagerUtil.executeGorManagerCommand(workDirPath.resolve(noHistDict2 + ".gord").toString(), new String[]{"--history", "false"}, "insert", new String[]{"--alias", "A", testFile1}, workDirPath.toString(), true);
        Assert.assertTrue(!Files.exists(workDirPath.resolve("." + noHistDict2).resolve(BaseTable.HISTORY_DIR_NAME)));

        String histDict2 = "histDict2";
        testTableManagerUtil.executeGorManagerCommand(workDirPath.resolve(histDict2 + ".gord").toString(), new String[]{"--history", "true"}, "insert", new String[]{"--alias", "A", testFile1}, workDirPath.toString(), true);
        testTableManagerUtil.executeGorManagerCommand(workDirPath.resolve(histDict2 + ".gord").toString(), new String[]{"--history", "true"}, "insert", new String[]{"--alias", "B", testFile2}, workDirPath.toString(), true);
        Assert.assertTrue(Files.exists(workDirPath.resolve("." + histDict2).resolve(BaseTable.HISTORY_DIR_NAME)));
        Assert.assertEquals(1, Files.list(workDirPath.resolve("." + histDict2).resolve(BaseTable.HISTORY_DIR_NAME)).count()); // 1 action.log.

    }

    @Test
    public void testDirectCLI() throws Exception {
        String name = "testDirectCLI";
        int fileCount = 4;
        String[] sources = new String[]{"A", "B", "C", "D"};
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManagerCLI.main(new String[]{"help"});

        Path dictFile = workDirPath.resolve(name + ".gord");

        String testFile1 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("A").get(0))).normalize().toString();
        String testFile2 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("B").get(0))).normalize().toString();
        String testFile3 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("C").get(0))).normalize().toString();
        String testFile4 = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("D").get(0))).normalize().toString();

        TableManagerCLI.main(new String[]{dictFile.toString(), "insert", "--alias", "A", testFile1});
        TableManagerCLI.main(new String[]{dictFile.toString(), "insert", "--alias", "B", testFile2});
        TableManagerCLI.main(new String[]{dictFile.toString(), "insert", "--alias", "D", testFile4});

        TableManager man = new TableManager();
        BaseTable<BucketableTableEntry> table = man.initTable(dictFile);

        String result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFile1 + "\tA\n" + testFile2 + "\tB\n" + testFile4 + "\tD\n", result);

        TableManagerCLI.main(new String[]{dictFile.toString(), "delete", "--tags", "B"});
        table.reload();
        result = table.selectUninon(table.filter().tags("B")).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Delete failed", "", result);

        TableManagerCLI.main(new String[]{dictFile.toString(), "bucketize", "-w", "1", "--min_bucket_size", "1"});
        table.reload();
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
    }

}
    
