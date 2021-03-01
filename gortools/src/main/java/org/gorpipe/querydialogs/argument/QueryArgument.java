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
package org.gorpipe.querydialogs.argument;

import org.gorpipe.gor.model.QueryEvaluator;
import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.ArgumentDescription;
import org.gorpipe.querydialogs.ArgumentType;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents an argument that uses a gor/nor query to create the values the user can select from.
 */
@SuppressWarnings({"serial"})
public class QueryArgument extends Argument {
    private final QueryEvaluator queryEvaluator;
    private final String query;

    public QueryArgument(ArgumentDescription argDescr, Boolean optional, Object defaultValue, String query, List<String> operators, Boolean advanced, Integer displayWidth, QueryEvaluator queryEvaluator) {
        super(ArgumentType.QUERY, argDescr, optional, defaultValue, DEFERRED_LIST, null, operators, advanced, displayWidth);
        this.queryEvaluator = queryEvaluator;
        this.query = query;
    }

    public QueryArgument(final QueryArgument arg) {
        super(arg);
        this.queryEvaluator = arg.queryEvaluator;
        this.query = arg.query;
    }

    @Override
    public QueryArgument copyArgument() {
        return new QueryArgument(this);
    }

    @Override
    public void loadDeferredValues(final boolean checkForHeader, Function<String, BufferedReader> fileResolver) throws IOException {
        List<Object> values = new ArrayList<>();
        List<String> queryResults = queryEvaluator.asList(query);
        for (String line : queryResults) {
            values.add(line.split("\t"));
        }
        allowedValues = values;
        fireContentsChanged(0, allowedValues.size());
    }

    @Override
    public boolean hasDeferredValues() {
        return true;
    }

    @Override
    protected void checkAllowed(Object theValue) {
        // Garpur 2020-04-30 intentionally left empty since the user can only select values from a pre-filled list.
    }
}
