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

package org.gorpipe.gor.function;

import org.gorpipe.model.genome.files.gor.Row;
import gorsat.parser.ParseArith;
import scala.Function1;

import java.io.Serializable;
import java.util.function.Function;

public class GorRowMapFunction implements Function<Row,Row>, Serializable {
    transient ParseArith filter;
    public String calcType;
    Function1 func;

    public GorRowMapFunction(String query, String[] header, String[] gortypes) {
        filter = new ParseArith(null);
        filter.setColumnNamesAndTypes(header, gortypes);
        calcType = filter.compileCalculation(query);

        if (calcType.equals("String") ) func = filter.stringFunction();
        else if (calcType.equals("Double") ) func = filter.doubleFunction();
        else if( calcType.equals("Long") ) func = filter.longFunction();
        else if (calcType.equals("Int") ) func = filter.intFunction();
        else if( calcType.equals("Boolean") ) func = filter.booleanFunction();
    }

    @Override
    public Row apply(Row row) {
        return row.rowWithAddedColumn(func.apply(row).toString());
    }
}