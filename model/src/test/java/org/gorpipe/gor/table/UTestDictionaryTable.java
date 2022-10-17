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

import gorsat.TestUtils;
import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.table.dictionary.*;
import org.gorpipe.gor.table.lock.ExclusiveFileTableLock;
import org.gorpipe.gor.table.util.GenomicRange;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.gorpipe.gor.table.dictionary.DictionaryTableMeta.HEADER_BUCKETIZE_KEY;


/**
 * Unit tests for gor table.
 * <p>
 * Created by gisli on 03/01/16.
 */
public class UTestDictionaryTable {

    private static final Class DEFAULT_TEST_LOCK_TYPE = ExclusiveFileTableLock.class;
    private static Path tableWorkDir;
    private static String gort1;
    private File genesSmall;

    @Before
    public void setUp() throws Exception {
        tableWorkDir = Files.createTempDirectory("UnitTestGorTableWorkDir");

        for (int i = 1; i < 25; i++) {
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
                "filepath17.gor\ttagB\n" +
                "filepath18.gor\t\t\t\t\t\ttagJ,tagM\n" +
                "filepath19.gor\ttagK\n";

        genesSmall = FileTestUtils.createGenericSmallGorFile(tableWorkDir.toFile());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(tableWorkDir.toFile());
    }

    @Test
    public void testTableCreation() {
        String tableName = "gortable_table_creation";
        Path gordFile = new File(tableWorkDir.toFile(), tableName + ".gord").toPath();

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).build();
        dict.save();

        Assert.assertEquals("Path check failed", gordFile.toAbsolutePath().toString(), dict.getPath());

        dict = new DictionaryTable.Builder<>(gordFile).build();
        Assert.assertEquals("Path check failed", gordFile.toAbsolutePath().toString(), dict.getPath());

        Assert.assertEquals(null, dict.getBooleanConfigTableProperty(HEADER_BUCKETIZE_KEY, null));
    }

    @Test
    public void testTableSaveLoad() throws IOException {
        String tableName = "gortable_table_load";
        Path gordFile = new File(tableWorkDir.toFile(), tableName + ".gord").toPath();
        FileUtils.write(gordFile.toFile(), gort1, (String) null);

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).build();
        dict.save();

