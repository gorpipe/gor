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

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.model.util.ByteTextBuilder;
import org.gorpipe.util.collection.ByteArray;
import org.gorpipe.model.util.NCGZIPInputStream;
import org.gorpipe.model.util.Util;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.driver.adapters.PositionAwareInputStream;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.util.string.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Simple genomic iterator for zipped vcf files, can not be seeked into
 */
public class VcfGzGenomicIterator extends GenomicIterator {
    private int[] columns; // Source columns to include
    private String[] header; // Header of file
    public BufferedReader reader; // The reader to use
    private StreamSource streamSource;
    final GenomicIterator.ChromoLookup lookup; // chromosome name lookup service
    private Line linebuf; // The linebuf to temporarily read the data into
    public String next;
    public VcfGzTabixGenomicIterator.ChrNameSystem chrNameSystem;

    int len = 0;

    public VcfGzGenomicIterator(ChromoLookup lookup) {
        this.lookup = lookup;
    }

    public VcfGzGenomicIterator(GenomicIterator.ChromoLookup lookup, String file, int cols[], StreamSource streamsource, boolean compressed) throws IOException {
        this(lookup, file, cols, new BufferedReader(new InputStreamReader(compressed ? new GZIPInputStream(new NCGZIPInputStream(new PositionAwareInputStream(streamsource.open()))) : streamsource.open())));
        this.streamSource = streamsource;
    }

    public VcfGzGenomicIterator(GenomicIterator.ChromoLookup lookup, String file, int cols[], BufferedReader reader) throws IOException {
        this(lookup);
        init(file, cols, reader);
    }

    public void init(String file, int cols[], BufferedReader reader) throws IOException {
        this.reader = reader;
//
//    		// File is either normal gzip file or pgzip file, must use distinct classes for reading thoose.
//    		final InputStream stream = new BufferedInputStream(new FileInputStream(file));
//    		boolean isBgZip = BlockCompressedInputStream.isValidFile(stream);
//    		isBgZip = false;
//    		stream.close();
//
//    		final InputStream in = isBgZip ? new BlockCompressedInputStream(new FileInputStream(file)) : new GZIPInputStream(new FileInputStream(file), 16*1024);
//    		reader = new BufferedReader(new InputStreamReader(in), 16*1024);

        // Must iterate to the beginning of the file, ignoring commenting header lines
        String line = null;
        String contig = "##contig=<ID=";
        String length = "length=";
        while ((line = reader.readLine()) != null && line.startsWith("##")) {
            if (line.startsWith(contig)) {
                if (chrNameSystem == null) {
                    if (line.substring(contig.length()).startsWith("chr")) {
                        chrNameSystem = VcfGzTabixGenomicIterator.ChrNameSystem.WITH_CHR_PREFIX;
                    } else {
                        chrNameSystem = VcfGzTabixGenomicIterator.ChrNameSystem.WITHOUT_CHR_PREFIX;
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
        final int totalExtraCols = headerAll.length - 2;

        // Set up the column maps
        if (cols != null) {
            this.columns = new int[cols.length - 2];
            final String[] newheader = new String[cols.length];
            newheader[0] = headerAll[0];
            newheader[1] = headerAll[1];
            for (int i = 2; i < cols.length; i++) {
                this.columns[i - 2] = cols[i] - 2;
                newheader[i] = headerAll[cols[i]];
            }
            setHeader(String.join("\t",newheader));
            linebuf = new Line(totalExtraCols);
        } else {
            columns = null;
            linebuf = null;
            setHeader(String.join("\t",headerAll));
        }
        next = reader.readLine();
    }

    @Override
    public boolean seek(String chr, int pos) {
        // Could use tabix to seek and read
        throw new RuntimeException("Cant seek to gzipped .vcf files");
    }

    @Override
    public boolean next(Line line) {
        len++;
        try {
            if (reader != null) {
                if (next != null) {
                    final byte[] buf = next.getBytes(Util.utf8Charset);
                    next = reader.readLine();
                    // Read chromosome and position first
                    line.chrIdx = lookup.prefixedChrToId(buf, 0, buf.length);
                    if (line.chrIdx != -1) {
                        line.chr = lookup.idToName(line.chrIdx);
                        int offset = 0;
                        while (offset < buf.length && buf[offset++] != '\t') {
                            // Find end of column
                        }

                        line.pos = ByteArray.toInt(buf, offset);
                        offset += ByteTextBuilder.cntDigits(line.pos) + 1;

                        if (linebuf != null) {
                            // Rearange according to column selection
                            linebuf.setData(buf, offset);
                            for (int i = 0; i < columns.length; i++) {
                                line.cols[i].set(linebuf.cols[columns[i]].getBytes());
                            }
                        } else {
                            // Read all of the rest of the columns
                            line.setData(buf, offset);
                        }
                        return true;
                    }
                }
                close();
            }
            return false;
        } catch (IOException ex) {
            if ("Stream closed".equals(ex.getMessage())) {
                // Just return false if the stream was closed.
                return false;
            }
            throw new RuntimeException(ex);
        }
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
