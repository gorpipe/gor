package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.FileReader;
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
 * Notes:
 * 1. No timestamp or serial is treated as 0 (older).
 * 2. Entries are added to the bottom.
 * 3. If entries have the same timestamp, the appearing later in the file is picked.
 *
 */
public abstract class LinkFile {

    public static final int LINK_FILE_MAX_SIZE = 10000;

    public static LinkFile load(StreamSource source) {
        var content = loadContentFromSource(source);
        return load(source, content);
    }

    public static LinkFile load(StreamSource source, String content) {
        var meta = LinkFileMeta.createAndLoad(content);

        if ("0".equals(meta.getVersion())) {
            return new LinkFileV0(source, meta, content);
        } else {
            return new LinkFileV1(source, meta, content);
        }
    }

    public static LinkFile load(StreamSource source, int linkVersion) {
        switch (linkVersion) {
            case 0:
                return new LinkFileV0(source);
            case 1:
            default:
                return new LinkFileV1(source);
        }
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

    public LinkFile appendEntry(String link, String md5) {
        return appendEntry(link, md5, null);
    }

    public abstract LinkFile appendEntry(String link, String md5, FileReader reader);

    public void save() {
        try (OutputStream os = source.getOutputStream()) {
            save(os);
        } catch (IOException e) {
            throw new GorResourceException("Could not save: " + source.getFullPath(), source.getFullPath(), e);
        }
    }

    private void save(OutputStream os) {
        var content = new StringBuilder(getHeader());

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

    protected abstract List<LinkFileEntry> parseEntries(String content);


    /**
     * Load content from the source, if it exists.
     *
     * @param source the source to load from
     * @return the content of the link file or null if it does not exist (empty indicates version 0 link file).
     */
    protected static String loadContentFromSource(StreamSource source) {
        if (!source.exists()) {
            return null;
        }

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
