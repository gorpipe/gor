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

package org.gorpipe.util.collection;

import java.io.Serializable;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * IntArray is a dynamic array for integer primitives. It follows the same
 * pattern as the Java Collection framework with an add method to add to the array
 * get and set routines for usages/modification.
 *
 * @version $Id$
 */
public final class IntArray implements Serializable, Iterable<Integer> {

    /**
     * Initialized empty array used in construction of empty array
     */
    private static final int[] EMPTY_ARRAY = new int[0];

    /**
     * The array holding them integers
     */
    private int[] array;

    /**
     * Position of last integer added to the array, means this
     * is equal to count of ints in the array
     */
    private int pos = 0;

    /**
     * Construct a new empty IntArray
     */
    public IntArray() {
        array = EMPTY_ARRAY;
    }

    /**
     * Copy constructor to create a new IntArray from the source IntArray
     *
     * @param source IntArray to copy from
     */
    public IntArray(IntArray source) {
        array = new int[source.array.length];
        System.arraycopy(source.array, 0, this.array, 0, source.array.length);
        pos = source.pos;
    }

    /**
     * Construct a new IntArray of the specified size
     *
     * @param initial_size The size of the array
     */
    public IntArray(int initial_size) {
        array = new int[initial_size];
    }

    /**
     * Create an instance of IntArray that contains the mapping of every element in the stream
     *
     * @param mapper The mapper function, extracting the integer value from the element
     * @param stream The stream to read elements from
     */
    public <T> IntArray(ToIntFunction<T> mapper, Stream<T> stream) {
        this();
        add(mapper, stream);
    }

    /**
     * Create an instance of IntArray that contains the mapping of every element in the stream
     *
     * @param stream The stream to read elements from
     */
    public IntArray(IntStream stream) {
        this();
        add(stream);
    }

    /**
     * Create an instance of IntArray that contains the mapping of every element in the provided array
     *
     * @param mapper The mapper function, extracting the integer value from the element
     * @param values The array to read elements from
     */
    @SafeVarargs
    public <T> IntArray(ToIntFunction<T> mapper, T... values) {
        this((int) Math.ceil(values.length / 0.75f));
        for (int i = 0; i < values.length; i++) {
            set(i, mapper.applyAsInt(values[i]));
        }
    }

    /**
     * Appends an int to the int primitives array
     *
     * @param value The value to add (at the end of the array)
     */
    public void add(int value) {
        if (pos >= capacity())
            grow((int) (Math.min((capacity() + 1L) * (capacity() + 1), Math.max(500000L, 2 * capacity()))));
        set(pos++, value);
    }

    /**
     * Add the specifed collection of values to the array
     *
     * @param values
     */
    public void add(Collection<Integer> values) {
        for (Integer value : values) {
            add(value);
        }
    }

    /**
     * Add the mapping of every element in the stream
     *
     * @param stream The stream to read elements from
     */
    public void add(IntStream stream) {
        stream.forEach(v -> add(v));
    }

    /**
     * @return IntStream instance to convert this array into a stream
     */
    public IntStream stream() {
        if (size() == 0) {
            return StreamSupport.intStream(Spliterators.emptyIntSpliterator(), false);
        } else {
            return StreamSupport.intStream(new Spliterator.OfInt() {
                private int next = 0;

                @Override
                public boolean tryAdvance(IntConsumer consumer) {
                    Objects.requireNonNull(consumer);
                    if (next < size()) {
                        consumer.accept(get(next++));
                        return true;
                    }
                    return false;
                }

                @Override
                public void forEachRemaining(IntConsumer consumer) {
                    Objects.requireNonNull(consumer);
                    while (next < size()) {
                        consumer.accept(get(next++));
                    }
                }

                @Override
                public long estimateSize() {
                    return size() - next;
                }

                @Override
                public int characteristics() {
                    return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
                            Spliterator.IMMUTABLE | Spliterator.NONNULL |
                            Spliterator.DISTINCT | Spliterator.SORTED;
                }

                @Override
                public Comparator<? super Integer> getComparator() {
                    return null;
                }

                @Override
                public Spliterator.OfInt trySplit() {
                    return null;
                }
            }, false);
        }
    }

