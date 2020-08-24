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
package org.gorpipe.model.gava;

import org.gorpipe.model.util.GLogGamma;

/**
 * Iterate through all combinations (n choose k) and provide permutations representing the combinations.
 * Designed to work in permutation tests, where instead of randomization all combinations are tested.
 *
 * @author gfj
 * @version $Id$
 */
public class CombinationIterator {

    private final int n;
    private final int k;
    private boolean[] included;
    private int j; // Index of last not included.
    private int i; // Index of last included before j.
    private int[] perm;

    /**
     * Instantiate the iterator.
     *
     * @param totalCount the total number of elements
     * @param subCount   the number of elements to choose
     */
    public CombinationIterator(int totalCount, int subCount) {
        n = totalCount;
        k = subCount;
        j = n - 1;
        i = j;
    }

    /**
     * @return a permutation representing the combination
     */
    public int[] getPermutation() {
        return perm;
    }

    /**
     * Advance to the next combination.
     * (First call stays in the initial combination.)
     *
     * @return true if there is a next combination
     */
    public boolean next() {
        if (included == null) {
            if (n <= 0 || k < 0 || k > n) return false;
            included = new boolean[n];
            perm = new int[n];
            for (int t = 0; t < n; ++t) {
                included[t] = (t < k);
                perm[t] = t;
            }
        } else {
            if (i < 0 || j < 0) return false;
            if (i < j - 1) {
                included[i] = false;
                included[++i] = true;
                while (j < n - 1) {
                    included[++j] = false;
                    included[++i] = true;
                }
            } else {
                included[i] = false;
                included[i + 1] = true;
            }
        }
        while (j >= 0 && included[j]) --j;
        while (i >= 0 && !included[i]) --i;

        int r = 0;
        int s = k;
        for (int t = 0; t < included.length; ++t) {
            if (included[t]) {
                perm[r++] = t;
            } else {
                perm[s++] = t;
            }
        }
        return true;
    }

    /**
     * Computes the number of combinations. Returns a double because rounding to int
     * will cause undesired results when the number is larger than max int.
     *
     * @param n total count
     * @param k sub count
     * @return the number of combinations (n choose k)
     */
    public static double getNumberOfCombinations(int n, int k) {
        return Math.exp(GLogGamma.get(n) - GLogGamma.get(k) - GLogGamma.get(n - k));
    }
}

