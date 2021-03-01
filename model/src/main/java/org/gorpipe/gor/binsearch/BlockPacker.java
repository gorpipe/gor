/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.util.ByteTextBuilder;
import org.gorpipe.gor.util.GLongHashMap;
import org.gorpipe.gor.util.Util;
import org.gorpipe.util.collection.ByteArray;
import org.gorpipe.util.collection.IntArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * BlockPacker encodes and decodes a sequence of GOR compatible lines into a more compact form. The encoded form is as folows:
 * block: rowcnt | { type } | { column }
 * rowcnt: unsigned 16 bits
 * type: zero terminated sequence of typeid
 * {@literal typeid: byte := 3->intoffset, 4-> shortoffset, 5->byteoffset, 6->longseq, 7->incr, 8->constant, 9->unsigned byte diff, 10->unsigned short diff, 11->unsigned int diff}
 * {@literal 12->signed byte diff, 13->signed short diff, 19->empty, 20->charseq, 21->varcharseq, 22->textlookup, 23->sametext, 24->externalTextLookup, 25->externalTextLookupWithDiff}
 * {@literal 26->externalTextLookupWithCycle, 28->textlookupWithDiff, 29->externalTextLookupWith16bitDiff}
 * column: sequence of coldata
 * coldata: { longseq | byteoffset | shortoffset | intoffset | incr | constant | usbytediff | sbytediff | ushortdiff | sshortdiff | uintdiff | charseq | varcharseq | textlookup | textlookupdiff | sametest }
 * longseq: sequence of 64 bits
 * byteoffset: 64 bit base value and byteseq of value to add to base
 * shortoffset: 64 bit base value and shortseq of value to add to base
 * intoffset: 64 bit base value and intseq of value to add to base
 * incr: 64 bits base value and 32 bit step size
 * constant: 64 bits constant value
 * usbytediff: 64 bit value of first row, unsigned byte sequence of diff from last value
 * sbytediff: 64 bit value of first row, byte sequence of diff from last value
 * usshortdiff: 64 bit value of first row, unsigned short sequence of diff from last value
 * sshortdiff: 64 bit value of first row, short sequence of diff from last value
 * usintdiff: 64 bit value of first row, unsigned int sequence of diff from last value
 * charseq: 16 bit len and sequence of constant size byteseq
 * varcharseq: sequence of zero ending byteseq
 * textlookup: 8 bit length, varcharseq of distinct values and byteseq of references
 * textlookupdiff: 8 bit length, varcharseq of distinct values and byteseq of references diffs, i.e. first value is v[0], i-th value is v[i]-v[i-1]
 * sametext: varchar constant
 *
 * @version $Id$
 */
public class BlockPacker {
    private static final Logger log = LoggerFactory.getLogger(BlockPacker.class);
    private static final String[] ENCODE_TYPES = {"", "", "", "ioff", "soff", "boff", "lseq", "incr", "lconst", "ubdiff", "usdiff", "uidiff", "bdiff", "sdiff", "", "", "", "", "", "empty", "cseq", "vcseq", "tlookup", "tconst", "exttlookup", "exttlookupdiff", "", "lookupcycle", "lookupdiff", "extlookupdiffmulti"};

    /**
     * Decode a block previously encoded by format defined by this class
     *
     * @param src        The source byte buffer containing the block
     * @param off        The position in buffer where the block starts
     * @param dest       The destination byte buffer for the decoded block
     * @param destOffset The position in the destination buffer to start writing
     * @return The number of bytes written into the destination buffer
     */
    public static int decode(final byte[] src, int off, byte[] dest, int destOffset) {
        return decode(src, off, dest, destOffset, new HashMap<>());
    }

    /**
     * Decode a block previously encoded by format defined by this class
     *
     * @param src               The source byte buffer containing the block
     * @param off               The position in buffer where the block starts
     * @param dest              The destination byte buffer for the decoded block
     * @param destOffset        The position in the destination buffer to start writing
     * @param mapExternalTables External lookup table content, used to get values from
     * @return The number of bytes written into the destination buffer
     */
    public static int decode(final byte[] src, int off, byte[] dest, int destOffset, Map<Integer, Map<Integer, byte[]>> mapExternalTables) {
        // Read the rowcnt
        final int rowcnt = ByteArray.readUnsignedShortBigEndian(src, off);

        // Read the types
        int pos = off + 2;
        final IntArray types = new IntArray();
        while (src[pos] != 0) {
            types.add(src[pos++]);
        }
        pos++; // Increment past the zero termination

        // Read the column data
        if (log.isTraceEnabled()) {
            log.trace("{} rows : column encoding: {}", rowcnt, toEncodeTypes(types));
        }

        final RowDecoder[] decoders = new RowDecoder[types.size()];
        for (int i = 0; i < types.size(); i++) {
            final int type = types.get(i);
            decoders[i] = getDecoder(src, pos, type, rowcnt, mapExternalTables.get(i));
            pos += decoders[i].getReadLen();
        }

        // Write the decoded block into the destination buffer
        int dp = destOffset;
        final int colcnt = decoders.length;
        if (colcnt != 0) {
            for (int i = 0; i < rowcnt; i++) {
                dp += decoders[0].decodeNext(dest, dp);
                for (int j = 1; j < colcnt; j++) {
                    dest[dp++] = '\t';
                    dp += decoders[j].decodeNext(dest, dp);
                }
                dest[dp++] = '\n';
            }
        }

        return dp;
    }

