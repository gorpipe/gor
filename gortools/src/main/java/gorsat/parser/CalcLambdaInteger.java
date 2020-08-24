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

public class CalcLambdaInteger extends CalcLambdaNumeric implements IntegerType {
    private CvpIntegerLambda lambda;

    public CalcLambdaInteger(CvpIntegerLambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public String getType() {
        return FunctionTypes.IntFun();
    }

    @Override
    public int evaluateInt(ColumnValueProvider cvp) {
        return lambda.evaluate(cvp);
    }

    @Override
    public long evaluateLong(ColumnValueProvider cvp) {
        return lambda.evaluate(cvp);
    }

    @Override
    public String evaluateString(ColumnValueProvider cvp) {
        int value = lambda.evaluate(cvp);
        return String.valueOf(value);
    }

    @Override
    public double evaluateDouble(ColumnValueProvider cvp) {
        return lambda.evaluate(cvp);
    }

    @Override
    public boolean evaluateBoolean(ColumnValueProvider cvp) {
        throw new GorParsingException("Can't cast number to boolean");
    }

    @Override
    public TypedCalcLambda addedTo(TypedCalcLambda left) {
        return left.add(this);
    }

    @Override
    public TypedCalcLambda add(int other) {
        CvpIntegerLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other;
        return this;
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
        CvpIntegerLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other.evaluateInt(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda add(CalcLambdaLong other) {
        return new CalcLambdaLong(cvp -> evaluateLong(cvp) + other.evaluateLong(cvp));
    }

    @Override
    public TypedCalcLambda add(long other) {
        return new CalcLambdaLong(cvp -> evaluateLong(cvp) + other);
    }

    @Override
    public TypedCalcLambda add(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> evaluateLong(cvp) + other.evaluateDouble(cvp));
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
        return new CalcLambdaInteger(cvp -> evaluateInt(cvp) - other);
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
    public TypedCalcLambda subtract(CalcLambdaInteger other) {
        CvpIntegerLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) - other.evaluateInt(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaLong other) {
        return new CalcLambdaLong(cvp -> evaluateLong(cvp) - other.evaluateLong(cvp));
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> evaluateDouble(cvp) - other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaVariable other) {
        return other.toLambda().subtractedFrom(this);
    }

    @Override
    public TypedCalcLambda multipliedWith(TypedCalcLambda left) {
        return left.multiply(this);
    }

    @Override
    public TypedCalcLambda multiply(int other) {
        CvpIntegerLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) * other;
        return this;
    }

    @Override
    public TypedCalcLambda multiply(long other) {
        return new CalcLambdaLong(cvp -> evaluateLong(cvp) * other);
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
        CvpIntegerLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) * other.evaluateInt(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaLong other) {
        return new CalcLambdaLong(cvp -> evaluateLong(cvp) * other.evaluateLong(cvp));
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
        return new CalcLambdaDouble(cvp -> (double)lambda.evaluate(cvp) / (double)other);
    }

    @Override
    public TypedCalcLambda divide(long other) {
        return new CalcLambdaDouble(cvp -> (double)lambda.evaluate(cvp) / (double)other);
    }

    @Override
    public TypedCalcLambda divide(double other) {
        return new CalcLambdaDouble(cvp -> (double)lambda.evaluate(cvp) / other);
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

    public TypedCalcLambda divide(TypedCalcLambda o) {
        TypedCalcLambda other = o.toLambda();

        if (!(other instanceof Numeric)) {
            throw new GorParsingException("Number expected for / operator");
        }

        CvpIntegerLambda prev = lambda;
        if (other instanceof Constant) {
            double d = other.evaluateDouble(null);
            return new CalcLambdaDouble(cvp -> (double)lambda.evaluate(cvp) / d);
        } else {
            return new CalcLambdaDouble(cvp -> (double) prev.evaluate(cvp) / other.evaluateDouble(cvp));
        }
    }

    @Override
    public TypedCalcLambda pow(TypedCalcLambda o) {
        TypedCalcLambda other = o.toLambda();

        if (!(other instanceof Numeric)) {
            throw new GorParsingException("Number expected for ^ operator");
        }

        if (other instanceof IntegerType) {
            final CvpIntegerLambda prev = lambda;
            lambda = cvp -> (int) Math.pow(prev.evaluate(cvp), other.evaluateInt(cvp));
            return this;
        }

        if (other instanceof LongType) {
            return new CalcLambdaLong(cvp -> (long) Math.pow(evaluateLong(cvp), other.evaluateLong(cvp)));
        }

        if (other instanceof DoubleType) {
            return new CalcLambdaDouble(cvp -> Math.pow(evaluateDouble(cvp), other.evaluateDouble(cvp)));
        }

        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda negate() {
        CvpIntegerLambda prev = lambda;
        lambda = cvp -> -prev.evaluate(cvp);
        return this;
    }

}
