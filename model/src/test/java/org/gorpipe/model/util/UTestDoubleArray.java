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


import junit.framework.TestCase;

/**
 * Unittests for the DoubleArray class
 *
 * @version $Id$
 */

public class UTestDoubleArray extends TestCase {

    private DoubleArray doubleArray;

    /**
     * Construct a new UTestDoubleArray for the specified test case
     *
     * @param name The name of the test case
     */
    public UTestDoubleArray(String name) {
        super(name);
    }

    /**
     * Test the adding of value into the DoubleArray
     */
    public void testAddValue() {
        doubleArray = new DoubleArray(10);

        TestCase.assertEquals(10, doubleArray.capacity());

        doubleArray.add(1);
        doubleArray.add(1);
        doubleArray.add(1);
        doubleArray.add(1);
        doubleArray.add(1);

        String strValues = doubleArray.toString();
        TestCase.assertEquals("{'size=5','capacity=10'}[1.0,1.0,1.0,1.0,1.0]", strValues);

        doubleArray.removeLast();
        strValues = doubleArray.toString();
        TestCase.assertEquals("{'size=4','capacity=10'}[1.0,1.0,1.0,1.0]", strValues);

        final double[] values = {0, 1, 2, 0.1, 1.2, 1000};
        doubleArray = new DoubleArray(values);
        TestCase.assertEquals(values.length, doubleArray.size());
    }

    /**
     * Test convertion of the DoubleArray to primitive array
     */
    public void testToArray() {
        doubleArray = new DoubleArray(50);

        doubleArray.add(new double[]{3, 3, 3, 3544, 5});

        double[] sameArray = doubleArray.toArray();

        TestCase.assertEquals(5, sameArray.length);
        TestCase.assertEquals(3, sameArray[0], 0.0);
    }

    /**
     * Test convertion of the DoubleArray to primitive float array
     */
    public void testToFloatArray() {
        doubleArray = new DoubleArray(50);

        doubleArray.add(new double[]{3, 3, 3, 3544, 5});

        float[] sameArray = doubleArray.toFloatArray();

        TestCase.assertEquals(5, sameArray.length);
        TestCase.assertEquals(3.0f, sameArray[0], 0.0f);
    }

    /**
     * Test that the DoubleArray contains a specific value
     */
    public void testContains() {
        doubleArray = new DoubleArray();

        doubleArray.add(5);
        doubleArray.add(1000);

        TestCase.assertTrue(doubleArray.contains(1000));
        TestCase.assertFalse(doubleArray.contains(40));
    }

    /**
     * Test that the value at a specific index can be set
     */
    public void testSetValue() {
        doubleArray = new DoubleArray();

        doubleArray.add(4);
        doubleArray.set(0, 10);

        TestCase.assertEquals(10, doubleArray.get(0), 0.0);
    }

    /**
     * Test that a range of certain size can be reserved in the double array
     */
    public void testReserveRange() {
        doubleArray = new DoubleArray();

        //Reserves the first 50 ints
        doubleArray.reserveRange(50);
        doubleArray.add(50);

        TestCase.assertEquals(0, doubleArray.get(0), 0);
        TestCase.assertEquals(0, doubleArray.get(49), 0);
        TestCase.assertEquals(50, doubleArray.get(50), 0);
    }

    /**
     * Test that the DoubleArray can be sorted
     */
    public void testSort() {
        doubleArray = new DoubleArray();

        doubleArray.add(new double[]{4, 8, 2, 5, 32, 892, 1, 2, 4});

        doubleArray.sort();

        TestCase.assertEquals(1, doubleArray.get(0), 0);
        TestCase.assertEquals(2, doubleArray.get(1), 0);
        TestCase.assertEquals(2, doubleArray.get(2), 0);
        TestCase.assertEquals(4, doubleArray.get(3), 0);
        TestCase.assertEquals(4, doubleArray.get(4), 0);
        TestCase.assertEquals(5, doubleArray.get(5), 0);
        TestCase.assertEquals(8, doubleArray.get(6), 0);
        TestCase.assertEquals(32, doubleArray.get(7), 0);
        TestCase.assertEquals(892, doubleArray.get(8), 0);
    }

