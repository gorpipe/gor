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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Created by sigmar on 07/12/2016.
 */
public class UTestCalcDivision {

    private static final Logger log = LoggerFactory.getLogger(UTestCalcDivision.class);

    @Test
    public void testFloatDivision() {
        String gorcmd = "gorrow chr1,1,2 | calc a -1 | calc b float(10) | calc c a/b";
        CalculateAndPerformTest(gorcmd, "Test floating point division", (Double d) -> d.equals(-0.1), 5);
    }

    @Test
    public void testIntegerDivision() {
        String gorcmd = "gorrow chr1,1,2 | calc a 20 | calc b 3 | calc c a/b";
        CalculateAndPerformTest(gorcmd, "Test infinity by float casting", (Double d) -> d.equals(6.0), 5);
    }

    @Test
    public void testFloatDivisionByZero() {
        String gorcmd = "gorrow chr1,1,2 | calc a 1 | calc b float(0) | calc c a/b";
        CalculateAndPerformTest(gorcmd, "Test infinity by float casting", (Double d) -> Double.isInfinite(d), 5);
    }

    @Test()
    public void testIntegerDivisionByZero() {
        String gorcmd = "gorrow chr1,1,2 | calc a 1 | calc b 0 | calc c a/b";
        CalculateAndPerformTest(gorcmd, "Test infinity by float casting", (Double d) -> Double.isInfinite(d), 5);
    }

    @Test
    public void testIntegerDivisionByZeroWithFormatting() {
        String gorcmd = "gorrow chr1,1,2 | calc a 0 | calc b 0 | calc c form(a/b, 4,4)";
        CalculateAndPerformTest(gorcmd, "Testing NaN with form function", (Double d) -> Double.isNaN(d), 5);
    }

    @Test
    public void testIntegerDivisionByZeroWithFormatting2() {
        String gorcmd = "gorrow chr1,1,2 | calc a float(0) | calc b float(0) | calc c form(a/b, 4,4)";
        CalculateAndPerformTest(gorcmd, "Test NaN with character values and form function", (Double d) -> Double.isNaN(d), 5);
    }

    private static void CalculateAndPerformTest(String query, String comment, Function<Double, Boolean> test, int targetColumn) {
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        test.apply(Double.parseDouble(lines[0].split("\t")[targetColumn]));
    }
}
