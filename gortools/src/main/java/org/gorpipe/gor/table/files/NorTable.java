package org.gorpipe.gor.table.files;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.model.RowBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Table class representing nor file.
 */
public class NorTable<T extends Row> extends GorTable<T> {

    public static final String HEADER_PRIMARY_KEY_KEY = "PRIMARY_KEY";

    private static final Logger log = LoggerFactory.getLogger(NorTable.class);

    public NorTable(Builder builder) {
        super(builder);
    }

    public NorTable(URI uri, FileReader inputFileReader) {
        super(uri, inputFileReader);
    }

    public NorTable(URI uri) {
        this(uri, null);
    }

    @Override
    protected String getInputTempFileEnding() {
        return ".nor";
    }

    @Override
    protected String getGorCommand() {
        return "nor";
    }

    @Override
    protected T createRow(String line) {
        return (T)new RowBase("chrN\t0\t" + line);
    }

    @Override
    protected void writeRowToStream(Row r, OutputStream os) throws IOException {
        r.writeNorRowToStream(os);
    }

    @Override
    public void delete(Collection<T> lines) {
        createDeleteTempFile(lines.stream().map(Row::otherCols).toArray(String[]::new));
    }

    @Override
    protected String createInsertTempFileCommand(String... insertFiles) {
        if (insertFiles.length != 1) {
            throw new GorSystemException("Insert into nor table only supports 1 file at time", null);
        }

        String insertFile = insertFiles[0];

        String key = getProperty(HEADER_PRIMARY_KEY_KEY);

        if (key == null) {
            return super.createInsertTempFileCommand(insertFiles);
        }

        Path mainFile = getMainFile();
        tempOutFilePath = getNewTempFileName();

        // Only pick lines from the orignal file that are not in the insert file (using the primary key field).
        return String.format("%s %s | map <(%s %s) -c %s -n %s -m 'Include' | where %sx = 'Include' | hide %sx" +
                        " | merge %s | sort -c %s | write %s",
                getGorCommand(), mainFile, getGorCommand(), insertFile, key, key, key, key, 
                insertFile, key, tempOutFilePath);
    }

    // Need to overwrite this as NOR does not support multiple input files.  Need to merge each file.
    protected String createInsertTempFileCommandString(Path mainFile, String insertPostProcessing, String[] insertFiles) {
        return String.format("%s %s | merge %s %s | write %s", getGorCommand(), mainFile,
                String.join(" | merge ", insertFiles),
                insertPostProcessing, tempOutFilePath);
    }
}

