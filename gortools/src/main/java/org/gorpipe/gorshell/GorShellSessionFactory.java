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

package org.gorpipe.gorshell;

import org.gorpipe.client.FileCache;
import org.gorpipe.gor.*;
import org.gorpipe.gor.clients.LocalFileCacheClient;
import org.gorpipe.gor.clients.NoCacheFileCacheClient;
import org.gorpipe.model.genome.files.gor.DriverBackedFileReader;
import gorsat.QueryHandlers.GeneralQueryHandler;
import gorsat.process.FreemarkerReportBuilder;
import gorsat.process.GenericRunnerFactory;
import gorsat.process.SessionBasedQueryEvaluator;

import java.nio.file.Paths;
import java.util.UUID;

public class GorShellSessionFactory extends GorSessionFactory {

    private String root = "";
    private final String cacheDir;
    private String configFile = "";
    private boolean fileCacheEnabled = true;
    private boolean requestStatsEnabled = false;

    public GorShellSessionFactory() {
        cacheDir = System.getProperty("java.io.tmpdir");
    }

    public GorShellSessionFactory(String root) {
        this.root = root;
        cacheDir = System.getProperty("java.io.tmpdir");
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public void setFileCacheEnabled(boolean fileCacheEnabled) {
        this.fileCacheEnabled = fileCacheEnabled;
    }

    public void setRequestStatsEnabled(boolean requestStatsEnabled) {
        this.requestStatsEnabled = requestStatsEnabled;
    }

    @Override
    public GorSession create() {
        String requestId = UUID.randomUUID().toString();

        GorSession session = new GorSession(requestId);

        ProjectContext.Builder projectContextBuilder = new ProjectContext.Builder();

        FileCache fileCache;
        if(fileCacheEnabled) {
            fileCache = new LocalFileCacheClient(Paths.get(this.cacheDir));
        } else {
            fileCache = new NoCacheFileCacheClient(Paths.get(this.cacheDir));
        }

        projectContextBuilder
                .setRoot(this.root)
                .setCacheDir(this.cacheDir)
                .setConfigFile(this.configFile)
                .setFileReader(new DriverBackedFileReader("", this.root, null))
                .setFileCache(fileCache)
                .setQueryHandler(new GeneralQueryHandler(session.getGorContext(), false))
                .setQueryEvaluator(new SessionBasedQueryEvaluator(session));

        SystemContext.Builder systemContextBuilder = new SystemContext.Builder();
        systemContextBuilder
                .setReportBuilder(new FreemarkerReportBuilder(session))
                .setRunnerFactory(new GenericRunnerFactory())
                .setServer(false)
                .setStartTime(System.currentTimeMillis());

        GorSessionCache cache = GorSessionCacheManager.getCache(requestId);

        EventLogger eventLogger;
        if (requestStatsEnabled) {
            eventLogger = new RequestStats(session);
        } else {
            eventLogger = new DefaultEventLogger();
        }

        session.init(projectContextBuilder.build(),
                systemContextBuilder.build(),
                cache,
                eventLogger
                );

        return session;
    }
}
