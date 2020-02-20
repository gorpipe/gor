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

package org.gorpipe.model.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @version $Id:  $
 */
public class NCGZIPInputStream extends FilterInputStream {
    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public NCGZIPInputStream(InputStream in) {
        super(in);
    }

    /**
     * Returns super.available() if the value is not zero or else always returns
     * 1.  This is necessary to get around a GZIPInputStream bug which would
     * mis-behave in some edge cases upon zero returned from available(),
     * causing file truncation.
     * <p>
     * http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7036144
     * <p>
     * Reference TT: 0034867351
     */
    @Override
    public int available() throws IOException {
        int estimate = in.available();
        return estimate == 0 ? 1 : estimate;
    }
}
