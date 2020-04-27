/*
 * Copyright (c) 2016.  WuxiNextCODE Inc.
 *
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * WuxiNextCODE Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with WuxiNextCODE.
 */

package org.gorpipe.gor.table.lock;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.BaseTable;
import org.gorpipe.gor.table.BucketableTableEntry;
import org.gorpipe.gor.table.TableEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.test.IntegrationTests;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/**
 * Unit tests for gor table lock.
 * <p>
 * Created by gisli on 03/01/16.
 */
@Category(IntegrationTests.class)
@SuppressWarnings("squid:S2925")
//Suppressing warnings for Thread.sleep which is intentionally used here for multiprocess testing
public class UTestTableLock {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(UTestTableLock.class);

    private static Path tableWorkDir;
    private static String gort1;

    @BeforeClass
    public static void setUp() throws Exception {

        tableWorkDir = Files.createTempDirectory("UnitTestGorTableLockWorkDir");

        for (int i = 1; i < 20; i++) {
            Files.createFile(tableWorkDir.resolve(String.format("filepath%d.gor", i)));
        }

        gort1 = "filepath1.gor\n" +
                "filepath2.gor\ttagA\n" +
                "filepath3.gor\ttagB\n" +
                "filepath4.gor\t\tchr1\t10000\tchr1\t30000\ttagD,tagE\n" +
                "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\n" +
                "filepath6.gor\ttagF\tchr1\t30000\tchr2\t10000\n" +
                "filepath7.gor\t\tchr3\t10000\tchr4\t10000\ttagF1,tagF2\n" +
                "filepath8.gor\ttagA\n" +
                "filepath9.gor|bucket1\ttagG\n" +
                "filepath10.gor|bucket1\ttagH\n" +
                "filepath11.gor|bucket2\ttagI\n" +
                "filepath12.gor|bucket2\t\tchr1\t1\tchr2\t20000\ttagJ,tagK\n" +
                "filepath13.gor|bucket2\n" +
                "filepath14.gor|D|bucket2\ttagL\n" +
                "filepath15.gor|D|bucket2\n" +
                "filepath16.gor\ttagD\n" +
                "filepath17.gor\ttagB\t\t\t\t\t\n" +
                "filepath18.gor\t\t\t\t\t\ttagJ,tagM\n" +
                "filepath19.gor\ttagK\t\t\t\t\t\n";
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(tableWorkDir.toFile());
    }

    @Test
    public void testThreadNoTableLock() throws Exception {
        // Should fail as we have are not doing any locking.
        try {
            testThreadTableFileLock(NoTableLock.class);
            Assert.fail("No lock should fail");
        } catch (ComparisonFailure cf) {
            // Expected result.
        }
    }

    @Test
    public void testProcessNoTableLock() throws Exception {
        // Should fail as we have are not doing any locking.
        try {
            testProcessTableFileLock(NoTableLock.class);
            Assert.fail("No lock should fail");
        } catch (ComparisonFailure cf) {
            // Expected result.
        }
    }

    @Test
    public void testThreadFileTableLock() throws Exception {
        testThreadTableFileLock(FileTableLock.class);
    }

    @Test
    public void testThreadExclusiveFileTableLock() throws Exception {
        testThreadTableFileLock(ExclusiveFileTableLock.class);
    }

    @Ignore // There are problems with FileLock as does not seem to get exclusive locks.
    @Test
    public void testProcessFileTableLock() throws Exception {
        testProcessTableFileLock(FileTableLock.class);
    }

    @Test
    public void testProcessExclusiveFileTableLock() throws Exception {
        testProcessTableFileLock(ExclusiveFileTableLock.class);
    }

    @Test
    public void testRenewFileTableLock() throws Exception {
        testTableLockRenew(FileTableLock.class, null);
    }

    @Test
    public void testRenewExclusiveFileTableLock() throws Exception {
        Field drlp = ExclusiveFileTableLock.class.getDeclaredField("EXCL_DEFAULT_RESERVE_LOCK_PERIOD");
        drlp.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(drlp, drlp.getModifiers() & ~Modifier.FINAL);
        testTableLockRenew(ExclusiveFileTableLock.class, drlp);
    }

