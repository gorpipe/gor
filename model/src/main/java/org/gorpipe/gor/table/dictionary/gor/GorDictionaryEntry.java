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

package org.gorpipe.gor.table.dictionary.gor;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
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
public class GorDictionaryEntry extends DictionaryEntry {

    public static final GorDictionaryEntry dummy = new GorDictionaryEntry("dummy", "dummy", "dummy", new String[]{}, GenomicRange.EMPTY_RANGE, "dummy", false, false);

    // Use strings for performance reasons (using Path or URI takes twice as long to parse).
    private final GenomicRange range;

    // Copy constructor.
    public GorDictionaryEntry(GorDictionaryEntry entry) {
        super(entry);
        this.range = entry.getRange();
    }

    /**
     *
     * @param contentLogical
     * @param rootUri
     * @param alias
     * @param tags
     * @param range          range for the entry.

     */
    protected GorDictionaryEntry(String contentLogical, String rootUri, String alias, String[] tags, GenomicRange range, String bucketLogical, boolean isDeleted, boolean sourceInserted) {
        super(contentLogical, rootUri, alias, tags, bucketLogical, isDeleted, sourceInserted);

        this.range = range != null ? range : GenomicRange.EMPTY_RANGE;
    }

    public GenomicRange getRange() {
        return range;
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
    public static GorDictionaryEntry parseEntry(String line, String rootUri, boolean needsRelativize) {
        List<String> columns = StringUtil.split(line);
        return parseEntry(columns, rootUri, needsRelativize);
    }

    public static GorDictionaryEntry parseEntry(String line, String rootUri) {
        List<String> columns = StringUtil.split(line);
        return parseEntry(columns, rootUri, false);
    }

    public static GorDictionaryEntry parseEntry(List<String> columns, String rootUri, boolean needsRelativize) {
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

        Builder<GorDictionaryEntry.Builder> builder = (GorDictionaryEntry.Builder)new Builder<>(file, rootUri).needsRelativize(needsRelativize);
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

        String line = sb.toString();
        if (line.endsWith("\t\t\t\t")) {
            line = line.trim();
        }
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GorDictionaryEntry)) return false;
        if (!super.equals(o)) return false;
        GorDictionaryEntry that = (GorDictionaryEntry) o;
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
                ", range=" + range.toString();
    }

    public static class Builder<B extends DictionaryEntry.Builder> extends DictionaryEntry.Builder<Builder<B>> {
        protected GenomicRange range;

        public Builder(String contentLogical, String rootUri) {
            super(contentLogical, rootUri);
        }

        public Builder(Path contentLogical, String rootUri) {
            super(contentLogical.toString(), rootUri);
        }

        public GorDictionaryEntry.Builder<B> range(GenomicRange range) {
            this.range = range;
            return self();
        }


        @Override
        public GorDictionaryEntry build() {
            return new GorDictionaryEntry(!needsRelativize ? contentLogical : relativize(rootUri, contentLogical), rootUri, alias, tags != null ? tags.toArray(new String[0]) : null, range,
                    !needsRelativize ? bucketLogical : relativize(rootUri, bucketLogical), isDeleted, sourceInserted);
        }
    }
}
