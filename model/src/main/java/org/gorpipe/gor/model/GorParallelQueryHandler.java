/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.gorpipe.gor.monitor.GorMonitor;

/**
 * GorParallelQueryHandler
 *
 * @version $Id$
 */
public interface GorParallelQueryHandler {
    /**
     * Execute batch of gor queries on cluster.
     * All the arrays are in consistent order
     *
     * @param fingerprints      Fingerprints for each query
     * @param commandsToExecute Gor commands
     * @param batchGroupNames   Name of batch each query belongs to
     * @param cancelMonitor     Cancel Monitor
     * @return File paths
     */
    String[] executeBatch(String[] fingerprints, String[] commandsToExecute, String[] batchGroupNames, String[] cacheFiles, GorMonitor cancelMonitor);

    /**
     * Force overwrite on all subqueries.
     *
     * @param force
     */
    void setForce(boolean force);

    /**
     *  Submission time (if available) in seconds from Jan 1st 1970 (i.e. like System.currentTimeMillis()/1000)
     *
     * @param time Time in seconds (From Jan 1st 1970)
     */
    void setQueryTime(Long time);

    /**
     * Get time spent waiting for jobs
     *
     * @return Time in milliseconds
     */
    long getWaitTime();
}
