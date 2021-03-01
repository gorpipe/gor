/*
 * Copyright (c) 2021.  WuxiNextCODE Inc.
 *
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * WuxiNextCODE Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with WuxiNextCODE.
 */

package org.gorpipe.gor.table.dictionary;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.table.GenomicRange;
import org.gorpipe.gor.table.TableEntry;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

/**
 * Line from gor dictionary (GORD).
 * <p>
 * Created by gisli on 22/08/16.
 */
public class DictionaryRawEntry extends DictionaryEntry {

    private String rawLine;  // Line as stored on disk.
    private final RowParser rawColumns;
    private final RowParser rawFileInfo;

    private String contentRelative;              // Normalized URI as specified in the table (normalized and absolute or relative to the table).
    protected String[] tags = TableEntry.EMPTY_TAGS_LIST;                       // For performance use string array.
    protected GenomicRange range;

    private DictionaryRawEntry(String rawLine, URI rootUri) {
        super(null, rootUri, null, null, null, false, false);
        this.rawLine = rawLine;
        this.rawColumns = new RowParser(rawLine, '\t', 7, 0);
        this.rawFileInfo = new RowParser(rawColumns.getColumn(0), '|', 3, 0);
    }

    // Copy constructor.
    public DictionaryRawEntry(DictionaryRawEntry entry) {
        super(entry);

        this.rawLine = entry.rawLine;
        this.rawColumns = entry.rawColumns;
        this.rawFileInfo = entry.rawFileInfo;
    }

    /**
     * Parse entry from dict file.
     * Assumes the entry has been created by us, i.e. paths normalized etc.
     *
     * NOTE:  invoked through reflection.
     *
     * @param line          the line to parse.
     * @param rootUri       root URI to resolve relative paths.
     * @return new entry from the entryString
     */
    public static DictionaryRawEntry parseEntry(String line, URI rootUri) {
        return new DictionaryRawEntry(line, rootUri);
    }

    @Override
    public String getContentRelative() {
        if (contentRelative == null) {
            parseFileInfoLocal();
        }
        return contentRelative;
    }

    @Override
    public String[] getTags() {
        // Actually checking for object equivalence.
        if (tags == EMPTY_TAGS_LIST) {
            parseAliasAndTagsLocal();
        }
        return tags;
    }

    @Override
    public GenomicRange getRange() {
        if (range == null) {
            parseRangeLocal();
        }
        return range;
    }

    @Override
    public String getBucket() {
        if (bucketLogical == null) {
            parseFileInfoLocal();
        }
        return this.bucketLogical;
    }

    /**
     * Set the bucket logical path.
     *
     * @param bucket bucket file, normalized and relative to table or absolute.
     */
    @Override
    protected void setBucket(String bucket) {
        this.bucketLogical = bucket;
        this.rawLine = super.formatEntryNoNewLine();
    }

    @Override
    public final boolean isDeleted() {
        if (isDeleted == null) {
            parseFileInfoLocal();
        }
        return isDeleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
        this.rawLine = super.formatEntryNoNewLine();
    }

    private void parseRangeLocal() {
        if (rawColumns.hasColumn(2)) {
            if (rawColumns.hasColumn(5)) {
                this.range = GenomicRange.parseGenomicRange(rawColumns.getColumns(2, 6).toString());
            } else {
                throw new GorDataException("Error Intializing Query. Expected 4 columns for genomic range specification!");
            }
        } else {
            this.range = GenomicRange.EMPTY_RANGE;
        }
    }

    private void parseFileInfoLocal() {
        this.contentRelative = rawFileInfo.getColumn(0).toString();
        final CharSequence flags = rawFileInfo.getColumn(1);
        final boolean lineDeleted = flags != null && flags.length() > 0 && (flags.charAt(0) == 'D' || flags.charAt(0) == 'd');
        if (lineDeleted) {
            this.isDeleted = true;
        }
        final CharSequence bucketFileName = rawFileInfo.hasColumn(2) ? rawFileInfo.getColumn(2) : rawFileInfo.getColumn(1);
        this.bucketLogical = bucketFileName.toString();
    }

    private void parseAliasAndTagsLocal() {
        if (rawColumns.hasColumn(1)) {
            final String alias = rawColumns.getColumn(1).toString();

            if (!rawColumns.hasColumn(6)) {
                if (!alias.isEmpty()) {
                    this.tags = new String[1];
                    this.tags[0] = alias;
                }
            } else {
                // support both comma separadted and tab separated tags
                String tagsString = rawColumns.getColumn(6).toString();
                if (tagsString.indexOf(',') >= 0) {
                    this.tags = tagsString.split(",", -1);
                } else {
                    this.tags = tagsString.split("\t", -1);
                }
            }
        }
    }

    @Override
    public String formatEntryNoNewLine() {
        return rawLine; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DictionaryRawEntry)) return false;
        if (!super.equals(o)) return false;
        DictionaryRawEntry that = (DictionaryRawEntry) o;
        return Objects.equals(rawLine, that.rawLine) &&
                Objects.equals(rawColumns, that.rawColumns) &&
                Objects.equals(rawFileInfo, that.rawFileInfo) &&
                Objects.equals(contentRelative, that.contentRelative) &&
                Arrays.equals(tags, that.tags) &&
                Objects.equals(range, that.range);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), rawLine, rawColumns, rawFileInfo, contentRelative, range);
        result = 31 * result + Arrays.hashCode(tags);
        return result;
    }

    private static class RowParser {
        private final CharSequence sequence;
        private final int[] splits;   // Location of delimters (the first, which is imaginary one, is located at -1).

        private RowParser(CharSequence sequence, char splitter, int maxSplits, int offset) {
            this.sequence = sequence;
            this.splits = findSplits(sequence, splitter, maxSplits, offset);
        }

        private CharSequence getColumn(int column) {
            return hasColumn(column) ? sequence.subSequence(splits[column] + 1, splits[column + 1]) : "";
        }

        /**
         * Get char sequence for column range.
         * @param start start column inclusive
         * @param stop  stop column exclusive
         * @return the charSequence for column start to stop - 1;
         */
        public CharSequence getColumns(int start, int stop) {
            return hasColumn(stop - 1) ? sequence.subSequence(splits[start] + 1, splits[stop]) : "";
        }

        private boolean hasColumn(int column) {
            return splits[column + 1] != 0;
        }

        private static int[] findSplits(CharSequence sequence, char splitter, int maxSplits, int offset) {
            int[] splits = new int[maxSplits + 1];
            splits[0] = offset - 1;   // -1 as we add 1 (pass the delimiter) when reading start of segment.
            int splitIndex = 1;
            for (int i = 0; i < sequence.length() && splitIndex < maxSplits; i++) {
                if (sequence.charAt(i) == splitter) {
                    splits[splitIndex++] = i + offset;
                }
            }
            splits[splitIndex] = sequence.length() + offset;
            return splits;
        }
    }

}
