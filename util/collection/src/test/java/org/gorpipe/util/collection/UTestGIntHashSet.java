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

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Random;

/**
 * GIntHashSetTest test the IntHashSet container
 *
 * @version $Id $
 */
public class UTestGIntHashSet extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(UTestGIntHashSet.class);

    private IntHashSet set;

    /**
     * Construct a new UTestGIntHashSet for the specified test case
     *
     * @param name The name of the test case
     */
    public UTestGIntHashSet(String name) {
        super(name);
    }

    public String getTestedClassName() {
        return IntHashSet.class.getName();
    }

    /**
     */
    public void testNotFound() {
        set = new IntHashSet();
        for (int i = 0; i < 10000; i++)
            assertFalse(set.contains(i));
    }

    /**
     * Test removing one set from another
     */
    public void testRemoveAll() {
        set = new IntHashSet(1, 3, 5, 7, 9, 10);
        final IntHashSet b = new IntHashSet(1, 7, 10);

        set.removeAll(b);

        assertEquals(3, set.size());
        assertTrue(set.contains(3));
        assertTrue(set.contains(5));
        assertTrue(set.contains(9));
        assertFalse(set.contains(1));
        assertFalse(set.contains(10));
        assertFalse(set.contains(7));
    }

    /**
     */
    public void testEmpty() {
        set = new IntHashSet();
        assertTrue(set.isEmpty());
        assertTrue(set.add(0));
        assertFalse(set.isEmpty());
        for (int i = 0; i < 100; i++) {
            assertTrue(set.add(i * 2 + 1));
        }
        set.clear();
        assertTrue(set.isEmpty());
    }

    /**
     * Test hashset grouping, i.e. min, max, ...
     */
    public void testGrouping() {
        final int MIN = 10;
        final int MAX = 10000;
        set = new IntHashSet();
        for (int i = MIN; i <= MAX; i++) {
            set.add(i);
        }

        assertEquals(MIN, set.min());
        assertEquals(MAX, set.max());

        set.add(MAX + 1);
        set.add(MIN - 1);
        assertEquals(MIN - 1, set.min());
        assertEquals(MAX + 1, set.max());
    }

    /**
     */
    public void testNormalUse() {
        set = new IntHashSet();
        final int min = -10000;
        final int max = 50000;
        for (int i = min; i < max; i++) {
            assertTrue(set.add(i));
        }
        for (int i = min; i < max; i++) {
            assertEquals("The value " + i + " should be in the set", true, set.contains(i));
        }

        for (int i = -10100; i < -10000; i++) {
            assertFalse(set.contains(i));
        }
        for (int i = 50001; i < 50010; i++) {
            assertFalse(set.contains(i));
        }
    }

    /**
     * Test that removing elements really works
     */
    public void testRemoval() {
        set = new IntHashSet();
        for (int i = -10; i < 100; i++)
            assertTrue(set.add(i));
        assertEquals("size must be right", 110, set.size());
        for (int i = 99; i > -11; i--)
            assertTrue(set.remove(i));
        assertEquals("size must be right", 0, set.size());
        for (int i = -10; i < 100; i++)
            assertTrue(set.add(i));
        assertEquals("size must be right", 110, set.size());
        for (int i = -10; i < 10; i++)
            assertTrue(set.remove(i));
        for (int i = 10; i < 100; i++)
            assertTrue(set.contains(i));
        assertEquals(90, set.size());
        for (int i = -100000; i < -1000; i++)
            assertFalse(set.remove(i));
        assertEquals(90, set.size());
        for (int i = 1000; i < 100000; i++)
            assertFalse(set.remove(i));
        assertEquals(90, set.size());

        set = new IntHashSet();
        for (int i = -10; i < 100; i++)
            assertTrue(set.add(i));
        assertEquals("size must be right", 110, set.size());
        set.retainAll(-10, 0, -1, 1, 50, 99);
        assertEquals(6, set.size());
        assertTrue(set.contains(-10));
        assertTrue(set.contains(0));
        assertTrue(set.contains(-10));
        assertTrue(set.contains(1));
        assertTrue(set.contains(50));
        assertTrue(set.contains(99));
    }

    /**
     * Test the contain function for all legal input
     */
    public void testContainForAllValues() {
        set = new IntHashSet();
        assertTrue(set.add(-42));
        assertTrue(set.add(0));
        assertTrue(set.add(1000000));
        for (int i = -1000000; i < 9999999; i++) {
            if (set.contains(i))
                TestCase.assertTrue(i == -42 || i == 0 || i == 1000000);
        }
    }

    /**
     * Test the union of two sets
     */
    public void testUnion() {
        set = new IntHashSet();
        for (int i = 1; i < 100; i++)
            assertTrue(set.add(i));

        IntHashSet b = new IntHashSet();
        for (int i = -10000; i < -100; i++)
            assertTrue(b.add(i));

        IntHashSet union = set.union(b);

        for (int i = 1; i < 100; i++)
            assertTrue(union.contains(i));
        for (int i = -10000; i < -100; i++)
            assertTrue(union.contains(i));

        assertEquals("Expect the whole set to be added", set.size(), b.add(set));
        assertTrue(b.equals(union));
    }

    /**
     * Test the intersection of two sets
     */
    public void testIntersection() {
        set = new IntHashSet();
        for (int i = 0; i < 100; i++)
            assertTrue(set.add(i));

        IntHashSet b = new IntHashSet();
        for (int i = -10; i < 4; i++)
            assertTrue(b.add(i));

        IntHashSet c = set.intersection(b);
        assertEquals("Must be of the right size", 4, c.size());
        assertTrue(c.contains(0));
        assertTrue(c.contains(1));
        assertTrue(c.contains(2));
        assertTrue(c.contains(3));
    }

    /**
     * Test conversion of int hash set into an array
     */
    public void testToArray() {
        // Create initial set
        set = new IntHashSet();
        for (int i = -1000; i < 50000; i++) {
            assertTrue(set.add(i));
        }

        // Convert set to array and create a new set presumably identical to first set
        int[] a = set.toArray();
        assertEquals(set.size(), a.length);
        IntHashSet newSet = new IntHashSet();
        for (int value : a) {
            assertTrue(newSet.add(value));
        }

        // Test that the first set is identical to the second
        assertEquals(set.size(), newSet.size());
        for (int i = -1000; i < 50000; i++) {
            assertTrue(newSet.contains(i));
        }
        for (int i = 50010; i < 150000; i++) {
            assertFalse(newSet.contains(i));
        }
    }

    /**
     * Test the clone method on IntHashSet
     */
    public void testCloning() {
        set = new IntHashSet();
        for (int i = -1000; i < 50000; i++) {
            assertTrue(set.add(i));
        }

        IntHashSet clone = new IntHashSet(set);
        TestCase.assertTrue(clone.size() == set.size());
        assertTrue(set.equals(clone));
        assertFalse(clone.contains(-292929));
        for (int i = -1000; i < 50000; i++) {
            assertTrue(clone.contains(i));
        }
        for (int i = 50010; i < 150000; i++) {
            assertFalse(clone.contains(i));
        }
    }

    /**
     * Test random use of add, contains, remove
     */
    public void testRandomUse() {
        set = new IntHashSet();
        final int LENGTH = 1000;
        Random rand = new Random();
        int[] values = new int[LENGTH];
        int sign = 1;
        for (int k = 0; k < 1024; k++) {
            for (int i = 0; i < LENGTH; i++) {
                values[i] = rand.nextInt() * sign;
            }
            for (int value : values)
                set.add(value);
            for (int value : values)
                assertTrue(set.contains(value));
            for (int i = values.length - 1; i >= 0; i--)
                set.remove(values[i]);
            assertEquals(0, set.size());
            sign *= -1;
        }
        set.hashCode();
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
        measureSetPerformance(LENGTH + " Random values", values);

        for (int i = 0; i < LENGTH; i++) {
            values[i] = i;
        }
        measureSetPerformance("Growing Order from 0.." + LENGTH, values);

        for (int i = 0; i < LENGTH; i++) {
            values[i] = i * 512;
        }
        measureSetPerformance(LENGTH + " values inc. by 512", values);
    }

    private void measureSetPerformance(String name, int[] values) {
        set = new IntHashSet();
        long start = System.nanoTime();
        for (int value : values)
            set.add(value);
        long addTime = (System.nanoTime() - start) / (1000 * 1000);
        start = System.nanoTime();
        for (int value : values)
            set.contains(value);
        long containsTime = (System.nanoTime() - start) / (1000 * 1000);
        start = System.nanoTime();
        for (int i = values.length - 1; i >= 0; i--)
            set.remove(values[i]);
        long removeTime = (System.nanoTime() - start) / (1000 * 1000);

        String msg = name + " IntHashSet Add=" + addTime + ", Contains=" + containsTime
                + ", Remove=" + removeTime;
//        assertTrue(msg + " is less than second", addTime+containsTime+removeTime < 1000);
        log.info(msg);

        HashSet<Integer> hset = new HashSet<Integer>();
        start = System.nanoTime();
        for (int value : values)
            hset.add(value);
        long hsetAddTime = (System.nanoTime() - start) / (1000 * 1000);
        start = System.nanoTime();
        for (int value : values)
            hset.contains(value);
        long hsetContainsTime = (System.nanoTime() - start) / (1000 * 1000);
        start = System.nanoTime();
        for (int i = values.length - 1; i >= 0; i--)
            hset.remove(values[i]);
        long hsetRemoveTime = (System.nanoTime() - start) / (1000 * 1000);

        msg = name + " Java HashSet Add=" + hsetAddTime + ", Contains=" + hsetContainsTime
                + ", Remove=" + hsetRemoveTime;
        log.info(msg);
    }
}
