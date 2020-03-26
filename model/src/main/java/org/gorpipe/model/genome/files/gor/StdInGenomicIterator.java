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

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.util.collection.ByteArray;

import java.util.ArrayList;
import java.util.Arrays;

class StdInGenomicIterator extends GenomicIterator {
    private byte[] buf;
    private int begin = 0;
    private int end = 0;
    private int[] columns;
    private int[] columnMap; // map source columns into Line data columns
    private boolean hasMore = true;
    private ChromoLookup lookup;

    StdInGenomicIterator(ChromoLookup lookup, int[] columns) {
        buf = new byte[1024 * 1024];
        this.lookup = lookup;
        cache();
        setHeader(readHeader());
        int headerLength = getHeader().split("\t").length;
        this.columns = columns != null ? columns : createDefaultColumns(headerLength);
        this.columnMap = new int[headerLength];
        Arrays.fill(columnMap, -1);
        for (int i = 2; i < this.columns.length; i++) {
            columnMap[this.columns[i]] = i - 2;
        }
    }

    @Override
    public void close() {
        // Can't close the standard input
    }

    private static int[] createDefaultColumns(int len) {
        int[] cols = new int[len];
        for (int i = 0; i < cols.length; i++) {
            cols[i] = i;
        }
        return cols;
    }

    private void cacheIfNeeded(int lenNeeded) {
        if (begin + lenNeeded > end) {
            cache();
        }
    }

    private void cache() { // cache more data from standard input into out buffer
        if (hasMore) {
            copyIntoBuf();
            try {
                final int read = System.in.read(buf, end, buf.length - end);
                if (read == -1) {
                    hasMore = begin != end; // Only signal no more data if all data has been processed
                } else {
                    end += read;
                }
            } catch (Exception ex) {
                throw new GorSystemException("Cache error in sourceRef.", ex);
            }
        }
    }

    @Override
    public boolean next(Line line) {
        if (begin < end || hasMore) {
            cacheIfNeeded(100);
            if (!hasMore) { // For empty files this is needed
                return false;
            }
            line.chrIdx = lookup.prefixedChrToId(buf, begin, end - begin);
            line.chr = line.chrIdx >= 0 ? lookup.idToName(line.chrIdx) : null;
            while (begin < buf.length && buf[begin++] != '\t') {
                // Find column end
            }

            int colend = findColumnEnd();
            line.pos = ByteArray.toInt(buf, begin, colend);
            if (buf[colend] == '\n') {
                if (columns.length > 2) {
                    throw new GorSystemException("Line starting with " + line.chr + " " + line.pos + " is missing data columns", null);
                }
            }
            if (colend > begin && buf[colend + 1] == '\r') {
                colend++;
            }
            begin = colend + 1;

            for (int i = 2; i < columns.length; i++) {
                if (columnMap[i] >= 0) {
                    colend = findColumnEnd();
                    int len = colend - begin;
                    line.cols[columnMap[i]].set(buf, begin, len);
                    if (buf[colend] == '\n') {
                        if (i != columns.length - 1) {
                            throw new GorSystemException("Line starting with " + line.chr + " " + line.pos + " is missing data columns", null);
                        }
                    }
                    if (colend > begin && buf[colend + 1] == '\r') {
                        len++;
                    }
                    begin += len + 1;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean seek(String chr, int pos) {
        throw new RuntimeException("Can't seek into standard input");
    }

    private int findColumnEnd() {
        for (int i = begin; ; i++) {
            if (i + 1 >= end) { // Cache more data
                final int read = i - begin;
                cache();
                i = begin + read;
            }
            if (i == end || buf[i] == '\n' || buf[i] == '\t') {
                return i;
            }
        }
    }

    private void copyIntoBuf() {
        if (begin < end) {
            int len = end - begin;
            System.arraycopy(buf, begin, buf, 0, len);
            begin = 0;
            end = len;
        } else {
            begin = end = 0;
        }
    }

    private String readHeader() {
        // Start by checking first line, is it header line or data line
        int secColBegin = -1, secColLen = -1, colcnt = 1;
        for (int i = begin; i < end && buf[i] != '\n'; i++) {
            if (i == end && end == buf.length - 1) {
                throw new GorSystemException("Standard input header to big, must be less than " + buf.length + " bytes!", null);
            }
            if (buf[i] == '\t') {
                colcnt++;
                if (secColLen == -1) {
                    if (secColBegin >= 0) {
                        secColLen = i - secColBegin;
                    } else {
                        secColBegin = i + 1;
                    }
                }
            }
        }

        try {
            Integer.parseInt(new String(buf, secColBegin, secColLen));
            // Found number data which indicate a non header
            final String[] h = new String[colcnt];
            h[0] = "Chromo";
            h[1] = "Pos";
            for (int i = 2; i < colcnt; i++) {
                h[i] = "Col" + (i + 1);
            }
            return String.join("\t",h);
        } catch (NumberFormatException ex) {
            // Found non numeric data which indicates a header line
        }

        final ArrayList<String> cols = new ArrayList<String>();
        for (int i = begin; ; i++) {
            if (buf[i] == '\n' || i == end) {
                cols.add(new String(buf, begin, i - begin));
                if (i > begin && buf[i + 1] == '\r') {
                    i++;
                }
                begin = i + 1;
                break; // At the end of line, just return the headers found
            } else if (buf[i] == '\t') {
                cols.add(new String(buf, begin, i - begin));
                begin = i + 1;
            }
            if (i == end && end == buf.length - 1) {
                throw new GorSystemException("Standard input header to big, must be less than " + buf.length + " bytes!", null);
            }
        }
        return String.join("\t",cols);
    }
}
