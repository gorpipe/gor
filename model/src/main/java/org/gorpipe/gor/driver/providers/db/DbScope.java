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

package org.gorpipe.gor.driver.providers.db;

import com.google.common.base.Strings;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class DbScope {
    private final String column;
    private final Object value;

    private static final String PREFIX = "dbscope=";
    private static final String SUFFIX = "|||";
    private static final String DELIMITER = ",";

    protected DbScope(String column, Object value) {
        this.column = column;
        this.value = value;
    }

    public static List<DbScope> parse(String text) {
        List<DbScope> dbScopes = new ArrayList<>();
        String parsedtext = StringUtil.substring(text, PREFIX, SUFFIX);

        if (!Strings.isNullOrEmpty(parsedtext)) {
            String[] textArray = parsedtext.split(DELIMITER);
            for (String dbScopeString : textArray) {
                try {
                    DbScope dbScope = parseSingle(dbScopeString);
                    if (!dbScopes.contains(dbScope)) {
                        dbScopes.add(dbScope);
                    }
                } catch (GorSystemException e) {
                    throw new GorSystemException("Error parsing text to db scope: " + text, e);
                }

            }
        }
        return dbScopes;
    }

    private static DbScope parseSingle(String text) {
        if (text == null) {
            return null;
        }

        String column;
        Object value;
        final String[] values = text.split("#");
        if (values.length != 3) {
            throw new GorSystemException("Expected " + text + " to be on the form column#type#value", null);
        }
        column = values[0];
        switch (values[1].toLowerCase()) {
            case "int":
                value = Integer.parseInt(values[2]);
                break;
            case "string":
                value = values[2];
                break;
            default:
                throw new GorSystemException("Unexpected value type " + values[1] + " found in context " + text, null);
        }
        return new DbScope(column, value);
    }

    public String getColumn() {
        return column;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "scopingCol:" + getColumn() + " scopingVal: " + getValue();
    }

    @Override
    public boolean equals(Object object) {
        boolean same = false;
        if (object instanceof DbScope) {
            DbScope dbscope = (DbScope) object;
            same = (this.column.equals(dbscope.column)) && (this.value.equals(dbscope.value));
        }

        return same;
    }

    public static String dbScopesToString(List<DbScope> dbScopes) {
        StringBuilder result = new StringBuilder();
        for (DbScope dbScope : dbScopes) {
            result.append(" ").append(dbScope.toString()).append(";");
        }
        return result.toString();
    }

    public static String dbScopesColumnsToString(List<DbScope> dbScopes) {
        StringBuilder result = new StringBuilder();
        for (DbScope dbScope : dbScopes) {
            result.append(" ").append(dbScope.getColumn()).append(";");
        }
        return result.toString();
    }
}
