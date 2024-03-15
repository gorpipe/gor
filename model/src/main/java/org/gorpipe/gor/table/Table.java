package org.gorpipe.gor.table;

import org.gorpipe.gor.table.livecycle.TableInsertDelete;
import org.gorpipe.gor.table.livecycle.TableTwoPhaseCommit;

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
public interface Table<T> extends TableInfo, TableTwoPhaseCommit, TableInsertDelete<T> {

    void reload();

    void setProperty(String key, String value);

    void setColumns(String[] columns);

    void setValidateFiles(boolean validateFiles);

    void setUseHistory(boolean useHistory);
}
