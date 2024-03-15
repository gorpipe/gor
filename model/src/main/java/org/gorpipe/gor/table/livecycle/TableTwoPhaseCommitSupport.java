package org.gorpipe.gor.table.livecycle;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.TableHeader;
import org.gorpipe.gor.table.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


/**
 * Helper class to implement table live cycle with two phase commit.
 *
 * @param <T>
 */
public abstract class TableTwoPhaseCommitSupport extends TableLifeCycleSupport implements TableTwoPhaseCommit {

    private static final Logger log = LoggerFactory.getLogger(TableTwoPhaseCommitSupport.class);

    /**
     * Main constructor.
     *
     * @param table              table to support.
     */
    public TableTwoPhaseCommitSupport(TableInfoBase table) {
        super(table);
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
            updateFromTempFile(getTempMetaFileName(), table.getMetaPath());
            updateFromTempFile(getTempMainFileName(), table.getPath());
        } catch (IOException e) {
            throw new GorSystemException("Could not commit " + table.getPath(), e);
        }

        if (table.isUseHistory()) {
            tableLog.commit(table.getFileReader());
        }
    }

    /**
     * Update the dictionary file from this.
     */
    @Override
    public void save() {
        commitRequest();
        commit();
        explicitlySetHeaderProbs.clear();
    }

    protected void updateMetaBeforeSave() {
        String oldSerial = table.header.getProperty(TableHeader.HEADER_SERIAL_KEY);
        table.header.setProperty(TableHeader.HEADER_SERIAL_KEY, oldSerial != null ? String.valueOf(Long.parseLong(oldSerial) + 1) : "1");
    }

    protected void saveTempMetaFile() {
        try(OutputStream os = table.fileReader.getOutputStream(getTempMetaFileName()))  {
            os.write(table.header.formatHeader().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ioe) {
            throw new GorSystemException("Could not save meta file", ioe);
        }
    }

    protected abstract void saveTempMainFile();

    protected String getTempMainFileName() {
        return getTempFileName(table.getPath());
    }

    protected String getTempMetaFileName() {
        return getTempFileName(table.getMetaPath());
    }

    protected String getTempFileName(String pathString) {
        pathString = insertTempIntoFileName(pathString);
        pathString = insertTableFolderIntoFilePath(pathString);
        return pathString;
    }

    private String insertTableFolderIntoFilePath(String pathString) {
        String fileName = PathUtils.getFileName(pathString);
        return PathUtils.resolve(table.getFolderPath(), (fileName));
    }

    private String insertTempIntoFileName(String pathString) {
        int lastDotIndex = pathString.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return pathString + ".temp";
        } else {
            return pathString.substring(0, lastDotIndex ) + ".temp" + pathString.substring(lastDotIndex);
        }
    }

    protected void updateFromTempFile(String tempFile, String file) throws IOException {
        log.debug("Updating main file ({}) from {}", file, tempFile);
        if (table.fileReader.exists(tempFile)) {
            table.fileReader.move(tempFile, file);
        }
    }
}
