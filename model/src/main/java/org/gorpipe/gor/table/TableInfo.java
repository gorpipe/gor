package org.gorpipe.gor.table;

import org.gorpipe.gor.model.FileReader;

import java.util.stream.Stream;

/**
 * Interface to table basic info.
 *
 * The table object can not be muted using this interface.
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
public interface TableInfo<T> {

    /**
     * @return name of this table.
     */
    String getName();

    String getId();

    /**
     * Get absolute path to this table.
     *
     * @return absolute path to this table.
     */
    String getPath();

    /**
     * Get the table folder path.   The table folder is hidden folder that sits next to
     * the dictionary and contains various files related to it.
     *
     * @return the table folder path.
     */
    String getFolderPath();

    String[] getColumns();

    /**
     * Get table property {@code key}
     *
     * @param key property to get.
     * @return table properyt {@code key}
     */
    String getProperty(String key);

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
    Stream<String> getLines();

    String getRootPath();

    String formatHeader();

    FileReader getFileReader();

}
