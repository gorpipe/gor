package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.util.Strings;

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
 */
public record LinkFileEntryV1(String url, long timestamp, String md5, int serial) implements LinkFileEntry {

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
                parts.length > 1 && !Strings.isNullOrEmpty(parts[1]) ? Long.parseLong(parts[1]) : 0,
                parts.length > 2 ? parts[2] : "",
                parts.length > 3 && !Strings.isNullOrEmpty(parts[3]) ? Integer.parseInt(parts[3]) : 0
                );
    }

    public String format() {
        return url + "\t" + timestamp + "\t" + md5 + "\t" + serial;
    }
}
