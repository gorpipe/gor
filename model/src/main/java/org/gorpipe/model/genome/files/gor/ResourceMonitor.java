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

package org.gorpipe.model.genome.files.gor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.gorpipe.util.collection.extract.Extract;

/**
 * ResourceMonitor implements resource usage monitoring service within the gor system. It allows
 * reporting on multiple subqueries with in a single overall query.
 *
 * @version $Id$
 */
public class ResourceMonitor implements Serializable {
    /**
     * Resource Monitor entry describing the data volumes involved with the query
     */
    public static class Entry implements Serializable {
        /**
         * The query being executed
         */
        public final String queryText;
        /**
         * The time when the query started
         */
        public final long startTime;
        /**
         * The time when the query completed
         */
        public final long stopTime;
        /**
         * The number of resulting lines from the query
         */
        public final long resultLines;
        /**
         * The number of resulting columns from the query
         */
        public final int resultColumns;
        /**
         * The sources referenced by the query
         */
        public final String[] sources;
        /**
         * The number of lines read from each source
         */
        public final long[] sourceLines;
        /**
         * The entries describing each source subprocesses
         */
        public final Entry[][] sourceEntries;

        /**
         * @param queryText   The query text executed
         * @param resultLines The number of lines that are the results of this query
         * @param resultColumns The number of columns that are the results of this query
         * @param startTime   The start time of the query
         * @param sources     The sources used
         * @param sourceLines The source lines read
         * @param entries     All subprocesses entries
         */
        public Entry(String queryText, long resultLines, int resultColumns,  long startTime, String[] sources, long[] sourceLines, Entry[][] entries) {
            this.stopTime = System.currentTimeMillis();
            this.startTime = startTime;
            this.queryText = queryText;
            this.resultLines = resultLines;
            this.resultColumns = resultColumns;
            this.sources = sources.clone();
            this.sourceLines = sourceLines.clone();
            this.sourceEntries = entries.clone();
            assert sources.length == sourceLines.length && sources.length == entries.length;

        }

        /**
         * @return The duration time of the query
         */
        public String durationString() {
            return Extract.durationString(stopTime - startTime);
        }
    }

    /**
     * The unique ID of the query to monitor
     */
    public final String id;
    /**
     * The ip address of the client performing the query
     */
    public final String clientIpAddress;
    /**
     * More detailed information to identifying the context of the query
     */
    public final String context;
    /**
     * The time of query start
     */
    public final long startTime;

    private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
    private transient boolean done = false;

    private static final transient ConcurrentHashMap<String, ResourceMonitor> monitors = new ConcurrentHashMap<>();

    /**
     * @param id              The id of the newly constructed resource monitor
     * @param clientIpAddress The ip address
     * @param context         The query context
     */
    public ResourceMonitor(String id, String clientIpAddress, String context) {
        this.id = id;
        this.clientIpAddress = clientIpAddress;
        this.context = context;
        this.startTime = System.currentTimeMillis();
        if (id != null && null != monitors.putIfAbsent(id, this)) {
            throw new RuntimeException("There already exists a resouce monitor with this ID " + id);
        }
    }

    /**
     * @param id The unique ID of the monitor
     * @return The ResourceMonitor associated with the specified id, or null if not found
     */
    public static ResourceMonitor find(String id) {
        return monitors.get(id);
    }

    /**
     * @return The usage entries collected in this resource monitor
     */
    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Log the usage of the specified query
     *
     * @param entry The entry to log
     */
    void logUsage(Entry entry) {
        if (done) {
            throw new IllegalArgumentException("Can't log more usage after finish monitoring.");
        }
        entries.add(entry);
    }

    /**
     * @return True if all monitoring is done, else false
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Finish the monitoring task, i.e. mark the task done and collect summary statistics
     */
    public void finish() {
        done = true;

        // Calculate any summary statistics
    }

    /**
     * @return True if the monitored was deactivated (in effect removing it from the set of available monitors), false if already deactivated.
     */
    public boolean deactive() {
        finish(); // Complete the monitoring task and ensure we will not report again after closing
        return id == null || monitors.remove(id) != null;
    }
}
