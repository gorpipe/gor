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

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.ColumnValueProvider;

public class CalcLambdaVariable implements TypedCalcLambda {
    private final int columnIndex;
    private final String columnType;

    public CalcLambdaVariable(int columnIndex, String columnType) {
        this.columnIndex = columnIndex;
        this.columnType = columnType;
    }

    @Override
    public TypedCalcLambda toLambda() {
        TypedCalcLambda result;
        switch (columnType) {
            case "S":
                result = new CalcLambdaString((ColumnValueProvider cvp) -> cvp.stringValue(columnIndex));
                break;
            case "D":
                result = new CalcLambdaDouble((ColumnValueProvider cvp) -> cvp.doubleValue(columnIndex));
                break;
            case "L":
                result = new CalcLambdaLong((ColumnValueProvider cvp) -> cvp.longValue(columnIndex));
                break;
            case "I":
                result = new CalcLambdaInteger((ColumnValueProvider cvp) -> cvp.intValue(columnIndex));
                break;
            default:
                throw new GorSystemException("Unknown type", null);
        }

        return result;
    }

    @Override
    public String getType() {
        return columnType;
    }

    @Override
    public int evaluateInt(ColumnValueProvider cvp) {
        return cvp.intValue(columnIndex);
    }

    @Override
    public long evaluateLong(ColumnValueProvider cvp) {
        return cvp.longValue(columnIndex);
    }

    @Override
    public double evaluateDouble(ColumnValueProvider cvp) {
        return cvp.doubleValue(columnIndex);
    }

    @Override
    public String evaluateString(ColumnValueProvider cvp) {
        return cvp.stringValue(columnIndex);
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
    public TypedCalcLambda add(String other) {
        return new CalcLambdaString(cvp -> cvp.stringValue(columnIndex) + other);
    }

    @Override
    public TypedCalcLambda add(CalcLambdaInteger other) {
        return toLambda().add(other);
    }

    @Override
    public TypedCalcLambda add(int other) {
        if (columnType.equals("I")) {
            return new CalcLambdaInteger(cvp -> cvp.intValue(columnIndex) + other);
        }
        if (columnType.equals("L")) {
            return new CalcLambdaLong(cvp -> cvp.longValue(columnIndex) + other);
        }
        if (columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) + other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda add(CalcLambdaLong other) {
        return toLambda().add(other);
    }

    @Override
    public TypedCalcLambda add(long other) {
        if (columnType.equals("I") || columnType.equals("L")) {
            return new CalcLambdaLong(cvp -> cvp.longValue(columnIndex) + other);
        }
        if (columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) + other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda add(CalcLambdaDouble other) {
        return toLambda().add(other);
    }

    @Override
    public TypedCalcLambda add(double other) {
        if (columnType.equals("I") || columnType.equals("L") || columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) + other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda add(CalcLambdaVariable other) {
        // This is questionable behavior from the old parser
        if (columnType.equals("S") || other.getType().equals("S")) {
            return new CalcLambdaString(cvp -> cvp.stringValue(columnIndex) + other.evaluateString(cvp));
        } else {
            return toLambda().add(other);
        }
    }

    @Override
    public TypedCalcLambda add(CalcLambdaString other) {
        return toLambda().add(other);
    }

    @Override
    public TypedCalcLambda subtractedFrom(TypedCalcLambda left) {
        return left.subtract(this);
    }

    @Override
    public TypedCalcLambda subtract(int other) {
        if (columnType.equals("I")) {
            return new CalcLambdaInteger(cvp -> cvp.intValue(columnIndex) - other);
        }
        if (columnType.equals("L")) {
            return new CalcLambdaLong(cvp -> cvp.longValue(columnIndex) - other);
        }
        if (columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) - other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda subtract(long other) {
        if (columnType.equals("I") || columnType.equals("L")) {
            return new CalcLambdaLong(cvp -> cvp.longValue(columnIndex) - other);
        }
        if (columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) - other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda subtract(double other) {
        if (columnType.equals("I") || columnType.equals("L") || columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) - other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaLong other) {
        return toLambda().subtract(other);
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaDouble other) {
        return toLambda().subtract(other);
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaVariable other) {
        return toLambda().subtract(other);
    }

    @Override
    public TypedCalcLambda subtract(CalcLambdaInteger other) {
        return toLambda().subtract(other);
    }

    @Override
    public TypedCalcLambda multipliedWith(TypedCalcLambda left) {
        return toLambda().multipliedWith(left);
    }

    @Override
    public TypedCalcLambda multiply(int other) {
        if (columnType.equals("I")) {
            return new CalcLambdaInteger(cvp -> cvp.intValue(columnIndex) * other);
        }
        if (columnType.equals("L")) {
            return new CalcLambdaLong(cvp -> cvp.longValue(columnIndex) * other);
        }
        if (columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) * other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda multiply(long other) {
        if (columnType.equals("I") || columnType.equals("L")) {
            return new CalcLambdaLong(cvp -> cvp.longValue(columnIndex) * other);
        }
        if (columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) * other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda multiply(double other) {
        if (columnType.equals("I") || columnType.equals("L") || columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) * other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaVariable other) {
        return toLambda().multiply(other);
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaInteger other) {
        return toLambda().multiply(other);
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaLong other) {
        return toLambda().multiply(other);
    }

    @Override
    public TypedCalcLambda multiply(CalcLambdaDouble other) {
        return toLambda().multiply(other);
    }

    @Override
    public TypedCalcLambda dividedInto(TypedCalcLambda left) {
        return toLambda().dividedInto(left);
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaVariable other) {
        return other.toLambda().dividedInto(this);
    }

    @Override
    public TypedCalcLambda divide(int other) {
        if (columnType.equals("I")) {
            return new CalcLambdaInteger(cvp -> cvp.intValue(columnIndex) / other);
        }
        if (columnType.equals("L")) {
            return new CalcLambdaLong(cvp -> cvp.longValue(columnIndex) / other);
        }
        if (columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) / other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda divide(long other) {
        if (columnType.equals("I") || columnType.equals("L")) {
            return new CalcLambdaLong(cvp -> cvp.longValue(columnIndex) / other);
        }
        if (columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) / other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda divide(double other) {
        if (columnType.equals("I") || columnType.equals("L") || columnType.equals("D")) {
            return new CalcLambdaDouble(cvp -> cvp.doubleValue(columnIndex) / other);
        }
        throw TypedCalcLambda.getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaInteger other) {
        return toLambda().divide(other);
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaLong other) {
        return toLambda().divide(other);
    }

    @Override
    public TypedCalcLambda divide(CalcLambdaDouble other) {
        return toLambda().divide(other);
    }

    @Override
    public TypedCalcLambda pow(TypedCalcLambda other) {
        return toLambda().pow(other);
    }

    @Override
    public TypedCalcLambda negate() {
        return toLambda().negate();
    }

    @Override
    public TypedCalcLambda compare(TypedCalcLambda other, int op) {
        if (other instanceof StringType) {
            return new CalcLambdaString(cvp -> cvp.stringValue(columnIndex)).compare(other, op);
        }
        return toLambda().compare(other, op);
    }
}
