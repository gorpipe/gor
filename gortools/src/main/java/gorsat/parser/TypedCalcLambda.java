/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package gorsat.parser;

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.ColumnValueProvider;

public interface TypedCalcLambda {
    static GorParsingException getIncompatibleTypes() {
        return new GorParsingException("Incompatible types");
    }

    static GorSystemException getNotImplemented() {
        return new GorSystemException("Not implemented", null);
    }

    default TypedCalcLambda toLambda() {
        return this;
    }

    String getType();

    int evaluateInt(ColumnValueProvider cvp);

    long evaluateLong(ColumnValueProvider cvp);

    double evaluateDouble(ColumnValueProvider cvp);

    String evaluateString(ColumnValueProvider cvp);

    boolean evaluateBoolean(ColumnValueProvider cvp);

    TypedCalcLambda addedTo(TypedCalcLambda left);

    TypedCalcLambda add(CalcLambdaVariable other);

    TypedCalcLambda add(CalcLambdaString other);

    TypedCalcLambda add(String other);

    TypedCalcLambda add(CalcLambdaInteger other);

    TypedCalcLambda add(int other);

    TypedCalcLambda add(CalcLambdaLong other);

    TypedCalcLambda add(long other);

    TypedCalcLambda add(CalcLambdaDouble other);

    TypedCalcLambda add(double other);

    TypedCalcLambda subtractedFrom(TypedCalcLambda other);

    TypedCalcLambda subtract(int other);

    TypedCalcLambda subtract(long other);

    TypedCalcLambda subtract(double other);

    TypedCalcLambda subtract(CalcLambdaVariable other);

    TypedCalcLambda subtract(CalcLambdaInteger other);

    TypedCalcLambda subtract(CalcLambdaLong other);

    TypedCalcLambda subtract(CalcLambdaDouble other);

    TypedCalcLambda multipliedWith(TypedCalcLambda left);

    TypedCalcLambda multiply(int other);

    TypedCalcLambda multiply(long other);

    TypedCalcLambda multiply(double other);

    TypedCalcLambda multiply(CalcLambdaVariable other);

    TypedCalcLambda multiply(CalcLambdaInteger other);

    TypedCalcLambda multiply(CalcLambdaLong other);

    TypedCalcLambda multiply(CalcLambdaDouble other);

    TypedCalcLambda dividedInto(TypedCalcLambda left);

    TypedCalcLambda divide(int other);

    TypedCalcLambda divide(long other);

    TypedCalcLambda divide(double other);

    TypedCalcLambda divide(CalcLambdaVariable other);

    TypedCalcLambda divide(CalcLambdaInteger other);

    TypedCalcLambda divide(CalcLambdaLong other);

    TypedCalcLambda divide(CalcLambdaDouble other);

    TypedCalcLambda pow(TypedCalcLambda other);

    TypedCalcLambda negate();

    TypedCalcLambda compare(TypedCalcLambda other, int op);
}
