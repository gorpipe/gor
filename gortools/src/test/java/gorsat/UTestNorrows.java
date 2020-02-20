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

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 12/06/2017.
 */
public class UTestNorrows {


    @Test
    public void testWithoutOptions() {
        String[] result = TestUtils.runGorPipeLines("norrows 0");
        Assert.assertEquals(1, result.length);

        result = TestUtils.runGorPipeLines("norrows 1");
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("0", getRowNumber(result[1]));


        result = TestUtils.runGorPipeLines("norrows 100");
        Assert.assertEquals(101, result.length);
        Assert.assertEquals("0", getRowNumber(result[1]));
        Assert.assertEquals("99", getRowNumber(result[100]));
    }

    @Test
    public void testWithStepOptions() {
        String[] result = TestUtils.runGorPipeLines("norrows -step 2 0");
        Assert.assertEquals(1, result.length);

        result = TestUtils.runGorPipeLines("norrows -step 10 10");
        Assert.assertEquals(11, result.length);
        Assert.assertEquals("0", getRowNumber(result[1]));
        Assert.assertEquals("90", getRowNumber(result[10]));

        result = TestUtils.runGorPipeLines("norrows -step 200 100");
        Assert.assertEquals(101, result.length);
        Assert.assertEquals("0", getRowNumber(result[1]));
        Assert.assertEquals("19800", getRowNumber(result[100]));
    }

    @Test
    public void testWithOffsetOptions() {
        String[] result = TestUtils.runGorPipeLines("norrows -offset 0 0");
        Assert.assertEquals(1, result.length);

        result = TestUtils.runGorPipeLines("norrows -offset 0 10");
        Assert.assertEquals(11, result.length);
        Assert.assertEquals("0", getRowNumber(result[1]));
        Assert.assertEquals("9", getRowNumber(result[10]));

        result = TestUtils.runGorPipeLines("norrows -offset 200 100");
        Assert.assertEquals(101, result.length);
        Assert.assertEquals("200", getRowNumber(result[1]));
        Assert.assertEquals("299", getRowNumber(result[100]));
    }

    @Test
    public void testWitOptionCombo() {
        String[] result = TestUtils.runGorPipeLines("norrows -offset 0 -step 1 0");
        Assert.assertEquals(1, result.length);

        result = TestUtils.runGorPipeLines("norrows -offset 0 -step 10 10");
        Assert.assertEquals(11, result.length);
        Assert.assertEquals("0", getRowNumber(result[1]));
        Assert.assertEquals("90", getRowNumber(result[10]));

        result = TestUtils.runGorPipeLines("norrows -offset 200 -step 100 100");
        Assert.assertEquals(101, result.length);
        Assert.assertEquals("200", getRowNumber(result[1]));
        Assert.assertEquals("10100", getRowNumber(result[100]));
    }


    @Test(expected = GorParsingException.class)
    public void testNegativeOffset() {
        TestUtils.runGorPipeLines("norrows -offset -1 100");
    }

    @Test(expected = GorParsingException.class)
    public void testZeroStep() {
        TestUtils.runGorPipeLines("norrows -step 0 100");
    }


    @Test(expected = GorParsingException.class)
    public void testNegativeValue() {
        TestUtils.runGorPipeLines("norrows -10");
    }


    private String getRowNumber(String rowText) {
        String[] strings = rowText.trim().split("\t");
        Assert.assertEquals(3, strings.length);
        return strings[2];
    }

}
