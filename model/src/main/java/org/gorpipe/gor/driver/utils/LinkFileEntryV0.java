package org.gorpipe.gor.driver.utils;

/**
 * Link file entry
 * @param url          content of the link file, path, url or a gor/nor query.  Can be have more than one line.
 */
record LinkFileEntryV0(String url) implements LinkFileEntry {
    public static LinkFileEntryV0 parse(String content) {
        return new LinkFileEntryV0(content.trim());
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
