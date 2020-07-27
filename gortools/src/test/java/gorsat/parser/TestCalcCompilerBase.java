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

import org.antlr.v4.runtime.tree.ParseTree;
import org.gorpipe.gor.SyntaxChecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class TestCalcCompilerBase {
    protected void assertResult(String input, int expected) {
        TypedCalcLambda f = compileLambda(input, null);
        assertEquals(expected, f.evaluateInt(null));
    }

    protected void assertConstantResult(String input, int expected) {
        TypedCalcLambda f = compileLambda(input, null);
        assertTrue(f instanceof Constant);
        assertEquals(expected, f.evaluateInt(null));
    }

    protected void assertResult(String input, ColumnValue[] cols, int expected) {
        TypedCalcLambda f = compileLambda(input, cols);
        TestCalcCompilerCVP cvp = new TestCalcCompilerCVP(cols);
        assertEquals(expected, f.evaluateInt(cvp));
    }

    protected void assertResult(String input, double expected) {
        TypedCalcLambda f = compileLambda(input, null);
        assertEquals(expected, f.evaluateDouble(null), 1e-6);
    }

    protected void assertResult(String input, ColumnValue[] cols, double expected) {
        TypedCalcLambda f = compileLambda(input, cols);
        TestCalcCompilerCVP cvp = new TestCalcCompilerCVP(cols);
        assertEquals(expected, f.evaluateDouble(cvp), 1e-6);
    }

    protected void assertResult(String input, long expected) {
        TypedCalcLambda f = compileLambda(input, null);
        assertEquals(expected, f.evaluateLong(null));
    }

    protected void assertResult(String input, ColumnValue[] cols, long expected) {
        TypedCalcLambda f = compileLambda(input, cols);
        TestCalcCompilerCVP cvp = new TestCalcCompilerCVP(cols);
        assertEquals(expected, f.evaluateLong(cvp));
    }

    protected void assertResult(String input, String expected) {
        TypedCalcLambda f = compileLambda(input, null);
        assertEquals(expected, f.evaluateString(null));
    }

    protected void assertResult(String input, ColumnValue[] cols, String expected) {
        TypedCalcLambda f = compileLambda(input, cols);
        TestCalcCompilerCVP cvp = new TestCalcCompilerCVP(cols);
        assertEquals(expected, f.evaluateString(cvp));
    }

    private TypedCalcLambda compileLambda(String input, ColumnValue[] cols) {
        SyntaxChecker syntaxChecker = new SyntaxChecker();
        ParseTree context = syntaxChecker.parseCalc(input);
        CalcCompiler calcCompiler = new CalcCompiler();
        if (cols != null) {
            String[] names = new String[cols.length];
            String[] types = new String[cols.length];
            for (int i = 0; i < cols.length; i++) {
                names[i] = cols[i].getName();
                types[i] = cols[i].getType();
            }
            calcCompiler.setColumnNamesAndTypes(names, types);
        }
        TypedCalcLambda calcLambda = context.accept(calcCompiler);
        return calcLambda;
    }
}
