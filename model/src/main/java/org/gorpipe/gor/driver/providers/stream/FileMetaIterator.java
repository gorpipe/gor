package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.util.DynamicRowIterator;

import java.io.IOException;
import java.nio.file.Path;


public class FileMetaIterator extends DynamicRowIterator {

    final static String SOURCE = "FILE";
    final static String FILE_PATH = "PATH";
    final static String FILE_NAME = "NAME";
    final static String FILE_TYPE = "TYPE";
    final static String FILE_SUFFIX = "SUFFIX";
    final static String FILE_SIZE = "SIZE";
    final static String FILE_MODIFIED = "MODIFIED";
    final static String FILE_MODIFIED_UTC = "MODIFIED_UTC";
    final static String FILE_ID = "ID";
    final static String FILE_SUPPORTS_INDEX = "SUPPORTS_INDEX";
    final static String FILE_SUPPORTS_REFERENCE = "SUPPORTS_REFERENCE";
    final static String FILE_REFERENCE = "REFERENCE";
    final static String FILE_INDEX = "INDEX";
    final static String LAST_MODIFIED_UTC = "LAST_MODIFIED_UTC";

    public void initMeta(StreamSourceFile file) throws IOException {
        var meta = file.getFileSource().getSourceMetadata();
        addRow(SOURCE, FILE_PATH,  meta.getNamedUrl());
        addRow(SOURCE, FILE_NAME,  Path.of(meta.getNamedUrl()).getFileName().toString());
        addRow(SOURCE, FILE_TYPE,  file.getType().name());
        addRow(SOURCE, FILE_SUFFIX,  file.getType().suffix);
        addRow(SOURCE, FILE_SIZE, meta.getLength().toString());
        addRow(SOURCE, FILE_MODIFIED, meta.getLastModified().toString());
        addRow(SOURCE, FILE_MODIFIED_UTC, meta.attributes().getOrDefault(LAST_MODIFIED_UTC, ""));
        addRow(SOURCE, FILE_ID, meta.getUniqueId());
        addRow(SOURCE, FILE_SUPPORTS_INDEX, String.valueOf(file.supportsIndex()));
        addRow(SOURCE, FILE_INDEX, nullToEmpty(file.getIndexSource()));
        addRow(SOURCE, FILE_SUPPORTS_REFERENCE, String.valueOf(file.supportsReference()));
        addRow(SOURCE, FILE_REFERENCE, nullToEmpty(file.getReferenceSource()));
    }

    private static String nullToEmpty(StreamSource data) {
        return data == null ? "" : data.getName();
    }
}