    private static String toEncodeTypes(final IntArray types) {
        final StringBuilder sb = new StringBuilder(200);
        for (int idx : types) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(ENCODE_TYPES[idx]);
        }
        return sb.toString();
    }

    /**
     * Encoded the specified block into the specified destination buffer
     *
     * @param block The block to encode
     * @param dest  The destination byte buffer
     * @return The number of bytes written
     */
    public static int encode(String block, byte[] dest) {
        return encode(block.getBytes(), dest);
    }

    /**
     * Encoded the specifed block into the specified destination buffer
     *
     * @param src  The block to encode
     * @param dest The destination byte buffer
     * @return The number of bytes written
     */
    public static int encode(byte[] src, byte[] dest) {
        return encode(src, src.length, dest, new HashMap<>(), false, 0);
    }

    /**
     * Encoded the specifed block into the specified destination buffer
     *
     * @param src               The block to encode
     * @param dest              The destination byte buffer
     * @param mapExternalTables A map of available external tables
     * @param allowAddExtTable  True if the map of external tables can be appended
     * @param extTableSize      The total number of bytes allowed to be written into the external map
     * @return The number of bytes written
     */

    public static int encode(byte[] src, byte[] dest, Map<Integer, Map<String, Integer>> mapExternalTables, boolean allowAddExtTable, int extTableSize) {
        return encode(src, src.length, dest, mapExternalTables, allowAddExtTable, extTableSize);
    }

    public static int encode(byte[] src, int bufferLen, byte[] dest, Map<Integer, Map<String, Integer>> mapExternalTables, boolean allowAddExtTable, int extTableSize) {
        // Start by parsing the block into a column based data model (matrix of strings)
        final ArrayList<ArrayList<String>> data = toStringMatrix(src, bufferLen);
        final int rowcnt = data.get(0).size();
        if (allowAddExtTable) {
            // first columns are assumed to be chromosome and position, which will not be externalized into lookup map, all others are candidate
            for (int i = 2; i < data.size(); i++) {
                if (!mapExternalTables.containsKey(i)) { // If already added, do nothing
                    mapExternalTables.put(i, new HashMap<>());
                }
            }
        }

        // Analyze each column and choose the best storage format
        final ColEncoder[] encoders = chooseEncoders(data, rowcnt, mapExternalTables, allowAddExtTable, extTableSize);

        // Encode the block into the destination buffer according to choosen formats
        ByteArray.writeUnsignedShort(dest, 0, ByteOrder.BIG_ENDIAN, rowcnt); // Row count
        int pos = 2;
        for (ColEncoder encoder : encoders) {
            dest[pos++] = encoder.typeId; // column encoder type
        }
        dest[pos++] = 0; // Zero termination
        for (ColEncoder encoder : encoders) {
            pos += encoder.encode(dest, pos); // encoded column data
        }
        return pos;
    }

    /**
     * Convert an external lookup map into a byte representation, as folows
     * COLCNT | { COLIDXDIFF | MAPCNT | TEXTLIST }
     * COLCNT is the number of columns as unsigned short
     * COLIDXDIFF is the index of the column as difference to previous index in the list, as unsigned byte
     * MAPCNT is the number of elements in the map as unsigned short
     * TEXTLIST is an list of zero terminated text strings, as many as MAPCNT specifies.
     *
     * @param map The map to convert
     * @return The bytes representing the map
     */
    public static byte[] bytesFromLookupMap(Map<Integer, Map<String, Integer>> map) {
        final byte[] bytes = new byte[64 * 1024];
        int lastColIdx = 0;
        int cnt = 0;
        int pos = 2;
        for (Map.Entry<Integer, Map<String, Integer>> entry : map.entrySet()) {
            final Map<String, Integer> colMap = entry.getValue();
            if (colMap.size() > 0) {
                // Write colidx diff and map size
                final int colidx = entry.getKey();
                int colidxdiff = colidx - lastColIdx;
                if (colidxdiff > 255) {
                    throw new GorSystemException("Cant zip file. Cannot externalize the lookup map, column difference cannot be represented by byte", null);
                }
                lastColIdx = colidx;
                bytes[pos++] = (byte) colidxdiff;
                if (colMap.size() >= 256 * 256) {
                    throw new GorSystemException("Cant zip file. Cannot externalize the lookup map, to many entries in the map, i.e. " + colMap.size() + " >= " + (256 * 256), null);
                }
                ByteArray.writeUnsignedShort(bytes, pos, ByteOrder.BIG_ENDIAN, colMap.size());
                pos += 2;

                // Write text out in key order
                String[] keys = new String[colMap.size()];
                for (Map.Entry<String, Integer> lookup : colMap.entrySet()) {
                    keys[lookup.getValue()] = lookup.getKey();
                }
                for (String key : keys) {
                    pos += copyToBuffer(key, bytes, pos);
                }
                cnt++; // Found a column that will be included
            }
        }
        // Can only write the size in the end since empty lookup maps will be skipped
        ByteArray.writeUnsignedShort(bytes, 0, ByteOrder.BIG_ENDIAN, cnt);

        final byte[] out = new byte[pos];
        System.arraycopy(bytes, 0, out, 0, out.length);
        return out;
    }

    /**
     * Popuplate a lookup map with the lookup tables in the byte representation
     *
     * @param mapExtTable The lookup map to be filled
     * @param bytes       The bytes to populate from, see bytesFromLookupMap for format
     */
    static void lookupMapFromBytes(Map<Integer, Map<Integer, byte[]>> mapExtTable, byte[] bytes) {
        assert bytes.length >= 2;
        final int colcnt = ByteArray.readUnsignedShortBigEndian(bytes, 0);
        int pos = 2;
        int colIdx = 0;

        for (int i = 0; i < colcnt; i++) {
            final int colidxdiff = ByteArray.readUnsignedByte(bytes, pos++);
            colIdx += colidxdiff;
            final int mapcnt = ByteArray.readUnsignedShortBigEndian(bytes, pos);
            pos += 2;
            final HashMap<Integer, byte[]> map = new HashMap<>();
            mapExtTable.put(colIdx, map);

            for (int j = 0; j < mapcnt; j++) {
                final int begin = pos;
                while (bytes[pos] != 0) {
                    pos++; /* find zero termination */
                }
                byte[] text = new byte[pos - begin];
                System.arraycopy(bytes, begin, text, 0, text.length);
                pos++;
                map.put(j, text);
            }
        }
    }


    /**
     * Zip all contents of the specified buffer and write into a new buffer
     *
     * @param bytes The bytes to zip
     * @return The zipped content
     * @throws IOException
     */
    public static byte[] zip(byte[] bytes) throws IOException {
        return zip(bytes, 0, bytes.length);
    }

    /**
     * Zip the specified contents of the specified buffer and write into a new buffer
     *
     * @param bytes  The bytes to zip
     * @param off    The offset into the buffer to start reading
     * @param length The number of bytes to include in the zip
     * @return The zipped content
     * @throws IOException
     */
    public static byte[] zip(byte[] bytes, int off, int length) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream(bytes.length);
        final Deflater d = new Deflater(9, false);
        final DeflaterOutputStream zipOutput = new DeflaterOutputStream(output, d, 64 * 1024);

        zipOutput.write(bytes, off, length);
        zipOutput.close();
        return output.toByteArray();
    }

    /**
     * Copy string value to buffer.
     *
     * @param value  value to copy to buffer
     * @param buffer buffer to copy to.
     * @param pos    position to copy to.
     * @return size of inserted text.
     */
    private static int copyToBuffer(String value, byte[] buffer, int pos) {
        int size = 0;
        if (value != null) {
            // \0 is used as delimiter so we replace any that have been accidentally put into the file.
            final byte[] text = value.replace("\u0000", "").getBytes();
            System.arraycopy(text, 0, buffer, pos, text.length);
            size = text.length;
        }
        buffer[pos + size++] = 0;
        return size;
    }

    private static ColEncoder[] chooseEncoders(final ArrayList<ArrayList<String>> data, final int rowcnt, Map<Integer, Map<String, Integer>> mapExternalTables, boolean allowAddExtTable, int extTableSize) {
        // Start by analyzing column types, i.e. string vs long vs double, non null columns vs contains null
        final ColType[] types = analyzeTypes(data, rowcnt);
        final ColEncoder[] encoders = new ColEncoder[types.length];
        for (int i = 0; i < types.length; i++) {
            int extTableUsed = 0;
            if (allowAddExtTable) {
                for (Map<String, Integer> m : mapExternalTables.values()) {
                    extTableUsed += 2; // Assume two bytes for each column in map for size
                    for (String k : m.keySet()) {
                        extTableUsed += k.length() + 1; // The key size plus zero termination
                    }
                }
            }
            encoders[i] = types[i].createEncoder(mapExternalTables.get(i), allowAddExtTable, extTableSize - extTableUsed);
        }
        return encoders;
    }

    private static ColType[] analyzeTypes(ArrayList<ArrayList<String>> data, int rowcnt) {
        final int[] types = new int[data.size()];
        Object[][] values = new Object[data.size()][rowcnt];
        Arrays.fill(types, 1);
        // Types: 1->long no null, 2->long with null, 3->double no null, 4->double with null, 5->text, 6->empty
        for (int i = 0; i < types.length; i++) {
            Number[] column = new Number[rowcnt];
            values[i] = column;
            final ArrayList<String> list = data.get(i);
            boolean isEmpty = true;
            for (int j = 0; j < list.size(); j++) {
                final String value = list.get(j);
                if (types[i] <= 2) {
                    if (value == null) {
                        types[i] = 2;
                        column[j] = null;
                    } else {
                        isEmpty = false;
                        try {
                            column[j] = Long.parseLong(value);
                            continue;
                        } catch (NumberFormatException ex) {
                            types[i] += 2; // Convert to double, 1->3, 2->4
                            // Will continue with floating point convertion below
                        }
                    }
                }

                if (value == null) {
                    types[i] = 4;
                    column[j] = null;
                } else {
                    isEmpty = false;
                    try {
                        column[j] = Double.parseDouble(value);
                    } catch (NumberFormatException nex) {
                        types[i] = 5; // This is simply text
                        break;
                    }
                }
            }

            if (isEmpty) { // Mark empty columns specially, allow for very efficient encoding
                types[i] = 6;
            }
        }

        final ColType[] res = new ColType[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == 5) {
                res[i] = new TextColType(data.get(i));
            } else if (types[i] == 6) {
                res[i] = new EmptyColType();
            } else if (types[i] > 2) {
                res[i] = new DoubleColType((Number[]) values[i], data.get(i), types[i] == 4);
            } else {
                res[i] = new LongColType((Number[]) values[i], data.get(i), types[i] == 2);
            }
        }
        return res;
    }

    private static ArrayList<ArrayList<String>> toStringMatrix(byte[] src, int length) {
        final ArrayList<ArrayList<String>> data = new ArrayList<>();
        for (int begin = 0; begin < length; ) {
            int col = 0, end = begin;
            boolean eol = false;
            while (!eol) {
                while (end < length && src[end] != '\t' && src[end] != '\n') {
                    end++;
                }
                if (data.size() == col) {
                    data.add(new ArrayList<>());
                }
                final int l = end - begin;
                data.get(col++).add(l > 0 ? new String(src, begin, l) : null);
                if (end >= length || src[end] == '\n') {
                    eol = true;
                    if (end + 1 < length && src[end + 1] == '\r') { // Ignore DOS form feeds
                        end++;
                    }
                    begin = end + 1;
                } else {
                    begin = end + 1;
                    end = begin;
                }
            }
        }
        return data;
    }

    private static RowDecoder getDecoder(final byte[] src, int pos, final int type, final int rowcnt, final Map<Integer, byte[]> mapColExternalTable) {
        switch (type) {
            case 3: // Int offset, i.e. minimum value is written as long and the rest of the values as value-min (which is guaranteed to fit in 32 bits)
                final long minvalue3 = ByteArray.readLongBigEndian(src, pos);
                final int start3 = pos + 8;
                return new RowDecoder() {
                    int srcpos = start3;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        final long value = minvalue3 + ByteArray.readUnsignedIntBigEndian(src, srcpos);
                        srcpos += 4;
                        return ByteTextBuilder.writeLong(bytes, offset, value);
                    }

                    @Override
                    final int getReadLen() {
                        return 8 + (4 * rowcnt); // Long base value + byte per row
                    }
                };

            case 4: // Short offset, i.e. minimum value is written as long and the rest of the values as value-min (which is guaranteed to fit in 16 bits)
                final long minvalue4 = ByteArray.readLongBigEndian(src, pos);
                final int start4 = pos + 8;
                return new RowDecoder() {
                    int srcpos = start4;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        final long value = minvalue4 + ByteArray.readUnsignedShortBigEndian(src, srcpos);
                        srcpos += 2;
                        return ByteTextBuilder.writeLong(bytes, offset, value);
                    }

                    @Override
                    final int getReadLen() {
                        return 8 + (2 * rowcnt); // Long base value + byte per row
                    }
                };

            case 5: // Byte offset, i.e. minimum value is written as long and the rest of the values as value-min (which is guaranteed to fit in 8 bits)
                final long minvalue5 = ByteArray.readLongBigEndian(src, pos);
                final int start5 = pos + 8;
                return new RowDecoder() {
                    int srcpos = start5;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        final long value = minvalue5 + ByteArray.readUnsignedByte(src, srcpos++);
                        return ByteTextBuilder.writeLong(bytes, offset, value);
                    }

                    @Override
                    final int getReadLen() {
                        return 8 + rowcnt; // Long base value + byte per row
                    }
                };

            case 7: // Incremental values, i.e. a basevalue and the step size to generate the value of each row
                final long basevalue = ByteArray.readLongBigEndian(src, pos);
                final int incr = ByteArray.readIntBigEndian(src, pos + 8);
                return new RowDecoder() {
                    private long next = basevalue;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        next += incr;
                        return ByteTextBuilder.writeLong(bytes, offset, next);
                    }

                    @Override
                    final int getReadLen() {
                        return 8 + 4; // Long base value + int increment
                    }
                };

            case 9: // Byte diff, i.e. first value is written as long and the rest of the values as value-last_value (which is guaranteed to fit in 8 bits)
                final long firstvalue9 = ByteArray.readLongBigEndian(src, pos);
                final int start9 = pos + 8;
                return new RowDecoder() {
                    int srcpos = start9;
                    long lastvalue = firstvalue9;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        lastvalue += ByteArray.readUnsignedByte(src, srcpos++);
                        return ByteTextBuilder.writeLong(bytes, offset, lastvalue);
                    }

                    @Override
                    final int getReadLen() {
                        return 8 + rowcnt; // Long base value + byte per row
                    }
                };

            case 10: // Short diff, i.e. first value is written as long and the rest of the values as value-last_value (which is guaranteed to fit in 16 bits)
                final long firstvalue10 = ByteArray.readLongBigEndian(src, pos);
                final int start10 = pos + 8;
                return new RowDecoder() {
                    int srcpos = start10;
                    long lastvalue = firstvalue10;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        lastvalue += ByteArray.readUnsignedShortBigEndian(src, srcpos);
                        srcpos += 2;
                        return ByteTextBuilder.writeLong(bytes, offset, lastvalue);
                    }

                    @Override
                    final int getReadLen() {
                        return 8 + (2 * rowcnt); // Long base value + short per row
                    }
                };

            case 11: // Int diff, i.e. first value is written as long and the rest of the values as value-last_value (which is guaranteed to fit in 32 bits)
                final long firstvalue11 = ByteArray.readLongBigEndian(src, pos);
                final int start11 = pos + 8;
                return new RowDecoder() {
                    int srcpos = start11;
                    long lastvalue = firstvalue11;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        lastvalue += ByteArray.readUnsignedIntBigEndian(src, srcpos);
                        srcpos += 4;
                        return ByteTextBuilder.writeLong(bytes, offset, lastvalue);
                    }

                    @Override
                    final int getReadLen() {
                        return 8 + (4 * rowcnt); // Long base value + short per row
                    }
                };

            case 12: // Byte diff, i.e. first value is written as long and the rest of the values as value-last_value (which is guaranteed to fit in 8 bits)
                final long firstvalue12 = ByteArray.readLongBigEndian(src, pos);
                final int start12 = pos + 8;
                return new RowDecoder() {
                    int srcpos = start12;
                    long lastvalue = firstvalue12;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        lastvalue += src[srcpos++];
                        return ByteTextBuilder.writeLong(bytes, offset, lastvalue);
                    }

                    @Override
                    final int getReadLen() {
                        return 8 + rowcnt; // Long base value + byte per row
                    }
                };

            case 13: // Short diff, i.e. first value is written as long and the rest of the values as value-last_value (which is guaranteed to fit in 16 bits)
                final long firstvalue13 = ByteArray.readLongBigEndian(src, pos);
                final int start13 = pos + 8;
                return new RowDecoder() {
                    int srcpos = start13;
                    long lastvalue = firstvalue13;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        lastvalue += ByteArray.readShortBigEndian(src, srcpos);
                        srcpos += 2;
                        return ByteTextBuilder.writeLong(bytes, offset, lastvalue);
                    }

                    @Override
                    final int getReadLen() {
                        return 8 + (2 * rowcnt); // Long base value + short per row
                    }
                };

            case 19: // Empty column
                return new RowDecoder() {
                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        return 0;
                    }

                    @Override
                    final int getReadLen() {
                        return 0; // No data written for empty columns
                    }
                };

            case 21: // Varchar sequence, i.e. sequence of zero terminated string, one per row
                final int readlen = ByteArray.readIntBigEndian(src, pos);
                final int start21 = pos + 4;
                return new RowDecoder() {
                    int srcpos = start21;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        int begin = srcpos;
                        while (src[srcpos] != 0) {
                            srcpos++; /* find zero termination */
                        }
                        final int length = (srcpos - begin);
                        System.arraycopy(src, begin, bytes, offset, length);
                        srcpos++;
                        return length;
                    }

                    @Override
                    final int getReadLen() {
                        return readlen + 4; // Number of bytes for varchars plus the byte counter
                    }
                };

            case 22: // Text lookup, i.e. a sequence of zero terminated strings forming a lookup table, followed by byte ref per row
                final int start22 = pos;
                final int cnt = ByteArray.readUnsignedByte(src, pos++);
                final int[] begins = new int[cnt];
                final int[] lengths = new int[cnt];
                for (int i = 0; i < cnt; i++) {
                    begins[i] = pos;
                    while (src[pos] != 0) {
                        pos++; /* find zero termination */
                    }
                    lengths[i] = (pos - begins[i]);
                    pos++; // Increment past zero termination
                }
                final int refbegin = pos;

                return new RowDecoder() {
                    private int next = refbegin;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        final int ref = ByteArray.readUnsignedByte(src, next++);
                        System.arraycopy(src, begins[ref], bytes, offset, lengths[ref]);
                        return lengths[ref];
                    }

                    @Override
                    final int getReadLen() {
                        return (refbegin - start22) + rowcnt; // table length + byte per entry
                    }
                };

            case 23: // Sametext, i.e. constant text for each row
                final int begin = pos;
                while (src[pos] != 0) {
                    pos++; /* find zero termination */
                }
                final int length = pos - begin;
                return new RowDecoder() {
                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        System.arraycopy(src, begin, bytes, offset, length);
                        return length;
                    }

                    @Override
                    final int getReadLen() {
                        return length + 1; // the constant length plus the terminating zero
                    }
                };

            case 24: // External table, i.e. byte ref per row, refering to an externally stored table
                final int refbegin24 = pos;
                return new RowDecoder() {
                    private int next = refbegin24;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        final int ref = ByteArray.readUnsignedByte(src, next++);
                        final byte[] text = mapColExternalTable.get(ref);
                        if (text == null) return 0;
                        System.arraycopy(text, 0, bytes, offset, text.length);
                        return text.length;
                    }

                    @Override
                    final int getReadLen() {
                        return rowcnt; // byte per entry
                    }
                };

            case 25: // External table, i.e. byte ref per row, diff from last value refering to an externally stored table
                final int refbegin25 = pos;
                return new RowDecoder() {
                    private int lastval = 0;
                    private int next = refbegin25;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        final int diff = src[next++];
                        lastval += diff;
                        final byte[] text = mapColExternalTable.get(lastval);
                        if (text == null) return 0;
                        System.arraycopy(text, 0, bytes, offset, text.length);
                        return text.length;
                    }

                    @Override
                    final int getReadLen() {
                        return rowcnt; // byte per entry
                    }
                };

            case 28: // Text lookup with diff keys, i.e. byte ref per row, diff from last value refering to an externally stored table
                final int start28 = pos;
                final int cnt28 = ByteArray.readUnsignedByte(src, pos++);
                final int[] begins28 = new int[cnt28];
                final int[] lengths28 = new int[cnt28];
                for (int i = 0; i < cnt28; i++) {
                    begins28[i] = pos;
                    while (src[pos] != 0) {
                        pos++; /* find zero termination */
                    }
                    lengths28[i] = (pos - begins28[i]);
                    pos++; // Increment past zero termination
                }

                final int refbegin28 = pos;
                return new RowDecoder() {
                    private int lastval = 0;
                    private int next = refbegin28;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        final int diff = src[next++];
                        lastval += diff;
                        System.arraycopy(src, begins28[lastval], bytes, offset, lengths28[lastval]);
                        return lengths28[lastval];
                    }

                    @Override
                    final int getReadLen() {
                        return (refbegin28 - start28) + rowcnt; // table length + byte per entry
                    }
                };

            case 29: // External table, i.e. short ref per row, diff from last value refering to an externally stored table
                final int refbegin29 = pos;
                return new RowDecoder() {
                    private int index = 0; // First value is the actual value, so by adding zero we get that value
                    private int next = refbegin29;

                    @Override
                    final int decodeNext(byte[] bytes, int offset) {
                        final int diff = ByteArray.readShortBigEndian(src, next);
                        next += 2;
                        index += diff; // Index will always be the sum of last index and the difference between them
                        final byte[] text = mapColExternalTable.get(index);
                        if (text != null) {
                            System.arraycopy(text, 0, bytes, offset, text.length);
                            return text.length;
                        }
                        return 0;
                    }

                    @Override
                    final int getReadLen() {
                        return rowcnt * 2; // short per entry
                    }
                };


            default:
                throw new GorSystemException("Unexpected data type " + type + " when decoding packed block", null);
        }
    }

    abstract static class ColEncoder {
        final byte typeId;

        ColEncoder(int typeId) {
            this.typeId = (byte) typeId;
        }

        abstract int encode(byte[] bytes, int offset);
    }

    /**
     * Allows reading the needed bytes for each row in the block
     */
    abstract static class RowDecoder {
        // Get the number of source byte consumed by the row reader
        abstract int getReadLen();

        // Write the next row into the destination buffer, starting at the specified offset. Return number of bytes written.
        abstract int decodeNext(byte[] bytes, int offset);
    }

    abstract static class ColType {
        static final long MAX_UINT = 256 * 256 * 256 * 256L - 1;
        final boolean hasNull;

        ColType(boolean hasNull) {
            this.hasNull = hasNull;
        }

        abstract ColEncoder createEncoder(Map<String, Integer> mapColExternalTable, boolean allowAddExtTable, int extTableFreeSpace);

        ColEncoder createVarcharEncoder(final ArrayList<String> data) {
            return new ColEncoder(21) {
                @Override
                int encode(byte[] bytes, int offset) {
                    int pos = offset + 4; // save space for total length
                    for (String value : data) {
                        pos += copyToBuffer(value, bytes, pos);
                    }

                    // We know the total length, so write it now at the beginning of the buffer
                    ByteArray.writeInt(bytes, offset, ByteOrder.BIG_ENDIAN, (pos - offset) - 4);
                    return pos - offset;
                }
            };
        }

        static ColEncoder createEmptyEncoder() {
            return new ColEncoder(19) {
                @Override
                int encode(byte[] bytes, int offset) {
                    return 0;
                }
            };
        }
    }

    static class EmptyColType extends ColType {
        EmptyColType() {
            super(true);
        }

        @Override
        ColEncoder createEncoder(Map<String, Integer> mapColExternalTable, boolean allowAddExtTable, int extTableFreeSpace) {
            return ColType.createEmptyEncoder();
        }
    }

    static class TextColType extends ColType {
        final ArrayList<String> data;

        TextColType(ArrayList<String> data) {
            super(true);
            this.data = data;
        }

        int encodeLookupMapInColumn(final String[] keys, byte[] bytes, int offset) {
            int pos = offset;
            bytes[pos++] = (byte) keys.length;
            for (String key : keys) {
                pos += copyToBuffer(key, bytes, pos);
            }
            return pos;
        }

        boolean hasExtTableSpaceFor(Set<String> set, int freeSpace) {
            int size = 0;
            for (String key : set) { // Add all new keys to the map, with a new lookup key created
                size += key.length() + 1; // text + zero termination
            }

            return size < freeSpace;
        }

        @Override
        ColEncoder createEncoder(final Map<String, Integer> mapColExternalTable, boolean allowAddExtTable, int extTableFreeSpace) {
            // Check cardinality and choose fitting encoder if low cardinality
            final LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
            int tableSize = 0;
            int dataSize = 0;
            final int MAX_MAP_SIZE = 1000; // Should find a good value for this constant
            for (String value : data) {
                final String v = value != null ? value : "";
                if (!map.containsKey(v)) {
                    if (map.size() > MAX_MAP_SIZE) {
                        break; // No need to continue, we know that a lookup table will not be used
                    }
                    map.put(v, map.size());
                    tableSize += v.length() + 1; // include zero termination in size count
                }
                dataSize += v.length();
            }

            if (map.size() < MAX_MAP_SIZE) { // Try lookup table
                if (map.size() == 1) { // Single constant value
                    return new ColEncoder(23) {
                        final String constant = Util.nvl(data.get(0).replace("\u0000", ""), "");

                        @Override
                        int encode(byte[] bytes, int offset) {
                            System.arraycopy(constant.getBytes(), 0, bytes, offset, constant.length());
                            bytes[offset + constant.length()] = 0;
                            return constant.length() + 1;
                        }
                    };
                }

                if (dataSize > tableSize) { // only use lookup if it will compress the data
                    if (mapColExternalTable != null) { // Has an external lookup table
                        final LinkedHashSet<String> set = new LinkedHashSet<>(map.keySet()); // Let insertion order define add order for external map
                        set.removeAll(mapColExternalTable.keySet()); // Create a subset with only new additions
                        if (set.isEmpty() || (allowAddExtTable && hasExtTableSpaceFor(set, extTableFreeSpace))) { // Can use the external table
                            for (String key : set) { // Add all new keys to the map, with a new lookup key created
                                mapColExternalTable.put(key, mapColExternalTable.size());
                            }

                            if (mapColExternalTable.size() < 128) {
                                return new ColEncoder(25) {
                                    @Override
                                    int encode(byte[] bytes, int offset) {
                                        int pos = offset;
                                        // Encode the values as the difference in corrsponding lookup key to the external map
                                        byte lastval = mapColExternalTable.get(Util.nvl(data.get(0), "")).byteValue();
                                        bytes[pos++] = lastval;
                                        for (int i = 1; i < data.size(); i++) {
                                            final byte curval = mapColExternalTable.get(Util.nvl(data.get(i), "")).byteValue();
                                            bytes[pos++] = (byte) (curval - lastval);
                                            lastval = curval;
                                        }
                                        return pos - offset;
                                    }
                                };
                            }

                            assert mapColExternalTable.size() < 256 * 256;

                            return new ColEncoder(29) {
                                @Override
                                int encode(byte[] bytes, int offset) {
                                    int pos = offset;
                                    // Encode the values as the difference in corrsponding lookup key to the external map, as unsigned short
                                    int lastval = mapColExternalTable.get(Util.nvl(data.get(0), "")).intValue();
                                    ByteArray.writeUnsignedShort(bytes, pos, ByteOrder.BIG_ENDIAN, lastval);
                                    pos += 2;
                                    for (int i = 1; i < data.size(); i++) {
                                        final int curval = mapColExternalTable.get(Util.nvl(data.get(i), "")).intValue();
                                        ByteArray.writeShort(bytes, pos, ByteOrder.BIG_ENDIAN, (short) (curval - lastval));
                                        pos += 2;
                                        lastval = curval;
                                    }
                                    return pos - offset;
                                }
                            };

                        }
                    }

                    // Create encoder for string lookup table
                    // The order of keys can affect zipping (applied after encoding). Repeating pattern is best for zipping
                    // Try to order keys in such away that they will create a repeating pattern. This is the same as insertion
                    // order into the hash map, hence the use of LinkedHashSet
                    final String[] keys = new String[map.size()];
                    map.keySet().toArray(keys);
                    if (map.size() < 128) {
                        return new ColEncoder(28) {
                            @Override
                            int encode(byte[] bytes, int offset) {
                                int pos = encodeLookupMapInColumn(keys, bytes, offset);

                                // Encode the values as the difference in corrsponding lookup key to the external map
                                byte lastval = map.get(Util.nvl(data.get(0), "")).byteValue();
                                bytes[pos++] = lastval;
                                for (int i = 1; i < data.size(); i++) {
                                    final byte curval = map.get(Util.nvl(data.get(i), "")).byteValue();
                                    bytes[pos++] = (byte) (curval - lastval);
                                    lastval = curval;
                                }

                                return pos - offset;
                            }
                        };
                    }

                    if (map.size() < 256) {
                        return new ColEncoder(22) {
                            @Override
                            int encode(byte[] bytes, int offset) {
                                int pos = encodeLookupMapInColumn(keys, bytes, offset);

                                // Encode the values as the corrsponding lookup key
                                for (String value : data) {
                                    bytes[pos++] = map.get(Util.nvl(value, "")).byteValue();
                                }

                                return pos - offset;
                            }
                        };
                    }
                }
            }

            // Here are some possibilities, i.e. fixed len strings, similar/sub strings, key-value pairs, rs-names, and more
            ColEncoder encoder = handleNumericNames();

            // As a fallback write the values as zero terminated varchars (most general purpose encoding, least efficient in size)
            return encoder != null ? encoder : createVarcharEncoder(data);
        }

        private ColEncoder handleNumericNames() {
            // rsnames, chr-pos combinations, various namings are of the following pattern: PREFIX | ZERO-PAD | INTEGER
            // The prefix and zero pad can be stored as a constant and the integer can be differential encoded for optimal zipping
            // in some cases the integer part can even be stored as stepping constant
            return null;
        }
    }

    static class LongColType extends ColType {
        final Number[] data;
        final ArrayList<String> org;
        private boolean hasSmallDiff = true;
        private long max = Long.MIN_VALUE;
        private long min = Long.MAX_VALUE;
        private long maxNegDiff = 0; // Maximum negative difference of two consequetive numbers, i.e. most negative difference
        private long maxPosDiff = 0; // Maximum positive difference of two consequetive numbers
        private final long[] keys = new long[257];
        private final GLongHashMap distincts = new GLongHashMap();

        LongColType(Number[] data, ArrayList<String> org, boolean hasNull) {
            super(hasNull);
            this.data = data;
            this.org = org;
        }

        @Override
        ColEncoder createEncoder(Map<String, Integer> mapColExternalTable, boolean allowAddExtTable, int extTableFreeSpace) {
            if (!hasNull) {
                if (data.length > 2) { // Check if data is of fixed increment, note that increment of zero (i.e. constant) is included
                    long incr = data[1].longValue() - data[0].longValue();
                    for (int i = 2; i < data.length; i++) {
                        if (data[i].longValue() - data[i - 1].longValue() != incr) {
                            incr = Long.MAX_VALUE;
                            break;
                        }
                    }
                    if (incr < Integer.MAX_VALUE) { // Is there a increment pattern and can we encode it with int step size
                        return createIncrEncoder(data[0].longValue() - incr, (int) incr);
                    }
                }

                // How many bits are required to represent the data
                analyze();
                final long range = max - min; // The numerical range in the column, use it to decide number of bits required for data
                if (hasSmallDiff && maxNegDiff == 0 && maxPosDiff < 256) {
                    return createUnsignedByteDiffEncoder();
                } else if (hasSmallDiff && maxNegDiff >= Byte.MIN_VALUE && maxPosDiff <= Byte.MAX_VALUE) {
                    return createSignedByteDiffEncoder();
                } else if (range < 256) { // use byte
                    return createByteEncoder();
                } else if (hasSmallDiff && maxNegDiff == 0 && maxPosDiff < 256 * 256) {
                    return createUnsignedShortDiffEncoder();
                } else if (hasSmallDiff && maxNegDiff >= Short.MIN_VALUE && maxPosDiff <= Short.MAX_VALUE) {
                    return createSignedShortDiffEncoder();
                } else if (range < 256 * 256) { // use 2 bytes
                    // Should we check for lookup table? Might be better
                    return createShortEncoder();
                } else if (hasSmallDiff && maxNegDiff == 0 && maxPosDiff <= MAX_UINT) {
                    return createUnsignedIntDiffEncoder();
                } else if (range <= MAX_UINT) { // use 4 bytes
                    if (isLookupUsable()) { // Use a lookup table with byte values

                    }
                    return createIntEncoder();
                } else { // use 8 bytes
                    if (isLookupUsable()) { // Use a lookup table with byte values

                    }
                }
            } /*else {

            }*/
            return createVarcharEncoder(org);
        }

        ColEncoder createByteEncoder() {
            return new ColEncoder(5) {
                @Override
                int encode(byte[] bytes, int offset) {
                    ByteArray.writeLong(bytes, offset, ByteOrder.BIG_ENDIAN, min);
                    for (int i = 0; i < data.length; i++) {
                        bytes[offset + 8 + i] = (byte) (data[i].longValue() - min);
                    }
                    return 8 + data.length; // basevalue as long + diff as byte
                }
            };
        }

        ColEncoder createUnsignedByteDiffEncoder() {
            return new ColEncoder(9) {
                @Override
                int encode(byte[] bytes, int offset) {
                    ByteArray.writeLong(bytes, offset, ByteOrder.BIG_ENDIAN, data[0].longValue());
                    bytes[offset + 8] = (byte) 0;
                    for (int i = 1; i < data.length; i++) {
                        bytes[offset + 8 + i] = (byte) (data[i].longValue() - data[i - 1].longValue());
                    }
                    return 8 + data.length; // basevalue as long + diff as byte
                }
            };
        }

        ColEncoder createSignedByteDiffEncoder() {
            return new ColEncoder(12) {
                @Override
                int encode(byte[] bytes, int offset) {
                    ByteArray.writeLong(bytes, offset, ByteOrder.BIG_ENDIAN, data[0].longValue());
                    bytes[offset + 8] = (byte) 0;
                    for (int i = 1; i < data.length; i++) {
                        bytes[offset + 8 + i] = (byte) (data[i].longValue() - data[i - 1].longValue());
                    }
                    return 8 + data.length; // basevalue as long + diff as byte
                }
            };
        }


        ColEncoder createUnsignedShortDiffEncoder() {
            return new ColEncoder(10) {
                @Override
                int encode(byte[] bytes, int offset) {
                    ByteArray.writeLong(bytes, offset, ByteOrder.BIG_ENDIAN, data[0].longValue());
                    ByteArray.writeUnsignedShort(bytes, offset + 8, ByteOrder.BIG_ENDIAN, 0);
                    for (int i = 1; i < data.length; i++) {
                        ByteArray.writeUnsignedShort(bytes, offset + 8 + (2 * i), ByteOrder.BIG_ENDIAN, (int) (data[i].longValue() - data[i - 1].longValue()));
                    }
                    return 8 + (2 * data.length); // basevalue as long + diff as 16 bits
                }
            };
        }

        ColEncoder createSignedShortDiffEncoder() {
            return new ColEncoder(13) {
                @Override
                int encode(byte[] bytes, int offset) {
                    ByteArray.writeLong(bytes, offset, ByteOrder.BIG_ENDIAN, data[0].longValue());
                    ByteArray.writeUnsignedShort(bytes, offset + 8, ByteOrder.BIG_ENDIAN, 0);
                    for (int i = 1; i < data.length; i++) {
                        ByteArray.writeShort(bytes, offset + 8 + (2 * i), ByteOrder.BIG_ENDIAN, (short) (data[i].longValue() - data[i - 1].longValue()));
                    }
                    return 8 + (2 * data.length); // basevalue as long + diff as 16 bits
                }
            };
        }

        ColEncoder createUnsignedIntDiffEncoder() {
            return new ColEncoder(11) {
                @Override
                int encode(byte[] bytes, int offset) {
                    ByteArray.writeLong(bytes, offset, ByteOrder.BIG_ENDIAN, data[0].longValue());
                    ByteArray.writeUnsignedInt(bytes, offset + 8, ByteOrder.BIG_ENDIAN, 0);
                    for (int i = 1; i < data.length; i++) {
                        ByteArray.writeUnsignedInt(bytes, offset + 8 + (4 * i), ByteOrder.BIG_ENDIAN, (data[i].longValue() - data[i - 1].longValue()));
                    }
                    return 8 + (4 * data.length); // basevalue as long + diff as 32 bits
                }
            };
        }

        ColEncoder createShortEncoder() {
            return new ColEncoder(4) {
                @Override
                int encode(byte[] bytes, int offset) {
                    ByteArray.writeLong(bytes, offset, ByteOrder.BIG_ENDIAN, min);
                    for (int i = 0; i < data.length; i++) {
                        ByteArray.writeUnsignedShort(bytes, offset + 8 + (2 * i), ByteOrder.BIG_ENDIAN, (int) (data[i].longValue() - min));
                    }
                    return 8 + (2 * data.length); // basevalue as long + diff as short
                }
            };
        }

        ColEncoder createIntEncoder() {
            return new ColEncoder(3) {
                @Override
                int encode(byte[] bytes, int offset) {
                    ByteArray.writeLong(bytes, offset, ByteOrder.BIG_ENDIAN, min);
                    for (int i = 0; i < data.length; i++) {
                        ByteArray.writeUnsignedInt(bytes, offset + 8 + (4 * i), ByteOrder.BIG_ENDIAN, (data[i].longValue() - min));
                    }
                    return 8 + (4 * data.length); // basevalue as long + diff as short
                }
            };
        }


        ColEncoder createIncrEncoder(final long basevalue, final int increment) {
            return new ColEncoder(7) {
                @Override
                int encode(byte[] bytes, int offset) {
                    ByteArray.writeLong(bytes, offset, ByteOrder.BIG_ENDIAN, basevalue);
                    ByteArray.writeInt(bytes, offset + 8, ByteOrder.BIG_ENDIAN, increment);
                    return 8 + 4; // basevalue as long + incr as integer
                }
            };
        }

        ColEncoder createLookupEncoder() {
            return null;
        }

        void analyze() {
            for (int i = 0; i < data.length; i++) {
                final long value = data[i].longValue();
                if (distincts.size() <= 256 && !distincts.containsKey(value)) {
                    keys[distincts.size()] = value;
                    distincts.put(value, distincts.size());
                }
                if (min > value) {
                    min = value;
                }
                if (max < value) {
                    max = value;
                }

                // Find difference boundaries, i.e. lowest negative value and highest positive value
                if (i != 0 && hasSmallDiff) {
                    final double fdiff = data[i].doubleValue() - data[i - 1].doubleValue();
                    if (fdiff < Integer.MIN_VALUE || fdiff > Integer.MAX_VALUE) {
                        hasSmallDiff = false;
                    } else {
                        final long diff = data[i].longValue() - data[i - 1].longValue();
                        if (diff < 0) {
                            if (diff < maxNegDiff) {
                                maxNegDiff = diff;
                            }
                        } else if (diff > 0 && diff > maxPosDiff) {
                            maxPosDiff = diff;
                        }
                    }
                }
            }
        }

        boolean isLookupUsable() {
            return distincts.size() <= 256;
        }
    }

    static class DoubleColType extends ColType {
        final Number[] data;
        final ArrayList<String> org;

        DoubleColType(Number[] data, ArrayList<String> org, boolean hasNull) {
            super(hasNull);
            this.data = data;
            this.org = org;
        }

        @Override
        ColEncoder createEncoder(Map<String, Integer> mapColExternalTable, boolean allowAddExtTable, int extTableFreeSpace) {
            return createVarcharEncoder(org);
        }
    }
}
