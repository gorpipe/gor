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
import org.gorpipe.gor.model.RowBase;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * An array of GOR row objects implementing iterator interface
 * The user class, BatchedReadSource, ensures thread safety
 * <p>
 * Created by sigmar on 24/11/2016.
 */
public class RowBuffer implements Iterator<Row> {
    static final int MAX_NUMBER_OF_ROWS = Integer.parseInt(System.getProperty("gor.rowbuffer.max_rows_buffered", "1024"));
    private static final int DEFAULT_MAX_BYTES_IN_BUFFER = Integer.parseInt(System.getProperty("gor.rowbuffer.max_bytes_buffered", "1073741824"));  // Default 1 GB
    private static final int NUM_LINES_TO_ESTIMATE_LINE_SIZE = Integer.parseInt(System.getProperty("gor.rowbuffer.lines_for_size_estimation", "100"));

    private Row[] rowArray;
    private int count;
    private int idx;
    private RowBuffer next;
    private int capacity;
    private final int maxBytes;              // Maximum bytes used for buffering.
    private int byteCount;             // Used bytes (or currently an estimate of the used bytes).
    private int columnCount = -1;

    private int estimatedAvgLineSize;

    public RowBuffer(int capacity, RowBuffer next) {
        this(capacity, DEFAULT_MAX_BYTES_IN_BUFFER, next);
    }

    public RowBuffer(int capacity, int maxBytes, RowBuffer next) {
        this(capacity, maxBytes, MAX_NUMBER_OF_ROWS, next);
    }

    public RowBuffer(int capacity, int maxBytes, int maxNumerOfRows, RowBuffer next) {
        rowArray = new Row[maxNumerOfRows];
        count = 0;
        byteCount = 0;
        idx = 0;
        this.maxBytes = maxBytes;
        this.capacity = capacity;
        this.next = next;
        estimatedAvgLineSize = 0;
    }

    public RowBuffer(RowBuffer next) {
        this(1, next);
    }

    public RowBuffer(int capacity) {
        this(capacity, null);
    }

    public RowBuffer() {
        this(null);
    }

    public void resize(int newSize) {
        rowArray = new Row[newSize];
    }

    public Row[] getRowArray() {
        return rowArray;
    }

    public boolean containsEndRow() {
        return count > 0 && rowArray[count-1].pos == -1;
    }

    public void setNextRowBuffer(RowBuffer buffer) {
        this.next = buffer;
    }

    public RowBuffer nextRowBuffer() {
        next.count = 0;
        next.byteCount = 0;
        next.idx = 0;
        next.estimatedAvgLineSize = 0;
        return next;
    }

    public boolean enlarge(int newsize) {
        int oldcapacity = capacity;
        if( byteCount < maxBytes ) capacity = Math.min(newsize, rowArray.length);
        return capacity != oldcapacity;
    }

    public void reduce(int newsize) {
        capacity = Math.max(newsize, 1);
    }

    public Row get(int i) {
        return rowArray[i];
    }

    public void add(Row r) {
        rowArray[count++] = r;

        if (count < NUM_LINES_TO_ESTIMATE_LINE_SIZE) {
            int lineBytes = r.length();
            estimatedAvgLineSize = (count == 1) ? lineBytes : (estimatedAvgLineSize * (count - 1) + lineBytes) / count;
            byteCount += lineBytes;
        } else {
            byteCount += estimatedAvgLineSize;
        }
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    void update(byte[] buffer, int length) {
        final var str = length > 0 ? new String(buffer, 0, length, StandardCharsets.UTF_8) : "";
        update(str);
    }

    void parUpdate(String buffer) {
        var tsplit = buffer.split("\n");
        count = tsplit.length;
        if (rowArray.length < count) resize(count);
        IntStream.range(0,count).parallel().forEach(i -> rowArray[i] = new RowBase(tsplit[i], columnCount));
    }

    void seqUpdate(String buffer) {
        count = 0;
        var l = 0;
        var k = buffer.indexOf('\n');
        while (k != -1) {
            if (count>=rowArray.length) {
                rowArray = Arrays.copyOf(rowArray,rowArray.length+2);
            }
            rowArray[count++] = new RowBase(buffer.substring(l,k), columnCount);
            l = k+1;
            k = buffer.indexOf('\n', l);
        }
        if (l < buffer.length()) {
            if (count>=rowArray.length) {
                rowArray = Arrays.copyOf(rowArray,rowArray.length+2);
            }
            rowArray[count++] = new RowBase(buffer.substring(l), columnCount);
        }
    }

    void update(String buffer) {
        byteCount = buffer.length();
        idx = 0;
        if (byteCount==0) {
            clear();
        } else {
            seqUpdate(buffer);
        }
        estimatedAvgLineSize = byteCount/count;
    }

    public Row pop() { return rowArray[--count]; }

    public boolean hasNext() {
        return available() && rowArray[idx].pos != -1;
    }

    public Row next() {
        return rowArray[idx++];
    }

    public void seek(Row key) {
        idx = Arrays.binarySearch(rowArray, 0, count, key);
        if (idx<0) idx = -(idx+1);
    }

    public synchronized void clear() {
        count = 0;
        byteCount = 0;
        idx = 0;
        estimatedAvgLineSize = 0;
    }

    public int getIndex() {
        return idx;
    }

    public boolean isWaiting() {
        return !available() && !isFull();
    }

    public boolean available() {
        return idx < count;
    }

    public boolean isFull() {
        return count == capacity || byteCount >= maxBytes;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public int size() {
        return count;
    }

    public int getCapacity() {
        return capacity;
    }
}
