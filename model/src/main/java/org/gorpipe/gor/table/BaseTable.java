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

package org.gorpipe.gor.table;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.GorOptions;
import org.gorpipe.model.util.ByteTextBuilder;
import org.gorpipe.model.util.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gorpipe.gor.table.PathUtils.*;

/**
 * Base class for tables and dictionaries.
 * <p>
 * Created by gisli on 25/07/16.
 *
 */
public abstract class BaseTable<T extends BucketableTableEntry> {

    private static final Logger log = LoggerFactory.getLogger(BaseTable.class);

    private static final boolean DEFAULT_VALIDATE_FILES = Boolean.parseBoolean(System.getProperty("GOR_TABLE_FILES_VALIDATE", "true"));
    private static final String DEFAULT_SOURCE_COLUMN = "PN";
    private static final boolean FORCE_SAME_COLUMN_NAMES = false;
    public static final String HISTORY_DIR_NAME = "history";
    public static final boolean DEFAULT_USE_HISTORY = true;

    private final Path path;            // Path to the table (currently absolute instead of real for compatibility with older code).
    private final Path folderPath;      // Path to the table folder.  The table folder is hidden folder that sits next to the
    // table and contains various files related to it.
    private final Path rootPath;        // Path to the table root (currently absolute instead of real for compatibility with older code)..
    private final String name;          // Name of the table.
    private String id = null;           // Unique id (based on full path, just so we don't always have to refer to full path).
    private final URI rootUri;          // uri to table root (just to improve performance when working with uri's).
    private final Path historyDir;      // Backup dir for older versions of this dictionary (absolute) (if requested).

    protected String securityContext;

    protected final TableHeader header;                // Header info.
    private String tagColumn = DEFAULT_SOURCE_COLUMN;     // Name of the files tag column (source column).
    private boolean useHistory = DEFAULT_USE_HISTORY;
    private boolean hasUniqueTags = false;    //True if we don't want to allow double entries of the same tag
    private boolean validateFiles = DEFAULT_VALIDATE_FILES;

    protected ITableEntries<T> tableEntries;

    private TableLog tableLog;

    protected BaseTable(Builder builder) {
        this(builder.path);
        if (builder.tagColumn != null) {
            setTagColumn(builder.tagColumn);
        }
        if (builder.validateFiles != null) {
            setValidateFiles(builder.validateFiles);
        }
        if (builder.useHistory != null) {
            setUseHistory(builder.useHistory);
        }
        if (builder.uniqueTags != null) {
            setUniqueTags(builder.uniqueTags);
        }
        if(builder.securityContext != null) {
            setSecurityContext(builder.securityContext);
        }
    }

    /**
     * Main constructor.
     *
     * @param path              path to the dictionary file.
     */
    protected BaseTable(Path path) {

        this.rootPath = normalize(path.getParent() != null ? path.getParent() : Paths.get("")).toAbsolutePath();
        this.rootUri = normalize(Paths.get(this.rootPath + "/").toUri());

        this.path = this.rootPath.resolve(path.getFileName());
        this.name = FilenameUtils.removeExtension(path.getFileName().toString());
        this.folderPath = this.rootPath.resolve("." + this.name);

        this.historyDir = this.folderPath.resolve(HISTORY_DIR_NAME);
        this.tableLog = new TableLog(this.historyDir);

        this.tableEntries = createTableEntries(this.getPath());
        this.header = new TableHeader();

        // Loads the header, and loads/initializes fields read from header.
        reload();
    }

    protected abstract ITableEntries<T> createTableEntries(Path path);

    /**
     * @return name of this table.
     */
    public String getName() {
        return this.name;
    }

    public String getId() {
        // Lazy initialization.
        if (this.id == null) {
            this.id = Util.md5(this.path.toString());
        }
        return this.id;
    }

    /**
     * Get real path of this table.
     *
     * @return real path of this table.
     */
    public Path getPath() {
        return this.path;
    }

    public void setColumns(String[] columns) {
        if (this.tableEntries.size() > 0 && columns.length != this.header.getColumns().length) {
            throw new GorDataException("Invalid columns - " + String.format("New columns length (%d) does not fit current data column count (%d)",
                    columns.length, this.header.getColumns().length));
        }
        this.header.setColumns(columns);
    }

    public String[] getColumns() {
        return this.header.getColumns();
    }

    public String getTagColumn() {
        return this.tagColumn;
    }