    /**
     * Test that the DoubleArray can shrink its capacity to fit its size
     */
    public void testShrinkToFit() {
        doubleArray = new DoubleArray(10);

        doubleArray.shrinkToFit();
        TestCase.assertEquals(0, doubleArray.size());
    }

    /**
     * Test that an value can be found in the array with binary search
     */
    public void testBinarySearch() {
        doubleArray = new DoubleArray();

        doubleArray.add(new double[]{3, 5, 7, 8, 12, 17, 22, 33});

        TestCase.assertEquals(2, doubleArray.binarySearch(7));
        TestCase.assertEquals(7, doubleArray.binarySearch(33));
    }

    /**
     * Test that the median value in the array can be found
     */
    public void testGetMedian() {
        doubleArray = new DoubleArray();

        doubleArray.add(new double[]{3.4, 5.5, 1.1, 1.2, 1.3});
        TestCase.assertEquals(doubleArray.getMedian(), 1.3, 0);

        doubleArray.add(7.7);
        TestCase.assertEquals(doubleArray.getMedian(), 2.35, 0);
    }

    /**
     * Test that the DoubleArray can be cloned
     *
     * @throws Exception
     */
    public void testClone() throws Exception {
        doubleArray = new DoubleArray();

        doubleArray.add(new double[]{3.4, 5.5, 1.1, 1.2, 1.3});
        DoubleArray clone = (DoubleArray) doubleArray.clone();
        TestCase.assertEquals(doubleArray.size(), clone.size());
        for (int i = 0; i < doubleArray.size(); i++) {
            TestCase.assertEquals(doubleArray.get(i), clone.get(i), 0.001);
        }
    }

    /**
     * Test that an value can be inserted into the DoubleArray
     */
    public void testInsertValue() {
        doubleArray = new DoubleArray();

        doubleArray.add(5.2);
        doubleArray.insert(6.1, 0);
        doubleArray.insert(2.5, 1);

        TestCase.assertEquals(3, doubleArray.size());
        TestCase.assertEquals(6.1, doubleArray.get(0), 0);
        TestCase.assertEquals(2.5, doubleArray.get(1), 0);
        TestCase.assertEquals(5.2, doubleArray.get(2), 0);

        doubleArray = new DoubleArray();

        doubleArray.insert(5.11, 0);
        doubleArray.insert(6.12, 1);

        TestCase.assertEquals(5.11, doubleArray.get(0), 0);
        TestCase.assertEquals(6.12, doubleArray.get(1), 0);
    }

    /**
     * Test that a value can be deleted from the DoubleArray
     */
    public void testDeleteValue() {
        doubleArray = new DoubleArray();

        doubleArray.add(new double[]{5.5, 56.1, 76.1, 3.7, 3.5, 5.0});

        doubleArray.delete(0);
        doubleArray.delete(4);

        TestCase.assertEquals(4, doubleArray.size());
        TestCase.assertEquals(56.1, doubleArray.get(0), 0);
        TestCase.assertEquals(76.1, doubleArray.get(1), 0);
        TestCase.assertEquals(3.7, doubleArray.get(2), 0);
        TestCase.assertEquals(3.5, doubleArray.get(3), 0);
    }

    /**
     * Test method
     */
    public void testConvertToDoubleArray() {
        Double[] expArr = new Double[]{new Double(1), new Double(2), new Double(3)};
        double[] actArr = DoubleArray.toDoubleArray(expArr);
        for (int i = 0; i < actArr.length; i++) {
            TestCase.assertEquals(expArr[i].doubleValue(), actArr[i], 0);
        }
    }


}
