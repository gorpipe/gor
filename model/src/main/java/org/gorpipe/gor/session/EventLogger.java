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

package org.gorpipe.gor.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.gorpipe.gor.stats.StatsCollector;

import java.util.Collection;
import java.util.Map;

public interface EventLogger {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    class SessionInfo {
        public String query;
        public String requestId;
        public long startedAt;
        public long endedAt;
        public int iteratorsCreated;
        public Map<String, GorScriptTask> tasks;
        public Map<String, StatsCollector> stats;
        public Map<String, String> createdFiles;
    }

    void query(String query);
    void tasks(Collection<GorScriptTask> tasks);
    void commandCreated(String name, String origin, String commandSignature, String commandToExecute);
    void commandStarted(String name, String commandSignature, String cacheFile, String commandToExecute);
    void commandEnded(String commandSignature);
    void commandCached(String name, String cacheFile);
    void iteratorCreated(String args);
    void endSession();
    SessionInfo getInfo();
    StatsCollector getStatsCollector();
}
