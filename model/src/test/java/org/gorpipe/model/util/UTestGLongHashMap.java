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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Test case for GLongHashMap
 *
 * @version $Id $
 */
public class UTestGLongHashMap extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(UTestGLongHashMap.class);

    /**
     */
    public void testNotFound() {
        GLongHashMap map = new GLongHashMap();
        for (int i = 0; i < 10000; i++)
            assertFalse(map.containsKey(i));
    }

    /**
     * Test using actual longs
     */
    public void testLongs() {
        GLongHashMap map = new GLongHashMap();
        for (long i = Integer.MAX_VALUE + 10L; i < 10000; i++) {
            assertFalse(map.containsKey(i));
            final int val = (int) (i - Integer.MAX_VALUE);
            map.put(i, val);
            assertTrue(map.containsKey(i));
            assertEquals(val, map.get(i));
        }
    }

    /**
     * Test getting keys that have been sorted after the values
     */
    public void testGetKeysSorted() {
        GLongHashMap map = new GLongHashMap();
        map.put(2, 2);
        map.put(1, 1);
        map.put(4, 4);
        map.put(3, 3);
        map.put(111, 111);
        map.put(10, 10);
        long[] keys = map.keysToArraySortByValue(false);
        for (int i = 1; i < 5; i++) {
            assertEquals(i, keys[i - 1]);
        }
        assertEquals(10, keys[4]);
        assertEquals(111, keys[5]);

        keys = map.keysToArraySortByValue(true);
        assertEquals(111, keys[0]);
        assertEquals(10, keys[1]);
        for (int i = 1; i < 5; i++) {
            assertEquals(5 - i, keys[i + 1]);
        }
    }

    /**
     */
    public void testEmpty() {
        GLongHashMap map = new GLongHashMap();
        assertTrue(map.isEmpty());
        map.put(0, 0);
        assertFalse(map.isEmpty());
        for (int i = 0; i < 100; i++) {
            map.put(i, i * 2 + 1);
        }
        assertEquals(100, map.size());
        map.clear();
        assertTrue(map.isEmpty());
    }

    /**
     * Test the contain function for all legal input
     */
    public void testContainForAllValues() {
        GLongHashMap map = new GLongHashMap();
        map.put(-42, 1);
        map.put(0, 1);
        map.put(1000000, -1);
        assertTrue(map.containsValue(1));
        assertTrue(map.containsValue(-1));
        assertFalse(map.containsValue(0));
        assertEquals(3, map.size());
        for (int i = -1000000; i < 9999999; i++) {
            if (map.containsKey(i))
                assertTrue(i == -42 || i == 0 || i == 1000000);
            else
                assertEquals(Integer.MIN_VALUE, map.get(i, Integer.MIN_VALUE));
        }
    }

    /**
     * Test the clone method on GLongHashMap
     */
    public void testCloning() {
        GLongHashMap map = new GLongHashMap();
        for (int i = -1000; i < 50000; i++) {
            map.put(i, i);
        }

        GLongHashMap clone = map.clone();
        assertTrue(clone.size() == map.size());
        assertTrue(map.equals(clone));
        assertFalse(clone.containsKey(-292929));
        for (int i = -1000; i < 50000; i++) {
            assertTrue(clone.containsKey(i));
        }
        for (int i = 50010; i < 150000; i++) {
            assertFalse(clone.containsKey(i));
        }
    }

    /**
     * Test random use of add, contains, remove
     */
    public void testRandomUse() {
        GLongHashMap map = new GLongHashMap();
        final int LENGTH = 1000;
        Random rand = new Random();
        int[] values = new int[LENGTH];
        int sign = 1;
        for (int k = 0; k < 1024; k++) {
            for (int i = 0; i < LENGTH; ) {
                values[i] = rand.nextInt() * sign;
                if (map.put(values[i], values[i] + 1))
                    i++;
            }
//            for (int i = 0; i < values.length; i++)
//                map.put(values[i], values[i]+1);
            for (int i = 0; i < values.length; i++) {
                assertTrue(map.containsKey(values[i]));
                assertEquals(values[i] + 1, map.get(values[i]));
            }
            long[] keys = map.keysToArray();
            for (int i = 0; i < values.length; i++) {
                assertTrue(map.containsKey(keys[i]));
            }

            for (int i = values.length - 1; i >= 0; i--)
                assertTrue(map.remove(values[i]));
            assertEquals(0, map.size());
            sign *= -1;
        }
    }


    /**
     * Check that the performance of the map is within expected range
     */
    public void testPerformance() {
        final int LENGTH = 1000000;
        int[] values = new int[LENGTH];

        Random rand = new Random();
        for (int i = 0; i < LENGTH; i++) {
            values[i] = rand.nextInt();
        }
        measureMapPerformance(LENGTH + " Random values", values);

        for (int i = 0; i < LENGTH; i++) {
            values[i] = i;
        }
        measureMapPerformance("Growing Order from 0.." + LENGTH, values);

        for (int i = 0; i < LENGTH; i++) {
            values[i] = i * 512;
        }
        measureMapPerformance(LENGTH + " values inc. by 512", values);
    }

    private void measureMapPerformance(String name, int[] values) {
        GLongHashMap map = new GLongHashMap();
        long start = System.nanoTime();
        for (int i = 0; i < values.length; i++)
            map.put(values[i], i);
        long addTime = (System.nanoTime() - start) / (1000 * 1000);
        start = System.nanoTime();
        for (int i = 0; i < values.length; i++)
            map.containsKey(values[i]);
        long containsTime = (System.nanoTime() - start) / (1000 * 1000);
        start = System.nanoTime();
        for (int i = values.length - 1; i >= 0; i--)
            map.remove(values[i]);
        long removeTime = (System.nanoTime() - start) / (1000 * 1000);

        String msg = name + " GLongHashMap Add=" + addTime + ", Contains=" + containsTime
                + ", Remove=" + removeTime;
//        assertTrue(msg + " is less than 1 second", addTime+containsTime+removeTime < 1000);
        log.info(msg);
/*        
        HashMap<Integer, Integer> hset = new HashMap<Integer, Integer>(); 
        start = System.nanoTime();
        for (int i = 0; i < values.length; i++)
            hset.put(values[i], i);
        long hsetAddTime = (System.nanoTime() - start)/(1000*1000);
        start = System.nanoTime();
        for (int i = 0; i < values.length; i++)
            hset.containsKey(values[i]);
        long hsetContainsTime = (System.nanoTime() - start)/(1000*1000);
        start = System.nanoTime();
        for (int i = values.length-1; i >= 0; i--)
            hset.remove(values[i]);
        long hsetRemoveTime = (System.nanoTime() - start)/(1000*1000);

        msg = name + " Java HashMap Add=" + hsetAddTime +", Contains=" + hsetContainsTime
            + ", Remove=" + hsetRemoveTime;
        log.info(msg);
*/
    }
}