        String savedContent = FileUtils.readFileToString(gordFile.toFile(), Charset.defaultCharset());
        Assert.assertEquals("Content not loaded or saved correctly", gort1, savedContent);
    }

    @Test
    public void testTableInternallHeader() {
        String tableName = "gortable_internal_header";
        Path gordFile = new File(tableWorkDir.toFile(), tableName + ".gord").toPath();

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).embeddedHeader(true).build();
        dict.setProperty("TestProp", "SomeValue");
        dict.save();

        Assert.assertEquals("Path check failed", gordFile.toAbsolutePath().toString(), dict.getPath());

        dict = new DictionaryTable.Builder<>(gordFile).embeddedHeader(true).build();
        dict.reload();
        Assert.assertEquals("Missing header property", "SomeValue", dict.getProperty("TestProp"));
    }

    @Test
    public void testTableExternalHeader() {
        String tableName = "gortable_external_header";
        Path gordFile = new File(tableWorkDir.toFile(), tableName + ".gord").toPath();

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).embeddedHeader(false).build();
        dict.setProperty("TestProp", "SomeValue");
        dict.save();

        Assert.assertEquals("Path check failed", gordFile.toAbsolutePath().toString(), dict.getPath());
        Assert.assertTrue("Should be external header", Files.exists(Path.of(dict.getPath().toString() + ".meta")));

        dict = new DictionaryTable.Builder<>(gordFile).embeddedHeader(false).build();
        dict.reload();
        Assert.assertEquals("Missing header property", "SomeValue", dict.getProperty("TestProp"));
    }

    @Test
    public void testRepeatedSaves() {
        String tableName = "gortable_repeated_saves";
        Path gordFile = new File(tableWorkDir.toFile(), tableName + ".gord").toPath();

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).embeddedHeader(false).build();
        dict.save();
        dict.save();

        DictionaryTable dict2 = new DictionaryTable.Builder<>(gordFile).embeddedHeader(false).build();
        dict2.reload();
        Assert.assertEquals("Dicts are differnt", dict.getEntries(), dict2.getEntries());
    }

    @Test
    public void testCreateDefineGetColumns() {
        // Add one file.
        String tableName = "gortable_create_define_columns";
        File gordFile = new File(tableWorkDir.toFile(), tableName + ".gord");
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();
        dict.save();

        Assert.assertTrue("BaseTable file was not created", gordFile.exists());

        String[] columnDef = new String[]{"Chrom", "Pos", "Alt", "PN", "COL1", "COL2"};
        dict.setColumns(columnDef);
        dict.save();
        
        Assert.assertArrayEquals("Columns def returned correctly", columnDef, dict.getColumns());

        DictionaryTable dict2 = new DictionaryTable(gordFile.getPath());
        Assert.assertArrayEquals("Columns def loaded correctly", columnDef, dict2.getColumns());
    }

    @Test
    public void testCreateSimple() {
        // Add one file.
        String tableName = "gortable_create_simple";
        File gordFile = new File(tableWorkDir.toFile(), tableName + ".gord");
        String dataFileName = Paths.get("../tests/data/gor/genes.gor").toAbsolutePath().toString();
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.insert(dataFileName);
        dict.save();
        
        Assert.assertTrue("BaseTable file was not created", gordFile.exists());
        Assert.assertFalse("Logging dir should not be created", new File(tableWorkDir.toFile(), tableName + ".log").exists());
        Assert.assertArrayEquals("Columns def not correct", new String[]{"Chrom", "gene_start", "gene_end", "Gene_Symbol"}, dict.getColumns());
    }

    @Test
    public void testCreateOptions() throws IOException {

        // Add one file.
        String tableName = "gortable_create_options";
        File gordFile = new File(tableWorkDir.toFile(), tableName + ".gord");
        String dataFileName = Paths.get("../tests/data/gor/genes.gor").toAbsolutePath().toString();
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toString()).sourceColumn("PNA").useHistory(true).embeddedHeader(true).build();

        dict.insert(dataFileName);
        dict.save();
        
        Assert.assertTrue("BaseTable file was not created", gordFile.exists());
        Assert.assertTrue("History folder was not created", new File(dict.getFolderPath(), BaseDictionaryTable.HISTORY_DIR_NAME).exists());
        Assert.assertEquals("Source column not set correctly", "PNA", dict.getSourceColumn());

        dataFileName = genesSmall.getCanonicalPath();
        dict.insert(dataFileName);
        dict.save();
        Assert.assertEquals("Incorrect number of entries in the history folder", 1, new File(dict.getFolderPath(), BaseDictionaryTable.HISTORY_DIR_NAME).list().length);

        // Test turn off history.
        String tableNameNoHist = "gortable_create_options_no_hist";
        File gordFileNoHist = new File(tableWorkDir.toFile(), tableNameNoHist + ".gord");
        DictionaryTable dictNoHist = new DictionaryTable.Builder<>(gordFileNoHist.toPath()).sourceColumn("PNA").useHistory(false).build();

        dictNoHist.insert(dataFileName);
        dictNoHist.save();
        Assert.assertTrue("BaseTable file was not created", gordFileNoHist.exists());
        Assert.assertTrue("History folder was created", !new File(dictNoHist.getFolderPath(), BaseDictionaryTable.HISTORY_DIR_NAME).exists());
    }

    // Test table operations (on unbucketized table).

    @Test
    public void testAddSingleLine() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_singleline.gord");
        Path gorFile =tableWorkDir.resolve("gor1.gor");
        FileUtils.write(gorFile.toFile(), "chrom\tpos\tcol1\nchr1\t1\tpn1gor\n", "UTF-8");
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();
        dict.setLineFilter(false);
        dict.insert("gor1.gor");
        dict.save();
        Assert.assertEquals(TestUtils.runGorPipe(gordFile.getCanonicalPath()), TestUtils.runGorPipe(gorFile.toString()));
    }

    @Test
    public void testAddRelativePath() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_testrelativefilepath.gord");
        String dataFileName = Paths.get("../tests/data/gor/genes.gor").toAbsolutePath().toString();
        String insertFileName = "X/genes.gor";

        Files.createDirectories(tableWorkDir.resolve("X"));
        Files.copy(Paths.get(dataFileName), tableWorkDir.resolve(insertFileName));

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();
        dict.insert(insertFileName);
        dict.setLineFilter(false);
        dict.save();
        Assert.assertEquals(TestUtils.runGorPipe(gordFile.getCanonicalPath()), TestUtils.runGorPipe(dataFileName));
    }

    @Test
    public void testAddMoreThanOneLine() throws Exception {
        // Add two file.
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_morethanone.gord");
        String[] addFileNames = new String[]{
                Paths.get("../tests/data/gor/genes.gor").toAbsolutePath().toString(),
                genesSmall.getCanonicalPath()
        };
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.insert(Arrays.stream(addFileNames).map(fn -> new DictionaryEntry.Builder(fn, dict.getRootUri()).build()).toArray(DictionaryEntry[]::new));
        dict.setLineFilter(false);
        dict.save();
        
        Assert.assertEquals(TestUtils.runGorPipe(String.join(" ", Arrays.asList(addFileNames))), TestUtils.runGorPipe(gordFile.getCanonicalPath()));
    }

    @Test
    public void testAddSameFileTwice() throws Exception {
        // Add two file.
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_same_file_twice.gord");

        Path file = Paths.get("../tests/data/gor/genes.gor").toAbsolutePath();
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.insert((DictionaryEntry)new DictionaryEntry.Builder(file, dict.getRootUri()).alias("A").build());
        dict.insert((DictionaryEntry)new DictionaryEntry.Builder(file, dict.getRootUri()).alias("B").build());
        dict.setLineFilter(false);
        dict.save();
        
        Assert.assertEquals("File should be added twice", 2, dict.filter().files(file.toString()).get().size());
        Assert.assertEquals(TestUtils.runGorPipe(file.toString() + " " + file.toString()), TestUtils.runGorPipe(gordFile.getCanonicalPath()));
    }

    @Test
    public void testAddWithPartialRange() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_with_partial_range.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.insert((DictionaryEntry)new DictionaryEntry.Builder<>(Paths.get("filepath20.gor"), dict.getRootUri()).range(GenomicRange.parseGenomicRange("chr8")).build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath20.gor").includeDeleted());
        Assert.assertEquals("Line with partial range inserted incorrectly", "filepath20.gor\t\tchr8\t0\tchr8\n", selectRes);
    }

    @Test
    public void testAddExistingLineFileMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_existing_matchfile.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.insert("filepath1.gor");
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath1.gor").includeDeleted());
        Assert.assertEquals("Existing line should just be updated", "filepath1.gor\n", selectRes);
    }

    @Test
    public void testAddExistingLineTagMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_existing_matchtag.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.insert((DictionaryEntry)new DictionaryEntry.Builder<>(Paths.get("filepath3.gor"), dict.getRootUri()).alias("tagB").build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath3.gor").includeDeleted());
        Assert.assertEquals("Existing line should just be updated", "filepath3.gor\ttagB\n", selectRes);
    }

    @Test
    public void testAddExistingLineRangeMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_existing_matchrange.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.insert((DictionaryEntry)new DictionaryEntry.Builder<>(Paths.get("filepath5.gor"), dict.getRootUri()).range(GenomicRange.parseGenomicRange("chr1:10000-chr1:20000")).alias("tagF").build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath5.gor").includeDeleted());
        Assert.assertEquals("Existing line should just be updated", "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\n", selectRes);
    }

    @Test
    public void testAddTagOnExisting() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_existing_addtag.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.delete(dict.filter().files("filepath17.gor").includeDeleted().get());
        dict.insert(new DictionaryEntry.Builder<>(Paths.get("filepath17.gor"), dict.getRootUri()).tags(new String[]{"tagB", "tagD"}).build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath17.gor").includeDeleted());
        Assert.assertEquals("Existing line should just be updated", "filepath17.gor\t\t\t\t\t\ttagB,tagD\n", selectRes);
    }

    @Test
    public void testRemoveTagOnExisting() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_existing_removetag.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.delete(dict.filter().files("filepath4.gor").includeDeleted().get());
        dict.insert(new DictionaryEntry.Builder<>(Paths.get("filepath4.gor"), dict.getRootUri()).range(GenomicRange.parseGenomicRange("chr1:10000-chr1:30000")).alias("tagD").build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath4.gor").includeDeleted());
        Assert.assertEquals("Existing line should just be updated", "filepath4.gor\ttagD\tchr1\t10000\tchr1\t30000\n", selectRes);
    }

    @Test
    public void testAddExistingBucketized() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_existing_deletedbucket.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.insert(new DictionaryEntry.Builder<>(Paths.get("filepath13.gor"), dict.getRootUri()).build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath13.gor").includeDeleted());
        Assert.assertEquals("Bucketized files should not keep their bucket info", "filepath13.gor\nfilepath13.gor|D|bucket2\n", selectRes);
    }

    @Test
    public void testAddExistingDeletedBucketized() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_add_existing_deletedbucket.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();
        dict.setValidateFiles(false);

        String selectRes = selectStringFilter(dict, dict.filter().files("filepath15.gor").includeDeleted());
        Assert.assertEquals("Deleted file should be included if option: include_deleted", "filepath15.gor|D|bucket2\n", selectRes);

        dict.insert("filepath15.gor");
        selectRes = selectStringFilter(dict, dict.filter().files("filepath15.gor").includeDeleted());
        Assert.assertEquals("Deleted file should not be reinstated", "filepath15.gor\nfilepath15.gor|D|bucket2\n", selectRes);

        // Add to new bucket and then delete again.
        dict.addToBucket("bucket3", dict.filter().files("filepath15.gor").get());
        dict.delete(dict.filter().files("filepath15.gor").get());

        dict.insert("filepath15.gor");
        selectRes = selectStringFilter(dict, dict.filter().files("filepath15.gor"));
        Assert.assertEquals("Deleted file should not be reinstated", "filepath15.gor\n", selectRes);
        selectRes = selectStringFilter(dict, dict.filter().files("filepath15.gor").includeDeleted());
        Assert.assertEquals("Deleted file should not be reinstated", "filepath15.gor\nfilepath15.gor|D|bucket2\nfilepath15.gor|D|bucket3\n", selectRes);
    }

    @Test
    public void testAddLineColumnMismatch() {
        String tableName = "gortable_addlinecolumnmismatch";
        File gordFile = new File(tableWorkDir.toFile(), tableName + ".gord");
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        String[] columnDef = new String[]{"Chrom", "Pos", "Alt", "PN", "COL1", "COL2"};
        dict.setColumns(columnDef);

        // Add one file.
        String dataFileName = Paths.get("../tests/data/gor/genes.gor").toAbsolutePath().toString();
        try {
            dict.insert(dataFileName);
            Assert.fail("Should not be able ot insert file with different columns def.");
        } catch (Exception ex) {
            // Ignrore, success.
        }
    }

    @Test
    public void testDeletLineFileMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_delete_filematch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toPath()).build();

        dict.delete(dict.filter().files("filepath1.gor", "filepath8.gor").get());
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath1.gor", "filepath8.gor").includeDeleted());
        Assert.assertEquals("Files should be deleted", "", selectRes);
    }

    @Test
    public void testDeletLineTagMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_delete_tagmatch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.delete(dict.filter().tags("tagA", "tagF").get());
        String selectRes = selectStringFilter(dict, dict.filter().tags("tagA", "tagF").includeDeleted());
        Assert.assertEquals("Files should be deleted", "", selectRes);
    }

    @Test
    public void testDeletLineTagMatchSubset() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_delete_tagmatchsubset.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.delete(dict.filter().tags("tagD").get());
        String selectRes = selectStringFilter(dict, dict.filter().tags("tagD").includeDeleted());
        Assert.assertEquals("Files should be deleted", "", selectRes);
    }

    @Test
    public void testDeletLineExactMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_delete_exactmatch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.delete(dict.filter().matchAllTags("tagJ", "tagK").get());
        String selectRes = selectStringFilter(dict, dict.filter().tags("tagJ"));
        Assert.assertEquals("Only exact match should be deleted",
                "filepath18.gor\t\t\t\t\t\ttagJ,tagM\n", selectRes);
    }

    @Test
    public void testDeletLineBucketized() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_delete_bucketized.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.delete(new DictionaryEntry.Builder("filepath13.gor", dict.getRootUri()).build());
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath13.gor").includeDeleted());
        Assert.assertEquals("Delete bucketized failed", "filepath13.gor|D|bucket2\n", selectRes);

        Files.createFile(tableWorkDir.resolve("filepath14B.gor"));
        dict.insert((DictionaryEntry)new DictionaryEntry.Builder(Paths.get("filepath14B.gor"), dict.getRootUri()).alias("tagL").build());
        selectRes = selectStringFilter(dict, dict.filter().tags("tagL").includeDeleted());
        Assert.assertEquals("Insert same tag as deleted failed", "filepath14.gor|D|bucket2\ttagL\nfilepath14B.gor\ttagL\n", selectRes);
    }

    @Test
    public void testSelectLineMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_select_filematch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().files("filepath1.gor", "filepath8.gor"));
        Assert.assertEquals("Select failed", "filepath1.gor\n" + "filepath8.gor\ttagA\n", selectRes);
    }

    @Test
    public void testSelectTagMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_select_tagmatch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().tags("tagA", "tagF"));
        Assert.assertEquals("Select failed",
                "filepath2.gor\ttagA\n" +
                        "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\n" +
                        "filepath6.gor\ttagF\tchr1\t30000\tchr2\t10000\n" +
                        "filepath8.gor\ttagA\n", selectRes);

        selectRes = selectStringFilter(dict, dict.filter().tags("tagD", "tagF2"));
        Assert.assertEquals("Select failed",
                "filepath16.gor\ttagD\n" +
                        "filepath4.gor\t\tchr1\t10000\tchr1\t30000\ttagD,tagE\n" +
                        "filepath7.gor\t\tchr3\t10000\tchr4\t10000\ttagF1,tagF2\n"
                , selectRes);


        selectRes = selectStringFilter(dict, dict.filter().tags("tagA", "tagA"));
        Assert.assertEquals("Select failed, should only get lines once.",
                "filepath2.gor\ttagA\n" +
                        "filepath8.gor\ttagA\n", selectRes);
    }

    @Test
    public void testSelectAliasMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_select_tagmatch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().tags("tagA", "tagF"));
        Assert.assertEquals("Select failed",
                "filepath2.gor\ttagA\n" +
                        "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\n" +
                        "filepath6.gor\ttagF\tchr1\t30000\tchr2\t10000\n" +
                        "filepath8.gor\ttagA\n", selectRes);
    }

    @Test
    public void testSelectBucketMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_select_bucketmatch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().buckets("bucket2"));
        Assert.assertEquals("Select failed",
                "filepath11.gor|bucket2\ttagI\n" +
                        "filepath12.gor|bucket2\t\tchr1\t1\tchr2\t20000\ttagJ,tagK\n" +
                        "filepath13.gor|bucket2\n" +
                        "filepath14.gor|D|bucket2\ttagL\n" +
                        "filepath15.gor|D|bucket2\n", selectRes);
    }

    @Test
    public void testSelectRangeMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_select_rangematch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().chrRange("chr1:10000-chr1:20000"));
        Assert.assertEquals("Select range with gor format failed",
                "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\n", selectRes);

        selectRes = selectStringFilter(dict, dict.filter().chrRange("chr1:10000-20000"));
        Assert.assertEquals("Select range with spaces failed",
                "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\n", selectRes);
    }

    @Test
    public void testSelectTagOrRangeMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_select_tagorrangematch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().tags("tagD"), dict.filter().chrRange("chr1:10000-chr1:20000"));
        Assert.assertEquals("Select failed",
                "filepath16.gor\ttagD\n" +
                        "filepath4.gor\t\tchr1\t10000\tchr1\t30000\ttagD,tagE\n" +
                        "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\n"
                , selectRes);
    }

    @Test
    public void testSelectExactMatch() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_select_exactmatch.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().tags("tagF").chrRange("chr1:10000-chr1:20000"));
        Assert.assertEquals("Select failed", "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\n", selectRes);
    }

    @Test
    public void testNeedBucketizing() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_addtobucket_file.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        String result = dict.needsBucketizing().stream().map(DictionaryEntry::formatEntry).collect(Collectors.joining());
        Assert.assertEquals("Need bucketizing incorrect",
                "filepath1.gor\n" +
                        "filepath2.gor\ttagA\n" +
                        "filepath3.gor\ttagB\n" +
                        "filepath4.gor\t\tchr1\t10000\tchr1\t30000\ttagD,tagE\n" +
                        "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\n" +
                        "filepath6.gor\ttagF\tchr1\t30000\tchr2\t10000\n" +
                        "filepath7.gor\t\tchr3\t10000\tchr4\t10000\ttagF1,tagF2\n" +
                        "filepath8.gor\ttagA\n" +
                        "filepath16.gor\ttagD\n" +
                        "filepath17.gor\ttagB\n" +
                        "filepath18.gor\t\t\t\t\t\ttagJ,tagM\n" +
                        "filepath19.gor\ttagK\n", result);
    }

    @Test
    public void testAddToBucketFile() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_addtobucket_file.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.addToBucket("newbucket", dict.filter().files("filepath3.gor").get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("newbucket"));
        Assert.assertEquals("Add to bucket incorrect",
                "filepath3.gor|newbucket\ttagB\n", selectRes);
    }

    @Test
    public void testAddToBucketTags() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_addtobucket_tags.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.addToBucket("newbucket", dict.filter().tags("tagA", "tagB").get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("newbucket"));
        Assert.assertEquals("Add to bucket incorrect",
                "filepath17.gor|newbucket\ttagB\n" +
                        "filepath2.gor|newbucket\ttagA\n" +
                        "filepath3.gor|newbucket\ttagB\n" +
                        "filepath8.gor|newbucket\ttagA\n", selectRes);
    }

    @Test
    public void testAddToBucketExisting() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_addtobucket_tags.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        try {
            dict.addToBucket("newbucket", dict.filter().tags("tagG").get());
            Assert.fail("Should not be able to set bucket on line already with a bucket");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testDeleteFromBucketFile() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_deletefrombucket_file.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.removeFromBucket(dict.filter().files("filepath13.gor").get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("bucket2"));
        Assert.assertEquals("Delete from bucket incorrect",
                "filepath11.gor|bucket2\ttagI\n" +
                        "filepath12.gor|bucket2\t\tchr1\t1\tchr2\t20000\ttagJ,tagK\n" +
                        "filepath14.gor|D|bucket2\ttagL\n" +
                        "filepath15.gor|D|bucket2\n", selectRes);

    }

    @Test
    public void testDeleteFromBucketTags() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_deletefrombucket_tags.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.removeFromBucket(dict.filter().tags("tagJ", "tagL").includeDeleted().get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("bucket2"));
        Assert.assertEquals("Delete from bucket incorrect",
                "filepath11.gor|bucket2\ttagI\n" +
                        "filepath13.gor|bucket2\n" +
                        "filepath15.gor|D|bucket2\n", selectRes);

        selectRes = selectStringFilter(dict, dict.filter().files("filepath14.gor"));
        Assert.assertEquals("Delete from bucket should remove filepath14.gor form table", "", selectRes);
    }

    @Test
    public void testDeleteFromBucketDeleted() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_deletefrombucket_deleted.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.removeFromBucket(dict.filter().files("filepath15.gor").get());
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath15.gor"));
        Assert.assertEquals("Delete from bucket should remove filepath15.gor form table", "", selectRes);
    }

    @Test
    public void testRemoveFromBucket() throws Exception {
        File gordFile = new File(tableWorkDir.toFile(), "gortable_deletebucket.gord");
        FileUtils.write(gordFile, gort1, (String) null);
        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        dict.removeFromBucket(dict.filter().buckets("bucket2").get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("bucket2"));
        Assert.assertEquals("Delete bucket incorrect", "", selectRes);
    }

    @Test
    public void testQueryOnReAddedFile() throws Exception {
        String testName = "testQueryOnReAddedFile";
        File gordFile = new File(tableWorkDir.toFile(), testName + ".gord");
        File pn1gor = new File(tableWorkDir.toFile(), testName + "_pn1.gor");
        File pn2gor = new File(tableWorkDir.toFile(), testName + "_pn2.gor");
        File bucket1gor = new File(tableWorkDir.toFile(), testName + "_bucket1.gor");

        // Setup data.
        FileUtils.write(pn1gor, "chrom\tpos\tcol1\nchr1\t1\tpn1gor\n", "UTF-8");
        FileUtils.write(pn2gor, "chrom\tpos\tcol1\nchr2\t2\tpn2gor\n", "UTF-8");

        String bucketData = "chrom\tpos\tcol1\tSource\n" +
                "chr1\t1\tpn1gor\tpn1\n" +
                "chr2\t2\tpn2gor\tpn2\n" +
                "chr3\t3\tpn3gor\tpn3\n" +
                "chr4\t4\tpn4gor\tpn4\n" +
                "chr5\t5\tpn5gor\tpn5\n" +
                "chr6\t6\tpn6gor\tpn6\n" +
                "chr7\t7\tpn7gor\tpn7\n" +
                "chr8\t8\tpn8gor\tpn8\n" +
                "chr9\t9\tpn9gor\tpn9\n";
        FileUtils.write(bucket1gor, bucketData, "UTF-8");

        FileUtils.write(gordFile,
                testName + "_pn1.gor|D|" + testName + "_bucket1.gor\tpn1\n"
                        + testName + "_pn2.gor|" + testName + "_bucket1.gor\tpn2\n"
                        + testName + "_pn3.gor|" + testName + "_bucket1.gor\tpn3\n"
                        + testName + "_pn4.gor|" + testName + "_bucket1.gor\tpn4\n"
                        + testName + "_pn5.gor|" + testName + "_bucket1.gor\tpn5\n"
                        + testName + "_pn6.gor|" + testName + "_bucket1.gor\tpn6\n"
                        + testName + "_pn7.gor|" + testName + "_bucket1.gor\tpn7\n"
                        + testName + "_pn8.gor|" + testName + "_bucket1.gor\tpn8\n"
                        + testName + "_pn9.gor|" + testName + "_bucket1.gor\tpn9\n"
                        + testName + "_pn1.gor\tpn1\n", "UTF-8");

        // Do testing
        Assert.assertEquals("No filter fails", bucketData, TestUtils.runGorPipe(gordFile.getAbsolutePath() + " "));

        Assert.assertEquals("All filter fails", bucketData, TestUtils.runGorPipe(gordFile.getAbsolutePath() + " -f pn1,pn2,pn3,pn4,pn5,pn6,pn7,pn8,pn9"));

        Assert.assertEquals("Partial filter fails",
                "chrom\tpos\tcol1\tSource\n" +
                "chr1\t1\tpn1gor\tpn1\n" +
                "chr2\t2\tpn2gor\tpn2\n" , TestUtils.runGorPipe(gordFile.getAbsolutePath() + " -f pn1,pn2"));

        Assert.assertEquals("Filter deleted fails", "chr1\t1\tpn1gor\tpn1", TestUtils.runGorPipeNoHeader(gordFile.getAbsolutePath() + " -f pn1").trim());

        Assert.assertEquals("Filter not deleted fails", "chr2\t2\tpn2gor\tpn2", TestUtils.runGorPipeNoHeader(gordFile.getAbsolutePath() + " -f pn2").trim());
    }

    @Test
    public void testRewriteOfManuallyWrittenGords() throws IOException {
        String testName = "testQueryOnReAddedFile";
        File gordFile = new File(tableWorkDir.toFile(), testName + ".gord");
        FileUtils.writeStringToFile(gordFile, "./file1.gor\tpn1\n", Charset.defaultCharset());
        Assert.assertEquals("./file1.gor\tpn1\n", FileUtils.readFileToString(gordFile, Charset.defaultCharset()));

        List<String> files = new ArrayList();
        List<String> tags = new ArrayList<>();
        files.add("file1.gor");
        tags.add("pn1");

        DictionaryTable dict = new DictionaryTable(gordFile.toPath());
        dict.setValidateFiles(false);
        dict.insert("file2.gor\tpn2\n");
        dict.save();

        Assert.assertEquals("file1.gor\tpn1\nfile2.gor\tpn2\n", FileUtils.readFileToString(gordFile, Charset.defaultCharset()));
    }

    @SafeVarargs
    private final String selectStringFilter(DictionaryTable table, TableFilter<DictionaryEntry>... filters) {
        return table.selectUninon(filters).stream().map(DictionaryEntry::formatEntry).sorted().collect(Collectors.joining());
    }
}
