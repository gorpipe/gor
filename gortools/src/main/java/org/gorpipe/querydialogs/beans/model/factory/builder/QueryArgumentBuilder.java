/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2020 WuXi NextCode Inc.
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
package org.gorpipe.querydialogs.beans.model.factory.builder;

import org.gorpipe.model.genome.files.gor.FileReader;
import org.gorpipe.model.genome.files.gor.QueryEvaluator;
import org.gorpipe.querydialogs.beans.model.ArgumentDescription;
import org.gorpipe.querydialogs.beans.model.argument.QueryArgument;
import org.gorpipe.querydialogs.beans.model.factory.ArgumentBuilder;

import java.util.List;
import java.util.Map;

/**
 * Responsible for building GOR/NOR query arguments.
 */
public class QueryArgumentBuilder extends ArgumentBuilder {
    private final QueryEvaluator queryEvaluator;

    public QueryArgumentBuilder(FileReader fileResolver, QueryEvaluator queryEval) {
        super(fileResolver);
        queryEvaluator = queryEval;
    }

    @SuppressWarnings("unchecked")
    @Override
    public QueryArgument build(String name, Map<String, ?> attributes) {
        final ArgumentDescription argDescr = getArgumentDescription(attributes, name);
        final Boolean optional = (Boolean) attributes.get("optional");
        final String defaultValue = safeString(attributes.get("default"));
        final String query = safeString(attributes.get("query"));
        final List<String> operators = (List<String>) attributes.get("operators");
        final Boolean advanced = (Boolean) attributes.get("advanced");
        final Integer displayWidth = getDisplayWidth(attributes);

        return new QueryArgument(argDescr, optional, defaultValue, query, operators, advanced, displayWidth, queryEvaluator);
    }
}
