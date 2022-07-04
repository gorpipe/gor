package org.gorpipe.gor.table;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.TableEntry;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
public interface Table<T> {

    String getName();

    /**
     * Get absolute path to this table.
     *
     * @return absolute path to this table.
     */
    Path getPath();

    /**
     * Get the path to the table folder (used for additional table data)
     * @return  the table folder path.
     */
    Path getFolderPath();

    String[] getColumns();

    /**
     * Get table property {@code key}
     *
     * @param key property to get.
     * @return table properyt {@code key}
     */
    String getProperty(String key);

    /**
     * Set table property {@code key}
     *
     * @param key   property name.
     * @param value property value
     */
    void setProperty(String key, String value);

    /**
     * Check if the table contains property.
     *
     * @param key   property name.
     * @return  true if the table contains the property, otherwise false.
     */
    boolean containsProperty(String key);

    /**
     * Get the lines of the table.
     */
    Iterator<String> getLines();

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

    void reload();

    /**
     * Save the table.
     * Does commitRequest and commit in one go.
     */
    void save();

    /**
     * Commit request.
     * Would usually carry out the update without committing it.
     * Throws GorException if commit request fails.
     */
    void commitRequest() throws GorException;

    /**
     * Commit.
     * Update (save) the table so every one sees the changes.
     */
    void commit();

    // CHECK if we need these

    void initialize();

    Path getRootPath();

    String getSecurityContext();

    void setColumns(String[] columns);
}
