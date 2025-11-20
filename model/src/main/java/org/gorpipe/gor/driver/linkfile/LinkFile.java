package org.gorpipe.gor.driver.linkfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.util.Strings;

/**
 * Class to work with link files, read, write and access metadata.
 *
 * Link file format, a valid nor format.  Example:
 *
 * ## VERSION=<file format version>
 * ## SERIAl=<serial number of thislink file>
 * ## ENTRIES_COUNT_MAX=<max entries to store in this file>
 * ## ENTRIES_AGE_MAX=<max age of entries>
 * # FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
 * source/var/var.gorz\t1734304890790\tABCDEAF13422\t1\tSome info
 * source/var/var.gorz\t1734305124533\t334DEAF13422\t2\tSome other info
 *
 * Notes:
 * 1. No timestamp or serial is treated as 0 (older).
 * 2. Entries are added to the bottom.
 * 3. If entries have the same timestamp, the appearing later in the file is picked.
 * 4. Required fields.
 *     - URL
 * 5, Optional fields.
 *     - TIMESTAMP - in ISO data format or milliseconds since epoch, active time.
 *     - MD5 - md5 checksum of the file or data the link points to.
 *     - SERIAL - incrementing serial number for the link file entry.
 *     - INFO - free text info field.
 * 6, Required meta fields.
 *     - VERSION - Link file format version.
 * 7. Optional meta fields.  See:  LinkFileMeta for complete list.
 *     - SERIAL - serial number of this link file.
 *     - ENTRIES_COUNT_MAX - max entries to store in this file.
 *     - ENTRIES_AGE_MAX - max age of entries in milliseconds.
 *
 */
