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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.gorpipe.model.util.ByteTextBuilder;
import org.gorpipe.model.util.Util;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.model.gor.RowObj;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecord.SAMTagAndValue;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GenomicIterator on top of BAM Files
 *
 * @version $Id$
 */
public class BamIterator extends GenomicIterator {
    private static final Logger log = LoggerFactory.getLogger(BamIterator.class);
    private int chrId = Integer.MIN_VALUE; // Min-value indicates no query has been initiated, -1 indicates reading all bam, else the id of chromsome last saught
    protected int hgSeekIndex = -1; // index into ChromoCache.HG_IN_LEXICO last sought for data in hg reference standard ordering, -1 indicate not being used
    private boolean initialize = true;
    public SamReader reader;
    public CloseableIterator<SAMRecord> it;
    public static final String[] HEADER = {"Chromo", "Pos", "End", "QName", "Flag", "MapQ", "Cigar", "MD", "MRNM", "MPOS", "ISIZE", "SEQ", "QUAL", "TAG_VALUES"};
    private static final int[] ALL_COLUMNS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
    private int[] columns; // source columns to include
    private ToIntFunction<SAMRecord>[] defaultReverseIntMap;
    private Function<SAMRecord, String>[] defaultReverseStringMap;
    private GenomicIterator.ChromoLookup lookup;
    public byte chrnamesystem = -1; // -1 indicates not specified, 0 is lexicographical ordering witch chr prefix,
    // 1 is Human genome reference consortium standard ordering and names, 2 is like 0 except starting with chrM

    private int chrOneLength = -1;
    public SAMFileHeader samFileHeader;
    boolean isCRAM = false;
    public int pos = 0;

    protected BamIterator() {
    }

