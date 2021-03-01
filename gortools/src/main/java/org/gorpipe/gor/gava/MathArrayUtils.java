/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import java.util.Comparator;

/**
 * Provides various useful functions on arrays.
 */
public class MathArrayUtils {

    private MathArrayUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Convert a double array to a Double array.
     *
     * @param doubleArray the double array
     * @return an array of Double objects
     */
    private static Double[] toDoubleArray(double[] doubleArray) {
        Double[] result = new Double[doubleArray.length];
        for (int i = 0; i < doubleArray.length; i++) {
            result[i] = doubleArray[i];
        }
        return result;
    }

    /**
     * Find a permutation that puts an array in increasing order.
     *
     * @param <T>
     * @param voToSort the array to be sorted.
     * @return a permutation that puts voToSort in increasing order.
     * That is, if
     * <code>result</code> is the returned <code>int[]</code> array
     * then
     * <code>voToSort[result[i]], i = 0, 1, ..., n-1</code>
     * is increasing.
     */
    private static <T extends Comparable<T>> Integer[] getSortingPermutation(T[] voToSort) {

        Integer[] vnIndices = new Integer[voToSort.length];

        for (int i = 0; i < vnIndices.length; ++i) {
            vnIndices[i] = i;
        }

        Comparator<Integer> referenceComparator = new DefaultReferenceComparatorC<T>(voToSort);
        java.util.Arrays.sort(vnIndices, referenceComparator);

        return vnIndices;
    }

    /**
     * Find a permutation that puts d in increasing order.
     *
     * @param d the d
     * @return a permutation that puts d in increasing order
     */
    static int[] getSortingPermutationInt(double[] d) {
        return getSortingPermutationInt(toDoubleArray(d));
    }

    /**
     * Same as {@link #getSortingPermutation(Comparable[])} but returning <code>int[]</code>.
     *
     * @param <T>
     * @param voToSort the vo to sort
     * @return a permutation that puts voToSort in increasing order.
     */
    private static <T extends Comparable<T>> int[] getSortingPermutationInt(T[] voToSort) {
        return toIntArray(getSortingPermutation(voToSort));
    }

    /**
     * change Integer[] to int[]
     *
     * @param array
     * @return int[]
     */
    private static int[] toIntArray(Integer[] array) {
        int[] retVal = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            retVal[i] = array[i];
        }

        return retVal;
    }
}
