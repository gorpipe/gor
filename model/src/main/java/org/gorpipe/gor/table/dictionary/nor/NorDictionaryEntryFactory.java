package org.gorpipe.gor.table.dictionary.nor;

import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.IDictionaryEntryFactory;

public class NorDictionaryEntryFactory implements IDictionaryEntryFactory<DictionaryEntry> {

    @Override
    public DictionaryEntry parseEntry(String line, String rootPath, boolean b) {
        return DictionaryEntry.parseEntry(line, rootPath, b);
    }

    @Override
    public DictionaryEntry copy(DictionaryEntry template) {
        return DictionaryEntry.copy(template);
    }

    @Override
    public DictionaryEntry.Builder getBuilder(String contentLogical, String rootUri) {
        return new DictionaryEntry.Builder(contentLogical, rootUri);
    }

}
