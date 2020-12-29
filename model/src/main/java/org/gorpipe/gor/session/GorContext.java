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

import org.gorpipe.gor.stats.StatsCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GorContext {
    private final GorSession session;
    private final String name;
    private final String command;
    private final String signature;

    private boolean allowMergeQuery;
    private Optional<String> sortCols = Optional.empty();
    private StatsCollector stats = null;

    private long startedAt = System.currentTimeMillis();
    private long endedAt = 0;

    private final GorContext outerContext;
    private final List<GorContext> nestedContexts = new ArrayList<>();

    private int numIteratorsCreated = 0;

    public GorContext(GorSession session) {
        this(session, null, "gorfinal", "", "[gorfinal]");
    }

    public GorContext(GorSession session, GorContext outer, String signature, String cmd, String name, boolean allowMergeQuery) {
        this.session = session;
        this.outerContext = outer;
        this.signature = signature;
        this.command = cmd;
        this.name = name;
        this.allowMergeQuery = allowMergeQuery;
    }

    public GorContext(GorSession session, GorContext outer, String signature, String cmd, String name) {
        this(session, outer, signature, cmd, name, true);
    }

    public boolean getAllowMergeQuery() {
        return allowMergeQuery;
    }

    public void setAllowMergeQuery(boolean allowMergeQuery) {
        this.allowMergeQuery= allowMergeQuery;
    }

    public Optional<String> getSortCols() {
        return sortCols;
    }

    public void setSortCols(String sortCols) {
        this.sortCols = Optional.of(sortCols);
    }

    public String getName() {
        return name;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public long getDuration() {
        if (endedAt > 0) {
            return endedAt - startedAt;
        } else {
            return 0;
        }
    }

    public void start(String outfile) {
        startedAt = System.currentTimeMillis();
        session.getEventLogger().commandStarted(name, signature, outfile, command);
    }

    public void end() {
        endedAt = System.currentTimeMillis();
        session.getEventLogger().commandEnded(name);
    }

    public void cached(String cacheFile) {
        session.getEventLogger().commandCached(name, cacheFile);
    }

    public GorSession getSession() {
        return session;
    }

    public String getSignature() {
        return signature;
    }

    public StatsCollector getStats() {
        if (stats == null && session.getEventLogger() != null) {
            stats = session.getEventLogger().getStatsCollector();
        }
        return stats;
    }

    public String getCommand() {
        return command;
    }

    public GorContext[] getNestedContexts() {
        return nestedContexts.toArray(new GorContext[0]);
    }

    public synchronized GorContext createNestedContext(String name, String signature, String cmd) {
        String sig = signature != null ? signature : String.format("%s.nested_%02d", this.signature, nestedContexts.size());
        String nestedName = name != null ? name : String.format("%s.nested_%02d", this.name, nestedContexts.size());
        GorContext gorContext = new GorContext(session, this, sig, cmd, nestedName);
        nestedContexts.add(gorContext);
        return gorContext;
    }

    public void iteratorCreated(String s) {
        numIteratorsCreated++;
    }
}
