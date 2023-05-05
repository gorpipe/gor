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

package org.gorpipe.gor.table;

import gorsat.TestUtils;
import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.manager.BucketManager;
import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.dictionary.BaseDictionaryTable;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.util.ByteTextBuilder;
import org.gorpipe.test.SlowTests;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Test class for base table and entry unit tests.
 * <p>
 * Created by gisli on 14/11/2016.
 */
public class UTestBaseTable {

    private static final Logger log = LoggerFactory.getLogger(UTestBaseTable.class);

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    private Path gordFile;
    private Path afile;
    private Path bfile;
    private Path cfile;


    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    @Test
    public void testSignature() throws Exception {
        setupSimpleDict();

        DictionaryTable dict = new DictionaryTable.Builder(gordFile.toAbsolutePath()).build();

        // Check that file signature can be calculated correctly
        final String tagset1SignatureA = dict.getSignature("ABC1234", "ABC2234", "ABC3234");
        Assert.assertEquals(tagset1SignatureA, dict.getSignature("ABC1234", "ABC2234", "ABC3234"));
        Assert.assertFalse(tagset1SignatureA.equalsIgnoreCase(dict.getSignature("ABC1234", "ABC3234")));

        // Change file modification date and ensure there is a new signature
        Files.copy(afile, workDirPath.resolve("b.gor"), StandardCopyOption.REPLACE_EXISTING);
        bfile.toFile().setLastModified(System.currentTimeMillis() + 10000);
        log.debug(tagset1SignatureA);
        log.debug(dict.getSignature("ABC1234", "ABC2234", "ABC3234"));
        Assert.assertFalse(tagset1SignatureA.equalsIgnoreCase(dict.getSignature("ABC1234", "ABC2234", "ABC3234")));

        // Check empty tag list
        Assert.assertEquals("Error in get signature without tag list",
                new ByteTextBuilder(dict.getPath().toString() + "&" + Path.of(dict.getPath()).toFile().lastModified()).md5(), dict.getSignature());
    }

    @Test
    public void testLastModified() throws Exception {
        setupSimpleDict();

        // Check that lastModified can be queried for a given tag set
        DictionaryTable dict = new DictionaryTable.Builder(gordFile.toAbsolutePath()).build();
        final long lastModified = dict.getLastModified("ABC1234", "ABC2234", "ABC3234");
        Assert.assertTrue(lastModified != bfile.toFile().lastModified());
        Assert.assertEquals(lastModified, cfile.toFile().lastModified());
        // Now try without the previously last modified file
        final long lastModified2 = dict.getLastModified("ABC1234", "ABC2234");
        Assert.assertEquals(lastModified2, bfile.toFile().lastModified());

        // Check empty tag list
        Assert.assertEquals("Error in get lastmodified without tag list", Path.of(dict.getPath()).toFile().lastModified(), dict.getLastModified());
    }

    @Test
    /**
     * Test adding indirect files dictionary an signature/lastmodified for indirect files.
     *
     * Both of these end up using same lastModified utiltity method (that does the indirection) so we need just to test lastModified.
     */
    public void testIndirections() throws Exception {
        setupSimpleDict();

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).validateFiles(true).build();

        // Check symbolic links (system).

        Path dfile = Files.copy(afile, workDirPath.resolve("d.gor"), StandardCopyOption.REPLACE_EXISTING);
        dfile.toFile().setLastModified(System.currentTimeMillis() + 10000);
        Path dlink = Files.createSymbolicLink(workDirPath.resolve("dlink.gor"), dfile);
        dlink.toFile().setLastModified(System.currentTimeMillis() + 20000);
        dict.insert((DictionaryEntry)new DictionaryEntry.Builder(dlink, dict.getRootUri()).alias("d").build());
        dict.save();

        Assert.assertEquals("LastModfied system link failed", dfile.toFile().lastModified(), dict.getLastModified("d"));

        // Check link files

        Path efile = Files.copy(afile, workDirPath.resolve("elinkedto.gor"), StandardCopyOption.REPLACE_EXISTING);
        efile.toFile().setLastModified(System.currentTimeMillis() + 30000);
        Path elink = workDirPath.resolve("e.gor.link");
        Files.write(elink, efile.toString().getBytes());
        elink.toFile().setLastModified(System.currentTimeMillis() + 40000);
        dict.insert((DictionaryEntry)new DictionaryEntry.Builder(elink, dict.getRootUri()).alias("dl").build());
        dict.save();

