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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Deflater;

import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.model.genome.files.sort.LexRow;
import org.gorpipe.util.collection.ByteArray;
import org.gorpipe.util.collection.ByteArrayWrapper;
import org.gorpipe.model.util.Util;
import com.github.luben.zstd.ZstdOutputStream;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.DataType;
import htsjdk.samtools.util.Md5CalculatingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes data to the GOR Zip format  (gorz)
 * The key is a chromosome and positions
 */
public class GorZipLexOutputStream extends OutputStream {

    private static final Logger log = LoggerFactory.getLogger(GorZipLexOutputStream.class);

    static final int DEFAULT_CHUNK = 1024 * 32; //The default unzipped size of blocks to be zipped.
    private static final int DEF_CHR_COL = 0;
    private static final int DEF_POS_COL = 1;

    private final OutputStream target;
    private final GorIndexFile idx;
    private final ByteArrayWrapper byteOutput;
    private int beginOfLastLine = 0;
    private String headerToWrite = null;
    private boolean isHeaderWritten = false;
    private final Map<Integer, Map<String, Integer>> extLookupMap = new LinkedHashMap<>();
    private final boolean useColumnEncodingZip;
    private final boolean useZStd = Boolean.parseBoolean(System.getProperty("gor.compression.useZStd", "false"));
    private final BufferInfo[] cachedOutput = new BufferInfo[16];
    private int cachedOutputIdx = 0;
    private final byte byteToWrite;

    private final int compressionLevel;
    private final boolean base64;

    private final LexRow chrColRow;
    private String lastChr = null;

    class BufferInfo {
        byte[] keyInBytes; //the chr and pos fields of the last line in block as byte array.
        byte[] block; //Buffer to write data block to.
        int blockLen; //End of block data in block.
        byte[] zipBuffer; //Buffer to write zipped output to.

        BufferInfo() {
            this.zipBuffer = new byte[DEFAULT_CHUNK];
        }

        void updateVariables(byte[] keyInBytes, byte[] block, int blockLen) {
            this.keyInBytes = keyInBytes.clone();
            this.blockLen = blockLen;
            if (this.block == null) {
                int len = 1;
                while ((len <<= 1) < this.blockLen);
                this.block = new byte[len];
            } else if (this.block.length < this.blockLen) {
                int len = this.block.length;
                while ((len <<= 1) < this.blockLen);
                this.block = new byte[len];
            }
            System.arraycopy(block, 0, this.block, 0, blockLen);
        }
    }

    /**
     * @param fileName             name of the file to create
     * @param useColumnEncodingZip
     * @throws IOException
     */
    public GorZipLexOutputStream(String fileName, boolean useColumnEncodingZip) throws IOException {
        this(fileName, false, useColumnEncodingZip, false, GorIndexType.NONE);
    }

    /**
     * @param fileName             Name of gorz file to write to
     * @param append               true if lines should be append to the end of the file
     * @param useColumnEncodingZip
     * @throws IOException
     */
    public GorZipLexOutputStream(String fileName, boolean append, boolean useColumnEncodingZip, boolean md5, GorIndexType idx) throws IOException {
        this(new FileOutputStream(fileName, append), useColumnEncodingZip, md5 ? new File(fileName + ".md5") : null, idx != GorIndexType.NONE ? new File(fileName + DataType.GORI.suffix) : null, idx, Deflater.BEST_SPEED);
    }

    /**
     * @param fileName             Name of gorz file to write to
     * @param append               true if lines should be append to the end of the file
     * @param useColumnEncodingZip
     * @throws IOException
     */
    public GorZipLexOutputStream(String fileName, boolean append, boolean useColumnEncodingZip, boolean md5, GorIndexType idx, int compressionLevel) throws IOException {
        this(new FileOutputStream(fileName, append), useColumnEncodingZip, md5 ? new File(fileName + ".md5") : null, idx != GorIndexType.NONE ? new File(fileName + DataType.GORI.suffix) : null, idx, compressionLevel);
    }

    /**
     * @param output
     * @param useColumnEncodingZip
     */
    public GorZipLexOutputStream(OutputStream output, boolean useColumnEncodingZip, File md5File, boolean base64) throws IOException {
        this(output, DEF_CHR_COL, DEF_POS_COL, useColumnEncodingZip, md5File, null, GorIndexType.NONE, Deflater.BEST_SPEED, base64);
    }

