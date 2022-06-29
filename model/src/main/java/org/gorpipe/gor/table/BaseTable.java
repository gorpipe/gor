package org.gorpipe.gor.table;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GorOptions;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import static org.gorpipe.gor.table.util.PathUtils.isLocal;
import static org.gorpipe.gor.table.util.PathUtils.normalize;

public abstract class BaseTable<T> implements Table<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseTable.class);

    private static final boolean DEFAULT_VALIDATE_FILES = Boolean.parseBoolean(System.getProperty("GOR_TABLE_FILES_VALIDATE", "true"));
    protected static final boolean FORCE_SAME_COLUMN_NAMES = false;
    public static final String HISTORY_DIR_NAME = "history";

    protected final URI path;           // Path to the table (currently absolute instead of real for compatibility with older code).
    private final URI folderPath;       // Path to the table folder.  The table folder is hidden folder that sits next to the
                                        // table and contains various files related to it.
    private final URI rootUri;          // uri to table root (just to improve performance when working with uri's).
    private final String name;          // Name of the table.
    private String id = null;           // Unique id (based on full path, just so we don't always have to refer to full path).
    protected String prevSerial;        //

    protected TableHeader header; // Header info.

    protected final URI historyDir;     // Backup dir for older versions of this dictionary (absolute) (if requested).
    private boolean useHistory = true;
    private boolean validateFiles = DEFAULT_VALIDATE_FILES;

    protected final TableLog tableLog;
    protected FileReader fileReader;

    protected BaseTable(Builder builder) {
        this(builder.path, builder.fileReader);

        if (builder.validateFiles != null) {
            setValidateFiles(builder.validateFiles);
        }
        if (builder.useHistory != null) {
            setUseHistory(builder.useHistory);
        }
    }

    /**
     * Main constructor.
     *
     * @param uri              path to the dictionary file.
     */
    protected BaseTable(URI uri, FileReader inputFileReader) {
        this.fileReader = inputFileReader != null ? inputFileReader : ProjectContext.DEFAULT_READER;

        var fileName = FilenameUtils.getName(uri.getPath());
        this.name = FilenameUtils.removeExtension(fileName);

        // Not all datasources support isDirectory (so just check for the dict file)
        if (safeCheckExists(PathUtils.resolve(uri, GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME).toString())) {
            // Gord folder passed in.
            this.rootUri = PathUtils.toURIFolder(uri.toString());
            this.path =  PathUtils.resolve(rootUri, GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME);
            this.folderPath = rootUri;
        } else if (GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME.equals(fileName)) {
            // thedict passed in (gord folder content)
            this.rootUri = normalize(PathUtils.parent(uri));
            this.path = PathUtils.resolve(rootUri, GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME);
            this.folderPath = rootUri;
        } else {
            // Old school dict.
            this.rootUri = normalize(PathUtils.parent(uri));
            this.path = PathUtils.resolve(rootUri, fileName);
            this.folderPath = PathUtils.toURIFolder(PathUtils.resolve(rootUri, "." + this.name).toString()); // PathUtils.resolve(rootUri, "." + this.name);
        }

        this.historyDir = PathUtils.toURIFolder(PathUtils.resolve(folderPath, HISTORY_DIR_NAME).toString()); // PathUtils.resolve(folderPath, HISTORY_DIR_NAME);
        this.tableLog = new TableLog(historyDir);
    }

    protected BaseTable(URI uri) {
        this(uri, null);
    }

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

    public Path getPath() {
        return PathUtils.toPath(this.path);
    }

    public URI getPathUri() {
        return this.path;
    }

    public Path getRootPath() {
        return PathUtils.toPath(rootUri);
    }

    public URI getRootUri() {
        return rootUri;
    }

    public TableHeader getHeader() {
        return header;
    }
    /**
     * Get the table folder path.   The table folder is hidden folder that sits next to
     * the dictionary and contains various files related to it.
     *
     * @return the table folder path.
     */
    public URI getFolderUri() {
        return folderPath;
    }

    // For testing and code that just runs on local.
    public Path getFolderPath() {
        return PathUtils.toPath(folderPath);
    }

    public void setColumns(String... columns) {
        this.header.setColumns(columns);
    }

    public String[] getColumns() {
        return this.header.getColumns();
    }

    public String getSecurityContext() {
        return fileReader != null ? fileReader.getSecurityContext() : "";
    }


    /**
     * Get table property {@code key}
     *
     * @param key property to get.
     * @return table properyt {@code key}
     */
    public String getProperty(String key) {
        return this.header.getProperty(key);
    }

    /**
     * Set table property {@code key}
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

    public boolean isUseHistory() {
        return this.useHistory;
    }

    public void setUseHistory(boolean useHistory){
        this.useHistory = useHistory;
    }

    public FileReader getFileReader() {
        return fileReader;
    }

    public void setFileReader(FileReader fileReader) {
        this.fileReader = fileReader;
    }

    @Override
    public void initialize() {
        log.trace("Initialize {}", getName());

        if (!fileReader.exists(getRootUri().toString())) {
            throw new GorSystemException("Table " + path + " can not be created as the parent path does not exists!", null);
        }

        try {
            fileReader.createDirectoryIfNotExists(getFolderUri().toString(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));

            if (this.isUseHistory()) {
                fileReader.createDirectoryIfNotExists(this.historyDir.toString(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
            }
        } catch (IOException ioe) {
            throw new GorSystemException("Could not create directory", ioe);
        }

        if (!fileReader.exists(path.toString())) {
            // Create the header.
            this.header.setProperty(TableHeader.HEADER_FILE_FORMAT_KEY, "1.0");
            this.header.setProperty(TableHeader.HEADER_CREATED_KEY, new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
        }
    }

    /**
     * Reload from file.
     * Note:  Reload is called when we open read transaction.
     */
    public void reload() {

        log.debug("Loading table {}", getName());
        prevSerial = this.header.getProperty(TableHeader.HEADER_SERIAL_KEY);
        parseHeader();

        validateFiles =  Boolean.parseBoolean(getConfigTableProperty(TableHeader.HEADER_VALIDATE_FILES_KEY, Boolean.toString(validateFiles)));
        useHistory = Boolean.parseBoolean(getConfigTableProperty(TableHeader.HEADER_USE_HISTORY_KEY, Boolean.toString(useHistory)));
    }


    @Override
    public void commitRequest() {
        initialize();
        updateMetaBeforeSave();
        saveTempMainFile();
        saveTempMetaFile();
    }

    @Override
    public void commit() {
        try {
            updateFromTempFile(getTempMetaFileName(), getMetaFileName());
            updateFromTempFile(getTempMainFileName(), getPath().toString());
        } catch (IOException e) {
            throw new GorSystemException("Could not commit " + getPath(), e);
        }

        if (isUseHistory()) {
            tableLog.commit(fileReader);
        }
    }

    /**
     * Update the dictionary file from this.
     */
    public void save() {
        commitRequest();
        commit();
    }

    protected void updateMetaBeforeSave() {
        this.header.setProperty(TableHeader.HEADER_USE_HISTORY_KEY, Boolean.toString(this.isUseHistory()));
        this.header.setProperty(TableHeader.HEADER_VALIDATE_FILES_KEY, Boolean.toString(this.isValidateFiles()));

        String oldSerial = this.header.getProperty(TableHeader.HEADER_SERIAL_KEY);
        this.header.setProperty(TableHeader.HEADER_SERIAL_KEY, oldSerial != null ? String.valueOf(Long.parseLong(oldSerial) + 1) : "1");
    }

    protected void saveTempMetaFile() {
        try(OutputStream os = fileReader.getOutputStream(getTempMetaFileName()))  {
            os.write(header.formatHeader().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ioe) {
            throw new GorSystemException("Could not save meta file", ioe);
        }
    }

    protected abstract void saveTempMainFile();

    protected String getMetaFileName() {
        return getPath().toString() + ".meta";
    }

    protected String getTempMainFileName() {
        return getTempFileName(getPath().toString());
    }

    protected String getTempMetaFileName() {
        return getTempFileName(getMetaFileName());
    }

    protected String getTempFileName(String pathString) {
        pathString = insertTempIntoFileName(pathString);
        pathString = insertTableFolderIntoFilePath(pathString);
        return pathString;
    }

    private String insertTableFolderIntoFilePath(String pathString) {
        String fileName = Path.of(pathString).getFileName().toString();
        return PathUtils.resolve(getFolderPath(), (fileName)).toString();
    }

    private String insertTempIntoFileName(String pathString) {
        int lastDotIndex = pathString.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return pathString + ".temp";
        } else {
            return pathString.substring(0, lastDotIndex ) + ".temp" + pathString.substring(lastDotIndex);
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

    public Boolean getBooleanConfigTableProperty(String key, Boolean def) {
        String configValue = getConfigTableProperty(key, null);
        return configValue != null ? Boolean.valueOf(configValue) : def;
    }

    @SuppressWarnings("squid:S00108") // Emtpy blocks on purpose (enough to force meta data update)
    public void updateNFSFolderMetadata() {
        if (isLocal(getRootUri())) {
            try {
                try (Stream<String> paths = fileReader.list(rootUri.toString())) {
                }
                if (fileReader.exists(getFolderPath().toString())) {
                    try (Stream<String> paths = fileReader.list(getFolderPath().toString())) {
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
    protected void parseHeader() {
        log.debug("Parsing header for {}", getName());
        this.header.clear();

        this.header.loadAndMergeMeta(getFolderPath().resolve("header")); // For backward compatibility.
        this.header.loadAndMergeMeta(getPath());
        this.header.loadAndMergeMeta(Path.of(getPath().toString() + ".meta"));
    }


    /**
     * Validate that the content of the given file matches the content of the table.
     * @param file
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

    protected void updateFromTempFile(String tempFile, String file) throws IOException {
        log.debug("Updating main file ({}) from {}", file, tempFile);
        if (fileReader.exists(tempFile)) {
            fileReader.move(tempFile, file);
        }
    }

    // Util method.
    protected void logAfter(TableLog.LogAction action, String argument, String... lines) {
        if (useHistory) {
            for (String line : lines) {
                tableLog.logAfter(action, argument, line);
            }
        }
    }

    protected abstract static class Builder<B extends Builder<B>> {
        protected URI path;
        protected Boolean useHistory;
        protected Boolean validateFiles;
        protected FileReader fileReader;

        protected Builder(URI path) {
            this.path = path;
        }

        @SuppressWarnings("unchecked")
        protected final B self() {
            return (B) this;
        }

        public B fileReader(FileReader val) {
            this.fileReader = val;
            return self();
        }

        public B useHistory(boolean useHistory) {
            this.useHistory = useHistory;
            return self();
        }

        public B validateFiles(boolean val) {
            this.validateFiles = val;
            return self();
        }

        public abstract BaseTable build();
    }

}
