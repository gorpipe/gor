package org.gorpipe.gor.table.livecycle;


/**
 * Interface to change tables.
 */
public interface TableLifeCycle  {

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
