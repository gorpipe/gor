package org.gorpipe.gor.table.dictionary;

import org.apache.commons.lang3.ArrayUtils;
import org.gorpipe.gor.table.util.GenomicRange;
import org.gorpipe.gor.table.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for passing in row filtering criteria.
 */
public class TableFilter<T extends DictionaryEntry> {

    private static final Logger log = LoggerFactory.getLogger(TableFilter.class);
    private final BaseDictionaryTable<T> baseDictionaryTable;
    String[] files;
    String[] aliases;
    String[] tags;
    String[] buckets;
    String chrRange;
    boolean matchAllTags = false;
    boolean includeDeleted = false;

    public TableFilter(BaseDictionaryTable<T> baseDictionaryTable) {
        this.baseDictionaryTable = baseDictionaryTable;
    }

    /**
     * Filter for files names (content)
     *
     * @param val file names to filter by, absolute or relative to the table.
     * @return return new filter on files.
     */
    public TableFilter<T> files(String... val) {
        this.files = val != null ? Arrays.stream(val).map(f -> PathUtils.formatUri(PathUtils.resolve(baseDictionaryTable.getRootUri(), f))).toArray(String[]::new) : null;
        return this;
    }

    /**
     * Filter for files names (content)
     *
     * @param val file names to filter by, absolute or relative to the table.
     * @return return new filter on files.
     */
    public TableFilter<T> files(URI... val) {
        this.files = val != null ? Arrays.stream(val).map(f -> PathUtils.formatUri(PathUtils.resolve(baseDictionaryTable.getRootUri(), f))).toArray(String[]::new) : null;
        return this;
    }

    public TableFilter<T> aliases(String... val) {
        this.aliases = val;
        return this;
    }

    // Tags matches line tags if line tags, otherwise line alias.
    public TableFilter<T> tags(String... val) {
        this.tags = val;
        return this;
    }

    public TableFilter<T> matchAllTags(String... val) {
        this.tags = val;
        this.matchAllTags = true;
        return this;
    }

    public TableFilter<T> buckets(String... val) {
        this.buckets = val != null ? Arrays.stream(val).map(b -> PathUtils.formatUri(PathUtils.resolve(baseDictionaryTable.getRootUri(), b))).toArray(String[]::new) : null;
        return this;
    }

    public TableFilter<T> buckets(Path... val) {
        this.buckets = val != null ? Arrays.stream(val).map(b -> PathUtils.formatUri(PathUtils.resolve(baseDictionaryTable.getRootUri(), b.toString()))).toArray(String[]::new) : null;
        return this;
    }

    public TableFilter<T> chrRange(String val) {
        GenomicRange gr = GenomicRange.parseGenomicRange(val);
        this.chrRange = gr != null ? gr.formatAsTabDelimited() : null;
        return this;
    }

    public TableFilter<T> includeDeleted(boolean val) {
        this.includeDeleted = val;
        return this;
    }

    public TableFilter<T> includeDeleted() {
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
                || (matchFiles(l) && matchAliases(l) && matchTags(l) && matchBuckets(l) && matchRange(l)));
    }

    private boolean matchIncludeLine(T l) {
        return !l.isDeleted() || includeDeleted || buckets != null;
    }

    private boolean matchIsNoFilter() {
        return files == null && aliases == null && tags == null && buckets == null && chrRange == null;
    }

    private boolean matchFiles(T l) {
        return files == null || Stream.of(files).anyMatch(f -> f.equals(l.getContentReal()));
    }

    private boolean matchBuckets(T l) {
        return buckets == null || (!l.hasBucket() && buckets.length == 0) ||
                (l.hasBucket() && Stream.of(buckets).anyMatch(b -> b.equals(l.getBucketReal())));
    }

    private boolean matchAliases(T l) {
        return aliases == null || Stream.of(aliases).anyMatch(f -> f.equals(l.getAlias()));
    }

    private boolean matchTags(T l) {
        String[] filterTags = l.getFilterTags();
        return tags == null || (filterTags.length == 0 && tags.length == 0)
                || (matchAllTags ? Stream.of(tags).allMatch(t -> ArrayUtils.contains(filterTags, t)) : Stream.of(tags).anyMatch(t -> ArrayUtils.contains(filterTags, t)));
    }

    private boolean matchRange(T l) {
        return chrRange == null || (l.getRange() != null && chrRange.equals(l.getRange().formatAsTabDelimited()));
    }

    public List<T> get() {
        // Set initial candidates for search (this also forces load if not loaded and populates the tagHashToLines index)
        List<T> lines2Search = baseDictionaryTable.getEntries(ArrayUtils.addAll(aliases, tags));
        return lines2Search.stream().filter(this::match).collect(Collectors.toCollection(ArrayList::new));
    }

    public BaseDictionaryTable<T> getTable() {
        return baseDictionaryTable;
    }

}