    /**
     * @param output
     * @param useColumnEncodingZip
     */
    public GorZipLexOutputStream(OutputStream output, boolean useColumnEncodingZip, File md5File) throws IOException {
        this(output, DEF_CHR_COL, DEF_POS_COL, useColumnEncodingZip, md5File, null, GorIndexType.NONE, Deflater.BEST_SPEED, false);
    }

    /**
     * @param output
     * @param useColumnEncodingZip
     */
    public GorZipLexOutputStream(OutputStream output, boolean useColumnEncodingZip, File md5File, File idxFile, GorIndexType idxType, int compressionLevel) throws IOException {
        this(output, DEF_CHR_COL, DEF_POS_COL, useColumnEncodingZip, md5File, idxFile, idxType, compressionLevel, false);
    }

    /**
     * Default constructor
     *
     * @param output
     * @param chrCol
     * @param posCol
     * @param useColumnEncodingZip
     */
    private GorZipLexOutputStream(OutputStream output, int chrCol, int posCol, boolean useColumnEncodingZip, File md5File, File idxFile, GorIndexType idxType, int compressionLevel, boolean base64) throws IOException {
        this.chrColRow = new LexRow(chrCol, posCol);
        this.idx = idxFile != null ? new GorIndexFile(idxFile, idxType) : null;
        this.target = md5File != null ? new Md5CalculatingOutputStream(output, md5File) : output;
        this.byteOutput = new ByteArrayWrapper(DEFAULT_CHUNK);
        this.useColumnEncodingZip = useColumnEncodingZip;
        this.byteToWrite = (byte) ((useColumnEncodingZip ? 1 : 0) + ((useZStd ? 1 : 0) << 1));
        this.compressionLevel = compressionLevel;
        this.base64 = base64;
    }

    /**
     * @param data
     * @return true if flush should be forced after next write
     */
    private boolean forceFlush(byte[] data) {
        boolean forceFlush = false;
        LexRow newRow = this.chrColRow.createRow(this.chrColRow.getSource(), data, 0);
        String chr = newRow.key.chr;
        if (this.lastChr != null && !(this.lastChr.equals(chr))) {
            forceFlush = true;
        }
        this.lastChr = chr;
        return forceFlush;
    }

    /**
     * @param header
     * @throws IOException
     */
    public void setHeader(String header) throws IOException {
        if (this.isHeaderWritten || this.headerToWrite != null) {
            throw new IOException("Error: Header can only be written to the first line");
        }
        if (header != null && header.endsWith("\t")) {
            throw new IOException("Error: Header ends with a single tab character");
        }
        this.headerToWrite = header;
    }


    @Override
    public void write(int b) throws IOException {
        throw new IOException("Not supported operation.");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (this.byteOutput.size() + len >= DEFAULT_CHUNK || forceFlush(b)) {
            log.trace("Buffer zip size: {}", this.byteOutput.size());
            writeBuffer();
        }
        this.beginOfLastLine = this.byteOutput.size();
        this.byteOutput.write(b, off, len);
        if (b[off + len - 1] != '\n') {
            this.byteOutput.write('\n');
        }
    }

    /**
     * @param line line to write to the zip output
     * @throws IOException
     */
    public void write(String line) throws IOException {
        byte[] bytes = line.getBytes(Util.utf8Charset);
        write(bytes, 0, bytes.length);
    }

    /**
     * @param line line to write to the zip output
     * @throws IOException
     */
    public void write(Row line) throws IOException {
        if (this.lastChr != null && !this.lastChr.equals(line.chr)) {
            //Force flush
            writeBuffer();
            this.beginOfLastLine = 0;
            line.writeRowToStream(this.byteOutput);
            this.byteOutput.write('\n');
        } else {
            final int oldPos = this.byteOutput.size();
            line.writeRowToStream(this.byteOutput);
            this.byteOutput.write('\n');
            if (this.byteOutput.size() > DEFAULT_CHUNK) {
                if (oldPos != 0) {
                    final int leftInBuffer = this.byteOutput.size() - oldPos;
                    writeBuffer(oldPos);
                    System.arraycopy(this.byteOutput.getBuffer(), oldPos, this.byteOutput.getBuffer(),0, leftInBuffer);
                    this.byteOutput.setPos(leftInBuffer);
                } else {
                    writeBuffer();
                }
                this.beginOfLastLine = 0;
            } else this.beginOfLastLine = oldPos;
        }
        this.lastChr = line.chr;
    }


