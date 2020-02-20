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

public class CalcLambdaLongConstant extends CalcLambdaNumeric implements LongType, Constant {
    private long value;

    CalcLambdaLongConstant(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
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
        return value;
    }

    @Override
    public double evaluateDouble(ColumnValueProvider cvp) {
        return value;
    }

    @Override
    public String evaluateString(ColumnValueProvider cvp) {
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public boolean evaluateBoolean(ColumnValueProvider cvp) {
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda addedTo(TypedCalcLambda left) {
        return left.add(value);
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
    public TypedCalcLambda add(int other) {
        value += other;
        return this;
    }

    @Override
    public TypedCalcLambda add(long other) {
        value += other;
        return this;
    }

    @Override
    public TypedCalcLambda add(double other) {
        return new CalcLambdaDoubleConstant((double) value + other);
    }

    @Override
    public TypedCalcLambda add(CalcLambdaInteger other) {
        return new CalcLambdaLong(cvp -> value + other.evaluateInt(cvp));
    }

    @Override
    public TypedCalcLambda add(CalcLambdaLong other) {
        return new CalcLambdaLong(cvp -> value + other.evaluateLong(cvp));
    }

    @Override
    public TypedCalcLambda add(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> (double) value + other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda subtractedFrom(TypedCalcLambda other) {
        return other.subtract(value);
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaVariable other) {
        return other.toLambda().subtractedFrom(this);
    }

    @Override
    public TypedCalcLambda subtract(int other) {
        value -= other;
        return this;
    }

    @Override
    public TypedCalcLambda subtract(long other) {
        value -= other;
        return this;
    }

    @Override
    public TypedCalcLambda subtract(double other) {
        return new CalcLambdaDoubleConstant( (double)value - other);
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaInteger other) {
        return new CalcLambdaLong(cvp -> value - other.evaluateLong(cvp));
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaLong other) {
        return new CalcLambdaLong(cvp -> value - other.evaluateLong(cvp));
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> (double) value - other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda multipliedWith(TypedCalcLambda left) {
        return left.multiply(value);
    }

    @Override
    public TypedCalcLambda multiply(int other) {
        value *= other;
        return this;
    }

    @Override
    public TypedCalcLambda multiply(long other) {
        value *= other;
        return this;
    }

    @Override
    public TypedCalcLambda multiply(double other) {
        return new CalcLambdaDoubleConstant( (double)value * other);
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaVariable other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaInteger other) {
        return new CalcLambdaLong(cvp -> value * other.evaluateLong(cvp));
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaLong other) {
        return new CalcLambdaLong(cvp -> value * other.evaluateLong(cvp));
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> value * other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda dividedInto(TypedCalcLambda left) {
        return left.divide(value);
    }

    @Override
    public TypedCalcLambda divide(int other) {
        if (value % other == 0) {
            value /= other;
            return this;
        } else {
            return new CalcLambdaDoubleConstant((double)value / (double)other);
        }
    }

    @Override
    public TypedCalcLambda divide(long other) {
        return new CalcLambdaDoubleConstant((double)value / (double)other);
    }

    @Override
    public TypedCalcLambda divide(double other) {
        return new CalcLambdaDoubleConstant( (double)value / other);
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaVariable other) {
        throw TypedCalcLambda.getNotImplemented();
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaInteger other) {
        return new CalcLambdaDouble(cvp -> (double)value / other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaLong other) {
        return new CalcLambdaDouble(cvp -> (double)value / other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaDouble other) {
        return new CalcLambdaDouble(cvp -> (double)value / other.evaluateDouble(cvp));
    }

    @Override
    public TypedCalcLambda pow(TypedCalcLambda o) {
        TypedCalcLambda other = o.toLambda();

        if (!(other instanceof Numeric)) {
            throw new GorParsingException("Number expected for ^ operator");
        }

        if (other instanceof CalcLambdaIntegerConstant || other instanceof CalcLambdaLongConstant) {
            value = (long) Math.pow(value, other.evaluateLong(null));
            return this;
        }

        if (other instanceof CalcLambdaDoubleConstant) {
            return new CalcLambdaDoubleConstant(Math.pow(value, other.evaluateDouble(null)));
        }

        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda negate() {
        value = -value;
        return this;
    }
}
