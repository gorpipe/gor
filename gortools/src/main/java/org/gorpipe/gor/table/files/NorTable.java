package org.gorpipe.gor.table.files;

import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.model.RowBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Table class representing nor file.
 */
public class NorTable<T extends Row> extends GorTable<T> {

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

    protected String getInputTempFileEnding() {
        return ".nor";
    }

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
}