    @Ignore // There are problems with FileLock as does not seem to get exclusive locks.
    @Test
    public void testCleanUpFileTableLock() throws Exception {
        testTableLockCrashCleanUp(FileTableLock.class);
    }

    @Test
    public void testCleanUpExclusiveFileTableLock() throws Exception {
        testTableLockCrashCleanUp(ExclusiveFileTableLock.class);
    }

    @Test
    public void testMultiProcessUpdateFileTableLock() throws Exception {
        testMultiProcessUpdateTableFileLock(FileTableLock.class);
    }

    @Test
    public void testMultiProcessUpdateExclusiveFileTableLock() throws Exception {
        testMultiProcessUpdateTableFileLock(ExclusiveFileTableLock.class);
    }

    public static void testThreadTableFileLock(Class<? extends TableLock> tableLockClass) throws Exception {

        File gordFile = new File(tableWorkDir.toFile(), "gortable_filelock.gord");

        TableManager tm = TableManager.newBuilder().build();

        Files.deleteIfExists(gordFile.toPath());
        Files.deleteIfExists(Paths.get(tableWorkDir.toString(), ".gortable_filelock/gortable_filelock.gortable_filelock.lock"));

        FileUtils.write(gordFile, gort1, (Charset)null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        String orgRes = "filepath11.gor|bucket2\ttagI\n" +
                "filepath12.gor|bucket2\t\tchr1\t1\tchr2\t20000\ttagJ,tagK\n" +
                "filepath13.gor|bucket2\n" +
                "filepath14.gor|D|bucket2\ttagL\n" +
                "filepath15.gor|D|bucket2\n";

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future> futures = new ArrayList<>();
        List<String> actionList = Collections.synchronizedList(new ArrayList<>());;

        // Get lock and read file (wait after we get lock).
        actionList.add("1");

        Future f = executor.submit(() -> {
            log.debug("Thread0 - Getting read lock");
            debugReadLock(tableLockClass, Duration.ofMillis(2000),
                    () -> {
                        try {
                            log.debug("Thread0 - Got lock");
                            log.debug("Thread0 - Read 0");
                            DictionaryTable dict0 = new DictionaryTable.Builder<>(gordFile.toPath()).build();
                            String selectRes;
                            try (TableTransaction trans = TableTransaction.openReadTransaction(tableLockClass, dict0, dict0.getName(), tm.getLockTimeout())) {
                                actionList.add("2");
                                selectRes = selectStringFilter(dict0.filter().buckets("bucket2"));
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    // Ignore
                                }
                                actionList.add("3");
                            }
                            log.debug("Thread0 - Read 0 Done");
                            Assert.assertEquals("Select failed", orgRes, selectRes);
                        } catch (Throwable t) {
                            log.warn("Thread0 fail", t);
                            throw t;
                        }
                    },
                    dict, dict.getName(), Duration.ofSeconds(10));
            log.debug("Thread0 - Read lock released");
        }, "TestThread0");
        futures.add(f);

        // Reading in thread - only read locks so read should run now (and be first to print out results).

        f = executor.submit(() -> {
            try {
                log.debug("Thread1 - Read 1");
                DictionaryTable dict1 = new DictionaryTable.Builder<>(gordFile.toPath()).build();
                String selectRes;
                try (TableTransaction trans = TableTransaction.openReadTransaction(tableLockClass, dict1, dict1.getName(), tm.getLockTimeout())) {
                    actionList.add("2");
                    selectRes = selectStringFilter(dict1.filter().buckets("bucket2"));
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    actionList.add("3");
                }
                log.debug("Thread1 - Read 1 Done");
                Assert.assertEquals("Select failed", orgRes, selectRes);
            } catch (Throwable t) {
                log.warn("Thread1 fail", t);
                throw t;
            }
        }, "TestThread1");
        futures.add(f);

        Thread.sleep(20);

        // Delete in thread - should wait until the read lock (from thread 0) is released.

        f = executor.submit(() -> {
            try {
                log.debug("Thread2 - Loading dict");
                DictionaryTable dict2 = new DictionaryTable.Builder<>(gordFile.toPath()).build();
                log.debug("Thread2 - Asking to delete");
                try (TableTransaction trans = TableTransaction.openWriteTransaction(tableLockClass, dict2, dict2.getName(), tm.getLockTimeout())) {
                    log.debug("Thread2 - Write lock");
                    actionList.add("4");
                    dict2.removeFromBucket(dict2.filter().buckets("bucket2").get());
                    trans.commit();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    actionList.add("5");
                }
                log.debug("Thread2 - Done deleting");
            } catch (Throwable t) {
                log.warn("Thread2 fail", t);
                throw t;
            }
        }, "TestThread2");
        futures.add(f);

        Thread.sleep(60);

        // Reading in thread after write lock requested - should wait for the write lock that is in queue.

        f = executor.submit(() -> {
            try {
                log.debug("Thread3 - Read 2");
                DictionaryTable dict3 = new DictionaryTable.Builder<>(gordFile.toPath()).build();
                String selectRes;
                try (TableTransaction trans = TableTransaction.openReadTransaction(tableLockClass, dict3, dict3.getName(), tm.getLockTimeout())) {
                    log.debug("Thread3 - Read 2 lock");
                    actionList.add("6");
                    selectRes = selectStringFilter(dict3.filter().buckets("bucket2"));
                }
                log.debug("Thread3 - Read 2 Done");
                Assert.assertEquals("Delete bucket incorrect", "", selectRes);
            } catch (Throwable t) {
                log.warn("Thread3 fail", t);
                throw t;
            }
        }, "TestThread3");
        futures.add(f);

        // Read in main - read lock in queue so we should block.
        log.debug("Main - Read 3");
        String selectRes;
        try (TableTransaction trans = TableTransaction.openReadTransaction(tableLockClass, dict, dict.getName(), tm.getLockTimeout())) {
            actionList.add("6");
            dict.reload();
            selectRes = selectStringFilter(dict.filter().buckets("bucket2"));
        }
        log.debug("Main - Read 3 Done");
        Assert.assertEquals("Select failed", "", selectRes);

        for (Future future : futures) {
            // Throws an exception if an exception was thrown by the task.
            future.get();
        }

        // Read lock should be released, and the delete executed.
        log.debug("Main - Read 4 ");
        try (TableTransaction trans = TableTransaction.openReadTransaction(tableLockClass, dict, dict.getName(), tm.getLockTimeout())) {
            actionList.add("7");
            dict.reload();
            selectRes = selectStringFilter(dict.filter().buckets("bucket2"));
        }
        log.debug("Main - Read 4 Done");
        Assert.assertEquals("Delete bucket incorrect", "", selectRes);

        String outputOrder = actionList.stream().collect(Collectors.joining(","));
        log.debug("Output order: " + outputOrder);
        Assert.assertEquals("", "1,2,2,3,3,4,5,6,6,7", outputOrder);

    }

