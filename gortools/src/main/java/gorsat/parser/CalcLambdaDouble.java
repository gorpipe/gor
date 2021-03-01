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

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.model.ColumnValueProvider;

public class CalcLambdaDouble extends CalcLambdaNumeric implements DoubleType {
    private CvpDoubleLambda lambda;

    public CalcLambdaDouble(CvpDoubleLambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public int evaluateInt(ColumnValueProvider columnValueProvider) {
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public String getType() {
        return FunctionTypes.DoubleFun();
    }

    @Override
    public long evaluateLong(ColumnValueProvider cvp) {
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public String evaluateString(ColumnValueProvider cvp) {
        double value = lambda.evaluate(cvp);
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
    public TypedCalcLambda add(CalcLambdaInteger other) {
        return addNumber(other);
    }

    @Override
    public TypedCalcLambda add(int other) {
        return addNumber(other);
    }

    @Override
    public TypedCalcLambda add(CalcLambdaLong other) {
        return addNumber(other);
    }

    @Override
    public TypedCalcLambda add(long other) {
        return addNumber(other);
    }

    @Override
    public TypedCalcLambda add(CalcLambdaDouble other) {
        return addNumber(other);
    }

    @Override
    public TypedCalcLambda add(double other) {
        return addNumber(other);
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

    private TypedCalcLambda addNumber(TypedCalcLambda other) {
        CvpDoubleLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other.evaluateDouble(cvp);
        return this;
    }

    private TypedCalcLambda addNumber(double other) {
        CvpDoubleLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) + other;
        return this;
    }

    @Override
    public TypedCalcLambda subtractedFrom(TypedCalcLambda other) {
        return other.subtract(this);
    }

    @Override
    public TypedCalcLambda subtract(int other) {
        return subtract((double)other);
    }

    @Override
    public TypedCalcLambda subtract(long other) {
        return subtract((double)other);
    }

    @Override
    public TypedCalcLambda subtract(double other) {
        CvpDoubleLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) - other;
        return this;
    }

    private TypedCalcLambda subtractNumber(TypedCalcLambda other) {
        CvpDoubleLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) - other.evaluateDouble(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaInteger other) {
        return subtractNumber(other);
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaLong other) {
        return subtractNumber(other);
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaDouble other) {
        return subtractNumber(other);
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
        return multiply((double)other);
    }

    @Override
    public TypedCalcLambda multiply(long other) {
        return multiply((double)other);
    }

    @Override
    public TypedCalcLambda multiply(double other) {
        CvpDoubleLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) * other;
        return this;
    }

    private TypedCalcLambda multiplyNumber(TypedCalcLambda other) {
        CvpDoubleLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) * other.evaluateDouble(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaInteger other) {
        return multiplyNumber(other);
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaLong other) {
        return multiplyNumber(other);
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaDouble other) {
        return multiplyNumber(other);
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaVariable other) {
        return other.toLambda().multipliedWith(this);
    }

    @Override
    public TypedCalcLambda dividedInto(TypedCalcLambda left) {
        return left.divide(this);
    }

    @Override
    public TypedCalcLambda divide(int other) {
        return divide((double)other);
    }

    @Override
    public TypedCalcLambda divide(long other) {
        return divide((double)other);
    }

    @Override
    public TypedCalcLambda divide(double other) {
        CvpDoubleLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) / other;
        return this;
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaVariable other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    private TypedCalcLambda divideNumber(TypedCalcLambda other) {
        CvpDoubleLambda prev = lambda;
        lambda = cvp -> prev.evaluate(cvp) / other.evaluateDouble(cvp);
        return this;
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaInteger other) {
        return divideNumber(other);
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaLong other) {
        return divideNumber(other);
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaDouble other) {
        return divideNumber(other);
    }

    @Override
    public TypedCalcLambda pow(TypedCalcLambda o) {
        TypedCalcLambda other = o.toLambda();

        if (!(other instanceof Numeric)) {
            throw new GorParsingException("Number expected for ^ operator");
        }

        final CvpDoubleLambda prev = lambda;
        lambda = cvp -> Math.pow(prev.evaluate(cvp), other.evaluateDouble(cvp));
        return this;
    }

    @Override
    public TypedCalcLambda negate() {
        CvpDoubleLambda prev = lambda;
        lambda = cvp -> -prev.evaluate(cvp);
        return this;
    }
}
