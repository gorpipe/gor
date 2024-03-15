package org.gorpipe.gor.table.dictionary.gor;

import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.IDictionaryEntryFactory;

public class GorDictionaryEntryFactory implements IDictionaryEntryFactory<GorDictionaryEntry> {

    @Override
    public GorDictionaryEntry parseEntry(String line, String rootPath, boolean b) {
        return GorDictionaryEntry.parseEntry(line, rootPath, b);
    }

    @Override
    public GorDictionaryEntry copy(GorDictionaryEntry template) {
        return (GorDictionaryEntry) DictionaryEntry.copy(template);
    }

    @Override
    public GorDictionaryEntry.Builder getBuilder(String contentLogical, String rootUri) {
        return new GorDictionaryEntry.Builder(contentLogical, rootUri);
    }

}
