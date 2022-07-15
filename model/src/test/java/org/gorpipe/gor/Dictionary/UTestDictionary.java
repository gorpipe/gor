/*
 * Copyright (c) 2017.  WuxiNextCODE Inc.
 *
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * WuxiNextCODE Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with WuxiNextCODE.
 */

package org.gorpipe.gor.Dictionary;

import gorsat.TestUtils;
import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.gor.table.Dictionary;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Unit tests for gor dictionary.
 * <p>
 * To some extent this is just a copy of Gísli´s UTestDictionaryTable class, adapted to the dictionary class.
 */

public class UTestDictionary {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private static String gort1 = "filepath1.gor\ttag0\n" +
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
            "filepath13.gor|bucket2\ttag1000\n" +
            "filepath14.gor|D|bucket2\ttagL\n" +
            "filepath15.gor|D|bucket2\n" +
            "filepath16.gor\ttagD\n" +
            "filepath17.gor\ttagB\n" +
            "filepath18.gor\ttagJ,tagM\n" +
            "filepath19.gor\ttagK\n" +
            "filepath20.gor\ttagL\n" +
            "filepath21.gor|bucket3\ttagL";

    private static File dbsnp;
    private File dictionary;
    private File gorFile;
    private File simpleDictionary;
    private File pnFile;

    @Before
    public void setUp() throws Exception {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
        simpleDictionary = FileTestUtils.createGenericDictionaryFile(workDir.getRoot(), gorFile.getCanonicalPath(), "dictionary1.gord");
        pnFile = FileTestUtils.createPNTsvFile(workDir.getRoot());

        //Create a file to join with and a dictionary.
        final File bucketFile = workDir.newFile("bucketFile.gor");
        dbsnp = workDir.newFile("dbsnp_test.gor");
        final BufferedReader br = new BufferedReader(new FileReader("../tests/data/gor/dbsnp_test.gor"));
        final FileWriter fw = new FileWriter(bucketFile);
        final FileWriter fw3 = new FileWriter(dbsnp);
        final boolean[] header = {true};
        br.lines().forEach(line -> {
            if (!header[0]) {
                try {
                    for (int i = 0; i < 6; ++i) {
                        fw.write(line + "\tPN" + i);
                    }
                    fw3.write(line + "\n");
                } catch (IOException e) {
                }
            } else {
                try {
                    fw.write(line + "\tPN\n");
                    fw3.write(line + "\n");
                } catch (IOException e) {
                }
            }
        });
        fw.close();
        fw3.close();
        dictionary = workDir.newFile("dictionary.gord");
        final FileWriter fw2 = new FileWriter(dictionary);
        for (int i = 0; i < 6; ++i) fw2.write(dbsnp.toString() + "|" + bucketFile.toString() + "\tPN" + i + "\n");
        fw2.write(dbsnp.toString() + "\tPN" + 6);
        fw2.close();
    }


    @Test
    public void testDictionaryCacheAlias() throws IOException {
        File tmpFile = workDir.newFile("testDataCacheAlias.gord");
        FileWriter fileWriter = new FileWriter(tmpFile);
        for (int i = 0; i < 100; ++i) {
            fileWriter.write("file" + i + ".gor\tPN" + i + "\n");
        }
        fileWriter.close();

        HashSet<String> tagList1 = new HashSet<>(81);
        HashSet<String> tagList2 = new HashSet<>(81);

        for (int i = 0; i < 81; ++i) tagList1.add("PN" + i);
        for (int i = 19; i < 100; ++i) tagList2.add("PN" + i);

        Dictionary dictionary1 = getDictionary(tmpFile.getPath(), ".");
        Dictionary dictionary2 = getDictionary(tmpFile.getPath(), ".");
        Dictionary dictionary3 = getDictionary(tmpFile.getPath(), ".");

        // Check the same query results in same file count.
        Assert.assertEquals(tagList1.size(), dictionary1.getSources(tagList1, true, false).length);
        Assert.assertEquals(dictionary1.getSources(tagList1, true, false).length, dictionary2.getSources(tagList1, true, false).length);

        // Check if different tag list results in correct count.
        Assert.assertEquals(tagList2.size(), dictionary3.getSources(tagList2, true, false).length);
    }

    @Test
    public void testDictionaryCacheTags() throws IOException {
        File tmpFile = workDir.newFile("testDataCacheTags.gord");

        int listSize = 5;
        HashSet<String> tagList1 = new HashSet<>(listSize);
        HashSet<String> tagList2 = new HashSet<>(listSize);

        for (int i = 0; i < listSize; ++i) tagList1.add("PN" + i);
        for (int i = listSize; i < listSize + listSize; ++i) tagList2.add("PN" + i);

        try (FileWriter fileWriter = new FileWriter(tmpFile)) {
            fileWriter.write("file1.gor\ttagList1\tchr1\t-1\tchrN\t-1\t" + tagList1.stream().collect(Collectors.joining(",")) + "\n");
            fileWriter.write("file2.gor\ttagList2\tchr1\t-1\tchrN\t-1\t" + tagList2.stream().collect(Collectors.joining(",")) + "\n");
        }

        Dictionary dictionary1 = getDictionary(tmpFile.getPath(), ".");
        Dictionary dictionary2 = getDictionary(tmpFile.getPath(), ".");
        Dictionary dictionary3 = getDictionary(tmpFile.getPath(), ".");

        // Check the same query results in same file count.
        Assert.assertEquals(1, dictionary1.getSources(tagList1, true, false).length);
        Assert.assertEquals(dictionary1.getSources(tagList1, true, false).length, dictionary2.getSources(tagList1, true, false).length);

        // Check if different tag list results in correct count.
        Assert.assertEquals(1, dictionary3.getSources(tagList2, true, false).length);
    }

