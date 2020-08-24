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

package org.gorpipe.querydialogs;

import org.gorpipe.gor.model.RequiredColumn;
import org.gorpipe.querydialogs.argument.StringArgument;

import java.util.Collections;
import java.util.List;

/**
 * Argument for selecting grids or values from grids.
 *
 * @version $Id$
 */
public class GridArgument extends StringArgument {
    /**
     * Grid required columns.
     */
    public final List<RequiredColumn> requiredColumns;
    /**
     * Defines if only required columns may exist in grid.
     */
    public final Boolean requiredColumnsOnly;
    /**
     * Defines whether grid values or grid name should be used in selector.
     */
    public final Boolean extractValues;
    /**
     * Defines if only gor grids should be selected.
     */
    public final Boolean gorOnly;

    /**
     * Constructor.
     *
     * @param arg                 string argument
     * @param requiredColumns     grid required columns
     * @param requiredColumnsOnly <code>true</code> if only required columns may exist in grid, otherwise <code>false</code>
     * @param extractValues       <code>true</code> if values should be extracted from grid, <code>false</code> if grid name should be used
     * @param gorOnly             <code>true</code> if only gor grids should be selected, otherwise <code>false</code>
     */
    public GridArgument(final StringArgument arg, final List<RequiredColumn> requiredColumns, final Boolean requiredColumnsOnly,
                        final Boolean extractValues, final Boolean gorOnly) {
        super(arg);
        this.requiredColumns = Collections.unmodifiableList(requiredColumns);
        this.requiredColumnsOnly = requiredColumnsOnly != null && requiredColumnsOnly.booleanValue();
        this.extractValues = extractValues != null && extractValues.booleanValue();
        this.gorOnly = gorOnly != null && gorOnly.booleanValue();
    }

    /**
     * Copy constructor.
     *
     * @param arg the argument to copy
     */
    public GridArgument(final GridArgument arg) {
        this(arg, arg.requiredColumns, arg.requiredColumnsOnly, arg.extractValues, arg.gorOnly);
    }

    /**
     * Check if required columns are defined.
     *
     * @return <code>true</code> if required columns are defined, otherwise <code>false</code>
     */
    public boolean hasRequiredColumns() {
        return requiredColumns.size() > 0;
    }

    @Override
    public GridArgument copyArgument() {
        return new GridArgument(this);
    }
}
