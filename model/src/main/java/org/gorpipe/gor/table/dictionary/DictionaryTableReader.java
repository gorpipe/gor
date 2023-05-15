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
import org.apache.commons.lang3.BooleanUtils;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.livecycle.TableInfoBase;
import org.gorpipe.gor.table.TableHeader;
import org.gorpipe.gor.util.ByteTextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gorpipe.gor.table.dictionary.DictionaryTableMeta.DEFAULT_SOURCE_COLUMN;
import static org.gorpipe.gor.table.util.PathUtils.*;

/**
 * Dictionary table info.
 *
 * Contains basic info for a dictionary table.
 *
 */
public class DictionaryTableReader extends TableInfoBase<DictionaryEntry> {

    private static final Logger log = LoggerFactory.getLogger(DictionaryTableReader.class);

    private String contentType = null;  // For caching content type.

    protected ITableEntries<DictionaryEntry> tableEntries;

    protected TableAccessOptimizer tableAccessOptimizer;

    public DictionaryTableReader(String path) {
        this(path, null);
    }

    public DictionaryTableReader(String path, FileReader fileReader) {
        super(path, fileReader);
    }

    public DictionaryTableReader(Path path) {
        this(path.toUri().toString(), null);
    }

    public String getSourceColumn() {
        return getConfigTableProperty(DictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY, DEFAULT_SOURCE_COLUMN);
    }

    public boolean isBucketize() {
        Boolean headerOrSetBucketize = getBooleanConfigTableProperty(DictionaryTableMeta.HEADER_BUCKETIZE_KEY, null);
        return headerOrSetBucketize != null ? headerOrSetBucketize : BooleanUtils.isTrue(inferShouldBucketizeFromContent());
    }

    public Boolean getLineFilter() {
        return Boolean.valueOf(header.getProperty(DictionaryTableMeta.HEADER_LINE_FILTER_KEY, "true"));
    }

    public boolean isHasUniqueTags() {
        return Boolean.parseBoolean(getConfigTableProperty(DictionaryTableMeta.HEADER_UNIQUE_TAGS_KEY,  "false"));
    }

    public String getContentReal(DictionaryEntry entry) {
        return resolve(getRootPath(), entry.getContentRelative());
    }

    public String getContentProjectRelative(DictionaryEntry entry) {
        return relativize(getProjectPath(),  resolve(getRootPath(), entry.getContentRelative()));
    }

    /**
     * Select the union of the rows defined by the given filters.
     *
     * @param filters filters to filter rows by.
     * @return union of rows as specified by the given filters.
     */
    public final List<DictionaryEntry> selectUninon(TableFilter<DictionaryEntry>... filters) {
        return Arrays.stream(filters).flatMap(f -> f.get().stream()).collect(Collectors.toList());
    }

    /**
     * Selects all the rows, including the deleted rows.
     *
     * @return list with all the rows including the deleted rows.
     */
    public final List<DictionaryEntry> selectAll() {
        // Copy the list but not the elements, so it can be used as input into for example delete.
        return new ArrayList<>(getEntries());
    }

    /**
     * Creates new filter on this table.
     * Filter does NOT by default include deleted rows.
     *
     * @return new filter on this table.
     */
    public TableFilter<DictionaryEntry> filter() {
        return new TableFilter<>(this, tableEntries);
    }

    public List<DictionaryEntry> getEntries() {
        return tableEntries.getEntries();
    }

    public List<DictionaryEntry> getEntries(String... aliasesAndTags) {
        return tableEntries.getEntries(aliasesAndTags);
    }

    /**
     * Get all buckets in the table.
     *
     * @return List of Path elements representing all the buckets in the table.
     */
    public List<String> getBuckets() {
        return filter().get().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(DictionaryEntry::getBucket).distinct().collect(Collectors.toList());
    }

    public boolean hasBuckets() {
        return isBucketize();
    }

    @Override
    public Stream<String> getLines() {
        return getEntries().stream().map(l -> l.formatEntryNoNewLine());
    }


    /**
     * @return list of lines that need bucketizing.
     */
    public List<DictionaryEntry> needsBucketizing() {
        return this.selectAll().stream().filter(l -> !l.hasBucket()).collect(Collectors.toList());
    }

