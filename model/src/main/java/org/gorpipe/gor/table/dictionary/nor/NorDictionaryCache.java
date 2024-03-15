package org.gorpipe.gor.table.dictionary.nor;

import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.TableCache;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NorDictionaryCache extends TableCache<DictionaryTable> {

    private static final Logger log = LoggerFactory.getLogger(NorDictionaryCache.class);

    public static NorDictionaryCache dictCache = new NorDictionaryCache();

    @Override
    protected DictionaryTable<DictionaryEntry> createTable(String path, FileReader fileReader) {
        return new NorDictionaryTable(path, fileReader);
    }
}