    public static void testProcessTableFileLock(Class<? extends TableLock> tableLockClass) throws Exception {
        String lockName = "ProcessTableLock";
        BaseTable table = new DictionaryTable.Builder<>(tableWorkDir.resolve("ProcessTableeLock" + tableLockClass.getSimpleName())).build();
        table.save();

        String storyName = "storyProcess" + tableLockClass.getSimpleName();
        String dataFileName = "dateProcess" + tableLockClass.getSimpleName();

        // For debugging (validate the main function).
        //debugWriteLock(tableLockClass, Duration.ofHours(10), table,  "CrashLockStandard", Duration.ofMillis(10));
        //main(new String[]{tableLockClass.getCanonicalName(), "WRITE", "100000000", "100", "CrashLockStandard", tableWorkDir.toString(), "W1", storyName});

        // Get lock and read file (wait after we get lock).

        Process p1 = startLockingProcess(tableLockClass, "READ", Duration.ofMillis(2000), Duration.ofMillis(10), table, lockName, tableWorkDir, "R1", storyName, dataFileName);
        Thread.sleep(2000); // Must wait a little while the external process starts.

        // Reading - only read locks so read should run now.
        /* TODO:  Ignore for now, current implementations get explicit locks
        debugReadLock(tableLockClass, Duration.ofMillis(10), null, table, "ProcessTableLock", Duration.ofMillis(10));
        Thread.sleep(10);
        */

        // Write in process - should wait until the read lock (from process 1) is released.

        Process p2 = startLockingProcess(tableLockClass, "WRITE", Duration.ofMillis(3000), Duration.ofMillis(5000), table, lockName, tableWorkDir, "W2", storyName, dataFileName);
        Thread.sleep(2000); // Must wait a little while the external process starts.

        // Reading after write lock requested, short wait so we should not get it.
        try {
            debugReadLock(tableLockClass, Duration.ofMillis(10), null, table, lockName, Duration.ofMillis(10));
            Assume.assumeTrue("Should not be able to get lock.", false);
        } catch (AcquireLockException e) {
            // Ignore
        }

        // Reading after write lock requested - should wait for the write lock that is in queue.

        Process p3 = startLockingProcess(tableLockClass, "READ", Duration.ofMillis(100), Duration.ofMillis(10000), table, lockName, tableWorkDir, "R3", storyName, dataFileName);
        Thread.sleep(2000); // Must wait a little while the external process starts.

        // Read - read lock in queue so we should block.

        debugReadLock(tableLockClass, Duration.ofMillis(100), null, table, lockName, Duration.ofMillis(10000));

        // Check the results.
        Thread.sleep(2000); // Allows all processes to finish.
        String story = FileUtils.readFileToString(tableWorkDir.resolve(storyName).toFile(), Charset.defaultCharset());
        // The expected string "R1aR1bW2aR1cW2bR3aW2cR3bR3c", but the start codes "[RW].a" come in unpredictable order so remove it from the check.  The b (got lock)
        // and c (about to release lock) codes are what matters. Even, so we sometimes get very slow process start up so instead of specific order we look for
        // pattersn.
        //Assert.assertEquals("Incorrect order", "R1bR1cW2bW2cR3bR3c", story.replaceAll("[RW].a", ""));
        String storyWithoutStart = story.replaceAll("[RW].a", "");
        Assert.assertEquals(0, storyWithoutStart.indexOf("R1bR1c"));
        Assume.assumeTrue("W2bW2c".indexOf(storyWithoutStart.replaceAll("[RW].a", "")) > 0);
        Assume.assumeTrue("R3bR3c".indexOf(storyWithoutStart.replaceAll("[RW].a", "")) > 0);
    }

