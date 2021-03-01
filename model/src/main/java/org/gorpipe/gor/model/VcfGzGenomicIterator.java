/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.adapters.PositionAwareInputStream;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.util.ByteTextBuilder;
import org.gorpipe.gor.util.NCGZIPInputStream;
import org.gorpipe.gor.util.StringUtil;
import org.gorpipe.gor.util.Util;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.util.collection.ByteArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Simple genomic iterator for zipped vcf files, can not be seeked into
 */
public class VcfGzGenomicIterator extends GenomicIterator {
    public BufferedReader reader;
    private StreamSource streamSource;
    final GenomicIterator.ChromoLookup lookup; // chromosome name lookup service
    public String next;
    public ChrNameSystem chrNameSystem;

    int len = 0;
    private String fileName;

    public enum ChrNameSystem {
        WITH_CHR_PREFIX,
        WITHOUT_CHR_PREFIX,
    }

    public VcfGzGenomicIterator(ChromoLookup lookup) {
        this.lookup = lookup;
    }

    public VcfGzGenomicIterator(GenomicIterator.ChromoLookup lookup, String file, StreamSource streamsource, boolean compressed) throws IOException {
        this(lookup, file, new BufferedReader(new InputStreamReader(compressed ? new GZIPInputStream(new NCGZIPInputStream(new PositionAwareInputStream(streamsource.open()))) : streamsource.open())));
        this.streamSource = streamsource;
    }

    public VcfGzGenomicIterator(GenomicIterator.ChromoLookup lookup, String file, BufferedReader reader) throws IOException {
        this(lookup);
        init(file, reader);
    }

    public void init(String file, BufferedReader reader) throws IOException {
        fileName = file;
        this.reader = reader;

        // Must iterate to the beginning of the file, ignoring commenting header lines
        String line;
        String contig = "##contig=<ID=";
        String length = "length=";
        while ((line = reader.readLine()) != null && line.startsWith("##")) {
            if (line.startsWith(contig)) {
                if (chrNameSystem == null) {
                    if (line.substring(contig.length()).startsWith("chr")) {
                        chrNameSystem = ChrNameSystem.WITH_CHR_PREFIX;
                    } else {
                        chrNameSystem = ChrNameSystem.WITHOUT_CHR_PREFIX;
                    }
                }

                int c = line.indexOf(length, contig.length());
                if (c != -1) {
                    int l = line.indexOf('>');
                    String key = line.substring(contig.length(), line.indexOf(',', contig.length()));
                    int lc = line.indexOf(',', c + length.length());
                    String val = line.substring(c + length.length(), Math.min(l, lc == -1 ? line.length() : lc));
                    int ival = Integer.parseInt(val);
                    lookup.getChromCache().setLen(key, ival);
                }
            }
        }
        // support bgen_to_vcf
        while (line != null && !line.startsWith("#")) {
            line = reader.readLine();
        }
        if( line == null || !line.startsWith("#") ) {
            throw new GorDataException("Error Initializing Query. Expected to find header line start with a single # in file " + file);
        }
        String[] headerAll = StringUtil.splitToArray(line, 1, '\t');

        setHeader(String.join("\t",headerAll));

        next = reader.readLine();
    }

    @Override
    public boolean seek(String chr, int pos) {
        // Could use tabix to seek and read
        throw new RuntimeException("Cant seek to gzipped .vcf files");
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        try {
            next = reader.readLine();
        } catch (IOException e) {
            throw new GorResourceException("Error reading file", fileName, e);
        }
        return next != null;
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
    public Row next() {
        if (next == null) {
            try {
                next = reader.readLine();
            } catch (IOException e) {
                throw new GorResourceException("Error reading file", fileName, e);
            }
        }
        if (next == null) {
            return null;
        }

        Row row = createRow(next);
        next = null;
        return row;
    }

    @Override
    public boolean next(Line line) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChromoLookup getLookup() {
        return lookup;
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
                reader = null;
            } catch (Exception e) {
                throw new RuntimeException("Failed closing VcfGzGenomicIterator reader", e);
            }
        }
        if (streamSource != null) {
            String name = "<unknown>";
            try {
                streamSource.close();
                name = streamSource.getName();
            } catch (IOException e) {
                throw new GorResourceException("Failed closing VcfGzGenomicIterator stream source", name, e);
            }
        }
    }
}
