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

package org.gorpipe.querydialogs.factory;

/**
 * Holds a definition of columns that should be grouped together.
 * Columns can either be listed with their full name or defined by their common prefix (prefix mode)
 *
 * @version $Id$
 */
public class ColumnGroup {

    /**
     * Name of this group. To be used in meta-data lookup
     */
    public final String name;
    /**
     * The name to use in graphical representations of the group
     */
    public final String displayName;
    /**
     * Names (and order) of the columns in this group. This should either be a comma-separated string or a single name ending with a '*' (prefix mode)
     */
    public final String columnNames;
    /**
     * True if the group is predefined and should precede other groups in the view
     */
    public final boolean isPredefined;
    /**
     * For groups that are predefined, this is the ranking between them. The group with the highest number will be rendered first
     */
    public final int order;


    /**
     * @param displayName The name to use in graphical representations of the group
     */
    public ColumnGroup(String displayName) {
        this(displayName, displayName, "", false, 0);
    }

    /**
     * @param name        Name of this group. To be used in meta-data lookup
     * @param displayName The name to use in graphical representations of the group
     * @param columnNames Names (and order) of the columns in this group. This should either be a comma-separated string or a single name ending with a '*' (prefix mode)
     */
    public ColumnGroup(String name, String displayName, String columnNames) {
        this(name, displayName, columnNames, false, 0);
    }

    /**
     * @param name         Name of this group. To be used in meta-data lookup
     * @param displayName  The name to use in graphical representations of the group
     * @param columnNames  Names (and order) of the columns in this group. This should either be a comma-separated string or a single name ending with a '*' (prefix mode)
     * @param isPredefined True if the group is predefined and should precede other groups in the view
     * @param order        For groups that are predefined, this is the ranking between them. The group with the highest number will be rendered first
     */
    public ColumnGroup(String name, String displayName, String columnNames, boolean isPredefined, int order) {
        this.name = name;
        this.displayName = displayName;
        this.columnNames = columnNames;
        this.isPredefined = isPredefined;
        this.order = order;
    }

    /**
     * @param column
     * @return True if the provided column name exists in the column list for this group. Note! name comparison is case-insensitive.
     */
    public boolean contains(String column) {
        return columnNames.toLowerCase().contains(column.toLowerCase());
    }

    @Override
    public String toString() {
        return displayName + ": " + columnNames;
    }

    @Override
    public int hashCode() {
        return displayName.hashCode();
    }

}
