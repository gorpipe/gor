package org.gorpipe.gor.driver.utils;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.util.Strings;

/**
 * Link file entry
 * @param url          content of the link file, path, url or a gor/nor query.  Can be have more than one line.
 * @param timestamp     timestamp of the link file entry.  Optional.
 * @param md5           md5 of file or data the link points to.  Optional.
 * @param serial        incrementing serial number for the link file entry.  Optional.
 */
record LinkFileEntry(String url, long timestamp, String md5, int serial) {
    public static LinkFileEntry parse(String line) {
        String[] parts = line.split("\t");
        if (Strings.isNullOrEmpty(parts[0])) {
            throw new GorResourceException("Invalid link file entry: " + line, null);
        }
        return new LinkFileEntry(
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
