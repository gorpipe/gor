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

package org.gorpipe.gor.gava;

import org.junit.Assert;
import org.junit.Test;

import static org.gorpipe.gor.gava.CombinationIterator.getNumberOfCombinations;

public class UTestCombinationIterator {
    private static final double EPSILON = 1e-12;

    @Test
    public void test_getNumberOfCombinations() {
        Assert.assertEquals(1, getNumberOfCombinations(0, 0), EPSILON);

        Assert.assertEquals(1, getNumberOfCombinations(1, 0), EPSILON);
        Assert.assertEquals(1, getNumberOfCombinations(1, 1), EPSILON);

        Assert.assertEquals(1, getNumberOfCombinations(2, 0), EPSILON);
        Assert.assertEquals(2, getNumberOfCombinations(2, 1), EPSILON);
        Assert.assertEquals(1, getNumberOfCombinations(2, 2), EPSILON);

        Assert.assertEquals(1, getNumberOfCombinations(3, 0), EPSILON);
        Assert.assertEquals(3, getNumberOfCombinations(3, 1), EPSILON);
        Assert.assertEquals(3, getNumberOfCombinations(3, 2), EPSILON);
        Assert.assertEquals(1, getNumberOfCombinations(3, 3), EPSILON);

        Assert.assertEquals(1, getNumberOfCombinations(4, 0), EPSILON);
        Assert.assertEquals(4, getNumberOfCombinations(4, 1), EPSILON);
        Assert.assertEquals(6, getNumberOfCombinations(4, 2), EPSILON);
        Assert.assertEquals(4, getNumberOfCombinations(4, 3), EPSILON);
        Assert.assertEquals(1, getNumberOfCombinations(4, 4), EPSILON);
    }

    @Test
    public void test_iterate_0() {
        final CombinationIterator ci = new CombinationIterator(0, 0);
        Assert.assertFalse(ci.next());
    }

    @Test
    public void test_iterate_1() {
        final CombinationIterator ci0 = new CombinationIterator(1, 0);
        Assert.assertTrue(ci0.next());
        Assert.assertArrayEquals(new int[] {0}, ci0.getPermutation());
        Assert.assertFalse(ci0.next());

        final CombinationIterator ci1 = new CombinationIterator(1, 1);
        Assert.assertTrue(ci1.next());
        Assert.assertArrayEquals(new int[] {0}, ci1.getPermutation());
        Assert.assertFalse(ci1.next());
    }

    @Test
    public void test_iterate_2() {
        final CombinationIterator ci0 = new CombinationIterator(2, 0);
        Assert.assertTrue(ci0.next());
        Assert.assertArrayEquals(new int[] {0, 1}, ci0.getPermutation());
        Assert.assertFalse(ci0.next());

        final CombinationIterator ci1 = new CombinationIterator(2, 1);
        Assert.assertTrue(ci1.next());
        Assert.assertArrayEquals(new int[] {0, 1}, ci1.getPermutation());
        Assert.assertTrue(ci1.next());
        Assert.assertArrayEquals(new int[] {1, 0}, ci1.getPermutation());
        Assert.assertFalse(ci1.next());

        final CombinationIterator ci2 = new CombinationIterator(2, 2);
        Assert.assertTrue(ci2.next());
        Assert.assertArrayEquals(new int[] {0, 1}, ci2.getPermutation());
        Assert.assertFalse(ci2.next());
    }

    @Test
    public void test_iterate_3() {
        final CombinationIterator ci0 = new CombinationIterator(3, 0);
        Assert.assertTrue(ci0.next());
        Assert.assertArrayEquals(new int[] {0, 1, 2}, ci0.getPermutation());
        Assert.assertFalse(ci0.next());

        final CombinationIterator ci1 = new CombinationIterator(3, 1);
        Assert.assertTrue(ci1.next());
        Assert.assertArrayEquals(new int[] {0, 1, 2}, ci1.getPermutation());
        Assert.assertTrue(ci1.next());
        Assert.assertArrayEquals(new int[] {1, 0, 2}, ci1.getPermutation());
        Assert.assertTrue(ci1.next());
        Assert.assertArrayEquals(new int[] {2, 0, 1}, ci1.getPermutation());
        Assert.assertFalse(ci1.next());

        final CombinationIterator ci2 = new CombinationIterator(3, 2);
        Assert.assertTrue(ci2.next());
        Assert.assertArrayEquals(new int[] {0, 1, 2}, ci2.getPermutation());
        Assert.assertTrue(ci2.next());
        Assert.assertArrayEquals(new int[] {0, 2, 1}, ci2.getPermutation());
        Assert.assertTrue(ci2.next());
        Assert.assertArrayEquals(new int[] {1, 2, 0}, ci2.getPermutation());
        Assert.assertFalse(ci2.next());

        final CombinationIterator ci3 = new CombinationIterator(3, 3);
        Assert.assertTrue(ci3.next());
        Assert.assertArrayEquals(new int[] {0, 1, 2}, ci3.getPermutation());
        Assert.assertFalse(ci3.next());
    }
}
