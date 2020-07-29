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

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UTestRangeMergeIterator {
    public static File tempDir;
    public static int totalLineCountInSourceRefs;

    public static List<SourceRef> sourceRefs;
    public static List<SourceRef> badSourceRefs;
    public static List<SourceRef> emptySources;
    public static List<SourceRef> oneLineManySources;

    @BeforeClass
    public static void writeFiles() throws IOException {
        tempDir = Files.createTempDirectory("testrangemergeiterator").toFile();
        writeSourceRefs();
        writeBadSources();
        writeEmptySources();
        writeOneLineManyRanges();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);
    }

    private static void writeBadSources() throws IOException {
        badSourceRefs = new ArrayList<>();
        int sourceIdx = 1;
        for (int startPos = 1; startPos <= 5; ++startPos) {
            for (int stopPos = startPos; stopPos <= 5; ++stopPos) {
                final File source = new File(tempDir, startPos + "_" + stopPos + ".gor");
                final BufferedWriter bw = new BufferedWriter(new FileWriter(source));
                bw.write("CHROM\tPOS\tSOURCE\tROWCOPY\n");
                for (int pos = startPos; pos <= stopPos; ++pos) {
                    bw.write("chr1\t" + pos + "\t" + sourceIdx + "\t1\n");
                    bw.write("chr1\t" + pos + "\t" + sourceIdx + "\t2\n");
                }
                bw.close();
                sourceIdx++;
                badSourceRefs.add(new SourceRef(source.getAbsolutePath(), null, null, null, "chr1", startPos, "chr1", stopPos, null, false, null, null));
            }
        }
    }

    private static void writeSourceRefs() throws IOException {
        sourceRefs = new ArrayList<>();
        int count = 0;

        final Random r = new Random(17);
        final String[] chromosomes = IntStream.rangeClosed(1, 10).mapToObj(chr -> "chr" + chr).sorted().toArray(String[]::new);
        final int[] positions = IntStream.rangeClosed(1, 10).map(i -> r.nextInt(1_000_000)).toArray();
        final int posCount = 10;
        for (int i = 0; i < posCount; ++i) {
            for (int j = i + 1; j < posCount; ++j) {
                final String startChr = chromosomes[i];
                final int startPos = positions[i];
                final String stopChr = chromosomes[j];
                final int stopPos = positions[j];
                final File file = new File(tempDir, startChr + "_" + startPos + "_" + stopChr + "_" + stopPos + ".gor");
                final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write("CHROM\tPOS\tBLABLABLA\n");
                count += writeLinesOnChr(startChr, startPos, 1_000_000, bw, r);
                for (int k = i + 1; k < j; ++k) {
                    count += writeLinesOnChr(chromosomes[k], 1, 1_000_000, bw, r);
                }
                count += writeLinesOnChr(stopChr, 1, stopPos, bw, r);
                bw.close();
                sourceRefs.add(new SourceRef(file.getAbsolutePath(), null, null, null, startChr, startPos, stopChr, stopPos, null, false, null, null));
            }
        }
        totalLineCountInSourceRefs = count;
    }

    public static void writeEmptySources() throws IOException {
        final String[] chromosomes = IntStream.rangeClosed(1, 10).mapToObj(chr -> "chr" + chr).sorted().toArray(String[]::new);
        emptySources = new ArrayList<>();
        for (String chr : chromosomes) {
            final File file = new File(tempDir, "empty_" + chr + ".gor");
            final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write("CHROM\tPOS\tBLABLABLA\n");
            bw.close();
            emptySources.add(new SourceRef(file.getAbsolutePath(), null, null, null, chr, 1, chr, 1000000, null, false, null, null));
        }
    }

    public static void writeOneLineManyRanges() throws IOException {
        oneLineManySources = new ArrayList<>();
        for (int source_idx = 1; source_idx <= 10; ++source_idx) {
            final File oneLine = new File(tempDir, "oneLine_" + source_idx + ".gor");
            final BufferedWriter bw = new BufferedWriter(new FileWriter(oneLine));
            bw.write("Chrom\tPos\tSource\nchr1\t10\tsource" + source_idx + "\n");
            bw.close();
            oneLineManySources.add(new SourceRef(oneLine.getAbsolutePath(), null, null, null, "chr1", 2 * ((source_idx + 1) / 2), "chr1", 20, null, false, null, tempDir.getAbsolutePath()));
        }
    }

    public static int writeLinesOnChr(String chr, int begin, int end, BufferedWriter bw, Random r) throws IOException {
        int count = 0;
        int pos = begin + r.nextInt(100_000);
        while (pos < end) {
            bw.write(chr + "\t" + pos + "\tblablabla\n");
            pos += r.nextInt(100_000);
            count++;
        }
        return count;
    }

    @Test
    public void testStream() {
        final RangeMergeIterator rmit = new RangeMergeIterator(sourceRefs);
        final Iterator<String> expectedDummyRows = sourceRefs.stream()
                .map(sr -> RowBase.getProgressRow(sr.startChr, sr.startPos).toString()).distinct().iterator();
        String chr = "";
        int pos = 0;
        int count = 0;
        while (rmit.hasNext()) {
            final Row next = rmit.next();
            if (next.toString().endsWith("progress")) {
                Assert.assertTrue(expectedDummyRows.hasNext());
                Assert.assertEquals(expectedDummyRows.next(), next.toString());
            } else {
                count++;
            }
            final String nextChr = next.chr;
            final int nextPos = next.pos;
            Assert.assertTrue(compareKeys(chr, pos, nextChr, nextPos) < 0);
            chr = nextChr;
            pos = nextPos;
        }
        Assert.assertEquals(totalLineCountInSourceRefs, count);
    }

    @Test
    public void testSeekTwiceToSamePos() {
        final RangeMergeIterator rmit = new RangeMergeIterator(sourceRefs);
        final String wantedLine = "chr3\t837230\tblablabla";

        Assert.assertTrue(rmit.seek("chr3", 837230));
        Assert.assertEquals(wantedLine, rmit.next().toString());
        Assert.assertTrue(rmit.seek("chr3", 837230));
        Assert.assertEquals(wantedLine, rmit.next().toString());
        rmit.close();
    }

    @Test
    public void testSeekBackWards() {
        final RangeMergeIterator rmit = new RangeMergeIterator(sourceRefs);

        Assert.assertTrue(rmit.seek("chr9", 258872));
        Assert.assertEquals("chr9\t258872\tblablabla", rmit.next().toString());
        Assert.assertTrue(rmit.seek("chr5",812832));
        Assert.assertEquals("chr5\t812832\tblablabla", rmit.next().toString());
        Assert.assertTrue(rmit.seek("chr1"  ,647900));
        Assert.assertEquals("chr1\t647900\tblablabla", rmit.next().toString());
        rmit.close();
    }

    @Test
    public void testSeekBeyondEnd() {
        final RangeMergeIterator rmit = new RangeMergeIterator(sourceRefs);

        Assert.assertFalse(rmit.seek("chrX",  1));
        Assert.assertTrue(rmit.seek("chr1"  ,647900));
        Assert.assertEquals("chr1\t647900\tblablabla", rmit.next().toString());
        rmit.close();
    }

    @Test
    public void testStreamAndSeekBackWards() {
        final RangeMergeIterator rmit = new RangeMergeIterator(sourceRefs);

        Assert.assertTrue(rmit.seek("chr5",812832));
        for (int i = 0; i < 100; ++i) {
            Assert.assertTrue(rmit.hasNext());
            rmit.next();
        }
        Assert.assertTrue(rmit.seek("chr1"  ,647900));
        Assert.assertEquals("chr1\t647900\tblablabla", rmit.next().toString());
        rmit.close();
    }

    @Test
    public void testStreamBadSources() {
        final RangeMergeIterator rmit = new RangeMergeIterator(badSourceRefs);
        final Iterator expectedDummyRows = badSourceRefs.stream()
                .map(sr -> RowBase.getProgressRow(sr.startChr, sr.startPos).toString()).distinct().iterator();
        int count = 0;
        String chr = "";
        int pos = 0;
        int sourceIdx = 0;
        int rowCopy = 0;
        boolean lastDummy = false;
        while (rmit.hasNext()) {
            final Row next = rmit.next();
            final String nextChr = next.chr;
            final int nextPos = next.pos;
            final int cmp = compareKeys(chr, pos, nextChr, nextPos);
            Assert.assertTrue(cmp <= 0);
            if (next.toString().endsWith("progress")) {
                lastDummy = true;
                Assert.assertTrue(expectedDummyRows.hasNext());
                Assert.assertEquals(expectedDummyRows.next(), next.toString());
            } else {
                count++;
                final int nextSourceIdx = next.colAsInt(2);
                final int nextRowCopy = next.colAsInt(3);
                if (cmp == 0 && !lastDummy) {
                    Assert.assertTrue(sourceIdx < nextSourceIdx || (sourceIdx == nextSourceIdx && rowCopy < nextRowCopy));
                }
                lastDummy = false;
                sourceIdx = nextSourceIdx;
                rowCopy = nextRowCopy;
            }
            chr = nextChr;
            pos = nextPos;
        }
        Assert.assertEquals(70, count);
        rmit.close();
    }

    @Test
    public void testSeekToBadSources() {
        final RangeMergeIterator rmit = new RangeMergeIterator(badSourceRefs);
        final int[] positions = {5, 4, 3, 2, 1, 5, 3, 4, 1, 2};
        for (final int position : positions) {
            Assert.assertTrue(rmit.seek("chr1", position));
            Assert.assertEquals("chr1\t" + position + "\t" + position + "\t1", rmit.next().toString());
        }
        rmit.close();
    }

    @Test
    public void testSeekAndStreamRepeatedlyToBadSources() {
        final RangeMergeIterator rmit = new RangeMergeIterator(badSourceRefs);

        Assert.assertTrue(rmit.seek("chr1", 5));
        Assert.assertEquals("chr1\t5\t5\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t5\t5\t2", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t5\t9\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t5\t9\t2", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t5\t12\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t5\t12\t2", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t5\t14\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t5\t14\t2", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t5\t15\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t5\t15\t2", rmit.next().toString());
        Assert.assertFalse(rmit.hasNext());

        Assert.assertTrue(rmit.seek("chr1", 1));
        Assert.assertEquals("chr1\t1\t1\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t1\t1\t2", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t1\t2\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t1\t2\t2", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t1\t3\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t1\t3\t2", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t1\t4\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t1\t4\t2", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t1\t5\t1", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr1\t1\t5\t2", rmit.next().toString());
        rmit.close();
    }

    @Test
    public void testSeekTwiceToBadSources() {
        final RangeMergeIterator rmit = new RangeMergeIterator(badSourceRefs);
        Assert.assertTrue(rmit.seek("chr1", 3));
        Assert.assertTrue(rmit.seek("chr1", 3));
        Assert.assertEquals("chr1\t3\t3\t1", rmit.next().toString());
        Assert.assertTrue(rmit.seek("chr1", 3));
        Assert.assertEquals("chr1\t3\t3\t1", rmit.next().toString());

        Assert.assertTrue(rmit.seek("chr1", 5));
        Assert.assertTrue(rmit.seek("chr1", 5));
        Assert.assertEquals("chr1\t5\t5\t1", rmit.next().toString());
        Assert.assertTrue(rmit.seek("chr1", 5));
        Assert.assertEquals("chr1\t5\t5\t1", rmit.next().toString());
        rmit.close();
    }

    @Test
    public void test_getDummyWhenSeekingBeforeRange() {
        final List<SourceRef> myRefs = sourceRefs.stream().filter(sr -> sr.startChr.compareTo("chr1") > 0).collect(Collectors.toList());
        final RangeMergeIterator rmit = new RangeMergeIterator(myRefs);
        Assert.assertTrue(rmit.seek("chr1", 1));
        Assert.assertEquals("chr10\t306220\tprogress", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr10\t313611\tblablabla", rmit.next().toString());
        rmit.close();
    }

    @Test
    public void test_getDummyWhenSeeking() {
        final List<SourceRef> myRefs = sourceRefs.stream().filter(sr -> sr.startChr.compareTo("chr1") > 0).collect(Collectors.toList());
        Assert.assertTrue(myRefs.addAll(emptySources));
        final RangeMergeIterator rmit = new RangeMergeIterator(myRefs);
        Assert.assertTrue(rmit.seek("chr1", 1));
        Assert.assertEquals("chr10\t1\tprogress", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr10\t306220\tprogress", rmit.next().toString());
        Assert.assertTrue(rmit.hasNext());
        Assert.assertEquals("chr10\t313611\tblablabla", rmit.next().toString());
        rmit.close();
    }

    @Test
    public void test_sameLineManyRanges_stream() {
        final GenomicIterator git = new RangeMergeIterator(oneLineManySources);
        for (int i = 0; i < 5; ++i) {
            Assert.assertTrue(git.hasNext());
            final Row r = git.next();
            Assert.assertTrue(r.isProgress);
            Assert.assertEquals("chr1\t" + (i + 1) * 2 + "\tprogress", r.toString());
        }
        for (int source_idx = 1; source_idx <= 10; ++source_idx) {
            Assert.assertTrue(git.hasNext());
            Assert.assertEquals("chr1\t10\tsource" + source_idx, git.next().toString());
        }
    }

    @Test
    public void test_sameLineManyRanges_seek() {
        final GenomicIterator git = new RangeMergeIterator(oneLineManySources);
        Assert.assertFalse(git.seek("chr1", 11));

        Assert.assertTrue(git.seek("chr1", 1));
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr1\t2\tprogress", git.next().toString());
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr1\t4\tprogress", git.next().toString());
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr1\t6\tprogress", git.next().toString());
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr1\t8\tprogress", git.next().toString());
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr1\t10\tprogress", git.next().toString());

        Assert.assertTrue(git.seek("chr1", 10));
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr1\t10\tsource1", git.next().toString());
    }

    @Test
    public void test_filterProgressRows() {
        final GenomicIterator git = new RangeMergeIterator(badSourceRefs).filter(r -> !r.isProgress);
        while (git.hasNext()) {
            Assert.assertFalse(git.next().isProgress);
        }
    }

    private static int compareKeys(String chr1, int pos1, String chr2, int pos2) {
        final int chrCmp = chr1.compareTo(chr2);
        if (chrCmp != 0) return chrCmp;
        return Integer.compare(pos1, pos2);
    }
}
