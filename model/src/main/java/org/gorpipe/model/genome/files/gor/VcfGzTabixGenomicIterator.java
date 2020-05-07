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
import org.gorpipe.util.collection.ByteArray;
import org.gorpipe.model.util.Util;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableStream;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.util.string.StringUtil;
import htsjdk.tribble.readers.TabixReader;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;

/**
 * Simple genomic iterator for zipped vcf files, can not be seeked into
 */
public class VcfGzTabixGenomicIterator extends GenomicIterator {
    private int chrId = Integer.MIN_VALUE;
    private int hgSeekIndex = -1;
    private TabixReader reader;
    private TabixReader.Iterator iterator;
    private Line linebuf;

    public enum ChrNameSystem {
        WITH_CHR_PREFIX,
        WITHOUT_CHR_PREFIX,
    }

    private ChrNameSystem chrNameSystem;

    private int[] columns; // Source columns to include
    GenomicIterator.ChromoLookup lookup; // chromosome name lookup service

    @Override
    public ChromoLookup getLookup() {
        return lookup;
    }

    public VcfGzTabixGenomicIterator(GenomicIterator.ChromoLookup lookup, StreamSource file, StreamSource idxfile, int cols[]) throws IOException {
        init(lookup, new TabixReader(file.getName(), idxfile.getName(), new StreamSourceSeekableStream(file)), cols, StringIntKey.cmpLexico);
    }

    private void init(GenomicIterator.ChromoLookup lookup, TabixReader reader, int cols[], Comparator<StringIntKey> comparator) throws IOException {
        this.reader = reader;
        this.lookup = lookup;
        setColumns(cols);

        Set<String> chrs = reader.getChromosomes();

        String chrname = "";
        if (chrs.size() > 0) {
            // if we have a chromosome in the data, lets get it
            chrname = chrs.iterator().next();
        }

        if (chrname.startsWith("chr")) {
            chrNameSystem = ChrNameSystem.WITH_CHR_PREFIX;
        } else {
            chrNameSystem = ChrNameSystem.WITHOUT_CHR_PREFIX;
        }
    }

    private void setColumns(int[] cols) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && line.startsWith("##")) {
            // Read all extra header lines
        }
        if (line == null || !line.startsWith("#")) {
            throw new GorDataException("Error Initializing Query. Expected to find header line start with a single # in file");
        }
        String[] header = StringUtil.splitToArray(line, 1, '\t');
        final int totalExtraCols = header.length - 2;

        if (cols != null) {
            this.columns = new int[cols.length - 2];
            final String[] newheader = new String[cols.length];
            newheader[0] = header[0];
            newheader[1] = header[1];
            for (int i = 2; i < cols.length; i++) {
                this.columns[i - 2] = cols[i] - 2;
                newheader[i] = header[cols[i]];
            }
            setHeader(String.join("\t",newheader));
            linebuf = new Line(totalExtraCols);
        } else {
            columns = null;
            linebuf = null;
            setHeader(String.join("\t",header));
        }
    }

    @Override
    public boolean seek(String chr, int pos) {
        chrId = lookup.chrToId(chr); // Mark that a single chromosome seek
        createIterator(chrId, Math.max(0, pos - 1));
        return iterator != null;
    }

    @Override
    public boolean next(Line l) {
        if (chrId == Integer.MIN_VALUE) {
            hgSeekIndex = 0;
            createIterator(ChrDataScheme.ChrLexico.order2id[hgSeekIndex], 0);
            chrId = -1;
        }

        if (iterator != null) {
            try {
                final String s = iterator.next();
                if (s != null) {
                    final byte[] buf = s.getBytes(Util.utf8Charset);
                    // Read chromosome and position first
                    l.chrIdx = lookup.prefixedChrToId(buf, 0, buf.length);
                    if (l.chrIdx != -1) {
                        l.chr = lookup.idToName(l.chrIdx);
                        int offset = 0;
                        while (offset < buf.length && buf[offset++] != '\t') {
                            // Find end of column
                        }

                        l.pos = ByteArray.toInt(buf, offset);
                        offset += ByteTextBuilder.cntDigits(l.pos) + 1;

                        if (linebuf != null) {
                            // Rearange according to column selection
                            linebuf.setData(buf, offset);
                            for (int i = 0; i < columns.length; i++) {
                                l.cols[i].set(linebuf.cols[columns[i]].getBytes());
                            }
                        } else {
                            // Read all of the rest of the columns
                            l.setData(buf, offset);
                        }
                        return true;
                    }
                }
                iterator = null;

                if (hgSeekIndex >= 0) { // Is seeking through differently ordered data
                    if (++hgSeekIndex >= ChrDataScheme.ChrLexico.order2id.length) {
                        return false; // All human genome chromosomes have been read, so nothing more to return
                    }
                    final int order = ChrDataScheme.ChrLexico.order2id[hgSeekIndex];
                    createIterator(order, 0);

                    return next(l);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return false;
    }

    private void createIterator(int chrId, int pos) {
        assert chrId >= 0;

        if (chrNameSystem == ChrNameSystem.WITHOUT_CHR_PREFIX) { // BAM data on hg chromsome names, use the hg name for the chromsome for the seek
            String chr = ChromoCache.getHgName(chrId);
            iterator = reader.query(chr, pos, Integer.MAX_VALUE);
        } else if (chrNameSystem == ChrNameSystem.WITH_CHR_PREFIX) {
            String chr = ChromoCache.getStdChrName(chrId);
            iterator = reader.query(chr, pos, Integer.MAX_VALUE);
        }
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
