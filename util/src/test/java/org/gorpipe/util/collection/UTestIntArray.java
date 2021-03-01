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

package org.gorpipe.util.collection;

import junit.framework.TestCase;
import org.gorpipe.util.collection.IntArray;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Unittests for the IntArray class
 *
 * @version $Id$
 */
public class UTestIntArray extends TestCase {

    private IntArray array;

    /**
     * Construct a new UTestIntArray for the specified test case
     *
     * @param name The name of the test case
     */
    public UTestIntArray(String name) {
        super(name);
    }

    public String getTestedClassName() {
        return IntArray.class.getName();
    }

    /**
     * Test Stream support of IntArray
     */
    public void testStream() {
        final List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        IntArray a = new IntArray(Integer::intValue, numbers.stream()
                .filter(n -> {
                    return n % 2 == 0;
                })
                .map(n -> {
                    return n * n;
                })
                .limit(2));
        assertEquals(2, a.size());
        assertEquals(4, a.get(0));
        assertEquals(16, a.get(1));

        a = new IntArray(numbers.stream()
                .filter(n -> {
                    return n % 2 == 0;
                })
                .mapToInt(n -> {
                    return n * n;
                })
                .limit(2));
        assertEquals(2, a.size());
        assertEquals(4, a.get(0));
        assertEquals(16, a.get(1));

        array = new IntArray(Integer::intValue, numbers.stream());
        a = new IntArray(array.stream().filter(n -> {
            return n % 2 == 0;
        })
                .map(n -> {
                    return n * n;
                })
                .limit(2));
        assertEquals(2, a.size());
        assertEquals(4, a.get(0));
        assertEquals(16, a.get(1));
    }


    /**
     * Test reversing an int array
     */
    public void testReverse() {
        int[] a = IntArray.sequence(0, 100);
        IntArray.reverse(a);
        for (int i = 1; i < a.length; i++) {
            TestCase.assertTrue(a[i] <= a[i - 1]);
        }

        a = IntArray.sequence(0, 101);
        IntArray.reverse(a);
        for (int i = 1; i < a.length; i++) {
            TestCase.assertTrue(a[i] <= a[i - 1]);
        }
    }

    /**
     * Test that an value can be added to the IntArray
     */
    public void testAddValue() {
        array = new IntArray(10);

        assertEquals(10, array.capacity());

        array.add(1);
        array.add(1);
        array.add(1);
        array.add(1);
        array.add(1);

        int sum = array.sum();
        TestCase.assertEquals(5, sum);

        assertEquals("{'size=5','capacity=10'}[1,1,1,1,1]", array.toString());

        array.removeLast();
        assertEquals("{'size=4','capacity=10'}[1,1,1,1]", array.toString());

        array.increment(0);
        assertEquals("{'size=4','capacity=10'}[2,1,1,1]", array.toString());

        final int[] seq = IntArray.sequence(100, 10);
        TestCase.assertEquals(10, seq.length);
        for (int i = 0; i < seq.length; i++) {
            TestCase.assertEquals(100 + i, seq[i]);
        }

    }

    /**
     * Test that a provided array only contains unique values
     */
    public void testUnique() {
        assertTrue(IntArray.isUnique(Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1, 2, 4304049));
        assertFalse(IntArray.isUnique(Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1, 2, 4304049, 2));
    }

