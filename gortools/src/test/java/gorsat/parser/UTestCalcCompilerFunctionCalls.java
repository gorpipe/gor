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

public class UTestCalcCompilerFunctionCalls extends TestCalcCompilerBase {
    @Test
    public void functionCall() {
        ColumnValue[] cols = {
                new ColumnValue("x", "D", 1.0),
                new ColumnValue("y", "I", 1),
        };
        assertResult("sin(1)", cols, 0.8414709848);
        assertResult("sin(1.0)", cols, 0.8414709848);
        assertResult("sin(x)", cols, 0.8414709848);
        assertResult("sin(y)", cols, 0.8414709848);
    }

    @Test
    public void functionCallTwoArgs() {
        assertResult("chi2(3,3)", 0.3916251762710878);
        assertResult("mod(12345,-678)", 141);
    }

    @Test
    public void functionCallStringArgs() {
        assertResult("upper('abc')", "ABC");
    }

    @Test
    public void functionCallVariants() {
        assertResult("float('bingo', -1.0)", -1.0);
        assertResult("float('3')", 3.0);
    }

    @Test
    public void functionCallWithStringList() {
        assertResult("if(contains('the test string', 't', 'h', 'e', 's'), 'true', 'false')", "true");
    }

    @Test
    public void functionCallBooleanReturn() {
        assertResult("if(containsany('abcdefg', 'a'), 'true', 'false')", "true");
    }

    @Test
    public void functionCallLongReturn() {
        assertResult("epoch('16/06/2017','dd/MM/yyyy')", 1497571200000L);
    }

    @Test
    public void functionCallCaseInsensitive() {
        assertResult("sin(1)", 0.8414709848);
        assertResult("SIN(1)", 0.8414709848);
    }

    @Test
    public void functionCallShouldAllowNumericVariableWhenStringExpected() {
        ColumnValue[] cols = {
                new ColumnValue("x", "I", 10),
                new ColumnValue("y", "L", 123456789012345L),
                new ColumnValue("z", "D", 3.14),
        };
        assertResult("len(x)", cols,2);
        assertResult("len(y)", cols,15);
        assertResult("len(z)", cols,4);
    }
}
