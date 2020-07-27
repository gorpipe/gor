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

package org.gorpipe.model.util;

import java.io.Serializable;
import java.util.Arrays;

/**
 * <p>Title: DoubleArray</p>
 * <p>Description:  DoubleArray is a dynamic array for double primitives. It follows the same
 * pattern as the Java Collection framework with an add method to add to the array
 * get and set routines for usages/modification.</p>
 *
 * @version $Id$
 */

public class DoubleArray implements Serializable, Cloneable {

    /**
     * Initialized empty array used in construction of empty array
     */
    private static final double[] EMPTY_ARRAY = new double[0];

    /**
     * The array holding them doubles
     */
    private double[] array;

    /**
     * Position of last double added to the array, means this
     * is equal to count of doubles in the array
     */
    private int pos = 0;

    /**
     * Construct a new empty DoubleArray
     */
    public DoubleArray() {
        array = EMPTY_ARRAY;
    }

    /**
     * Construct a new DoubleArray of the specified size
     *
     * @param initial_size The initial size of the DoubleArray to use
     */
    public DoubleArray(int initial_size) {
        array = new double[initial_size];
    }

    /**
     * Construct a new DoubleArray of the specified size
     *
     * @param values The values to add
     */
    public DoubleArray(double[] values) {
        this(values.length);
        add(values);
    }

    /**
     * Remove the last element from the array (this is O(1) op)
     */
    public void removeLast() {
        pos--;
    }

    /**
     * Appends a double to the double primitives array
     *
     * @param value The value to add to the array
     */
    public void add(double value) {
        if (pos >= capacity())
            grow((int) (Math.min((capacity() + 1L) * (capacity() + 1), Math.max(500000L, 2 * capacity()))));
        set(pos++, value);
    }

    /**
     * Appends a double to the double primitives array
     *
     * @param values An array of double values to add
     */
    public void add(double[] values) {
        if (pos + values.length >= capacity())
            grow(Math.max(size() + values.length,
                    (int) Math.min((capacity() + 1L) * (capacity() + 1), Math.max(500000L, 2 * capacity()))));
        for (double value : values)
            set(pos++, value);
    }

    /**
     * inserts a value at the specified index
     *
     * @param value the value to insert
     * @param index index to insert at This index must be greater than or equal to 0, and less than or equal to the
     *              length of this DoubleArray.
     * @throws ArrayIndexOutOfBoundsException if index is less than 0 or larger than size of array
     */
    public void insert(double value, int index) {
        int count = pos + 1;

        if (index < 0 || index > size()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        if (count > capacity()) {
            grow((int) Math.min((capacity() + 1L) * (capacity() + 1), Math.max(500000L, 2 * capacity())));
        }

        System.arraycopy(array, index, array, index + 1, pos - index);
        array[index] = value;
        pos = count;
    }

    /**
     * removes the value at the specified index from the array
     *
     * @param index index into array
     * @throws ArrayIndexOutOfBoundsException if the index
     *                                        is negative or greater than or equal to the size of the array
     */
    public void delete(int index) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        System.arraycopy(array, index + 1, array, index, size() - index - 1);

        pos--;
    }

    /**
     * Gives the median of the array
     *
     * @return The median of the current values in the arary
     */
    public double getMedian() {
        Arrays.sort(array, 0, size());
        int mid = size() / 2;
        if ((size() & 0x1) == 0x1)
            return array[mid];
        return (array[mid - 1] + array[mid]) / 2;
    }

    /**
     * Reserves a range of a particular length
     *
     * @param range_length the length of the range
     */
    public void reserveRange(int range_length) {
        if (pos + range_length > capacity())
            grow(pos + range_length);
        pos += range_length;
    }

    /**
     * sets value at index i to value
     *
     * @param i     the index
     * @param value the value to set
     */
    public void set(int i, double value) {
        if (i >= pos)
            throw new ArrayIndexOutOfBoundsException("Element " + (i + 1) + " at index " + i + " is referenced when size is " + size());
        array[i] = value;
    }

    /**
     * gets value at index
     *
     * @param i the index
     * @return The value at position i
     */
    public double get(int i) {
        return array[i];
    }

    /**
     * gives the size of the DoubleArray
     *
     * @return The number of values in the array
     */
    public int size() {
        return pos;
    }

    /**
     * Gives the size of the buffer that has been allocated for the
     * array of doubles
     *
     * @return The capacity of the array
     */
    public int capacity() {
        return array.length;
    }

    /**
     * Sorts the array of doubles into ascending numerical order.
     */
    public void sort() {
        Arrays.sort(array, 0, pos);
    }

    /**
     * Searches for the specified value using the binary search
     * algorithm.  The DoubleArray object must be sorted prior to
     * making this call.
     *
     * @param key the value to be searched for.
     * @return The index of the key being searced for, or -1 if not found
     */
    public int binarySearch(int key) {
        return Arrays.binarySearch(array, key);
    }

    /**
     * Truncates the size of the array buffer
     * to the used size
     */
    public void shrinkToFit() {
        if (size() != capacity()) array = toArray();
    }

    /**
     * Gives the array of double primitives
     *
     * @return A new array of double primitives with the content from
     * this and length equal to size
     */
    public double[] toArray() {
        double[] a = new double[pos];
        System.arraycopy(array, 0, a, 0, pos);
        return a;
    }

    /**
     * Gives the array of float primitives
     *
     * @return A new array of float primitives with the content from
     * this and length equal to size
     */
    public float[] toFloatArray() {
        float[] a = new float[pos];
        for (int i = 0; i < pos; i++)
            a[i] = (float) array[i];
        return a;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer((size() + 3) * 10);
        // Add size and capacity info
        sb.append("{'size=");
        sb.append(size());
        sb.append("','capacity=");
        sb.append(capacity());
        sb.append("'}[");

        // Add contents
        if (size() > 0) {
            sb.append(array[0]);
            for (int i = 1; i < size(); i++) {
                sb.append(',');
                sb.append(array[i]);
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Tells whether a given value exists in the double array
     *
     * @param element the value to look for
     * @return True if the value specified is found in the array
     */
    public boolean contains(int element) {
        for (int i = 0; i < pos; i++) {
            if (array[i] == element) {
                return true;
            }
        }
        return false;
    }

    private void grow(int amount) {
        double[] a = new double[amount];
        System.arraycopy(array, 0, a, 0, pos);
        array = a;
    }

    /**
     * change Double[] to double[]
     *
     * @param array
     * @return double[]
     */
    public static double[] toDoubleArray(Double[] array) {
        double[] retVal = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            retVal[i] = array[i].doubleValue();
        }
        return retVal;
    }


    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
