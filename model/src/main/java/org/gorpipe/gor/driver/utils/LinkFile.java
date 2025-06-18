package org.gorpipe.gor.driver.utils;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.table.util.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Class to work with link files, read, write and access metadata.
 *
 * Link file format, a valid nor format.   Note, the required fields form the current link file format.
 *
 * ## VERSION=<file format version>
 * ## ENTRIES_COUNT_MAX=<max entries to store in this file>
 * ## ENTRIES_AGE_MAX=<max age of entries>
 * # FILE\tTIMESTAMP\tMD5\tSERIAL
 * source/var/var.gorz\t1734304890790\tABCDEAF13422\t1
 * source/var/var.gorz\t1734305124533\t334DEAF13422\t2
 *
 * Empty timestamp or serial are always considered as 0 (older).
 */
public class LinkFile {

    public static final int LINK_FILE_MAX_SIZE = 10000;

    // Defaults to creating versioned link files.
    public static LinkFile create(StreamSource source, String content) {
        return new LinkFile(source, content);
    }

    public static LinkFile load(StreamSource source) {
        return new LinkFile(source);
    }

    private final StreamSource source;
    private final LinkFileMeta meta;
    private final List<LinkFileEntry> entries;  // Entries sorted by time (oldest first)

    // Create new link file from content.
    public LinkFile(StreamSource source, String content) {
        this.source = source;
        this.meta = LinkFileMeta.createAndLoad(content);
        this.entries = parseEntries(content);
    }

    // Load from source
    public LinkFile(StreamSource source) {
        this(source, loadContentFromSource(source));
    }

    public LinkFileMeta getMeta() {
        return meta;
    }

    public String getPath() {
        return source.getFullPath();
    }

    public void appendEntry(String link, String md5) {
        entries.add(new LinkFileEntryV1(link, System.currentTimeMillis(), md5, getLatestEntry().serial() + 1));
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
        return linkUrl;
    }

    List<LinkFileEntry> getEntries() {
        return entries;
    }

    /**
     * Get the entry that matches the timestamp.
     * @param timestamp  timestamp to match
     * @return best match entry or null if no entries.
     */
    LinkFileEntry getEntry(long timestamp) {
        int index = entries.size() - 1;
        while (index >= 0 && entries.get(index).timestamp() > timestamp) {
            index--;
        }
        return index >= 0 ? entries.get(index) : null;
    }

    /**
     * Get the latest entry.
     * @return the latest entry
     */
    LinkFileEntry getLatestEntry() {
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

    public void save(OutputStream os) {
        var content = switch (getMeta().getVersion()) {
            case "1" -> new StringBuilder(meta.formatHeader());
            default -> new StringBuilder();
        };

        if (!entries.isEmpty()) {
            var currentTimestamp = System.currentTimeMillis();
            entries.stream()
                    .skip(Math.max(0, entries.size() - getEntriesCountMax()))
                    .filter(entry -> entry.timestamp() <= 0 || entry.timestamp() + getEntriesAgeMax() >= currentTimestamp)
                    .forEach(entry -> content.append(entry.format()).append("\n"));
        }
        try {
            os.write(content.toString().getBytes());
        } catch (IOException e) {
            throw new GorResourceException("Could not save: " + source.getFullPath(), source.getFullPath(), e);
        }
    }

    private List<LinkFileEntry> parseEntries(String content) {
        return switch (getMeta().getVersion()) {
            case "1" -> LinkFileEntryV1.parse(content);
            default -> List.of(LinkFileEntryV0.parse(content));
        };
    }


    private static String loadContentFromSource(StreamSource source) {
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