    /**
     * Construct a BamIterator
     *
     * @param lookup  The lookup service for chromosome name to ids
     * @param file    The BAM File to iterate through
     * @param columns The columns to be included, or null to include all
     */
    public BamIterator(GenomicIterator.ChromoLookup lookup, String file, int[] columns) {
        this(lookup, SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT).open(new File(file)), columns);
    }

    /**
     * Construct a BamIterator
     *
     * @param lookup  The lookup service for chromosome name to ids
     * @param reader  The BAM Reader to use for accessing the bam file
     * @param columns The columns to be included, or null to include all
     */
    public BamIterator(GenomicIterator.ChromoLookup lookup, SamReader reader, int[] columns) {
        init(lookup, reader, columns, false);
    }

    public void init(GenomicIterator.ChromoLookup lookup, SamReader reader, int[] columns, boolean isCRAM) {
        this.isCRAM = isCRAM;
        this.reader = reader;
        this.samFileHeader = reader.getFileHeader();
        this.lookup = lookup;
        String[] txtHeader = new String[0];
        String h = samFileHeader.getTextHeader();
        if (h != null && h.length() > 0) {
            txtHeader = h.split("\n");
        }
        if (txtHeader.length == 0) {
            // If there is no header, assume lexicographic order of chromosomes with chr prefix
            chrnamesystem = 2;
        } else {
            // Allow first line to be either @HD line or start directly with @SQ
            for (int i = txtHeader[0].startsWith("@HD") ? 1 : 0; i < txtHeader.length; i++) {
                if (txtHeader[i].startsWith("@SQ\tSN:")) {
                    int end = txtHeader[i].indexOf('\t', 7);
                    if (end < 0) {
                        throw new GorDataException("Format of chromosome line is not as expected!", -1, txtHeader[i]);
                    }
                    final String firstChr = txtHeader[i].substring(7, end);
                    if (firstChr.startsWith("chr")) {
                        chrnamesystem = 2;
                    } else {
                        chrnamesystem = 1;
                    }
                    int start = txtHeader[i].indexOf("LN:");
                    if (start > -1) {
                        end = txtHeader[i].indexOf('\t', start);
                        if (end > -1) {
                            chrOneLength = Integer.parseInt(txtHeader[i].substring(start + 3, end));
                        } else {
                            chrOneLength = Integer.parseInt(txtHeader[i].substring(start + 3));
                        }
                    }
                    break;
                }
            }
        }
        if (chrnamesystem == -1) {
            throw new GorDataException("Chromsome naming system for BAM file could not be determined!");
        }

        this.columns = columns != null ? columns : ALL_COLUMNS;
        int colnum = this.columns.length;
        this.defaultReverseIntMap = new ToIntFunction[colnum];
        defaultReverseIntMap[0] = (record) -> Integer.parseInt(lookup.chrToName(record.getReferenceName()));
        defaultReverseIntMap[1] = SAMRecord::getAlignmentStart;

        this.defaultReverseStringMap = new Function[colnum];
        defaultReverseStringMap[0] = (record) -> lookup.chrToName(record.getReferenceName());
        defaultReverseStringMap[1] = (record) -> Integer.toString(record.getAlignmentStart());
        for (int i = 2; i < this.columns.length; i++) {
            int k = this.columns[i];
            if (i == 2) defaultReverseIntMap[k] = SAMRecord::getAlignmentEnd;
            else if (i == 4) defaultReverseIntMap[k] = SAMRecord::getFlags;
            else if (i == 5) defaultReverseIntMap[k] = SAMRecord::getMappingQuality;
            else if (i == 9) defaultReverseIntMap[k] = SAMRecord::getMateAlignmentStart;
            else if (i == 10) defaultReverseIntMap[k] = SAMRecord::getInferredInsertSize;

            if (i == 2) defaultReverseStringMap[k] = (record) -> Integer.toString(record.getAlignmentEnd());
            else if (i == 3) defaultReverseStringMap[k] = SAMRecord::getReadName;
            else if (i == 4) defaultReverseStringMap[k] = (record) -> Integer.toString(record.getFlags());
            else if (i == 5) defaultReverseStringMap[k] = (record) -> Integer.toString(record.getMappingQuality());
            else if (i == 6) defaultReverseStringMap[k] = SAMRecord::getCigarString;
            else if (i == 7) defaultReverseStringMap[k] = (record) -> {
                String s = record.getStringAttribute("MD");
                return s == null ? "" : s;
            };
            else if (i == 8) defaultReverseStringMap[k] = SAMRecord::getMateReferenceName;
            else if (i == 9) defaultReverseStringMap[k] = (record) -> Integer.toString(record.getMateAlignmentStart());
            else if (i == 10) defaultReverseStringMap[k] = (record) -> Integer.toString(record.getInferredInsertSize());
            else if (i == 11) defaultReverseStringMap[k] = (record) -> new String(record.getReadBases());
            else if (i == 12) defaultReverseStringMap[k] = (record) -> getBaseQualities(record);
            else if (i == 13) defaultReverseStringMap[k] = (record) -> getAttributes(record);
        }
    }

    public SamReader getReader() {
        return this.reader;
    }

    public void setReader(SamReader reader) {
        this.reader = reader;
        if (this.reader != null) this.it = this.reader.iterator();
    }

    @Override
    public String getHeader() {
        String[] headers = new String[columns.length];
        for (int i = 0; i < headers.length; i++) {
            headers[i] = HEADER[columns[i]];
        }
        return String.join("\t",headers);
    }

    @Override
    public void close() {
        if (it != null) {
            it.close();
            it = null;
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("Error closing", e);
            }
            reader = null;
        }
    }

    void fillLine(Line line, SAMRecord record) {
        line.chrIdx = lookup.chrToId(record.getReferenceName());
        line.chr = line.chrIdx >= 0 ? lookup.idToName(line.chrIdx) : record.getReferenceName(); // Allways report chromosome name on choosen gor form, but fallback to reference name if id lookup failed
        line.pos = record.getAlignmentStart();
        if (columns == ALL_COLUMNS) { // Add all used columns
            line.cols[0].set(record.getAlignmentEnd());
            line.cols[1].set(record.getReadName());
            line.cols[2].set(record.getFlags()); // shall we translate this

            line.cols[3].set(record.getMappingQuality());
            line.cols[4].set(record.getCigarString());

            line.cols[5].set(record.getStringAttribute("MD"));
            line.cols[6].set(record.getMateReferenceName());
            line.cols[7].set(record.getMateAlignmentStart());
            line.cols[8].set(record.getInferredInsertSize());
            line.cols[9].set(record.getReadBases());
            line.cols[10].clear();
            prepareBaseQualities(record, line.cols[10]);
            line.cols[11].clear();
            prepareAttributes(record, line.cols[11]);
        } else { // Pick out the columns used
            for (int c = 0; c < columns.length - 2; c++) {
                int v = columns[c + 2];
                if (v == 2) {
                    line.cols[c].set(record.getAlignmentEnd());
                } else if (v == 3) {
                    line.cols[c].set(record.getReadName());
                } else if (v == 4) {
                    line.cols[c].set(record.getFlags()); // shall we translate this
                } else if (v == 5) {
                    line.cols[c].set(record.getMappingQuality());
                } else if (v == 6) {
                    line.cols[c].set(record.getCigarString());
                } else if (v == 7) {
                    line.cols[c].set(record.getStringAttribute("MD"));
                } else if (v == 8) {
                    line.cols[c].set(record.getMateReferenceName());
                } else if (v == 9) {
                    line.cols[c].set(record.getMateAlignmentStart());
                } else if (v == 10) {
                    line.cols[c].set(record.getInferredInsertSize());
                } else if (v == 11) {
                    line.cols[c].set(record.getReadBases());
                } else if (v == 12) {
                    line.cols[c].clear();
                    prepareBaseQualities(record, line.cols[c]);
                } else if (v == 13) {
                    line.cols[c].clear();
                    prepareAttributes(record, line.cols[c]);
                }
            }
        }
    }

    private void resizeDefaultColumnMaps(int newsize) {
        Function<SAMRecord, String>[] revStringMap = new Function[newsize];
        ToIntFunction<SAMRecord>[] revIntMap = new ToIntFunction[newsize];
        for (int i = 0; i < defaultReverseStringMap.length; i++) {
            revStringMap[i] = defaultReverseStringMap[i];
            revIntMap[i] = defaultReverseIntMap[i];
        }
        defaultReverseStringMap = revStringMap;
        defaultReverseIntMap = revIntMap;
    }

    protected void initIterator() {
        if (this.initialize) {
            int colnumWithPos = getColnum()+2;
            if( colnumWithPos > defaultReverseStringMap.length ) {
                resizeDefaultColumnMaps( colnumWithPos );
            }
            this.initialize = false;
        }
        if (chrId == Integer.MIN_VALUE) {
            if (chrnamesystem == 0) { // Use an iterator on the complete content since its the same order as GOR use
                it = reader.iterator();
            } else {
                String name = "";
                for (hgSeekIndex = 0; hgSeekIndex < ChrDataScheme.ChrLexico.order2id.length; hgSeekIndex++) {
                    int chridx = ChrDataScheme.ChrLexico.order2id[hgSeekIndex];
                    if (chrnamesystem == 1) { // Must seek to each chromosome since chromosome ordering and names are different
                        name = ChromoCache.getHgName(chridx);
                    } else if (chrnamesystem == 2) { // Must seek for chr1 since it is not at the beginning of BAM data
                        name = ChromoCache.getStdChrName(chridx);
                    } else {
                        throw new GorDataException("Unexpected chromosome name system: " + chrnamesystem, -1, samFileHeader.getTextHeader());
                    }

                    if (samFileHeader.getSequenceIndex(name) > -1) {
                        break;
                    }
                }
                createIterator(name, 0);
            }
            chrId = -1;
        }
    }

    private boolean seekToNextChrom(Line line) {
        if (hgSeekIndex >= 0) { // Is seeking through differently ordered data
            while (++hgSeekIndex < ChrDataScheme.ChrLexico.order2id.length) {
                String name = getChromName();
                if (samFileHeader.getSequenceIndex(name) > -1) {
                    createIterator(name, 0);
                    return next(line);
                }
            }
        }
        return false; // All human genome chromosomes have been read, so nothing more to return
    }

    @Override
    public boolean next(Line line) {
        initIterator();
        if (it != null) {
            SAMRecord record = null;
            while (it.hasNext() && (record = it.next()) != null && (record.getReadUnmappedFlag() || "*".equals(record.getCigarString()))) {
                record = null; // Do not want to include unmapped or invalid reads
            }
            if (record != null) {
                fillLine(line, record);
                return true;
            }

            it.close();
            it = null;

            return seekToNextChrom(line);
        }
        return false;
    }

    private String assemblyBasedOnChrOneLength() {
        switch (chrOneLength) {
            case 247249719:
                return "hg18";
            case 249250621:
                return "hg19";
            case 248956422:
                return "hg38";
            default:
                return "";
        }
    }

    @Override
    public boolean seek(String chr, int pos) {
        chrId = lookup.chrToId(chr); // Mark that a single chromosome seek
        assert chrId >= 0;
        if (chrnamesystem == 1) { // BAM data on hg chromsome names, use the hg name for the chromsome for the seek
            chr = ChromoCache.getHgName(chrId);
        } else if (chrnamesystem == 2) {
            chr = ChromoCache.getStdChrName(chrId);
        }
        if (it != null) {
            it.close();
            it = null;
        }
        if (samFileHeader.getSequenceIndex(chr) > -1) {
            createIterator(chr, pos);
        }

        return it != null && it.hasNext();
    }

    public void createIterator(String chr, int pos) {
        if (it != null) {
            it.close();
        }
        it = reader.queryContained(chr, pos == 0 ? 1 : pos, Integer.MAX_VALUE);
    }

    public void prepareBaseQualities(SAMRecord record, ByteTextBuilder btb) {
        for (byte baseQual : record.getBaseQualities()) {
            btb.append((byte) (33 + baseQual));
        }
    }

    public String getBaseQualities(SAMRecord record) {
        ByteTextBuilder btb = new ByteTextBuilder(10);
        //        btb.append(record.getBaseQualities());
        // The Picard library brakes the contract in the SAM format specification. Thus we must add 33 to the quality values to be compliant to the specifications
        prepareBaseQualities(record, btb);
        return btb.toString();
    }

    public String getAttributes(SAMRecord record) {
        ByteTextBuilder tb = new ByteTextBuilder(10);
        prepareAttributes(record, tb);
        return tb.toString();
    }

    public void prepareAttributes(SAMRecord record, ByteTextBuilder tb) {
        for (SAMTagAndValue tagval : record.getAttributes()) {
            if (!tagval.tag.equalsIgnoreCase("MD")) {
                if (tb.length() != 0) {
                    tb.append((byte) (' '));
                }
                tb.append(tagval.tag);
                tb.append((byte) '=');
                // Ensure that no spaces exist in the values, since space is a delimiter, Must assume utf8 conversion is needed.
                if (tagval.value.getClass().isArray()) {
                    tb.append((byte) '[');
                    if (tagval.value instanceof int[]) {
                        int[] a = (int[]) tagval.value;
                        for (int i = 0; i < a.length; i++) {
                            if (i != 0) {
                                tb.append((byte) ',');
                            }
                            tb.append(a[i]);
                        }
                    } else if (tagval.value instanceof short[]) {
                        short[] a = (short[]) tagval.value;
                        for (int i = 0; i < a.length; i++) {
                            if (i != 0) {
                                tb.append((byte) ',');
                            }
                            tb.append(a[i]);
                        }
                    } else if (tagval.value instanceof float[]) {
                        float[] a = (float[]) tagval.value;
                        for (int i = 0; i < a.length; i++) {
                            if (i != 0) {
                                tb.append((byte) ',');
                            }
                            tb.append(a[i]);
                        }
                    } else if (tagval.value instanceof byte[]) {
                        byte[] a = (byte[]) tagval.value;
                        for (int i = 0; i < a.length; i++) {
                            if (i != 0) {
                                tb.append((byte) ',');
                            }
                            tb.append(a[i]);
                        }
                    } else {
                        tb.append(tagval.value.toString());
                    }
                    tb.append((byte) ']');
                } else {
                    tb.append(tagval.value.toString().replace(' ', '-').getBytes(Util.utf8Charset));
                }
            }
        }
        // Attempt to read the assembly information from the header of the record
        String assembly = samFileHeader.getSequenceDictionary().getSequence(0).getAttribute("AS");
        if (assembly != null) {
            // If found, the it is written into the attribute map of the alignment. Key "RB" is used since "AS" is already in use.
            assembly = (tb.length() == 0 ? "RB=" : " RB=") + assembly;
            tb.append(assembly.getBytes(Util.utf8Charset));
        } else if (chrOneLength > -1) {
            assembly = (tb.length() == 0 ? "RB=" : " RB=") + assemblyBasedOnChrOneLength();
            tb.append(assembly.getBytes(Util.utf8Charset));
        }
    }

    class SAMRecordRow extends Line {
        SAMRecord record;
        Function<SAMRecord, String>[] reverseStringMap;
        ToIntFunction<SAMRecord>[] reverseIntMap;

        SAMRecordRow(SAMRecord record, Function<SAMRecord, String>[] reverseStringMap, ToIntFunction<SAMRecord>[] reverseIntMap) {
            super();
            this.record = record;
            chrIdx = lookup.chrToId(record.getReferenceName());
            chr = lookup.chrToName(record.getReferenceName()); //chrIdx >= 0 ? lookup.idToName(chrIdx) : record.getReferenceName(); // Allways report chromosome name on choosen gor form, but fallback to reference name if id lookup failed
            pos = record.getAlignmentStart();

            this.reverseStringMap = reverseStringMap;
            this.reverseIntMap = reverseIntMap;
        }

        @Override
        public String toColString() {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < numCols(); i++) {
                line.append("(").append(colAsString(i)).append(") ");
            }
            return line.toString();
        }

        @Override
        public int colAsInt(int colNum) {
            return reverseIntMap[colNum].applyAsInt(record);
        }

        @Override
        public double colAsDouble(int colNum) {
            return (double) reverseIntMap[colNum].applyAsInt(record);
        }

        @Override
        public String colAsString(int colNum) {
            return reverseStringMap[colNum].apply(record);
        }

        @Override
        public String stringValue(int col) {
            return reverseStringMap[col].apply(record);
        }

        @Override
        public int intValue(int col) {
            return reverseIntMap[col].applyAsInt(record);
        }

        @Override
        public double doubleValue(int col) {
            return (double)reverseIntMap[col].applyAsInt(record);
        }

        @Override
        public String otherCols() {
            StringBuilder sb = new StringBuilder();
            otherCols(sb);
            return sb.toString();
        }

        private void otherCols(StringBuilder sb) {
            if (numCols() > 2) sb.append(reverseStringMap[2].apply(record));
            for (int i = 3; i < numCols(); i++) {
                sb.append('\t');
                sb.append(reverseStringMap[i].apply(record));
            }
        }

        @Override
        public CharSequence getAllCols() {
            StringBuilder sb = new StringBuilder();
            sb.append(chr);
            sb.append('\t');
            sb.append(pos);
            sb.append('\t');
            otherCols(sb);
            return sb;
        }

        @Override
        public int numCols() {
            return reverseStringMap.length;
        }

        @Override
        public int numOtherCols() {
            return reverseStringMap.length - 2;
        }

        private void appendColumn(StringBuilder sb, int i) {
            if (i == 0) sb.append(chr);
            else sb.append(reverseStringMap[i].apply(record));
        }

        @Override
        public String selectedColumns(int[] columnIndices) {
            StringBuilder sb = new StringBuilder();
            appendColumn(sb, columnIndices[0]);
            for (int i = 1; i < columnIndices.length; i++) {
                sb.append('\t');
                appendColumn(sb, columnIndices[i]);
            }
            return sb.toString();
        }

        @Override
        public int otherColsLength() {
            int total = numCols() - 3;
            for (int i = 2; i < numCols(); i++) {
                total += reverseStringMap[i].apply(record).length();
            }
            return total;
        }

        @Override
        public void addSingleColumnToRow(String rowString) {
            System.err.println("d");
        }

        @Override
        public Row slicedRow(int startCol, int stopCol) {
            return null;
        }

        @Override
        public Row rowWithSelectedColumns(int[] columnIndices) {
            Function<SAMRecord, String>[] revStringMap = new Function[columnIndices.length];
            ToIntFunction<SAMRecord>[] revIntMap = new ToIntFunction[columnIndices.length];
            for (int i = 0; i < columnIndices.length; i++) {
                revStringMap[i] = reverseStringMap[columnIndices[i]];
                revIntMap[i] = reverseIntMap[columnIndices[i]];
            }
            reverseStringMap = revStringMap;
            reverseIntMap = revIntMap;
            return this;
        }

        @Override
        public Row rowWithAddedColumn(CharSequence s) {
            return RowObj.apply(getAllCols() + "\t" + s);
        }

        @Override
        public int sa(int i) {
            return 0;
        }

        void resizeColumnMaps(int newsize) {
            int newlen = newsize;
            Function<SAMRecord, String>[] revStringMap = new Function[newlen];
            ToIntFunction<SAMRecord>[] revIntMap = new ToIntFunction[newlen];
            for (int i = 0; i < reverseStringMap.length; i++) {
                revStringMap[i] = reverseStringMap[i];
                revIntMap[i] = reverseIntMap[i];
            }
            reverseStringMap = revStringMap;
            reverseIntMap = revIntMap;
        }

        @Override
        public void resize(int newsize) {
            resizeColumnMaps(newsize);
        }

        @Override
        public void setColumn(int i, String val) {
            int idx = i + 2;
            reverseStringMap[idx] = (sr) -> val;
            reverseIntMap[idx] = (sr) -> Integer.parseInt(val);
        }

        @Override
        public void set(int i, String val) {
            setColumn(i, val);
        }

        @Override
        public void addColumns(int num) {
            resize(numCols() + num);
        }

        @Override
        public String toString() {
            return getAllCols().toString();
        }

        @Override
        public void writeRowToStream(OutputStream outputStream) throws IOException {
            outputStream.write(chr.getBytes());
            outputStream.write('\t');
            outputStream.write(String.valueOf(pos).getBytes());
            for (int i = 2; i < this.reverseStringMap.length; ++i) {
                outputStream.write('\t');
                outputStream.write(this.reverseStringMap[i].apply(record).getBytes());
            }
        }
    }

    public String getChromName() {
        String name;
        final int order = ChrDataScheme.ChrLexico.order2id[hgSeekIndex];
        if (chrnamesystem == 1) {
            // Must perform seek on next chromosome in lexico order and return value for that
            name = ChromoCache.getHgName(order);
        } else if (chrnamesystem == 2) {
            name = ChromoCache.getStdChrName(order);
        } else {
            throw new GorDataException("Unexpected chromosome name and ordering");
        }
        return name;
    }

    public SAMRecord record = null;

    @Override
    public boolean hasNext() {
        initIterator();
        boolean hasNext = it.hasNext();
        while (hasNext && (record = it.next()) != null && (record.getReadUnmappedFlag() || "*".equals(record.getCigarString()) || record.getStart() < pos)) {
            hasNext = it.hasNext();
        }
        if (!hasNext) {
            it.close();
            it = null;
            if (hgSeekIndex >= 0) { // Is seeking through differently ordered data
                while (++hgSeekIndex < ChrDataScheme.ChrLexico.order2id.length) {
                    String name = getChromName();
                    if (samFileHeader.getSequenceIndex(name) > -1) {
                        createIterator(name, 0);
                        return hasNext();
                    }
                }
            }
        }
        return hasNext;
    }

    @Override
    public Row next() {
        return new SAMRecordRow(record, defaultReverseStringMap, defaultReverseIntMap);
    }
}
