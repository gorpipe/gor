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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.model.ColumnValueProvider;

public class HeaderCVP implements ColumnValueProvider {
    public static final int COLNAME = 0;
    public static final int COLNUM = 1;

    private final String[] columns;
    private int currentColumn;

    public HeaderCVP(String header) {
        columns = header.split("\t");
    }

    public void setCurrentColumn(int col) {
        currentColumn = col;
    }

    @Override
    public String stringValue(int col) {
        switch (col) {
            case COLNAME:
                return columns[currentColumn-1];
            case COLNUM:
                return String.valueOf(currentColumn);
            default:
                throw new GorDataException("Invalid column");
        }
    }

    @Override
    public int intValue(int col) {
        if (col == COLNUM) {
            return currentColumn;
        } else {
            throw new GorDataException("Not a numeric column");
        }
    }

    @Override
    public long longValue(int col) {
        return intValue(col);
    }

    @Override
    public double doubleValue(int col) {
        return intValue(col);
    }
}
