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

/**
 * Created on 12/5/2019.
 *
 * @author hjaltii
 */
public class OptimalDistribution {

    /**
     * Takes in numbers a and b and an array p which contains numbers between a and b in an increasing order.
     * The method finds those m indices i_1,...,i_m such that the sum
     *
     * (p_{i_1} - a)^2 + (p_{i_2} - p_{i_1})^2 + ... + (p_{i_m} - p_{i_{m-1}})^2 + (b - p_{i_m})^2
     *
     * is minimized. If m is greater than the length of p, n, we return {0, 1, ..., n - 1}.
     *
     * The idea of this algorithm is from Sigurður Jens Albertsson.
     */
    public static int[] getOptimalIndices(double a, double b, double[] p, int m) {
        verifyArguments(a, b, p, m);

        final int n = p.length;

        if (m == 0) {
            return new int[0];
        } else if (m >= n) {
            return rangeVector(n);
        }

        final int len = n - m + 1;
        double[] cr = new double[len];
        double[] lr = new double[len];

        for (int j = 0; j < len; ++j) {
            lr[j] = squareOf(p[j] - a);
        }

        final int[][] idxTrack = new int[m - 1][len];
        int[] idxTrack_i;

        for (int i = 0; i < m - 1; ++i) {
            idxTrack_i = idxTrack[i];
            int minIdx = 0;
            double minVal;
            for (int j = 0; j < len; ++j) {
                minVal = lr[minIdx] + squareOf(p[i + j + 1] - p[i + minIdx]);
                for (int k = minIdx + 1; k <= j; ++k) {
                    final double val = lr[k] + squareOf(p[i + j + 1] - p[i + k]);
                    if (val < minVal) {
                        minVal = val;
                        minIdx = k;
                    }
                }
                cr[j] = minVal;
                idxTrack_i[j] = minIdx;
            }
            final double[] tmp = lr;
            lr = cr;
            cr = tmp;
        }

        int minIdx = 0;
        double minVal = lr[0] + squareOf(b - p[m - 1]);
        for (int j = 1; j < len; ++j) {
            final double val = lr[j] + squareOf(b - p[j + m - 1]);
            if (val < minVal) {
                minVal = val;
                minIdx = j;
            }
        }

        final int[] indices = new int[m];
        indices[m - 1] = minIdx + m - 1;
        for (int i = m - 2; i != -1; --i) {
            indices[i] = idxTrack[i][indices[i + 1] - (i + 1)] + i;
        }
        return indices;
    }

    private static void verifyArguments(double a, double b, double[] p, int m) {
        if (p == null) {
            throw new IllegalArgumentException("p must be non-null");
        }
        if (a > b) {
            throw new IllegalArgumentException("a must be less than b");
        }
        if (m < 0) {
            throw new IllegalArgumentException("m must be non negative");
        }
    }

    /**
     * Returns the square of a.
     */
    private static double squareOf(double a) {
        return a * a;
    }

    /**
     * Returns the vector {0, 1, 2, ..., n - 1}.
     */
    private static int[] rangeVector(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative.");
        final int[] toReturn = new int[n];
        for (int i = 0; i < n; ++i) {
            toReturn[i] = i;
        }
        return toReturn;
    }
}
