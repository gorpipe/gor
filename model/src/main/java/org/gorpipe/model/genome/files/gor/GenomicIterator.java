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

import org.gorpipe.gor.GorContext;
import org.gorpipe.gor.GorSession;
import org.gorpipe.gor.stats.StatsCollector;

import java.util.Iterator;

/**
 * GenomicIterator is a seekable iterator on genomic ordered data. It assumes the data source contains
 * chromosome and position information and is sorted on those in ascending order.
 * <p>
 * This is implemented as input into the gor engine.
 *
 * @version $Id$
 */
public abstract class GenomicIterator implements Iterator<Row>, AutoCloseable {
    /**
     * Lookup Class for chromosome names to ids
     */
    public interface ChromoLookup {
        /**
         * @param id The chromosome id to convert to name
         * @return The chromosome name
         */
        String idToName(int id);

        /**
         * @param chr chromosome name
         * @return The id of the chromosome, or -1 if chr is not a known chromosome
         */
        int chrToId(String chr);

        default String chrToName(String chr) {
            int id = chrToId(chr);
            if (id >= 0) return idToName(id);
            return chr;
        }

        /**
         * @param chr chromosome name
         * @return The length of the chromosome, or -1 if chr is not a known chromosome/length
         */
        int chrToLen(String chr);

        /**
         * Given a string that starts with chr and is at least 4 char long, find the chr id of it
         *
         * @param str    The string, assumed to start with chr and be at least 4 chars long
         * @param strlen The total length of the string
         * @return The id of the chromosome or -1 if this is not a valid chromosome id.
         */
        int chrToId(CharSequence str, int strlen);

        /**
         * Assume the incoming buffer is prefixed with chromosome information that end with a tab char.
         *
         * @param buf    The buffer to read
         * @param offset The offset into the buffer to start reading from
         * @return The id of the chromosome information part in the buffer, or -1 if buffer doesn't start with a known chromosome
         */
        int prefixedChrToId(byte[] buf, int offset);

        /**
         * Assume the incoming buffer is prefixed with chromosome information that end with a tab char.
         *
         * @param buf    The buffer to read
         * @param offset The offset into the buffer to start reading from
         * @param buflen The length of the buffer
         * @return The id of the chromosome information part in the buffer, or -1 if buffer doesn't start with a known chromosome
         */
        int prefixedChrToId(byte[] buf, int offset, int buflen);

        /**
         * @return current chromosome cache
         */
        ChromoCache getChromCache();


    }
    private int colnum = 0;

    private String sourceName = "";

    private int sourceIndex = 0;
    private boolean sourceAlreadyInserted;

    private static final int[][] DEFAULT_COLS = {{}, {2}, {2, 3}, {2, 3, 4}, {2, 3, 4, 5}, {2, 3, 4, 5, 6}}; // Default cols of common length

    private Line line;

    private byte tagStatus = SourceRef.NO_TAG;

    private GorContext context = null;
    private StatsCollector statsCollector = null;
    private int statsSenderId = -1;
    String statsSenderName = "";
    String statsSenderAnnotation = "";


    public GorContext getContext() {
        return context;
    }

    public void setContext(GorContext context) {
        this.context = context;
        if (context != null) {
            statsCollector = context.getStats();
            if (statsCollector != null && !statsSenderName.equals("")) {
                statsSenderId = statsCollector.registerSender(statsSenderName, statsSenderAnnotation);
            }
        }
    }

    void incStat(String name) {
        if (statsCollector != null) {
            statsCollector.inc(statsSenderId, name);
        }
    }

    void decStat(String name) {
        if (statsCollector != null) {
            statsCollector.dec(statsSenderId, name);
        }
    }

    void addStat(String name, float delta) {
        if (statsCollector != null) {
            statsCollector.add(statsSenderId, name, delta);
        }
    }

    /**
     * @return Source name associated with this iterator
     */
    public String getSourceName() {
        return sourceName;
    }
    /**
     * @param sourceName Source name associated with this iterator
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public void setSourceIndex(int sourceIndex) {
        this.sourceIndex = sourceIndex;
    }

    public boolean isSourceAlreadyInserted() {
        return sourceAlreadyInserted;
    }

    public void setSourceAlreadyInserted(boolean sourceAlreadyInserted) {
        this.sourceAlreadyInserted = sourceAlreadyInserted;
    }

    public int getColnum() {
        return colnum;
    }

    public void setColnum(int colnum) {
        this.colnum = colnum;
    }

    public byte getTagStatus() {
        return tagStatus;
    }

    public void setTagStatus(byte tagStatus) {
        this.tagStatus = tagStatus;
    }

    /**
     * Get custom chromosome lookup if any
     */
    public ChromoLookup getLookup() {
        return null;
    }

    /**
     * Get the header describing the data
     *
     * @return An array of column names
     */
    public abstract String[] getHeader();

    /**
     * Seek to the specified genomic position in the data source
     *
     * @param session Gor session
     */
    public void init(GorSession session) {
    }


    /**
     * Seek to the specified genomic position in the data source
     *
     * @param chr The chromosome to find
     * @param pos The position within the chromosome to start with
     * @return True if data is available at or after the specified position, else false
     */
    public abstract boolean seek(String chr, int pos);

    /**
     * Seek to the specified genomic position in the data source
     *
     * @param chr The chromosome to find
     * @param pos The position within the chromosome to start with
     * @param end The position within the chromosome to end
     * @return True if data is available at or after the specified position, else false
     */
    public boolean seek(String chr, int pos, int end) {
        return seek(chr, pos);
    }

    /**
     * Read the next data line
     *
     * @param line The result holder for the line to be read
     * @return True if data was read, else false
     */
    public abstract boolean next(Line line);

    /**
     * Close the data source, releasing all resources (typically files).
     */
    public abstract void close();

    /**
     * @return ResourceMonitor instance for source managing resources, or null for simple sources like file references.
     */
    public ResourceMonitor getMonitor() {
        return null;
    }

    static int[] createAllCols(int len) {
        if (len < DEFAULT_COLS.length) {
            return DEFAULT_COLS[Math.max(0, len)]; // Use common cols arrays if possible, will be negative for empty files
        }

        int[] a = new int[len];
        for (int i = 0; i < len; i++) {
            a[i] = i + 2;
        }
        return a;
    }

    public boolean hasNext() {
        line = new Line(colnum);
        return next(line);
    }

    public Row next() {
        return line;
    }
}