        Assert.assertEquals("LastModfied link file failed", efile.toFile().lastModified(), dict.getLastModified("dl"));

        // Check missing files (that default to link file with same name).


        dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).validateFiles(false).build();
        Path ffile = Files.copy(afile, workDirPath.resolve("flinkedto.gor"), StandardCopyOption.REPLACE_EXISTING);
        ffile.toFile().setLastModified(System.currentTimeMillis() + 50000);
        Path flink = workDirPath.resolve("f.gor.link");
        Files.write(flink, ffile.toString().getBytes());
        flink.toFile().setLastModified(System.currentTimeMillis() + 60000);
        dict.insert((DictionaryEntry)new DictionaryEntry.Builder(Paths.get("f.gor"), dict.getRootUri()).alias("f").build());
        dict.save();

        Assert.assertEquals("LastModfied missing file (link failover) failed", ffile.toFile().lastModified(), dict.getLastModified("f"));
    }



    /**
     * Test dictionary signature methods
     *
     * @throws Exception
     */
    @Category(SlowTests.class)
    @Test
    public void testSignatureMaxFiles() throws Exception {
        int maxFiles = Integer.valueOf(System.getProperty("gor.table.signature.maxfiles", "10"));
        List<Path> dataFiles = new ArrayList<>();
        for (int i = 0; i < maxFiles + 1; i++) {
            Path p = workDir.newFile("datafile_" + Integer.toString(i) + ".gor").toPath();
            dataFiles.add(p);
        }

        Path d = workDir.newFile("x.gord").toPath();

        // Add files under the limit to the dictionary and test.

        DictionaryTable dict = new DictionaryTable.Builder(d).build();

        FileUtils.write(d.toFile(), String.join("\n", dataFiles.subList(0, 2).stream()
                .map(p -> p.getName(p.getNameCount() - 1).toString() + "\t" + p.getNameCount())
                .collect(Collectors.toList())) + "\n", (Charset)null);

        dict.reloadForce();

        String signature1 = dict.getSignature((String[]) null);
        Thread.sleep(1000); // File modified resolution is 1 sec on some systems.
        Assert.assertEquals("Should return same signature if no change", signature1, dict.getSignature((String[]) null));
        Thread.sleep(1000); // File modified resolution is 1 sec on some systems.
        Files.setLastModifiedTime(dataFiles.get(0), FileTime.from(System.currentTimeMillis(), TimeUnit.MILLISECONDS));
        String signature2 = dict.getSignature((String[]) null);
        Assert.assertEquals("Signature should not change upon touching a data file as few files (no tags)", signature1, signature2);
        String signature2a = dict.getSignature("1");
        Assert.assertNotEquals("Signature should change upon touching a data file as few files (tags)", signature1, signature2a);
        Thread.sleep(1000); // File modified resolution is 1 sec on some systems.
        Files.setLastModifiedTime(d, FileTime.from(System.currentTimeMillis(), TimeUnit.MILLISECONDS));
        Assert.assertNotEquals("Signature should change upon touching a dictionary file with few files (no tags)", signature2, dict.getSignature((String[]) null));
        Assert.assertEquals("Signature should not change upon touching a dictionary file with few files (tags)", signature2a, dict.getSignature("1"));

        // Add more files so we go over the max files limit and test.

        FileUtils.write(d.toFile(), String.join("\n", dataFiles.subList(2, dataFiles.size()).stream()
                .map(p -> p.getName(p.getNameCount() - 1).toString())
                .collect(Collectors.toList())) + "\n", (Charset)null, true);

        dict.reload();

        String signature3 = dict.getSignature((String[]) null);
        Thread.sleep(1000); // File modified resolution is 1 sec on some systems.
        Assert.assertEquals("Should return same signature if no change", signature3, dict.getSignature((String[]) null));
        Thread.sleep(1000); // File modified resolution is 1 sec on some systems.
        Files.setLastModifiedTime(dataFiles.get(0), FileTime.from(System.currentTimeMillis(), TimeUnit.MILLISECONDS));
        String signature4 = dict.getSignature((String[]) null);
        Assert.assertEquals("Signature should not change upon touching a data file as many files", signature3, signature4);
        Thread.sleep(1000); // File modified resolution is 1 sec on some systems.
        Files.setLastModifiedTime(d, FileTime.from(System.currentTimeMillis(), TimeUnit.MILLISECONDS));
        Assert.assertNotEquals("Signature should change upon touching the dictionary file as many files", signature4, dict.getSignature((String[]) null));

    }

    /**
     * Test handling of the source column.
     *
     * @throws Exception
     */
    @Test
    public void testSourceColumnDefaultNotBucketized() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - default not bucketized", "chromo\tpos\tdata\ttag\tSpecial\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultNotBucketizedNoMeta() throws Exception {
        prepareDictGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - default not bucketized", "chromo\tpos\tdata\ttag\tSource\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultNotBucketizedNoMetaNoY() throws Exception {
        prepareDictGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - default not bucketized", "chromo\tpos\tdata\ttag\tSource\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultNotBucketizedNoMetaHeaderLine() throws Exception {
        prepareDictGordFileWithHeader();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - default not bucketized", "chromo\tpos\tdata\ttag\tExtraSpecial\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultBucketized() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - bucketized", "chromo\tpos\tdata\ttag\tSpecial\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultBucketizedPgor() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("pgor " + gordFile.toString());
            Assert.assertEquals("Source col name incorrect - bucketized", "chromo\tpos\tdata\ttag\tSpecial\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultBucketizedNoMeta() throws Exception {
        prepareDictGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - bucketized", "chromo\tpos\tdata\ttag\tSource\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultBucketizedNoMetaHeaderLine() throws Exception {
        prepareDictGordFileWithHeaderBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - bucketized", "chromo\tpos\tdata\ttag\tExtraSpecial\n", result[0]);
    }

    @Test
    public void testSourceColumnNotBucketized() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -s PAX " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - -s not bucketized", "chromo\tpos\tdata\ttag\tPAX\n", result[0]);
    }

    @Test
    public void testSourceColumnBucketized() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -s PAX " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - -s bucketized", "chromo\tpos\tdata\ttag\tPAX\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultNotBucketizedWithSelect() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString() + " | select 1,2");
        Assert.assertEquals("Source col name incorrect - -s not bucketized", "chromo\tpos\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultBucketizedWithSelect() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString() + " | select 1,2");
        Assert.assertEquals("Source col name incorrect - -s bucketized", "chromo\tpos\n", result[0]);

    }

    @Test
    public void testSourceColumnNotBucketizedWithSelect() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -s PAX " + gordFile.toString() + " | select 1,2");
        Assert.assertEquals("Source col name incorrect - -s not bucketized", "chromo\tpos\n", result[0]);

    }

    @Test
    public void testSourceColumnBucketizedWithSelect() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -s PAX " + gordFile.toString() + " | select 1,2");
        Assert.assertEquals("Source col name incorrect - -s bucketized", "chromo\tpos\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultNotBucketizedWithSelectThreeColumns() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString() + " | select 1,2,3");
        Assert.assertEquals("Source col name incorrect - -s not bucketized", "chromo\tpos\tdata\n", result[0]);

    }

    @Test
    public void testSourceColumnDefaultBucketizedWithSelectThreeColumns() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor " + gordFile.toString() + " | select 1,2,3");
        Assert.assertEquals("Source col name incorrect - -s bucketized", "chromo\tpos\tdata\n", result[0]);

    }

    @Test
    public void testSourceColumnNotBucketizedWithSelectThreeColumns() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -s PA " + gordFile.toString() + " | select 1,2,3");
        Assert.assertEquals("Source col name incorrect - -s not bucketized", "chromo\tpos\tdata\n", result[0]);

    }

    @Test
    public void testSourceColumnBucketizedWithSelectThreeColumns() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -s PAX " + gordFile.toString() + " | select 1,2,3");
        Assert.assertEquals("Source col name incorrect - -s bucketized", "chromo\tpos\tdata\n", result[0]);

        // -s -c (including source)
        /* TODO Ignore for now:  Picking the source column using -c does not work very well.
          a. for not bucketized table it does not find the column
          b. for bucketize it finds the column but does not know which column to rename so we get column name from the bucket.

        result = TestUtils.runGorPipeLines("gor -Y -c 1,2,5 " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - -s not bucketized", "chromo\tpos\tSource\n", result[0]);

        result = TestUtils.runGorPipeLines("gor -c 1,2,5 " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - -s bucketized", "chromo\tpos\tSource\n", result[0]);

        result = TestUtils.runGorPipeLines("gor -Y -c 1,2,5 -s PAX " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - -s not bucketized", "chromo\tpos\tPAX\n", result[0]);

        result = TestUtils.runGorPipeLines("gor -c 1,2,5 -s PAX " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - -s bucketized", "chromo\tpos\tPAX\n", result[0]);
        */

        // -s -c -f - Checking that filtering works.

    }

    @Test
    public void testSourceColumnDefaultNotBucketizedWithFiltering() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -f a " + gordFile.toString() + " | select 1,2,3");
        Assert.assertEquals("Source col name incorrect - -s not bucketized", "chromo\tpos\tdata\n", result[0]);

    }

    @Test
    public void testSourceColumnDefaultBucketizedWithFiltering() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -f a " + gordFile.toString() + " | select 1,2,3");
        Assert.assertEquals("Source col name incorrect - -s bucketized", "chromo\tpos\tdata\n", result[0]);

    }

    @Test
    public void testSourceColumnNotBucketizedWithFiltering() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -s PAX -f a " + gordFile.toString()+ " | select 1,2,3");
        Assert.assertEquals("Source col name incorrect - -s not bucketized", "chromo\tpos\tdata\n", result[0]);

    }

    @Test
    public void testSourceColumnBucketizedWithFiltering() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -s PAX -f a " + gordFile.toString() + " | select 1,2,3");
        Assert.assertEquals("Source col name incorrect - -s bucketized", "chromo\tpos\tdata\n", result[0]);

        // -s -c -f - Checking that filtering works.

    }

    @Test
    public void testSourceColumnDefaultNotBucketizedWithMoreFiltering() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -f a,b " + gordFile.toString()+ " | select 1,2,3");
        Assert.assertEquals("Row count incorrect - -c -f not bucketized", 21, result.length);

    }

    @Test
    public void testSourceColumnDefaultBucketizedWithMoreFiltering() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -f a,b " + gordFile.toString()+ " | select 1,2,3");
        Assert.assertEquals("Row count incorrect - -c -f bucketized", 21, result.length);

        // Emtpy result.

    }

    @Test
    public void testSourceColumnDefaultNotBucketizedWithEmptyFiltering() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -p chr99 " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - empty result not bucketized", "chromo\tpos\tdata\ttag\tSpecial\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultNotBucketizedWithEmptyFilteringNoMeta() throws Exception {
        prepareDictGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -p chr99 " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - empty result not bucketized", "chromo\tpos\tdata\ttag\tSource\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultNotBucketizedWithEmptyFilteringNoMetaWithHeader() throws Exception {
        prepareDictGordFileWithHeader();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -p chr99 " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - empty result not bucketized", "chromo\tpos\tdata\ttag\tExtraSpecial\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultBucketizedWithEmptyFiltering() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -p chr99 " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - empty result bucketized", "chromo\tpos\tdata\ttag\tSpecial\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultBucketizedWithEmptyFilteringNoMeta() throws Exception {
        prepareDictGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -p chr99 " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - empty result bucketized", "chromo\tpos\tdata\ttag\tSource\n", result[0]);
    }

    @Test
    public void testSourceColumnDefaultBucketizedWithEmptyFilteringNoMetaWithHeader() throws Exception {
        prepareDictGordFileWithHeaderBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -p chr99 " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - empty result bucketized", "chromo\tpos\tdata\ttag\tExtraSpecial\n", result[0]);
    }

    @Test
    public void testSourceColumnNotBucketizedWithEmptyFiltering() throws Exception {
        prepareTableGordFile();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -p chr99 -s PAX " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - empty result not bucketized", "chromo\tpos\tdata\ttag\tPAX\n", result[0]);
    }

    @Test
    public void testSourceColumnBucketizedWithEmptyFiltering() throws Exception {
        prepareTableGordFileBucketized();

        String[] result;
        result = TestUtils.runGorPipeLines("gor -p chr99 -s PAX " + gordFile.toString());
        Assert.assertEquals("Source col name incorrect - empty result bucketized", "chromo\tpos\tdata\ttag\tPAX\n", result[0]);
    }

    private void prepareTableGordFile() throws IOException {
        gordFile = workDirPath.resolve("dict.gord");
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).sourceColumn("Special").build();

        Path afile = createTestFile(workDirPath.resolve("a.gor"), 10, "a");
        Path bfile = createTestFile(workDirPath.resolve("b.gor"), 10, "b");
        Path cfile = createTestFile(workDirPath.resolve("c.gor"), 10, "c");

        Path abfile = createTestFile(workDirPath.resolve("ab.gor"), 10, "a", "b");
        Path bcfile = createTestFile(workDirPath.resolve("bc.gor"), 10, "b", "c");

        dict.insert(new DictionaryEntry.Builder<>(afile, dict.getRootUri()).alias("a").build());
        dict.insert(new DictionaryEntry.Builder<>(bfile, dict.getRootUri()).alias("b").build());
        dict.insert(new DictionaryEntry.Builder<>(cfile, dict.getRootUri()).alias("c").build());
        dict.insert(new DictionaryEntry.Builder<>(abfile, dict.getRootUri()).alias("ab").build());
        dict.insert(new DictionaryEntry.Builder<>(bcfile, dict.getRootUri()).alias("bc").build());
        dict.save();
    }

    private void prepareTableGordFileBucketized() throws IOException {
        prepareTableGordFile();

        gordFile = workDirPath.resolve("dict.gord");
        DictionaryTable dict =  new DictionaryTable.Builder<>(gordFile).build();
        TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(2).build();
        man.bucketize(dict.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);
    }

    private void prepareDictGordFile() throws IOException {
        prepareTableGordFile();
        Files.delete(workDirPath.resolve("dict.gord.meta"));
        Files.delete(workDirPath.resolve(".dict/header"));
    }

    private void prepareDictGordFileBucketized() throws IOException {
        prepareTableGordFileBucketized();
        Files.delete(workDirPath.resolve("dict.gord.meta"));
        Files.delete(workDirPath.resolve(".dict/header"));
    }

    private void prepareDictGordFileWithHeader() throws IOException {
        prepareTableGordFile();
        Files.delete(workDirPath.resolve("dict.gord.meta"));
        Files.delete(workDirPath.resolve(".dict/header"));
        String content = FileUtils.readFileToString(gordFile.toFile(), "utf8");
        content = "#Content\tExtraSpecial\n" + content;
        FileUtils.writeStringToFile(gordFile.toFile(), content, "utf8");
    }

    private void prepareDictGordFileWithHeaderBucketized() throws IOException {
        prepareTableGordFileBucketized();
        Files.delete(workDirPath.resolve("dict.gord.meta"));
        Files.delete(workDirPath.resolve(".dict/header"));
        String content = FileUtils.readFileToString(gordFile.toFile(), "utf8");
        content = "#Content\tExtraSpecial\n" + content;
        FileUtils.writeStringToFile(gordFile.toFile(), content, "utf8");
    }

    @Test
    public void testEntryPaths() throws Exception {
        String commonRoot = workDirPath.toString();
        Files.createDirectories(workDirPath.resolve("a/b/x/y"));
        Files.createFile(workDirPath.resolve("a/b/x/y/z.gor"));
        Files.createFile(Paths.get(".").resolve("t.gor")).toFile().deleteOnExit();

        Path gordFile = Paths.get(commonRoot).resolve("a/b/dict.gord");
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).validateFiles(false).build();

        // Relative
        URI path = URI.create("x/y/z.gor");
        DictionaryEntry entry = new DictionaryEntry.Builder(path.toString(), dict.getRootUri()).build();

        Assert.assertEquals("Relative path, wrong absolute path", gordFile.getParent().resolve(path.toString()).toString(), entry.getContentReal(dict.getRootUri()));
        Assert.assertEquals("Relative path, wrong relative path", "x/y/z.gor", entry.getContentRelative());


        // Absolute (to subfolder)
        path = URI.create(workDirPath.resolve("a/b/x/y/z.gor").toString());
        entry = new DictionaryEntry.Builder(path.toString(), dict.getRootUri()).build();
        dict.insert(entry);

        Assert.assertEquals("Absolute path to subfolder, wrong absolute path", path.toString(), entry.getContentReal(dict.getRootUri()));
        Assert.assertEquals("Absolute path to subfolder, wrong relative path", "x/y/z.gor", entry.getContentRelative());

        // Absolute path
        Path pathPath = Paths.get("t1.gor").toAbsolutePath();
        entry = new DictionaryEntry.Builder(pathPath, dict.getRootUri()).build();
        dict.insert(entry);

        Assert.assertEquals("Absolute path, wrong absolute path", pathPath.toString(), entry.getContentReal(dict.getRootUri()));
        Assert.assertEquals("Absolute path, wrong relative path", pathPath.toString(), entry.getContentRelative());

        // Absolute uri
        path = URI.create(Paths.get(".").toAbsolutePath().normalize().resolve("t2.gor").toString());
        entry = new DictionaryEntry.Builder(path.toString(), dict.getRootUri()).build();
        dict.insert(entry);

        Assert.assertEquals("Absolute uri, wrong absolute path", path.getPath(), entry.getContentReal(dict.getRootUri()));
        Assert.assertEquals("Absolute uri, wrong relative path", path.getPath(), entry.getContentRelative());


        // With schmea
        path = URI.create("s3://someaddress/path/x/y?a=b;c=d#xxx");
        entry = new DictionaryEntry.Builder(path.toString(), dict.getRootUri()).build();
        dict.insert(entry);

        Assert.assertEquals("Schema path, wrong absolute path", path.toString(), entry.getContentReal(dict.getRootUri()));
        Assert.assertEquals("Schema path wrong relative path", path.toString(), entry.getContentRelative());


        // Link file
        Path tmpDir = Files.createTempDirectory("linkdir");
        tmpDir.toFile().deleteOnExit();
        Path somegor = Files.createFile(tmpDir.resolve("some.gor"));
        Files.createDirectories(workDirPath.resolve("a/b"));
        path = URI.create(Files.createSymbolicLink(workDirPath.resolve("a/b/link_to_some.gor"), somegor).toString());
        entry = new DictionaryEntry.Builder(path.toString(), dict.getRootUri()).build();
        dict.insert(entry);

        Assert.assertEquals("Schema path, wrong absolute path", path.toString(), entry.getContentReal(dict.getRootUri()));
        Assert.assertEquals("Schema path wrong relative path", "link_to_some.gor", entry.getContentRelative());

        dict.save();
    }


    @Test
    public void testSpecialCharInPath() throws Exception {
        setupSimpleDict();
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).build();

        Path strangeFile = Files.copy(afile, workDirPath.resolve("strangename_#?_xxx.gor"), StandardCopyOption.REPLACE_EXISTING);
        dict.insert(strangeFile.toString());
        dict.save();

        String[] result = TestUtils.runGorPipeLines("gor " + gordFile.toString());
        Assert.assertEquals("Could not read special chars in filename", "chromo\tpos\tdata\tSource\n", result[0]);
    }


    @Test
    public void testOptimizerForReaddedLines() throws Exception {
        setupSimpleDict();
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).build();

        // Delete and readd - can confuse the optimzer.

        dict.delete(dict.filter().files(URI.create(afile.toString())).get());
        dict.save();
        dict.insert(new DictionaryEntry.Builder<>(afile, dict.getRootUri()).alias("ABC1234").build());
        dict.save();

        List<? extends DictionaryEntry> lines = dict.getOptimizedLines(new HashSet<>(Arrays.asList("ABC1234")), false, false);
        Assert.assertEquals("Optimizer failed if line readded - wrong number of files", 1, lines.size());

        String[] result = TestUtils.runGorPipeLines("gor -f ABC1234 " + gordFile.toString());
        Assert.assertEquals("Optimizer failed if line readded - wrong number of lines", 2, result.length);  // Header + 1 line.
    }

    @Test
    public void testInferShouldBucketizeFromFile() throws IOException {
        gordFile = workDirPath.resolve("dict.gord");
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).build();

        Assert.assertTrue(dict.inferShouldBucketizeFromFile("x.gor"));
        Assert.assertTrue(dict.inferShouldBucketizeFromFile("x.gorz"));
        Assert.assertTrue(dict.inferShouldBucketizeFromFile("/a/c/b/x.gor"));

        Assert.assertFalse(dict.inferShouldBucketizeFromFile("y.bam"));
        Assert.assertFalse(dict.inferShouldBucketizeFromFile("y.cram"));
        Assert.assertFalse(dict.inferShouldBucketizeFromFile("/a/b/c.bam"));
        Assert.assertFalse(dict.inferShouldBucketizeFromFile("y.vcf"));

        Assert.assertNull(dict.inferShouldBucketizeFromFile("y"));
        Assert.assertNull(dict.inferShouldBucketizeFromFile(""));
    }

    @Test
    public void testInferShouldBucketizeFromLinkFile() throws IOException {
        gordFile = workDirPath.resolve("dict.gord");
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).build();

        Path linkFile1 = Files.write(workDirPath.resolve("a.link"), "/x.gor\n".getBytes());
        Path linkFile2 = Files.write(workDirPath.resolve("b.link"), "/y.bam\n".getBytes());
        Assert.assertTrue(dict.inferShouldBucketizeFromFile(linkFile1.toString()));
        Assert.assertFalse(dict.inferShouldBucketizeFromFile(linkFile2.toString()));
    }

    @Test
    public void testInferBucketizeFromInsertTrue() throws IOException {
        gordFile = workDirPath.resolve("dict.gord");

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).build();

        afile = Files.write(workDirPath.resolve("a.gor"), "chromo\tpos\tdata\n1\t1000\tx\n".getBytes());
        dict.insert(new DictionaryEntry.Builder<>(afile, dict.getRootUri()).alias("ABC1234").build());

        Assert.assertTrue(dict.isBucketize());
    }

    @Test
    public void testInferBucketizeFromInsertFalse() throws IOException {
        gordFile = workDirPath.resolve("dict.gord");

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).build();

        afile = Files.write(workDirPath.resolve("a.tsv"), "chromo\tpos\tdata\n1\t1000\tx\n".getBytes());
        dict.insert(new DictionaryEntry.Builder<>(afile, dict.getRootUri()).alias("ABC1234").build());

        Assert.assertFalse(dict.isBucketize());
    }
    
    private void setupSimpleDict() throws Exception {
        gordFile = workDirPath.resolve("dict.gord");

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile.toAbsolutePath()).build();

        afile = Files.write(workDirPath.resolve("a.gor"), "chromo\tpos\tdata\n1\t1000\tx\n".getBytes());
        bfile = Files.write(workDirPath.resolve("b.gor"), "chromo\tpos\tdata\n1\t1010\ty\n".getBytes());
        bfile.toFile().setLastModified(System.currentTimeMillis());
        cfile = Files.write(workDirPath.resolve("c.gor"), "chromo\tpos\tdata\n1\t1020\tz\n".getBytes());
        cfile.toFile().setLastModified(System.currentTimeMillis() + 10000);

        Files.write(workDirPath.resolve("bucket1.gor"), "chromo\tpos\tdata\ttag\n1\t1000\tx\tABC1234\n1\t1020\tx\tABC3234\n".getBytes());
        Files.write(workDirPath.resolve("bucket2.gor"), "chromo\tpos\tdata\ttag\n1\t1010\ty\tABC2234\n".getBytes());

        dict.insert(new DictionaryEntry.Builder<>(afile, dict.getRootUri()).alias("ABC1234").bucket("bucket1.gor").build());
        dict.insert(new DictionaryEntry.Builder<>(bfile, dict.getRootUri()).alias("ABC2234").bucket("bucket2.gor").build());
        dict.insert(new DictionaryEntry.Builder<>(cfile, dict.getRootUri()).alias("ABC3234").bucket("bucket1.gor").build());
        dict.save();
    }

    /**
     * @param file
     * @param lines
     * @param tags  optional tags, if no tags specified no tags column is added.
     * @return
     * @throws IOException
     */
    private Path createTestFile(Path file, int lines, String... tags) throws IOException {
        StringBuilder result = new StringBuilder();

        result.append(tags.length > 0 ? "chromo\tpos\tdata\ttag\n" : "chromo\tpos\tdata\n");

        for (int i = 1; i <= lines; i++) {
            result.append(String.format("1\t%d\t%s%s\n", 1000 + i, "data " + i, tags.length > 0 ? "\t" + tags[i % tags.length] : ""));
        }
        Files.write(file, result.toString().getBytes());
        return file;
    }

}
