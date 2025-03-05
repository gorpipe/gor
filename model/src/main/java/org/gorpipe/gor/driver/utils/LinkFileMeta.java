package org.gorpipe.gor.driver.utils;

import org.gorpipe.gor.model.BaseMeta;
import org.gorpipe.gor.model.FileReader;

public class LinkFileMeta extends BaseMeta {
    public static final String HEADER_ENTRIES_COUNT_MAX_KEY = "ENTRIES_COUNT_MAX";
    public static final String HEADER_ENTRIES_AGE_MAX_KEY = "ENTRIES_AGE_MAX";

    public static final int DEFAULT_ENTRIES_COUNT_MAX = 100;
    public static final long DEFAULT_ENTRIES_AGE_MAX = 315360000000L;

    public static LinkFileMeta createAndLoad(FileReader fileReader, String metaPath) {
        LinkFileMeta meta = new LinkFileMeta();
        meta.loadAndMergeMeta(fileReader, metaPath);
        return meta;
    }

    public static LinkFileMeta createAndLoad(String metaContent) {
        if (metaContent == null) {
            metaContent = String.format("## VERSION=1%n" +
                    "## ENTRIES_COUNT_MAX=%d%n" +
                    "## ENTRIES_AGE_MAX=%d%n" +
                    "# FILE\tTIMESTAMP\tMD5\tSERIAL%n", DEFAULT_ENTRIES_COUNT_MAX, DEFAULT_ENTRIES_AGE_MAX);
        }

        LinkFileMeta meta = new LinkFileMeta();
        meta.loadAndMergeMeta(metaContent);
        return meta;
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


}
