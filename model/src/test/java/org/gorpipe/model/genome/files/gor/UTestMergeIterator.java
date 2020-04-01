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

package org.gorpipe.model.genome.files.gor;

import org.gorpipe.exceptions.GorDataException;

import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.GorOptions;
import org.gorpipe.model.genome.files.gor.MergeIterator;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.test.GorDictionarySetup;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class UTestMergeIterator {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static int READ_TEST_DICT_NUM_FILES = 100;
    private static int READ_TEST_DICT_LINES_PER_TAG = 10;
    private static GorDictionarySetup dictForReadTest;

    @BeforeClass
    public static void setupClass() throws IOException {
        dictForReadTest = new GorDictionarySetup("readingFromDictionary", READ_TEST_DICT_NUM_FILES, 5, new int[]{1}, READ_TEST_DICT_LINES_PER_TAG, true);
    }

    @Test
    public void singleFileHeaderIsValid() throws IOException {
        GorOptions options = GorOptions.createGorOptions("../tests/data/gor/dbsnp_test.gor");
        List<GenomicIterator> genomicIterators = options.getIterators();

        try(MergeIterator mi = new MergeIterator(genomicIterators, options) ) {
            String[] header = mi.getHeader().split("\t");
            assertEquals(5, header.length);
            assertEquals("Chrom", header[0]);
            assertEquals("POS", header[1]);
            assertEquals("reference", header[2]);
            assertEquals("allele", header[3]);
            assertEquals("differentrsIDs", header[4]);
        }
    }

    @Test
    public void singleFileHeaderIsValidWhenInsertingSourceColumn() throws IOException {
        GorOptions options = GorOptions.createGorOptions("-s Source ../tests/data/gor/dbsnp_test.gor");
        List<GenomicIterator> genomicIterators = options.getIterators();

        try(MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            String[] header = mi.getHeader().split("\t");
            assertEquals(6, header.length);
            assertEquals("Chrom", header[0]);
            assertEquals("POS", header[1]);
            assertEquals("reference", header[2]);
            assertEquals("allele", header[3]);
            assertEquals("differentrsIDs", header[4]);
            assertEquals("Source", header[5]);
        }
    }

    @Test
    public void singleFileHasNextIsTrueAtStart() throws IOException {
        GorOptions options = GorOptions.createGorOptions("../tests/data/gor/dbsnp_test.gor");
        List<GenomicIterator> genomicIterators = options.getIterators();

        try(MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            assertTrue(mi.hasNext());
        }
    }

    @Test
    public void singleFileNextGetsValidLineAtStart() throws IOException {
        GorOptions options = GorOptions.createGorOptions("../tests/data/gor/dbsnp_test.gor");
        List<GenomicIterator> genomicIterators = options.getIterators();

        try(MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            // todo next doesn't work without a hasNext call first
            mi.hasNext();

            Row r = mi.next();

            assertEquals("chr1\t10179\tC\tCC\trs367896724", r.getAllCols().toString());
        }
    }

    @Test
    public void singleFileNextIteratesCorrectNumberOfLines() throws IOException {
        GorOptions options = GorOptions.createGorOptions("../tests/data/gor/dbsnp_test.gor");
        assertLineCount(48, options);
    }

    @Test
    public void multipleFilesHeaderIsValid() throws IOException {
        GorOptions options = GorOptions.createGorOptions("1.mem 2.mem 3.mem 4.mem 5.mem");
        List<GenomicIterator> genomicIterators = options.getIterators();

        try(MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            String[] header = mi.getHeader().split("\t");
            assertEquals(5, header.length);
            assertEquals("Chromo", header[0]);
            assertEquals("Pos", header[1]);
            assertEquals("Col3", header[2]);
            assertEquals("Col4", header[3]);
            assertEquals("Col5", header[4]);
        }
    }

    @Test
    public void multipleFilesHeaderIsValidWhenAddingSourceColumn() throws IOException {
        GorOptions options = GorOptions.createGorOptions("-s From 1.mem 2.mem 3.mem 4.mem 5.mem");
        List<GenomicIterator> genomicIterators = options.getIterators();

        try(MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            String[] header = mi.getHeader().split("\t");
            assertEquals(6, header.length);
            assertEquals("Chromo", header[0]);
            assertEquals("Pos", header[1]);
            assertEquals("Col3", header[2]);
            assertEquals("Col4", header[3]);
            assertEquals("Col5", header[4]);
            assertEquals("From", header[5]);
        }

    }

    @Test
    public void multipleFilesThrowsExceptionWhenFilesDontMatch() throws IOException {
        GorOptions options = GorOptions.createGorOptions("1.mem 2.mem 3.mem 4.mem ../tests/data/gor/dbsnp_test.gor");
        List<GenomicIterator> genomicIterators = options.getIterators();

        thrown.expect(GorDataException.class);
        new MergeIterator(genomicIterators, options);
    }

    @Test
    public void multipleFilesHasNextIsTrueAtStart() throws IOException {
        GorOptions options = GorOptions.createGorOptions("1.mem 2.mem 3.mem 4.mem 5.mem");
        List<GenomicIterator> genomicIterators = options.getIterators();

        try(MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            assertTrue(mi.hasNext());
        }
    }

    @Test
    public void multipleFilesNextGetsValidLineAtStart() throws IOException {
        GorOptions options = GorOptions.createGorOptions("1.mem 2.mem 3.mem 4.mem 5.mem");
        List<GenomicIterator> genomicIterators = options.getIterators();

        try(MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            // todo next doesn't work without a hasNext call first
            mi.hasNext();

            Row r = mi.next();

            assertEquals("chr1\t0\tdata1\t0\tdata0", r.getAllCols().toString());
        }
    }

    @Test
    public void multipleFilesNextIteratesCorrectNumberOfLines() throws IOException {
        GorOptions options = GorOptions.createGorOptions("1.mem 2.mem 3.mem 4.mem 5.mem");
        int expected = 20000;
        assertLineCount(expected, options);
    }

    @Test
    public void multipleFilesSeek() throws IOException {
        GorOptions options = GorOptions.createGorOptions("1.mem 2.mem 3.mem 4.mem 5.mem");
        List<GenomicIterator> genomicIterators = options.getIterators();
        try(MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            mi.seek("chr1", 100);

            int counter = 0;
            while (mi.hasNext()) {
                Row r = mi.next();
                if (r.pos > 100) {
                    break;
                }
                counter++;
            }

            // There should be as many rows with this position as there are files being merged,
            // as they are basically all the same.
            assertEquals(5, counter);
        }
    }

    @Test
    public void canMergeTwoFilesWithHeadersWithDifferentCase() throws IOException {
        String file1 = createGorFile("Chrom\tPos\tData", "chr1\t1\t123");
        String file2 = createGorFile("CHROM\tPOS\tDATA", "chr2\t11\t456");

        GorOptions options = GorOptions.createGorOptions(file1 + " " + file2);
        assertLineCount(2, options);
    }


    @Test
    public void readingFromDictionaryWithFilterOnAliasSingleTag() throws IOException {
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dictForReadTest.dictionary, "PN1"));
        assertLineCount(READ_TEST_DICT_LINES_PER_TAG, options);
    }

    @Test
    public void readingFromDictionaryWithFilterOnAliasSingleTagNoSourceRename() throws IOException {
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -f %s", dictForReadTest.dictionary, "PN1"));
        assertLineCount(READ_TEST_DICT_LINES_PER_TAG, options);
    }

    @Test
    public void readingFromDictionaryWithFilterOnAliasTwoTags() throws IOException {
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dictForReadTest.dictionary, "PN1,PN5"));
        assertLineCount(2 * READ_TEST_DICT_LINES_PER_TAG, options);
    }

    @Test
    public void readingFromDictionaryWithFilterOnAliasAllTags() throws IOException {
        ArrayList<String> tags = new ArrayList<>();
        for (int i = 1; i <= READ_TEST_DICT_NUM_FILES; i++) {
            tags.add("PN" + i);
        }
        String filterTags = tags.stream().collect(Collectors.joining(","));
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dictForReadTest.dictionary, filterTags));
        assertLineCount(READ_TEST_DICT_NUM_FILES * READ_TEST_DICT_LINES_PER_TAG, options);
    }

    @Test
    public void readingFromDictionaryWithFilterOnTagsSingleTagNoSourceRename() throws IOException {
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -f %s", dictForReadTest.dictionary, "PN1"));
        String expected = "Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData\tSource\n" +
                "chr1\t1\tPN1\tLineData for the chromosome and position line 1 1\tThis line should be long enough for this test purpose\t101808\tPN1\n" +
                "chr1\t2\tPN1\tLineData for the chromosome and position line 1 2\tThis line should be long enough for this test purpose\t280385\tPN1\n" +
                "chr1\t3\tPN1\tLineData for the chromosome and position line 1 3\tThis line should be long enough for this test purpose\t321302\tPN1\n" +
                "chr1\t4\tPN1\tLineData for the chromosome and position line 1 4\tThis line should be long enough for this test purpose\t342112\tPN1\n" +
                "chr1\t5\tPN1\tLineData for the chromosome and position line 1 5\tThis line should be long enough for this test purpose\t389825\tPN1\n" +
                "chr1\t6\tPN1\tLineData for the chromosome and position line 1 6\tThis line should be long enough for this test purpose\t406406\tPN1\n" +
                "chr1\t7\tPN1\tLineData for the chromosome and position line 1 7\tThis line should be long enough for this test purpose\t580390\tPN1\n" +
                "chr1\t8\tPN1\tLineData for the chromosome and position line 1 8\tThis line should be long enough for this test purpose\t671283\tPN1\n" +
                "chr1\t9\tPN1\tLineData for the chromosome and position line 1 9\tThis line should be long enough for this test purpose\t756981\tPN1\n" +
                "chr1\t10\tPN1\tLineData for the chromosome and position line 1 10\tThis line should be long enough for this test purpose\t940393\tPN1\n";
        assertContent(expected, options);
    }

    @Test
    public void readingFromDictionaryWithFilterOnTagsSingleTag() throws IOException {
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dictForReadTest.dictionary, "PN1"));
        String expected = "Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData\tPN\n" +
                "chr1\t1\tPN1\tLineData for the chromosome and position line 1 1\tThis line should be long enough for this test purpose\t101808\tPN1\n" +
                "chr1\t2\tPN1\tLineData for the chromosome and position line 1 2\tThis line should be long enough for this test purpose\t280385\tPN1\n" +
                "chr1\t3\tPN1\tLineData for the chromosome and position line 1 3\tThis line should be long enough for this test purpose\t321302\tPN1\n" +
                "chr1\t4\tPN1\tLineData for the chromosome and position line 1 4\tThis line should be long enough for this test purpose\t342112\tPN1\n" +
                "chr1\t5\tPN1\tLineData for the chromosome and position line 1 5\tThis line should be long enough for this test purpose\t389825\tPN1\n" +
                "chr1\t6\tPN1\tLineData for the chromosome and position line 1 6\tThis line should be long enough for this test purpose\t406406\tPN1\n" +
                "chr1\t7\tPN1\tLineData for the chromosome and position line 1 7\tThis line should be long enough for this test purpose\t580390\tPN1\n" +
                "chr1\t8\tPN1\tLineData for the chromosome and position line 1 8\tThis line should be long enough for this test purpose\t671283\tPN1\n" +
                "chr1\t9\tPN1\tLineData for the chromosome and position line 1 9\tThis line should be long enough for this test purpose\t756981\tPN1\n" +
                "chr1\t10\tPN1\tLineData for the chromosome and position line 1 10\tThis line should be long enough for this test purpose\t940393\tPN1\n";

        assertContent(expected, options);
    }

    @Test
    public void readingFromDictionaryWithFilterOnTagsTwoTags() throws IOException {
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dictForReadTest.dictionary, "PN1,PN3"));
        String expected = "Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData\tPN\n" +
                "chr1\t1\tPN1\tLineData for the chromosome and position line 1 1\tThis line should be long enough for this test purpose\t101808\tPN1\n" +
                "chr1\t1\tPN3\tLineData for the chromosome and position line 1 1\tThis line should be long enough for this test purpose\t183275\tPN3\n" +
                "chr1\t2\tPN1\tLineData for the chromosome and position line 1 2\tThis line should be long enough for this test purpose\t280385\tPN1\n" +
                "chr1\t2\tPN3\tLineData for the chromosome and position line 1 2\tThis line should be long enough for this test purpose\t343810\tPN3\n" +
                "chr1\t3\tPN1\tLineData for the chromosome and position line 1 3\tThis line should be long enough for this test purpose\t321302\tPN1\n" +
                "chr1\t3\tPN3\tLineData for the chromosome and position line 1 3\tThis line should be long enough for this test purpose\t386447\tPN3\n" +
                "chr1\t4\tPN1\tLineData for the chromosome and position line 1 4\tThis line should be long enough for this test purpose\t342112\tPN1\n" +
                "chr1\t4\tPN3\tLineData for the chromosome and position line 1 4\tThis line should be long enough for this test purpose\t429532\tPN3\n" +
                "chr1\t5\tPN1\tLineData for the chromosome and position line 1 5\tThis line should be long enough for this test purpose\t389825\tPN1\n" +
                "chr1\t5\tPN3\tLineData for the chromosome and position line 1 5\tThis line should be long enough for this test purpose\t485312\tPN3\n" +
                "chr1\t6\tPN1\tLineData for the chromosome and position line 1 6\tThis line should be long enough for this test purpose\t406406\tPN1\n" +
                "chr1\t6\tPN3\tLineData for the chromosome and position line 1 6\tThis line should be long enough for this test purpose\t514182\tPN3\n" +
                "chr1\t7\tPN1\tLineData for the chromosome and position line 1 7\tThis line should be long enough for this test purpose\t580390\tPN1\n" +
                "chr1\t7\tPN3\tLineData for the chromosome and position line 1 7\tThis line should be long enough for this test purpose\t549587\tPN3\n" +
                "chr1\t8\tPN1\tLineData for the chromosome and position line 1 8\tThis line should be long enough for this test purpose\t671283\tPN1\n" +
                "chr1\t8\tPN3\tLineData for the chromosome and position line 1 8\tThis line should be long enough for this test purpose\t775215\tPN3\n" +
                "chr1\t9\tPN1\tLineData for the chromosome and position line 1 9\tThis line should be long enough for this test purpose\t756981\tPN1\n" +
                "chr1\t9\tPN3\tLineData for the chromosome and position line 1 9\tThis line should be long enough for this test purpose\t779465\tPN3\n" +
                "chr1\t10\tPN1\tLineData for the chromosome and position line 1 10\tThis line should be long enough for this test purpose\t940393\tPN1\n" +
                "chr1\t10\tPN3\tLineData for the chromosome and position line 1 10\tThis line should be long enough for this test purpose\t853857\tPN3\n";

        assertContent(expected, options);
    }

    @Test
    public void readingFromDictionaryWithFilterOnTagsTaglistWithEmptyTagAtEnd() throws IOException {
        thrown.expect(GorDataException.class);
        thrown.expectMessage("Empty tag is not allowed");
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dictForReadTest.dictionary, "PN1,PN3,"));
    }

    @Test
    public void readingFromDictionaryWithFilterOnTagsTaglistWithEmptyTagInMiddle() throws IOException {
        thrown.expect(GorDataException.class);
        thrown.expectMessage("Empty tag is not allowed");
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dictForReadTest.dictionary, "PN1,,PN3"));
    }

    @Test
    public void readingFromDictionaryWithFilterOnTagsSingleTagWithSourceNoSourceRename() throws IOException {
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -f %s", dictForReadTest.dictionary, "PNMB1"));
        assertContent("Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData\tSource\tSource\n" +
                "chr1\t10\tPNMB1\tLineData for the chromosome and position line 1 10\tThis line should be long enough for this test purpose\t940393\tPNMB1\tMany\n", options);
    }

    @Test
    public void readingFromDictionaryWithFilterOnTagsSingleTagWithSource() throws IOException {
        GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dictForReadTest.dictionary, "PNMB1"));
        assertContent("Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData\tSource\tPN\n" +
                "chr1\t10\tPNMB1\tLineData for the chromosome and position line 1 10\tThis line should be long enough for this test purpose\t940393\tPNMB1\tMany\n", options);
    }

    @Test
    public void readingFromDictionaryWithFilterOnTagsInvalidTag() throws IOException {
        try {
            GorOptions options = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dictForReadTest.dictionary, "xxx"));
            assertLineCount(0, options);
            Assert.fail("Invalid tag should throw exception");
        } catch (GorDataException ex) {
            // Expected
        }
    }

    @Test
    public void canMergeDictWithRanges() throws IOException {
        String file1 = createGorFile("Chrom\tPos\tData", "chr1\t1\t1");
        String file2 = createGorFile("Chrom\tPos\tData", "chr1\t2000\t1");
        String dict = createDictFile(String.format("%s\tfirst\tchr1\t0\tchr1\t1000\n%s\tsecond\tchr1\t1001\tchr1\t3000\n", file1, file2));
        GorOptions options = GorOptions.createGorOptions(dict);
        assertContent("Chrom\tPos\tData\nchr1\t1\t1\nchr1\t2000\t1\n", options);
    }

    @Test
    public void canMergeDictWithRangesReverseOrder() throws IOException {
        String file1 = createGorFile("Chrom\tPos\tData", "chr1\t1\t1");
        String file2 = createGorFile("Chrom\tPos\tData", "chr1\t2000\t1");
        String dict = createDictFile(String.format("%s\tsecond\tchr1\t1001\tchr1\t3000\n%s\tfirst\tchr1\t0\tchr1\t1000\n", file2, file1));
        GorOptions options = GorOptions.createGorOptions(dict);
        assertContent("Chrom\tPos\tData\nchr1\t1\t1\nchr1\t2000\t1\n", options);
    }

    @Test
    public void canMergeDictWithRangesWithRangedData() throws IOException {
        String file1 = createGorFile("Chrom\tStart\tStop\tData", "chr1\t0\t100000000\t1");
        String file2 = createGorFile("Chrom\tStart\tStop\tData", "chr1\t0\t100000000\t2");
        String dict = createDictFile(String.format("%s\tsecond\tchr1\t1001\tchr1\t3000\n%s\tfirst\tchr1\t0\tchr1\t1000\n", file2, file1));
        GorOptions options = GorOptions.createGorOptions(dict);
        assertContent("Chrom\tStart\tStop\tData\nchr1\t0\t100000000\t2\nchr1\t0\t100000000\t1\n", options);
    }

    private String createGorFile(String header, String data) throws IOException {
        File file = File.createTempFile("UTestMergeIterator", ".gor");
        file.deleteOnExit();
        PrintStream printStream = new PrintStream(file);
        printStream.println(header);
        printStream.println(data);

        return file.getAbsolutePath();
    }

    private String createDictFile(String data) throws IOException {
        File file = File.createTempFile("UTestMergeIterator", ".gord");
        file.deleteOnExit();
        PrintStream printStream = new PrintStream(file);
        printStream.println(data);

        return file.getAbsolutePath();
    }

    private void assertLineCount(int expected, GorOptions options) throws IOException {
        List<GenomicIterator> genomicIterators = options.getIterators();
        try (MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            int counter = 0;
            while (mi.hasNext()) {
                mi.next();
                counter++;
            }

            assertEquals(expected, counter);
        }
    }

    private void assertContent(String expected, GorOptions options) throws IOException {
        StringBuffer buffer = new StringBuffer();
        List<GenomicIterator> genomicIterators = options.getIterators();
        try (MergeIterator mi = new MergeIterator(genomicIterators, options)) {
            buffer.append(String.join("\t", mi.getHeader()));
            buffer.append('\n');
            while (mi.hasNext()) {
                buffer.append(mi.next());
                buffer.append('\n');
            }
            assertEquals(expected, buffer.toString());
        }
    }

}