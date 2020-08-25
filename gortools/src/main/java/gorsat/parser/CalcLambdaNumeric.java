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

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.model.ColumnValueProvider;
import org.gorpipe.gor.GorScriptParser;

public abstract class CalcLambdaNumeric implements Numeric, TypedCalcLambda {
    @Override
    public TypedCalcLambda compare(TypedCalcLambda o, int op) {
        TypedCalcLambda other = o.toLambda();

        if (!(other instanceof Numeric)) {
            throw new GorParsingException("Number can only be compared to another number");
        }
        switch (op) {
            case GorScriptParser.EQ:
            case GorScriptParser.S_EQ:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateDouble(cvp) == other.evaluateDouble(cvp)));
            case GorScriptParser.NE:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateDouble(cvp) != other.evaluateDouble(cvp)));
            case GorScriptParser.GT:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateDouble(cvp) > other.evaluateDouble(cvp)));
            case GorScriptParser.GE:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateDouble(cvp) >= other.evaluateDouble(cvp)));
            case GorScriptParser.LT:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateDouble(cvp) < other.evaluateDouble(cvp)));
            case GorScriptParser.LE:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateDouble(cvp) <= other.evaluateDouble(cvp)));
            default:
                throw new GorParsingException("Unknown operator");
        }
    }
}
