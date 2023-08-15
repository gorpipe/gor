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

package org.gorpipe.gor.table.dictionary;

import com.google.common.base.Strings;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.table.util.GenomicRange;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.StringUtil;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.gorpipe.gor.table.util.PathUtils.relativize;

/**
 * Line from gor dictionary (GORD).
 * <p>
 * Created by gisli on 22/08/16.
 * TODO:  Dictinoary entry and SourceRef are almost the samething. DictionaryEntry is basically source ref stored in dict.  Can we combine.  What about SourceReference.
 */
public class DictionaryEntry extends TableEntry {

    private final boolean sourceInserted;

    // Use strings for performance reasons (using Path or URI takes twice as long to parse).
    protected String bucketLogical;
    protected boolean isDeleted;

    // Copy constructor.
    public DictionaryEntry(DictionaryEntry entry) {
        super(entry);
        this.sourceInserted = entry.sourceInserted;
        this.bucketLogical = entry.bucketLogical;
        this.isDeleted = entry.isDeleted;
    }

    protected DictionaryEntry(String contentLogical, String rootUri, String alias, String[] tags, GenomicRange range, String bucket, boolean isDeleted, boolean sourceInserted) {
        super(contentLogical, rootUri, alias, tags, range);
        this.sourceInserted = sourceInserted;
        this.bucketLogical = bucket;
        this.isDeleted = isDeleted;
    }

    public boolean isSourceInserted() {
        return sourceInserted;
    }

    /**
     * Get unique key for the entry.
     * NOTE: If they fields used to generate the key are changed then the entries must be deleted and reinserted.
     */
    @Override
    public String getKey() {
        // We keep deleted entries around for the the bucket (to know what to exclude).
        // So for each deleted entry we need to add the bucket to the key.
        if (isDeleted()) {
            return super.getKey() + getBucket();
        } else {
            return super.getKey();
        }
    }

    /**
     * Get the bucket (relative path)
     *
     * @return  the bucket.
     */
    public String getBucket() {
        return this.bucketLogical;
    }

    /**
     * Get the buckets real path.
     *
     * @return the buckets real path.
     */
    public String getBucketReal(String rootUri) {
        return getBucket() != null ? PathUtils.resolve(rootUri, getBucket()) : null;
    }

    /**
     * Set the bucket logical path.
     *
     * @param bucket bucket file, normalized and relative to table or absolute.
     */
    protected void setBucket(String bucket) {
        this.bucketLogical = bucket;
    }

    public boolean hasBucket() {
        return !Strings.isNullOrEmpty(getBucket());
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
    }

    /**
     * Parse entry from dict file.
     * Assumes the entry has been created by us, i.e. paths normalized etc.
     * NOTE:  invoked through reflection.
     *
     * @param line          the line to parse.
     * @param rootUri       root URI to resolve relative paths.
     * @param needsRelativize should we relatives the content (only needed if reading from a outside source)
     * @return new entry from the entryString
     */
    public static DictionaryEntry parseEntry(String line, String rootUri, boolean needsRelativize) {
        List<String> columns = StringUtil.split(line);
        return parseEntry(columns, rootUri, needsRelativize);
    }

    public static DictionaryEntry parseEntry(String line, String rootUri) {
        List<String> columns = StringUtil.split(line);
        return parseEntry(columns, rootUri, false);
    }

    public static DictionaryEntry parseEntry(List<String> columns, String rootUri, boolean needsRelativize) {
        if (columns.isEmpty()) {
            return null;
        }

        final int indexOf = columns.get(0).indexOf('|');
        final String file = indexOf > 0 ? columns.get(0).substring(0, indexOf) : columns.get(0);
        final String flags = indexOf > 0 && columns.get(0).length() - indexOf > 2 && columns.get(0).charAt(indexOf + 2) == '|' ?
                String.valueOf( columns.get(0).charAt(indexOf + 1) ) : null;
        final String bucketFileName = indexOf > 1 ? (flags != null ? columns.get(0).substring(indexOf + 3) : columns.get(0).substring(indexOf + 1)) : null;

        final boolean lineDeleted = flags != null && flags.toLowerCase().contains("d");
        final String alias = columns.size() > 1 ? columns.get(1) : null;

        Builder<DictionaryEntry.Builder> builder = (DictionaryEntry.Builder)new Builder<>(file, rootUri).needsRelativize(needsRelativize);
        builder.bucket(bucketFileName).alias(alias);

        if (lineDeleted) {
            builder.deleted();
        }

        if (columns.size() > 2) {
            if (columns.size() >= 6) {
                GenomicRange range = GenomicRange.parseGenomicRange(String.join("\t", columns.subList(2, 6)));
                builder.range(range);

                if (columns.size() == 7) {
                    // support both comma separadted and tab separated tags
                    if (columns.get(6).indexOf(',') >= 0) {
                        builder.tags(StringUtil.split(columns.get(6), ','));
                    } else {
                        builder.tags(columns.subList(6, columns.size()));
                    }
                }
            } else {
                throw new GorDataException("Error initializing query. Expected 4 columns for genomic range specification!");
            }
        }

        return builder.build();
    }

    @Override
    public String formatEntryNoNewLine() {
        StringBuilder sb = new StringBuilder();

        // File column
        sb.append(PathUtils.formatUri(getContentRelative()));
        if (hasBucket()) {
            if (isDeleted()) {
                sb.append("|D");
            }
            sb.append('|');
            sb.append(getBucket());
        }

        // Alias column
        sb.append('\t');
        sb.append(getAlias() != null ? getAlias() : "");

        // Range columns
        sb.append('\t');
        getRange().formatAsTabDelimited(sb);

        sb.append('\t');
        if (getTags() != null && getTags().length > 0) {
            sb.append(String.join(",", getTags()));
        }

        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DictionaryEntry)) return false;
        if (!super.equals(o)) return false;
        DictionaryEntry that = (DictionaryEntry) o;
        // We only include the bucket if the entry is deleted (do keep track of delete entries).
        return this.isDeleted == that.isDeleted
                && (this.isDeleted ? Objects.equals(this.bucketLogical, that.bucketLogical) : true);
    }

    @Override
    public String toString() {
        return "DictionaryEntry{" + toStringFields() + '}';
    }

    protected String toStringFields() {
        return super.toStringFields() +
                ", sourceInserted=" + sourceInserted +
                ", bucketLogical='" + bucketLogical + '\'' +
                ", isDeleted=" + isDeleted;
    }

    public static class Builder<B extends TableEntry.Builder> extends TableEntry.Builder<Builder<B>> {
        protected String bucketLogical = null;
        protected boolean isDeleted = false;
        private boolean sourceInserted = false;

        public Builder(String contentLogical, String rootUri) {
            super(contentLogical, rootUri);
        }

        public Builder(Path contentLogical, String rootUri) {
            super(contentLogical.toString(), rootUri);
        }

        public Builder<B> bucket(String bucketLogical) {
            this.bucketLogical = bucketLogical;
            return self();
        }

        public Builder<B> deleted() {
            this.isDeleted = true;
            return self();
        }

        public Builder<B> sourceInserted(boolean val) {
            this.sourceInserted = val;
            return self();
        }

        @Override
        public DictionaryEntry build() {
            return new DictionaryEntry(!needsRelativize ? contentLogical : relativize(rootUri, contentLogical), rootUri, alias, tags != null ? tags.toArray(new String[0]) : null, range,
                    !needsRelativize ? bucketLogical : relativize(rootUri, bucketLogical), isDeleted, sourceInserted);
        }
    }
}
