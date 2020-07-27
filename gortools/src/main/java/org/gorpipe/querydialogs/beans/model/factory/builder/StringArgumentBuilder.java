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
package org.gorpipe.querydialogs.beans.model.factory.builder;

import org.gorpipe.model.genome.files.gor.FileReader;
import org.gorpipe.querydialogs.beans.model.ArgumentDescription;
import org.gorpipe.querydialogs.beans.model.ArgumentType;
import org.gorpipe.querydialogs.beans.model.argument.StringArgument;
import org.gorpipe.querydialogs.beans.model.factory.ArgumentBuilder;
import org.gorpipe.querydialogs.beans.util.ValueFormatter;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Responsible for building string arguments.
 *
 * @author arnie
 * @version $Id$
 */
public class StringArgumentBuilder extends ArgumentBuilder {
    public StringArgumentBuilder(FileReader fileResolver) {
        super(fileResolver);
    }

    /* (non-Javadoc)
     * @see com.decode.sdl.beans.dialogs.extracted.factory.ArgumentBuilder#build(java.lang.String, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public StringArgument build(String name, Map<String, ? extends Object> attributes) {
        final ArgumentDescription argDescr = getArgumentDescription(attributes, name);
        final Boolean optional = (Boolean) attributes.get("optional");
        final String defaultValue = safeString(attributes.get("default"));
        final List<? extends Object> allowed = getAllowedValues(attributes);
        final URI valuesPath = getValuesPath(attributes);
        final List<String> operators = (List<String>) attributes.get("operators");

        ValueFormatter formatter = null;
        if (attributes.containsKey("format")) {
            formatter = new ValueFormatter((Map<Object, String>) attributes.get("format"));
        }

        final boolean quoted = !attributes.containsKey("quoted") || attributes.get("quoted") == Boolean.TRUE;
        final Boolean advanced = (Boolean) attributes.get("advanced");
        final Integer displayWidth = getDisplayWidth(attributes);
        final ArgumentType argType = attributes.containsKey("type") ? ArgumentType.valueOf(attributes.get("type").toString().trim().toUpperCase()) : ArgumentType.STRING;
        final Boolean singleSelection = (Boolean) attributes.get("single_selection");

        return new StringArgument(argDescr, quoted, optional, defaultValue, allowed, valuesPath, formatter,
                operators, advanced, displayWidth, argType, singleSelection);
    }

}