public abstract class LinkFile {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LinkFile.class);

    public static final int LINK_FILE_MAX_SIZE = 10000;

    private static final boolean USE_LINK_CACHE = Boolean.parseBoolean(System.getProperty("gor.driver.cache.link", "true"));
    private static final Cache<StreamSource, String> linkCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(2, TimeUnit.HOURS).build();

    public static LinkFile load(StreamSource source) throws IOException {
        var content = loadContentFromSource(source);
        return create(source, content);
    }

    public static LinkFile create(StreamSource source, String content) {
        var meta = LinkFileMeta.createAndLoad(content);

        if ("0".equals(meta.getVersion())) {
            return new LinkFileV0(source, meta, content);
        } else {
            return new LinkFileV1(source, meta, content);
        }
    }

    public static String validateAndUpdateLinkFileName(String linkFilePath) {
        if (Strings.isNullOrEmpty(linkFilePath) || DataUtil.isLink(linkFilePath)) {
            return linkFilePath;
        } else {
            return DataUtil.toLink(linkFilePath);
        }
    }

    /**
     * Infer the data file name from the link file name.
     *
     * @param linkSource the link file path with the link extension
     * @return the data file path
     */
    public static String inferDataFileNameFromLinkFile(StreamSource linkSource) throws IOException {
        if (linkSource == null || Strings.isNullOrEmpty(linkSource.getFullPath())) {
            throw new IllegalArgumentException("Link file path is null or empty.  Can not infer data file name.");
        }

        var linkPath = linkSource.getSourceReference().getUrl();

        if (PathUtils.isAbsolutePath(linkPath)) {
            throw new IllegalArgumentException("Link file path is absolute.  Can not infer data file name: " + linkSource.getFullPath());
        }

        var dataFileRootPath = "";

        if (linkSource.exists()) {
            var link = load(linkSource);
            var linkDataFileRootPath = link.getMeta().getProperty(LinkFileMeta.HEADER_CONTENT_LOCATION_MANAGED_KEY);
            if (!Strings.isNullOrEmpty(linkDataFileRootPath)) {
                dataFileRootPath = linkDataFileRootPath;
            }
        }

        if (Strings.isNullOrEmpty(dataFileRootPath)) {
            dataFileRootPath = System.getenv(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_FILES_URL);
        }

        if (Strings.isNullOrEmpty(dataFileRootPath)) {
            throw new IllegalArgumentException("Link file data root path is not set.  Can not infer data file name from link file: " + linkSource.getFullPath());
        }

        String randomString = RandomStringUtils.random(8, true, true);
        var linkPathSplit = linkPath.indexOf('.');
        if (linkPathSplit > 0) {
            linkPath = "%s.%s.%s".formatted(
                    linkPath.substring(0, linkPathSplit),
                    randomString,
                    linkPath.substring(linkPathSplit + 1));
        } else {
            linkPath = "%s.%s".formatted(linkPath, randomString);
        }

        linkPath = linkPath.replaceAll("\\.link$", "");

        return PathUtils.resolve(dataFileRootPath, linkPath);
    }

    protected final StreamSource source;
    protected final LinkFileMeta meta;
    protected final List<LinkFileEntry> entries;  // Entries sorted by time (oldest first)

    /**
     * Create a new link file from source and content.
     *
     * @param source the source to create the link file from
     * @param content the content of the link file, can be empty or null to create an empty link file.
     */
    protected LinkFile(StreamSource source, String content) {
        this(source, LinkFileMeta.createAndLoad(content), content);
    }

    protected LinkFile(StreamSource source, LinkFileMeta meta, String content) {
        this.source = source;
        this.meta = meta;
        this.entries = parseEntries(content);
    }

    public LinkFileMeta getMeta() {
        return meta;
    }

    public String getPath() {
        return source.getFullPath();
    }

    public String getEntryUrl(long timestamp) {
        return getUrlFromEntry(getEntry(timestamp));
    }

    public String getLatestEntryUrl() {
        return getUrlFromEntry(getLatestEntry());
    }

    private String getUrlFromEntry(LinkFileEntry entry) {
        var linkUrl = entry != null ? entry.url() : null;
        if (linkUrl != null && !PathUtils.isAbsolutePath(linkUrl) && this.source != null) {
            // Allow relative links:
            linkUrl = PathUtils.resolve(PathUtils.getParent(this.source.getFullPath()), linkUrl);
        }

        // Handle link sub-path if needed.
        SourceReference sourceReference = source.getSourceReference();
        if (sourceReference != null) {
            String linkSubPath = sourceReference.getLinkSubPath();
            linkUrl = linkSubPath != null ? linkUrl + linkSubPath : linkUrl;
        }

        return linkUrl;
    }

    protected String getHeader() {
        return meta.formatHeader();
    }

    List<LinkFileEntry> getEntries() {
        return entries;
    }

    public int getEntriesCount() {
        return entries.size();
    }

    /**
     * Get the entry that matches the timestamp.
     * @param timestamp  timestamp to match
     * @return best match entry or null if no entries.
     */
    public LinkFileEntry getEntry(long timestamp) {
        int index = entries.size() - 1;
        while (index >= 0 && entries.get(index).timestamp() > timestamp) {
            index--;
        }
        if (index < 0) {
            log.warn("No entry found for timestamp: %d in link file: %s".formatted(timestamp, source.getFullPath()));
            for (var entry : entries) {
                log.warn(" Entry: " + entry.url() + " ts: " + entry.timestamp());
            }
        }
        return index >= 0 ? entries.get(index) : null;
    }

    /**
     * Get the latest entry.
     * @return the latest entry
     */
    public LinkFileEntry getLatestEntry() {
        return entries != null && !entries.isEmpty() ? entries.get(entries.size() - 1) : null;
    }

    public void setEntriesCountMax(int entriesCountMax) {
        meta.setEntriesCountMax(entriesCountMax);
    }

    public int getEntriesCountMax() {
        return meta.getEntriesCountMax();
    }

    public void setEntriesAgeMax(int entriesAgeMax) {
        meta.setEntriesAgeMax(entriesAgeMax);
    }

    public long getEntriesAgeMax() {
        return meta.getEntriesAgeMax();
    }

    public LinkFile appendEntry(String link, String md5) {
        return appendEntry(link, md5, null, null);
    }

    public LinkFile appendEntry(String link, String md5, String info) {
        return appendEntry(link, md5, info, null);
    }

    public abstract LinkFile appendEntry(String link, String md5, String info, FileReader reader);

    public LinkFile appendMeta(String meta) {
        if (!Strings.isNullOrEmpty(meta)) {
            this.meta.loadAndMergeMeta(meta);
        }
        return this;
    }

    /**
     * Remove the latest entry, if any.
     *
     * @return true if an entry was removed, otherwise false.
     */
    public boolean rollbackLatestEntry() {
        if (entries.isEmpty()) {
            return false;
        }
        entries.remove(entries.size() - 1);
        return true;
    }

    /**
     * Remove entries that are newer than the provided timestamp.
     *
     * @param timestamp the timestamp to rollback to (inclusive)
     * @return true if one or more entries were removed, otherwise false.
     */
    public boolean rollbackToTimestamp(long timestamp) {
        boolean removed = false;
        while (!entries.isEmpty() && entries.get(entries.size() - 1).timestamp() > timestamp) {
            entries.remove(entries.size() - 1);
            removed = true;
        }
        return removed;
    }

    public void save() {
        save(-1);
    }

    public void save(long timestamp) {
        try (OutputStream os = source.getOutputStream()) {
            save(os, timestamp);
        } catch (IOException e) {
            throw new GorResourceException("Could not save: " + source.getFullPath(), source.getFullPath(), e);
        }
    }

    private void save(OutputStream os, long timestamp) {
        meta.setProperty(meta.HEADER_SERIAL_KEY, Integer.toString(Integer.parseInt(meta.getProperty(meta.HEADER_SERIAL_KEY, "0")) + 1));

        var content = new StringBuilder(getHeader());

        if (!entries.isEmpty()) {
            var currentTimestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
            entries.stream()
                    .skip(Math.max(0, entries.size() - getEntriesCountMax()))
                    .filter(entry -> entry.timestamp() <= 0 || currentTimestamp - entry.timestamp() <= getEntriesAgeMax())
                    .forEach(entry -> content.append(entry.format()).append("\n"));
        }
        try {
            os.write(content.toString().getBytes());
        } catch (IOException e) {
            throw new GorResourceException("Could not save: " + source.getFullPath(), source.getFullPath(), e);
        }
    }

    protected abstract List<LinkFileEntry> parseEntries(String content);


    /**
     * Load content from the source, if it exists.
     *
     * @param source the source to load from
     * @return the content of the link file or null if it does not exist (empty indicates version 0 link file).
     */
    protected static String loadContentFromSource(StreamSource source) throws IOException {
        if (source == null || !source.exists()) {
            return null;
        }

        if (USE_LINK_CACHE) {
            try {
                return linkCache.get(source, (k) -> {
                    try {
                        return readLimitedLinkContent(k);
                    } catch (Exception e) {
                        throw new UncheckedExecutionException(e);
                    }
                });
            } catch (UncheckedExecutionException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw new IOException(e.getCause());
            }
        } else {
            return readLimitedLinkContent(source);
        }
    }

    private static String readLimitedLinkContent(StreamSource source) {
        try (InputStream is = source.open()) {
            var content =  StreamUtils.readString(is, LINK_FILE_MAX_SIZE);
            if (content.length() == LINK_FILE_MAX_SIZE) {
                throw new GorResourceException(String.format("Link file '%s' too large (> %d bytes).",
                        source.getFullPath(), LINK_FILE_MAX_SIZE), source.getFullPath());
            }
            return content;
        } catch (IOException e) {
            throw new GorResourceException("Failed to read link file: " + source.getFullPath(), source.getFullPath(), e);
        }
    }


}
