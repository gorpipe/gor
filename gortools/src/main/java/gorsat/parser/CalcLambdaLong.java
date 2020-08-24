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
import org.gorpipe.gor.ColumnValueProvider;

public class CalcLambdaLong extends CalcLambdaNumeric implements LongType {
    private CvpLongLambda lambda;

    public CalcLambdaLong(CvpLongLambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public String getType() {
        return FunctionTypes.LongFun();
    }

    @Override
    public int evaluateInt(ColumnValueProvider columnValueProvider) {
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public long evaluateLong(ColumnValueProvider cvp) {
        return lambda.evaluate(cvp);
    }

    @Override
    public double evaluateDouble(ColumnValueProvider cvp) {
        return lambda.evaluate(cvp);
    }

    @Override
    public String evaluateString(ColumnValueProvider cvp) {
        long value = lambda.evaluate(cvp);
        return String.valueOf(value);
    }

    @Override
    public boolean evaluateBoolean(ColumnValueProvider cvp) {
        return false;
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
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda add(String other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda add(CalcLambdaInteger other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other.evaluateLong(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda add(int other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other;
        return this;
    }

    @Override
    public TypedCalcLambda add(CalcLambdaLong other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other.evaluateLong(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda add(long other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other;
        return this;
    }

    @Override
    public TypedCalcLambda add(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) + other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda add(double other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) + other);
    }

    @Override
    public TypedCalcLambda subtractedFrom(TypedCalcLambda other) {
        return other.subtract(this);
    }

    @Override
    public TypedCalcLambda subtract(int other) {
        return new CalcLambdaLong(cvp -> evaluateLong(cvp) - other);
    }

    @Override
    public TypedCalcLambda subtract(long other) {
        return new CalcLambdaLong(cvp -> evaluateLong(cvp) - other);
    }

    @Override
    public TypedCalcLambda subtract(double other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) - other);
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaVariable other) {
        return other.toLambda().subtractedFrom(this);
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaInteger other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) - other.evaluateLong(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) - other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaLong other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) - other.evaluateLong(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda multipliedWith(TypedCalcLambda left) {
        return left.multiply(this);
    }

    @Override
    public TypedCalcLambda multiply(int other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) * other;
        return this;
    }

    @Override
    public TypedCalcLambda multiply(long other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) * other;
        return this;
    }

    @Override
    public TypedCalcLambda multiply(double other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) * other);
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaVariable other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaInteger other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) * other.evaluateLong(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaLong other) {
        CvpLongLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) * other.evaluateLong(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) * other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda dividedInto(TypedCalcLambda left) {
        return left.divide(this);
    }

    @Override
    public TypedCalcLambda divide(int other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) / (double)other);
    }

    @Override
    public TypedCalcLambda divide(long other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) / (double)other);
    }

    @Override
    public TypedCalcLambda divide(double other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) / other);
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaVariable other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaInteger other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) / other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaLong other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) / other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) / other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda pow(TypedCalcLambda o) {
        TypedCalcLambda other = o.toLambda();

        if (!(other instanceof Numeric)) {
            throw new GorParsingException("Number expected for ^ operator");
        }

        if (other instanceof CalcLambdaLong || other instanceof CalcLambdaInteger) {
            final CvpLongLambda prev = lambda;
            lambda = cvp -> (long) Math.pow(prev.evaluate(cvp), other.evaluateLong(cvp));
            return this;
        }

        if (other instanceof CalcLambdaDouble) {
            return new CalcLambdaDouble(cvp -> Math.pow(evaluateDouble(cvp), other.evaluateDouble(cvp)));
        }

        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda negate() {
        CvpLongLambda prev = lambda;
        lambda = cvp -> -prev.evaluate(cvp);
        return this;
    }
}
