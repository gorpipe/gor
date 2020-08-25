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

package org.gorpipe.gor.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class UTestOptimalDistribution {

    @Test
    public void testOptimalDistribution_GeneralCases() {
        final double a = 0;
        final double b = 100;

        for (int n = 1; n <= 10; ++n) {
            final double[] p = new double[n];
            for (int i = 0; i < 10; ++i) {
                getRandomVector(a, b, p);
                Arrays.sort(p);
                testOptimalDistribution(a, b, p);
            }
        }
    }

    @Test
    public void testOptimalDistribution_NegativeM() {
        boolean success = false;
        try {
            OptimalDistribution.getOptimalIndices(0, 1, new double[0], -1);
        } catch (IllegalArgumentException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void testOptimalDistribution_NullArray() {
        boolean success = false;
        try {
            OptimalDistribution.getOptimalIndices(0, 1, null, 0);
        } catch (IllegalArgumentException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void testOptimalDistribution_bLessThanA() {
        boolean success = false;
        try {
            OptimalDistribution.getOptimalIndices(1, 0, new double[0], 0);
        } catch (IllegalArgumentException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    private static void testOptimalDistribution(double a, double b, double[] p) {
        for (int m = 0; m <= p.length; ++m) {
            final int[] wanted = getOptimalDistributionSlow(a, b, p, m);
            final int[] actual = OptimalDistribution.getOptimalIndices(a, b, p, m);
            Assert.assertArrayEquals(wanted, actual);
        }
    }

    private static int[] getOptimalDistributionSlow(double a, double b, double[] p, int m) {
        final int[] optIdx = new int[m];
        double minCost = Double.POSITIVE_INFINITY;

        final int[] currIdx = new int[m];
        double cost;

        final CombinationIterator ci = new CombinationIterator(p.length, m);

        while (ci.hasNext()) {
            ci.next(currIdx);
            cost = getCost(a, b, p, currIdx);
            if (cost < minCost) {
                minCost = cost;
                System.arraycopy(currIdx, 0, optIdx, 0, m);
            }
        }
        return optIdx;
    }

    private static double getCost(double a, double b, double[] p, int[] idx) {
        if (idx.length == 0) {
            return squareOf(b - a);
        } else {
            double sum = squareOf(p[idx[0]] - a) + squareOf(b - p[idx[idx.length - 1]]);
            for (int i = 1; i < idx.length; ++i) {
                sum += squareOf(p[idx[i]] - p[idx[i - 1]]);
            }
            return sum;
        }
    }

    private static double squareOf(double a) {
        return a * a;
    }

    private static void getRandomVector(double a, double b, double[] p) {
        final double d = b - a;
        for (int i = 0; i < p.length; ++i) {
            p[i] = a + d * Math.random();
        }
    }
}
