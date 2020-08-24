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

package org.gorpipe.model.genome.files.gor.pgen;

import org.gorpipe.util.collection.ByteArray;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;

abstract class FixedWidthPGenOutputStream<T extends VariantRecord> extends PGenOutputStream<T> {
    private final byte[] buffer;
    private OutputStream os;
    private int bufferIdx = 0;
    private boolean firstVariant = true;

    FixedWidthPGenOutputStream(String fileName) {
        super(fileName);
        this.buffer = new byte[1024 * 1024];
    }

    @Override
    void write(T record) throws IOException {
        if (this.firstVariant) {
            this.os = new FileOutputStream(this.fileName);
            writeHeaderBlock();
            this.firstVariant = false;
        }
        super.handleNumberOfSamples(record);
        int read;
        while ((read = record.write(this.buffer, this.bufferIdx, this.buffer.length - this.bufferIdx)) > 0 || (read == 0 && this.bufferIdx == this.buffer.length)) {
            this.os.write(this.buffer, 0, this.bufferIdx + read);
            this.bufferIdx = 0;
        }
        this.bufferIdx -= read; //read is negative.
        ++this.numberOfVariants;
    }

    @Override
    public void close() throws IOException {
        if (this.numberOfVariants != 0) {
            this.os.write(this.buffer, 0, this.bufferIdx);
            this.os.close();
            final byte[] bytes = new byte[8];
            ByteArray.writeInt(bytes, 0, ByteOrder.LITTLE_ENDIAN, this.numberOfVariants);
            ByteArray.writeInt(bytes, 4, ByteOrder.LITTLE_ENDIAN, this.numberOfSamples);
            try (final RandomAccessFile raf = new RandomAccessFile(this.fileName, "rw")) {
                raf.seek(3);
                raf.write(bytes);
            }
        }
    }

    abstract byte getStorageModeByte();

    private void writeHeaderBlock() throws IOException {
        final byte[] headerBlock = new byte[12];
        headerBlock[0] = MAGIC_BYTE_1;
        headerBlock[1] = MAGIC_BYTE_2;
        headerBlock[2] = getStorageModeByte();
        this.os.write(headerBlock);
    }
}
