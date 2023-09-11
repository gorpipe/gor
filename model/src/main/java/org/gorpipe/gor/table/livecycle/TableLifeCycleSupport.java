package org.gorpipe.gor.table.livecycle;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.TableHeader;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.table.util.TableLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static org.gorpipe.gor.table.livecycle.TableInfoBase.HISTORY_DIR_NAME;

/**
 * Helper class to implement table live cycle with two phase commit.
 *
 * @param <T>
 */
public abstract class TableLifeCycleSupport<T>  implements TableLifeCycle<T> {

    private static final Logger log = LoggerFactory.getLogger(TableLifeCycleSupport.class);

    protected HashMap<String, String> explicitlySetHeaderProbs = new HashMap<>(); // Header properties that were explicitly set by the user.

    protected final TableLog tableLog;

    protected TableInfoBase<T> table;


    /**
     * Main constructor.
     *
     * @param table              table to support.
     */
    public TableLifeCycleSupport(TableInfoBase<T> table) {
        this.table = table;
        this.tableLog = new TableLog(PathUtils.resolve(table.getFolderPath(), HISTORY_DIR_NAME));
    }

    public void setColumns(String... columns) {
        table.header.setColumns(columns);
    }

    /**
     * Set table property {@code key}
     *
     * @param key   property name.
     * @param value property value
     */
    public void setProperty(String key, String value) {
        table.header.setProperty(key, value);
        explicitlySetHeaderProbs.put(key, value);
    }

    public void setValidateFiles(boolean validateFiles) {
        setProperty(TableHeader.HEADER_VALIDATE_FILES_KEY, validateFiles ? "true" : "false");
    }

    public void setUseHistory(boolean useHistory){
       setProperty(TableHeader.HEADER_USE_HISTORY_KEY, useHistory ? "true" : "false");
    }

    @Override
    public void initialize() {
        log.trace("Initialize {}", table.getName());

        if (!table.getFileReader().exists(table.getPath())) {
            if (!table.getFileReader().exists(table.getRootPath())) {
                throw new GorSystemException("Table " + table.getPath() + " can not be created as the parent path does not exists!", null);
            }

            // Create the header.
            table.header.setProperty(TableHeader.HEADER_FILE_FORMAT_KEY, "1.0");
            table.header.setProperty(TableHeader.HEADER_CREATED_KEY, new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
        }

        try {
            table.getFileReader().createDirectoryIfNotExists(table.getFolderPath(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));

            if (table.isUseHistory()) {
                table.getFileReader().createDirectories(tableLog.getLogDir(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
            }
        } catch (IOException ioe) {
            throw new GorSystemException("Could not create directory", ioe);
        }


    }

    public void loadMeta() {
        table.header.setProperties(explicitlySetHeaderProbs);
    }

    @Override
    public void delete() {
        if (table.getFileReader().exists(table.getFolderPath())) {
            try {
                table.getFileReader().deleteDirectory(table.getFolderPath());
            } catch (IOException e) {
                // Best effort.
                log.warn("Could not delete table directory: " + table.getFolderPath(), e);
            }
        }

        if (table.getFileReader().exists(table.getMetaPath())) {
            try {
                table.getFileReader().delete(table.getMetaPath());
            } catch (IOException e) {
                // Best effort
                log.warn("Could not delete table: " + table.getFolderPath(), e);
            }
        }

        if (table.getFileReader().exists(table.getPath())) {
            try {
                table.getFileReader().delete(table.getPath());
            } catch (IOException e) {
                // Best effort
                log.warn("Could not delete table: " + table.getFolderPath(), e);
            }
        }
    }

    // Util method.
    public void logAfter(TableLog.LogAction action, String argument, String... lines) {
        if (table.isUseHistory()) {
            for (String line : lines) {
                tableLog.logAfter(table.header.getProperty(TableHeader.HEADER_SERIAL_KEY), action, argument, line);
            }
        }
    }
}
