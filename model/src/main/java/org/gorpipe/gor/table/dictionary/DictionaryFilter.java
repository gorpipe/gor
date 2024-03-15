package org.gorpipe.gor.table.dictionary;

import org.apache.commons.lang3.ArrayUtils;
import org.gorpipe.gor.table.TableInfo;
import org.gorpipe.gor.table.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for passing in row filtering criteria.
 */
public class DictionaryFilter<T extends DictionaryEntry> {

    private static final Logger log = LoggerFactory.getLogger(DictionaryFilter.class);
    private final IDictionaryEntries<T> tableEntries;
    protected final TableInfo table;
    String[] files;
    String[] aliases;
    String[] tags;
    String[] buckets;
    boolean matchAllTags = false;
    boolean includeDeleted = false;

    public DictionaryFilter(TableInfo table, IDictionaryEntries<T> tableEntries) {
        this.table = table;
        this.tableEntries = tableEntries;
    }

    /**
     * Filter for files names (content)
     *
     * @param val file names to filter by, absolute or relative to the table.
     * @return return new filter on files.
     */
    public DictionaryFilter<T> files(String... val) {
        this.files = val != null ? Arrays.stream(val).map(f -> PathUtils.formatUri(PathUtils.resolve(table.getRootPath(), f))).toArray(String[]::new) : null;
        return this;
    }

    public DictionaryFilter<T> aliases(String... val) {
        this.aliases = val;
        return this;
    }

    // Tags matches line tags if line tags, otherwise line alias.
    public DictionaryFilter<T> tags(String... val) {
        this.tags = val;
        return this;
    }

    public DictionaryFilter<T> matchAllTags(String... val) {
        this.tags = val;
        this.matchAllTags = true;
        return this;
    }

    public DictionaryFilter<T> buckets(String... val) {
        this.buckets = val != null ? Arrays.stream(val).map(b -> PathUtils.formatUri(PathUtils.resolve(table.getRootPath(), b))).toArray(String[]::new) : null;
        return this;
    }

    public DictionaryFilter<T> includeDeleted(boolean val) {
        this.includeDeleted = val;
        return this;
    }

    public DictionaryFilter<T> includeDeleted() {
        this.includeDeleted = true;
        return this;
    }

    /**
     * Match the line based on the filter.
     * Notes:
     * 1. An element (i.e. tags, files ...) in the filter to gets a match if any item in it matches.
     * <p>
     * 2. If the filter contains buckets we also include deleted lines.
     *
     * @param l line to match
     * @return {@code true} if the line matches the filter otherwise {@code false}.
     */
    protected boolean match(T l) {
        return matchIncludeLine(l)
                && (matchIsNoFilter()
                || (matchFiles(l) && matchAliases(l) && matchTags(l) && matchBuckets(l)));
    }

    protected boolean matchIncludeLine(T l) {
        return !l.isDeleted() || includeDeleted || buckets != null;
    }

    protected boolean matchIsNoFilter() {
        return files == null && aliases == null && tags == null && buckets == null;
    }

    protected boolean matchFiles(T l) {
        if (files == null) return true;
        String contentReal = l.getContentReal(getTable().getRootPath());
        return Stream.of(files).anyMatch(f -> f.equals(contentReal));
    }

    protected boolean matchBuckets(T l) {
        if (buckets == null || (!l.hasBucket() && buckets.length == 0)) return true;

        var bucketPath = l.getBucketReal(getTable().getRootPath());
        return l.hasBucket() && Stream.of(buckets).anyMatch(b -> b.equals(bucketPath));
    }

    protected boolean matchAliases(T l) {
        if (aliases == null) return true;
        String lineAlias = l.getAlias();
        return Stream.of(aliases).anyMatch(f -> f.equals(lineAlias));
    }

    protected boolean matchTags(T l) {
        String[] filterTags = l.getFilterTags();
        return tags == null || (filterTags.length == 0 && tags.length == 0)
                || (matchAllTags ? Stream.of(tags).allMatch(t -> ArrayUtils.contains(filterTags, t))
                                 : Stream.of(tags).anyMatch(t -> ArrayUtils.contains(filterTags, t)));
    }

    public List<T> get() {
        // Set initial candidates for search (this also forces load if not loaded and populates the tagHashToLines index)
        List<T> lines2Search = (List<T>)tableEntries.getEntries(ArrayUtils.addAll(aliases, tags));
        return lines2Search.stream().filter(this::match).collect(Collectors.toCollection(ArrayList::new));
    }

    public DictionaryTable getTable() {
        // TODO: GM
        return (DictionaryTable) table;
    }

    public DictionaryFilter<T> self() {
        return this;
    }

}
