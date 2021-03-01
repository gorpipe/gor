/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

public class UTestCalcCompiler extends TestCalcCompilerBase {

    @Test
    public void intVariables() {
        ColumnValue[] cols = {
                new ColumnValue("x", "I", 42),
                new ColumnValue("y", "I", 3),
        };

        assertResult("x", cols, 42);
        assertResult("y", cols, 3);
    }

    @Test
    public void longVariables() {
        long x = (long)Integer.MAX_VALUE + 10;
        long y = (long)Integer.MAX_VALUE + 37;

        ColumnValue[] cols = {
                new ColumnValue("x", "L", x),
                new ColumnValue("y", "L", y),
        };

        assertResult("x", cols, x);
        assertResult("y", cols, y);
    }

    @Test
    public void doubleVariables() {
        double x = 42.0;
        double y = 3.14;

        ColumnValue[] cols = {
                new ColumnValue("x", "D", x),
                new ColumnValue("y", "D", y),
        };

        assertResult("x", cols, x);
        assertResult("y", cols, y);
    }

    @Test
    public void variablesCaseInsensitive() {
        ColumnValue[] cols = {
                new ColumnValue("x", "I", 42),
                new ColumnValue("Y", "I", 3),
        };
        assertResult("x", cols, 42);
        assertResult("y", cols, 3);
        assertResult("X", cols, 42);
        assertResult("Y", cols, 3);
    }

    @Test
    public void expressionsWithIntVariables() {
        final int x = 42;
        final int y = 3;
        ColumnValue[] cols = {
                new ColumnValue("x", "I", x),
                new ColumnValue("y", "I", y),
        };

        assertResult("x+3", cols, x+3);
        assertResult("x+3.1", cols, x+3.1);
        assertResult("x+12345678901", cols, x+12345678901L);
        assertResult("3+x", cols, 3+x);
        assertResult("12345678901+x", cols, 12345678901L+x);
        assertResult("3.1+x", cols, 3.1+x);
        assertResult("x-3", cols, x-3);
        assertResult("x-12345678901", cols, x-12345678901L);
        assertResult("x-3.1", cols, x-3.1);
        assertResult("3-x", cols, 3-x);
        assertResult("3.1-x", cols, 3.1-x);
        assertResult("x*3", cols, x*3);
        assertResult("x*12345678901", cols, x*12345678901L);
        assertResult("x*3.1", cols, x*3.1);
        assertResult("3*x", cols, 3*x);
        assertResult("12345678901*x", cols, 12345678901L*x);
        assertResult("3.1*x", cols, 3.1*x);
        assertResult("x/3", cols, (double)x/3);
        assertResult("x/12345678901", cols, (double)x/12345678901L);
        assertResult("x/3.1", cols, x/3.1);
        assertResult("3/x", cols, 3/(double)x);
        assertResult("12345678901/x", cols, 12345678901L/(double)x);
        assertResult("3.1/x", cols, 3.1/x);
        assertResult("x+y", cols, x+y);
        assertResult("x-y", cols, x-y);
        assertResult("x*y", cols, x*y);
        assertResult("x/y", cols, (double)x/y);
        assertResult("-x", cols, -x);

        assertResult("x+(x+x)", cols, x+(x+x));
        assertResult("(x+x)+x", cols, (x+x)+x);
        assertResult("(x+x)+3", cols, (x+x)+3);
        assertResult("3+(x+x)", cols, 3+(x+x));
        assertResult("(x+x)+3.14", cols, (x+x)+3.14);
        assertResult("3.14+(x+x)", cols, 3.14+(x+x));
        assertResult("(x+x)+12345678901", cols, (x+x)+12345678901L);
        assertResult("12345678901+(x+x)", cols, 12345678901L+(x+x));

        assertResult("x-(x+x)", cols, x-(x+x));
        assertResult("(x+x)-x", cols, (x+x)-x);
        assertResult("(x+x)-3", cols, (x+x)-3);
        assertResult("3-(x+x)", cols, 3-(x+x));
        assertResult("(x+x)-3.14", cols, (x+x)-3.14);
        assertResult("3.14-(x+x)", cols, 3.14-(x+x));
        assertResult("(x+x)-12345678901", cols, (x+x)-12345678901L);
        assertResult("12345678901-(x+x)", cols, 12345678901L-(x+x));

        assertResult("x*(x+x)", cols, x*(x+x));
        assertResult("(x+x)*x", cols, (x+x)*x);
        assertResult("(x+x)*3", cols, (x+x)*3);
        assertResult("3*(x+x)", cols, 3*(x+x));
        assertResult("(x+x)*3.14", cols, (x+x)*3.14);
        assertResult("3.14*(x+x)", cols, 3.14*(x+x));
        assertResult("(x+x)*12345678901", cols, (x+x)*12345678901L);
        assertResult("12345678901*(x+x)", cols, 12345678901L*(x+x));

        assertResult("x/(x+x)", cols, (double)x/(x+x));
        assertResult("(x+x)/x", cols, (double)(x+x)/x);
        assertResult("(x+x)/3", cols, (double)(x+x)/3);
        assertResult("3/(x+x)", cols, (double)3/(x+x));
        assertResult("(x+x)/3.14", cols, (x+x)/3.14);
        assertResult("3.14/(x+x)", cols, 3.14/(x+x));
        assertResult("(x+x)/12345678901", cols, (double)(x+x)/12345678901L);
        assertResult("12345678901/(x+x)", cols, (double)12345678901L/(x+x));
    }

