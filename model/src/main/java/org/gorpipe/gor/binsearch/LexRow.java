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

import org.gorpipe.gor.model.Row;

/**
 * Row contains the row as a byte array and a key of long type
 */
public class LexRow implements IRow<LexRow> {
    /***/
    public final IRowSource<LexRow> source;
    /***/
    public byte[] line;
    /***/
    public StringIntKey key;

    /***/
    public LexRow() {
        source = null;
        key = null;
    }

    /**
     * @param chrCol
     * @param bpCol
     * @param line
     * @param bufLength
     * @param source
     */
    public LexRow(int chrCol, int bpCol, byte[] line, int beginOfLine, int bufLength, IRowSource<LexRow> source) {
        key = new StringIntKey(chrCol, bpCol, line, bufLength, beginOfLine, StringIntKey.cmpLexico);
        this.source = source;
        this.line = line;
    }

    /**
     * @param source
     */
    public LexRow(Row row, IRowSource<LexRow> source) {
        key = new StringIntKey(row.chr, row.pos);
        this.source = source;
        this.line = row.toString().getBytes();
    }

    /**
     * Creates a new row
     *
     * @param chrCol Position of the chr Column
     * @param bpCol  Position of the base pair column
     */
    public LexRow(int chrCol, int bpCol) {
        this(chrCol, bpCol, null,0,  0, null);
    }


    /**
     * Creates a new row with row data
     *
     * @param chrCol position of chr column
     * @param bpCol  position of the base pair column
     * @param line   The buffer containing data
     */
    public LexRow(int chrCol, int bpCol, byte[] line) {
        this(chrCol, bpCol, line, 0, line.length, null);
    }

    @Override
    public int compareTo(IKey row) {
        return key.compareTo(((LexRow) row).key);
    }

    /**
     * Returns true if this row is less than the other row
     *
     * @param r the Row to test
     * @return true if row is less than this row
     */
    public boolean isLessThan(LexRow r) {
        return compareTo(r) < 0;
    }

    @Override
    public byte[] getRow() {
        return line;
    }

    @Override
    public byte[] getKey() {
        return key.getKey();
    }

    @Override
    public IRowSource<LexRow> getSource() {
        return source;
    }

    @Override
    public LexRow createRow(IRowSource<LexRow> s, byte[] buffer, int beginOfLine) {
        return new LexRow(key.chrCol, key.posCol, buffer, beginOfLine, buffer.length, s);
    }

    @Override
    public LexRow createKey(byte[] buffer, int bufLength, int beginOfLine) {
        return new LexRow(key.chrCol, key.posCol, buffer, beginOfLine, bufLength, null);
    }

    @Override
    public IKey createKey(Row buffer) {
        return new LexRow(buffer,null);
    }

    public <T extends IKey> float deriveCoefficient(T left, T right) {
        return 0.5f; // Assume 50% coefficient of binary search is best estimate if not on the same chromosome
    }

    @Override
    public String toString() {
        return key.toString();
    }
}
