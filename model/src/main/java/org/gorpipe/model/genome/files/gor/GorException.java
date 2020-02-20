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

package org.gorpipe.model.genome.files.gor;

import org.gorpipe.model.util.Util;

import java.io.PrintStream;

/**
 * GorException is an exception that is handled by the thrower and its message needs
 * to be given to the user.
 *
 * @version $Id$
 */
public class GorException extends RuntimeException {
    /**
     * The details associated with the message.
     */
    public final String details;

    /**
     * Construct
     *
     * @param msg     The overall message
     * @param details Detailed message
     */
    public GorException(String msg, String details) {
        this(msg, details, null);
    }

    /**
     * Construct
     *
     * @param msg     The overall message
     * @param details Detailed message
     * @param ex      The causing exception
     */
    public GorException(String msg, String details, Throwable ex) {
        super(msg, ex);
        this.details = Util.nvl(details, "");
    }

    @Override
    public void printStackTrace(PrintStream s) {
        s.print(details);
        s.print('\n');
        super.printStackTrace(s);
    }
}
