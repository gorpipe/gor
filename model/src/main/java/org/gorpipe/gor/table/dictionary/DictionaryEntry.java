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
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.gorpipe.gor.table.util.PathUtils.relativize;
import static org.gorpipe.gor.table.util.PathUtils.resolve;

/**
 * Class that represent one table entry.
 * <p>
 * Created by gisli on 22/08/16.
 */
public class DictionaryEntry {

    private static final Logger log = LoggerFactory.getLogger(DictionaryEntry.class);

    protected static final String[] EMPTY_TAGS_LIST = new String[]{};

    public static final DictionaryEntry dummy = new DictionaryEntry("dummy", "dummy", "dummy", new String[]{}, "dummy", false, false);

    // Use strings for performance reasons (using Path or URI takes twice as long to parse).
    private final String contentRelative;              // Normalized URI as specified in the table (normalized and absolute or relative to the table).
    private final String alias;
    private final String[] tags;                       // For performance use string array.
    protected String bucketLogical;
    protected boolean isDeleted;

    private final boolean sourceInserted;


    // Copy constructor.
    public DictionaryEntry(DictionaryEntry entry) {
        this.contentRelative = entry.contentRelative;
        this.alias = entry.alias;
        this.tags = entry.getTags();

        this.sourceInserted = entry.sourceInserted;
        this.bucketLogical = entry.bucketLogical;
        this.isDeleted = entry.isDeleted;

    }

    /**
     * Create new table entry.
     *
     * @param contentLogical path to the content file, absolute or relative to the table root.  Normalized.
     *                       Use table.normalize(table.relativ)
     * @param rootUri        the parent folder of the table we are adding to.  Absolute path.
     * @param alias          alias
     * @param tags           tags associated with the entry.
     * @param bucket         bucket file, normalized and relative to table or absolute.
     * @param isDeleted      true if the entry is deleted.
     * @param sourceInserted
     */
     public DictionaryEntry(String contentLogical, String rootUri, String alias, String[] tags, String bucket, boolean isDeleted, boolean sourceInserted) {
        assert rootUri != null;

        this.contentRelative = contentLogical;
        this.alias = alias;
        this.tags = tags != null ? tags : EMPTY_TAGS_LIST;

        this.sourceInserted = sourceInserted;
        this.bucketLogical = bucket;
        this.isDeleted = isDeleted;

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

        DictionaryEntry.Builder<DictionaryEntry.Builder> builder = new DictionaryEntry.Builder<>(file, rootUri).needsRelativize(needsRelativize);
        builder.bucket(bucketFileName).alias(alias);

        if (lineDeleted) {
            builder.deleted();
        }

        if (columns.size() > 2) {
            if (columns.size() == 3) {
                // support both comma separadted and tab separated tags
                if (columns.get(2).indexOf(',') >= 0) {
                    builder.tags(StringUtil.split(columns.get(2), ','));
                } else {
                    builder.tags(columns.subList(2, columns.size()));
                }
            }
        }

        return builder.build();
    }

    public static DictionaryEntry.Builder getBuilder(String contentLogical, String rootUri) {
        return new Builder(contentLogical, rootUri);
    }

    public boolean isSourceInserted() {
        return sourceInserted;
    }

    /**
     * Get unique key for the entry.
     * NOTE: If they fields used to generate the key are changed then the entries must be deleted and reinserted.
     */
    public String getKey() {
        // We keep deleted entries around for the the bucket (to know what to exclude).
        // So for each deleted entry we need to add the bucket to the key.
        var key = this.contentRelative + ":" + alias;
        if (isDeleted()) {
            return key + getBucket();
        } else {
            return key;
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
    public void setBucket(String bucket) {
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
     * Hash code used to search speed up, not unique for the entry.
     *
     * @return non-unique hash code.
     */
    public int getSearchHash() {
        return contentRelative.hashCode();
    }
    
    public String formatEntry() {
        return formatEntryNoNewLine() + "\n";
    }

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


    public String getContentRelative() {
        return contentRelative;
    }

    public String getContentReal(String rootUri) {
        return resolve(rootUri, getContentRelative());
    }

    public String getContent() {
        return getContentRelative();
    }

    public String getAlias() {
        return alias;
    }

    public String[] getTags() {
        return tags;
    }

    /**
     * Correctly combine tags and the alias for filtering (in most cases should be used rather than getTags or getAlias)
     */
    public String[] getFilterTags() {
        String[] filterTags;
        if (tags != null && tags.length > 0) {
            filterTags = tags;
        } else if (alias != null) {
            filterTags = new String[]{alias};
        } else {
            filterTags = EMPTY_TAGS_LIST;
        }
        return filterTags;
    }

    public static DictionaryEntry copy(DictionaryEntry template) {
        try {
            Class<? extends DictionaryEntry> clazz = template.getClass();
            Constructor<? extends DictionaryEntry> constructor = clazz.getDeclaredConstructor(clazz);
            constructor.setAccessible(true);
            return constructor.newInstance(template);
        } catch (Exception e) {
            throw new GorSystemException("Could not copy table entry", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DictionaryEntry)) return false;

        DictionaryEntry entry = (DictionaryEntry) o;

        return this.getKey().equals(entry.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getKey());
    }

    @Override
    public String toString() {
        return "DictionaryEntry{" + toStringFields() + '}';
    }

    protected String toStringFields() {
        return "contentRelative='" + contentRelative + '\'' +
                ", alias='" + alias + '\'' +
                ", tags=" + Arrays.toString(tags) +
                ", bucketLogical='" + bucketLogical + '\'' +
                ", sourceInserted=" + sourceInserted +
                ", isDeleted=" + isDeleted;
    }

    public static class Builder<B extends Builder> {
        protected String contentLogical;
        protected String rootUri;
        protected String alias;
        protected List<String> tags = new ArrayList<>();
        protected String bucketLogical = null;
        protected boolean isDeleted = false;
        protected boolean needsRelativize = true;

        protected boolean sourceInserted = false;

        /**
         * Create entry builder.
         *
         * @param contentLogical entry content, absolute or relative path to the current dir..
         * @param rootUri        root URI for the table entry.
         */
        public Builder(String contentLogical, String rootUri) {
            this.contentLogical = contentLogical;
            this.rootUri = rootUri;
        }

        @SuppressWarnings("unchecked")
        protected final B self() {
            return (B) this;
        }

        public B alias(String alias) {
            this.alias = alias;
            return self();
        }

        public B tags(List<String> tags) {
            this.tags.addAll(tags.stream().filter(t -> t != null && t.length() > 0).collect(Collectors.toList()));
            return self();
        }

        public B tags(String[] tags) {
            this.tags.addAll(Arrays.stream(tags).filter(t -> t != null && t.length() > 0).collect(Collectors.toList()));
            return self();
        }

        public B bucket(String bucketLogical) {
            this.bucketLogical = bucketLogical;
            return self();
        }

        public B deleted() {
            this.isDeleted = true;
            return self();
        }

        public B sourceInserted(boolean val) {
            this.sourceInserted = val;
            return self();
        }

        public B needsRelativize(boolean val) {
            this.needsRelativize = val;
            return self();
        }

        public DictionaryEntry build() {
            return new DictionaryEntry(!needsRelativize ? contentLogical : relativize(rootUri, contentLogical), rootUri, alias, tags != null ? tags.toArray(new String[0]) : null,
                    !needsRelativize ? bucketLogical : relativize(rootUri, bucketLogical), isDeleted, sourceInserted);
        }


    }
}
