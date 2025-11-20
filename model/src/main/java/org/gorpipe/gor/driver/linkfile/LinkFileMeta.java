package org.gorpipe.gor.driver.linkfile;

import org.apache.commons.lang3.StringUtils;
import org.gorpipe.gor.model.BaseMeta;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.util.Strings;

public class LinkFileMeta extends BaseMeta {

    // Max number of entries to keep track of in the link file.
    public static final String HEADER_ENTRIES_COUNT_MAX_KEY = "ENTRIES_COUNT_MAX";
    // Max age of entries to keep track of in the link file.
    public static final String HEADER_ENTRIES_AGE_MAX_KEY = "ENTRIES_AGE_MAX";
    // Determines if the content data location should be managed.
    public static final String HEADER_CONTENT_LOCATION_MANAGED_KEY = "CONTENT_LOCATION_MANAGED";
    // Should the content lifecycle be managed (data deleted if the link is removed from the link file).
    public static final String HEADER_CONTENT_LIFECYCLE_MANAGED_KEY = "CONTENT_LIFECYCLE_MANAGED";

    public static final String[] DEFAULT_TABLE_HEADER = new String[] {"File", "Timestamp", "MD5", "Serial", "Info"};

    public static final int DEFAULT_ENTRIES_COUNT_MAX = 100;
    public static final long DEFAULT_ENTRIES_AGE_MAX = Long.MAX_VALUE;

    public static LinkFileMeta createAndLoad(String metaContent) {
        LinkFileMeta meta = new LinkFileMeta();
        if (Strings.isNullOrEmpty(metaContent)) {
            meta.loadAndMergeMeta(getDefaultMetaContent());
        } else {
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
        if (!columnsString.isBlank()) {
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

    @Override
    public String getVersion() {
        return getProperty(HEADER_VERSION_KEY, "0");
    }

    public static String getDefaultMetaContent() {
        return String.format("""
                ## SERIAL = 0
                ## VERSION = 1
                """);
    }
}
