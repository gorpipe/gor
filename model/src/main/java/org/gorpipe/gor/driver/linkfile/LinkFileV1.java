package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.FileReader;

import java.util.List;

/**
 * Link file format, version 1.
 */
public class LinkFileV1 extends LinkFile {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LinkFileV1.class);

    public static final String VERSION = "1";
    public static final String DEFAULT_TABLE_HEADER = "#File\tTimestamp\tMD5\tSerial\tInfo";

    enum LinkReuseStrategy {
        REUSE,            // Reuse previous entru.
        REUSE_DATA,       // Reuse the data, create new entry.
        NO_REUSE          // No reuse, create new entry and data.
    }

    public static LinkReuseStrategy defaultReuseStrategy
            = LinkReuseStrategy.valueOf(System.getProperty("gor.link.versioned.reuse.strategy.default", "NO_REUSE"));

    public static boolean allowOverwriteOfTargets
            = Boolean.parseBoolean(System.getProperty("gor.link.versioned.allow.overwrite", "true"));

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
        entry = handleRepeatedEntries(entry, reader);
        if (entry != null) {
            entries.add(entry);
        }
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
            // Only applies to non managed data.
            for (LinkFileEntry existingEntry : entries) {
                if (existingEntry.url().equals(entry.url()) && !canReuseEntryWithSameUrl(existingEntry, entry, reader)) {
                    throw new IllegalArgumentException("Duplicate entry URL: " + entry.url());
                }
            }
        }
    }

    private boolean canReuseEntryWithSameUrl(LinkFileEntry oldEntry, LinkFileEntry newEntry, FileReader reader) {
        // We can reuse an entry (same url) if the entries have the  same underlying file, as if not the integrity of the
        // versioned link file is violated (as the new entry file overwrites the old entry file, but the old entry
        // is still in the link file history).
        // BUT haven't we already ruined the integrity when we enter here!?

        if ((oldEntry.md5() != null && newEntry.md5() != null)) {
            // Use md5 if available.
            return oldEntry.md5().equals(newEntry.md5());
        } else {
            // The old entry timestamp should be newer than the old file (as the file existed before the entry was added).
            // If the new file as timestamp larger the old entry timestamp, we assume it is a different underlying file.
            return reader == null || oldEntry.timestamp() >= reader.resolveUrl(newEntry.url()).getSourceMetadata().getLastModified();
        }
    }

    private LinkFileEntryV1 handleRepeatedEntries(LinkFileEntryV1 newEntry, FileReader reader) {
        var reuseStrategy = LinkReuseStrategy.valueOf(getMeta().getProperty(LinkFileMeta.HEADER_REUSE_STRATEGY_KEY, defaultReuseStrategy.name()));

        if (reuseStrategy == LinkFileV1.LinkReuseStrategy.NO_REUSE) {
            return newEntry; // No reuse, always create new entry and data.
        }

        var existingEntry = findExistingEntryByMD5(newEntry);
        if (existingEntry == null) {
            return newEntry; // No existing entry with same MD5, create new entry and data.
        }

        // Can clean now we are not going to use the file, and it is managed so we should be able to delete it.
        cleanEntryDataIfManaged(newEntry, reader);

        var isExistingEntryLatestEntry = existingEntry.equals(getLatestEntry());
        if (reuseStrategy == LinkReuseStrategy.REUSE && isExistingEntryLatestEntry) {
            // Existing matching entry is the latest entry, do nothing.
            return null;
        } else if (reuseStrategy == LinkReuseStrategy.REUSE_DATA || reuseStrategy == LinkReuseStrategy.REUSE) {
            // Reuse the data, create new entry with same URL but new timestamp, serial and info.
            return new LinkFileEntryV1(existingEntry.url(), newEntry.timestamp(), existingEntry.md5(), newEntry.serial(), newEntry.info());
        } else {
            throw new IllegalArgumentException("Unsupported reuse strategy: " + getMeta().getProperty(LinkFileMeta.HEADER_REUSE_STRATEGY_KEY));
        }
    }

    private LinkFileEntry findExistingEntryByMD5(LinkFileEntry entry) {
        for (LinkFileEntry existingEntry : entries) {
            if (existingEntry.md5().equals(entry.md5())) {
                return existingEntry;
            }
        }
        return null;
    }

    private LinkFileEntry findExistingEntryByUrl(LinkFileEntry entry) {
        for (LinkFileEntry existingEntry : entries) {
            if (existingEntry.url().equals(entry.url())) {
                return existingEntry;
            }
        }
        return null;
    }

    /**
     *
     * @param candiateEntry     entry with the same MD5 as an existing entry, and thus candidate for reuse of the underlying data.
     * @param reader
     */
    private void cleanEntryDataIfManaged(LinkFileEntry candiateEntry, FileReader reader) {
        if (getMeta().getPropertyBool(LinkFileMeta.HEADER_DATA_LIFECYCLE_MANAGED_KEY, false)) {
            if (findExistingEntryByUrl(candiateEntry) == null) {
                // The file is not used by any existing entry, and the data is managed, we can safely delete it.
                try {
                    reader.delete(candiateEntry.url());
                } catch (Exception e) {
                    log.warn("Failed to delete data for existing entry with same MD5: " + candiateEntry.url(), e);
                }
            }
        }
    }

    private void checkDefaultMeta() {
        if (!meta.getVersion().equals(VERSION)) {
            meta.loadAndMergeMeta(getDefaultMetaContent());
        }
    }

    public static String getDefaultMetaContent() {
        return String.format("""
                ## SERIAL = 0
                ## VERSION = %s
                %s
                """, VERSION, DEFAULT_TABLE_HEADER);
    }
}
