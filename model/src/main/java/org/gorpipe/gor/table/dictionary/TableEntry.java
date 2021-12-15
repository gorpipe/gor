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

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.util.GenomicRange;
import org.gorpipe.gor.table.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.gorpipe.gor.table.util.PathUtils.isLocal;
import static org.gorpipe.gor.table.util.PathUtils.resolve;

/**
 * Class that represent one table entry.
 * <p>
 * Created by gisli on 22/08/16.
 */
public abstract class TableEntry {

    private static final Logger log = LoggerFactory.getLogger(TableEntry.class);

    protected static final String[] EMPTY_TAGS_LIST = new String[]{};

    // Use strings for performance reasons (using Path or URI takes twice as long to parse).
    private final String contentRelative;              // Normalized URI as specified in the table (normalized and absolute or relative to the table).
    private final String alias;
    private final String[] tags;                       // For performance use string array.
    private String[] filterTags;                       // Caching this.
    private final GenomicRange range;

    private final URI rootUri;                // Root for relative paths (parent folder of the table).  Absolute path.
    protected String key;                     // Unique key for the entry.
    private int indexOrderKey;                // Use to get correct order when getting values from hash maps.

    // Copy constructor.
    TableEntry(TableEntry entry) {
        this.rootUri = entry.getRootUri();
        this.contentRelative = entry.contentRelative;
        this.alias = entry.alias;
        this.tags = entry.getTags();
        this.range = entry.getRange();
        this.key = entry.getKey();
        this.indexOrderKey = entry.indexOrderKey;
    }

    /**
     * Create new table entry.
     *
     * @param contentLogical path to the content file, absolute or relative to the table root.  Normalized.
     *                       Use table.normalize(table.relativ)
     * @param rootUri        the parent folder of the table we are adding to.  Absolute path.
     * @param alias          alias
     * @param tags           tags associated with the entry.
     * @param range          range for the entry.
     */
     TableEntry(String contentLogical, URI rootUri, String alias, String[] tags, GenomicRange range) {
        assert rootUri != null;

        this.rootUri = rootUri;
        this.contentRelative = contentLogical;
        this.alias = alias;
        this.tags = tags != null ? tags : EMPTY_TAGS_LIST;
        this.range = range != null ? range : GenomicRange.EMPTY_RANGE;
    }

    /**
     * Get unique key for the entry.
     * NOTE: If they fields used to generate the key are changed then the entries must be deleted and reinserted.
     */
    public String getKey() {
        if (key == null) {
            String newKey = this.getContentRelative();
            newKey += String.join(",", getFilterTags());

            if (getRange() != GenomicRange.EMPTY_RANGE) {
                newKey += getRange().toString();
            }

            key = newKey;
        }
        return key;
    }

    /**
     * Hash code used to search speed up, not unique for the entry.
     *
     * @return non-unique hash code.
     */
    public int getSearchHash() {
        return getContentRelative().hashCode();
    }
    
    public String formatEntry() {
        return formatEntryNoNewLine() + "\n";
    }
    
    public abstract String formatEntryNoNewLine();

    public URI getRootUri() {
        return rootUri;
    }

    public String getContentRelative() {
        return contentRelative;
    }

    public String getContentReal() {
        return resolve(getRootUri(), getContentRelative());
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

    public GenomicRange getRange() {
        return range;
    }

    /**
     * Correctly combine tags and the alias for filtering (in most cases should be used rather than getTags or getAlias)
     */
    public String[] getFilterTags() {
        if (filterTags == null) {
            String[] tags = getTags();
            if (tags != null && tags.length > 0) {
                filterTags = tags;
            } else if (alias != null) {
                filterTags = new String[]{alias};
            } else {
                filterTags = EMPTY_TAGS_LIST;
            }
        }
        return filterTags;
    }

    // For backward compatibility.

    /**
     * Get content/file logical path.
     *
     * @param commonRoot the common root
     * @return logcial path relative to commonRoot or absolute if path is not part of the common root.
     */
    public String getLogical(String commonRoot) {
        return PathUtils.formatUri(commonRoot != null ? getCommonRootRelative(getContentReal(), commonRoot) : getContentReal());
    }

    // For backward compatibility.

    /**
     * Get content/file pysycial path.
     * Pysical path is the real path of the content/file.
     *
     * @param commonRoot the common root
     * @return physical path relative to commonRoot or absolute (real path) if path is not part of the common root.
     */
    public String getPhysical(String commonRoot) {
        URI commonRelativePath = URI.create(commonRoot != null ? getCommonRootRelative(getContentReal(), commonRoot) : getContentReal());
        return PathUtils.formatUri(PathUtils.toRealPath(commonRelativePath));
    }

    /**
     * Find relative path to common root (or absolute if it does not start with common root).
     *
     * @param resolvedPath resolvedPath (absolute)
     * @param commonRoot   commmon root (not null).
     * @return path to common root (or absolute if it does not start with common root)
     */
    private String getCommonRootRelative(String resolvedPath, String commonRoot) {
        if (resolvedPath == null || !isLocal(resolvedPath)) {
            return resolvedPath;
        }

        URI commonRootReal = URI.create(resolve(getRootUri(), commonRoot));

        if (resolvedPath.startsWith(commonRootReal.toString())) {
            return commonRootReal.relativize(URI.create(resolvedPath)).toString();
        } else {
            return resolvedPath;
        }
    }

    // For backward compatibility.
    public boolean isAcceptedAbsoluteRef() {
        // TODO:  We are only handling top level links here, old impl handles lower level links.
        String contentReal = getContentReal();
        return !isLocal(contentReal) || Files.isSymbolicLink(Paths.get(contentReal));
    }

    public int getIndexOrderKey() {
        return indexOrderKey;
    }

    public void setIndexOrderKey(int l) {
        this.indexOrderKey = l;
    }

    public static TableEntry copy(TableEntry template) {
        try {
            Class<? extends TableEntry> clazz = template.getClass();
            Constructor<? extends TableEntry> constructor = clazz.getDeclaredConstructor(clazz);
            constructor.setAccessible(true);
            return constructor.newInstance(template);
        } catch (Exception e) {
            throw new GorSystemException("Could not copy table entry", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableEntry)) return false;

        TableEntry entry = (TableEntry) o;

        return this.getKey().equals(entry.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getKey());
    }

    public abstract static class Builder<B extends Builder> {
        protected String contentLogical;
        protected URI rootUri;
        protected String alias;
        protected List<String> tags = new ArrayList<>();
        protected GenomicRange range;
        protected boolean contentVerified = false;

        /**
         * Create entry builder.
         *
         * @param contentLogical entry content, absolute or relative path to the current dir..
         * @param rootUri        root URI for the table entry.
         */
        public Builder(String contentLogical, URI rootUri) {
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

        public B range(GenomicRange range) {
            this.range = range;
            return self();
        }

        public B contentVerified() {
            this.contentVerified = true;
            return self();
        }

        public abstract TableEntry build();

    }
}
