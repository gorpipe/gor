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

package org.gorpipe.exceptions.custom;

/**
 * GorTimeoutException indicates a running operation having exceeded its allotted time to execute.
 *
 * @version $Id$
 */
public class GorTimeoutException extends RuntimeException {

    /**
     * Construct
     *
     * @param msg The overall message
     */
    public GorTimeoutException(String msg) {
        this(msg, null);
    }


    /**
     * Construct
     *
     * @param msg     The overall message
     * @param ex      The causing exception
     */
    public GorTimeoutException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
