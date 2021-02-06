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
package org.gorpipe.querydialogs.templating;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModelException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Template model that enables template writer to skip first occurence of a given argument.
 * <p>Example:
 * <pre>
 * {@literal &lt;#if condition>}
 * {@literal $skip('AND') something}
 * {@literal &lt;/#if>}
 * {@literal $skip('AND') something more}
 * </pre>
 * <p>The first time the method is called, nothing is output, the next call will output 'AND' (without quotes).
 * In this case that means that 'AND' will only be output if the condition is true, resulting in either:
 * "something AND something more" or just "something more".
 * <p>Calls to skip can optionally be scoped to provide more control.
 * Example:
 * <pre>
 * {@literal <#if condition>}
 * {@literal $skip('AND', 'scope1') something}
 * {@literal </#if>}
 * {@literal $skip('AND', 'scope1') something more}
 * OR
 * {@literal <#if another condition>}
 * {@literal $skip('AND', 'scope2') something else}
 * {@literal </#if>}
 * {@literal $skip('AND', 'scope2') more something else}
 * </pre>
 * <p>This effectively separates concerns for the first two calls and the last two calls, resulting in the possible outcomes:
 * <ul>
 * <li>something AND something more OR something else AND more something else
 * <li>something more OR something else AND more something else
 * <li>something AND something more OR more something else
 * <li>something more OR more something else
 * </ul>
 *
 * @author arnie
 * @version $Id$
 */
@SuppressWarnings("javadoc")
public class SkipFirstMethodModel implements TemplateMethodModel {
    private final Set<String> skipped = new HashSet<String>();

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        if (arguments.size() < 1 || arguments.size() > 2)
            throw new TemplateModelException("skip_first error, usage: skip(value[, scope])");
        Object value = arguments.get(0);
        String key = arguments.size() == 2 ? arguments.get(1).toString() : "[default_scope]";
        if (skipped.contains(key)) return value;
        skipped.add(key);
        return "";
    }

    public void reset() {
        skipped.clear();
    }
}