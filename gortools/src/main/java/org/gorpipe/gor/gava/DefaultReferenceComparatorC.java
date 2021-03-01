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

/**
 * Comparator to use to sort an integer array by reference to an array
 * of {@link Comparable}s, using the default ordering of the {@link Comparable}s.
 *
 * @author hersir
 * @version $Revision$ $Date$
 */
class DefaultReferenceComparatorC<T extends Comparable<T>> implements java.util.Comparator<Integer> {
    /**
     * The actual array.
     */
    private final T[] myActualArray;

    /**
     * Create a reference comparator with a given array.
     *
     * @param voActualArray The array of objects used for comparisons.
     */
    public DefaultReferenceComparatorC(T[] voActualArray) {
        myActualArray = voActualArray;
    }

    /**
     * Compare.
     *
     * @param i1 the first index
     * @param i2 the second index
     * @return the comparison result
     */
    public int compare(Integer i1, Integer i2) {
        return myActualArray[i1].compareTo(myActualArray[i2]);
    }
}
