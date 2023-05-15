package org.gorpipe.gor.table.livecycle;


/**
 * Interface to change tables.
 *
 * @param <T>
 */
public interface TableLifeCycle<T>  {

    /**
     * Intialize the table on disk.
     */
    void initialize();

    /**
     * Save the table.
     * Does commitRequest and commit in one go.
     */
    void save();

    void delete();

}
