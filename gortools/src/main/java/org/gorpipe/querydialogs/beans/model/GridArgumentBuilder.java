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

package org.gorpipe.querydialogs.beans.model;

import org.gorpipe.model.genome.files.gor.FileReader;
import org.gorpipe.model.genome.files.gor.RequiredColumn;
import org.gorpipe.querydialogs.beans.model.argument.StringArgument;
import org.gorpipe.querydialogs.beans.model.factory.builder.StringArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builder for grid argument.
 *
 * @version $Id$
 */
public class GridArgumentBuilder extends StringArgumentBuilder {
    public GridArgumentBuilder(FileReader fileResolver) {
        super(fileResolver);
    }

    @Override
    public GridArgument build(final String name, final Map<String, ? extends Object> attributes) {
        final StringArgument build = super.build(name, attributes);
        final List<RequiredColumn> requiredColumns = new ArrayList<>();
        @SuppressWarnings("unchecked") final List<String> requiredColumnsNotation = (List<String>) attributes.get("required_columns");
        if (requiredColumnsNotation != null) {
            for (String notation : requiredColumnsNotation) {
                requiredColumns.add(new RequiredColumn(notation));
            }
        }
        final Boolean requiredColumnsOnly = (Boolean) attributes.get("required_columns_only");
        final Boolean extractValues = (Boolean) attributes.get("extract_values");
        final Boolean gorOnly = (Boolean) attributes.get("gor_only");
        return new GridArgument(build, requiredColumns, requiredColumnsOnly, extractValues, gorOnly);
    }
}