    @Test
    public void testEquality() throws Exception {
        File gordFile = workDir.newFile("gorDictionaryTestEquality.gord");
        FileUtils.write(gordFile, gort1, (Charset) null);
        HashSet<String> tagList = new HashSet<>();
        tagList.add("tagL");
        tagList.add("tagA");

        Dictionary dict1 = getDictionary(gordFile.getPath(), ".");
        Dictionary dict2 = getDictionary(gordFile.getPath(), ".");


        List<Dictionary.DictionaryLine> res1 = Arrays.asList(dict1.getSources(tagList, true, false));
        List<Dictionary.DictionaryLine> res2 = Arrays.asList(dict2.getSources(tagList, true, false));

        Assert.assertEquals(res1.size(), res2.size());

        String[] res1String = res1.stream().map(Dictionary.DictionaryLine::toPreciseString).sorted().toArray(String[]::new);
        String[] res2String = res2.stream().map(Dictionary.DictionaryLine::toPreciseString).sorted().toArray(String[]::new);

        final int len = res1.size();

        for (int i = 0; i < len; ++i) {
            Assert.assertEquals(res1String[i], res2String[i]);
        }
    }

    @Test
    public void testDictionaryWithDictionary() throws Exception {
        File gorFile = workDir.newFile("gorFile.gor");
        FileUtils.write(gorFile, "Chrom\tgene_start\tgene_end\tGene_Symbol\nchr1\t11868\t14412\tDDX11L1", (Charset) null);
        File gordChildDict = workDir.newFile("dictChild.gord");
        FileUtils.write(gordChildDict, gorFile.toString(), (Charset) null);
        File gordMotherDict = workDir.newFile("dictMother.gord");
        FileUtils.write(gordMotherDict, gordChildDict.toString(), (Charset) null);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\tSource\n" +
                "chr1\t11868\t14412\tDDX11L1\tgorFile.gor\n", TestUtils.runGorPipe("gor " + gordMotherDict.getPath()));
    }

    @Test
    public void testDictionaryWithTagFile() {
        final String query = "create #pns# = nor -asdict " + dictionary.toString() + " | select #2 | rename #1 PN | top 0;\n" +
                "gor " + dictionary.toString() + " -s PN -ff [#pns#] | top 10";
        TestUtils.assertGorpipeResults("Chrom\tPOS\treference\tallele\tdifferentrsIDs\tPN\n", query);
    }

    @Test
    public void testFileListCache() {
        final String query = "gor " + dictionary.toString() + " -s PN -f PN1,PN2,PN3,PN4,PN5,PN6";
        final String result = TestUtils.runGorPipe(query);
        TestUtils.assertGorpipeResults(result, query);
    }

    @Test
    public void testNoFilter() {
        final String result = TestUtils.runGorPipe("gor " + dictionary.toString());
        Assert.assertTrue(result.split("\n").length == 97);
    }

    //Simmi's test
    @Test
    public void testPartgorDictionary() throws IOException {
        String[] args = new String[]{"create xxx = partgor -dict " + simpleDictionary.getCanonicalPath() + " -ff " + pnFile.getCanonicalPath() + " <(gor " + simpleDictionary.getCanonicalPath() + " -f #{tags}); gor [xxx]"};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals("Nor should read dictionary file, not the files in the dictionary", 18, count);
    }

    //Simmi's test
    @Test
    public void testGorDictionary() throws IOException {
        String[] args = new String[]{"gor " + simpleDictionary.getCanonicalPath() + " -ff " + pnFile.getCanonicalPath()};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals("Nor should read dictionary file, not the files in the dictionary", 18, count);
    }

    @Test
    public void testCacheIsClearedWhenUniqueIdIsChanged() throws Exception {
        final String dictionaryFile = this.workDir.newFile("dict.gord").getAbsolutePath();
        final FileWriter dictionaryFileWriter = new FileWriter(dictionaryFile);
        dictionaryFileWriter.write("gorfile1.gor\ttag1\n");
        dictionaryFileWriter.close();

        final Dictionary dict1 = getDictionary(dictionaryFile, this.workDir.getRoot().getAbsolutePath());
        final Dictionary.DictionaryLine[] lines1 = dict1.getSources(new HashSet<>(Collections.singletonList("tag1")), true, false);
        Assert.assertEquals(1, lines1.length);

        // We are dealing with file timestamps here (some systems only have 1s resolution).
        Thread.sleep(1000);

        final FileWriter newDictionaryFileWriter = new FileWriter(dictionaryFile);
        newDictionaryFileWriter.write("gorfile1.gor\ttag1\ngorfile2.gor\ttag1\n");
        newDictionaryFileWriter.close();

        final Dictionary dict2 = getDictionary(dictionaryFile, this.workDir.getRoot().getAbsolutePath());
        final Dictionary.DictionaryLine[] lines2 = dict2.getSources(new HashSet<>(Collections.singletonList("tag1")), true, false);

        Assert.assertEquals(2, lines2.length);
    }

    @Test
    public void testEmptyDictionary() throws IOException {
        final File dict = workDir.newFile("dict_with_deleted_entry.gord");
        final BufferedWriter dictWriter = new BufferedWriter(new FileWriter(dict));
        dictWriter.write("");
        dictWriter.close();

        final String query = "gor " + dict.getAbsolutePath();
        boolean success = false;
        try {
            TestUtils.runGorPipe(query);
        } catch (GorDataException e) {
            success = e.getMessage().matches("Dictionary .* has no active lines.");
        }
        Assert.assertTrue(success);
    }

    public static Dictionary getDictionary(String path, String commonRoot) throws IOException {
        return Dictionary.getDictionary(path, ProjectContext.DEFAULT_READER, commonRoot, true);
    }
}