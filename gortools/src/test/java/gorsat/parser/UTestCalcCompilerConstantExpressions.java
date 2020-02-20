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

import org.junit.Test;

public class UTestCalcCompilerConstantExpressions extends TestCalcCompilerBase {
    @Test
    public void numbersOnly() {
        assertResult("4", 4);
        assertResult("3.14", 3.14);
    }

    @Test
    public void longNumber() {
        long x = (long)Integer.MAX_VALUE + 10;
        String sx = String.valueOf(x);

        assertResult(sx, x);
    }

    @Test
    public void negativeNumbers() {
        assertResult("-3", -3);
        assertResult("-3.1", -3.1);
    }

    @Test
    public void negativeLong() {
        long z = -((long)Integer.MAX_VALUE + 10);
        String sz = String.valueOf(z);
        assertResult(sz, z);
    }

    @Test
    public void expressionsWithOnlyNumbers() {
        assertResult("4+1", 5);
        assertResult("3+1.4", 4.4);
        assertResult("3+123456789012", 3+123456789012L);
        assertResult("3.14+1.0", 4.14);
        assertResult("3.2+1", 4.2);
        assertResult("4-1", 3);
        assertResult("4-123456789012", 4-123456789012L);
        assertResult("3-1.4", 1.6);
        assertResult("3.2-1", 2.2);
        assertResult("3.2-123456789012", 3.2-123456789012L);
        assertResult("3.14-1.0", 2.14);
        assertResult("3*4", 12);
        assertResult("3*123456789012", 3*123456789012L);
        assertResult("3*4.1", 3*4.1);
        assertResult("3.1*4", 3.1*4);
        assertResult("3.1*123456789012", 3.1*123456789012L);
        assertResult("3.1*42.7", 3.1*42.7);
        assertResult("8/2", 4);
        assertResult("2/8", 0.25);
        assertResult("2/8.1", 2/8.1);
        assertResult("123456789012/2", 123456789012L/2);
        assertResult("1234567890123/2", 1234567890123L/2.0);
        assertResult("8.1/2", 8.1/2);
    }

    @Test
    public void constantExpressions() {
        assertConstantResult("4", 4);
        assertConstantResult("4+3", 7);
        assertConstantResult("(4+3)*2", 14);
        assertConstantResult("(4+3)*2^2", 28);
    }

    @Test
    public void expressionsWithLongNumbers() {
        long x = (long)Integer.MAX_VALUE + 10;
        String sx = String.valueOf(x);

        long y = (long)Integer.MAX_VALUE + 37;
        String sy = String.valueOf(y);

        assertResult(sx + "+" + sy, x+y);
        assertResult(sx + "-" + sy, x-y);
        assertResult(sx + "*" + sy, x*y);
        assertResult(sx + "/" + sy, (double)x/y);

        int i = 42;
        String si = String.valueOf(i);
        assertResult(sx + "+" + si, x+i);
        assertResult(sx + "-" + si, x-i);
        assertResult(sx + "*" + si, x*i);
        assertResult(sx + "/" + si, (double)x/i);

        double c = 3.14;
        String sc = String.valueOf(c);
        assertResult(sx + "+" + sc, x+c);
        assertResult(sx + "-" + sc, x-c);
        assertResult(sx + "*" + sc, x*c);
        assertResult(sx + "/" + sc, x/c);
    }

    @Test
    public void expressionsWithLongNumbersAndOtherTypes() {
        long x = (long)Integer.MAX_VALUE + 10;
        String sx = String.valueOf(x);

        assertResult(sx + "+3", x+3);
        assertResult(sx + "+3.14", x+3.14);
        assertResult("3+" + sx, 3+x);
        assertResult("3.14+" + sx, 3.14+x);

        assertResult(sx + "-3", x-3);
        assertResult(sx + "-3.14", x-3.14);
        assertResult("3-" + sx, 3-x);
        assertResult("3.14-" + sx, 3.14-x);

        assertResult(sx + "*3", x*3);
        assertResult(sx + "*3.14", x*3.14);
        assertResult("3*" + sx, 3*x);
        assertResult("3.14*" + sx, 3.14*x);

        assertResult(sx + "/3", (double)x/3);
        assertResult(sx + "/3.14", x/3.14);
        assertResult("3/" + sx, (double)3/x);
        assertResult("3.14/" + sx, 3.14/x);
    }

    @Test
    public void parentheses() {
        assertResult("(3)", 3);
        assertResult("(3 + 4)*2", (3 + 4)*2);
    }

}