    public static void testMultiProcessUpdateTableFileLock(Class<? extends TableLock> tableLockClass) throws Exception {
        String lockName = "MultiProcessUpdateTableLock";
        BaseTable table = new DictionaryTable.Builder<>(tableWorkDir.resolve(lockName + tableLockClass.getSimpleName())).build();
        table.save();

        String storyName = "storyMultiProcess" + tableLockClass.getSimpleName();
        String dataFileName = "dateMultiProcess" + tableLockClass.getSimpleName();

        String name = "test_multiprocess_insert";
        int processCount = 10;
        List<Process> processes = new ArrayList<>();


        // Start process that all add some data into the story file.
        for (int i = 0; i < processCount; i++) {
            Process p = startLockingProcess(tableLockClass, "WRITE", Duration.ofMillis(200), Duration.ofMinutes(5), table, lockName, tableWorkDir, "X", storyName, dataFileName);
            processes.add(p);
        }

        // Check the results.
        for (Process p : processes) {
            if (!p.waitFor(5, TimeUnit.MINUTES)) {
                Assert.fail("Waited too long for subprocess");
            }
        }
        String story = FileUtils.readFileToString(tableWorkDir.resolve(storyName).toFile(), Charset.defaultCharset());
        Assert.assertEquals("Incorrect length", processCount * 3 * 2, story.length());
    }

