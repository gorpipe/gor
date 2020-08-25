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

import gorsat.parser.ParseArith;
import org.gorpipe.gor.model.ColumnValueProvider;
import org.gorpipe.gor.model.Row;
import scala.Boolean;
import scala.Function1;

import java.io.Serializable;
import java.util.function.Predicate;

public class GorRowFilterFunction<T extends Row> implements Predicate<T>, Function1<T, Boolean>, Serializable {
    Function1<ColumnValueProvider,Object> booleanFunction;

    public GorRowFilterFunction(String gorwhere, String[] header, String[] gortypes) {
        ParseArith filter = new ParseArith(null);
        filter.setColumnNamesAndTypes(header, gortypes);
        filter.compileFilter(gorwhere);
        booleanFunction = filter.getBooleanFunction();
    }

    @Override
    public scala.Boolean apply(T row) {
        return (scala.Boolean) booleanFunction.apply(row);
    }

    @Override
    public boolean test(T row) {
        return (boolean) booleanFunction.apply(row);
    }
}