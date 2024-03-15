package org.gorpipe.gor.table.dictionary.gor;

import org.gorpipe.gor.table.TableInfo;
import org.gorpipe.gor.table.dictionary.DictionaryFilter;
import org.gorpipe.gor.table.dictionary.IDictionaryEntries;
import org.gorpipe.gor.table.util.GenomicRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for passing in row filtering criteria.
 */
public class GorDictionaryFilter<T extends GorDictionaryEntry> extends DictionaryFilter<T> {

    private static final Logger log = LoggerFactory.getLogger(GorDictionaryFilter.class);

    String chrRange;


    public GorDictionaryFilter(TableInfo table, IDictionaryEntries<T> tableEntries) {
        super(table, tableEntries);
    }

    public GorDictionaryFilter<T> chrRange(String val) {
        GenomicRange gr = GenomicRange.parseGenomicRange(val);
        this.chrRange = gr != null ? gr.formatAsTabDelimited() : null;
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

    protected boolean matchIsNoFilter() {
        return super.matchIsNoFilter() && chrRange == null;
    }


    private boolean matchRange(T l) {
        if (chrRange == null) return true;
        String lineRange = l.getRange().formatAsTabDelimited();
        return l.getRange() != null && chrRange.equals(lineRange);
    }


    @Override
    public GorDictionaryTable getTable() {
        // TODO: GM
        return (GorDictionaryTable) table;
    }

    @Override
    public GorDictionaryFilter<T> self() {
        return this;
    }

}
