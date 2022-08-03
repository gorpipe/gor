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

package org.gorpipe.gor.table;

import org.gorpipe.gor.model.BaseMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class the represents table header.
 * <p>
 * Created by gisli on 25/07/16.
 *
 * Note on naming of the table properties:
 *  Default naming of the table properties is the same as env variables (UPPER_SNAKE_CASE).  
 */
public class TableHeader extends BaseMeta {

    private static final Logger log = LoggerFactory.getLogger(TableHeader.class);

    public static final String NO_SERIAL = "0";

    // Basic properties
    public static final String HEADER_USE_HISTORY_KEY = "USE_HISTORY";
    public static final String HEADER_VALIDATE_FILES_KEY = "VALIDATE_FILES";

    // Gor code snippet for insert select/post insert transform. For example: sort | distinct
    public static final String HEADER_SELECT_TRANSFORM_KEY = "SELECT_TRANSFORM";

    /**
     * Check if proper table header.
     *
     * @return true if the table header (heador for the dict filee) is a proper header otherwise false.
     * Header is proper if it has defined at least 2 columns and they are not dummy.
     */
    public boolean isProperTableHeader() {
        return getFileHeader() != null && getFileHeader().length > 1 && !getFileHeader()[1].equalsIgnoreCase("col2");
    }

}
