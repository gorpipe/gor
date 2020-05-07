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

import org.gorpipe.model.genome.files.binsearch.StringIntKey;
import org.gorpipe.model.util.ByteTextBuilder;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableStream;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import htsjdk.tribble.readers.TabixReader;
import htsjdk.tribble.readers.TabixReader.Iterator;

import java.io.IOException;
import java.util.*;

/**
 * Simple genomic iterator for zipped vcf files, can not be seeked into
 */
public class GorGzGenomicIterator extends GenomicIterator {
    private Line line;
    private StringIntKey chrPosKey;
    private TabixReader reader; // The reader to use
    Iterator iterator;
    private GenomicIterator.ChromoLookup lookup; // chromosome name lookup service
    int[] columns; // source columns to include
    String filename;
    String firstChr;

    public GorGzGenomicIterator(GenomicIterator.ChromoLookup lookup, String file, String idxfile, int cols[]) throws IOException {
        this(lookup, file, idxfile, cols, StringIntKey.cmpLexico);
    }

    public GorGzGenomicIterator(GenomicIterator.ChromoLookup lookup, StreamSource file, StreamSource idxfile, int cols[]) throws IOException {
        init(lookup, new TabixReader(file.getName(), idxfile.getName(), new StreamSourceSeekableStream(file)), cols, StringIntKey.cmpLexico);
    }

    public GorGzGenomicIterator(GenomicIterator.ChromoLookup lookup, String file, String idxfile, int cols[], Comparator<StringIntKey> comparator) throws IOException {
        init(lookup, new TabixReader(file, idxfile), cols, comparator);
    }

    private void init(GenomicIterator.ChromoLookup lookup, TabixReader reader, int cols[], Comparator<StringIntKey> comparator) throws IOException {
        this.reader = reader;
        filename = reader.getSource();
        this.lookup = lookup;
        chrPosKey = columns == null ? new StringIntKey(0, 1, comparator) : new StringIntKey(columns[0], columns[1], comparator);
        Set<String> chrSet = new TreeSet(reader.getChromosomes());
        for (String chr : chrSet) {
            firstChr = chr;
            break;
        }
        setColumns(cols);
        line = new Line(getHeader().split("\t").length);
    }

    private void setColumns(int[] cols) throws IOException {
        String header = reader.readLine();
        String[] headerSplit = header.split("\t");
        if (cols == null) {
            columns = createAllCols(headerSplit.length - 2);
            setHeader(header);
        } else {
            columns = new int[cols.length - 2];
            String[] filteredHeader = new String[cols.length];
            filteredHeader[0] = headerSplit[cols[0]];
            filteredHeader[1] = headerSplit[cols[1]];
            for (int col = 2; col < cols.length; col++) {
                filteredHeader[col] = headerSplit[cols[col]];
                columns[col - 2] = cols[col];
            }
            setHeader(String.join("\t",filteredHeader));
        }
    }

    @Override
    public boolean next(Line l) {
        try {
            String buf = iterator == null ? reader.readLine() : iterator.next();
            if (buf != null) {
                line.setData(buf.getBytes(), 0);
                final ByteTextBuilder chrcol = line.cols[chrPosKey.chrCol];
                l.chrIdx = lookup.prefixedChrToId(chrcol.peekAtBuffer(), 0, chrcol.length()); // Encodes chrM as 0 as used by gor
                l.chr = l.chrIdx >= 0 ? lookup.idToName(l.chrIdx) : null;
                l.pos = line.cols[chrPosKey.posCol].toInt();
                for (int i = 0; i < columns.length; i++) {
                    l.cols[i].set(line.cols[columns[i]].getBytes());
                }
                return true;
            }
        } catch (IOException e) { /* ignore */ }
        return false;
    }

    @Override
    public boolean seek(String chr, int pos) {
        iterator = reader.query(chr, pos, Integer.MAX_VALUE);
        boolean ret = iterator != null;
        return ret;
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                if (iterator != null) {
                    iterator = null;
                }

                if (reader != null) {
                    reader.close();
                    reader = null;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
