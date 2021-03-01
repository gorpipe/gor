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

package org.gorpipe.querydialogs.templating;

import freemarker.template.*;
import org.gorpipe.gor.model.QueryEvaluator;

import java.util.List;

public class QueryEvalMethodModel implements TemplateMethodModel {

    private final QueryEvaluator queryEval;

    public QueryEvalMethodModel(QueryEvaluator queryEval) {
        this.queryEval = queryEval;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {

        if (queryEval == null) {
            throw new TemplateModelException("gor method error, query evaluator instance is not available");
        }

        if (arguments.size() < 1 || arguments.size() > 2) {
            throw new TemplateModelException("gor method error, usage: gor(query[, return type])");
        }

        String query = arguments.get(0).toString();
        String returnType = "string";

        if (arguments.size() == 2) {
            returnType = arguments.get(1).toString();
        }

        switch (returnType.toLowerCase()) {
            case "string": return queryEval.asValue(query);
            case "number": return new SimpleNumber(Double.parseDouble(queryEval.asValue(query)));
            case "list": return new SimpleSequence(queryEval.asList(query));
            case "iterator": return new SimpleCollection(queryEval.asList(query));
            default: throw new TemplateModelException("gor method error, supported return types are string, number, list or iterator");
        }
    }
}
