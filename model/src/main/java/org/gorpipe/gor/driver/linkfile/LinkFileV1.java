package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.BaseMeta;
import org.gorpipe.gor.model.FileReader;

import java.io.IOException;
import java.util.List;

/**
 * Link file format, version 1.
 */
public class LinkFileV1 extends LinkFile {

    private static boolean allowOverwriteOfTargets
            = Boolean.parseBoolean(System.getProperty("gor.link.versioned.allow.overwrite", "false"));

    /**
     * Load from a source, if it exists, otherwise create an empty link file.
     *
     * @param source the source to load from
     */
    public LinkFileV1(StreamSource source) throws IOException {
        super(source, loadContentFromSource(source));
        checkDefaultMeta();
    }

    protected LinkFileV1(StreamSource source, LinkFileMeta meta, String content) {
        super(source, meta, content);
        checkDefaultMeta();
    }

    @Override
    protected List<LinkFileEntry> parseEntries(String content) {
        return LinkFileEntryV1.parse(content);
    }

    @Override
    public LinkFile appendEntry(String link, String md5, String info, FileReader reader) {
        var latestEntry = getLatestEntry();
        var entry = new LinkFileEntryV1(link, System.currentTimeMillis(), md5, latestEntry != null ? latestEntry.serial() + 1 : 1, info);
        validateEntry(entry, reader);
        entries.add(entry);
        return this;
    }

    /**
     * Validate the entry to ensure it is of the correct type, format and does not violate integrity of the link file.
     * @param entry the link file entry to validate
     */
    private void validateEntry(LinkFileEntry entry, FileReader reader) {
        if (!(entry instanceof LinkFileEntryV1)) {
            throw new IllegalArgumentException("Invalid entry type: " + entry.getClass().getName());
        }
        if (entry.url() == null || entry.url().isEmpty()) {
            throw new IllegalArgumentException("Entry URL cannot be null or empty");
        }
        if (!allowOverwriteOfTargets) {
            for (LinkFileEntry existingEntry : entries) {
                if (existingEntry.url().equals(entry.url()) && !canReuseEntryWithSameUrl(existingEntry, entry, reader)) {
                    throw new IllegalArgumentException("Duplicate entry URL: " + entry.url());
                }
            }
        }
    }

    private boolean canReuseEntryWithSameUrl(LinkFileEntry oldEntry, LinkFileEntry newEntry, FileReader reader) {
        // We can reuse an entry if it is they has the same underlying file, as if not the integrity of the
        // versioned link file is violated.

        if ((oldEntry.md5() != null && newEntry.md5() != null)) {
            // Use md5 if available.
            return oldEntry.md5().equals(newEntry.md5());
        } else {
            // The old entry timestamp should be newer than the old file (as the file existed before the entry was added).
            // If the new file as timestamp larger the old entry timestamp, we assume it is a different underlying file.
            return reader == null || oldEntry.timestamp() >= reader.resolveUrl(newEntry.url()).getSourceMetadata().getLastModified();
        }
    }

    private void checkDefaultMeta() {
        if (!meta.containsProperty(BaseMeta.HEADER_VERSION_KEY)) {
            getMeta().loadAndMergeMeta(LinkFileMeta.getDefaultMetaContent());
            meta.setProperty(BaseMeta.HEADER_VERSION_KEY, "1");
        }
    }
}