    @Test
    public void expressionsWithLongVariables() {
        final long x = 42;
        final long y = 3;
        ColumnValue[] cols = {
                new ColumnValue("x", "L", x),
                new ColumnValue("y", "L", y),
        };

        assertResult("x+3", cols, x+3);
        assertResult("x+12345678901", cols, x+12345678901L);
        assertResult("x+3.1", cols, x+3.1);
        assertResult("3+x", cols, 3+x);
        assertResult("12345678901+x", cols, 12345678901L+x);
        assertResult("3.1+x", cols, 3.1+x);
        assertResult("x-3", cols, x-3);
        assertResult("x-12345678901", cols, x-12345678901L);
        assertResult("x-3.1", cols, x-3.1);
        assertResult("3-x", cols, 3-x);
        assertResult("12345678901-x", cols, 12345678901L-x);
        assertResult("3.1-x", cols, 3.1-x);
        assertResult("x*3", cols, x*3);
        assertResult("x*12345678901", cols, x*12345678901L);
        assertResult("x*3.1", cols, x*3.1);
        assertResult("3*x", cols, 3*x);
        assertResult("12345678901*x", cols, 12345678901L*x);
        assertResult("3.1*x", cols, 3.1*x);
        assertResult("x/3", cols, (double)x/3);
        assertResult("x/12345678901", cols, (double)x/12345678901L);
        assertResult("x/3.1", cols, x/3.1);
        assertResult("3/x", cols, 3/(double)x);
        assertResult("12345678901/x", cols, 12345678901L/(double)x);
        assertResult("3.1/x", cols, 3.1/x);
        assertResult("x+y", cols, x+y);
        assertResult("x-y", cols, x-y);
        assertResult("x*y", cols, x*y);
        assertResult("x/y", cols, (double)x/y);
        assertResult("-x", cols, -x);

        assertResult("x+(x+x)", cols, x+(x+x));
        assertResult("(x+x)+x", cols, (x+x)+x);
        assertResult("(x+x)+3", cols, (x+x)+3);
        assertResult("3+(x+x)", cols, 3+(x+x));
        assertResult("(x+x)+3.14", cols, (x+x)+3.14);
        assertResult("3.14+(x+x)", cols, 3.14+(x+x));
        assertResult("(x+x)+12345678901", cols, (x+x)+12345678901L);
        assertResult("12345678901+(x+x)", cols, 12345678901L+(x+x));

        assertResult("x-(x+x)", cols, x-(x+x));
        assertResult("(x+x)-x", cols, (x+x)-x);
        assertResult("(x+x)-3", cols, (x+x)-3);
        assertResult("3-(x+x)", cols, 3-(x+x));
        assertResult("(x+x)-3.14", cols, (x+x)-3.14);
        assertResult("3.14-(x+x)", cols, 3.14-(x+x));
        assertResult("(x+x)-12345678901", cols, (x+x)-12345678901L);
        assertResult("12345678901-(x+x)", cols, 12345678901L-(x+x));

        assertResult("x*(x+x)", cols, x*(x+x));
        assertResult("(x+x)*x", cols, (x+x)*x);
        assertResult("(x+x)*3", cols, (x+x)*3);
        assertResult("3*(x+x)", cols, 3*(x+x));
        assertResult("(x+x)*3.14", cols, (x+x)*3.14);
        assertResult("3.14*(x+x)", cols, 3.14*(x+x));
        assertResult("(x+x)*12345678901", cols, (x+x)*12345678901L);
        assertResult("12345678901*(x+x)", cols, 12345678901L*(x+x));

        assertResult("x/(x+x)", cols, (double)x/(x+x));
        assertResult("(x+x)/x", cols, (double)(x+x)/x);
        assertResult("(x+x)/3", cols, (double)(x+x)/3);
        assertResult("3/(x+x)", cols, (double)3/(x+x));
        assertResult("(x+x)/3.14", cols, (x+x)/3.14);
        assertResult("3.14/(x+x)", cols, 3.14/(x+x));
        assertResult("(x+x)/12345678901", cols, (double)(x+x)/12345678901L);
        assertResult("12345678901/(x+x)", cols, (double)12345678901L/(x+x));
    }

