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

package org.gorpipe.gor.model;

import org.gorpipe.gor.session.GorContext;
import org.gorpipe.gor.session.GorSession;

import java.util.Iterator;
import java.util.function.Predicate;

public interface GenomicIterator extends Iterator<Row>, RowSourceStats, AutoCloseable {
    /**
     * Initialize the iterator with the given session.
     *
     * @param session Gor session
     */
    void init(GorSession session);

    default void setInsertSource(boolean insertSource) {}

    void setContext(GorContext context);

    /**
     * @return Source name associated with this iterator
     */
    String getSourceName();

    /**
     * @param sourceName Source name associated with this iterator
     */
    void setSourceName(String sourceName);

    boolean isSourceAlreadyInserted();

    void setSourceAlreadyInserted(boolean sourceAlreadyInserted);

    /**
     * Get the header describing the data
     *
     * @return A tab-separated string of column names
     */
    String getHeader();

    /**
     * Set the header
     * @param header A tab-separated string of column names
     */
    void setHeader(String header);

    /**
     * Seek to the specified genomic position in the data source
     *
     * @param chr The chromosome to find
     * @param pos The position within the chromosome to start with
     * @return True if data is available at or after the specified position, else false
     */
    boolean seek(String chr, int pos);

    default void moveToPosition(String seekChr, int seekPos, int maxReads) {
        seek(seekChr, seekPos);
    }

    default void moveToPosition(String seekChr, int seekPos) {
        moveToPosition(seekChr, seekPos, 10000);
    }

    /**
     * Seek to the specified genomic position in the data source
     *
     * @param chr The chromosome to find
     * @param pos The position within the chromosome to start with
     * @param end The position within the chromosome to end
     * @return True if data is available at or after the specified position, else false
     */
    default boolean seek(String chr, int pos, int end) {
        return seek(chr, pos);
    }

    /**
     * @return ResourceMonitor instance for source managing resources, or null for simple sources like file references.
     */
    default ResourceMonitor getMonitor() {
        return null;
    }

    /**
     * Returns an iterator on the elements from {@code this} which match the predicate in {@code rf}.
     *
     * Note: Once {@code filter} has been called {@code this} should not be used any further.
     */
    default GenomicIterator filter(Predicate<Row> rf)  {
        return new FilteredIterator(this, rf);
    }

    /**
     * Returns an iterator whose rows are formed of a subset of the columns of the underlying iterator.
     */
    default GenomicIterator select(int[] cols) {
        return new SelectIterator(this, cols);
    }

    /**
     * Sends the gor filter down to the source iterator
     * The source iterator pushdownFilter implementation
     * must parse the gor filter and translate it to corresponding
     * readable form for the source iterator
     * @param gorwhere
     * @return true if the filter is successfully pushed down
     */
    default boolean pushdownFilter(String gorwhere) {
        return false;
    }

    /**
     * Pushes down a one-to-one line (.map) function corresponding to
     * the gor calc step. The source iterator pushdownCalc implementation
     * must parse the gor calc and translate it to corresponding
     * readable form for the source iterator.
     * @param formula
     * @param colName
     * @return true if the map/calc step is successfully pushed down
     */
    default boolean pushdownCalc(String formula, String colName) {
        return false;
    }

    /**
     * Pushes down a column selection to the source iterator.
     * @param colList
     * @return if the selection step is successfully pushed down
     */
    default boolean pushdownSelect(String[] colList) {
        return false;
    }

    /**
     * Pushes down writing results file the source iterator.
     * @param filename
     * @return if the selection step is successfully pushed down
     */
    default boolean pushdownWrite(String filename) {
        return false;
    }

    /**
    /**
     * Pushes down external command.
     * @param cmd
     * @return if the selection step is successfully pushed down
     */
    default boolean pushdownCmd(String cmd) {
        return false;
    }

    /**
     * Pushes down arbitrary gor command (many-to-many, flatMap) to the source iterator.
     * The source iterator pushdownGor implementation
     * must parse the gor command and translate it to corresponding
     * readable form for the source iterator.
     * @param cmd
     * @return if the selection step is successfully pushed down
     */
    default boolean pushdownGor(String cmd) {
        return false;
    }

    /**
     * Pushes down result limitation to the source iterator.
     * @param limit
     * @return if the result limitation is successfully pushed down
     */
    default boolean pushdownTop(int limit) {
        return false;
    }

    default boolean isBuffered() {
        return false;
    }

    void close();
}
