package org.gorpipe.gor.driver.linkfile;

public interface LinkFileEntry {
    String format();

    String url();

    long timestamp();

    String md5();

    int serial();

    String info();
}
