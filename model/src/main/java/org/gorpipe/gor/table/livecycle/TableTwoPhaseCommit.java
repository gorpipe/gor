package org.gorpipe.gor.table.livecycle;

import org.gorpipe.exceptions.GorException;


/**
 * Interface to safe tables with two phase commit.
 *
 * @param <T>
 */
public interface TableTwoPhaseCommit<T> extends TableLifeCycle<T> {

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

    default void save() {
        commitRequest();
        commit();
    }

}
