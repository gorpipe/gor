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

public class UTestCalcCompilerPredicates extends TestCalcCompilerBase {
    @Test
    public void ifExpressionNumericConstants() {
        assertResult("if(1==0, 1, 0)", 0);
        assertResult("if(1!=0, 1, 0)", 1);
        assertResult("if(1>0, 1, 0)", 1);
        assertResult("if(1>=0, 1, 0)", 1);
        assertResult("if(1<0, 1, 0)", 0);
        assertResult("if(1<=0, 1, 0)", 0);

        assertResult("if(111111234567000==111111234566000, 1, 0)", 0);
        assertResult("if(111111234567000!=111111234566000, 1, 0)", 1);
        assertResult("if(111111234567000>111111234566000, 1, 0)", 1);
        assertResult("if(111111234567000>=111111234566000, 1, 0)", 1);
        assertResult("if(111111234567000<111111234566000, 1, 0)", 0);
        assertResult("if(111111234567000<=111111234566000, 1, 0)", 0);

        assertResult("if(1==0.9, 1, 0)", 0);
        assertResult("if(1!=0.9, 1, 0)", 1);
        assertResult("if(1>0.9, 1, 0)", 1);
        assertResult("if(1>=0.9, 1, 0)", 1);
        assertResult("if(1<0.9, 1, 0)", 0);
        assertResult("if(1<=0.9, 1, 0)", 0);

        assertResult("if(0.1==0, 1, 0)", 0);
        assertResult("if(0.1!=0, 1, 0)", 1);
        assertResult("if(0.1>0, 1, 0)", 1);
        assertResult("if(0.1>=0, 1, 0)", 1);
        assertResult("if(0.1<0, 1, 0)", 0);
        assertResult("if(0.1<=0, 1, 0)", 0);

        assertResult("if(1.0==0.0, 1, 0)", 0);
        assertResult("if(1.0!=0.0, 1, 0)", 1);
        assertResult("if(1.0>0.0, 1, 0)", 1);
        assertResult("if(1.0>=0.0, 1, 0)", 1);
        assertResult("if(1.0<0.0, 1, 0)", 0);
        assertResult("if(1.0<=0.0, 1, 0)", 0);
    }

    @Test
    public void ifExpressionDoubleVariables() {
        final double x = 1.0;
        final double y = 0.0;
        ColumnValue[] cols = {
                new ColumnValue("x", "D", x),
                new ColumnValue("y", "D", y),
        };

        assertResult("if(x==y, 1, 0)", cols,0);
        assertResult("if(x!=y, 1, 0)", cols, 1);
        assertResult("if(x>y, 1, 0)", cols, 1);
        assertResult("if(x>=y, 1, 0)", cols, 1);
        assertResult("if(x<y, 1, 0)", cols, 0);
        assertResult("if(x<=y, 1, 0)", cols, 0);
    }

    @Test
    public void ifExpressionIntegerVariables() {
        final int x = 1;
        final int y = 0;
        final double x1 = 1.1;
        final double y1 = 0.1;
        ColumnValue[] cols = {
                new ColumnValue("x", "I", x),
                new ColumnValue("y", "I", y),
                new ColumnValue("x1", "D", x1),
                new ColumnValue("y1", "D", y1),
        };

        assertResult("if(x==y, 1, 0)", cols,0);
        assertResult("if(x!=y, 1, 0)", cols, 1);
        assertResult("if(x>y, 1, 0)", cols, 1);
        assertResult("if(x>=y, 1, 0)", cols, 1);
        assertResult("if(x<y, 1, 0)", cols, 0);
        assertResult("if(x<=y, 1, 0)", cols, 0);

        assertResult("if(x==x1, 1, 0)", cols,0);
        assertResult("if(x!=x1, 1, 0)", cols, 1);
    }

    @Test
    public void ifExpressionLongVariables() {
        final long x = 1;
        final long y = 0;
        ColumnValue[] cols = {
                new ColumnValue("x", "L", x),
                new ColumnValue("y", "L", y),
        };

        assertResult("if(x==y, 1, 0)", cols,0);
        assertResult("if(x!=y, 1, 0)", cols, 1);
        assertResult("if(x>y, 1, 0)", cols, 1);
        assertResult("if(x>=y, 1, 0)", cols, 1);
        assertResult("if(x<y, 1, 0)", cols, 0);
        assertResult("if(x<=y, 1, 0)", cols, 0);
    }

    @Test
    public void ifExpressionStringConstants() {
        assertResult("if('a'=='b', 'a', 'b')", "b");
        assertResult("if('a'!='b', 'a', 'b')", "a");
        assertResult("if('a'>'b', 'a', 'b')", "b");
        assertResult("if('a'>='b', 'a', 'b')", "b");
        assertResult("if('a'<'b', 'a', 'b')", "a");
        assertResult("if('a'<='b', 'a', 'b')", "a");
    }

    @Test
    public void ifExpressionStringVariables() {
        final String x = "1";
        final String y = "0";
        ColumnValue[] cols = {
                new ColumnValue("x", "S", x),
                new ColumnValue("y", "S", y),
        };

        assertResult("if(x==y, 1, 0)", cols,0);
        assertResult("if(x!=y, 1, 0)", cols, 1);
        assertResult("if(x>y, 1, 0)", cols, 1);
        assertResult("if(x>=y, 1, 0)", cols, 1);
        assertResult("if(x<y, 1, 0)", cols, 0);
        assertResult("if(x<=y, 1, 0)", cols, 0);
    }

    @Test
    public void booleanExpressions() {
        assertResult("if (1>0 or 0>1, 1, 0)", 1);
        assertResult("if (1>0 and 0>1, 1, 0)", 0);
        assertResult("if (1>0 and 0>1 or 1>0, 1, 0)", 1);
        assertResult("if (1>0 and 0>1 and 1>0, 1, 0)", 0);
        assertResult("if((1>0), 1, 0)", 1);
        assertResult("if(not(1>0), 1, 0)", 0);
    }

    @Test
    public void booleanFunctions() {
        assertResult("if (isint('1'), 1, 0)", 1);
    }

    @Test
    public void ifExpressionReturnTypes() {
        assertResult("if(1==0, 1.2, 0.7)", 0.7);
        assertResult("if(1==0, 'true', 'false')", "false");
    }

    @Test
    public void inExpression() {
        assertResult("if('a' in ('b'), 'true', 'false')", "false");
        assertResult("if('b' in ('a','b','c','d'), 'true', 'false')", "true");
        assertResult("if('a' in ('b','c','d','e'), 'true', 'false')", "false");

    }
}
