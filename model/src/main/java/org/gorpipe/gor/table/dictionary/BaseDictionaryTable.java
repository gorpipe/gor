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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.*;
import org.gorpipe.gor.table.util.GenomicRange;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.ByteTextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gorpipe.gor.table.util.PathUtils.*;

/**
 * Base class for tables and dictionaries.
 * <p>
 * Created by gisli on 25/07/16.
 *
 */
public abstract class BaseDictionaryTable<T extends BucketableTableEntry> extends BaseTable<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseDictionaryTable.class);

    public static final String DEFAULT_SOURCE_COLUMN = "Source";

    private String sourceColumn = DEFAULT_SOURCE_COLUMN;     // Name of the files tag column (source column).

    private boolean hasUniqueTags = false;    //True if we don't want to allow double entries of the same tag

    private Boolean bucketize = null;
    private String contentType = null;

    protected ITableEntries<T> tableEntries;

    protected BaseDictionaryTable(Builder builder) {
        super(builder);

        if (builder.sourceColumn != null) {
            setSourceColumn(builder.sourceColumn);
        }

        if (builder.uniqueTags != null) {
            setUniqueTags(builder.uniqueTags);
        }

        // Loads the header, and loads/initializes fields read from header.
        reload();
    }

    /**
     * Main constructor.
     *
     * @param uri              path to the dictionary file.
     */
    protected BaseDictionaryTable(URI uri, FileReader inputFileReader) {
        super(uri, inputFileReader);

        this.tableEntries = createTableEntries();

        // Loads the header, and loads/initializes fields read from header.
        reload();
    }

    protected BaseDictionaryTable(URI uri) {
        this(uri, null);
    }

    protected abstract ITableEntries<T> createTableEntries();

    @Override
    public void setColumns(String[] columns) {
        if (this.tableEntries.size() > 0 && columns.length != this.header.getColumns().length) {
            throw new GorDataException("Invalid columns - " + String.format("New columns length (%d) does not fit current data column count (%d)",
                    columns.length, this.header.getColumns().length));
        }
        super.setColumns(columns);
    }

    public String getSourceColumn() {
        return this.sourceColumn;
    }

    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }

    public boolean isBucketizeSet() {
        return bucketize != null;
    }

    public boolean isBucketize() {
        return isBucketizeSet() ? bucketize : BooleanUtils.isTrue(inferShouldBucketizeFromContent());
    }

    public void setBucketize(boolean bucketize) {
        this.bucketize = bucketize;
    }

    public Boolean getLineFilter() {
        return Boolean.valueOf(header.getProperty(DictionaryTableMeta.HEADER_LINE_FILTER_KEY, "true"));
    }

    public void setLineFilter(Boolean lineFilter) {
        header.setProperty(DictionaryTableMeta.HEADER_LINE_FILTER_KEY, lineFilter.toString());
    }

    public boolean isHasUniqueTags() {
        return this.hasUniqueTags;
    }

    public void setUniqueTags(boolean hasUniqueTags){
        this.hasUniqueTags = hasUniqueTags;
    }

    /**
     * Select the union of the rows defined by the given filters.
     *
     * @param filters filters to filter rows by.
     * @return union of rows as specified by the given filters.
     */
    public final List<T> selectUninon(TableFilter... filters) {
        return Arrays.stream(filters).flatMap(f -> f.get().stream()).collect(Collectors.toList());
    }

    /**
     * Selects all the rows, including the deleted rows.
     *
     * @return list with all the rows including the deleted rows.
     */
    public final List<T> selectAll() {
        // Copy the list but not the elements, so it can be used as input into for example delete.
        return new ArrayList<>(getEntries());
    }

    /**
     * Creates new filter on this table.
     * Filter does NOT by default include deleted rows.
     *
     * @return new filter on this table.
     */
    public TableFilter filter() {
        return new TableFilter();
    }

    /**
     * Get optimized lines.
     *
     * @param columnTags         map of column nr to a list of tags to filter the column on.
     *                           Seems to only set for -f -ff options ( and then uses col 3)
     * @param allowBucketAccess  can the optimizer use buckets.
     * @return                   optimzed list of files to data for the given tags.
     */
    protected abstract List<? extends T> getOptimizedLines(Map<Integer, Set<String>> columnTags, boolean allowBucketAccess);

    public List<T> getEntries() {
        return tableEntries.getEntries();
    }

    public List<T> getEntries(String... aliasesAndTags) {
        return tableEntries.getEntries(aliasesAndTags);
    }

    /**
     * Get all buckets in the table.
     *
     * @return List of Path elements representing all the buckets in the table.
     */
    public List<String> getBuckets() {
        return filter().get().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(BucketableTableEntry::getBucket).distinct().collect(Collectors.toList());
    }

    @Override
    public Stream<String> getLines() {
        return getEntries().stream().map(l -> l.formatEntryNoNewLine());
    }

    public void insert(Collection<T> lines) {
        int count = 0;
        for (T line : lines) {
            count++;
            if (count % 1000 == 0) {
                log.info("Inserting line {} of {}", count, lines.size());
            }
            // Validate the new file.
            if (isValidateFiles()) {
                validateFile(line.getContentReal());
            }

            this.tableEntries.insert(line, isHasUniqueTags());
            logAfter(TableLog.LogAction.INSERT, "", line.formatEntryNoNewLine());
        }
    }

    @Override
    public void insert(String... lines) {
        List<T> entries = lineStringsToEntries(lines);
        insert(entries);
    }

    /**
     * @param data map with alias to files, to be add to the dictionary.  The files must be normalized and either absolute or
     *             relative to the dictionary root.
     */
    public abstract void insert(Map<String, List<String>> data);


    @Override
    public void delete(Collection<T> lines) {
        for (T line : lines) {
            tableEntries.delete(line, true);
            logAfter(TableLog.LogAction.DELETE, "", line.formatEntryNoNewLine());
        }
    }

    @Override
    public void delete(String... lines) {
        List<T> entries = lineStringsToEntries(lines);
        delete(entries);
    }

    private List<T> lineStringsToEntries(String[] lines) {
        List<T> entries = new ArrayList<>();
        for (String line : lines) {
            line = line.stripLeading();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            entries.add((T)DictionaryEntry.parseEntry(line, getRootUri(), true));
        }
        return entries;
    }

    /**
     * @return list of lines that need bucketizing.
     */
    public List<T> needsBucketizing() {
        return this.selectAll().stream().filter(l -> !l.hasBucket()).collect(Collectors.toList());
    }

    /**
     * Remove the selected files from their bucket.
     *
     * @param lines lines to be removed.
     */
    public void removeFromBucket(Collection<T> lines) {
        try {
            for (T line : lines) {
                T lineToRemoveFrom = tableEntries.findLine(line);
                if (lineToRemoveFrom != null) {
                    String bucket = lineToRemoveFrom.getBucket();
                    if (lineToRemoveFrom.isDeleted()) {
                        tableEntries.delete(lineToRemoveFrom, false);
                        logAfter(TableLog.LogAction.DELETE, bucket, lineToRemoveFrom.formatEntryNoNewLine());
                    } else {
                        lineToRemoveFrom.setBucket("");
                        logAfter(TableLog.LogAction.REMOVEFROMBUCKET, bucket, lineToRemoveFrom.formatEntryNoNewLine());
                    }
                }
            }
        } catch (Exception e) {
            throw new GorDataException("Entries could not be removed from bucket", e);
        }
    }

    /**
     * Remove the selected files from their bucket.
     *
     * @param lines lines to be removed.
     */
    @SafeVarargs
    public final void removeFromBucket(T... lines) {
        removeFromBucket(Arrays.asList(lines));
    }


    /**
     * Calculate a MD5 digest for the specified dictionary file based on the fullname of all physical file and last modification date given
     * for the specified tags. Note that physical file, implies that link files and symbolic links are read for their content
     * and the actual physical file used.
     * <p>
     * Note:
     * * We assume that if content of the dictionary changes the dictionary file is touched.
     * * If we have a small tags list we try to optimize by checking the files behind the tags (in hope that even though the dict has changed this small
     * subset of files has not changed).  This would allow to make better use of caches.
     * * If not tags we use the signature of the dictionary.  In this case doing the file check optimization does not make sense.
     *
     * @param tags The tags from the dictionary file that will be touched
     * @return The MD5 digest that is the file signature
     */
    public String getSignature(Boolean useCommonRoot, String commonRoot, String... tags) throws IOException {
        int maxFiles = Integer.parseInt(getConfigTableProperty("gor.table.signature.maxfiles", "10"));
        ByteTextBuilder fingerPrintString;
        if ((tags != null && tags.length > 0 && tags.length <= maxFiles)) {
            List<T> matchingLines = filter().tags(tags).get();
            fingerPrintString = new ByteTextBuilder(matchingLines.size() * 300);
            for (T line : matchingLines) {
                fingerPrintString.append(line.getContentReal());
                fingerPrintString.append((byte) '&');
                fingerPrintString.append(getLastModifiedTime(line.getContentReal(), getSecurityContext(), commonRoot));
            }
        } else {
            fingerPrintString = new ByteTextBuilder(300);
            fingerPrintString.append(getPathUri().toString());
            fingerPrintString.append((byte) '&');
            fingerPrintString.append(getLastModifiedTime(getPathUri().toString(), getSecurityContext(), commonRoot));
        }

        return fingerPrintString.md5();
    }

    public String getSignature(String... tags) throws IOException {
        return getSignature(false,null, tags);
    }

    /**
     * Note:
     * * We assume that if content of the dictionary changes the dictionary file is touched.
     * * If we have a small tags list we try to optimize by checking the files behind the tags (in hope that even though the dict has changed this small
     * subset of files has not changed).  This would allow to make better use of caches.
     * * If not tags we use the signature of the dictionary.  In this case doing the file check optimization does not make sense.
     *
     * @param tags The tags from the dictionary file that will be touched
     * @return The last modified date of the source files for all the specified tags
     */
    public long getLastModified(Boolean useCommonRoot, String commonRoot, String... tags) throws IOException {
        int maxFiles = Integer.parseInt(getConfigTableProperty("gor.table.signature.maxfiles", "10"));
        long lastModified = 0;
        if ((tags != null && tags.length > 0 && tags.length <= maxFiles)) {
            // Empty tags here means no tags so replace with null.
            List<T> matchingLines = filter().tags(tags).get();
            for (T line : matchingLines) {
                lastModified = Math.max(lastModified, getLastModifiedTime(line.getContentReal(), getSecurityContext(), commonRoot));
            }
        } else {
            lastModified = Math.max(lastModified, getLastModifiedTime(getPath().toString(), getSecurityContext(), commonRoot));
        }

        return lastModified;
    }

    public long getLastModified(String... tags) throws IOException {
        return getLastModified(false, null, tags);
    }

    /**
     * Get all tags (and aliases) used in the dictionary file.
     *
     * @return all tags (and aliases) used in the dictionary file.
     */
    public Set<String> getAllActiveTags() {
        return this.tableEntries.getAllActiveTags();
    }


    @Override
    public void reload() {
        if (header == null) this.header = new DictionaryTableMeta();
        if (this.tableEntries == null) this.tableEntries = createTableEntries();
        
        // Loading is split into this method and getRawlines (but we can have update in between) will that affect us?? Do we need lock (and update metadata here)
        // but we definitely need it for getRawLines.
        super.reload();

        // Note, we are lazy loading the table so loading here only means clearing the data.  The actual load will happen
        // when we need the data.
        if (prevSerial.equals(TableHeader.NO_SERIAL) || !this.header.getProperty(TableHeader.HEADER_SERIAL_KEY).equals(prevSerial)) {
            tableEntries.clear();
        }

        // Reload meta data from the table, use the current values as defaults, so if the table has already been saved
        // the reload will replace the set values.  If the table has never been saved we keep the set values (we
        // do this for backward compatibility with TableManager, as it is probably more correct to use the default
        // values if the table has never been saved).
        sourceColumn = getConfigTableProperty(DictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY, sourceColumn);
        hasUniqueTags = Boolean.parseBoolean(getConfigTableProperty(DictionaryTableMeta.HEADER_UNIQUE_TAGS_KEY,  Boolean.toString(hasUniqueTags)));

        bucketize = getBooleanConfigTableProperty(DictionaryTableMeta.HEADER_BUCKETIZE_KEY, bucketize);
    }

    /**
     * Reload from file.
     */
    public void reloadForce() {
            this.header.setProperty(TableHeader.HEADER_SERIAL_KEY, TableHeader.NO_SERIAL);
            reload();
            getEntries();
    }

    @Override
    protected void updateMetaBeforeSave() {
        super.updateMetaBeforeSave();

        this.header.setProperty(DictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY, this.sourceColumn);
        this.header.setProperty(DictionaryTableMeta.HEADER_UNIQUE_TAGS_KEY, Boolean.toString(this.hasUniqueTags));

        if (bucketize != null) {
            this.header.setProperty(DictionaryTableMeta.HEADER_BUCKETIZE_KEY, Boolean.toString(this.bucketize));
        }

        this.header.setProperty(TableHeader.HEADER_LINE_COUNT_KEY, String.valueOf(tableEntries.size()));
    }

    /**
     * Initialize the gor dictionary if it does not exists.
     */
    @Override
    public void initialize() {
        super.initialize();

        // If new set extra basic meta data.
        if (!fileReader.exists(path.toString())) {
            // TODO:  Use same columnames as specified in TableHeader.
            ((TableHeader)this.header).setFileHeader(new String[]{"File", DEFAULT_SOURCE_COLUMN, "ChrStart", "PosStart", "ChrStop", "PosStop", "Tags"});
        }
    }

    /**
     * Add the given entries to the given bucket, done after the bucketization has been done.
     *
     * Note:  We are assuming here that the underlying datafiles are not changed (but can be deleted).
     *
     * @param bucket bucket to add to.
     * @param lines  files to select.
     */
    public void addToBucket(String bucket, List<T> lines) {
        String bucketLogical = relativize(getRootUri(), bucket);
        for (T line : lines) {
            T lineToUpdate = tableEntries.findLine(line);
            if (lineToUpdate != null) {
                if (lineToUpdate.hasBucket() && !lineToUpdate.getBucket().equals(bucketLogical)) {
                    throw new GorDataException(String.format("File %s is already in bucket %s and can not be added to bucket %s",
                            line.getContentRelative(), lineToUpdate.getBucket(), bucketLogical));
                }
                lineToUpdate.setBucket(bucketLogical);
                logAfter(TableLog.LogAction.ADDTOBUCKET, bucketLogical, line.formatEntryNoNewLine());
            } else {
                // No line found, must have been deleted.  To be able to use the bucket we must add a new line.
                T newDeletedLine = (T) TableEntry.copy(line);
                newDeletedLine.setDeleted(true);
                newDeletedLine.setBucket(bucketLogical);
                tableEntries.insert(newDeletedLine, false);
                logAfter(TableLog.LogAction.INSERT, bucketLogical, line.formatEntryNoNewLine());
            }
        }
    }

    @SafeVarargs
    public final void addToBucket(String bucket, T... lines) {
        addToBucket(bucket, Arrays.asList(lines));
    }

    private String getFileEndingFromContentFile(String fileName) {
        if (fileName != null) {
            DataSource source = getFileReader().resolveUrl(fileName);
            return FilenameUtils.getExtension(source.getFullPath());
        }
        return null;
    }

    public String getFileEndingFromContent() {
        List<T> entries = getEntries();
        if (!entries.isEmpty()) {
            return getFileEndingFromContentFile(entries.get(0).getContentReal());
        }
        return null;
    }

    public static Boolean inferShouldBucketizeFromType(String type) {
        if ("gor".equalsIgnoreCase(type) || "gorz".equalsIgnoreCase(type)) {
            return true;
        }

        if ("bam".equalsIgnoreCase(type) || "cram".equalsIgnoreCase(type) || "vcf".equalsIgnoreCase(type)) {
            return false;
        }

        return null;
    }

    public Boolean inferShouldBucketizeFromFile(String fileName) {
        String type = getFileEndingFromContentFile(fileName);
        return inferShouldBucketizeFromType(type);
    }

    public Boolean inferShouldBucketizeFromContent() {
        if (contentType == null) {
            contentType = getFileEndingFromContent();
        }
        return inferShouldBucketizeFromType(contentType);
    }

    protected abstract static class Builder<B extends Builder<B>> extends BaseTable.Builder<B> {
        protected String sourceColumn;
        protected Boolean uniqueTags;

        protected Builder(URI path) {
            super(path);
        }

        public B sourceColumn(String sourceColumn) {
            this.sourceColumn = sourceColumn;
            return self();
        }

        public B uniqueTags(boolean val) {
            this.uniqueTags = val;
            return self();
        }

        public abstract BaseDictionaryTable build();
    }

    /**
     * Helper class for passing in row filtering criteria.
     */
    public class TableFilter {
        String[] files;
        String[] aliases;
        String[] tags;
        String[] buckets;
        String chrRange;
        boolean matchAllTags = false;
        boolean includeDeleted = false;

        /**
         * Filter for files names (content)
         * @param val file names to filter by, absolute or relative to the table.
         * @return return new filter on files.
         */
        public TableFilter files(String... val) {
            this.files = val != null ? Arrays.stream(val).map(f -> formatUri(resolve(getRootUri(), f))).toArray(String[]::new) : null;
            return this;
        }

        /**
         * Filter for files names (content)
         * @param val file names to filter by, absolute or relative to the table.
         * @return return new filter on files.
         */
        public TableFilter files(URI... val) {
            this.files = val != null ? Arrays.stream(val).map(f -> formatUri(resolve(getRootUri(), f))).toArray(String[]::new) : null;
            return this;
        }

        public TableFilter aliases(String... val) {
            this.aliases = val;
            return this;
        }

        // Tags matches line tags if line tags, otherwise line alias.
        public TableFilter tags(String... val) {
            this.tags = val;
            return this;
        }

        public TableFilter matchAllTags(String... val) {
            this.tags = val;
            this.matchAllTags = true;
            return this;
        }

        public TableFilter buckets(String... val) {
            this.buckets = val != null ? Arrays.stream(val).map(b -> PathUtils.formatUri(resolve(getRootUri(), b))).toArray(String[]::new) : null;
            return this;
        }

        public TableFilter buckets(Path... val) {
            this.buckets = val != null ? Arrays.stream(val).map(b -> PathUtils.formatUri(resolve(getRootUri(), b.toString()))).toArray(String[]::new) : null;
            return this;
        }

        public TableFilter chrRange(String val) {
            GenomicRange gr = GenomicRange.parseGenomicRange(val);
            this.chrRange = gr != null ? gr.formatAsTabDelimited() : null;
            return this;
        }

        public TableFilter includeDeleted(boolean val) {
            this.includeDeleted = val;
            return this;
        }

        public TableFilter includeDeleted() {
            this.includeDeleted = true;
            return this;
        }

        /**
         * Match the line based on the filter.
         * Notes:
         * 1. An element (i.e. tags, files ...) in the filter to gets a match if any item in it matches.
         * <p>
         * 2. If the filter contains buckets we also include deleted lines.
         *
         * @param l    line to match
         * @return {@code true} if the line matches the filter otherwise {@code false}.
         */
        protected boolean match(T l) {
           return matchIncludeLine(l)
                   && (matchIsNoFilter()
                       || (matchFiles(l) && matchAliases(l) && matchTags(l) && matchBuckets(l) && matchRange(l)));
        }

        private boolean matchIncludeLine(T l) {
            return !l.isDeleted() || includeDeleted || buckets != null;
        }

        private boolean matchIsNoFilter() {
            return files == null && aliases == null && tags == null && buckets == null && chrRange == null;
        }

        private boolean matchFiles(T l) {
            return files == null || Stream.of(files).anyMatch(f -> f.equals(l.getContentReal()));
        }

        private boolean matchBuckets(T l) {
            return buckets == null || (!l.hasBucket() && buckets.length == 0) ||
                    (l.hasBucket() && Stream.of(buckets).anyMatch(b -> b.equals(l.getBucketReal())));
        }

        private boolean matchAliases(T l) {
            return aliases == null || Stream.of(aliases).anyMatch(f -> f.equals(l.getAlias()));
        }

        private boolean matchTags(T l) {
            String[] filterTags = l.getFilterTags();
            return tags == null || (filterTags.length == 0 && tags.length == 0)
                    || (matchAllTags ? Stream.of(tags).allMatch(t -> ArrayUtils.contains(filterTags, t)) : Stream.of(tags).anyMatch(t -> ArrayUtils.contains(filterTags, t)));
        }

        private boolean matchRange(T l) {
            return chrRange == null || (l.getRange() != null && chrRange.equals(l.getRange().formatAsTabDelimited()));
        }

        public List<T> get() {
            log.debug("Selecting lines from dictionary {}", getName());
            // Set initial candidates for search (this also forces load if not loaded and populates the tagHashToLines index)
            List<T> lines2Search = getEntries(ArrayUtils.addAll(aliases, tags));
            return lines2Search.stream().filter(this::match).collect(Collectors.toCollection(ArrayList::new));
        }

        public BaseDictionaryTable<T> getTable() {
            return BaseDictionaryTable.this;
        }

    }
}
