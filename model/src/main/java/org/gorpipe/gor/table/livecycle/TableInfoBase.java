package org.gorpipe.gor.table.livecycle;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GorOptions;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.gor.table.TableHeader;
import org.gorpipe.gor.table.TableInfo;
import org.gorpipe.gor.table.dictionary.DictionaryTableMeta;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.gor.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.stream.Stream;

import static org.gorpipe.gor.table.util.PathUtils.*;

/**
 * Abstract class representing table (table info part).
 * Covers:
 * - basic table info (name, path, etc).
 * - table metadata, including basic metadata loading.
 * <p>
 * Does not cover:
 * - building blocks/mechanism for updating table (including support for transactions and two phase commit).
 * - Loading of data/entries
 *
 * @param <T>
 */
public abstract class TableInfoBase<T> implements TableInfo<T> {

    private static final Logger log = LoggerFactory.getLogger(TableInfoBase.class);

    protected static final boolean FORCE_SAME_COLUMN_NAMES = Boolean.parseBoolean(System.getProperty("gor.table.validate.columnNames", "true"));
    public static final String HISTORY_DIR_NAME = "history";

    private final String path;        // Path to the table (currently absolute instead of real for compatibility with older code).
    private final String folderPath;    // Path to the table folder.  The table folder is hidden folder that sits next to the
                                        // table and contains various files related to it.
    private final String rootUri;       // uri to table root (just to improve performance when working with uri's).
    private final String name;          // Name of the table.
    protected String id = null;           // Unique id (based on full path (and possibly timestamp), just so we don't always have to refer to full path).

    protected TableHeader header; // Header info.

    protected final FileReader fileReader;

    protected boolean useEmbeddedHeader = false;  // Should the header be embedded in the table file stored in header file the table data dir.


    /**
     * Main constructor.
     *
     * @param uri              path to the dictionary file.
     */
    public TableInfoBase(String uri, FileReader inputFileReader) {
        var secureFileReader = inputFileReader != null ? inputFileReader : ProjectContext.DEFAULT_READER;
        DataSource source = secureFileReader.resolveUrl(uri);

        this.fileReader = secureFileReader;
        var realUri = source.getTopSourceReference().getUrl();

        var fileName = PathUtils.getFileName(source.getFullPath());
        this.name = FilenameUtils.removeExtension(fileName);

        if (GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME.equals(fileName)) {
            // thedict passed in (gord folder content)
            this.rootUri = normalize(PathUtils.getParent(realUri));
            this.path = PathUtils.resolve(rootUri, GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME);
            this.folderPath = rootUri;
        } else if (safeCheckExists(PathUtils.resolve(realUri, GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME))) {
            // Not all data sources support isDirectory (so just check for the dict file)
            // Gord folder passed in.
            this.rootUri = realUri;
            this.path =  PathUtils.resolve(rootUri, GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME);
            this.folderPath = rootUri;
        } else {
            // Old school dict.
            this.rootUri = normalize(PathUtils.getParent(realUri));
            this.path = PathUtils.resolve(rootUri, fileName);
            this.folderPath = PathUtils.resolve(rootUri, "." + this.name);
        }

        this.header = new DictionaryTableMeta();

        reload();
    }

