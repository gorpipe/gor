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
import org.gorpipe.querydialogs.ArgumentType;
import org.gorpipe.querydialogs.argument.NumberArgument;
import org.gorpipe.querydialogs.factory.ArgumentBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Responsible for building number arguments.
 *
 * @author arnie
 * @version $Id$
 */
public class NumberArgumentBuilder extends ArgumentBuilder {
    public NumberArgumentBuilder(FileReader fileResolver) {
        super(fileResolver);
    }

    /* (non-Javadoc)
     * @see com.decode.sdl.beans.dialogs.extracted.factory.ArgumentBuilder#build(java.lang.String, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public NumberArgument build(String name, Map<String, ? extends Object> attributes) {
        Number min = null, max = null;
        if (attributes.containsKey("range")) {
            String[] range = attributes.get("range").toString().split("\\.\\.");
            min = Double.parseDouble(range[0]);
            max = Double.parseDouble(range[1]);
        } else {
            if (attributes.containsKey("min")) {
                min = (Number) attributes.get("min");
            }
            if (attributes.containsKey("max")) {
                max = (Number) attributes.get("max");
            }
        }
        final ArgumentDescription argDescr = getArgumentDescription(attributes, name);
        Boolean optional = (Boolean) attributes.get("optional");
        String defaultValue = safeString(attributes.get("default"));
        List<? extends Object> allowed = getAllowedValues(attributes);
        URI valuesPath = getValuesPath(attributes);
        List<String> operators = (List<String>) attributes.get("operators");
        Boolean advanced = (Boolean) attributes.get("advanced");
        final Integer displayWidth = getDisplayWidth(attributes);
        ArgumentType argType = attributes.containsKey("type") ? ArgumentType.valueOf(attributes.get("type").toString().trim().toUpperCase()) : ArgumentType.NUMBER;

        return new NumberArgument(argDescr, optional, defaultValue, allowed, valuesPath, operators, advanced, displayWidth, min, max, argType);
    }
}
