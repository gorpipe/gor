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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.model.ChromoCache;
import org.gorpipe.gor.model.Row;

import java.util.Comparator;


/**
 * Two column key. Chromosome and base pair
 */
public class StringIntKey implements IKey {

    public final static int DEFAULT_CHR_COL = 0;

    public final static int DEFAULT_POS_COL = 1;

    /***/
    public String chr;
    /***/
    public int bpair;

    public int chrCol;

    public int posCol;

    /**
     * Compare positions based on lexicographic ordering
     */
    public static Comparator<StringIntKey> cmpLexico = (left, right) -> {
        if (left.chr.equals(right.chr)) {
            return Integer.compare(left.bpair, right.bpair);
        } else {
            return left.chr.compareTo(right.chr);
        }
    };

    /**
     * Compare positions based on human genome ordering, i.e. chrM, chr1, chr2, chr3, ..., chrX, chrXY, chrY
     */
    public static Comparator<StringIntKey> cmpHumanGenomeMitoFirst() {
        return customComparator(new ChromoCache());
    }

    public static Comparator<StringIntKey> customComparator(ChromoCache cache) {
        return (StringIntKey left, StringIntKey right) -> {
            if (left.chr.equals(right.chr)) {
                return Integer.compare(left.bpair, right.bpair);
            } else {
                // Use Ids in chromo cache to decide lexicographical ordering
                final Integer lId = cache.toId(left.chr);
                final Integer rId = cache.toId(right.chr);
                if (lId == null) return -1;
                if (rId == null) return 1;
                return lId != rId ? lId - rId : Integer.compare(left.bpair, right.bpair);
            }
        };
    }

    /**
     * The comparator to use for the key
     */
    public final Comparator<StringIntKey> comparator;


    /**
     * Creates a new key with default positions chrCol=0, posCol=1
     *
     * @param comparator The comparator to use when ordering
     */
    public StringIntKey(Comparator<StringIntKey> comparator) {
        this(DEFAULT_CHR_COL, DEFAULT_POS_COL, comparator);
    }

    /**
     * Creates a new key with no values
     *
     * @param chrCol     position of chr col
     * @param bpCol      position of base pair col
     * @param comparator The comparator to use when ordering
     */
    public StringIntKey(int chrCol, int bpCol, Comparator<StringIntKey> comparator) {
        this(chrCol, bpCol, null, -1, comparator);
    }

    /**
     * Creates a new key with chr and pos as values
     *
     * @param chrCol     position of chr col
     * @param bpCol      position of base pair col
     * @param chr
     * @param pos
     * @param comparator The comparator to use when ordering
     */
    public StringIntKey(int chrCol, int bpCol, String chr, Integer pos, Comparator<StringIntKey> comparator) {
        this.chrCol = chrCol;
        this.posCol = bpCol;
        this.comparator = comparator;
        this.chr = chr;
        if (pos == null) {
            this.bpair = -1;
        } else {
            this.bpair = pos;
        }
    }

    /**
     * Creates a new key with chr and pos as values
     *
     * @param chr
     * @param pos
     * @param comparator The comparator to use when ordering
     */
    public StringIntKey(String chr, Integer pos, Comparator<StringIntKey> comparator) {
        this.chrCol = DEFAULT_CHR_COL;
        this.posCol = DEFAULT_POS_COL;
        this.comparator = comparator;
        this.chr = chr;
        if (pos == null) {
            this.bpair = -1;
        } else {
            this.bpair = pos;
        }
    }

    /**
     * Creates a new key with chr and pos as values and human meto first comparator
     *
     * @param chr
     * @param pos
     */
    public StringIntKey(String chr, Integer pos) {
        this(chr, pos, cmpLexico);
    }

    /**
     * Creates a new key with values parsed from the buffer starting from beginOfLine
     *
     * @param chrCol
     * @param bpCol
     * @param buffer      data buffer containing the key
     * @param bufLength   The number of valid bytes in the buffer, i.e. the buffer length must be &le; buffer.length
     * @param beginOfLine start of the row in the buffer
     * @param comparator  The comparator to use when ordering
     */
    public StringIntKey(int chrCol, int bpCol, byte[] buffer, int bufLength, int beginOfLine, Comparator<StringIntKey> comparator) {
        this(chrCol, bpCol, comparator);
        if (buffer != null) {
            setValues(buffer, beginOfLine, bufLength);
        }
    }

