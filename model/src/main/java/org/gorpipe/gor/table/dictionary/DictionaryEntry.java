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

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Splitter;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.table.BucketableTableEntry;
import org.gorpipe.gor.table.GenomicRange;
import org.gorpipe.gor.table.PathUtils;
import org.gorpipe.util.string.StringUtil;

import static org.gorpipe.gor.table.PathUtils.fixFileSchema;
import static org.gorpipe.gor.table.PathUtils.relativize;

/**
 * Line from gor dictionar (GORD).
 * <p>
 * Created by gisli on 22/08/16.
 */
public class DictionaryEntry extends BucketableTableEntry {

    private boolean sourceInserted;

    // Copy constructor.
    public DictionaryEntry(DictionaryEntry entry) {
        super(entry);
        this.sourceInserted = entry.sourceInserted;
    }

    protected DictionaryEntry(String contentLogical, URI rootUri, String[] tags, GenomicRange range, String bucket, boolean isDeleted, boolean sourceInserted) {
        super(contentLogical, rootUri, tags, range, bucket, isDeleted);
        this.sourceInserted = sourceInserted;
    }


    private static final Splitter tabSplitter = Splitter.on('\t');
    private static final Splitter pipeSplitter = Splitter.on('|');
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
    public static DictionaryEntry parseEntry(String line, URI rootUri) {
        List<String> columns = tabSplitter.splitToList(line);
        return parseEntry(columns, rootUri);
    }
    
    public static DictionaryEntry parseEntry(List<String> columns, URI rootUri) {
        if (columns.isEmpty()) {
            return null;
        }

        final List<String> fileInfo = pipeSplitter.splitToList(columns.get(0).replace('\\', '/'));
        Builder<DictionaryEntry.Builder> builder = new Builder<>(fixFileSchema(fileInfo.get(0)), rootUri).contentVerified();

        final String flags = fileInfo.size() > 2 ? fileInfo.get(1) : null;
        final boolean lineDeleted = flags != null && flags.toLowerCase().contains("d");
        final String bucketFileName = fileInfo.size() > 2 ? fileInfo.get(2) : (fileInfo.size() > 1 ? fileInfo.get(1) : null);
        final String alias = columns.size() > 1 ? columns.get(1) : null;

        builder.bucket(bucketFileName);

        if (lineDeleted) {
            builder.deleted();
        }

        if (columns.size() > 2) {
            if (columns.size() >= 6) {
                GenomicRange range = GenomicRange.parseGenomicRange(String.join("\t", columns.subList(2, 6)));
                builder.range(range);

                if (columns.size() < 7) {
                    builder.tags(Collections.singletonList(alias));
                } else {
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
        } else {
            // Add alias as tag on the file, if alias is
            builder.alias(alias);
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
        if (getTags() != null && getTags().length == 1) {
            sb.append(getAliasTag() != null ? getAliasTag() : "");
        }

        // Range columns
        sb.append('\t');
        getRange().format(sb);

        // Tags (skip if only contains the alias)
        sb.append('\t');
        if (getTags() != null && getTags().length > 1) {
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
        return sourceInserted == that.sourceInserted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceInserted);
    }

    public static class Builder<B extends Builder> extends BucketableTableEntry.Builder<Builder<B>> {
        private boolean sourceInserted;

        public Builder(Path contentLogical, URI rootUri) {
            this(contentLogical.toString(), rootUri);
        }

        public Builder(String contentLogical, URI rootUri) {
            super(contentLogical, rootUri);
        }

        public Builder<B> sourceInserted(boolean val) {
            this.sourceInserted = val;
            return self();
        }

        public Builder<B> alias(String val) {
            if (val != null && val.length() > 0) {
                this.tags.add(0, val);
            }
            return self();
        }

        @Override
        public DictionaryEntry build() {
            return new DictionaryEntry(contentVerified ? contentLogical : relativize(rootUri, contentLogical), rootUri, tags != null ? tags.toArray(new String[0]) : null, range,
                    contentVerified ? bucketLogical : relativize(rootUri, bucketLogical), isDeleted, sourceInserted);
        }

    }
}
