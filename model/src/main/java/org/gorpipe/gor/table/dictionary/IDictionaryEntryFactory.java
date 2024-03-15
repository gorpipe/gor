package org.gorpipe.gor.table.dictionary;

public interface IDictionaryEntryFactory<T extends DictionaryEntry> {

    /**
     * Parse entry from dict file.
     * Assumes the entry has been created by us, i.e. paths normalized etc.
     * NOTE:  invoked through reflection.
     *
     * @param line          the line to parse.
     * @param rootUri       root URI to resolve relative paths.
     * @param needsRelativize should we relatives the content (only needed if reading from a outside source)
     * @return new entry from the entryString
     */
    T parseEntry(String line, String rootUri, boolean needsRelativize);

    T copy(T template);

    T.Builder getBuilder(String contentLogical, String rootUri);
}
