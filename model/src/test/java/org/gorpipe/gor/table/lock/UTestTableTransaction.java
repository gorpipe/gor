package org.gorpipe.gor.table.lock;

import gorsat.TestUtils;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.gorpipe.test.GorDictionarySetup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class UTestTableTransaction {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(UTestTableTransaction.class);

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    private GorDictionaryTable testTable;

    private int NUMBER_OF_FILES = 3;
    private String NAME = "test_table";

    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();



        Path dataDir = workDirPath.resolve("data");
        Files.createDirectory(dataDir);
        String[] sources = IntStream.range(1, NUMBER_OF_FILES + 1).mapToObj(i -> String.format("PN%d", i)).toArray(String[]::new);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                NAME, dataDir, NUMBER_OF_FILES, new int[]{1, 2, 3}, 10, "PN", true, sources);
        testTable = TestUtils.createDictionaryWithData(NAME, workDirPath, dataFiles);
    }

    @Test
    public void testOpenReadTransaction() {
        try (TableTransaction trans = TableTransaction.openReadTransaction(ExclusiveFileTableLock.class, testTable, testTable.getName(),
                Duration.of(10, ChronoUnit.SECONDS))) {
            Assert.assertEquals(NUMBER_OF_FILES, testTable.getEntries().size());
            trans.commit();
        }
    }

    @Test
    public void testOpenWriteTransaction() throws IOException {
        try (TableTransaction trans = TableTransaction.openWriteTransaction(ExclusiveFileTableLock.class, testTable, testTable.getName(),
                Duration.of(10, ChronoUnit.SECONDS))) {
            testTable.insert("PN4\tfile_new.gor");
            trans.commit();
        }
        Assert.assertEquals(NUMBER_OF_FILES + 1, testTable.getEntries().size());
        Assert.assertEquals(NUMBER_OF_FILES + 1, Files.readAllLines(Path.of(testTable.getPath())).size());
    }

    @Test
    public void testTransactionSetupFailure() throws IOException {
        Path lockPath = null;
        GorDictionaryTable nonExistingTable = new GorDictionaryTable(workDirPath.resolve("stuff").resolve("ubs.gord"));
        try (TableTransaction trans = TableTransaction.openWriteTransaction(ExclusiveFileTableCentralLock.class, nonExistingTable, nonExistingTable.getName(),
                Duration.of(1, ChronoUnit.SECONDS))) {
            lockPath = ((ExclusiveFileTableCentralLock)trans.getLock()).getLockPath();
            trans.commit();
        } catch (Exception e) {
            Assert.assertTrue(lockPath == null || !Files.exists(lockPath));
        } finally {
            if (lockPath != null) {
                Files.deleteIfExists(lockPath);
            }
        }
    }
}
