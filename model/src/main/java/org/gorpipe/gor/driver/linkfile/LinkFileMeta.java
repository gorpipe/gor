package org.gorpipe.gor.driver.linkfile;

import org.apache.commons.lang3.StringUtils;
import org.gorpipe.gor.model.BaseMeta;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.util.Strings;

public class LinkFileMeta extends BaseMeta {
    public static final String HEADER_ENTRIES_COUNT_MAX_KEY = "ENTRIES_COUNT_MAX";
    public static final String HEADER_ENTRIES_AGE_MAX_KEY = "ENTRIES_AGE_MAX";

    public static final String[] DEFAULT_TABLE_HEADER = new String[] {"File", "Timestamp", "MD5", "Serial"};

    public static final int DEFAULT_ENTRIES_COUNT_MAX = 100;
    public static final long DEFAULT_ENTRIES_AGE_MAX = 315360000000L;

    public static LinkFileMeta createAndLoad(FileReader fileReader, String metaPath) {
        LinkFileMeta meta = new LinkFileMeta();
        meta.loadAndMergeMeta(fileReader, metaPath);
        return meta;
    }

    public static LinkFileMeta createAndLoad(String metaContent) {
        LinkFileMeta meta = new LinkFileMeta();
        if (!Strings.isNullOrEmpty(metaContent)) {
            meta.loadAndMergeMeta(metaContent);
        }
        return meta;
    }

    public LinkFileMeta() {
        super();
        setFileHeader(DEFAULT_TABLE_HEADER);
        saveHeaderLine = true;
    }

    @Override
    protected void parseHeaderLine(String line) {
        String columnsString = StringUtils.strip(line, "\n #");
        if (columnsString.length() > 0) {
            setFileHeader(columnsString.split("[\t,]", -1));
        }
    }

    public int getEntriesCountMax() {
        return getPropertyInt(HEADER_ENTRIES_COUNT_MAX_KEY, DEFAULT_ENTRIES_COUNT_MAX);
    }

    public void setEntriesCountMax(int entriesCountMax) {
        setProperty(HEADER_ENTRIES_COUNT_MAX_KEY, String.valueOf(entriesCountMax));
    }

    public long getEntriesAgeMax() {
        return getPropertyLong(HEADER_ENTRIES_AGE_MAX_KEY, DEFAULT_ENTRIES_AGE_MAX);
    }

    public void setEntriesAgeMax(int entriesAgeMax) {
        setProperty(HEADER_ENTRIES_AGE_MAX_KEY, String.valueOf(entriesAgeMax));
    }

    public static String getDefaultMetaContent() {
        return String.format("""
                ## SERIAL = 0
                ## ENTRIES_AGE_MAX = %d
                ## ENTRIES_COUNT_MAX = %d
                ## VERSION = 1
                """, DEFAULT_ENTRIES_AGE_MAX, DEFAULT_ENTRIES_COUNT_MAX);
    }
}
