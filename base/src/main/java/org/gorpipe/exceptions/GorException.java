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

public abstract class GorException extends RuntimeException {

    String requestID = "";
    Object context;

    protected GorException(String message, Throwable cause) {
        super(message, cause);
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getRequestID() {
        return this.requestID;
    }

    public void setContext(Object ctx) {
        context = ctx;
    }

    public Object getContext() {
        return context;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append("\n");

        if (!ExceptionUtilities.isNullOrEmpty(requestID)) {
            builder.append("Request ID: ");
            builder.append(requestID);
            builder.append("\n");
        }

        if (getCause() != null) {
            var fullCause = ExceptionUtilities.getFullCause(this.getCause());
            if (!ExceptionUtilities.isNullOrEmpty(fullCause)) {
                builder.append("Cause: ");
                builder.append(fullCause);
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