    public void setTagColumn(String tagColumn) {
        this.tagColumn = tagColumn;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public URI getRootUri() {
        return rootUri;
    }

    /**
     * Get the table folder path.   The table folder is hidden folder that sits next to
     * the dictionary and contains various files related to it.
     *
     * @return the table folder path.
     */
    public Path getFolderPath() {
        return folderPath;
    }

    public String getSecurityContext() {
        return securityContext;
    }

    public void setSecurityContext(String securityContext) {
        this.securityContext = securityContext;
    }

    /**
     * Get table property <key>
     *
     * @param key property to get.
     * @return table properyt <key>
     */
    public String getProperty(String key) {
        return this.header.getProperty(key);
    }

    /**
     * Set table property <key>
     *
     * @param key   property name.
     * @param value property value
     */
    public void setProperty(String key, String value) {
        this.header.setProperty(key, value);
    }

    /**
     * Check if the table contains property.
     *
     * @param key   property name.
     * @return  true if the table contains the property, otherwise false.
     */
    public boolean containsProperty(String key) {
        return this.header.containsProperty(key);
    }

    public boolean isValidateFiles() {
        return validateFiles;
    }

    public void setValidateFiles(boolean validateFiles) {
        this.validateFiles = validateFiles;
    }

    public boolean isHasUniqueTags() {
        return this.hasUniqueTags;
    }

    public void setUniqueTags(boolean hasUniqueTags){
        this.hasUniqueTags = hasUniqueTags;
    }

    public boolean isUseHistory() {
        return this.useHistory;
    }

    public void setUseHistory(boolean useHistory){
        this.useHistory = useHistory;
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

    public List<T> getEntries(String... tags) {
        return tableEntries.getEntries(tags);
    }

    /**
     * Get all buckets in the table.
     *
     * @return List of Path elements representing all the buckets in the table.
     */
    public List<Path> getBuckets() {
        return filter().get().stream().filter(l -> l.hasBucket() && !l.isDeleted()).map(BucketableTableEntry::getBucket).distinct().map(Paths::get).collect(Collectors.toList());
    }

    /**
     * Insert/update dictionary lines.
     *
     * @param lines the line(s) to update.
     */
    public void insert(List<T> lines) {
        int count = 0;
        for (T line : lines) {
            count++;
            if (count % 1000 == 0) {
                log.info("Inserting line {} of {}", count, lines.size());
            }
            // Validate the new file.
            if (validateFiles) {
                try {
                    if (isLocal(line.getContentReal())) {
                        if (!Files.exists(Paths.get(fixFileSchema(line.getContentReal())))) {
                            throw new GorDataException(String.format("Local entry %s does not exists!", line.getContentReal()));
                        }
                    } else {
                        // Remote.
                        SourceReference sourceRef = new SourceReferenceBuilder(line.getContentReal()).securityContext(securityContext).build();
                        DataSource ds = GorDriverFactory.fromConfig().getDataSource(sourceRef);
                        if ((ds == null || !ds.exists()) && securityContext != null) {
                            throw new GorDataException(String.format("Remote entry %s does not exists!", line.getContentReal()));
                        }
                    }
                } catch (IOException ex) {
                    throw new GorDataException(String.format("Entry %s can not be verified!", line.getContentReal()), ex);
                }

                updateValidateHeader(line);
            }

            this.tableEntries.insert(line, hasUniqueTags);
            if (useHistory) {
                tableLog.logAfter(TableLog.LogAction.INSERT, "", line);
            }
        }
    }

    @SafeVarargs
    public final void insert(T... lines) {
        insert(Arrays.asList(lines));
    }

    /**
     * @param data map with alias to files, to be add to the dictionary.  The files must be normalized and either absolute or
     *             relative to the dictionary root.
     */
    public abstract void insert(Map<String, List<String>> data);

    /**
     * Delete the given lines from the dictionary.
     *
     * @param lines lines to remove.
     */
    public void delete(Collection<T> lines) {
        for (T line : lines) {
            tableEntries.delete(line, true);
            if (useHistory) {
                tableLog.logAfter(TableLog.LogAction.DELETE, "", line);
            }
        }
    }

    /**
     * Delete the given lines from the dictionary.
     *
     * @param lines lines to remove.
     */
    @SafeVarargs
    public final void delete(T... lines) {
        delete(Arrays.asList(lines));
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
                        if (useHistory) {
                            tableLog.logAfter(TableLog.LogAction.DELETE, bucket, lineToRemoveFrom);
                        }
                    } else {
                        lineToRemoveFrom.setBucket("");
                        if (useHistory) {
                            tableLog.logAfter(TableLog.LogAction.REMOVEFROMBUCKET, bucket, lineToRemoveFrom);
                        }
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
                fingerPrintString.append(getLastModifiedTime(line.getContentReal(), securityContext, commonRoot));
            }
        } else {
            fingerPrintString = new ByteTextBuilder(300);
            fingerPrintString.append(getPath().toString());
            fingerPrintString.append((byte) '&');
            fingerPrintString.append(getLastModifiedTime(getPath().toString(), securityContext, commonRoot));
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
                lastModified = Math.max(lastModified, getLastModifiedTime(line.getContentReal(), securityContext, commonRoot));
            }
        } else {
            lastModified = Math.max(lastModified, getLastModifiedTime(getPath().toString(), securityContext, commonRoot));
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

    /**
     * Reload from file.
     * Note:  Reload is called when we open read transaction.
     */
    public void reload() {
        // Loading is split into this method and getRawlines (but we can have update in between) will that affect us?? Do we need lock (and update metadata here)
        // but we definitly need it for getRawLines.

        updateFolderMetadata();

        log.debug("Loading table {}", getName());

        // Note, we are lazy loading the table so loading here only means clearing the data.  The actual load will happen
        // when we need the data.
        String prevSerial = this.header.getProperty(TableHeader.HEADER_SERIAL_KEY);
        parseHeader();

        if (prevSerial.equals(TableHeader.NO_SERIAL) || !this.header.getProperty(TableHeader.HEADER_SERIAL_KEY).equals(prevSerial)) {
            tableEntries.clear();
        }

        // Reload meta data from the table, use the current values as defaults, so if the table has already been saved
        // the reload will replace the set values.  If the table has never been saved we keep the set values (we
        // do this for backward compatibility with TableManager, as it is probably more correct to use the default
        // values if the table has never been saved).
        tagColumn = getConfigTableProperty(TableHeader.HEADER_SOURCE_COLUMN_KEY,  tagColumn);
        validateFiles =  Boolean.parseBoolean(getConfigTableProperty(TableHeader.HEADER_VALIDATE_FILES_KEY, Boolean.toString(validateFiles)));
        useHistory = Boolean.parseBoolean(getConfigTableProperty(TableHeader.HEADER_USE_HISTORY_KEY, Boolean.toString(useHistory)));
        hasUniqueTags = Boolean.parseBoolean(getConfigTableProperty(TableHeader.HEADER_UNIQUE_TAGS_KEY,  Boolean.toString(hasUniqueTags)));
    }

    /**
     * Reload from file.
     */
    public void reloadForce() {
            this.header.setProperty(TableHeader.HEADER_SERIAL_KEY, TableHeader.NO_SERIAL);
            reload();
            getEntries();
    }

    /**
     * Parse/load the table header.
     */
    protected void parseHeader() {
        this.header.load(this);
    }

    /**
     * Update the dictionary file from this.
     */
    public void save() {
        initialize();

        this.header.setProperty(TableHeader.HEADER_SOURCE_COLUMN_KEY, this.tagColumn);
        this.header.setProperty(TableHeader.HEADER_USE_HISTORY_KEY, Boolean.toString(this.useHistory));
        this.header.setProperty(TableHeader.HEADER_VALIDATE_FILES_KEY, Boolean.toString(this.validateFiles));
        this.header.setProperty(TableHeader.HEADER_UNIQUE_TAGS_KEY, Boolean.toString(this.hasUniqueTags));

        doSave();
        if (useHistory) {
            tableLog.commit();
        }
    }

    protected abstract void doSave();

    /**
     * Initalize the gor dictionary if it does not exists.
     */
    public void initialize() {
        log.trace("Initialize {}", getName());

        if (!Files.exists(this.rootPath)) {
            throw new GorSystemException("Table " + path + " can not be created as the parent path does not exists!", null);
        }

        updateFolderMetadata();

        if (!Files.exists(getFolderPath())) {
            try {
                log.trace("Creating table directory {}", getFolderPath());
                Files.createDirectory(getFolderPath(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
            } catch (FileAlreadyExistsException faee) {
                // Ignore, some one else created it.
                log.trace("Table directory {} already exists", getFolderPath());
            } catch (IOException e) {
                throw new GorSystemException("Could not create table directory: " + getFolderPath(), e);
            }
        }

        if (this.useHistory && !Files.exists(this.historyDir)) {
            try {
                log.trace("Creating table history directory {}", this.historyDir);
                Files.createDirectory(this.historyDir, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
            } catch (FileAlreadyExistsException faee) {
                // Ignore, some one else created it.
                log.trace("Table history directory {} already exists", this.historyDir);
            } catch (IOException e) {
                throw new GorSystemException("Could not create table hostory directory: " + this.historyDir, e);
            }
        }

        if (!Files.exists(getPath())) {
            // Create the header.
            this.header.setProperty(TableHeader.HEADER_FILE_FORMAT_KEY, "1.0");
            this.header.setProperty(TableHeader.HEADER_CREATED_KEY, new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
            this.header.setTableColumns(new String[]{"File", "Alias", "ChrStart", "PosStart", "ChrStop", "PosStop", "Tags"});
        }
    }

    private void updateValidateHeader(T line) {
        // Validate that line matches header columns.

        TableHeader lineHeader = parseHeaderFromLine(line);

        if (this.header.getColumns().length == 0
                || (!this.header.isProper() && lineHeader.isProper() && this.header.getColumns().length == lineHeader.getColumns().length)) {
            // Have better header.
            this.header.setColumns(lineHeader.getColumns());
        } else {
            // Validate the header.
            if (this.header.getColumns().length != lineHeader.getColumns().length) {
                throw new GorDataException(String.format("Can not update dictionary. The number of columns does not match (dict: %d, line: %d)",
                        this.header.getColumns().length, lineHeader.getColumns().length), -1, lineHeader.toString(), header.toString());
            }

            if (FORCE_SAME_COLUMN_NAMES && this.header.isProper() && lineHeader.isProper() &&
                    !String.join(",", this.header.getColumns()).equals(String.join(",", lineHeader.getColumns()))) {
                throw new GorDataException(String.format("Can not update dictionary - The columns do not match (dict: %s vs line: %s)",
                        String.join(",", this.header.getColumns()), String.join(",", lineHeader.getColumns())), -1, lineHeader.toString(), header.toString() );
            }
        }
    }

    /**
     * Collect header information from the table line (i.e. the file the line references).
     *
     * @param line the table line referencing the file.
     * @return new header object inferred from the table file.
     */
    private TableHeader parseHeaderFromLine(T line) {
        TableHeader newHeader = new TableHeader();
        String args = line.getContentReal() + (securityContext != null ? " " + securityContext : "");
        GorOptions gorOptions = GorOptions.createGorOptions(args);
        try(GenomicIterator source = gorOptions.getIterator()) {
            newHeader.setColumns(source.getHeader().split("\t"));
        }
        return newHeader;
    }

    /**
     * Add the given entries to the given bucket, done after the bucketization has been done.
     *
     * Note:  We are assuming here that the underlying datafiles are not changed (but can be deleted).
     *
     * @param bucket bucket to add to.
     * @param lines  files to select.
     */
    public void addToBucket(Path bucket, List<T> lines) {
        String bucketLogical = relativize(getRootPath(), bucket).toString();
        for (T line : lines) {
            T lineToUpdate = tableEntries.findLine(line);
            if (lineToUpdate != null) {
                if (lineToUpdate.hasBucket() && !lineToUpdate.getBucket().equals(bucketLogical)) {
                    throw new GorDataException(String.format("File %s is already in bucket %s and can not be added to bucket %s",
                            line.getContentRelative(), lineToUpdate.getBucket(), bucketLogical));
                }
                lineToUpdate.setBucket(bucketLogical);
                if (useHistory) {
                    tableLog.logAfter(TableLog.LogAction.ADDTOBUCKET, bucketLogical, line);
                }

            } else {
                // No line found, must have been deleted.  To be able to use the bucket we must add a new line.
                T newDeletedLine = (T) TableEntry.copy(line);
                newDeletedLine.setDeleted(true);
                newDeletedLine.setBucket(bucketLogical);
                tableEntries.insert(newDeletedLine, false);
                if (useHistory) {
                    tableLog.logAfter(TableLog.LogAction.INSERT, bucketLogical, line);
                }
            }
        }
    }

    @SafeVarargs
    public final void addToBucket(Path bucket, T... lines) {
        addToBucket(bucket, Arrays.asList(lines));
    }

    protected void updateFromTempFile(Path file, Path tempFile) throws IOException {
        Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    @SuppressWarnings("squid:S00108") // Emtpy blocks on purpose (enough to force meta data update)
    protected void updateFolderMetadata() {
        try {
            try (Stream<Path> paths = Files.list(getRootPath())) {
            }
            if (Files.exists(getFolderPath())) {
                try (Stream<Path> paths = Files.list(getFolderPath())) {
                }
            }
        } catch (IOException e) {
            log.warn("Error when listing dirs (to force refresh of meta data)", e);
        }
    }

    /**
     * Get a property that could be either defined in config or in the table it self. The table has preference.
     *
     * @param key the property key.
     * @param def the value to use if the key is not found.
     * @return the value of
     */
    public String getConfigTableProperty(String key, String def) {
        if (header.containsProperty(key)) {
            return header.getProperty(key);
        } else {
            return System.getProperty(key, def);
        }
    }

    protected abstract static class Builder<B extends Builder<B>> {
        protected Path path;
        protected Boolean useHistory;
        protected String tagColumn;
        protected String securityContext;
        protected Boolean validateFiles;
        protected Boolean uniqueTags;

        public Builder(Path path) {
            this.path = path;
        }

        @SuppressWarnings("unchecked")
        protected final B self() {
            return (B) this;
        }

        public B tagColumn(String tagColumn) {
            this.tagColumn = tagColumn;
            return self();
        }

        public B useHistory(boolean useHistory) {
            this.useHistory = useHistory;
            return self();
        }

        public B securityContext(String val) {
            this.securityContext = val;
            return self();
        }

        public B validateFiles(boolean val) {
            this.validateFiles = val;
            return self();
        }

        public B uniqueTags(boolean val) {
            this.uniqueTags = val;
            return self();
        }

        public abstract BaseTable build();
    }

    /**
     * Helper class for passing in row filtering criteria.
     */
    public class TableFilter {
        String[] files;
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
            this.files = val != null ? Arrays.stream(val).map(f -> relativize(rootUri, f)).toArray(String[]::new) : null;
            return this;
        }

        /**
         * Filter for files names (content)
         * @param val file names to filte by, absolute or relative to the table.
         * @return return new filter on files.
         */
        public TableFilter files(URI... val) {
            this.files = val != null ? Arrays.stream(val).map(f -> relativize(rootUri, f.toString())).toArray(String[]::new) : null;
            return this;
        }

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
            this.buckets = val != null ? Arrays.stream(val).map(b -> relativize(rootUri, b)).toArray(String[]::new) : null;
            return this;
        }

        public TableFilter buckets(Path... val) {
            this.buckets = val != null ? Arrays.stream(val).map(b -> relativize(rootUri, b.toString())).toArray(String[]::new) : null;
            return this;
        }

        public TableFilter chrRange(String val) {
            GenomicRange gr = GenomicRange.parseGenomicRange(val);
            this.chrRange = gr != null ? gr.format() : null;
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
         * @return <true> if the line matches the filter otherwise <false>.
         */
        protected boolean match(T l) {
           return ((!l.isDeleted() || includeDeleted || buckets != null)
                            && ((files == null && tags == null && buckets == null && chrRange == null)
                            ||
                            ((files == null || Stream.of(files).anyMatch(f -> f.equals(l.getContentRelative())))
                                    && (tags == null || (l.getTags().length == 0 && tags.length == 0)
                                        || (matchAllTags ? Stream.of(tags).allMatch(t -> ArrayUtils.contains(l.getTags(), t)) : Stream.of(tags).anyMatch(t -> ArrayUtils.contains(l.getTags(), t))))
                                    && (buckets == null || (!l.hasBucket() && buckets.length == 0) ||
                                        (l.hasBucket() && Stream.of(buckets).anyMatch(b -> b.equals(l.getBucket()))))
                                    && (chrRange == null || (l.getRange() != null && chrRange.equals(l.getRange().format())))
                            )
                    )
           );
        }

        public List<T> get() {
            log.debug("Selecting lines from dictionary {}", getName());
            // Set intial candiates for search (this also forces load if not loaded and populates the tagHashToLines index)
            List<T> lines2Search = getEntries(tags);

            return lines2Search.stream().filter(this::match)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        public BaseTable<T> getTable() {
            return BaseTable.this;
        }

    }
}
