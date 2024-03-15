package org.gorpipe.gor.table.livecycle;

import org.gorpipe.gor.table.dictionary.DictionaryEntry;

import java.util.Arrays;
import java.util.Collection;

/**
 * Interface to work with tables.
 *
 * @param <T>  the line type
 *
 * Notes:
 * 1. On the difference between lines and rows:
 *    Line:  What we work with when inserting and deleting from the table.  The can either be rows (data rows,
 *           for example in the case of GOR or NOR tables) or files (that contain rows, for in example in the case
 *           of dictionaries).
 *    Row:   The data rows, i.e. what the user gets when goring the table.
 */
public interface TableInsertDelete<T> {

    /**
     * Insert/update lines.
     *
     * @param lines the line(s) to insert/update.
     */
    void insert(Collection<T> lines);

    /**
     * Insert/update table lines.
     *
     * @param lines the line(s) to insert/update.
     */
    void insert(String... lines);

    /**
     * Insert/update lines.
     *
     * @param lines the line(s) to insert/update.
     */
    @SuppressWarnings("unchecked")
    default void insert(T... lines) {
        insert(Arrays.asList(lines));
    }

    /**
     * Insert dictionary entries/files into this table.
     * @param entries  the entries to insert.
     */
    void insertEntries(Collection<DictionaryEntry> entries);

    /**
     * Insert/update lines.
     *
     * @param table the table(s) containing the lines to insert/update.
     */
    //void insert(Table table);

    /**
     * Delete the given lines from the table.
     *
     * @param lines lines to remove.
     */
    void delete(Collection<T> lines);

    /**
     * Delete the given lines from the table.
     *
     * @param lines lines to remove.
     */
    void delete(String... lines);

    /**
     * Delete the given lines from the table.
     *
     * @param lines lines to remove.
     */
    @SuppressWarnings("unchecked")
    default void delete(T... lines) {
        delete(Arrays.asList(lines));
    }

    /**
     * Delete the given entries/files.
     * @param entries   the entries/files.
     */
    void deleteEntries(Collection<DictionaryEntry> entries);

}
