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

package org.gorpipe.querydialogs.factory.builder;

import org.gorpipe.gor.model.FileReader;
import org.gorpipe.querydialogs.ArgumentDescription;
import org.gorpipe.querydialogs.argument.FileArgument;
import org.gorpipe.querydialogs.factory.ArgumentBuilder;

import java.util.Map;

/**
 * Responsible for building file arguments.
 */
public class FileArgumentBuilder extends ArgumentBuilder {
    public FileArgumentBuilder(FileReader fileResolver) {
        super(fileResolver);
    }

    @Override
    public FileArgument build(final String name, final Map<String, ? extends Object> attributes) {
        final ArgumentDescription argDescr = getArgumentDescription(attributes, name);
        final Boolean optional = (Boolean) attributes.get("optional");
        final String defaultValue = safeString(attributes.get("default"));
        final Boolean advanced = (Boolean) attributes.get("advanced");
        final Integer displayWidth = getDisplayWidth(attributes);
        final String fileFilterSuffixes = safeString(attributes.get("file_filters"));
        return new FileArgument(argDescr, optional, defaultValue, advanced, displayWidth, fileFilterSuffixes);
    }
}
