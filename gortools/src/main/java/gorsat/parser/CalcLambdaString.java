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

public class CalcLambdaString implements StringType, TypedCalcLambda {
    private CvpStringLambda lambda;

    public CalcLambdaString(CvpStringLambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public String getType() {
        return FunctionTypes.StringFun();
    }

    @Override
    public int evaluateInt(ColumnValueProvider columnValueProvider) {
        throw new GorParsingException("Can't cast string to integer");
    }

    @Override
    public long evaluateLong(ColumnValueProvider cvp) {
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public String evaluateString(ColumnValueProvider cvp) {
        return lambda.evaluate(cvp);
    }

    @Override
    public double evaluateDouble(ColumnValueProvider cvp) {
        throw new GorParsingException("Can't cast string to number");
    }

    @Override
    public boolean evaluateBoolean(ColumnValueProvider cvp) {
        throw new GorParsingException("Can't cast string to boolean");
    }

    @Override
    public TypedCalcLambda addedTo(TypedCalcLambda left) {
        return left.add(this);
    }

    @Override
    public TypedCalcLambda add(CalcLambdaVariable other) {
        return other.toLambda().addedTo(this);
    }

    @Override
    public TypedCalcLambda add(CalcLambdaString other) {
        CvpStringLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other.evaluateString(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda add(String other) {
        CvpStringLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other;
        return this;
    }

    @Override
    public TypedCalcLambda add(CalcLambdaInteger other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda add(int other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda add(CalcLambdaLong other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda add(long other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda add(CalcLambdaDouble other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda add(double other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda subtractedFrom(TypedCalcLambda other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda subtract(int other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda subtract(long other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda subtract(double other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaVariable other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaInteger other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaLong other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaDouble other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multipliedWith(TypedCalcLambda left) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multiply(int other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multiply(long other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multiply(double other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaVariable other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaInteger other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaLong other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaDouble other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda dividedInto(TypedCalcLambda left) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda divide(int other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda divide(long other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda divide(double other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaVariable other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaInteger other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaLong other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaDouble other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    public TypedCalcLambda add(TypedCalcLambda o) {
        TypedCalcLambda other = o.toLambda();

        CvpStringLambda prev = lambda;
        if (other instanceof CalcLambdaStringConstant) {
            String c = other.evaluateString(null);
            lambda = cvp -> prev.evaluate(cvp) + c;
            return this;
        }
        if (other instanceof CalcLambdaString) {
            lambda = cvp -> prev.evaluate(cvp) + other.evaluateString(cvp);
            return this;
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda pow(TypedCalcLambda other) {
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda negate() {
        throw new GorParsingException("Negation not applicable for string");
    }

    @Override
    public TypedCalcLambda compare(TypedCalcLambda o, int op) {
        TypedCalcLambda other = o.toLambda();

        if (!(other instanceof StringType)) {
            throw TypedCalcLambda.getIncompatibleTypes();
        }
        switch (op) {
            case GorScriptParser.EQ:
            case GorScriptParser.S_EQ:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateString(cvp).equals(other.evaluateString(cvp))));
            case GorScriptParser.NE:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> !evaluateString(cvp).equals(other.evaluateString(cvp))));
            case GorScriptParser.GT:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateString(cvp).compareTo(other.evaluateString(cvp)) > 0));
            case GorScriptParser.GE:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateString(cvp).compareTo(other.evaluateString(cvp)) >= 0));
            case GorScriptParser.LT:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateString(cvp).compareTo(other.evaluateString(cvp)) < 0));
            case GorScriptParser.LE:
                return (new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateString(cvp).compareTo(other.evaluateString(cvp)) <= 0));
            case GorScriptParser.LIKE:
                if (other instanceof CalcLambdaStringConstant) {
                    final String pattern = other.evaluateString(null).replace("*", ".*").replace("?", ".");
                    return new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateString(cvp).matches(pattern));
                } else {
                    return new CalcLambdaBoolean((ColumnValueProvider cvp) -> {
                        final String pattern = other.evaluateString(cvp).replace("*", ".*").replace("?", ".");
                        return evaluateString(cvp).matches(pattern);
                    });
                }
            case GorScriptParser.RLIKE:
                if (other instanceof CalcLambdaStringConstant) {
                    final String pattern = other.evaluateString(null);
                    return new CalcLambdaBoolean((ColumnValueProvider cvp) -> evaluateString(cvp).matches(pattern));
                } else {
                    return new CalcLambdaBoolean((ColumnValueProvider cvp) -> {
                        final String pattern = other.evaluateString(cvp);
                        return evaluateString(cvp).matches(pattern);
                    });
                }
            default:
                throw new GorParsingException("Operator not supported for string");
        }
    }
}
