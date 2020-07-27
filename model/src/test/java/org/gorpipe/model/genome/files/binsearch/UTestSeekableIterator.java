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

package org.gorpipe.model.genome.files.binsearch;

import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class UTestSeekableIterator {

    private final static int NOT_SO_BIG_NUMBER = 1_000;
    private final static int QUITE_BIG_NUMBER = 25_000;
    private final static int BIG_NUMBER = 100_000;

    private final static int SEED = 5;

    public static File workDir;

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.deleteDirectory(workDir);
    }

    @Parameterized.Parameter
    public TestFileGenerator testFileGenerator;

    @Parameterized.Parameters(name = "Test file: {0}")
    public static Collection<Object[]> data() throws IOException {
        workDir = Files.createTempDirectory("uTestSeekableIterator").toFile();
        final TestFileGenerator[] testFileGenerators = new TestFileGenerator[]{
                new TestFileGenerator("BASIC_GOR_FILE", workDir, 10,1, NOT_SO_BIG_NUMBER, false),
                new TestFileGenerator("NOT_SO_TRIVIAL_GOR_FILE", workDir, 10, 10, QUITE_BIG_NUMBER, false),
                new TestFileGenerator("GOR_FILE_WITH_LONG_LINES", workDir,10, 1, BIG_NUMBER, false),
                new TestFileGenerator("GOR_FILE_WITH_MANY_LINES", workDir,5, 100, NOT_SO_BIG_NUMBER, false),
                new TestFileGenerator("PATHOLOGICAL_GOR_FILE_1", workDir,5, 10, BIG_NUMBER, true),
                new TestFileGenerator("PATHOLOGICAL_GOR_FILE_2", workDir, 2, 37, BIG_NUMBER, true),
                new TestFileGenerator("PATHOLOGICAL_GOR_FILE_3", workDir, 2, 41, BIG_NUMBER, true)
        };
        for (TestFileGenerator testFileGenerator : testFileGenerators) {
            testFileGenerator.writeFile(false);
        }
        return Arrays.stream(testFileGenerators).map(testFile -> new Object[]{testFile}).collect(Collectors.toList());
    }

    private static boolean seek(String chr, int pos, SeekableIterator bsi) throws IOException {
        bsi.seek(new StringIntKey(chr, pos));
        return bsi.hasNext();
    }

    @Test
    public void testStream() throws IOException {

        final BufferedReader br = new BufferedReader(new FileReader(testFileGenerator.path));
        final SeekableIterator bsi = getNewBinarySearchIterator(testFileGenerator.path);

        final String wantedHeader = br.readLine();
        final String actualHeader = bsi.getHeader();
        Assert.assertEquals(wantedHeader, actualHeader);

        String wantedLine;
        while ((wantedLine = br.readLine()) != null) {
            Assert.assertTrue(bsi.hasNext());
            final String actualLine = bsi.getNextAsString();
            Assert.assertEquals(wantedLine, actualLine);
        }

        Assert.assertFalse(bsi.hasNext());
        br.close();
        bsi.close();
    }

    @Test
    public void seekAtBeginning() throws IOException {
        final SeekableIterator bsi = getNewBinarySearchIterator(testFileGenerator.path);
        Assert.assertTrue(seekAtBeginning(bsi));
        final String firstLine = bsi.getNextAsString();
        Assert.assertTrue(firstLine.substring(0, Math.min(firstLine.length(), 100)), firstLine.startsWith("chr1\t1\t0\t"));
        bsi.close();
    }

    private static boolean seekAtBeginning(SeekableIterator bsi) throws IOException {
        return seek("chr1", 0, bsi);
    }

    @Test
    public void seekBeyondEnd() throws IOException {
        final SeekableIterator bsi = getNewBinarySearchIterator(testFileGenerator.path);
        Assert.assertFalse(seekBeyondEnd(bsi));
        bsi.close();
    }

    static boolean seekBeyondEnd(SeekableIterator bsi) throws IOException {
        return seek("chrY", 1_000_000_000, bsi);
    }

    @Test
    public void seekAtDifferentPositionsAndThenBetween() throws IOException {
        final SeekableIterator bsi = getNewBinarySearchIterator(testFileGenerator.path);
        Assert.assertTrue(seek("chr4", testFileGenerator.posPerChr, bsi));
        Assert.assertTrue(seek("chr5", 2, bsi));
        Assert.assertTrue(seek("chr5", 1, bsi));
        bsi.close();
    }

    @Test
    public void seekAndStreamRepeatedly() throws IOException {
        final SeekableIterator bsi = getNewBinarySearchIterator(testFileGenerator.path);
        for (String chr : this.testFileGenerator.getChromosomes()) {
            for (int pos = 1; pos <= testFileGenerator.posPerChr; ++pos) {
                Assert.assertTrue(seek(chr, pos, bsi));
                for (int i = 0; i < testFileGenerator.linesPerKey; ++i) {
                    Assert.assertTrue(bsi.hasNext());
                    final String line = bsi.getNextAsString();
                    Assert.assertTrue(line.startsWith(chr + "\t" + pos + "\t" + i + "\t"));
                }
                Assert.assertTrue(seek(chr, pos, bsi));
                final String line = bsi.getNextAsString();
                Assert.assertTrue(line.startsWith(chr + "\t" + pos + "\t" + 0 + "\t"));
            }
        }
        bsi.close();
    }

    @Test
    public void seekVariousTimes() throws IOException {
        final SeekableIterator bsi = getNewBinarySearchIterator(testFileGenerator.path);
        for (String chr : this.testFileGenerator.getChromosomes()) {
            Assert.assertTrue(seek(chr, testFileGenerator.posPerChr, bsi));
            Assert.assertTrue(bsi.hasNext());
            Assert.assertTrue(seek(chr, testFileGenerator.posPerChr, bsi));
            Assert.assertTrue(bsi.hasNext());
        }
        bsi.close();
    }

    @Test
    public void testSeekAtEndAndThenAtExistingPosition() throws IOException {
        final SeekableIterator bsi = getNewBinarySearchIterator(testFileGenerator.path);
        Assert.assertFalse(seekBeyondEnd(bsi));
        Assert.assertTrue(seekAtBeginning(bsi));
        Assert.assertTrue(bsi.hasNext());
        Assert.assertTrue(bsi.getNextAsString().startsWith("chr1\t1\t0"));
        bsi.close();
    }

    @Test
    public void testSeekAtRandomPositions() throws IOException {
        final SeekableIterator bsi = getNewBinarySearchIterator(testFileGenerator.path);
        final List<String> chrs = this.testFileGenerator.getChromosomes();
        final Random r = new Random(SEED);
        Collections.shuffle(chrs, r);
        chrs.forEach(chr -> {
            try {
                final int pos = 1 + r.nextInt(testFileGenerator.posPerChr - 1);
                Assert.assertTrue(seek(chr, pos, bsi));
                Assert.assertTrue(bsi.hasNext());
                final String line = bsi.getNextAsString();
                Assert.assertTrue(line.startsWith(chr + "\t" + pos + "\t0"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void testSeekAtPositionsInCache() throws IOException {
        testSeekAtRandomPositions();
        final SeekableIterator bsi = getNewBinarySearchIterator(testFileGenerator.path);
        final PositionCache cache = PositionCache.getFilePositionCache(bsi, testFileGenerator.path, testFileGenerator.path, testFileGenerator.offset, testFileGenerator.size);
        final List<StringIntKey> keysInCache = Arrays.asList(cache.getKeysInCache());
        Collections.shuffle(keysInCache, new Random(SEED));
        keysInCache.forEach(key -> {
            try {
                final StringIntKey k = (StringIntKey) key;
                bsi.seek(key);
                Assert.assertTrue(bsi.hasNext());
                Assert.assertTrue(bsi.getNextAsString().startsWith(k.chr + "\t" + k.bpair + "\t0"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void testSeekAtLastPosition() throws IOException {
        final SeekableIterator si = getNewBinarySearchIterator(testFileGenerator.path);
        seekAtLastPos(si);
    }

    private void seekAtLastPos(SeekableIterator si) throws IOException {
        final StringIntKey lastPos = testFileGenerator.getLastKey();
        si.seek(lastPos);
        Assert.assertTrue(si.hasNext());
        Assert.assertTrue(si.getNextAsString().startsWith(lastPos.chr + "\t" + lastPos.bpair));
    }

    @Test
    public void seekTwiceAtSamePos() throws IOException {
        final SeekableIterator si = getNewBinarySearchIterator(testFileGenerator.path);
        seekAtLastPos(si);
        seekAtLastPos(si);

        seekBeyondEnd(si);
        seekBeyondEnd(si);

        seekAtBeginning(si);
        seekAtBeginning(si);
    }

    private SeekableIterator getNewBinarySearchIterator(String path) throws IOException {
        return new SeekableIterator(new StreamSourceSeekableFile(new FileSource(new SourceReference(path))), true);
    }
}
