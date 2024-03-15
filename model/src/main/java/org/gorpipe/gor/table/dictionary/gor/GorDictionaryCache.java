package org.gorpipe.gor.table.dictionary.gor;

import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.TableCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GorDictionaryCache extends TableCache<GorDictionaryTable> {

    private static final Logger log = LoggerFactory.getLogger(GorDictionaryCache.class);

    public static GorDictionaryCache dictCache = new GorDictionaryCache();

    @Override
    protected GorDictionaryTable createTable(String path, FileReader fileReader) {
        return new GorDictionaryTable(path, fileReader);
    }
}
