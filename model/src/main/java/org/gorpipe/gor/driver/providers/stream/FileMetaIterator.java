package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.GorMeta;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.gor.util.DynamicRowIterator;

import java.io.IOException;
import java.nio.file.Path;


public class FileMetaIterator extends DynamicRowIterator {

    final static String FILE_PATH = "file.path";
    final static String FILE_NAME = "file.name";
    final static String FILE_TYPE = "file.type";
    final static String FILE_SUFFIX = "file.suffix";
    final static String FILE_SIZE = "file.size";
    final static String FILE_MODIFIED = "file.modified";
    final static String FILE_MODIFIED_UTC = "file.modified.utc";
    final static String FILE_ID = "file.id";
    final static String FILE_SUPPORTS_INDEX = "file.supports.index";
    final static String FILE_SUPPORTS_REFERENCE = "file.supports.reference";
    final static String FILE_REFERENCE = "file.reference";
    final static String FILE_INDEX = "file.index";
    final static String LAST_MODIFIED_UTC = "LastModifiedUtc";

    public void initMeta(StreamSourceFile file) throws IOException {
        var meta = file.getFileSource().getSourceMetadata();
        addRow(FILE_PATH,  meta.getNamedUrl());
        addRow(FILE_NAME,  Path.of(meta.getNamedUrl()).getFileName().toString());
        addRow(FILE_TYPE,  file.getType().name());
        addRow(FILE_SUFFIX,  file.getType().suffix);
        addRow(FILE_SIZE, meta.getLength().toString());
        addRow(FILE_MODIFIED, meta.getLastModified().toString());
        addRow(FILE_MODIFIED_UTC, meta.attributes().getOrDefault(LAST_MODIFIED_UTC, ""));
        addRow(FILE_ID, meta.getUniqueId());
        addRow(FILE_SUPPORTS_INDEX, String.valueOf(file.supportsIndex()));
        addRow(FILE_INDEX, nullToEmpty(file.getIndexSource()));
        addRow(FILE_SUPPORTS_REFERENCE, String.valueOf(file.supportsReference()));
        addRow(FILE_REFERENCE, nullToEmpty(file.getReferenceSource()));
    }

    private static String nullToEmpty(StreamSource data) {
        return data == null ? "" : data.getName();
    }
}
