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

package org.gorpipe.gor.function;

import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.model.RowBase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * For map-reduce type inferance, such as in SparkSQL which needs a schema
 * The gor type syntax, I for Int, D for Double and S for String
 * Two rows with types I and S are reduced to type S etc.
 **/
public class GorRowInferFunction implements BinaryOperator<Row>, Serializable {
    static Map<String,Character> convMap = new HashMap<>();

    static {
        convMap.put("IS",'S');
        convMap.put("SI",'S');
        convMap.put("DS",'S');
        convMap.put("SD",'S');
        convMap.put("ID",'D');
        convMap.put("DI",'D');
    }

    void checkNumericTypes(RowBase row, Row t1, int i) {
        String colval = row.stringValue(i);
        if (colval.equals("I")) {
            try {
                t1.intValue(i);
            } catch (Exception e) {
                colval = "D";
                ((StringBuilder) row.getAllCols()).setCharAt(i * 2, 'D');
            }
        }
        if (colval.equals("D")) {
            try {
                t1.doubleValue(i);
            } catch (Exception e) {
                ((StringBuilder) row.getAllCols()).setCharAt(i * 2, 'S');
            }
        }
    }

    void inferOther(RowBase row, Row t1) {
        for( int i = 0; i < row.numCols(); i++ ) {
            if( t1.colAsString(i).length() > 16 ) {
                ((StringBuilder)row.getAllCols()).setCharAt(i*2, 'S');
            } else {
                checkNumericTypes(row, t1, i);
            }
        }
    }

    Row inferBoth(Row row, Row t1) {
        int[] sa = new int[row.numCols()];
        StringBuilder sb = new StringBuilder();
        sb.append("I");
        int i = 1;
        for( ; i < row.numCols(); i++ ) {
            sb.append("\tI");
            sa[i-1] = i*2-1;
        }
        sa[i-1] = i*2-1;
        RowBase newrow = new RowBase(null, -1, sb, sa, null);
        inferOther(newrow, row);
        inferOther(newrow, t1);
        return newrow;
    }

    public Row infer(Row row, Row t1) {
        if( row.chr == null ) {
            if( t1.chr == null ) {
                for( int i = 0; i < row.numCols(); i++ ) {
                    String duo = row.stringValue(i)+t1.stringValue(i);
                    if( convMap.containsKey(duo) ) {
                        ((StringBuilder) row.getAllCols()).setCharAt(i*2, convMap.get(duo));
                    }
                }
            } else inferOther((RowBase)row, t1);
            return row;
        } else {
            if( t1.chr == null ) {
                inferOther((RowBase)t1,row);
                return t1;
            } else {
                return inferBoth(row, t1);
            }
        }
    }

    @Override
    public Row apply(Row row, Row row2) {
        return infer(row,row2);
    }
}