    @Override
    public void flush() throws IOException {
        writeBuffer();
        writeHeader();
        if (this.cachedOutputIdx != 0) {
            writeCachedData();
        }
        this.target.flush();
    }

    @Override
    public void close() throws IOException {
        flush();

        if (this.idx != null) {
            this.idx.close();
        }
        this.target.close();
    }

    private void writeBuffer() throws IOException {
        writeBuffer(this.byteOutput.size());
    }

    private void writeBuffer(int bufferLen) throws IOException {
        final byte[] buffer = this.byteOutput.getBuffer();
        if (bufferLen < 1) {
            return;
        }
        int endOfLastLineIdx = this.beginOfLastLine;
        while (buffer[endOfLastLineIdx++] != '\t'); //Run over chromosome key.
        //Now we are at the first byte of the position key.
        while (buffer[endOfLastLineIdx] != '\t' && buffer[endOfLastLineIdx] != '\n') ++endOfLastLineIdx; //Run over position.
        if (this.useColumnEncodingZip) {
            final byte[] dest = new byte[1024 + bufferLen];
            final int extMapSize = 32 * 1024 - (this.headerToWrite.length() + 1 + 1); // Total - header - zero - newline
            final int len = BlockPacker.encode(buffer, bufferLen, dest, this.extLookupMap, !this.isHeaderWritten, extMapSize);
            final byte[] it = new byte[len];
            System.arraycopy(dest, 0, it, 0, len);
            writeBlock(Arrays.copyOfRange(buffer, this.beginOfLastLine, endOfLastLineIdx), it, it.length);
        } else {
            writeBlock(Arrays.copyOfRange(buffer, this.beginOfLastLine, endOfLastLineIdx), buffer, bufferLen);
        }
        this.byteOutput.reset();
    }

    private void writeBlock(byte[] keyInBytes, byte[] block, int blockLen) throws IOException {
        // Cache prepared blocks and do not write to file so that an external table can be better
        if (this.cachedOutput[this.cachedOutputIdx] == null) {
            this.cachedOutput[this.cachedOutputIdx] = new BufferInfo();
        }
        final BufferInfo bi = this.cachedOutput[this.cachedOutputIdx];
        this.cachedOutputIdx += 1;
        bi.updateVariables(keyInBytes, block, blockLen);
        if (this.cachedOutputIdx == this.cachedOutput.length) {
            writeCachedData(); // Have reached the cache limit, force data into file
        }
    }

    private int base128Length(int len) {
        return (len % 7 == 0) ? (len * 8) / 7 : (len * 8) / 7 + 1;
    }
    private int base64Length(int srclen, boolean doPadding) {
        int len;
        if (doPadding) {
            len = 4 * ((srclen + 2) / 3);
        } else {
            int n = srclen % 3;
            len = 4 * (srclen / 3) + (n == 0 ? 0 : n + 1);
        }
        return len;
        //return (len % 6 == 0) ? (len * 8) / 6 : (len * 8) / 6 + 1;
    }

    private void writeHeader() throws IOException {
        if (!this.isHeaderWritten) {
            if (this.headerToWrite != null) {
                final byte[] headerbytes = this.headerToWrite.getBytes(StandardCharsets.UTF_8);
                this.target.write(headerbytes, 0, headerbytes.length);
                if (this.useColumnEncodingZip) { // Only write external column map if using column encoding
                    byte[] zippedExtMap = BlockPacker.bytesFromLookupMap(extLookupMap);
                    byte[] zipBuffer = this.useZStd ? zipItZStd(zippedExtMap, compressionLevel) : zipItZLib(zippedExtMap, compressionLevel);
                    int len = zipBuffer.length;
                    int siz = base64 ? base64Length(len, true) : base128Length(len);
                    if( zippedExtMap.length < siz ) {
                        zippedExtMap = new byte[siz];
                    }
                    if( base64 ) siz = Base64.getEncoder().encode(Arrays.copyOfRange(zipBuffer,0,len), zippedExtMap);
                    else ByteArray.to7Bit(zipBuffer, len, zippedExtMap);

                    // Must ensure that header + external tables are less than one read block in size. (64K/2 by default)
                    if (siz + headerbytes.length + 1 > DEFAULT_CHUNK) {
                        throw new GorSystemException("Can't zip, external maps + headers are to long, i.e. > " + DEFAULT_CHUNK, null);
                    }

                    this.target.write((byte) 0);
                    this.target.write(zippedExtMap, 0, siz);
                    this.start += siz+1;
                }
                this.target.write('\n');
                this.start += headerbytes.length + 1;
            } else log.warn("No header written in gorz");
            this.isHeaderWritten = true;
        }
    }

