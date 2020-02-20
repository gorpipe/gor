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

package org.gorpipe.model.genome.files.gor;

import com.github.luben.zstd.ZstdInputStream;
import org.gorpipe.exceptions.GorResourceException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Driver for indexed BGen v1.3 files. See description at http://www.well.ox.ac.uk/~gav/bgen_format/
 *
 * @author Hjalti Thor Isleifsson
 */

public class IndexedBGenFileDriver {

    final static byte SNP_NOT_COMPRESSED = 0;
    final static byte SNP_ZLIB_COMPRESSED = 1;
    final static byte SNP_ZSTD_COMPRESSED = 2;

    final static byte LAYOUT_TYPE_UNSPECIFIED = 0;
    final static byte LAYOUT_TYPE_ONE = 1;
    final static byte LAYOUT_TYPE_TWO = 2;

    private int sampleCount;
    private int variantDataBlockCount;
    private byte layoutType;
    private byte compressionType;

    byte[] buffer = new byte[1024];
    int idx; //The buffer idx.

    final private LineParser lineParser;

    final private int[] tagFilter;
    private boolean hasFilter;

    final private RandomAccessFile file;

    IndexedBGenFileDriver(String fileName) {
        this(fileName, null);
    }

    IndexedBGenFileDriver(String fileName, int[] tagFilter) {
        this.tagFilter = tagFilter;
        this.hasFilter = tagFilter != null && tagFilter.length != 0;
        try {
            this.file = new RandomAccessFile(fileName, "r");
        } catch (IOException e) {
            throw new GorResourceException(e.getMessage(), fileName, e);
        }
        parseHeaderBlock();
        if (layoutType == LAYOUT_TYPE_ONE) {
            lineParser = new TypeOneLineParser();
        } else if (layoutType == LAYOUT_TYPE_TWO) {
            lineParser = new TypeTwoLineParser();
        } else if (layoutType == LAYOUT_TYPE_UNSPECIFIED) {
            lineParser = new TypeTwoLineParser(); //Should review this later.
        } else {
            throw new GorResourceException("Unknown Layout type " + layoutType, fileName);
        }
    }

    private void parseHeaderBlock() {
        try {
            file.readFully(buffer, 0, 24);
            idx = 4;
            final int headerBlockLength = readInt();
            if (headerBlockLength > 20) { //This should happen on rare occasions
                buffer = ensureCapacity(buffer, headerBlockLength);
                file.readFully(buffer, 24, headerBlockLength - 24);
            }
            variantDataBlockCount = readInt();
            sampleCount = readInt();
            final byte tmp = buffer[headerBlockLength];
            compressionType = (byte) (tmp & 3);
            layoutType = (byte) ((tmp >>> 2) & 0xf);
        } catch (IOException e) {
            throw new GorResourceException("Could not read header block form bgen file.", this.file.toString(), e);
        }
    }

    boolean writeNextLine(Line line, long pos, int len) {
        buffer = ensureCapacity(buffer, len);
        try {
            file.seek(pos);
            file.readFully(buffer, 0, len);
            lineParser.parseLine(line);
        } catch (IOException | DataFormatException e) {
            throw new GorResourceException("Could not parse line from bgen file.", this.file.toString(), e);
        }
        return true;
    }


    int readInt() {
        return (buffer[idx++] & 0xff) | ((buffer[idx++] & 0xff) << 8) | ((buffer[idx++] & 0xff) << 16) | ((buffer[idx++] & 0xff) << 24);
    }

    static int readInt(byte[] array, int idx) {
        return (array[idx++] & 0xff) | ((array[idx++] & 0xff) << 8) | ((array[idx++] & 0xff) << 16) | ((array[idx] & 0xff) << 24);
    }

    short readShort() {
        return (short) ((buffer[idx++] & 0xff) | ((buffer[idx++] & 0xff) << 8));
    }

    static short readShort(byte[] array, int idx) {
        return (short) ((array[idx++] & 0xff) | ((array[idx] & 0xff) << 8));
    }

