package org.gorpipe.gor.table;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface Table<T> {

    String getName();

    /**
     * Get real path of this table.
     *
     * @return real path of this table.
     */
    Path getPath();

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
     * Insert/update dictionary lines.
     *
     * @param lines the line(s) to insert/update.
     */
    void insert(Collection<T> lines);

    /**
     * Insert/update dictionary lines.
     *
     * @param lines the line(s) to insert/update.
     */
    default void insert(T... lines) {
        insert(Arrays.asList(lines));
    }

    /**
     * Insert/update dictionary lines.
     *
     * @param table the table(s) containing the lines to insert/update.
     */
    //void insert(Table table);

    /**
     * Delete the given lines from the dictionary.
     *
     * @param lines lines to remove.
     */
    void delete(Collection<T> lines);

    /**
     * Delete the given lines from the dictionary.
     *
     * @param lines lines to remove.
     */
    default void delete(T... lines) {
        delete(Arrays.asList(lines));
    }

    void save();

    //GenomicIterator getIterator();


    // CHECK

    void initialize();

    Path getRootPath();

    String getSecurityContext();

    void setColumns(String[] columns);

//    String getSourceColumn();
//
//    void setSourceColumn(String sourceColumn);


}