    @Test
    public void expressionsWithDoubleVariables() {
        final double x = 42.7;
        final double y = 3.14;
        ColumnValue[] cols = {
                new ColumnValue("x", "D", x),
                new ColumnValue("y", "D", y),
        };

        assertResult("x+3", cols, x+3);
        assertResult("x+12345678901", cols, x+12345678901L);
        assertResult("x+3.1", cols, x+3.1);
        assertResult("3+x", cols, 3+x);
        assertResult("12345678901+x", cols, 12345678901L+x);
        assertResult("3.1+x", cols, 3.1+x);
        assertResult("x-3", cols, x-3);
        assertResult("3-x", cols, 3-x);
        assertResult("12345678901-x", cols, 12345678901L-x);
        assertResult("3.1-x", cols, 3.1-x);
        assertResult("x-3.1", cols, x-3.1);
        assertResult("x*3", cols, x*3);
        assertResult("x*12345678901", cols, x*12345678901L);
        assertResult("x*3.1", cols, x*3.1);
        assertResult("3*x", cols, 3*x);
        assertResult("12345678901*x", cols, 12345678901L*x);
        assertResult("3.1*x", cols, 3.1*x);
        assertResult("x/3", cols, x/3);
        assertResult("x/12345678901", cols, x /12345678901L);
        assertResult("x/3.1", cols, x/3.1);
        assertResult("3/x", cols, 3/x);
        assertResult("12345678901/x", cols, 12345678901L/ x);
        assertResult("3.1/x", cols, 3.1/x);
        assertResult("x+y", cols, x+y);
        assertResult("x-y", cols, x-y);
        assertResult("x*y", cols, x*y);
        assertResult("x/y", cols, x/y);
        assertResult("-x", cols, -x);

        assertResult("x+(x+x)", cols, x+(x+x));
        assertResult("(x+x)+x", cols, (x+x)+x);
        assertResult("(x+x)+3", cols, (x+x)+3);
        assertResult("3+(x+x)", cols, 3+(x+x));
        assertResult("(x+x)+3.14", cols, (x+x)+3.14);
        assertResult("3.14+(x+x)", cols, 3.14+(x+x));
        assertResult("(x+x)+12345678901", cols, (x+x)+12345678901L);
        assertResult("12345678901+(x+x)", cols, 12345678901L+(x+x));

        assertResult("x-(x+x)", cols, x-(x+x));
        assertResult("(x+x)-x", cols, (x+x)-x);
        assertResult("(x+x)-3", cols, (x+x)-3);
        assertResult("3-(x+x)", cols, 3-(x+x));
        assertResult("(x+x)-3.14", cols, (x+x)-3.14);
        assertResult("3.14-(x+x)", cols, 3.14-(x+x));
        assertResult("(x+x)-12345678901", cols, (x+x)-12345678901L);
        assertResult("12345678901-(x+x)", cols, 12345678901L-(x+x));

        assertResult("x*(x+x)", cols, x*(x+x));
        assertResult("(x+x)*x", cols, (x+x)*x);
        assertResult("(x+x)*3", cols, (x+x)*3);
        assertResult("3*(x+x)", cols, 3*(x+x));
        assertResult("(x+x)*3.14", cols, (x+x)*3.14);
        assertResult("3.14*(x+x)", cols, 3.14*(x+x));
        assertResult("(x+x)*12345678901", cols, (x+x)*12345678901L);
        assertResult("12345678901*(x+x)", cols, 12345678901L*(x+x));

        assertResult("x/(x+x)", cols, x /(x+x));
        assertResult("(x+x)/x", cols, (x+x) /x);
        assertResult("(x+x)/3", cols, (x+x) /3);
        assertResult("3/(x+x)", cols, (double)3/(x+x));
        assertResult("(x+x)/3.14", cols, (x+x)/3.14);
        assertResult("3.14/(x+x)", cols, 3.14/(x+x));
        assertResult("(x+x)/12345678901", cols, (x+x) /12345678901L);
        assertResult("12345678901/(x+x)", cols, (double)12345678901L/(x+x));
    }

