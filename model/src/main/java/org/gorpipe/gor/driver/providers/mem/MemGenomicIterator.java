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

package org.gorpipe.gor.driver.providers.mem;

import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.Line;

import java.util.Arrays;

/**
 * Simple memory based line generator for testing purposes.
 */
public class MemGenomicIterator extends GenomicIterator {
    int posit = 0;
    int chromo = 1;
    final byte[] data1 = "data1".getBytes();
    final byte[] data = "data".getBytes();
    final int lines;
    final int[] columns; // source columns to include
    final int[] columnMap; // map source columns into Line data columns
    static final String[] COLS = {"Chromo", "Pos", "Col3", "Col4", "Col5"};
    final GenomicIterator.ChromoLookup lookup;

    public MemGenomicIterator(GenomicIterator.ChromoLookup lookup, int lines, int columns[]) {
        this.lookup = lookup;
        this.lines = lines;
        this.columns = columns != null ? columns : new int[]{0, 1, 2, 3, 4};
        this.columnMap = new int[COLS.length];
        Arrays.fill(columnMap, -1);
        for (int i = 2; i < this.columns.length; i++) {
            columnMap[this.columns[i]] = i - 2;
        }
    }

    @Override
    public String getHeader() {
        String[] headers = new String[columns.length];
        for (int i = 0; i < headers.length; i++) {
            headers[i] = COLS[columns[i]];
        }
        return String.join("\t",headers);
    }

    @Override
    public boolean seek(String chr, int pos) {
        chromo = lookup.chrToId(chr);
        assert chromo >= 0;
        posit = pos;
        return true;
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public boolean next(Line line) {
        if (posit < lines) {
            line.chrIdx = chromo;
            line.chr = lookup.idToName(line.chrIdx);
            line.pos = posit++;
            if (columnMap[2] >= 0) {
                line.cols[columnMap[2]].set(data1);
            }
            if (columnMap[3] >= 0) {
                line.cols[columnMap[3]].set(line.pos % 5);
            }
            if (columnMap[4] >= 0) {
                line.cols[columnMap[4]].set(data);
                line.cols[columnMap[4]].append(line.pos % 5);
            }
            return true;
        }
        return false;
    }
}
