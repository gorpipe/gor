package org.gorpipe.gor.table.files;

import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.table.livecycle.TableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Table class representing gor file (gor/gorz)
 *
 * The internal data is stored in temp files.
 *
 */
public class GorTable<T extends Row> extends FileTable<T> {

    private static final Logger log = LoggerFactory.getLogger(GorTable.class);

    public GorTable(TableBuilder builder) {
        super(builder);
    }

    public GorTable(String uri, FileReader inputFileReader) {
        super(uri, inputFileReader);
    }

    public GorTable(String uri) {
        this(uri, null);
    }


    @Override
    protected String getInputTempFileEnding() {
        return DataType.GOR.suffix;
    }

    @Override
    protected String getGorCommand() {
        return "gor";
    }

    @Override
    protected String createInsertTempFileCommand(String mainFile, String outFile, String... insertFiles) {
        String mainFileString = fileReader.exists(mainFile) ? mainFile : "";
        return String.format("%s %s %s %s | write %s", getGorCommand(), mainFileString, String.join(" ", insertFiles),
                getPostProcessing(), outFile);
    }


}