    /**
     * Creates a new key with values parsed from the buffer starting from beginOfLine
     *
     * @param chrCol
     * @param bpCol
     * @param buffer      data buffer containing the key
     * @param bufLength   The number of valid bytes in the buffer, i.e. the buffer length must be &le; buffer.length
     * @param beginOfLine start of the row in the buffer
     * @param comparator  The comparator to use when ordering
     */
    public StringIntKey(int chrCol, int bpCol, String buffer, int bufLength, int beginOfLine, Comparator<StringIntKey> comparator) {
        this(chrCol, bpCol, comparator);
        if (buffer != null) {
            setValues(buffer, beginOfLine, bufLength);
        }
    }


    @Override
    public int compareTo(IKey otherkey) {
        return comparator.compare(this, (StringIntKey) otherkey);
    }

    @Override
    public boolean equals(Object key) {
        if (this == key) return true;
        if (!(key instanceof StringIntKey)) return false;
        return this.compareTo((StringIntKey) key) == 0;
    }

    @Override
    public <T extends IKey> float deriveCoefficient(T leftkey, T rightkey) {
        final StringIntKey left = (StringIntKey) leftkey;
        final StringIntKey right = (StringIntKey) rightkey;
        if (left.chr.equals(right.chr)) {
            // Assume relative distance on the single chromosome coordinate system is the same in the file positions
            // As this is a raw estimate don't, we don't go completely to the edges (going closer can lead to very
            // slow regression to the correct value)
            return Math.min(Math.max(((bpair - left.bpair) / (1.0f * (right.bpair - left.bpair))), 0.1f), 0.9f);
        }

        return 0.5f; // Assume 50% coefficient of binary search is best estimate if not on the same chromosome
    }


    @Override
    public byte[] getKey() {
        String result = chr + "\t" + bpair;
        return result.getBytes();
    }

    public void setValues(byte[] buffer, int offset, int upTo) {
        if (offset == upTo) {
          this.chr = "";
          this.bpair = -1;
        } else {
            final int chrCol = this.chrCol;
            final int posCol = this.posCol;

            int colIdx = 0;
            int bufferIdx = offset;
            int maxColIdx = Math.max(chrCol, posCol);
            do {
                if (colIdx == chrCol) {
                    final int beginIdx = bufferIdx;
                    while (bufferIdx < upTo && buffer[bufferIdx++] != '\t');
                    this.chr = new String(buffer, beginIdx, bufferIdx - beginIdx - 1);
                } else if (colIdx == posCol) {
                    int pos = 0;
                    byte b;
                    while (bufferIdx < upTo && (b = buffer[bufferIdx++]) != '\t' && b != '\n' && b != '\r') {
                        if (b > '9' || b < '0') {
                            throw new GorDataException("Cannot create key from " + new String(buffer, offset, Math.min(100, upTo - offset)));
                        }
                        pos = 10 * pos + (b - '0');
                    }
                    this.bpair = pos;
                } else {
                    while (buffer[bufferIdx++] != '\t');
                }
            } while (colIdx++ < maxColIdx);
        }
    }

    public void setValues(String buffer, int offset, int upTo) {
        if (offset == upTo) {
            this.chr = "";
            this.bpair = -1;
        } else {
            final int chrCol = this.chrCol;
            final int posCol = this.posCol;

            int colIdx = 0;
            int bufferIdx = offset;
            int maxColIdx = Math.max(chrCol, posCol);
            do {
                if (colIdx == chrCol) {
                    final int beginIdx = bufferIdx;
                    while (bufferIdx < upTo && buffer.charAt(bufferIdx++) != '\t');
                    this.chr = buffer.substring(beginIdx, bufferIdx - 1);
                } else if (colIdx == posCol) {
                    int pos = 0;
                    char b;
                    while (bufferIdx < upTo && (b = buffer.charAt(bufferIdx++)) != '\t' && b != '\n' && b != '\r') {
                        if (b > '9' || b < '0') {
                            throw new GorDataException("Cannot create key from " + buffer.substring(offset, Math.min(100+offset, upTo)));
                        }
                        pos = 10 * pos + (b - '0');
                    }
                    this.bpair = pos;
                } else {
                    while (buffer.charAt(bufferIdx++) != '\t');
                }
            } while (colIdx++ < maxColIdx);
        }
    }

    @Override
    public String toString() {
        return chr + ":" + bpair;
    }

    @Override
    public StringIntKey createKey(byte[] buffer, int bufLength, int beginOfLine) {
        return new StringIntKey(chrCol, posCol, buffer, bufLength, beginOfLine, comparator);
    }

    @Override
    public StringIntKey createKey(Row row) {
        return new StringIntKey(row.chr, row.pos, comparator);
    }
}