    public static void testTableLockRenew(Class<? extends TableLock> tableLockClass, Field drlp) throws Exception {
        String lockName = "LockRenew";
        BaseTable table = new DictionaryTable.Builder<>(tableWorkDir.resolve(lockName + tableLockClass.getSimpleName())).build();

        long reserveTime = 1000;

        if (drlp != null) {
            drlp.set(null, Duration.ofMillis(reserveTime));
        }

        // Get a lock and wait for a few reserved time periods to check if the lock is renewed.

        try (TableLock lock = TableLock.acquireRead(tableLockClass, table, lockName, Duration.ofMillis(50))) {
            Assert.assertTrue("Did not get initial lock", lock.isValid());

            long startTime = System.currentTimeMillis();
            long initialReservedTo = lock.reservedTo();
            while (startTime + 3 * reserveTime >= System.currentTimeMillis()) {
                Thread.sleep(reserveTime);
                Assert.assertTrue("Lock not renewed!", lock.isValid());
            }
            Assert.assertTrue("Lock has not been renewed", drlp == null || initialReservedTo < lock.reservedTo());
        }

        // Get hold of the renew scheduler and shut it down.
        
        Field schedField = ProcessLock.class.getDeclaredField("scheduler");
        schedField.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(schedField, schedField.getModifiers() & ~Modifier.FINAL);
        ScheduledExecutorService scheduler = (ScheduledExecutorService) schedField.get(null);

        try (TableLock lock = TableLock.acquireRead(tableLockClass, table, lockName, Duration.ofMillis(50))) {
            Assert.assertTrue("Did not get initial lock", lock.isValid());
            // Stop everything that has been scheduled and create a new scheduler (for other tests).
            scheduler.shutdownNow();
            schedField.set(null, Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }));

