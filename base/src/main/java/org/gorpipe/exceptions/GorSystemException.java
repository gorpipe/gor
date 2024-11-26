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

package org.gorpipe.exceptions;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * GorSystemException wraps a checked exception into an unchecked one, but adds no extra info other than a message string.
 * <p>
 * The reason to use a separate class is to easily identify that we are simply wrapping another exception, which is
 * not possible if you use RuntimeException directly.
 * <p>
 *
 * @version $Id$
 */
public class GorSystemException extends GorException {
    /**
     * Construct
     *
     * @param cause The causing exception to wrap.
     */
    public GorSystemException(Throwable cause) {
        super(cause.getClass() + ": " + cause.getMessage(), cause);
    }

    /**
     * Construct
     *
     * @param message The message to display.
     */
    public GorSystemException(String message) {
        super(message, null);
    }

    /**
     * Construct
     *
     * @param message The message to display.
     * @param cause The causing exception to wrap.
     */
    public GorSystemException(String message, Throwable cause) {
        super(message + (cause == null ? "" : "\n" + cause.getClass() + ": " + cause.getMessage()), cause);
    }


    @Override
    public String toString() {
        return this.getCause() != null ? this.getCause().toString() : super.toString();
    }

    @Override
    public void printStackTrace(PrintStream s) {
        if (this.getCause() != null) {
            this.getCause().printStackTrace(s);
        } else {
            super.printStackTrace(s);
        }
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        if (this.getCause() != null) {
            this.getCause().printStackTrace(s);
        } else {
            super.printStackTrace(s);
        }
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return this.getCause() != null ? this.getCause().getStackTrace() : super.getStackTrace();
    }

    // Note:  Will not overwrite setStackTrace and fillInStackTrace, and calling those will not have any
    //        affect on the reported stacktrace (which will always be from the underlying exception).
    //        If you wish to change the stacktrace, change the underlying exception.
}
