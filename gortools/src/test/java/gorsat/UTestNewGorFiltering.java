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

package gorsat;

import org.gorpipe.test.SlowTests;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class UTestNewGorFiltering {

    private String HEADER = "ChromNOR\tPosNOR\tRowNum\ttypecol\ttestCol\n";

    @Ignore("Type inference is changing")
    @Category(SlowTests.class)
    @Test
    public void testTypeInference() {
        // All ints
        TestUtils.assertGorpipeResults (HEADER + "chrN\t0\t0\t0\t1\n", "norrows 100 | calc typecol rownum | calc testCol typeCol + 1 | top 1");

        // All ints but one string
        TestUtils.assertGorpipeResults (HEADER + "chrN\t0\t0\t0\t01\n", "norrows 100 | calc typecol if(rownum = 99, 'xx', rownum) | calc testCol typeCol + 1 | top 1");

        // All ints but one float
        TestUtils.assertGorpipeResults (HEADER + "chrN\t0\t0\t0.0\t1.0\n", "norrows 100 | calc typecol if(rownum = 99, 1.1, rownum) | calc testCol typeCol + 1 | top 1");

        // All floats
        TestUtils.assertGorpipeResults (HEADER + "chrN\t0\t0\t1.5\t2.5\n", "norrows 100 | calc typecol 1.5 | calc testCol typeCol + 1 | top 1");

        // All floats with NaN
        TestUtils.assertGorpipeResults (HEADER + "chrN\t0\t0\t1.5\t2.5\n", "norrows 100 | calc typecol if(rownum = 99, NaN, 1.5) | calc testCol typeCol + 1 | top 1");

        // All ints with NaN
        TestUtils.assertGorpipeResults (HEADER + "chrN\t0\t0\t0.0\t1.0\n", "norrows 100 | calc typecol if(rownum = 99, NaN, rownum) | calc testCol typeCol + 1 | top 1");

        // Type inference all ints except string col out of range (wrong type inference).
        // TestUtils.assertGorpipeResults (HEADER + "chrN\t0\t0\t0\t1\n", "norrows 11000 | calc typecol if(rownum = 10500, 3.14, rownum) | calc testCol typeCol + 1 | top 1");
    }


    @Ignore("Type inference is changing")
    @Category(SlowTests.class)
    @Test
    public void testTypeInferenceBufferSmallLines() {
        // Check if full 10000 lines are used.
        TestUtils.assertGorpipeResults (HEADER + "chrN\t0\t0\t0\t01\n", "norrows 11000 | calc typecol if(rownum = 9999, 'xx', rownum) | calc testCol typeCol + 1 | top 1");
    }

    @Category(SlowTests.class)
    @Ignore("Calc is stricter now on types - need to rethink this test")
    @Test
    public void testTypeInferenceBufferLargeLines() {
        // Making sure we don´t use to much memory.
        StringBuilder largeString = new StringBuilder();
        for (int i = 0; i < 1000000; i++) {
            largeString.append('x');
        }

        String oldVal = System.getProperty("gor.gorfiltring.max_bytes_buffered");
        System.setProperty("gor.gorfiltering.max_bytes_buffered", "107374182");  // Set ca 100MB
        // Will not reach the last line as the type inference will use fewer lines.
        TestUtils.assertGorpipeResults (HEADER + "chrN\t0\t0\t0\t1\n", "norrows 10000 | calc largeField '" + largeString.toString() + "' | calc typecol if(rownum = 9999, 'xx', rownum) | calc testCol typeCol + 1 | top 1 | select 1,3,4");
        if (oldVal != null) System.setProperty("gor.gorfiltering.max_bytes_buffered", oldVal);
    }
}
