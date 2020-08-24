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

package org.gorpipe.gor.driver.pgen;

import org.gorpipe.util.collection.ByteArray;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class VariableWidthPGenOutputStream extends PGenOutputStream<VariantRecord> {
    private static final byte STANDARD_PLINK2_FORMAT_MAGIG_BYTE = (byte) 0x10;
    private static final int BLOCK_LEN = 65_536;

    private final List<File> tmpFiles;
    private final List<byte[]> recordTypes;
    private byte[] currentRecordTypes;
    private final List<int[]> recordLengths;
    private int[] currentRecordLengths;
    private final List<Long> blockLengths;
    private long currentBlockLen = 0;
    private final String tmpFilePrefix;
    private final byte[] buffer;
    private int bufferIdx = 0;
    private OutputStream os;
    private int variantsInCurrentFile = 0;

    VariableWidthPGenOutputStream(String fileName) throws FileNotFoundException {
        super(fileName);
        final String fileNameWithoutEnding = fileName.substring(0, fileName.lastIndexOf('.'));
        this.tmpFilePrefix = fileNameWithoutEnding + UUID.randomUUID().toString();

        this.tmpFiles = new ArrayList<>();
        this.recordTypes = new ArrayList<>();
        this.recordLengths = new ArrayList<>();
        this.blockLengths = new ArrayList<>();

        prepareNextTmpFile();
        this.buffer = new byte[1_048_576];
    }

    @Override
    void write(VariantRecord record) throws IOException {
        super.handleNumberOfSamples(record);

        if (this.variantsInCurrentFile == BLOCK_LEN) {
            closeCurrentStream();
            prepareNextTmpFile();
            this.blockLengths.add(this.currentBlockLen);
            this.currentBlockLen = 0;
            this.variantsInCurrentFile = 0;
        }

        int variantLen = 0;
        int read;
        while ((read = record.write(this.buffer, this.bufferIdx, this.buffer.length - this.bufferIdx)) > 0 || (read == 0 && this.bufferIdx == this.buffer.length)) {
            this.os.write(this.buffer, 0, this.bufferIdx + read);
            this.bufferIdx = 0;
            variantLen += read;
        }
        this.bufferIdx -= read;
        variantLen -= read;
        this.currentBlockLen += variantLen;

        this.currentRecordLengths[this.variantsInCurrentFile] = variantLen;
        this.currentRecordTypes[this.variantsInCurrentFile] = record.getType();
        ++this.variantsInCurrentFile;
        ++this.numberOfVariants;
    }

    @Override
    public void close() throws IOException {
        closeCurrentStream();
        if (this.numberOfVariants != 0) {
            this.os = new FileOutputStream(this.fileName);
            writeHeader();
            mergeVariantBlocks();
        }
    }

    private void writeHeader() throws IOException {
        writeFirstTwelve();
        computeAndWriteOffsets();
        writeVariantRecordTypesAndLengths();
    }

    private void writeFirstTwelve() throws IOException {
        final byte[] firstTwelve = new byte[12];
        firstTwelve[0] = MAGIC_BYTE_1;
        firstTwelve[1] = MAGIC_BYTE_2;
        firstTwelve[2] = STANDARD_PLINK2_FORMAT_MAGIG_BYTE;
        ByteArray.writeInt(firstTwelve, 3, ByteOrder.LITTLE_ENDIAN, this.numberOfVariants);
        ByteArray.writeInt(firstTwelve, 7, ByteOrder.LITTLE_ENDIAN, this.numberOfSamples);
        firstTwelve[11] = 0x07;
        this.os.write(firstTwelve);
    }

    private void computeAndWriteOffsets() throws IOException {
        final int numberOfBlocks = this.tmpFiles.size();
        long offset = 12L + 8 * numberOfBlocks + 5 * this.numberOfVariants;
        ByteArray.writeLong(this.buffer, 0, ByteOrder.LITTLE_ENDIAN, offset);
        for (int i = 1; i < numberOfBlocks; ++i) {
            offset += this.blockLengths.get(i - 1);
            ByteArray.writeLong(this.buffer, 8 * i, ByteOrder.LITTLE_ENDIAN, offset);
        }
        this.os.write(this.buffer, 0, 8 * numberOfBlocks);
    }

    private void writeVariantRecordTypesAndLengths() throws IOException {
        final int numberOfBlocks = this.tmpFiles.size();
        for (int blockIdx = 0; blockIdx < numberOfBlocks; ++blockIdx) {
            writeVariantRecordTypesAndLengths(blockIdx);
        }
    }

    private void writeVariantRecordTypesAndLengths(int blockIdx) throws IOException {
        final int numberOfVariantsInBlock = Math.min(BLOCK_LEN, this.numberOfVariants - blockIdx * BLOCK_LEN);
        this.os.write(this.recordTypes.get(blockIdx), 0, numberOfVariantsInBlock);
        final int[] lengths = this.recordLengths.get(blockIdx);
        for (int i = 0; i < numberOfVariantsInBlock; ++i) {
            ByteArray.writeInt(this.buffer, this.bufferIdx, ByteOrder.LITTLE_ENDIAN, lengths[i]);
            this.bufferIdx += 4;
        }
        this.os.write(this.buffer, 0, this.bufferIdx);
        this.bufferIdx = 0;
    }

    private void mergeVariantBlocks() throws IOException {
        for (File tmpFile : this.tmpFiles) {
            writeFileToStream(tmpFile);
            Files.delete(tmpFile.toPath());
        }
    }

    private void writeFileToStream(File file) throws IOException {
        try (final InputStream is = new FileInputStream(file)) {
            int read;
            while ((read = is.read(this.buffer)) > 0) {
                this.os.write(this.buffer, 0, read);
            }
        }
    }

    private void closeCurrentStream() throws IOException {
        if (this.bufferIdx > 0) {
            this.os.write(this.buffer, 0, this.bufferIdx);
            this.bufferIdx = 0;
        }
        this.os.close();
    }

    private void prepareNextTmpFile() throws FileNotFoundException {
        final File currentFile = new File(this.tmpFilePrefix + "_chr" + this.tmpFiles.size() + ".tmp");
        this.os = new FileOutputStream(currentFile);
        this.tmpFiles.add(currentFile);

        this.currentRecordTypes = new byte[BLOCK_LEN];
        this.recordTypes.add(this.currentRecordTypes);

        this.currentRecordLengths = new int[BLOCK_LEN];
        this.recordLengths.add(this.currentRecordLengths);
    }
}
