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
package org.gorpipe.querydialogs.factory.builder;

import org.gorpipe.gor.model.FileReader;
import org.gorpipe.querydialogs.ArgumentDescription;
import org.gorpipe.querydialogs.argument.DateArgument;
import org.gorpipe.querydialogs.factory.ArgumentBuilder;

import java.util.List;
import java.util.Map;

/**
 * Responsible for building date arguments.
 *
 * @author arnie
 * @version $Id$
 */
public class DateArgumentBuilder extends ArgumentBuilder {
    public DateArgumentBuilder(FileReader fileResolver) {
        super(fileResolver);
    }

    /* (non-Javadoc)
     * @see com.decode.sdl.beans.dialogs.extracted.factory.ArgumentBuilder#build(java.lang.String, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public DateArgument build(String name, Map<String, ? extends Object> attributes) {
        final ArgumentDescription argDescr = getArgumentDescription(attributes, name);
        Boolean optional = (Boolean) attributes.get("optional");
        String defaultValue = safeString(attributes.get("default"));
        List<? extends Object> allowed = getAllowedValues(attributes);
        List<String> operators = (List<String>) attributes.get("operators");
        Boolean advanced = (Boolean) attributes.get("advanced");
        final Integer displayWidth = getDisplayWidth(attributes);
        return new DateArgument(argDescr, optional, defaultValue, allowed, operators, advanced, displayWidth);
    }
}