    static byte[] ensureCapacity(byte[] array, int len) {
        if (array.length < len) {
            int newLen = array.length;
            while ((newLen <<= 1) < len) ;
            return Arrays.copyOf(array, newLen);
        } else return array;
    }

    abstract class LineParser {
        byte[] uncompressedProbs = compressionType != SNP_NOT_COMPRESSED ? new byte[Math.max(6 * sampleCount, 1)] : null;

        abstract void parseLine(Line line) throws DataFormatException, IOException;

        void readChrAndPos(Line line) {
            final short chrLen = readShort();
            final byte[] chr;
            if (buffer[idx] == '0') {
                chr = new byte[3 + chrLen - 1];
                System.arraycopy(buffer, idx + 1, chr, 3, chrLen - 1);
            } else {
                chr = new byte[3 + chrLen];
                System.arraycopy(buffer, idx, chr, 3, chrLen);
            }
            chr[0] = 'c'; chr[1] = 'h'; chr[2] = 'r';
            line.chr = new String(chr);
            final int upTo = idx + chrLen;
            while (idx < upTo) {
                line.chrIdx = 10 * line.chrIdx + buffer[idx++] - '0';
            }
            line.pos = readInt();
        }
    }

    class TypeOneLineParser extends LineParser {
        final byte[] chars = new byte[sampleCount << 1];

        @Override
        void parseLine(Line line) throws DataFormatException {
            idx = 0;
            final int numberOfIndividualsInLine = readInt();
            if (numberOfIndividualsInLine != sampleCount) {
                throw new DataFormatException("Expecting " + numberOfIndividualsInLine + " but there should be " + sampleCount);
            }
            final short variantIdLen = readShort();
            line.cols[3].append(buffer, idx, variantIdLen); //VariantId is column 2
            idx += variantIdLen;
            final short rsIdLen = readShort();
            line.cols[2].append(buffer, idx, rsIdLen);
            idx += rsIdLen;

            readChrAndPos(line);

            //Read alleles. There are only two of them since layoutType = 1
            final int formerAlleleLen = readInt();
            line.cols[0].append(buffer, idx, formerAlleleLen);
            idx += formerAlleleLen;
            final int latterAlleleLen = readInt();
            line.cols[1].append(buffer, idx, latterAlleleLen);
            idx += latterAlleleLen;

            //Read probabilities
            final int uncompressedProbsIdx;
            if (compressionType == SNP_NOT_COMPRESSED) {
                //In this case the probabilities are not compressed an can be read directly from the stream
                uncompressedProbsIdx = idx;
                uncompressedProbs = buffer;
            } else if (compressionType == SNP_ZLIB_COMPRESSED) {
                //In this case the probabilities are compressed using zlib
                final int compressedProbLen = readInt();
                final Inflater inflater = new Inflater();
                inflater.setInput(buffer, idx, compressedProbLen);
                inflater.inflate(uncompressedProbs);
                uncompressedProbsIdx = 0;
            } else {
                //No other compression type is supported if layoutType = 1
                throw new DataFormatException("Compression type " + compressionType
                        + " is not supported with layout type " + layoutType);
            }

            //Parse probabilities
            int probIdx = uncompressedProbsIdx;
            int charIdx = 0;
            final float scale = 1.0f / 32768;
            float p0, p1, p2;
            if (hasFilter) {
                for (int i : tagFilter) {
                    probIdx = uncompressedProbsIdx + 6 * i;
                    p0 = ((uncompressedProbs[probIdx++] & 0xff) | ((uncompressedProbs[probIdx++] & 0xff) << 8)) * scale;
                    p1 = ((uncompressedProbs[probIdx++] & 0xff) | ((uncompressedProbs[probIdx++] & 0xff) << 8)) * scale;
                    p2 = ((uncompressedProbs[probIdx++] & 0xff) | ((uncompressedProbs[probIdx] & 0xff) << 8)) * scale;

                    if (p0 == 0f && p1 == 0f && p2 == 0f) {
                        chars[charIdx++] = ' ';
                        chars[charIdx++] = ' ';
                    } else {
                        chars[charIdx++] = (byte) Math.round((1.0f - p1) * 93 + 33);
                        chars[charIdx++] = (byte) Math.round((1.0f - p2) * 93 + 33);
                    }
                }
            } else {
                for (int i = 0; i < sampleCount; ++i) {
                    p0 = ((uncompressedProbs[probIdx++] & 0xff) | ((uncompressedProbs[probIdx++] & 0xff) << 8)) * scale;
                    p1 = ((uncompressedProbs[probIdx++] & 0xff) | ((uncompressedProbs[probIdx++] & 0xff) << 8)) * scale;
                    p2 = ((uncompressedProbs[probIdx++] & 0xff) | ((uncompressedProbs[probIdx++] & 0xff) << 8)) * scale;

                    if (p0 == 0f && p1 == 0f && p2 == 0f) {
                        chars[charIdx++] = ' '; chars[charIdx++] = ' ';
                    } else {
                        chars[charIdx++] = (byte) Math.round((1.0f - p1) * 93 + 33);
                        chars[charIdx++] = (byte) Math.round((1.0f - p2) * 93 + 33);
                    }
                }
            }
            line.cols[4].append(chars, 0, charIdx);
        }
    }

