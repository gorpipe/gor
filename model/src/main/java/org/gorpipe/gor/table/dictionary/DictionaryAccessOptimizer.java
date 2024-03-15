package org.gorpipe.gor.table.dictionary;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface DictionaryAccessOptimizer<T extends DictionaryEntry> {

    // TODO:  Should not return dict entries, but rather SourceRef.
    List<T> getOptimizedEntries(Set<String> tags, boolean allowBucketAccess, boolean isSilentTagFilter);

    // TODO:  Can do this better. Include this data in the entry returned from getOptimized?
    Collection<String> getBucketDeletedFiles(String bucket);
}
