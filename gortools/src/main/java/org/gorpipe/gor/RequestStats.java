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

package org.gorpipe.gor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.stats.StatsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RequestStats implements EventLogger {
    private static final Logger log = LoggerFactory.getLogger(RequestStats.class);

    protected Map<String, GorScriptTask> nodes = new HashMap<>();
    protected Map<String, StatsCollector> stats = new HashMap<>();
    protected Map<String, String> createdFiles = new HashMap<>();

    protected int iteratorsCreated = 0;
    protected String query;
    protected final GorSession session;
    protected final String localHostName;

    public RequestStats(GorSession session) {
        log.info("RequestStats - {}", session);
        this.session = session;

        try {
            localHostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new GorSystemException("Cannot get local host name", e);
        }
    }

    public SessionInfo getInfo() {
        GorContext topLevelContext = session.getGorContext();
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.requestId = session.getRequestId();
        sessionInfo.query = query;
        sessionInfo.startedAt = topLevelContext.getStartedAt();
        sessionInfo.endedAt = topLevelContext.getEndedAt();

        stats.put("session", topLevelContext.getStats());
        addContextStats(topLevelContext);

        sessionInfo.stats = stats;
        sessionInfo.tasks = nodes;
        sessionInfo.createdFiles = createdFiles;
        sessionInfo.iteratorsCreated = iteratorsCreated;

        return sessionInfo;
    }

    private void addContextStats(GorContext ctx) {
        stats.put(ctx.getName(), ctx.getStats());
        for (GorContext nested: ctx.getNestedContexts()) {
            addContextStats(nested);
        }
    }

    protected void nodeAddedOrUpdated(GorScriptTask task) {
        // This is intended to be overridden in subclasses
    }

    protected void statsAdded(String signature, StatsCollector stats) {
        // This is intended to be overridden in subclasses
    }

    public void tasks(Collection<GorScriptTask> tasks) {
        tasks.forEach(t -> {
            GorScriptTask task = nodes.computeIfAbsent(t.name, k -> new GorScriptTask());
            task.update(t);
            nodeAddedOrUpdated(task);
        });
    }

    @Override
    public void query(String query) {
        this.query = query;
    }

    @Override
    public void commandCreated(String name, String origin, String commandSignature, String commandToExecute) {
        log.info("commandCreated: {} - {}", commandSignature, commandToExecute);

        GorScriptTask task = nodes.computeIfAbsent(name, k -> new GorScriptTask());
        task.name = name;
        task.origin = origin;
        task.signature = commandSignature;
        task.commandExecuted = commandToExecute;

        nodeAddedOrUpdated(task);
    }

    @Override
    public void commandStarted(String name, String commandSignature, String cacheFile, String commandToExecute) {
        log.info("commandStarted: {} - {}", commandSignature, commandToExecute);

        GorScriptTask task = nodes.computeIfAbsent(name, k -> new GorScriptTask());
        task.name = name;
        task.signature = commandSignature;
        task.startedAt = System.currentTimeMillis();
        task.host = localHostName;
        task.thread = Thread.currentThread().getName();
        task.cacheFile = cacheFile;
        task.commandExecuted = commandToExecute;

        nodeAddedOrUpdated(task);
    }

    @Override
    public void commandEnded(String name) {
        GorScriptTask task = nodes.get(name);
        if (task != null) {
            log.info("commandEnded: {})", name);
            task.endedAt = System.currentTimeMillis();
            nodeAddedOrUpdated(task);
        } else {
            log.warn("commandEnded: {} is unknown)", name);
        }

    }

    @Override
    public void commandCached(String name, String cacheFile) {
        GorScriptTask task = nodes.get(name);
        if (task != null) {
            log.info("commandCached: {})", name);
            task.endedAt = System.currentTimeMillis();
            task.cacheFile = cacheFile;
            task.cached = true;

            nodeAddedOrUpdated(task);
        } else {
            log.warn("commandCached: {} is unknown)", name);
        }
    }

    @Override
    public synchronized void iteratorCreated(String args) {
        iteratorsCreated++;
    }

    @Override
    public void endSession() {
        session.getGorContext().end();
        addContextStats(session.getGorContext());
    }

    static class QueryInfo {
        public String requestId;
        public Map<String, GorScriptTask> tasks = new HashMap<>();
        public Map<String, StatsCollector> stats = new HashMap<>();
    }

    public void saveToJson() throws IOException {
        QueryInfo queryInfo = new QueryInfo();
        queryInfo.requestId = session.getRequestId();
        queryInfo.tasks = nodes;
        queryInfo.stats = stats;

        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(queryInfo);
        String fileName = "gor-stats-" + session.getRequestId() + ".json";
        FileUtils.writeStringToFile(new File(fileName), json, Charset.defaultCharset());
    }
}
