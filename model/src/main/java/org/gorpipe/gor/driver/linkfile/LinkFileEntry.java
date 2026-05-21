package org.gorpipe.gor.driver.linkfile;

public interface LinkFileEntry {
    String format();

    String url();

    // Timestamp of the link file entry, in milliseconds since epoch.  Optional, can be 0 if not set.
    long timestamp();

    String md5();

    int serial();

    String info();
}
