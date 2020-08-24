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

/**
 * The types of dialogs currently supported
 *
 * @author arnie
 * @version $Id$
 */
public enum DialogType {
    // TODO as enums cannot be extended, all dialog types need to be included here
    // this should be refactored to not use enum
    /**
     * A generic dialog
     */GENERIC,
    /**
     * A dialog representing a SDL set query
     */SET,
    /**
     * A dialog representing a SDL report query (EVR)
     */REPORT,
    /**
     * A dialog representing a SDL report query, expecting a base set
     */LOG_REPORT,
    /**
     * A dialog which contains hints on how to render query results
     */HTML_COL
}
