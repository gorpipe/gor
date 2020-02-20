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
import org.junit.Test;

public class UTestTrigFunctions {
    @Test
    public void testSin() {
        TestUtils.assertCalculated("sin(0)", 0.0);
        TestUtils.assertCalculated("sin(1)", 0.8414709848078965);
        TestUtils.assertCalculated("sin(" + (Math.PI / 2) + ")", 1.0);
    }

    @Test
    public void testCos() {
        TestUtils.assertCalculated("cos(0)", 1.0);
        TestUtils.assertCalculated("cos(1)", 0.5403023058681398);
        TestUtils.assertCalculated("cos(" + (Math.PI * 2) + ")", 1.0);
    }

    @Test
    public void testAsin() {
        TestUtils.assertCalculated("asin(0)", 0.0);
        TestUtils.assertCalculated("asin(0.8414709848078965)", 1.0);
        TestUtils.assertCalculated("asin(1)", Math.PI / 2);
    }

    @Test
    public void testAcos() {
        TestUtils.assertCalculated("acos(1)", 0.0);
        TestUtils.assertCalculated("acos(0.5403023058681398)", 1.0);
        TestUtils.assertCalculated("acos(-1)", Math.PI);
    }

    @Test
    public void testTan() {
        TestUtils.assertCalculated("tan(0)", 0.0);
        TestUtils.assertCalculated("tan(1)", 1.5574077246549023);
        //TestUtils.assertCalculated("tan(" + (Math.PI/4) + ")", "1.0");
    }

    @Test
    public void testAtan() {
        TestUtils.assertCalculated("atan(0)", 0.0);
        TestUtils.assertCalculated("atan(1.5574077246549023)", 1.0);
        TestUtils.assertCalculated("atan(1)", Math.PI / 4);
    }

}