    protected TableInfoBase(String uri) {
        this(uri, null);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public String getRootPath() {
        return rootUri;
    }

    @Override
    public String getFolderPath() {
        return folderPath;
    }

    @Override
    public String[] getColumns() {
        return this.header.getColumns();
    }

    @Override
    public String getProperty(String key) {
        return this.header.getProperty(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return this.header.containsProperty(key);
    }

    @Override
    public String formatHeader() {
        return header.formatHeader();
    }

    public String getSecurityContext() {
        return fileReader != null ? fileReader.getSecurityContext() : "";
    }

    public boolean isUseEmbeddedHeader() {
        return useEmbeddedHeader;
    }

    /**
     * Get ID for the table, based on path and timestamp.
     * @return ID for the table.
     */
    public String getId() {
        // Lazy initialization.
        if (this.id == null) {
            if (this.header.getProperty(TableHeader.HEADER_SERIAL_KEY) == null) {
                loadMeta();
            }
            String serial = this.header.getProperty(TableHeader.HEADER_SERIAL_KEY);
            serial = serial != null ? serial : "";
            try {
                this.id = this.fileReader.resolveUrl(path).getSourceMetadata().getUniqueId() + serial;
            } catch (IOException e) {
                // Assuming we could not access the source meta.
                this.id = Util.md5(this.path) + serial;
            }
        }
        return this.id;
    }

    public String getProjectPath() {
        return fileReader != null && fileReader.getCommonRoot() != null ? fileReader.getCommonRoot() : PathUtils.getCurrentAbsolutePath();
    }

    protected String getContentReal(String relativePath) {
        return resolve(getRootPath(), relativePath);
    }

    public boolean isValidateFiles() {
        return Boolean.parseBoolean(getConfigTableProperty(TableHeader.HEADER_VALIDATE_FILES_KEY,
                System.getProperty("GOR_TABLE_VALIDATE_FILES", "true")));
    }

    public boolean isUseHistory() {
        return Boolean.parseBoolean(getConfigTableProperty(TableHeader.HEADER_USE_HISTORY_KEY, "true"));
    }

    public FileReader getFileReader() {
        return fileReader;
    }


    /**
     * Reload from file.
     * Note:  Reload is called when we open read transaction.
     */
    public void reload() {
        log.debug("Loading table {}", getName());
        loadMeta();
    }

    public String getMetaPath() {
        return DataUtil.toFile(getPath(), DataType.META);
    }

    /**
     * Get a property that could be either defined in config or in the table itself. The table has preference.
         *
     * @param key the property key.
     * @param def the value to use if the key is not found.
     * @return the value of
     */
    public String getConfigTableProperty(String key, String def) {
        if (header.containsProperty(key)) {
            return header.getProperty(key);
        } else {
            String env = System.getenv("GOR_TABLE_" + key);
            return env != null ? env : def;
        }
    }

    public Boolean getBooleanConfigTableProperty(String key, Boolean def) {
        String configValue = getConfigTableProperty(key, null);
        return configValue != null ? Boolean.valueOf(configValue) : def;
    }

    @SuppressWarnings("squid:S00108") // Emtpy blocks on purpose (enough to force meta data update)
    public void updateNFSFolderMetadata() {
        if (isLocal(getRootPath())) {
            try {
                try (Stream<String> paths = fileReader.list(rootUri)) {
                    // Intentionally empty.
                }
                if (fileReader.exists(getFolderPath())) {
                    try (Stream<String> paths = fileReader.list(getFolderPath())) {
                        // Intentionally empty.
                    }
                }
            } catch (IOException e) {
                log.warn("Error when listing dirs (to force refresh of meta data)", e);
            }
        }
    }

    /**
     * Parse/load the table header.
     */
    protected void loadMeta() {
        log.debug("Parsing header for {}", getName());

        this.header.clear();
        // Add props in increasing priority order.
        //this.header.loadAndMergeMeta(fileReader, PathUtils.resolve(getFolderPath(), "header")); // For backward compatibility.
        this.header.loadAndMergeMeta(fileReader, getPath());
        this.header.loadAndMergeMeta(fileReader, DataUtil.toFile(getPath(), DataType.META));
    }


    /**
     * Validate that the content of the given file matches the content of the table.
     * @param file  file to validate
     */
    protected void validateFile(String file) {
        // Validate file existence.
        log.trace("Start validating file");
        try {
            if (!fileReader.exists(file) && PathUtils.isLocal(file)) {
                throw new GorDataException(String.format("Entry %s does not exists!", file));
            }
        } catch (GorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GorDataException(String.format("Entry %s can not be verified!", file), ex);
        }

        // Validate file columns.
        updateValidateHeader(file);

        log.trace("Done validating file");
    }

    protected void updateValidateHeader(String file) {
        // Validate that line matches header columns.

        TableHeader lineHeader = parseHeaderFromFile(file);

        if (ArrayUtils.isEmpty(this.header.getColumns())
                || (!this.header.isProper() && lineHeader.isProper() && this.header.getColumns().length == lineHeader.getColumns().length)) {
            // Have better header.
            this.header.setColumns(lineHeader.getColumns());
        } else {
            // Validate the header.
            if (this.header.getColumns().length != lineHeader.getColumns().length) {
                throw new GorDataException(String.format("Can not update dictionary %s. The number of columns does not match (dict: %d, line: %d)",
                        getPath(), this.header.getColumns().length, lineHeader.getColumns().length), -1, lineHeader.toString(), header.toString());
            }

            if (FORCE_SAME_COLUMN_NAMES && this.header.isProper() && lineHeader.isProper() &&
                    !String.join(",", this.header.getColumns()).equals(String.join(",", lineHeader.getColumns()))) {
                throw new GorDataException(String.format("Can not update dictionary %s.  The columns do not match (dict: %s vs line: %s)",
                        getPath(), String.join(",", this.header.getColumns()), String.join(",",
                                lineHeader.getColumns())), -1, lineHeader.toString(), header.toString() );
            }
        }
    }

    private boolean safeCheckExists(String path) {
        try {
            return fileReader.exists(path);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Collect header information from the table line (i.e. the file the line references).
     *
     * @param file the data file.
     * @return new header object inferred from the table file.
     */
    protected TableHeader parseHeaderFromFile(String file) {
        TableHeader newHeader = new TableHeader();

        try {
            String headerLine = fileReader.readHeaderLine(file);
            newHeader.setColumns(headerLine != null ? headerLine.split("\t") : new String[]{""});
        } catch (Exception e) {
            throw new GorDataException("Could not get header for validation from input file " + file, e);
        }

        return newHeader;
    }
}
