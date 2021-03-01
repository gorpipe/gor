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

package org.gorpipe.gor.model;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for verifying required column.
 *
 * @version $Id$
 */
public class RequiredColumn {
    /**
     * The column regular expression notation.
     */
    public final String notation;
    private final Pattern pattern;

    /**
     * Constructor.
     *
     * @param notation column regular expression notation
     */
    public RequiredColumn(final String notation) {
        this.notation = notation;
        this.pattern = Pattern.compile("(?i)" + notation);
    }

    /**
     * Check if required column is contained in a set of column names. The check is case insensitive.
     *
     * @param columnNames set of column names
     * @return true if required column is contained in set
     */
    public boolean containedIn(final Set<String> columnNames) {
        final Set<String> colNamesInUpperCase = new HashSet<>();
        for (String columnName : columnNames) {
            colNamesInUpperCase.add(columnName.toUpperCase());
        }

        return hasRequiredColumnByRegExpr(colNamesInUpperCase);
    }

    /**
     * Get the first column that matches the required column.
     *
     * @param columnNames set of column names to match against
     * @return the name of the first column that matched
     */
    public String getMatchedColumn(final Set<String> columnNames) {
        String matchedColumn = null;
        for (String columnName : columnNames) {
            final Matcher m = pattern.matcher(columnName);
            if (m.matches()) {
                matchedColumn = columnName;
                break;
            }
        }
        return matchedColumn;
    }

    private boolean hasRequiredColumnByRegExpr(final Set<String> columnNames) {
        // (?i) case insensitive matching
        final String matchedColumn = getMatchedColumn(columnNames);
        return matchedColumn != null && matchedColumn.length() > 0;
    }
}
