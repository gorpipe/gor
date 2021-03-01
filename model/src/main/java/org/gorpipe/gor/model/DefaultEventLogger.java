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

import org.gorpipe.gor.session.EventLogger;
import org.gorpipe.gor.session.GorScriptTask;
import org.gorpipe.gor.stats.StatsCollector;

import java.util.Collection;

@SuppressWarnings("squid:S1186")
public class DefaultEventLogger implements EventLogger {
    @Override
    public void query(String query) {}

    @Override
    public void tasks(Collection<GorScriptTask> tasks) {}

    @Override
    public void commandCreated(String name, String origin, String commandSignature, String commandToExecute) {}

    @Override
    public void commandStarted(String name, String commandSignature, String cacheFile, String commandToExecute) {}

    @Override
    public void commandEnded(String commandSignature) {}

    @Override
    public void commandCached(String name, String cacheFile) {}

    @Override
    public void iteratorCreated(String args) {}

    @Override
    public void endSession() {}

    @Override
    public SessionInfo getInfo() {
        return null;
    }

    @Override
    public StatsCollector getStatsCollector() {
        return null;
    }
}
