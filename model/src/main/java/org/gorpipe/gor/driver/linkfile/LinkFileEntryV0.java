package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Link file entry, old style only containing the URL.
 *
 * @param url          content of the link file, path, url or a gor/nor query.  Can be have more than one line.
 */
public record LinkFileEntryV0(String url) implements LinkFileEntry {
    public static List<LinkFileEntry> parse(String content) {
        if (Strings.isNullOrEmpty(content)) {
            return new ArrayList<>();
        }
        List<LinkFileEntry> list = new ArrayList<>();
        list.add(new LinkFileEntryV0(content));
        return list;
    }

    public LinkFileEntryV0(String url) {
        if (Strings.isNullOrBlank(url)) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        this.url = url.trim();
    }

    public String format() {
        return url;
    }

    public long timestamp() {
        return 0; // No timestamp in V0
    }

    public String md5() {
        return ""; // No md5 in V0
    }

    public int serial() {
        return 0; // No serial in V0
    }
}
