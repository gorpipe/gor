package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.util.DateUtils;
import org.gorpipe.util.Strings;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Link file entry, with url, timestamp, md5 and serial.
 *
 *
 * @param url          content of the link file, path, url or a gor/nor query.  Can be have more than one line.
 * @param timestamp     timestamp of the link file entry.  Optional.
 * @param md5           md5 of file or data the link points to.  Optional.
 * @param serial        incrementing serial number for the link file entry.  Optional.
 * @param info          extra info about the this version
 */
public record LinkFileEntryV1(String url, long timestamp, String md5, int serial, String info) implements LinkFileEntry {

    // Special key to store entry info in link file metadata.
    public static final String ENTRY_INFO_KEY = "ENTRY_INFO";

    public LinkFileEntryV1(String url, long timestamp, String md5, int serial, String info) {
        if (Strings.isNullOrBlank(url)) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("Timestamp cannot be negative");
        }
        if (serial < 0) {
            throw new IllegalArgumentException("Serial cannot be negative");
        }

        this.url = url.trim();
        this.timestamp = timestamp;
        this.md5 = md5;
        this.serial = serial;
        this.info = info;
    }

    public static List<LinkFileEntry> parse(String content) {
        if (Strings.isNullOrEmpty(content)) {
            return new ArrayList<>();
        }
        return Arrays.stream(content.split("\n"))
                .filter(l -> !l.startsWith("#"))
                .map(LinkFileEntryV1::parseLine)
                .sorted(Comparator.comparingLong(LinkFileEntryV1::timestamp))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static LinkFileEntryV1 parseLine(String line) {
        String[] parts = line.split("\t");
        if (Strings.isNullOrEmpty(parts[0])) {
            throw new GorResourceException("Invalid link file entry: " + line, null);
        }
        return new LinkFileEntryV1(
                parts[0].trim(),
                parts.length > 1 && !Strings.isNullOrEmpty(parts[1]) ? DateUtils.parseDateISOEpoch(parts[1], true).toEpochMilli() : 0,
                parts.length > 2 ? parts[2] : "",
                parts.length > 3 && !Strings.isNullOrEmpty(parts[3]) ? Integer.parseInt(parts[3]) : 0,
                parts.length > 4 ? parts[4] : ""
                );
    }

    public String format() {
        return url + "\t" + Instant.ofEpochMilli(timestamp) + "\t" + md5 + "\t" + serial+ "\t" + info;
    }
}
