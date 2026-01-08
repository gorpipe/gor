package org.gorpipe.gor.driver.linkfile;

import org.apache.commons.lang3.StringUtils;
import org.gorpipe.gor.model.BaseMeta;
import org.gorpipe.util.Strings;

import java.util.stream.Collectors;

public class LinkFileMeta extends BaseMeta {

    // Max number of entries to keep track of in the link file.
    public static final String HEADER_ENTRIES_COUNT_MAX_KEY = "ENTRIES_COUNT_MAX";
    // Max age of entries to keep track of in the link file.
    public static final String HEADER_ENTRIES_AGE_MAX_KEY = "ENTRIES_AGE_MAX";
    // Path if the managed data location.
    public static final String HEADER_DATA_LOCATION_KEY = "DATA_LOCATION";
    // Should the content lifecycle be managed (data deleted if the link is removed from the link file) (true or false).
    public static final String HEADER_DATA_LIFECYCLE_MANAGED_KEY = "DATA_LIFECYCLE_MANAGED";

    private static final String DEFAULT_VERSION = System.getProperty("gor.driver.link.default.version", LinkFileV0.VERSION);

    public static final int DEFAULT_ENTRIES_COUNT_MAX = 100;
    public static final long DEFAULT_ENTRIES_AGE_MAX = Long.MAX_VALUE;

    /**
     * Create or load link file meta from content.
     * @param content
     * @param version   version if known, otherwise null.  Only used if content is null or empty.
     * @param isNew     true if creating new link file meta, false if loading existing.
     * @return
     */
    public static LinkFileMeta createOrLoad(String content, String version, boolean isNew) {
        var metaContent = !Strings.isNullOrEmpty(content)  ? content.lines().filter(line -> line.startsWith("#")).collect(Collectors.joining("\n")) : "";
        LinkFileMeta meta = new LinkFileMeta();
        if (Strings.isNullOrEmpty(metaContent) ) {
            // No meta, determine version to use
            if (Strings.isNullOrEmpty(version)) {
                version = Strings.isNullOrEmpty(content) || isNew ? DEFAULT_VERSION : LinkFileV0.VERSION;
            }

            metaContent = switch(version) {
                case "0" -> LinkFileV0.getDefaultMetaContent();
                case "1" -> LinkFileV1.getDefaultMetaContent();
                default -> throw new IllegalArgumentException("Unsupported link file version: " + meta.getVersion());
           };
        }
        meta.loadAndMergeMeta(metaContent);
        return meta;
    }

    public LinkFileMeta() {
        super();
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
        return getProperty(HEADER_VERSION_KEY, DEFAULT_VERSION);
    }


}
