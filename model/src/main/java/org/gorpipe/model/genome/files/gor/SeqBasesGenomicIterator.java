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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * Simple memory based line generator for testing purposes
 */
class SeqBasesGenomicIterator extends GenomicIterator {
    private static final Logger log = LoggerFactory.getLogger(SeqBasesGenomicIterator.class);

    int chromo;  // The index of the current chromosome
    String chromoName;        // The name of the chromosome
    RandomAccessFile seqfile; // The current file being read
    int seqfileLength;  // The total length of the current file
    int filePos = 0;    // The next position in the file to be queried
    int bufFilePos = 0; // The file position of the first element in the buffer
    final int[] columns; // source columns to include
    final int[] columnMap; // map source columns into Line data columns
    final String path; // The path to the directory containing the sequence bases
    static final String[] COLS = {"Chromo", "Pos", "Base"};
    final ChromoLookup lookup;
    final short BUF_SIZE = 1024 * 16;
    final byte[] buf = new byte[BUF_SIZE];

    SeqBasesGenomicIterator(String path, ChromoLookup lookup, int columns[]) {
        this.path = path + '/';
        this.lookup = lookup;
        this.columns = columns != null ? columns : new int[]{0, 1, 2};
        this.columnMap = new int[COLS.length];
        Arrays.fill(columnMap, -1);
        for (int i = 2; i < this.columns.length; i++) {
            columnMap[this.columns[i]] = i - 2;
        }
        filePos = 0;
        bufFilePos = filePos - (BUF_SIZE + 2); // Ensure we will start by reading
        try {
            chromo = 1;
            chromoName = lookup.idToName(chromo);
            seqfile = new RandomAccessFile(this.path + "chr1.txt", "r");
            assert seqfile.length() <= Integer.MAX_VALUE;
            seqfileLength = (int) seqfile.length();
        } catch (IOException e) {
            log.warn("Could not load chr1.txt on initialization of SeqBasesGenomicIterator", e);
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
        chromoName = lookup.idToName(chromo);
        final String name = path + chr + ".txt";
        try {
            if (seqfile != null) {
                seqfile.close();
            }
            seqfile = new RandomAccessFile(name, "r");
            filePos = Math.max(0, pos - 1);
            bufFilePos = filePos - (BUF_SIZE + 2); // Ensure we will start by reading
            seqfile.seek(filePos);
            assert seqfile.length() <= Integer.MAX_VALUE;
            seqfileLength = (int) seqfile.length();
        } catch (IOException e) {
            if (!chr.equals("chrXY")) { // There is no sequence data for this so just ignore such error
                throw new GorSystemException("Can't read file " + name, e);
            }
        }
        return true;
    }

    @Override
    public void close() {
        try {
            if (seqfile != null) {
                seqfile.close();
            }
        } catch (IOException e) {
            throw new GorSystemException("Cannot close seq file", e);
        }
    }

    @Override
    public boolean next(Line line) {
        if (filePos < seqfileLength) {
            line.chrIdx = chromo;
            line.chr = chromoName;
            if (filePos - bufFilePos >= BUF_SIZE) {
                try {
                    int read = seqfile.read(buf);
                    assert read == BUF_SIZE || read + filePos == seqfileLength;
                    bufFilePos = filePos;
                } catch (IOException e) {
                    throw new GorDataException("Error reading " + path + chromoName + ".txt. " + e.getMessage());
                }
            }

            line.cols[0].set(buf[filePos - bufFilePos]);
            line.pos = ++filePos;
            return true;
        }
        final int id = new ChromoCache().findNextInLexicoOrder(chromoName);
        if (id < 26) { // Only do this for standard human chromosome names
            String chr = lookup.idToName(id);
            if (chr != null) {
                seek(chr, 1);
                return next(line);
            }
        }
        return false;
    }
}
