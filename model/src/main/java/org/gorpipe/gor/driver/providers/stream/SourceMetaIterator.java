package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.util.DynamicRowIterator;

import java.io.IOException;
import java.nio.file.Path;


public class SourceMetaIterator extends DynamicRowIterator {

    final static String SOURCE_NAME = "source.name";
    final static String SOURCE_PATH = "source.path";
    final static String SOURCE_DATA_TYPE = "source.data.type";
    final static String SOURCE_TYPE = "source.type";
    final static String SOURCE_PROTOCOLS = "source.protocols";
    final static String SOURCE_REMOTE = "source.remote";
    final static String SOURCE_SUPPORTED = "source.supported";
    final static String SOURCE_MODIFIED = "source.modified";
    final static String SOURCE_ID = "source.id";
    final static String HEADER = "ChrN\tPosN\tname\tvalue";

    public void initMeta(DataSource source) throws IOException {
        setHeader(HEADER);
        addRow(SOURCE_NAME, source.getName());
        addRow(SOURCE_PATH, source.getFullPath());
        addRow(SOURCE_DATA_TYPE, source.getDataType().name());
        addRow(SOURCE_TYPE, source.getSourceType().getName());
        addRow(SOURCE_PROTOCOLS, String.join(",", source.getSourceType().getProtocols()));
        addRow(SOURCE_REMOTE, String.valueOf(source.getSourceType().isRemote()));
        addRow(SOURCE_SUPPORTED, String.valueOf(source.getSourceType().isSupported()));
        addRow(SOURCE_MODIFIED, String.valueOf(source.getSourceMetadata().getLastModified()));
        addRow(SOURCE_ID, source.getSourceMetadata().getUniqueId());
    }
}
