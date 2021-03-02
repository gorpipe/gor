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

package org.gorpipe.gor.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class UTestSelectIterator {

    @Test
    public void test_basic() {
        final GenomicIterator git = getIterator(1, 5).select(new int[]{0, 1, 3});
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr1\t1\tcol3", git.next().toString());
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr2\t1\tcol3", git.next().toString());
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr3\t1\tcol3", git.next().toString());
        Assert.assertFalse(git.hasNext());
    }

    @Test
    public void test_seek() {
        final GenomicIterator git = getIterator(1000, 5).select(new int[]{0, 1, 2, 4});
        Assert.assertTrue(git.seek("chr2", 500));
        Assert.assertEquals("chr2\t500\tcol2\tcol4", git.next().toString());
    }

    private static GenomicIterator getIterator(int positions, int cols) {
        final String[] chromosomes = IntStream.rangeClosed(1, 3).mapToObj(i -> "chr" + i).sorted().toArray(String[]::new);
        final Map<String, Integer> chrToIdx = new HashMap<>();
        for (int i = 0; i < chromosomes.length; ++i) {
            chrToIdx.put(chromosomes[i], i);
        }

        return new GenomicIteratorBase() {
            int chrIdx = 0;
            int posIdx = 1;

            @Override
            public boolean seek(String chr, int pos) {
                final int idx = chrToIdx.getOrDefault(chr, -1);
                if (idx == -1) {
                    chrIdx = chromosomes.length;
                    posIdx = 0;
                } else {
                    chrIdx = idx;
                    posIdx = pos;
                }
                return hasNext();
            }

            @Override
            public Row next() {
                final String chr = chromosomes[chrIdx];
                final int pos = posIdx;
                final StringBuilder sb = new StringBuilder();
                sb.append(chr).append('\t').append(pos);
                for (int i = 2; i < cols; ++i) {
                    sb.append("\tcol").append(i);
                }
                if (posIdx == positions) {
                    posIdx = 1;
                    chrIdx++;
                } else {
                    posIdx++;
                }
                return new RowBase(sb.toString());
            }

            @Override
            public boolean hasNext() {
                return chrIdx < chromosomes.length && posIdx <= positions;
            }

            @Override
            public void close() {}
        };
    }
}
