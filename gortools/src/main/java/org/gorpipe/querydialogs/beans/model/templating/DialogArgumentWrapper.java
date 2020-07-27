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
package org.gorpipe.querydialogs.beans.model.templating;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.gorpipe.querydialogs.beans.model.Argument;
import org.gorpipe.querydialogs.beans.model.argument.StringArgument;

/**
 * A wrapper for dialog arguments.
 *
 * @author arnie
 * @version $Id$
 */
public class DialogArgumentWrapper extends DefaultObjectWrapper {
    @Override
    protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
        if (obj instanceof Argument) {
            Argument a = (Argument) obj;
            SimpleHash wrapped = new SimpleHash();
            String raw = a.toString().trim();
            if (!raw.isEmpty()) {
                wrapped.put("raw", raw);
            }
            String value = processValue(a);
            if (value != null && !value.isEmpty()) {
                wrapped.put("value", value);
                wrapped.put("val", value);
            }
            if (a.getOperator() != null) {
                wrapped.put("operator", a.getOperator());
                wrapped.put("op", a.getOperator());
            }
            return wrapped;
        }
        return super.handleUnknownType(obj);
    }

    private String processValue(Argument a) {
        if (a instanceof StringArgument) {
            return ((StringArgument) a).getFormattedValue();
        }
        return a.toString().trim();
    }
}