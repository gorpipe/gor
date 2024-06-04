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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public class GorResourceException extends GorUserException {

    private final String uri;

    public GorResourceException(String message, String uri) {
        this(message, uri, null);
    }

    public GorResourceException(String message, String uri, Throwable cause) {
        this(message, uri, cause, true);
    }

    public GorResourceException(String message, String uri, Throwable cause, boolean doFormat) {
        super(doFormat ? message + "\nURI: " + uri : message, cause);
        this.uri = uri;
    }

    public String getUri() {
        return this.uri;
    }

    public static GorResourceException fromIOException(IOException e, String uri) {
        var message = e.getMessage();
        if (e instanceof FileNotFoundException fnfe) {
            message = "Input source does not exist: " + uri;
        }
        return new GorResourceException(message, uri, e);
    }

    public static GorResourceException fromIOException(IOException e, Path uri) {
        return fromIOException(e, uri.toString());
    }
}
