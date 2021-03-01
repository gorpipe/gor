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

/**
 * Argument description class.
 *
 * @version $Id$
 */
public class ArgumentDescription {
    public final String name;
    public final String shortDescr;
    public final String tooltip;
    public final String displayName;

    /**
     * @param name        argument name
     * @param shortDescr  short description
     * @param displayName the display name
     * @param tooltip     the tooltip
     */
    public ArgumentDescription(final String name, final String shortDescr, final String displayName, final String tooltip) {
        this.name = name;
        this.shortDescr = shortDescr;
        this.displayName = displayName;
        this.tooltip = tooltip;
    }

    /**
     * Get the display name. If display name is not defined then name is used instead.
     *
     * @return the display name
     */
    public String getDisplayName() {
        if (displayName != null && displayName.length() > 0) {
            return displayName;
        } else {
            return name;
        }
    }
}
