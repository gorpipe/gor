package org.gorpipe.gor.table.files;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.model.RowBase;
import org.gorpipe.gor.table.livecycle.TableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Table class representing nor file.
 */
public class NorTable<T extends Row> extends FileTable<T> {

    public static final String HEADER_PRIMARY_KEY_KEY = "PRIMARY_KEY";

    private static final Logger log = LoggerFactory.getLogger(NorTable.class);

    public NorTable(TableBuilder builder) {
        super(builder);
    }

    public NorTable(String uri, FileReader inputFileReader) {
        super(uri, inputFileReader);
    }

    public NorTable(String uri) {
        this(uri, null);
    }

    @Override
    protected String getInputTempFileEnding() {
        return DataType.NOR.suffix;
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
    protected String createInsertTempFileCommand(String mainFile, String outFile, String... insertFiles) {
        if (insertFiles.length != 1) {
            throw new GorSystemException("Insert into nor table only supports 1 file at time", null);
        }

        String insertFile = insertFiles[0];

        String key = getProperty(HEADER_PRIMARY_KEY_KEY);

        String postProcessing = getPostProcessing();

        if (key == null) {
            if (fileReader.exists(mainFile)) {
                // Only pick lines from the orignal file that are not in the insert file (using the primary key field).
                return String.format("%s %s | merge %s %s | write %s",
                        getGorCommand(), mainFile, insertFile, postProcessing, outFile );
            } else {
                return String.format("%s %s %s | write %s", getGorCommand(), insertFile, postProcessing, outFile);
            }
        } else {
            if (fileReader.exists(mainFile)) {
                // Only pick lines from the orignal file that are not in the insert file (using the primary key field).
                return String.format("%s %s | map <(%s %s) -c %s -n %s -m 'Include' | where %sx = 'Include' | hide %sx" +
                                " | merge %s | sort -c %s %s | write %s",
                        getGorCommand(), mainFile, getGorCommand(), insertFile, key, key, key, key,
                        insertFile, key, postProcessing, outFile );
            } else {
                return String.format("%s %s | sort -c %s %s | write %s", getGorCommand(), insertFile, key, postProcessing, outFile);
            }
        }
    }
}
