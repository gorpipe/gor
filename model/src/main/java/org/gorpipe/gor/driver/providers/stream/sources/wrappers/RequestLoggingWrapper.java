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

package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.gor.driver.adapters.PositionAwareInputStream;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSourceMetadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class RequestLoggingWrapper extends WrappedStreamSource {

    private StreamSourceMetadata sourceMeta;
    private File logFile;

    public RequestLoggingWrapper(StreamSource wrapped) throws IOException {
        super(wrapped);
        sourceMeta = wrapped.getSourceMetadata();
        logFile = new File("/tmp/" + UUID.fromString(sourceMeta.toString()));
        writeLine("Opening source: " + sourceMeta.toString());
    }

    private void writeLine(String string) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(logFile)) {
            fos.write((string + "\n").getBytes());
        }
    }

    @Override
    public InputStream open() throws IOException {
        return wrapStream(super.open(), null, null);
    }

    @Override
    public InputStream open(long start) throws IOException {
        return wrapStream(super.open(start), start, null);
    }

    @Override
    public InputStream open(long start, long minLength) throws IOException {
        return wrapStream(super.open(start, minLength), start, minLength);
    }

    private InputStream wrapStream(InputStream open, Long start,
                                   Long minLength) {
        return new LoggingStream(open, start, minLength);
    }

    public class LoggingStream extends PositionAwareInputStream {

        public LoggingStream(InputStream in, Long start, Long minLength) {
            super(in);
            // TODO Auto-generated constructor stub
        }


    }

}
