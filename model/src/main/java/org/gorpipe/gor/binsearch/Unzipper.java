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

package org.gorpipe.gor.binsearch;

import com.github.luben.zstd.ZstdInputStream;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.util.collection.ByteArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Unzipper {
    private Inflater inflater;
    boolean done = false;
    protected CompressionType type;
    private ZstdInputStream zstdIs;
    private boolean firstBlock = true;

    public ByteBuffer out;
    public final ByteBuffer rawDataHolder;

    private static final int BUFFER_START_SIZE = 32768;

    public Unzipper() {
        rawDataHolder = ByteBuffer.allocate(BUFFER_START_SIZE);
        out = ByteBuffer.allocate(BUFFER_START_SIZE);
    }

    private int getBeginningOfBlock(ByteBuffer in) {
        int idx = 0;
        while (idx < in.position() && in.get(idx++) != '\t');
        while (idx < in.position() && in.get(idx++) != '\t');

        if (idx == in.position() || idx + 1 == in.position()) {
            String msg = String.format("Could not find zipped block in %s%nBuffer contains %d bytes", /*this.filePath*/"", in.position());
            throw new GorDataException(msg);
        }

        if (this.firstBlock) {
            byte beginOfBlockByte = in.get(idx);
            final CompressionType type = (beginOfBlockByte & 0x02) == 0 ? CompressionType.ZLIB : CompressionType.ZSTD;
            setType(type);
            this.firstBlock = false;
        }

        return idx + 1;
    }

    public int unzipBlock() throws DataFormatException, IOException {
        final int len = rawDataHolder.position();
        final int blockIdx = getBeginningOfBlock(rawDataHolder);

        final int newLen = ByteArray.to8BitInplace(rawDataHolder.array(), blockIdx, len-blockIdx);
        int totalRead = 0;
        setInput(rawDataHolder, blockIdx, newLen);
        do {
            int read;
            while ((read = decompress(totalRead, out.capacity() - totalRead)) > 0) {
                totalRead += read;
            }
            if (totalRead == out.capacity()) {
                ByteBuffer tmpbuffer = ByteBuffer.allocate(2 * this.out.capacity());
                tmpbuffer.put(out);
                out.clear();
                out = tmpbuffer;
            } else {
                break;
            }
        } while (true);
        return totalRead;
    }

    public void setType(CompressionType type) {
        this.type = type;
        if (this.type == CompressionType.ZLIB) {
            this.inflater = new Inflater();
        }
    }

    public void setInput(ByteBuffer in, int offset, int len) {
        this.done = false;
        if (this.type == CompressionType.ZLIB) {
            this.inflater.reset();
            this.inflater.setInput(in.array(), offset, len);
        } else {
            try {
                this.zstdIs = new ZstdInputStream(new ByteArrayInputStream(in.array(), offset, len));
            } catch (IOException e) {
                throw new GorSystemException(e);
            }
        }
    }

    public int decompress(int offset) throws DataFormatException, IOException {
        return decompress(offset, out.capacity());
    }

    public int decompress(int offset, int len) throws DataFormatException, IOException {
        final int toReturn;
        if (this.done) {
            toReturn = 0;
        } else {
            if (this.type == CompressionType.ZLIB) {
                toReturn = this.inflater.inflate(out.array(), offset, len);
                this.done = this.inflater.finished();
            } else {
                toReturn = this.zstdIs.read(out.array(), offset, len);
                this.done = this.zstdIs.available() == 0;
                if (this.done) {
                    this.zstdIs.close();
                    this.zstdIs = null;
                }
            }
        }
        return toReturn;
    }
}
