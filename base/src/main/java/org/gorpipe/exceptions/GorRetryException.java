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

public abstract class GorRetryException extends GorException {

    // Request retry is a low level retry where the request is retried with the same parameters.
    private boolean retry = false;
    // Operation retry is a higher level retry where the whole operation is retried.
    private boolean fullRetry = false;

    protected GorRetryException(String message, Throwable cause) {
        super(message, cause);
    }

    public boolean isRetry() {
        return retry;
    }

    public boolean isFullRetry() {
        return fullRetry;
    }

    public GorRetryException noRetry() {
        retry = false;
        fullRetry = false;
        return this;
    }

    public GorRetryException retry() {
        retry = true;
        return this;
    }

    public GorRetryException fullRetry() {
        fullRetry = true;
        return this;
    }


}
