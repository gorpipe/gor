package org.gorpipe.gor.driver.utils;

interface LinkFileEntry {
    String format();

    String url();

    long timestamp();

    String md5();

    int serial();
}
