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

package org.gorpipe.gor.binsearch;

import org.gorpipe.gor.model.GenomicIterator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@RunWith(Parameterized.class)
public abstract class UTestSeekableGenomicIterator {
    protected final static int NOT_SO_BIG_NUMBER = 1_000;
    protected final static int QUITE_BIG_NUMBER = 25_000;
    protected final static int BIG_NUMBER = 100_000;

    final static int SEED = 5;

    @Parameterized.Parameter
    public TestFileGenerator testFileGenerator;

    public abstract GenomicIterator getIterator(String path);

    @Test
    public void seekAtBeginning() {
        final GenomicIterator bsi = getIterator(testFileGenerator.path);
        Assert.assertTrue(seekAtBeginning(bsi));
        final String firstLine = bsi.next().toString();
        Assert.assertTrue(firstLine.substring(0, Math.min(firstLine.length(), 100)), firstLine.startsWith("chr1\t1\t0\t"));
        bsi.close();
    }

    private static boolean seekAtBeginning(GenomicIterator bsi) {
        return bsi.seek("chr1", 0);
    }

    @Test
    public void seekBeyondEnd() {
        final GenomicIterator bsi = getIterator(testFileGenerator.path);
        Assert.assertFalse(seekBeyondEnd(bsi));
        bsi.close();
    }

    static boolean seekBeyondEnd(GenomicIterator bsi) {
        return bsi.seek("chrY", 1_000_000_000);
    }

    @Test
    public void seekAtDifferentPositionsAndThenBetween() {
        final GenomicIterator bsi = getIterator(testFileGenerator.path);
        Assert.assertTrue(bsi.seek("chr4", testFileGenerator.posPerChr));
        Assert.assertTrue(bsi.seek("chr5", 2));
        Assert.assertTrue(bsi.seek("chr5", 1));
        bsi.close();
    }

    @Test
    public void seekAndStreamRepeatedly() {
        final GenomicIterator bsi = getIterator(testFileGenerator.path);
        for (String chr : this.testFileGenerator.getChromosomes()) {
            for (int pos = 1; pos <= testFileGenerator.posPerChr; ++pos) {
                Assert.assertTrue(bsi.seek(chr, pos));
                for (int i = 0; i < testFileGenerator.linesPerKey; ++i) {
                    Assert.assertTrue(bsi.hasNext());
                    final String line = bsi.next().toString();
                    Assert.assertTrue(line.startsWith(chr + "\t" + pos + "\t" + i + "\t"));
                }
                Assert.assertTrue(bsi.seek(chr, pos));
                final String line = bsi.next().toString();
                Assert.assertTrue(line.startsWith(chr + "\t" + pos + "\t" + 0 + "\t"));
            }
        }
        bsi.close();
    }

    @Test
    public void seekVariousTimes() {
        final GenomicIterator bsi = getIterator(testFileGenerator.path);
        for (String chr : this.testFileGenerator.getChromosomes()) {
            Assert.assertTrue(bsi.seek(chr, testFileGenerator.posPerChr));
            Assert.assertTrue(bsi.hasNext());
            Assert.assertTrue(bsi.seek(chr, testFileGenerator.posPerChr));
            Assert.assertTrue(bsi.hasNext());
        }
        bsi.close();
    }

    @Test
    public void testSeekAtEndAndThenAtExistingPosition() {
        final GenomicIterator bsi = getIterator(testFileGenerator.path);
        Assert.assertFalse(seekBeyondEnd(bsi));
        Assert.assertTrue(seekAtBeginning(bsi));
        Assert.assertTrue(bsi.hasNext());
        Assert.assertTrue(bsi.next().toString().startsWith("chr1\t1\t0"));
        bsi.close();
    }

    @Test
    public void testSeekAtRandomPositions() {
        final GenomicIterator bsi = getIterator(testFileGenerator.path);
        List<String> chrs = this.testFileGenerator.getChromosomes();
        final Random r = new Random(SEED);
        Collections.shuffle(chrs, r);
        chrs.forEach(chr -> {
            final int pos = 1 + r.nextInt(testFileGenerator.posPerChr - 1);
            Assert.assertTrue(bsi.seek(chr, pos));
            Assert.assertTrue(bsi.hasNext());
            final String line = bsi.next().toString();
            Assert.assertTrue(line.startsWith(chr + "\t" + pos + "\t0"));
        });
    }

    @Test
    public void testSeekAtPositionsInCache() {
        testSeekAtRandomPositions();
        final GenomicIterator bsi = getIterator(testFileGenerator.path);
        final PositionCache cache = PositionCache.getFilePositionCache(bsi, testFileGenerator.path, testFileGenerator.path, testFileGenerator.offset, testFileGenerator.size);
        final List<StringIntKey> keysInCache = Arrays.asList(cache.getKeysInCache());
        Collections.shuffle(keysInCache, new Random(SEED));
        keysInCache.forEach(key -> {
            final StringIntKey k = (StringIntKey) key;
            Assert.assertTrue(bsi.seek(k.chr, k.bpair));
            Assert.assertTrue(bsi.hasNext());
            Assert.assertTrue(bsi.next().toString().startsWith(k.chr + "\t" + k.bpair + "\t0"));
        });
    }
}