    /**
     * Add the mapping of every element in the stream
     *
     * @param mapper The mapper function, extracting the integer value from the element
     * @param stream The stream to read elements from
     * @param <T>    The element type
     */
    public <T> void add(ToIntFunction<T> mapper, Stream<T> stream) {
        stream.forEach(v -> add(mapper.applyAsInt(v)));
    }

    /**
     * Add the mapping of every element in the array
     *
     * @param mapper The mapper function, extracting the integer value from the element
     * @param values The array to read elements from
     * @param <T>    The element type
     */
    @SafeVarargs
    public final <T> void add(ToIntFunction<T> mapper, T... values) {
        for (T v : values) {
            add(mapper.applyAsInt(v));
        }
    }

    /**
     * Create a primitive array containing all the mapped integers from the items
     *
     * @param mapper The mapping function to apply to each item
     * @param values The items to work with
     * @return A primitive int array with all the mapped values
     */
    @SafeVarargs
    public static <T> int[] toArray(ToIntFunction<T> mapper, T... values) {
        int[] array = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = mapper.applyAsInt(values[i]);
        }
        return array;
    }

    /**
     * Create a primitive array containing all the mapped integers from the stream
     *
     * @param mapper The mapping function to apply to each item
     * @param stream The stream to work with
     * @return A primitive int array with all the mapped values
     */
    public static <T> int[] toArray(ToIntFunction<T> mapper, Stream<T> stream) {
        return new IntArray(mapper, stream).toArray();
    }

    /**
     * Clear the array so new elements will be added at the beginning
     */
    public void clear() {
        pos = 0;
    }

    /**
     * Remove the last element from the array (this is O(1) op)
     */
    public void removeLast() {
        pos--;
    }

    /**
     * Appends an array of ints to the int primitives array
     *
     * @param values An array of int values to add to the IntArray
     */
    public void add(int[] values) {
        if (pos + values.length >= capacity())
            grow(Math.max(size() + values.length,
                    (int) Math.min((capacity() + 1L) * (capacity() + 1), Math.max(500000L, 2 * capacity()))));
        for (int value : values)
            set(pos++, value);
    }

    /**
     * Appends an IntBuffer to the int primitives array
     *
     * @param values An IntBuffer to add to the IntArray
     */
    public void add(IntBuffer values) {
        if (pos + values.capacity() >= capacity())
            grow(Math.max(size() + values.capacity(),
                    (int) Math.min((capacity() + 1L) * (capacity() + 1), Math.max(500000L, 2 * capacity()))));
        for (int i = 0; i < values.capacity(); i++)
            set(pos++, values.get(i));
    }

    /**
     * inserts a value at the specified index
     *
     * @param value the value to insert
     * @param index index to insert at
     *              This index must be greater than or equal to
     *              0, and less than or equal to the length of this
     *              IntArray.
     * @throws ArrayIndexOutOfBoundsException if index is out of range
     */
    public void insert(int value, int index) {
        int count = pos + 1;

        if (index < 0 || index > size()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        if (count > capacity()) {
            grow((int) Math.min((capacity() + 1L) * (capacity() + 1), Math.max(500000L, 2 * capacity())));
        }

        System.arraycopy(array, index, array, index + 1, size() - index);
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
    public void set(int i, int value) {
        if (i >= pos)
            throw new ArrayIndexOutOfBoundsException("Element " + (i + 1) + " at index " + i + " is referenced when size is " + size());
        array[i] = value;
    }

    /**
     * gets value at index
     *
     * @param i the index
     * @return The value at index i
     */
    public int get(int i) {
        return array[i];
    }

    /**
     * @return The value in the last position
     */
    public int getLast() {
        assert pos > 0;
        return array[pos - 1];
    }

    /**
     * Increment the last element of one
     */
    public void incrementLast() {
        assert pos > 0;
        array[pos - 1]++;
    }

    /**
     * Increment the last element of one
     *
     * @param idx The index of the element to increment
     */
    public void increment(int idx) {
        assert idx >= 0 && idx < pos;
        array[idx]++;
    }


    /**
     * gives the size of the IntArray
     *
     * @return The element count of the IntArray
     */
    public int size() {
        return pos;
    }

    /**
     * Gives the size of the buffer that has been allocated for the array of ints
     *
     * @return The capacity of the internal buffer
     */
    public int capacity() {
        return array.length;
    }

    /**
     * Sorts the array of ints into ascending numerical order.
     */
    public void sort() {
        shrinkToFit();
        Arrays.sort(array);
    }

    /**
     * Searches for the specified value using the binary search
     * algorithm.  The IntArray object must be sorted prior to
     * making this call.
     *
     * @param key the value to be searched for.
     * @return The index of the specified key or -1 if not found
     */
    public int binarySearch(int key) {
        return Arrays.binarySearch(array, key);
    }

    /**
     * Truncates the size of the array
     * to the used size
     */
    public void shrinkToFit() {
        if (size() != capacity()) array = toArray();
    }

    /**
     * Gives the array of int primitives
     *
     * @return An array of int primitives with all the values from the IntArray,
     * with length=size()
     */
    public int[] toArray() {
        int[] a = new int[pos];
        System.arraycopy(array, 0, a, 0, pos);
        return a;
    }

    /**
     * Gives the array of byte primitives
     *
     * @return An array of byte primitives with all the values from the IntArray,
     * with length=size()
     */
    public byte[] toByteArray() {
        byte[] a = new byte[pos];
        for (int i = 0; i < a.length; i++) {
            a[i] = (byte) array[i];
        }
        return a;
    }

    /**
     * Gives the array of byte primitives
     *
     * @return An array of byte primitives with all the values from the IntArray,
     * with length=size()
     */
    public short[] toShortArray() {
        short[] a = new short[pos];
        for (int i = 0; i < a.length; i++) {
            a[i] = (short) array[i];
        }
        return a;
    }


    /**
     * Conver the underlying ints to characters and return as character array
     *
     * @return The char array
     */
    public char[] toCharArray() {
        char[] a = new char[pos];
        for (int i = 0; i < pos; i++) {
            a[i] = (char) array[i];
        }
        return a;
    }

    /**
     * Calculate the sum of all the elements in the array
     *
     * @return The sum of the elements
     */
    public int sum() {
        int sum = 0;
        for (int i = 0; i < size(); i++) {
            sum += array[i];
        }
        return sum;
    }

    /**
     * Gives an IntBuffer
     *
     * @return An IntBuffer with all the values from the IntArray,
     * with capacity()=size()
     */
    public IntBuffer toBuffer() {
        int[] a = new int[pos];
        System.arraycopy(array, 0, a, 0, pos);
        return IntBuffer.wrap(a);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder((size() + 3) * 10);
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
     * Tells whether a given value exists in the int array
     *
     * @param element the value to look for
     * @return True if the element is found, else false
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
        int[] a = new int[amount];
        System.arraycopy(array, 0, a, 0, pos);
        array = a;
    }

    /**
     * Merger the two specified arrays into one, with the content of a preceding b.
     *
     * @param a The first array
     * @param b The second array
     * @param c The new array to contain a and b
     * @return The new array containing a and b
     */
    public static int[] merge(int[] a, int[] b, int[] c) {
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    /**
     * Enumerate an array of longs and return the enumartion in a map.
     *
     * @param keys
     * @return A map of the long value to the index
     */
    public static Map<Long, Integer> enumerate(long[] keys) {
        HashMap<Long, Integer> map = new HashMap<Long, Integer>();
        for (int i = 0; i < keys.length; i++)
            map.put(Long.valueOf(keys[i]), Integer.valueOf(i));
        return map;
    }

    /**
     * Enumerate an array of integers and return the enumartion in a map.
     *
     * @param keys
     * @return An map of the integer value to its index
     */
    public static Map<Integer, Integer> enumerate(int[] keys) {
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (int i = 0; i < keys.length; i++)
            map.put(Integer.valueOf(keys[i]), Integer.valueOf(i));
        return map;
    }

    /**
     * change Integer[] to int[]
     *
     * @param array
     * @return int[]
     */
    public static int[] toIntArray(Integer[] array) {
        int[] retVal = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            retVal[i] = array[i].intValue();
        }

        return retVal;
    }

    /**
     * change int[] to Integer[]
     *
     * @param array
     * @return int[]
     */
    public static Integer[] toIntegerArray(int[] array) {
        final Integer[] retVal = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            retVal[i] = array[i];
        }

        return retVal;
    }

    /**
     * change Integer Collection to int[]
     *
     * @param col
     * @return int[]
     */
    public static int[] toIntArray(Collection<Integer> col) {
        int[] retVal = new int[col.size()];
        int i = 0;
        for (int val : col) {
            retVal[i++] = val;
        }

        return retVal;
    }

    /**
     * Reverse the values in the specified array, such that value at a[0] is at a[len-1], a[1] is at a[len-2], etc.
     *
     * @param a The array to reverse content of
     */
    public static void reverse(int[] a) {
        // Reverse the sort order
        final int lastIdx = a.length - 1;
        final int half = a.length / 2;
        for (int l = 0; l < half; l++) {
            final int r = lastIdx - l;
            final int t = a[r];
            a[r] = a[l];
            a[l] = t;
        }
    }

    /**
     * Create an array with the integer sequence starting at start, upto start + length - 1
     *
     * @param start
     * @param length
     * @return An array with the integers
     */
    public static int[] sequence(int start, int length) {
        final int[] a = new int[length];
        for (int i = 0; i < length; i++) {
            a[i] = start + i;
        }
        return a;
    }

    /**
     * Find the maximum number in the array using linear search
     *
     * @param values The values array
     * @return The maximum value in the array
     */
    public static int max(int[] values) {
        return max(values, 0, values.length);
    }

    /**
     * Find the minimum number in the array using linear search
     *
     * @param values The values array
     * @return The minimum value in the array
     */
    public static int min(int[] values) {
        return min(values, 0, values.length);
    }

    /**
     * Find the maximum number in the array range using linear search
     *
     * @param values The values array
     * @param offset The offset into the array to start searching from
     * @param length The length of the range to search
     * @return The maximum value in the range
     */
    public static int max(int[] values, int offset, int length) {
        if (values.length < offset || values.length < offset + length || length == 0) {
            throw new ArrayIndexOutOfBoundsException("Can't calculate max of an empty range ");
        }
        int max = values[offset];
        for (int i = 1; i < length; i++) {
            if (values[offset + i] > max) {
                max = values[offset + i];
            }
        }

        return max;
    }

    /**
     * Find the minimum number in the array range using linear search
     *
     * @param values The values array
     * @param offset The offset into the array to start searching from
     * @param length The length of the range to search
     * @return The minimum value in the range
     */
    public static int min(int[] values, int offset, int length) {
        if (values.length < offset || values.length < offset + length || length == 0) {
            throw new ArrayIndexOutOfBoundsException("Can't calculate min of an empty range ");
        }
        int min = values[offset];
        for (int i = 1; i < length; i++) {
            if (values[offset + i] < min) {
                min = values[offset + i];
            }
        }

        return min;
    }


    /**
     * Create an int array from (inclusive) range [first,last].
     *
     * @param first First integer
     * @param last  Last integer
     * @return Array of all integers from first to last, both included.
     */
    public static int[] range(int first, int last) {
        int increment = 1;
        int len = Math.abs(last - first) + 1;
        if (last < first) {
            increment = -1;
        }

        int[] result = new int[len];
        int value = first;
        for (int i = 0; i < len; i++) {
            result[i] = value;
            value += increment;
        }
        return result;
    }

    /**
     * @param values An array of int values
     * @return True if the provided array contains only unique values
     */
    public static boolean isUnique(int... values) {
        final IntHashSet set = new IntHashSet(values);
        return values.length == set.size();
    }

    /**
     * Binary search in a short array
     *
     * @param a   The array to search
     * @param key The key to find
     * @return index of the search key, if it is contained in the array else <tt>(-(<i>insertion point</i>) - 1)</tt>.
     */
    public static int binarySearch(short[] a, short key) {
        int low = 0;
        int high = a.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }


    /**
     * Find (first) occurrence of a value in an array.
     *
     * @param value
     * @param array
     * @return index of value found or -1 if value not found.
     */
    public static int indexOf(int value, int[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) return i;
        }
        return -1;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new IntArrayIterator(this);
    }

    static class IntArrayIterator implements Iterator<Integer> {

        private final IntArray theArray;
        private int index = 0;

        public IntArrayIterator(IntArray theArray) {
            this.theArray = theArray;
        }

        @Override
        public boolean hasNext() {
            return index < theArray.size();
        }

        @Override
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException("You have reached the end of the IntArray");
            }
            return theArray.get(index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
