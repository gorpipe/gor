package org.gorpipe.gor.table.files;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;

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
}
