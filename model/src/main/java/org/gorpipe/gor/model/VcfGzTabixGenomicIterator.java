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
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableStream;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.util.StringUtil;
import org.gorpipe.model.gor.RowObj;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Simple genomic iterator for zipped vcf files with index
 */
public class VcfGzTabixGenomicIterator extends GenomicIteratorBase {
    private TabixReader reader;
    private TabixReader.Iterator iterator;
    private List<String> chrs;
    private int hgSeekIndex = 0;

    private Row nextRow = null;

    ChromoLookup lookup; // chromosome name lookup service
    private final String fileName;

    public VcfGzTabixGenomicIterator(ChromoLookup lookup, StreamSource file, StreamSource idxfile) throws IOException {
        fileName = file.getFullPath();
        init(lookup, new TabixReader(fileName, idxfile.getFullPath(), new StreamSourceSeekableStream(file)));
    }

    private void init(ChromoLookup lookup, TabixReader reader) throws IOException {
        this.reader = reader;
        this.lookup = lookup;
        findHeader();

        chrs = new ArrayList<>(reader.getChromosomes());
        chrs.sort(Comparator.naturalOrder());
    }

    private void findHeader() throws IOException {
        String line;
        while ((line = reader.readLine()) != null && line.startsWith("##")) {
            // Read all extra header lines
        }
        if (line == null || !line.startsWith("#")) {
            throw new GorDataException("Expected to find header line start with a single # in file", fileName);
        }
        String[] header = StringUtil.splitToArray(line, 1, '\t');

        setHeader(String.join("\t",header));
    }

    @Override
    public boolean seek(String chr, int pos) {
        nextRow = null;
        hgSeekIndex = chrs.indexOf(chr);
        if (hgSeekIndex == -1 && chr.startsWith("chr")) {
            chr = chr.substring(3);
            hgSeekIndex = chrs.indexOf(chr);
        }
        iterator = reader.query(chr, Math.max(0, pos - 1), Integer.MAX_VALUE);
        if (iterator != null) {
            try {
                String s = iterator.next();
                while (s != null) {
                    nextRow = createRow(s);
                    if (nextRow.pos >= pos) {
                        return true;
                    }
                    s = iterator.next();
                }
                return false;
            } catch (IOException e) {
                throw new GorResourceException("Error reading file while seeking", fileName, e);
            }
        }
        return false;
    }

    private Row createRow(String s) {
        Row row = RowObj.apply(s);
        String chr = lookup.chrToName(row.chr);
        if (!chr.equals(row.chr)) {
            String s2 = String.format("%s\t%d\t%s", chr, row.pos, row.otherCols());
            row = RowObj.apply(s2);
        }
        return row;
    }

    @Override
    public boolean hasNext() {
        if (nextRow != null) {
            return true;
        }
        if (iterator == null) {
            iterator = reader.query(chrs.get(hgSeekIndex), 0, Integer.MAX_VALUE);
        }
        if (iterator != null) {
            try {
                String s = iterator.next();
                if (s == null) {
                    iterator = null;
                    hgSeekIndex++;
                    if(hgSeekIndex < chrs.size()) {
                        return hasNext();
                    } else {
                        return false;
                    }
                } else {
                    nextRow = createRow(s);
                    return true;
                }
            } catch (IOException e) {
                throw new GorResourceException("Error reading file", fileName, e);
            }
        }
        return false;
    }

    @Override
    public Row next() {
        if (nextRow != null) {
            Row row = nextRow;
            nextRow = null;
            return row;
        }
        if (iterator == null) {
            iterator = reader.query(chrs.get(hgSeekIndex), 0, Integer.MAX_VALUE);
        }

        if (iterator != null) {
            try {
                final String s = iterator.next();
                return createRow(s);
            } catch (IOException e) {
                throw new GorResourceException("Error reading file", fileName, e);
            }
        }

        return null;
    }

    @Override
    public void close() {
        try {
            if (iterator != null) {
                iterator = null;
            }

            if (reader != null) {
                reader.close();
                reader = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("unable to close tabixreader", e);
        }
    }
}
