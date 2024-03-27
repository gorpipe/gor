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
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryFilter;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;

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
public class UTestNorDictionaryTable {

    private static Path tableWorkDir;
    private static String nort1;
    private File genesSmall;

    @Before
    public void setUp() throws Exception {
        tableWorkDir = Files.createTempDirectory("UnitTestnortableWorkDir");

        for (int i = 1; i < 25; i++) {
            Files.createFile(tableWorkDir.resolve(String.format("filepath%d.nor", i)));
        }

        nort1 = "filepath1.nor\n" +
                "filepath2.nor\ttagA\n" +
                "filepath3.nor\ttagB\n" +
                "filepath4.nor\t\ttagD,tagE\n" +
                "filepath5.nor\ttagF\t\n" +
                "filepath6.nor\ttagF\t\n" +
                "filepath7.nor\t\ttagF1,tagF2\n" +
                "filepath8.nor\ttagA\n" +
                "filepath9.nor|bucket1\ttagG\n" +
                "filepath10.nor|bucket1\ttagH\n" +
                "filepath11.nor|bucket2\ttagI\n" +
                "filepath12.nor|bucket2\t\ttagJ,tagK\n" +
                "filepath13.nor|bucket2\n" +
                "filepath14.nor|D|bucket2\ttagL\n" +
                "filepath15.nor|D|bucket2\n" +
                "filepath16.nor\ttagD\n" +
                "filepath17.nor\ttagB\n" +
                "filepath18.nor\t\ttagJ,tagM\n" +
                "filepath19.nor\ttagK\n";

        genesSmall = FileTestUtils.createGenericSmallGorFile(tableWorkDir.toFile());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(tableWorkDir.toFile());
    }

    @Test
    public void testNorTableCreation() {
        String tableName = "nortable_table_creation";
        Path nordFile = new File(tableWorkDir.toFile(), tableName + ".nord").toPath();

        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile).build();
        dict.save();

        Assert.assertEquals("Path check failed", nordFile.toAbsolutePath().toString(), dict.getPath());

        dict = new NorDictionaryTable.Builder<>(nordFile).build();
        Assert.assertEquals("Path check failed", nordFile.toAbsolutePath().toString(), dict.getPath());

