package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.util.DynamicRowIterator;

import java.io.IOException;
import java.nio.file.Path;


public class SourceMetaIterator extends DynamicRowIterator {

    final static String SOURCE = "SOURCE";
    final static String SOURCE_NAME = "NAME";
    final static String SOURCE_PATH = "PATH";
    final static String SOURCE_DATA_TYPE = "DATA_TYPE";
    final static String SOURCE_TYPE = "TYPE";
    final static String SOURCE_PROTOCOLS = "PROTOCOLS";
    final static String SOURCE_REMOTE = "REMOTE";
    final static String SOURCE_SUPPORTED = "SUPPORTED";
    final static String SOURCE_MODIFIED = "MODIFIED";
    final static String SOURCE_ID = "ID";
    final static String HEADER = "ChrN\tPosN\tsource\tname\tvalue";

    public void initMeta(DataSource source) throws IOException {
        setHeader(HEADER);
        addRow(SOURCE, SOURCE_NAME, source.getName());
        addRow(SOURCE, SOURCE_PATH, source.getFullPath());
        addRow(SOURCE, SOURCE_DATA_TYPE, source.getDataType().name());
        addRow(SOURCE, SOURCE_TYPE, source.getSourceType().getName());
        addRow(SOURCE, SOURCE_PROTOCOLS, String.join(",", source.getSourceType().getProtocols()));
        addRow(SOURCE, SOURCE_REMOTE, String.valueOf(source.getSourceType().isRemote()));
        addRow(SOURCE, SOURCE_SUPPORTED, String.valueOf(source.getSourceType().isSupported()));
        addRow(SOURCE, SOURCE_MODIFIED, String.valueOf(source.getSourceMetadata().getLastModified()));
        addRow(SOURCE, SOURCE_ID, source.getSourceMetadata().getUniqueId());
    }
}
