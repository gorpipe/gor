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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UTestGLogGamma {
    @Test
    public void get() {
        assertResult(0, 0);
        assertResult(1, 0);
        assertResult(2, 0.6931471805599453);
        assertResult(3, 1.791759469228055);
        assertResult(10, 15.104412573075518);
    }

    /**
     * Test the LogGamma calculations.
     *
     * From gfj
     */
    @Test
    public void testLogGamma() {

        // Test for some value ...
        assertEquals("Compute log(22!); ", 48.47118135183, GLogGamma.get(22), 1e-10);
        // and another value ...
        assertEquals("Compute log(16!); ", 30.67186010608, GLogGamma.get(16), 1e-10);
        // and then a higher value
        assertEquals("Compute log(38!); ", 102.9681986145, GLogGamma.get(38), 1e-10);

        // Check at the low boundary:
        assertEquals("Compute log(0!); ", 0, GLogGamma.get(0), 1e-10);
        assertEquals("Compute log(1!); ", 0, GLogGamma.get(1), 1e-10);

        // Test using LogGamma to compute number of combinations (n choose k).
        int n = 38;
        int k = 32;
        double numComb = Math.exp(GLogGamma.get(n) - GLogGamma.get(k) - GLogGamma.get(n - k));
        assertEquals("Combinations ( " + n + " choose " + k + "): ", 2760681, numComb, 1e-5);

        // Note that all the tests are in a sinlge test method, because LogGamma is static
        // and we need to ensure that the test values are computed in the right order.
    }

    @Test
    @Ignore("Run this locally for verifying performance")
    public void benchmark() {
        int n = 38;
        int k = 22;
        double numComb = Math.exp(GLogGamma.get(n) - GLogGamma.get(k) - GLogGamma.get(n - k));

        System.out.println("(" + n + " choose " + k + ") = " + numComb);
        long t1 = System.currentTimeMillis();
        double c = 0;
        for (int i = 1; i < 75000; ++i) {
            for (int j = 0; j < i; ++j) {
                c = GLogGamma.get(i) - GLogGamma.get(j) - GLogGamma.get(i - j);
            }
        }
        long t2 = System.currentTimeMillis();
        System.out.println("time: " + (t2 - t1) + " last c = " + c);
    }

    private void assertResult(int i, double expected) {
        final double v = GLogGamma.get(i);
        Assert.assertEquals(expected, v, 1e-8);
    }
}