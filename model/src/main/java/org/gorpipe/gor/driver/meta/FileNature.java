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

package org.gorpipe.gor.driver.meta;

/**
 * Classification of Source types
 * <p>
 * Created by villi on 24/08/15.
 */
public enum FileNature {
    /**
     * File with variant data
     **/
    VARIANTS,
    /**
     * A table of files (e.g. gord, gort,)
     **/
    TABLE,
    /**
     * An index file (bai ..)
     **/
    INDEX,
    /**
     * A reference to another file (e.g. .link)
     **/
    REFERENCE,
    /**
     * A reference to a md5 file
     **/
    MD5_LINK,
    /**
     * A reference to a meta file
     **/
    METAINFO,
    /**
     * A report file
     **/
    REPORT,
    /**
     * A script file
     **/
    SCRIPT,
    /**
     * A compressed file
     **/
    COMPRESSED
}
