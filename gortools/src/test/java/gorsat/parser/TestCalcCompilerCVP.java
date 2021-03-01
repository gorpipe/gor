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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.model.ColumnValueProvider;

class TestCalcCompilerCVP implements ColumnValueProvider {
    private final ColumnValue[] columns;

    TestCalcCompilerCVP(ColumnValue[] columns) {
        this.columns = columns;
    }

    @Override
    public String stringValue(int col) {
        ColumnValue column = columns[col];
        if (column.getType().equals("S")) {
            return (String)column.getValue();
        }
        if (column.getType().equals("I")) {
            return String.valueOf((int)column.getValue());
        }
        if (column.getType().equals("L")) {
            return String.valueOf((long)column.getValue());
        }
        if (column.getType().equals("D")) {
            return String.valueOf((double)column.getValue());
        }
        return null;
    }

    @Override
    public int intValue(int col) {
        ColumnValue column = columns[col];
        if (column.getValue() instanceof Integer) {
            return (Integer)column.getValue();
        }
        throw new GorDataException("Wrong type");
    }

    @Override
    public long longValue(int col) {
        ColumnValue column = columns[col];
        if (column.getValue() instanceof Long) {
            return (Long)column.getValue();
        }
        if (column.getValue() instanceof Integer) {
            return (Integer)column.getValue();
        }
        throw new GorDataException("Wrong type");
    }

    @Override
    public double doubleValue(int col) {
        ColumnValue column = columns[col];
        if (column.getValue() instanceof Double) {
            return (Double)column.getValue();
        }
        if (column.getValue() instanceof Long) {
            return (Long)column.getValue();
        }
        if (column.getValue() instanceof Integer) {
            return (Integer)column.getValue();
        }

        throw new GorDataException("Wrong type");
    }
}
