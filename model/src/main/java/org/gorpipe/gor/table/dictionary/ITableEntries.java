/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor.table.dictionary;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Interface to work with table entries.
 * @param <T>  type of the table entries.
 */
public interface ITableEntries<T extends DictionaryEntry> {

    /**
     * Insert new entry
     * @param entry             the entry to add.
     * @param hasUniqueTags     
     */
    void insert(T entry, boolean hasUniqueTags);

    /**
     * Remove line.
     *
     * @param entryToRemove the entry to remove.
     * @param keepIfBucket if true and the line has bucket we mark the entry deleted, else we just remove the entry.
     */
    void delete(T entryToRemove, boolean keepIfBucket);

    /**
     * Clear/remove all the entries.
     */
    void clear();

    /**
     * Get all the entries.  Includes deleted entries.
     * @return all the entries.
     */
    List<T> getEntries();

    /**
     * Get the entries for the given tags/alias.
     * @param aliasesAndTags   the tags/alias to filter by.
     * @return  the entries for the given tags/alias.
     */
    List<T> getEntries(String... aliasesAndTags);

    /**
     * Get iterator to iterate through all the entries.
     * @return iterator for all the entries.
     */
    Iterator<T> iterator();

    /**
     * Check if the data has been loaded.
     * @return {@code True} if the data has been loaded, {@code False} otherwise.
     */
    boolean isLoaded();

    /**
     * Get all tags (and aliases) for active (not deleted) used in the dictionary file.
     *
     * @return all tags (and aliases) for active (not deleted) used in the dictionary file.
     */
    Set<String> getAllActiveTags();

    /**
     * Get number of entries.
     * @return  number of entries.
     */
    long size();

    /**
     * Find the entry matching the {@code entry}.
     * @param entry     the template to search by.
     * @return  the entry matching the template, or null if the entry is not found.
     */
    T findLine(T entry);
}
