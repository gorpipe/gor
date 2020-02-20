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

import java.io.IOException;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * Created by sigmar on 14/03/16.
 */
public class BlockCompressedSeekableFile extends StreamSourceSeekableFile {
    BlockCompressedInputStream bcis;

    public BlockCompressedSeekableFile(StreamSource source) {
        super(source);
        StreamSourceSeekableStream ssss = new StreamSourceSeekableStream(source);
        bcis = new BlockCompressedInputStream(ssss);
    }


    @Override
    public long getFilePointer() {
        return bcis.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        bcis.seek(pos);
    }

    @Override
    public long length() throws IOException {
        return this.getDataSource().getSourceMetadata().getLength();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return bcis.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        bcis.close();
    }
}
