package org.gorpipe.gor.table.dictionary;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TableAccessOptimizer {

    // TODO:  Should not return dict entries, but rather SourceRef.
    List<DictionaryEntry> getOptimizedEntries(Set<String> tags, boolean allowBucketAccess, boolean isSilentTagFilter);

    // TODO:  Can do this better. Include this data in the entry returned from getOptimized?
    Collection<String> getBucketDeletedFiles(String bucket);
}
