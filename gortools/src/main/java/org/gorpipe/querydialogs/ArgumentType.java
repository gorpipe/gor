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

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of argument types.
 * <p>
 * <p>Note: not using enum because it needs to be extendable.</p>
 *
 * @author arnie
 * @version $Id$
 */
@SuppressWarnings("javadoc")
public class ArgumentType {

    private static final Map<String, ArgumentType> TYPE_MAP = new HashMap<>();
    public final Integer defaultDisplayWidth;
    private final int ordinal;
    private final String name;

    protected ArgumentType(final int ordinal, final String name, final Integer defaultDisplayWidth) {
        this.ordinal = ordinal;
        this.name = name;
        this.defaultDisplayWidth = defaultDisplayWidth;
        if (TYPE_MAP.containsKey(name)) {
            throw new RuntimeException("Internal error: redefinition of ArgumentType " + name);
        }
        TYPE_MAP.put(name, this);
    }

    protected ArgumentType(int ordinal, String name) {
        this(ordinal, name, null);
    }

    public static ArgumentType valueOf(String name) {
        if (name == null) {
            throw new NullPointerException("");
        }
        ArgumentType type = TYPE_MAP.get(name);
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("No ArgumentType " + name);
    }

    public int ordinal() {
        return ordinal;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final int DATE_ORDINAL = 0;
    public static final ArgumentType DATE = new ArgumentType(DATE_ORDINAL, "DATE");
    public static final int NUMBER_ORDINAL = 1;
    public static final ArgumentType NUMBER = new ArgumentType(NUMBER_ORDINAL, "NUMBER", 150);
    public static final int STRING_ORDINAL = 2;
    public static final ArgumentType STRING = new ArgumentType(STRING_ORDINAL, "STRING");
    public static final int FILE_ORDINAL = 3;
    public static final ArgumentType FILE = new ArgumentType(FILE_ORDINAL, "FILE");
    public static final int POSITION_RANGE_ORDINAL = 4;
    public static final ArgumentType POSITION_RANGE = new ArgumentType(POSITION_RANGE_ORDINAL, "POSITION_RANGE", 200);
    public static final int PN_LISTS_ORDINAL = 5;
    public static final ArgumentType PN_LISTS = new ArgumentType(PN_LISTS_ORDINAL, "PN_LISTS");
    public static final int FILTERED_PN_LISTS_ORDINAL = 6;
    public static final ArgumentType FILTERED_PN_LISTS = new ArgumentType(FILTERED_PN_LISTS_ORDINAL, "FILTERED_PN_LISTS");
    public static final int SLIDER_ORDINAL = 7;
    public static final ArgumentType SLIDER = new ArgumentType(SLIDER_ORDINAL, "SLIDER");
    public static final int VIRTUAL_PN_FILE_ORDINAL = 8;
    public static final ArgumentType VIRTUAL_PN_FILE = new ArgumentType(VIRTUAL_PN_FILE_ORDINAL, "VIRTUAL_PN_FILE");
    /**
     * For selection from a grid.
     */
    public static final int GRID_ORDINAL = 9;
    public static final ArgumentType GRID = new ArgumentType(GRID_ORDINAL, "GRID", 250);
    /**
     * For selection from a gor grid (based on class GorViewer).
     */
    public static final int GOR_GRID_ORDINAL = 10;
    public static final ArgumentType GOR_GRID = new ArgumentType(GOR_GRID_ORDINAL, "GOR_GRID", 250);
    /**
     * For selection from a value grid (based on class GridTableView).
     */
    public static final int VALUE_GRID_ORDINAL = 11;
    public static final ArgumentType VALUE_GRID = new ArgumentType(VALUE_GRID_ORDINAL, "VALUE_GRID", 250);
    public static final int PN_LISTS_ENTRIES_ORDINAL = 12;
    public static final ArgumentType PN_LISTS_ENTRIES = new ArgumentType(PN_LISTS_ENTRIES_ORDINAL, "PN_LISTS_ENTRIES");
    public static final int GENE_LIST_ORDINAL = 13;
    public static final ArgumentType GENE_LIST = new ArgumentType(GENE_LIST_ORDINAL, "GENE_LIST", 250);
    public static final int CHECK_ITEMS_ORDINAL = 14;
    public static final ArgumentType CHECK_ITEMS = new ArgumentType(CHECK_ITEMS_ORDINAL, "CHECK_ITEMS", 250);

    /**
     * For creating a selection dialog from a nor/gor query
     */
    public static final int QUERY_ORDINAL = 15;
    public static final ArgumentType QUERY = new ArgumentType(QUERY_ORDINAL, "QUERY", 250);
}
