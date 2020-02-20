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

package org.gorpipe.model.genome.files.binsearch;

import com.github.luben.zstd.ZstdInputStream;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.util.collection.ByteArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Unzipper {
    private Inflater inflater;
    boolean done = false;
    protected CompressionType type;
    private ZstdInputStream zstdIs;

    public void setType(CompressionType type) {
        this.type = type;
        if (this.type == CompressionType.ZLIB) {
            this.inflater = new Inflater();
        }
    }

    public void setInput(byte[] in, int offset, int len) {
        final int newLen = ByteArray.to8BitInplace(in, offset, len);
        setRawInput(in, offset, newLen);
    }

    public void setRawInput(byte[] in, int offset, int len) {
        this.done = false;
        if (this.type == CompressionType.ZLIB) {
            this.inflater.reset();
            this.inflater.setInput(in, offset, len);
        } else {
            try {
                this.zstdIs = new ZstdInputStream(new ByteArrayInputStream(in, offset, len));
            } catch (IOException e) {
                throw new GorSystemException(e);
            }
        }
    }

    public int decompress(byte[] out, int offset, int len) throws DataFormatException, IOException {
        final int toReturn;
        if (this.done) {
            toReturn = 0;
        } else {
            if (this.type == CompressionType.ZLIB) {
                toReturn = this.inflater.inflate(out, offset, len);
                this.done = this.inflater.finished();
            } else {
                toReturn = this.zstdIs.read(out, offset, len);
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