        Assert.assertEquals(null, dict.getBooleanConfigTableProperty(HEADER_BUCKETIZE_KEY, null));
    }

    @Test
    public void testTableSaveLoad() throws IOException {
        String tableName = "nortable_table_load";
        Path nordFile = new File(tableWorkDir.toFile(), tableName + ".nord").toPath();
        FileUtils.write(nordFile.toFile(), nort1, (String) null);

        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile).build();
        dict.save();

        String savedContent = FileUtils.readFileToString(nordFile.toFile(), Charset.defaultCharset());
        Assert.assertEquals("Content not loaded or saved correctly", trimLines(nort1), trimLines(savedContent));
    }

    @Test
    public void testRepeatedSaves() {
        String tableName = "nortable_repeated_saves";
        Path nordFile = new File(tableWorkDir.toFile(), tableName + ".nord").toPath();

        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile).embeddedHeader(false).build();
        dict.save();
        dict.save();

        NorDictionaryTable dict2 = new NorDictionaryTable.Builder<>(nordFile).embeddedHeader(false).build();
        dict2.reload();
        Assert.assertEquals("Dicts are differnt", dict.getEntries(), dict2.getEntries());
    }

    @Test
    public void testCreateDefineGetColumns() {
        // Add one file.
        String tableName = "nortable_create_define_columns";
        File nordFile = new File(tableWorkDir.toFile(), tableName + ".nord");
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();
        dict.save();

        Assert.assertTrue("BaseTable file was not created", nordFile.exists());

        String[] columnDef = new String[]{"Chrom", "Pos", "Alt", "PN", "COL1", "COL2"};
        dict.setColumns(columnDef);
        dict.save();

        Assert.assertArrayEquals("Columns def returned correctly",
                columnDef,
                dict.getColumns());

        NorDictionaryTable dict2 = new NorDictionaryTable(nordFile.getPath());
        Assert.assertArrayEquals("Columns def loaded correctly",
                columnDef,
                dict.getColumns());
    }

    @Test
    public void testCreateSimple() {
        // Add one file.
        String tableName = "nortable_create_simple";
        File nordFile = new File(tableWorkDir.toFile(), tableName + ".nord");
        String dataFileName = Paths.get("../tests/data/nor/simple.nor").toAbsolutePath().toString();
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        dict.insert(dataFileName);
        dict.save();

        Assert.assertTrue("BaseTable file was not created", nordFile.exists());
        Assert.assertFalse("Logging dir should not be created", new File(tableWorkDir.toFile(), tableName + ".log").exists());
        Assert.assertArrayEquals("Columns def not correct", new String[]{"Chrom","gene_start","gene_end","Gene_Symbol"}, dict.getColumns());
    }
    
    // Test table operations (on unbucketized table).

    @Ignore("We have two iterators inferring the header and the treat the first line (without #) differently")
    @Test
    public void testAddSingleLine() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_singleline.nord");
        Path norFile =tableWorkDir.resolve("nor1.nor");
        FileUtils.write(norFile.toFile(), "col1\tpn1gor\n", "UTF-8");
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();
        dict.setLineFilter(false);
        dict.insert("nor1.nor");
        dict.save();
        Assert.assertEquals(
                TestUtils.runGorPipe("nor " + nordFile.getCanonicalPath()),
                TestUtils.runGorPipe("nor " + norFile.toString()));
    }

    @Test
    public void testAddRelativePath() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_testrelativefilepath.nord");
        String dataFileName = Paths.get("../tests/data/gor/genes.gor").toAbsolutePath().toString();
        String insertFileName = "X/genes.nor";

        Files.createDirectories(tableWorkDir.resolve("X"));
        Files.copy(Paths.get(dataFileName), tableWorkDir.resolve(insertFileName));

        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();
        dict.insert(insertFileName);
        dict.setLineFilter(false);
        dict.save();
        Assert.assertEquals(
                TestUtils.runGorPipe("nor " + dataFileName + " | top 10"),
                TestUtils.runGorPipe("nor " + nordFile.getCanonicalPath() + " | top 10"));
    }

    @Test
    public void testAddMoreThanOneLine() throws Exception {
        // Add two file.
        File nordFile = new File(tableWorkDir.toFile(), "nortable_add_morethanone.nord");
        String[] addFileNames = new String[]{
                Paths.get("../tests/data/gor/genes.gor").toAbsolutePath().toString(),
                genesSmall.getCanonicalPath()
        };
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        dict.insert(Arrays.stream(addFileNames).map(fn -> new DictionaryEntry.Builder(fn, dict.getRootPath()).build()).toArray(DictionaryEntry[]::new));
        dict.setLineFilter(false);
        dict.save();

        Assert.assertEquals(TestUtils.runGorPipe("nor " + addFileNames[0]).split("\n").length
                        + TestUtils.runGorPipe("nor " + addFileNames[1]).split("\n").length - 1,
                TestUtils.runGorPipe("nor " + nordFile.getCanonicalPath()).split("\n").length);
    }

    @Test
    public void testAddSameFileTwice() throws Exception {
        // Add two file.
        File nordFile = new File(tableWorkDir.toFile(), "nortable_add_same_file_twice.nord");

        Path file = Paths.get("../tests/data/nor/simple.nor").toAbsolutePath();
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        dict.insert(new DictionaryEntry.Builder(file.toString(), dict.getRootPath()).alias("A").build());
        dict.insert(new DictionaryEntry.Builder(file.toString(), dict.getRootPath()).alias("B").build());
        dict.setLineFilter(false);
        dict.save();

        Assert.assertEquals("File should be added twice", 2, dict.filter().files(file.toString()).get().size());
        Assert.assertEquals(TestUtils.runGorPipe("nor " + file).split("\n").length * 2 - 1,
                TestUtils.runGorPipe("nor " + nordFile.getCanonicalPath()).split("\n").length);
    }
    
    @Test
    public void testAddExistingLineFileMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_add_existing_matchfile.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        dict.insert("filepath1.nor");
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath1.nor").includeDeleted());
        Assert.assertEquals("Existing line should just be updated", "filepath1.nor\t\t\n", selectRes);
    }

    @Test
    public void testAddExistingLineTagMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_add_existing_matchtag.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        dict.insert((DictionaryEntry)new DictionaryEntry.Builder<>("filepath3.nor", dict.getRootPath()).alias("tagB").build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath3.nor").includeDeleted());
        Assert.assertEquals("Existing line should just be updated", "filepath3.nor\ttagB\t\n", selectRes);
    }



    @Test
    public void testAddTagOnExisting() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_add_existing_addtag.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        dict.delete(dict.filter().files("filepath17.nor").includeDeleted().get());
        dict.insert(new DictionaryEntry.Builder<>("filepath17.nor", dict.getRootPath()).tags(new String[]{"tagB", "tagD"}).build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath17.nor").includeDeleted());
        Assert.assertEquals("Existing line should just be updated", "filepath17.nor\t\ttagB,tagD\n", selectRes);
    }

    @Test
    public void testRemoveTagOnExisting() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_add_existing_removetag.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        dict.delete(dict.filter().files("filepath4.nor").includeDeleted().get());
        dict.insert(new DictionaryEntry.Builder<>("filepath4.nor", dict.getRootPath()).alias("tagD").build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath4.nor").includeDeleted());
        Assert.assertEquals("Existing line should just be updated", "filepath4.nor\ttagD\t\n", selectRes);
    }

    @Test
    public void testAddExistingBucketized() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_add_existing_deletedbucket.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        dict.insert(new DictionaryEntry.Builder<>("filepath13.nor", dict.getRootPath()).build());
        dict.save();
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath13.nor").includeDeleted());
        Assert.assertEquals("Bucketized files should not keep their bucket info", "filepath13.nor\t\t\nfilepath13.nor|D|bucket2\t\t\n", selectRes);
    }

    @Test
    public void testAddExistingDeletedBucketized() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_add_existing_deletedbucket.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();
        dict.setValidateFiles(false);

        String selectRes = selectStringFilter(dict, dict.filter().files("filepath15.nor").includeDeleted());
        Assert.assertEquals("Deleted file should be included if option: include_deleted", "filepath15.nor|D|bucket2\t\t\n", selectRes);

        dict.insert("filepath15.nor");
        selectRes = selectStringFilter(dict, dict.filter().files("filepath15.nor").includeDeleted());
        Assert.assertEquals("Deleted file should not be reinstated", "filepath15.nor\t\t\nfilepath15.nor|D|bucket2\t\t\n", selectRes);

        // Add to new bucket and then delete again.
        dict.addToBucket("bucket3", dict.filter().files("filepath15.nor").get());
        dict.delete(dict.filter().files("filepath15.nor").get());

        dict.insert("filepath15.nor");
        selectRes = selectStringFilter(dict, dict.filter().files("filepath15.nor"));
        Assert.assertEquals("Deleted file should not be reinstated", "filepath15.nor\t\t\n", selectRes);
        selectRes = selectStringFilter(dict, dict.filter().files("filepath15.nor").includeDeleted());
        Assert.assertEquals("Deleted file should not be reinstated", "filepath15.nor\t\t\nfilepath15.nor|D|bucket2\t\t\nfilepath15.nor|D|bucket3\t\t\n", selectRes);
    }

    @Test
    public void testAddLineColumnMismatch() {
        String tableName = "nortable_addlinecolumnmismatch";
        File nordFile = new File(tableWorkDir.toFile(), tableName + ".nord");
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        String[] columnDef = new String[]{"Chrom", "Pos", "Alt", "PN", "COL1", "COL2"};
        dict.setColumns(columnDef);

        // Add one file.
        String dataFileName = Paths.get("../tests/data/gor/genes.nor").toAbsolutePath().toString();
        try {
            dict.insert(dataFileName);
            Assert.fail("Should not be able ot insert file with different columns def.");
        } catch (Exception ex) {
            // Ignrore, success.
        }
    }

    @Test
    public void testDeletLineFileMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_delete_filematch.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable.Builder<>(nordFile.toPath()).build();

        dict.delete(dict.filter().files("filepath1.nor", "filepath8.nor").get());
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath1.nor", "filepath8.nor").includeDeleted());
        Assert.assertEquals("Files should be deleted", "", selectRes);
    }

    @Test
    public void testDeletLineTagMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_delete_tagmatch.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.delete(dict.filter().tags("tagA", "tagF").get());
        String selectRes = selectStringFilter(dict, dict.filter().tags("tagA", "tagF").includeDeleted());
        Assert.assertEquals("Files should be deleted", "", selectRes);
    }

    @Test
    public void testDeletLineTagMatchSubset() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_delete_tagmatchsubset.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.delete(dict.filter().tags("tagD").get());
        String selectRes = selectStringFilter(dict, dict.filter().tags("tagD").includeDeleted());
        Assert.assertEquals("Files should be deleted", "", selectRes);
    }

    @Test
    public void testDeletLineExactMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_delete_exactmatch.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.delete(dict.filter().matchAllTags("tagJ", "tagK").get());
        String selectRes = selectStringFilter(dict, dict.filter().tags("tagJ"));
        Assert.assertEquals("Only exact match should be deleted",
                "filepath18.nor\t\ttagJ,tagM\n", selectRes);
    }

    @Test
    public void testDeletLineBucketized() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_delete_bucketized.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.delete(new DictionaryEntry.Builder("filepath13.nor", dict.getRootPath()).build());
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath13.nor").includeDeleted());
        Assert.assertEquals("Delete bucketized failed", "filepath13.nor|D|bucket2\t\t\n", selectRes);

        Files.createFile(tableWorkDir.resolve("filepath14B.nor"));
        dict.insert((DictionaryEntry)new DictionaryEntry.Builder("filepath14B.nor", dict.getRootPath()).alias("tagL").build());
        selectRes = selectStringFilter(dict, dict.filter().tags("tagL").includeDeleted());
        Assert.assertEquals("Insert same tag as deleted failed", "filepath14.nor|D|bucket2\ttagL\t\nfilepath14B.nor\ttagL\t\n", selectRes);
    }

    @Test
    public void testSelectLineMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_select_filematch.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().files("filepath1.nor", "filepath8.nor"));
        Assert.assertEquals("Select failed", "filepath1.nor\t\t\n" + "filepath8.nor\ttagA\t\n", selectRes);
    }

    @Test
    public void testSelectTagMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_select_tagmatch.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().tags("tagA", "tagF"));
        Assert.assertEquals("Select failed",
                "filepath2.nor\ttagA\t\n" +
                        "filepath5.nor\ttagF\t\n" +
                        "filepath6.nor\ttagF\t\n" +
                        "filepath8.nor\ttagA\t\n", selectRes);

        selectRes = selectStringFilter(dict, dict.filter().tags("tagD", "tagF2"));
        Assert.assertEquals("Select failed",
                "filepath16.nor\ttagD\t\n" +
                        "filepath4.nor\t\ttagD,tagE\n" +
                        "filepath7.nor\t\ttagF1,tagF2\n"
                , selectRes);


        selectRes = selectStringFilter(dict, dict.filter().tags("tagA", "tagA"));
        Assert.assertEquals("Select failed, should only get lines once.",
                "filepath2.nor\ttagA\t\n" +
                        "filepath8.nor\ttagA\t\n", selectRes);
    }

    @Test
    public void testSelectAliasMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_select_tagmatch.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().tags("tagA", "tagF"));
        Assert.assertEquals("Select failed",
                "filepath2.nor\ttagA\t\n" +
                        "filepath5.nor\ttagF\t\n" +
                        "filepath6.nor\ttagF\t\n" +
                        "filepath8.nor\ttagA\t\n", selectRes);
    }

    @Test
    public void testSelectBucketMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_select_bucketmatch.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().buckets("bucket2"));
        Assert.assertEquals("Select failed",
                "filepath11.nor|bucket2\ttagI\t\n" +
                        "filepath12.nor|bucket2\t\ttagJ,tagK\n" +
                        "filepath13.nor|bucket2\t\t\n" +
                        "filepath14.nor|D|bucket2\ttagL\t\n" +
                        "filepath15.nor|D|bucket2\t\t\n", selectRes);
    }

    @Test
    public void testSelectExactMatch() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_select_exactmatch.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        String selectRes = selectStringFilter(dict, dict.filter().tags("tagF"));
        Assert.assertEquals("Select failed", "filepath5.nor\ttagF\t\nfilepath6.nor\ttagF\t\n", selectRes);
    }

    @Test
    public void testNeedBucketizing() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_addtobucket_file.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        String result = dict.needsBucketizing().stream().map(DictionaryEntry::formatEntry).collect(Collectors.joining());
        Assert.assertEquals("Need bucketizing incorrect",
                "filepath1.nor\t\t\n" +
                        "filepath2.nor\ttagA\t\n" +
                        "filepath3.nor\ttagB\t\n" +
                        "filepath4.nor\t\ttagD,tagE\n" +
                        "filepath5.nor\ttagF\t\n" +
                        "filepath6.nor\ttagF\t\n" +
                        "filepath7.nor\t\ttagF1,tagF2\n" +
                        "filepath8.nor\ttagA\t\n" +
                        "filepath16.nor\ttagD\t\n" +
                        "filepath17.nor\ttagB\t\n" +
                        "filepath18.nor\t\ttagJ,tagM\n" +
                        "filepath19.nor\ttagK\t\n", result);
    }

    @Test
    public void testAddToBucketFile() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_addtobucket_file.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.addToBucket("newbucket", dict.filter().files("filepath3.nor").get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("newbucket"));
        Assert.assertEquals("Add to bucket incorrect",
                "filepath3.nor|newbucket\ttagB\t\n", selectRes);
    }

    @Test
    public void testAddToBucketTags() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_addtobucket_tags.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.addToBucket("newbucket", dict.filter().tags("tagA", "tagB").get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("newbucket"));
        Assert.assertEquals("Add to bucket incorrect",
                "filepath17.nor|newbucket\ttagB\t\n" +
                        "filepath2.nor|newbucket\ttagA\t\n" +
                        "filepath3.nor|newbucket\ttagB\t\n" +
                        "filepath8.nor|newbucket\ttagA\t\n", selectRes);
    }

    @Test
    public void testAddToBucketExisting() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_addtobucket_tags.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        try {
            dict.addToBucket("newbucket", dict.filter().tags("tagG").get());
            Assert.fail("Should not be able to set bucket on line already with a bucket");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testDeleteFromBucketFile() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_deletefrombucket_file.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.removeFromBucket(dict.filter().files("filepath13.nor").get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("bucket2"));
        Assert.assertEquals("Delete from bucket incorrect",
                "filepath11.nor|bucket2\ttagI\t\n" +
                        "filepath12.nor|bucket2\t\ttagJ,tagK\n" +
                        "filepath14.nor|D|bucket2\ttagL\t\n" +
                        "filepath15.nor|D|bucket2\t\t\n", selectRes);

    }

    @Test
    public void testDeleteFromBucketTags() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_deletefrombucket_tags.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.removeFromBucket(dict.filter().tags("tagJ", "tagL").includeDeleted().get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("bucket2"));
        Assert.assertEquals("Delete from bucket incorrect",
                "filepath11.nor|bucket2\ttagI\t\n" +
                        "filepath13.nor|bucket2\t\t\n" +
                        "filepath15.nor|D|bucket2\t\t\n", selectRes);

        selectRes = selectStringFilter(dict, dict.filter().files("filepath14.nor"));
        Assert.assertEquals("Delete from bucket should remove filepath14.nor form table", "", selectRes);
    }

    @Test
    public void testDeleteFromBucketDeleted() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_deletefrombucket_deleted.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.removeFromBucket(dict.filter().files("filepath15.nor").get());
        String selectRes = selectStringFilter(dict, dict.filter().files("filepath15.nor"));
        Assert.assertEquals("Delete from bucket should remove filepath15.nor form table", "", selectRes);
    }

    @Test
    public void testRemoveFromBucket() throws Exception {
        File nordFile = new File(tableWorkDir.toFile(), "nortable_deletebucket.nord");
        FileUtils.write(nordFile, nort1, (String) null);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());

        dict.removeFromBucket(dict.filter().buckets("bucket2").get());
        String selectRes = selectStringFilter(dict, dict.filter().buckets("bucket2"));
        Assert.assertEquals("Delete bucket incorrect", "", selectRes);
    }

    @Test
    public void testQueryOnReAddedFile() throws Exception {
        String testName = "testQueryOnReAddedFile";
        File nordFile = new File(tableWorkDir.toFile(), testName + ".nord");
        File bucket1gor = new File(tableWorkDir.toFile(), testName + "_bucket1.nor");

        // Setup data.
        FileUtils.write(new File(tableWorkDir.toFile(), testName + "_pn1.nor"), "#chrom\tpos\tcol1\nchr1\t1\tpn1gor\n", "UTF-8");
        FileUtils.write(new File(tableWorkDir.toFile(), testName + "_pn2.nor"), "#chrom\tpos\tcol1\nchr2\t2\tpn2gor\n", "UTF-8");
        FileUtils.write(new File(tableWorkDir.toFile(), testName + "_pn3.nor"), "#chrom\tpos\tcol1\nchr3\t3\tpn3gor\n", "UTF-8");
        FileUtils.write(new File(tableWorkDir.toFile(), testName + "_pn4.nor"), "#chrom\tpos\tcol1\nchr4\t4\tpn4gor\n", "UTF-8");
        FileUtils.write(new File(tableWorkDir.toFile(), testName + "_pn5.nor"), "#chrom\tpos\tcol1\nchr5\t5\tpn5gor\n", "UTF-8");
        FileUtils.write(new File(tableWorkDir.toFile(), testName + "_pn6.nor"), "#chrom\tpos\tcol1\nchr6\t6\tpn6gor\n", "UTF-8");
        FileUtils.write(new File(tableWorkDir.toFile(), testName + "_pn7.nor"), "#chrom\tpos\tcol1\nchr7\t7\tpn7gor\n", "UTF-8");
        FileUtils.write(new File(tableWorkDir.toFile(), testName + "_pn8.nor"), "#chrom\tpos\tcol1\nchr8\t8\tpn8gor\n", "UTF-8");
        FileUtils.write(new File(tableWorkDir.toFile(), testName + "_pn9.nor"), "#chrom\tpos\tcol1\nchr9\t9\tpn9gor\n", "UTF-8");

        String bucketData = "#chrom\tpos\tcol1\tSource\n" +
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

        FileUtils.write(nordFile,
                testName + "_pn1.nor|D|" + testName + "_bucket1.nor\tpn1\n"
                        + testName + "_pn2.nor|" + testName + "_bucket1.nor\tpn2\n"
                        + testName + "_pn3.nor|" + testName + "_bucket1.nor\tpn3\n"
                        + testName + "_pn4.nor|" + testName + "_bucket1.nor\tpn4\n"
                        + testName + "_pn5.nor|" + testName + "_bucket1.nor\tpn5\n"
                        + testName + "_pn6.nor|" + testName + "_bucket1.nor\tpn6\n"
                        + testName + "_pn7.nor|" + testName + "_bucket1.nor\tpn7\n"
                        + testName + "_pn8.nor|" + testName + "_bucket1.nor\tpn8\n"
                        + testName + "_pn9.nor|" + testName + "_bucket1.nor\tpn9\n"
                        + testName + "_pn1.nor\tpn1\n", "UTF-8");

        // Do testing
        Assert.assertEquals("No filter fails",
                TestUtils.runGorPipe("nor " + bucket1gor.getAbsolutePath() + " "),
                TestUtils.runGorPipe("nor " + nordFile.getAbsolutePath() + " | sort"));

        Assert.assertEquals("All filter fails",
                TestUtils.runGorPipe("nor " + bucket1gor.getAbsolutePath() + " -f pn1,pn2,pn3,pn4,pn5,pn6,pn7,pn8,pn9 "),
                TestUtils.runGorPipe("nor " + nordFile.getAbsolutePath() + " -f pn1,pn2,pn3,pn4,pn5,pn6,pn7,pn8,pn9"));

        Assert.assertEquals("Partial filter fails",
                "ChromNOR\tPosNOR\tchrom\tpos\tcol1\tSource\n" +
                        "chrN\t0\tchr1\t1\tpn1gor\tpn1\n" +
                        "chrN\t0\tchr2\t2\tpn2gor\tpn2\n" ,
                TestUtils.runGorPipe("nor " + nordFile.getAbsolutePath() + " -f pn1,pn2"));

        Assert.assertEquals("Filter deleted fails", "chrN\t0\tchr1\t1\tpn1gor\tpn1", TestUtils.runGorPipeNoHeader("nor " + nordFile.getAbsolutePath() + " -f pn1").trim());

        Assert.assertEquals("Filter not deleted fails", "chrN\t0\tchr2\t2\tpn2gor\tpn2", TestUtils.runGorPipeNoHeader("nor " + nordFile.getAbsolutePath() + " -f pn2").trim());
    }

    @Test
    public void testRewriteOfManuallyWrittenNords() throws IOException {
        String testName = "testRewriteOfManuallyWrittenNords";
        File nordFile = new File(tableWorkDir.toFile(), testName + ".nord");
        FileUtils.writeStringToFile(nordFile, "./file1.nor\tpn1\n", Charset.defaultCharset());
        Assert.assertEquals("./file1.nor\tpn1\n", FileUtils.readFileToString(nordFile, Charset.defaultCharset()));

        List<String> files = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        files.add("file1.nor");
        tags.add("pn1");

        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());
        dict.setValidateFiles(false);
        dict.insert("file2.nor\tpn2");
        dict.save();

        Assert.assertEquals("file1.nor\tpn1\t\nfile2.nor\tpn2\t\n", FileUtils.readFileToString(nordFile, Charset.defaultCharset()));
    }


    @Test
    public void testNordWithHeaderFiles() throws IOException {
        String testName = "testNordWithHeaderlessFiles";

        FileUtils.writeStringToFile(new File(tableWorkDir.toFile(), "file1.nor"), "#col1\tcol2\ncol111\tcol112\ncol121\tpn122\n", Charset.defaultCharset());
        FileUtils.writeStringToFile(new File(tableWorkDir.toFile(), "file2.nor"), "#col1\tcol2\ncol211\tpn212\ncol221\tpn222\n", Charset.defaultCharset());

        File nordFile = new File(tableWorkDir.toFile(), testName + ".nord");
        FileUtils.writeStringToFile(nordFile, "./file1.nor\tpn1\n", Charset.defaultCharset());

        Assert.assertEquals("./file1.nor\tpn1\n", FileUtils.readFileToString(nordFile, Charset.defaultCharset()));

        var results = TestUtils.runGorPipe("nor " + nordFile.getAbsolutePath());
        Assert.assertEquals("ChromNOR\tPosNOR\tcol1\tcol2\tSource\n" +
                "chrN\t0\tcol111\tcol112\tpn1\n" +
                "chrN\t0\tcol121\tpn122\tpn1\n", results);

        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());
        dict.setValidateFiles(true);
        dict.insert("file2.nor\tpn2");
        dict.save();

        results = TestUtils.runGorPipe("nor " + nordFile.getAbsolutePath());
        Assert.assertEquals("ChromNOR\tPosNOR\tcol1\tcol2\tSource\n" +
                "chrN\t0\tcol111\tcol112\tpn1\n" +
                "chrN\t0\tcol121\tpn122\tpn1\n" +
                "chrN\t0\tcol211\tpn212\tpn2\n" +
                "chrN\t0\tcol221\tpn222\tpn2\n", results);
    }

    @Test
    public void testNordWithHeaderlessFiles() throws IOException {
        String testName = "testNordWithHeaderlessFiles";

        FileUtils.writeStringToFile(new File(tableWorkDir.toFile(), "file1.nor"), "col111\tcol112\ncol121\tpn122\n", Charset.defaultCharset());
        FileUtils.writeStringToFile(new File(tableWorkDir.toFile(), "file2.nor"), "col211\tpn212\ncol221\tpn222\n", Charset.defaultCharset());

        File nordFile = new File(tableWorkDir.toFile(), testName + ".nord");
        FileUtils.writeStringToFile(nordFile, "./file1.nor\tpn1\n", Charset.defaultCharset());

        Assert.assertEquals("./file1.nor\tpn1\n", FileUtils.readFileToString(nordFile, Charset.defaultCharset()));

        var results = TestUtils.runGorPipe("nor " + nordFile.getAbsolutePath());
        Assert.assertEquals("ChromNOR\tPosNOR\tcol1\tcol2\tSource\n" +
                "chrN\t0\tcol111\tcol112\tpn1\n" +
                "chrN\t0\tcol121\tpn122\tpn1\n", results);
        NorDictionaryTable dict = new NorDictionaryTable(nordFile.toPath());
        dict.setValidateFiles(true);
        dict.insert("file2.nor\tpn2");
        dict.save();

        results = TestUtils.runGorPipe("nor " + nordFile.getAbsolutePath());
        Assert.assertEquals("ChromNOR\tPosNOR\tcol1\tcol2\tSource\n" +
                "chrN\t0\tcol111\tcol112\tpn1\n" +
                "chrN\t0\tcol121\tpn122\tpn1\n" +
                "chrN\t0\tcol211\tpn212\tpn2\n" +
                "chrN\t0\tcol221\tpn222\tpn2\n", results);
    }

    @SafeVarargs
    private final String selectStringFilter(NorDictionaryTable table, DictionaryFilter<DictionaryEntry>... filters) {
        return table.selectUninon(filters).stream().map(DictionaryEntry::formatEntry).sorted().collect(Collectors.joining());
    }

    private String trimLines(String input) {
        return Arrays.stream(input.split("\n")).map(String::trim).collect(Collectors.joining("\n"));
    }
}
