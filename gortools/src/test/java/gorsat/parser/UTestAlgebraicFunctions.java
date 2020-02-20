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

package gorsat.parser;

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class UTestAlgebraicFunctions {
    @Test
    public void testFloor() {
        TestUtils.assertCalculated("floor(5)", 5);
        TestUtils.assertCalculated("floor(99.99999)", 99);
        TestUtils.assertCalculated("floor(100.1)", 100);
    }

    @Test
    public void testInt() {
        TestUtils.assertCalculated("int(5)", 5);
        TestUtils.assertCalculated("int(99.99999)", 99);
        TestUtils.assertCalculated("int(100.1)", 100);
    }

    @Test
    public void testCeil() {
        TestUtils.assertCalculated("ceil(5)", 5);
        TestUtils.assertCalculated("ceil(99.9)", 100);
        TestUtils.assertCalculated("ceil(100.00001)", 101);
    }

    @Test
    public void testRound() {
        TestUtils.assertCalculated("round(5)", 5);
        TestUtils.assertCalculated("round(99.9)", 100);
        TestUtils.assertCalculated("round(100.1)", 100);
        TestUtils.assertCalculated("round(100.5)", 101);
        TestUtils.assertCalculated("round(0.12345)", 0);
    }

    @Test
    public void testDiv() {
        TestUtils.assertCalculated("div(4,2)", 2);
        TestUtils.assertCalculated("div(5,2)", 2);
        TestUtils.assertCalculated("div(-19,3)", -6);
        TestUtils.assertCalculated("div(12345,-678)", -18);
    }

    @Test
    public void testMod() {
        TestUtils.assertCalculated("mod(4,2)", 0);
        TestUtils.assertCalculated("mod(5,2)", 1);
        TestUtils.assertCalculated("mod(-19,3)", -1);
        TestUtils.assertCalculated("mod(12345,-678)", 141);
    }

    @Test
    public void testPow() {
        TestUtils.assertCalculated("pow(0,0)", 1.0);
        TestUtils.assertCalculated("pow(0,1)", 0.0);
        TestUtils.assertCalculated("pow(2,2)", 4.0);
        TestUtils.assertCalculated("pow(-2,2)", 4.0);
        TestUtils.assertCalculated("pow(4,-2)", 0.0625);
        TestUtils.assertCalculated("pow(1.234,5.657)", 3.2852608509254346);

        TestUtils.assertCalculated("0^0", 1.0);
        TestUtils.assertCalculated("2^2", 4.0);
        TestUtils.assertCalculated("(1.234^5.657)", 3.2852608509254346);
    }

    @Test
    public void testMin() {
        TestUtils.assertCalculated("min(0,0)", 0.0);
        TestUtils.assertCalculated("min(1,0)", 0.0);
        TestUtils.assertCalculated("min(0,1)", 0.0);
        TestUtils.assertCalculated("min(0,-1)", -1.0);
        TestUtils.assertCalculated("min(4.123, -5.678)", -5.678);
        TestUtils.assertCalculatedLong("min(1234567890123456789, 1234567890123456788)", 1234567890123456788L);
        TestUtils.assertCalculatedLong("min(1234567890123456789, 123)", 123L);
        TestUtils.assertCalculated("min(1234567890123456789, 123.3)", 123.3);
    }

    @Test
    public void testMax() {
        TestUtils.assertCalculated("max(0,0)", 0.0);
        TestUtils.assertCalculated("max(1,0)", 1.0);
        TestUtils.assertCalculated("max(0,1)", 1.0);
        TestUtils.assertCalculated("max(0,-1)", 0.0);
        TestUtils.assertCalculated("max(4.123, -5.678)", 4.123);
        TestUtils.assertCalculatedLong("max(1234567890123456789, 1234567890123456788)", 1234567890123456789L);
        TestUtils.assertCalculatedLong("max(1234567890123456789, 123)", 1234567890123456789L);
        TestUtils.assertCalculated("max(1234567890123456789, 123.3)", (double)1234567890123456789L);
    }

    @Test
    public void testMinWithStringArgs() {
        TestUtils.assertCalculated("min('a','b')", "a");
        TestUtils.assertCalculated("min('this is a','longer string')", "longer string");
    }

    @Test
    public void testMaxWithStringArgs() {
        TestUtils.assertCalculated("max('a','b')", "b");
        TestUtils.assertCalculated("max('this is a','longer string')", "this is a");
    }

    @Test
    public void testSqrt() {
        TestUtils.assertCalculated("sqrt(0)", 0.0);
        TestUtils.assertCalculated("sqrt(64)", 8.0);
        TestUtils.assertCalculated("sqrt(5.5)", 2.345207879911715);
        TestUtils.assertCalculated("sqrt(1234)", 35.12833614050059);

    }

    @Test
    public void testSqr() {
        TestUtils.assertCalculated("sqr(0)", 0.0);
        TestUtils.assertCalculated("sqr(8)", 64.0);
        TestUtils.assertCalculated("sqr(-10)", 100.0);
        TestUtils.assertCalculated("sqr(2.345207879911715)", 5.5);
        TestUtils.assertCalculated("sqr(1234)", 1522756.0);
    }

    @Test
    public void testAbsWithIntArgs() {
        TestUtils.assertCalculated("abs(7)", 7);
        TestUtils.assertCalculated("abs(-150)", 150);
    }

    @Test
    public void testAbsWithDoubleArgs() {
        TestUtils.assertCalculated("abs(5.4321)", 5.4321);
        TestUtils.assertCalculated("abs(-5.4321)", 5.4321);
    }

    @Test
    public void testLog() {
        TestUtils.assertCalculated("log(10)", 1.0);
        TestUtils.assertCalculated("log(125)", 2.0969100130080562);
        TestUtils.assertCalculated("log(1000.5)", 3.00021709297223);
    }

    @Test
    public void testLn() {
        TestUtils.assertCalculated("ln(" + Math.E + ")", 1.0);
        TestUtils.assertCalculated("ln(10)", 2.302585092994046);
        TestUtils.assertCalculated("ln(1000000)", 13.815510557964274);
    }

    @Test
    public void testExp() {
        TestUtils.assertCalculated("exp(-3)", 0.049787068367863944);
        TestUtils.assertCalculated("exp(1)", Math.E);
        TestUtils.assertCalculated("exp(10.5)", 36315.502674246636);
    }

    @Test
    public void testRandom() {
        String resultAsString = TestUtils.getCalculated("random()");
        double d = Double.parseDouble(resultAsString);
        Assert.assertTrue(d >= 0.0 && d <= 1.0);
    }

    @Test
    public void testSegOverlap() {
        TestUtils.assertCalculated("segoverlap(0, 0, 0, 0)", 0);
        TestUtils.assertCalculated("segoverlap(0, 10, 15, 20)", 0);
        TestUtils.assertCalculated("segoverlap(15, 20, 0, 10)", 0);

        TestUtils.assertCalculated("segoverlap(0, 10, 0, 10)", 1);
        TestUtils.assertCalculated("segoverlap(0, 10, 5, 20)", 1);
        TestUtils.assertCalculated("segoverlap(5, 20, 0, 10)", 1);
        TestUtils.assertCalculated("segoverlap(5, 10, 0, 20)", 1);
        TestUtils.assertCalculated("segoverlap(0, 20, 5, 10)", 1);
    }

    @Test
    public void testSegDist() {
        TestUtils.assertCalculated("segdist(0, 0, 0, 0)", 1);
        TestUtils.assertCalculated("segdist(0, 10, 5, 20)", 0);
        TestUtils.assertCalculated("segdist(0, 10, 15, 20)", 6);
    }
}