    /**
     * Test that an value can be inserted to the IntArray
     */
    public void testInsertValue() {
        array = new IntArray();

        array.add(5);
        assertEquals(5, array.getLast());
        array.insert(6, 0);
        array.insert(2, 1);

        assertEquals(3, array.size());
        assertEquals(6, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(5, array.get(2));

        char[] chars = array.toCharArray();
        assertEquals(array.size(), chars.length);
        TestCase.assertEquals(6, chars[0]);
        TestCase.assertEquals(2, chars[1]);
        TestCase.assertEquals(5, chars[2]);


        array = new IntArray();

        array.insert(5, 0);
        array.insert(6, 1);

        assertEquals(5, array.get(0));
        assertEquals(6, array.get(1));


        array.clear();
        assertEquals(0, array.size());
        array.add(10);
        assertEquals(10, array.getLast());
        array.incrementLast();
        assertEquals(11, array.getLast());
    }

    /**
     * Test that an value can be deleted from the IntArray
     */
    public void testDeleteValue() {
        array = new IntArray();

        array.add(new int[]{5, 56, 76, 3, 3, 5});
        assertEquals(5, array.getLast());

        array.delete(0);
        array.delete(4);

        assertEquals(4, array.size());
        assertEquals(56, array.get(0));
        assertEquals(76, array.get(1));
        assertEquals(3, array.get(2));
        assertEquals(3, array.get(3));

    }

    /**
     * Test that the IntArray can be cloned
     *
     * @throws Exception
     */
    public void testClone() throws Exception {
        array = new IntArray();

        array.add(new int[]{5, 56, 76, 3, 3, 5});
        IntArray clone = new IntArray(array);
        assertEquals(array.size(), clone.size());
        for (int i = 0; i < array.size(); i++) {
            assertEquals(array.get(i), clone.get(i));
        }
    }


    /**
     * Test that the IntArray can be converted into an int primitive array
     */
    public void testToArray() {
        array = new IntArray(50);

        array.add(new int[]{3, 3, 3, 3544, 5});

        int[] sameArray = array.toArray();

        TestCase.assertEquals(5, sameArray.length);
        TestCase.assertEquals(3, sameArray[0]);

        final short[] shortArray = array.toShortArray();
        for (int i = 0; i < shortArray.length; i++) {
            assertEquals(array.get(i), shortArray[i]);
        }
    }

    /**
     * Test that the IntArray can be converted into an IntBuffer
     */
    public void testToBuffer() {
        array = new IntArray(50);

        array.add(IntBuffer.wrap(new int[]{3, 3, 3, 3544, 5}));

        IntBuffer sameArray = array.toBuffer();

        TestCase.assertEquals(5, sameArray.capacity());
        TestCase.assertEquals(3, sameArray.get(0));
    }

    /**
     * Test that the IntArray contains a specific value
     */
    public void testContains() {
        array = new IntArray();

        array.add(5);
        array.add(1000);
        assertEquals(1000, array.getLast());

        assertTrue(array.contains(1000));
        assertFalse(array.contains(40));

        int[] a = {1, 5};
        int[] b = {2, 4};
        int[] expected = {1, 5, 2, 4};
        int[] c = new int[4];
        IntArray.merge(a, b, c);
        for (int i = 0; i < c.length; i++) {
            TestCase.assertEquals(expected[i], c[i]);
        }
    }

    /**
     * Test that an value can be set in the IntArray
     */
    public void testSetValue() {
        array = new IntArray();

        array.add(4);
        array.set(0, 10);

        assertEquals(10, array.get(0));
    }

    /**
     * Test that an specific range can be reserved in the IntArray
     */
    public void testReserveRange() {
        array = new IntArray();

        //Reserves the first 50 ints
        array.reserveRange(50);
        array.add(50);

        assertEquals(0, array.get(0));
        assertEquals(0, array.get(49));
        assertEquals(50, array.get(50));
    }

    /**
     * Test that the IntArray can be sorted
     */
    public void testSort() {
        array = new IntArray();

        array.add(new int[]{4, 8, 2, 5, 32, 892, 1, 2, 4});

        array.sort();

        assertEquals(1, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(2, array.get(2));
        assertEquals(4, array.get(3));
        assertEquals(4, array.get(4));
        assertEquals(5, array.get(5));
        assertEquals(8, array.get(6));
        assertEquals(32, array.get(7));
        assertEquals(892, array.get(8));
    }

    /**
     * Test that the IntArray can shrink its capacity to its size
     */
    public void testShrinkToFit() {
        array = new IntArray(10);

        array.shrinkToFit();
        assertEquals(0, array.size());
    }

    /**
     * Test that an element can be found in a sorted IntArray with binary search
     */
    public void testBinarySearch() {
        array = new IntArray();

        array.add(new int[]{3, 5, 7, 8, 12, 17, 22, 33});

        assertEquals(2, array.binarySearch(7));
        assertEquals(7, array.binarySearch(33));
    }

    /**
     * Test enumeration methods
     */
    public void testIntEnumeration() {
        int[] keys = {1, 19, -1, 19, 100, -1000};
        Map<Integer, Integer> map = IntArray.enumerate(keys);
        for (int key : keys) {
            TestCase.assertTrue("All incoming keys must exists in map", map.containsKey(key));
            int idx = map.get(key);
            TestCase.assertEquals("Entry at map index must be the key", key, keys[idx]);
        }

        for (int value : map.keySet()) {
            boolean found = false;
            for (int key : keys) {
                if (value == key) {
                    found = true;
                    break;
                }
            }
            TestCase.assertTrue("All keys must exists in original array", found);
        }
    }

    /**
     * Test enumeration methods
     */
    public void testLongEnumeration() {
        long[] keys = {-3939, 1, 383, -1, 19, 100, -1000};
        Map<Long, Integer> map = IntArray.enumerate(keys);
        for (long key : keys) {
            TestCase.assertTrue("All incoming keys must exists in map", map.containsKey(key));
            int idx = map.get(key);
            TestCase.assertEquals("Entry at map index must be the key", key, keys[idx]);
        }

        for (long value : map.keySet()) {
            boolean found = false;
            for (long key : keys) {
                if (value == key) {
                    found = true;
                    break;
                }
            }
            TestCase.assertTrue("All keys must exists in original array", found);
        }
    }

    /**
     * Test indexOf methods.
     */
    public void testIndexOf() {
        int[] ia = new int[]{10, 0, 1, 3, 1, 6};
        assertEquals(0, IntArray.indexOf(10, ia));
        assertEquals(-1, IntArray.indexOf(2, ia));
        assertEquals(2, IntArray.indexOf(1, ia));
        assertEquals(5, IntArray.indexOf(6, ia));
    }


    /**
     * Test method
     */
    public void testConvertToIntArray() {
        Integer[] expArr = new Integer[]{Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)};
        int[] actArr = IntArray.toIntArray(expArr);
        for (int i = 0; i < actArr.length; i++) {
            TestCase.assertEquals(expArr[i].intValue(), actArr[i]);
        }

        array = new IntArray();
        array.add(actArr);
        final byte[] bArr = array.toByteArray();
        for (int i = 0; i < bArr.length; i++) {
            TestCase.assertEquals(expArr[i].intValue(), bArr[i]);
        }

        final Integer[] nextArr = IntArray.toIntegerArray(actArr);
        for (int i = 0; i < actArr.length; i++) {
            TestCase.assertEquals(nextArr[i].intValue(), actArr[i]);
        }


    }

    private int[] m(int... a) {
        return a;
    }

    private void assertEqual(int[] a1, int... a2) {
        TestCase.assertEquals(a1.length, a2.length);
        for (int i = 0; i < a1.length; i++) {
            TestCase.assertEquals("Index " + i, a1[i], a2[i]);
        }
    }

    /**
     * Test range method.
     */
    public void testRange() {
        assertEqual(m(0), IntArray.range(0, 0));
        assertEqual(m(0, 1), IntArray.range(0, 1));
        assertEqual(m(1, 0), IntArray.range(1, 0));
        assertEqual(m(5, 6, 7, 8, 9, 10), IntArray.range(5, 10));
        assertEqual(m(-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), IntArray.range((-10), 10));
    }

    /**
     * Test implementation of the Iterable interface
     */
    public void testIterator() {
        array = new IntArray(100);
        //Direct call to the iterator() method to make the CoverageTest happy
        array.iterator();

        for (int i = 0; i <= 20; i++) {
            array.add(i);
        }
        int expected = 0;
        for (Integer retrieved : array) {
            TestCase.assertEquals("Expected element not retrieved", expected, retrieved.intValue());
            expected++;
        }
        TestCase.assertEquals("Number of elements retrieved not same as number of added", 21, expected);
    }

    /**
     * Text linear Min/Max on an unordered array
     */
    public void testMinMax() {
        final int[] values = {-10, -1, 0, 5000, -5000, 200, 2, 7};
        assertEquals(-5000, IntArray.min(values));
        assertEquals(5000, IntArray.max(values));
        assertEquals(-10, IntArray.min(values, 0, 4));
        assertEquals(200, IntArray.max(values, 4, 4));
    }

}