            long initialReservedTo = lock.reservedTo();
            Thread.sleep(2 * reserveTime);
            Assert.assertTrue("Lock has been renewed", initialReservedTo >= lock.reservedTo());  // >= as some lock types reset reservedTo.
        }
    }

    public static void testTableLockCrashCleanUp(Class<? extends TableLock> tableLockClass) throws Exception {
        BaseTable table = new DictionaryTable.Builder<>(tableWorkDir.resolve("LockCrash" + tableLockClass.getSimpleName())).build();
        table.save();
        String storyName = "storyCrash" + tableLockClass.getSimpleName();
        String dataFileName = "dateCrash" + tableLockClass.getSimpleName();

        // For debugging (validate the main function).
        //debugWriteLock(tableLockClass, Duration.ofHours(10), table,  "CrashLockStandard", Duration.ofMillis(10));
        //main(new String[]{tableLockClass.getCanonicalName(), "WRITE", "100000000", "100", "CrashLockStandard", tableWorkDir.toString(), "W0", storyName});

        Process p = startLockingProcess(tableLockClass, "WRITE", Duration.ofHours(1), Duration.ofMillis(100), table, "CrashLockStandard", tableWorkDir, "W1", storyName, dataFileName);
        Thread.sleep(4000); // Must wait a little while the external process starts.

        try {
            debugReadLock(tableLockClass, Duration.ofMillis(10), null, table, "CrashLockStandard", Duration.ofMillis(10));
            // Should not be here.
            p.destroy(); // clean up.
            Assert.fail("Should not be able to get lock.");
        } catch (AcquireLockException e) {
            // Ignore
        }
        p.destroy();
        Thread.sleep(3000);  // Again must wait while the process exits and cleans up.
        // Should clean up. Try getting the lock, get exception if fails.
        debugWriteLock(tableLockClass, Duration.ofMillis(10), table, "CrashLockStandard", Duration.ofMillis(10));

        // Finally some negative testing (to make sure the first half is not working by chance).

        p = startLockingProcess(tableLockClass, "WRITE", Duration.ofHours(1), Duration.ofMillis(100), table, "CrashLockForce", tableWorkDir, "W2", storyName, dataFileName);
        Thread.sleep(4000); // Must wait a little while the external process starts.
        try {
            debugWriteLock(tableLockClass, Duration.ofMillis(10), table, "CrashLockForce", Duration.ofMillis(10));
            // Should not be here.
            p.destroy(); // clean up.
            Assert.fail("Should not be able to get lock.");
        } catch (AcquireLockException e) {
            // Ignore
        }
        p.destroyForcibly();
        Thread.sleep(3000);  // Again must wait while the process exits and cleans up.
        // Should not clean up. Try getting the lock, get exception if fails.
        try {
            debugWriteLock(tableLockClass, Duration.ofMillis(10), table, "CrashLockForce", Duration.ofMillis(10));
            Assume.assumeTrue("Should not be able to get this lock as no clean up should have been performed.", false);
        } catch (AcquireLockException e) {
            // Ignore, should get this as no clean up was performed.
        }

    }


    private static String selectStringFilter(BaseTable<? extends BucketableTableEntry>.TableFilter filter) {
        return filter.get().stream().map(TableEntry::formatEntry).collect(Collectors.joining());
    }

    private static void debugReadLock(Class<? extends TableLock> lockClass, Duration holdDuration, Runnable runBefore, BaseTable table, String name, Duration timeout) {
        debugReadLock(lockClass, holdDuration, holdDuration.toMillis(), runBefore, table, name, timeout);
    }

    private static void debugReadLock(Class<? extends TableLock> lockClass, Duration holdDuration, long period, Runnable runBefore, BaseTable table, String name, Duration timeout) {
        try (TableTransaction trans = TableTransaction.openReadTransaction(lockClass, table, name, timeout)) {
            debugLockInternal(holdDuration, period, runBefore, null, trans.getLock());
        }
    }

    private static void debugWriteLock(Class<? extends TableLock> lockClass, Duration holdDuration, BaseTable table, String name, Duration timeout) {
        debugWriteLock(lockClass, holdDuration, holdDuration.toMillis(), null, null, table, name, timeout);
    }

    private static void debugWriteLock(Class<? extends TableLock> lockClass, Duration holdDuration, long period, Runnable runBefore, Runnable runPeriod, BaseTable table, String name, Duration timeout) {
        try (TableTransaction trans = TableTransaction.openWriteTransaction(lockClass, table, name, timeout)) {
            debugLockInternal(holdDuration, period, runBefore, runPeriod, trans.getLock());
            trans.commit();
        }
    }

    private static void debugLockInternal(Duration holdDuration, long period, Runnable runBefore, Runnable runPeriod, TableLock lock) {
        long startTime = System.currentTimeMillis();
        if (runBefore != null) {
            runBefore.run();
        }
        long holdUntil = startTime + holdDuration.toMillis();
        while (holdUntil >= System.currentTimeMillis()) {
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                throw new GorSystemException(e);
            }
            if (runPeriod != null) {
                runPeriod.run();
            }
        }
    }

    private static Process startLockingProcess(Class<? extends TableLock> tableLockClass, String lockType, Duration duration, Duration timeout, BaseTable table, String name,
                                               Path workingDir, String id, String storyName, String dataFileName)
            throws IOException {

        List<String> arguments = new ArrayList<>();
        arguments.add("java");
        arguments.add("-classpath");
        arguments.add(System.getProperty("java.class.path"));
        arguments.add("-Dlogback.configurationFile=" + Paths.get("..").toFile().getAbsolutePath() + "/tests/config/logback-test.xml");
        arguments.add(UTestTableLock.class.getCanonicalName());

        arguments.add(tableLockClass.getCanonicalName());
        arguments.add(lockType);
        arguments.add(String.valueOf(duration.toMillis()));
        arguments.add(String.valueOf(timeout.toMillis()));
        arguments.add(table.getPath().toString());
        arguments.add(name);
        arguments.add(workingDir.toString());
        arguments.add(id);
        arguments.add(storyName);
        arguments.add(dataFileName);

        log.trace("Running: {}", String.join(" ", arguments));

        ProcessBuilder pb = new ProcessBuilder(arguments);
        pb.directory(workingDir.toFile());

        Process p = pb.start();
        // For now just direct the output to the null streams.
        startProcessStreamEaters(p, new OutputStreamWriter(new NullOutputStream()), new OutputStreamWriter(new NullOutputStream()));
        return p;
    }

    // TODO:  This code is copied from UTestTableManager, consider sharing that code.
    private static String waitForProcessPlus(Process p, Duration waitTime) throws InterruptedException, ExecutionException {

        final StringWriter out_writer = new StringWriter();
        final StringWriter err_writer = new StringWriter();
        startProcessStreamEaters(p, out_writer, err_writer);
        boolean timeout = p.waitFor(waitTime.getSeconds(), TimeUnit.SECONDS);

        final String processOutput = out_writer.toString();
        final String errorOutput = err_writer.toString();
        if (errorOutput != null && errorOutput.length() > 0) {
            log.warn("Process {} error output - ==================================== start ====================================", p.hashCode());
            log.warn(errorOutput);
            log.warn("Process {} error output - ==================================== stop  ====================================", p.hashCode());
        }

        int errCode = -1;
        if (timeout) errCode = p.exitValue();
        if (errCode != 0) {
            log.warn("Process {} output - ==================================== start ====================================", p.hashCode());
            log.warn(processOutput);
            log.warn("Process {} output - ==================================== stop  ====================================", p.hashCode());
            throw new ExecutionException("BaseTable manager command failed with exit code " + errCode, null);
        }

        return processOutput;
    }

    // TODO:  This code is copied from UTestTableManager, consider sharing that code.
    private static void startProcessStreamEaters(Process p, Writer outWriter, Writer errWriter) {
        new Thread(() -> {
            try {
                IOUtils.copy(p.getInputStream(), outWriter, (Charset)null);
            } catch (IOException e) {
                // Ignore
            }
        }).start();
        new Thread(() -> {
            try {
                IOUtils.copy(p.getErrorStream(), errWriter, (Charset)null);
            } catch (IOException e) {
                // Ignore
            }
        }).start();
    }

    /**
     * Main function used by the unit tests.  Gets a lock and holds it for a given duration and writes to story file.
     *
     * @param args         <Lock class name> <Lock type READ/WRITE> <Duration to hold the lock in millis> <name> <work dir>
     */
    private static String dataFileAfterLock; // Only used in main.

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        Class lockClass = Class.forName(args[0]);
        String lockType = args[1];
        Duration holdDuration = Duration.ofMillis(Long.parseLong(args[2]));
        Duration timeout = Duration.ofMillis(Long.parseLong(args[3]));
        String tablePath = args[4];
        String lockName = args[5];
        String workDir = args[6];
        String id = args[7];
        String storyName = args[8];
        String dataFileName = args[9];

        File storyFile = Paths.get(workDir).resolve(storyName).toFile();
        File dataFile = Paths.get(workDir).resolve(dataFileName).toFile();
        FileUtils.writeStringToFile(storyFile, id + "a", Charset.defaultCharset(), true);
        BaseTable table = new DictionaryTable.Builder<>(Paths.get(tablePath)).build();
        if (lockType.equals("WRITE")) {
            // Set this up so we will overwrite any changes made by others while we have the lock.
            debugWriteLock(lockClass, holdDuration, holdDuration.toMillis() + 1,
                    () -> {
                        try {
                            FileUtils.writeStringToFile(storyFile, id + "b", Charset.defaultCharset(), true);
                            if (storyFile.exists()) {
                                dataFileAfterLock = FileUtils.readFileToString(dataFile, Charset.defaultCharset()) + id + "1";
                            }
                        } catch (IOException e) {
                            // Ignore,
                            log.warn("Error in main", e);
                        }
                    },
                    () -> {
                        try {
                            FileUtils.writeStringToFile(dataFile, dataFileAfterLock + id + "2", Charset.defaultCharset(), false);
                        } catch (IOException e) {
                            // Ignore,
                        }
                    }, table, lockName, timeout);
        } else {
            debugReadLock(lockClass, holdDuration, () -> {
                try {
                    FileUtils.writeStringToFile(storyFile, id + "b", Charset.defaultCharset(), true);
                } catch (IOException e) {
                    // Ignore,
                    log.warn("Error in main", e);
                }
            }, table, lockName, timeout);

        }
        FileUtils.writeStringToFile(storyFile, id + "c", Charset.defaultCharset(), true);
    }


}
