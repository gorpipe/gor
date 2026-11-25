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

/**
 * A source was replaced while it was being read, so the read can't be resumed at the offset it reached (ENGKNOW-4312).
 * <p>
 * Not retryable at the request level: reading on would mix the old and the new content. Readers of small files that
 * are read whole (dictionaries, tag files) catch it and read again from the start. Anything else fails, flagged for
 * a full retry so a GOR worker re-queues the job. The cause (e.g. "Stale file handle") is kept out of the message so
 * message-based retry classifiers don't mistake this for a request-level retry.
 */
public class GorSourceChangedException extends GorRetryException {

    private final String source;

    public GorSourceChangedException(String source, long offset, Throwable cause) {
        super(String.format("Source %s changed during read (reopened at offset %d after a retryable read error)",
                source, offset), cause);
        this.source = source;
        fullRetry();
    }

    public String getSource() {
        return source;
    }
}