    public Boolean inferShouldBucketizeFromContent() {
        if (contentType == null) {
            contentType = getFileEndingFromContent();
        }
        return inferShouldBucketizeFromType(contentType);
    }

    public Boolean inferShouldBucketizeFromFile(String fileName) {
        String type = getFileEndingFromContentFile(fileName);
        return inferShouldBucketizeFromType(type);
    }

    private String getFileEndingFromContentFile(String fileName) {
        if (fileName != null) {
            DataSource source = getFileReader().resolveUrl(fileName);
            return FilenameUtils.getExtension(source.getFullPath());
        }
        return null;
    }

    public String getFileEndingFromContent() {
        List<DictionaryEntry> entries = getEntries();
        if (!entries.isEmpty()) {
            return getFileEndingFromContentFile(getContentReal(entries.get(0)));
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
            List<DictionaryEntry> matchingLines = filter().tags(tags).get();
            fingerPrintString = new ByteTextBuilder(matchingLines.size() * 300);
            for (DictionaryEntry line : matchingLines) {
                fingerPrintString.append(getContentReal(line));
                fingerPrintString.append((byte) '&');
                fingerPrintString.append(getLastModifiedTime(getContentReal(line), getSecurityContext(), commonRoot));
            }
        } else {
            fingerPrintString = new ByteTextBuilder(300);
            fingerPrintString.append(getPath());
            fingerPrintString.append((byte) '&');
            fingerPrintString.append(getLastModifiedTime(getPath(), getSecurityContext(), commonRoot));
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
            List<DictionaryEntry> matchingLines = filter().tags(tags).get();
            for (DictionaryEntry line : matchingLines) {
                lastModified = Math.max(lastModified, getLastModifiedTime(getContentReal(line), getSecurityContext(), commonRoot));
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
        String prevSerial = this.header.getProperty(TableHeader.HEADER_SERIAL_KEY);

        // Loading is split into this method and getRawlines (but we can have update in between) will that affect us?? Do we need lock (and update metadata here)
        // but we definitely need it for getRawLines.
        super.reload();

        if (this.tableEntries == null) this.tableEntries = createTableEntries();

        // Note, we are lazy loading the table so loading here only means clearing the data.  The actual load will happen
        // when we need the data.
        if (prevSerial.equals(TableHeader.NO_SERIAL) || !this.header.getProperty(TableHeader.HEADER_SERIAL_KEY).equals(prevSerial)) {
            tableEntries.clear();
        }

        tableAccessOptimizer = null;
    }

    /**
     * Reload from file.
     */
    public void reloadForce() {
            this.header.setProperty(TableHeader.HEADER_SERIAL_KEY, TableHeader.NO_SERIAL);
            reload();
            getEntries();
    }

    protected ITableEntries<DictionaryEntry> createTableEntries() {
        return new TableEntries<>(this, DictionaryEntry.class);
        // Leave this in here for easy try out.
        //return new TableEntries<>(path, DictionaryRawEntry.class);
    }

    /**
     * Get optimized lines.
     *
     * @param tags               a list of tags to filter the column on.
     *                           Seems to only set for -f -ff options
     * @param allowBucketAccess  can the optimizer use buckets.
     * @param isSilentTagFilter
     * @return                   optimzed list of files to data for the given tags.
     */
    public List<DictionaryEntry> getOptimizedLines(Set<String> tags, boolean allowBucketAccess, boolean isSilentTagFilter) {
        return getTableAccessOptimizer().getOptimizedEntries(tags, allowBucketAccess, isSilentTagFilter);
    }

    protected TableAccessOptimizer getTableAccessOptimizer() {
        if (tableAccessOptimizer == null) {
            tableAccessOptimizer = new DefaultTableAccessOptimizer(this, tableEntries);
        }
        return tableAccessOptimizer;
    }

    
    public boolean hasDeletedEntries() {
        return this.tableEntries.hasDeletedTags();
    }

    public Collection<String> getBucketDeletedFiles(String path) {
        return getTableAccessOptimizer().getBucketDeletedFiles(path);
    }

}