    @Test
    public void mixedTypeVariables() {
        final int x = 42;
        final long y = 123;
        final double z = 3.14;

        ColumnValue[] cols = {
                new ColumnValue("x", "I", x),
                new ColumnValue("y", "L", y),
                new ColumnValue("z", "D", z),
        };

        assertResult("x+y", cols,x+y);
        assertResult("x+z", cols,x+z);
        assertResult("y+z", cols,y+z);
        assertResult("y+x", cols,y+x);
        assertResult("z+x", cols,z+x);
        assertResult("z+y", cols,z+y);

        assertResult("x-y", cols,x-y);
        assertResult("x-z", cols,x-z);
        assertResult("y-z", cols,y-z);
        assertResult("y-x", cols,y-x);
        assertResult("z-x", cols,z-x);
        assertResult("z-y", cols,z-y);

        assertResult("x*y", cols,x*y);
        assertResult("x*z", cols,x*z);
        assertResult("y*z", cols,y*z);
        assertResult("y*x", cols,y*x);
        assertResult("z*x", cols,z*x);
        assertResult("z*y", cols,z*y);

        assertResult("x/y", cols,(double)x/y);
        assertResult("x/z", cols,x/z);
        assertResult("y/z", cols,y/z);
        assertResult("y/x", cols,(double)y/x);
        assertResult("z/x", cols,z/x);
        assertResult("z/y", cols,z/y);
    }

    @Test
    public void powOperatorWithConstants() {
        assertResult("3^2", 9);
        assertResult("3^-2", 0);
        assertResult("3^1.2", Math.pow(3, 1.2));
        assertResult("1^123456789012345", 1L);
        assertResult("123456789012345^1", 123456789012345L);
        assertResult("123456789012345^123456789012345", 9223372036854775807L);
        assertResult("123456789012345^1.0", 123456789012345.0);
        assertResult("3.14^2", Math.pow(3.14, 2));
        assertResult("3.14^1.2", Math.pow(3.14, 1.2));
        assertResult("1.0^123456789012345", 1.0);
    }

    @Test
    public void powOperatorWithVariables() {
        final int x = 42;
        final int x2 = 2;
        final long y = 123;
        final long y2 = 2;
        final double z = 3.14;
        final double z2 = 2.0;

        ColumnValue[] cols = {
                new ColumnValue("x", "I", x),
                new ColumnValue("x2", "I", x2),
                new ColumnValue("y", "L", y),
                new ColumnValue("y2", "L", y2),
                new ColumnValue("z", "D", z),
                new ColumnValue("z2", "D", z2),
        };

        assertResult("x^x2", cols, (int)Math.pow(x, x2));
        assertResult("x^y2", cols, (long)Math.pow(x, y2));
        assertResult("x^z2", cols, Math.pow(x, z2));
        assertResult("x^2", cols, (int)Math.pow(x, 2));

        assertResult("y^x2", cols, (long)Math.pow(y, x2));
        assertResult("y^y2", cols, (long)Math.pow(y, y2));
        assertResult("y^z2", cols, Math.pow(y, z2));

        assertResult("z^x2", cols, Math.pow(z, x2));
        assertResult("z^y2", cols, Math.pow(z, y2));
        assertResult("z^z2", cols, Math.pow(z, z2));

    }


    @Test
    public void columnReferences() {
        ColumnValue[] cols = {
                new ColumnValue("x", "S", "bingo"),
                new ColumnValue("y", "S", "bongo"),
                new ColumnValue("z", "S", "foo"),
        };

        assertResult("#2", cols, "bongo");
    }
}