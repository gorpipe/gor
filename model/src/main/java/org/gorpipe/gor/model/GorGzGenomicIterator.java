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

package org.gorpipe.gor.model;

import htsjdk.tribble.readers.TabixReader;
import htsjdk.tribble.readers.TabixReader.Iterator;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableStream;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.binsearch.StringIntKey;
import org.gorpipe.model.gor.RowObj;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Simple genomic iterator for zipped vcf files, can not be seeked into
 */
public class GorGzGenomicIterator extends GenomicIteratorBase {
    private TabixReader reader; // The reader to use
    Iterator iterator;
    String filename;
    String firstChr;
    private Row nextRow = null;

    public GorGzGenomicIterator(ChromoLookup lookup, String file, String idxfile) throws IOException {
        this(lookup, file, idxfile, StringIntKey.cmpLexico);
    }

    public GorGzGenomicIterator(ChromoLookup lookup, StreamSource file, StreamSource idxfile) throws IOException {
        init(lookup, new TabixReader(file.getName(), idxfile.getName(), new StreamSourceSeekableStream(file)), StringIntKey.cmpLexico);
    }

    public GorGzGenomicIterator(ChromoLookup lookup, String file, String idxfile, Comparator<StringIntKey> comparator) throws IOException {
        init(lookup, new TabixReader(file, idxfile), comparator);
    }

    private void init(ChromoLookup lookup, TabixReader reader, Comparator<StringIntKey> comparator) throws IOException {
        this.reader = reader;
        filename = reader.getSource();
        Set<String> chrSet = new TreeSet(reader.getChromosomes());
        for (String chr : chrSet) {
            firstChr = chr;
            break;
        }
        String header = this.reader.readLine();
        setHeader(header);
    }

    @Override
    public boolean seek(String chr, int pos) {
        iterator = reader.query(chr, pos, Integer.MAX_VALUE);
        return iterator != null;
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

    @Override
    public boolean hasNext() {
        if (nextRow != null) {
            return true;
        }
        nextRow = next();
        return nextRow != null;
    }

    @Override
    public Row next() {
        if (nextRow != null) {
            Row row = nextRow;
            nextRow = null;
            return row;
        }
        try {
            String buf;
            if (iterator != null) {
                buf = iterator.next();
                if (buf == null) {
                    buf = reader.readLine();
                    iterator = null;
                }
            } else {
                buf = reader.readLine();
            }
            if (buf != null) {
                return RowObj.apply(buf);
            }
        } catch (IOException e) {
            // Ignore
        }
        return null;
    }
}