    class TypeTwoLineParser extends LineParser {
        byte[] chars = new byte[sampleCount];

        @Override
        void parseLine(Line line) throws IOException, DataFormatException {
            idx = 0;
            final short variantIdLen = readShort();
            line.cols[3].append(buffer, idx, variantIdLen);
            idx += variantIdLen;
            final short rsIdLen = readShort();
            line.cols[2].append(buffer,idx, rsIdLen);
            idx += rsIdLen;

            readChrAndPos(line);

            final short alleleCount = readShort();
            if (alleleCount < 0 || alleleCount > 2) {
                throw new DataFormatException("Illegal number of alleles: " + alleleCount + ". Number of alleles must be equal to 0, 1 or 2");
            }
            int alleleLen;
            for (int alleleIdx = 0; alleleIdx < alleleCount; ++alleleIdx) {
                alleleLen = readInt();
                line.cols[alleleIdx].append(buffer, idx, alleleLen);
                idx += alleleLen;
            }

            //Fetch probs
            final int leftInLine = readInt();
            int uncompressedProbsIdx;
            if (compressionType == SNP_NOT_COMPRESSED) {
                uncompressedProbsIdx = idx;
                uncompressedProbs = buffer;
            } else {
                uncompressedProbsIdx = 0;
                final int uncompressedProbsLen = readInt();
                final int compressedProbsLen = leftInLine - 4;
                uncompressedProbs = ensureCapacity(uncompressedProbs, uncompressedProbsLen);
                if (compressionType == SNP_ZLIB_COMPRESSED) {
                    final Inflater inflater = new Inflater();
                    inflater.setInput(buffer, idx, compressedProbsLen);
                    inflater.inflate(uncompressedProbs, 0, uncompressedProbsLen);
                } else if (compressionType == SNP_ZSTD_COMPRESSED){
                    final ZstdInputStream zstdInputStream = new ZstdInputStream(new ByteArrayInputStream(buffer, idx, compressedProbsLen));
                    zstdInputStream.read(uncompressedProbs, 0, uncompressedProbsLen);
                } else {
                    throw new DataFormatException("Unknown compression type " + compressionType);
                }
            }
            final int numberOfIndividualsInLine = readInt(uncompressedProbs, uncompressedProbsIdx);
            if (numberOfIndividualsInLine != sampleCount) {
                throw new DataFormatException("Expecting " + numberOfIndividualsInLine + " but there should be " + sampleCount);
            }
            uncompressedProbsIdx += 4;
            final short alleleCount2 = readShort(uncompressedProbs, uncompressedProbsIdx);
            uncompressedProbsIdx += 2;
            if (alleleCount != alleleCount2) {
                throw new DataFormatException("Expecting " + alleleCount2 + " but there should be " + alleleCount);
            }
            final byte minPloidy = uncompressedProbs[uncompressedProbsIdx++];
            final byte maxPloidy = uncompressedProbs[uncompressedProbsIdx++];

            if (minPloidy != maxPloidy || minPloidy != 2) {
                throw new DataFormatException("Ploidy must equal 2");
            }

            int ploidyIdx = uncompressedProbsIdx;
            boolean missing;
            int probIdx = numberOfIndividualsInLine + uncompressedProbsIdx;
            final byte phaseByte = uncompressedProbs[probIdx++];
            if (phaseByte != 0) {
                throw new DataFormatException("GOR does not support reading of .bgen files with phased haplotypes.");
            }
            final byte bits = uncompressedProbs[probIdx++];
            final float scale = 1.0f / ((1L << bits) - 1);

            final BitStream bitStream = new BitStream(bits, uncompressedProbs, probIdx);
            final int probCount = alleleCount;
            int charsIdx = 0;

            if (probCount == 0) {
                final int upTo = hasFilter ? tagFilter.length : sampleCount;
                chars = ensureCapacity(chars, upTo);
                while (charsIdx < upTo) {
                    chars[charsIdx++] = 33;
                }
            } else {
                if (hasFilter) {
                    chars = ensureCapacity(chars,tagFilter.length << 1);
                    if (probCount == 1) {
                        float tmp;
                        for (int i : tagFilter) {
                            ploidyIdx = uncompressedProbsIdx + i;
                            bitStream.seek(i);
                            missing = (uncompressedProbs[ploidyIdx] >> 7) == 1;
                            if (missing) {
                                chars[charsIdx++] = ' '; chars[charsIdx++] = ' ';
                            } else {
                                tmp = bitStream.next() * scale;
                                chars[charsIdx++] = (byte) (Math.round((1 - tmp) * 93) + 33);
                                chars[charsIdx++] = (byte) (Math.round(tmp * 93) + 33);
                            }
                        }
                    } else {
                        float p0, p1, p2;
                        for (int i : tagFilter) {
                            ploidyIdx = uncompressedProbsIdx + i;
                            bitStream.seek(i << 1);
                            missing = (uncompressedProbs[ploidyIdx] >> 7) == 1;
                            if (missing) {
                                chars[charsIdx++] = ' '; chars[charsIdx++] = ' ';
                            } else {
                                p0 = bitStream.next() * scale;
                                p1 = bitStream.next() * scale;
                                p2 = 1 - p0 - p1;
                                chars[charsIdx++] = (byte) (Math.round((1 - p1) * 93) + 33);
                                chars[charsIdx++] = (byte) (Math.round((1 - p2) * 93) + 33);
                            }
                        }
                    }
                } else {
                    chars = ensureCapacity(chars, sampleCount << 1);
                    if (probCount == 1) {
                        float tmp;
                        for (int i = 0; i < sampleCount; ++i) {
                            missing = (uncompressedProbs[ploidyIdx++] >> 7) == 1;
                            if (missing) {
                                bitStream.skip(1);
                                chars[charsIdx++] = ' '; chars[charsIdx++] = ' ';
                            } else {
                                tmp = bitStream.next() * scale;
                                chars[charsIdx++] = (byte) (Math.round((1 - tmp) * 93) + 33);
                                chars[charsIdx++] = (byte) (Math.round(tmp * 93) + 33);
                            }
                        }
                    } else {
                        float p0, p1, p2;
                        for (int i = 0, j = 0; i < sampleCount; ++i) {
                            missing = (uncompressedProbs[ploidyIdx++] >> 7) == 1;
                            if (missing) {
                                bitStream.skip(2);
                                chars[j++] = ' '; chars[j++] = ' ';
                            } else {
                                p0 = bitStream.next() * scale;
                                p1 = bitStream.next() * scale;
                                p2 = 1 - p0 - p1;
                                chars[charsIdx++] = (byte) (Math.round((1 - p1) * 93) + 33);
                                chars[charsIdx++] = (byte) (Math.round((1 - p2) * 93) + 33);
                            }
                        }
                    }
                }
            }
            line.cols[4].append(chars, 0, charsIdx);
        }
    }
}