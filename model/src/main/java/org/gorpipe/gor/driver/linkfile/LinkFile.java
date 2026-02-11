package org.gorpipe.gor.driver.linkfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.util.Strings;

import static org.gorpipe.gor.driver.linkfile.LinkFileMeta.HEADER_DATA_LIFECYCLE_MANAGED_KEY;

/**
 * Class to work with link files, read, write and access metadata.
 *
 * Link file format, a valid nor format.  Example:
 *
 * ## VERSION=<file format version>
 * ## SERIAl=<serial number of this link file>
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

    // Approx max size of link file content to read or write. Stopp adding lines if exceeded. Dont load if twice this size.
    public static final int LINK_FILE_MAX_SIZE = Integer.parseInt(System.getProperty("gor.driver.link.maxfilesize", "10000"));
    private static final boolean USE_LINK_CACHE = Boolean.parseBoolean(System.getProperty("gor.driver.link.cache", "true"));

    private static final Cache<StreamSource, String> linkCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(15, TimeUnit.MINUTES).build();

    public static LinkFile load(StreamSource source) throws IOException {
        var content = loadContentFromSource(source);
        var meta = LinkFileMeta.createOrLoad(content, null, false);
        return create(source, meta, content);
    }

    public static LinkFile createOrLoad(StreamSource source, String version) throws IOException {
        var content = loadContentFromSource(source);
        var meta = LinkFileMeta.createOrLoad(content, version, false);
        return create(source, meta, content);
    }

    public static LinkFile create(StreamSource source, String content) {
        var meta = LinkFileMeta.createOrLoad(content, null, true);
        return create(source, meta, content);
    }

    public static LinkFile create(StreamSource source, LinkFileMeta meta, String content) {
        return switch (meta.getVersion()) {
            case "0" -> new LinkFileV0(source, meta, content);
            case "1" -> new LinkFileV1(source, meta, content);
            default -> throw new GorResourceException("Unsupported link file version: " + meta.getVersion(), source.getFullPath());
        };
    }

    public static LinkFile loadV0(StreamSource source) throws IOException {
        var content = loadContentFromSource(source);
        return new LinkFileV0(source, LinkFileMeta.createOrLoad(content, LinkFileV0.VERSION, true), content);
    }

    public static LinkFile loadV1(StreamSource source) throws IOException {
        var content = loadContentFromSource(source);
        return new LinkFileV1(source, LinkFileMeta.createOrLoad(content, LinkFileV1.VERSION, true), content);
    }

    public static LinkFile createV0(StreamSource source, String content) {
        return new LinkFileV0(source, LinkFileMeta.createOrLoad(content, LinkFileV0.VERSION, true), content);
    }

    public static LinkFile createV1(StreamSource source, String content) {
        return new LinkFileV1(source, LinkFileMeta.createOrLoad(content, LinkFileV1.VERSION, true), content);
    }

    public static String validateAndUpdateLinkFileName(String linkFilePath) {
        if (Strings.isNullOrEmpty(linkFilePath) || DataUtil.isLink(linkFilePath)) {
            return linkFilePath;
        } else {
            return DataUtil.toLink(linkFilePath);
        }
    }

    protected final StreamSource source;
    protected final LinkFileMeta meta;
    protected final List<LinkFileEntry> entries;  // Entries sorted by time (oldest first)


    protected LinkFile(StreamSource source, LinkFileMeta meta, String content) {
        this.source = source;
        this.meta = meta;
        this.entries = parseEntries(content);
    }

    public LinkFileMeta getMeta() {
        return meta;
    }

    public int getSerial() {
        return meta.getPropertyInt(LinkFileMeta.HEADER_SERIAL_KEY, 0);
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

        // Handle the link sub-path if needed.
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

    public List<LinkFileEntry> getEntries() {
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
        return appendEntry(link, md5, info, new DriverBackedFileReader(null, "."));
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
     * @param timestamp the timestamp to roll back to (inclusive)
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

    public void save(FileReader reader) {
        save(-1, reader);
    }

    public void save(long timestamp, FileReader reader) {
        try (OutputStream os = source.getOutputStream()) {
            save(os, timestamp, reader);
        } catch (IOException e) {
            throw new GorResourceException("Could not save: " + source.getFullPath(), source.getFullPath(), e);
        }
    }


    private void save(OutputStream os, long timestamp, FileReader reader) {
        meta.setProperty(LinkFileMeta.HEADER_SERIAL_KEY, Integer.toString(Integer.parseInt(meta.getProperty(LinkFileMeta.HEADER_SERIAL_KEY, "0")) + 1));

        var currentTimestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        var header = getHeader();
        var content = new StringBuilder(header);

        for (var i = 0; i < entries.size(); i++) {
            var entry = entries.get(entries.size() - 1 - i);

            if ((i >= getEntriesCountMax())
                    || (entry.timestamp() > 0 && currentTimestamp - entry.timestamp() > getEntriesAgeMax())
                    || (content.length() > LINK_FILE_MAX_SIZE)) {
                checkAndGCEntries(0, entries.size() - 1 - i, reader);
                break;
            }

            content.insert(header.length(), entry.format() + "\n");
        }

        try {
            os.write(content.toString().getBytes());
        } catch (IOException e) {
            throw new GorResourceException("Could not save: " + source.getFullPath(), source.getFullPath(), e);
        }
    }

    protected abstract List<LinkFileEntry> parseEntries(String content);

    // Check if we can garbage collect entries between fromIndex and toIndex (inclusive).

    /**
     * Check if we can garbage collect entries between fromIndex and toIndex (inclusive), if so do it.
     * @param fromIndex     fromIndex (inclusive)
     * @param toIndex       toIndex (inclusive)
     */
    protected void checkAndGCEntries(int fromIndex, int toIndex, FileReader reader) {
        if (meta.getPropertyBool(HEADER_DATA_LIFECYCLE_MANAGED_KEY, false)) {
            List<String> dataUrlsToDelete = new ArrayList<>();
            // Have managed link file.
            for (int i = fromIndex; i <= toIndex; i++) {
                var entry = entries.get(i);
                if (!matchEntryUrls(entry, toIndex + 1, entries.size() - 1)) {
                     // This entry url is not used by newer entries, can be deleted.
                     dataUrlsToDelete.add(getUrlFromEntry(entry));
                }
            }

            new Thread(() -> gcEntries(dataUrlsToDelete, reader)).start();

        }
    }

    private boolean matchEntryUrls(LinkFileEntry entry, int fromIndex, int toIndex) {
        for (int i = fromIndex; i <= toIndex; i++) {
            if (entries.get(i).url().equals(entry.url())) {
                return true;
            }
        }
        return false;
    }

    private void gcEntries(List<String> dataUrlsToDelete, FileReader reader) {
        var sourceLinkFielUrl = source.getFullPath();
        for (String linkUrl : dataUrlsToDelete) {
            var linkSource = reader.resolveUrl(linkUrl);
            if (linkSource != null && linkSource.exists()) {
                log.info("Garbage collecting link file {}, entry data for {}", sourceLinkFielUrl, linkUrl);
                try {
                    linkSource.delete();
                } catch (Exception e) {
                    log.warn("Failed to garbage collect link file {} entry data for {}", sourceLinkFielUrl, linkUrl, e);
                }
            }
        }
    }

    /**
     * Load content from the source if it exists.
     *
     * @param source the source to load from
     * @return the content of the link file or null if it does not exist (empty indicates version 0 link file).
     */
    public static String loadContentFromSource(StreamSource source) throws IOException {
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
            var content =  StreamUtils.readString(is, 2 * LINK_FILE_MAX_SIZE);
            if (content.length() > 2 * LINK_FILE_MAX_SIZE) {
                throw new GorResourceException(String.format("Link file '%s' too large (> %d bytes).",
                        source.getFullPath(), LINK_FILE_MAX_SIZE), source.getFullPath());
            }
            return content;
        } catch (IOException e) {
            throw new GorResourceException("Failed to read link file: " + source.getFullPath(), source.getFullPath(), e);
        }
    }


}