    private long start = 0;

    private void writeCachedData() throws IOException {
        writeHeader();
        Arrays.stream(this.cachedOutput, 0, this.cachedOutputIdx).parallel().forEach(bufferInfo -> {
            int len = this.useZStd ? zipItZStd(bufferInfo, compressionLevel) : zipItZLib(bufferInfo, compressionLevel);
            byte[] buffer = bufferInfo.block;
            byte[] zipBuffer = bufferInfo.zipBuffer;
            int siz = base64 ? base64Length(len, true) : base128Length(len);
            if( buffer.length < siz ) {
                int newLen = buffer.length;
                while ((newLen <<= 1) < siz);
                buffer = new byte[newLen];
                bufferInfo.block = buffer;
            }
            if( base64 ) {
                siz = Base64.getEncoder().encode(Arrays.copyOfRange(zipBuffer,0,len), buffer);
            } else ByteArray.to7Bit(zipBuffer, len, buffer);
            bufferInfo.blockLen = siz;
        });

        BufferInfo bufferInfo;
        for (int i = 0; i < this.cachedOutputIdx; ++i) {
            bufferInfo = this.cachedOutput[i];
            final byte[] keyInBytes = bufferInfo.keyInBytes;
            this.target.write(keyInBytes);
            this.target.write('\t');
            this.target.write(this.byteToWrite);
            this.target.write(bufferInfo.block, 0, bufferInfo.blockLen);
            this.target.write('\n');

            if (this.idx != null) {
                this.start += 3 + keyInBytes.length + bufferInfo.blockLen;
                String key = new String(keyInBytes);
                String[] components = key.split("\t");
                this.idx.putFilePosition(components[0], Integer.parseInt(components[1]), this.start);
            }
        }
        this.cachedOutputIdx = 0;
    }

    private static byte[] zipItZLib(byte[] buffer, int compressionLevel) {
        final Deflater deflater = new Deflater(compressionLevel);
        deflater.setInput(buffer);
        deflater.finish();
        byte[] toWriteTo = new byte[1024];
        int len = 0;
        do {
            len += deflater.deflate(toWriteTo, len, toWriteTo.length - len, 0);
            if (deflater.finished()) break;
            if (len == toWriteTo.length) {
                final byte[] oldToWriteTo = toWriteTo;
                toWriteTo = new byte[len << 1];
                System.arraycopy(oldToWriteTo, 0, toWriteTo, 0, len);
            }
        } while (true);
        deflater.end();
        return Arrays.copyOfRange(toWriteTo, 0, len);
    }

    private static byte[] zipItZStd(byte[] buffer, int compressionLevel) {
        final ByteArrayWrapper bufferToWriteTo = new ByteArrayWrapper(16);
        try (final ZstdOutputStream zipStream = new ZstdOutputStream(bufferToWriteTo, compressionLevel)) {
            zipStream.write(buffer);
            zipStream.flush();
            zipStream.close();
            return bufferToWriteTo.toByteArray();
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
    }

    private static int zipItZLib(BufferInfo bufferInfo, int compressionLevel) {
        final Deflater deflater = new Deflater(compressionLevel);
        deflater.setInput(bufferInfo.block, 0, bufferInfo.blockLen);
        deflater.finish();
        int len = 0;
        do {
            len += deflater.deflate(bufferInfo.zipBuffer, len, bufferInfo.zipBuffer.length - len, 0);
            if (deflater.finished()) break;
            if (len == bufferInfo.zipBuffer.length) {
                final byte[] oldZipBuffer = bufferInfo.zipBuffer;
                bufferInfo.zipBuffer = new byte[len << 1];
                System.arraycopy(oldZipBuffer, 0, bufferInfo.zipBuffer, 0, len);
            }
        } while (true);
        deflater.end();
        return len;
    }

    private static int zipItZStd(BufferInfo bufferInfo, int compressionLevel) {
        final ByteArrayWrapper zipBuffer = new ByteArrayWrapper(bufferInfo.zipBuffer);
        try (final ZstdOutputStream zipStream = new ZstdOutputStream(zipBuffer, compressionLevel)){
            zipStream.write(bufferInfo.block, 0, bufferInfo.blockLen);
            zipStream.flush();
            zipStream.close();
            bufferInfo.zipBuffer = zipBuffer.getBuffer();
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
        return zipBuffer.size();
    }
}