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

package org.gorpipe.gor.driver.adapters;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.RacFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by villi on 06/01/17.
 */
public class StreamSourceRacFile extends RacFile {
    private final static Logger log = LoggerFactory.getLogger(StreamSourceRacFile.class);
    private final StreamSourceSeekableFile delegate;
    private boolean open = true;

    public StreamSourceRacFile(StreamSource source) {
        this.delegate = new StreamSourceSeekableFile(source);
    }

    @Override
    public long length() throws IOException {
        return delegate.length();
    }

    @Override
    public int read() throws IOException {
        return this.delegate.read();
    }

    @Override
    public int read(byte[] buf, int offset, int len) {
        try {
            return delegate.read(buf, offset, len);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void seek(long pos) {
        try {
            delegate.seek(pos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            open = false;
            delegate.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (open) {
            try {
                close();
            } catch (Exception e) {
                // Ignore here
            }
            String msg = "StreamSourceRacFile(" + delegate.getDataSource().getName() + ") not closed on finalize - auto closing";
            log.warn(msg);
        }
        super.finalize();
    }

}
