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

package gorsat.analysis;

import gorsat.QueryHandlers.GeneralQueryHandler;
import gorsat.process.*;
import org.gorpipe.gor.clients.LocalFileCacheClient;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.session.GorSessionCache;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.gor.session.SystemContext;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.model.gor.iterators.RefSeqRotatingFactory;

import java.nio.file.Paths;
import java.util.UUID;

/**
 * Factory class for creating session for analysis tests. Includes rotating ref seq factory.
 */
public class AnalysisTestSessionFactory extends GorSessionFactory {

    private String cacheDir;

    AnalysisTestSessionFactory() {
        cacheDir = System.getProperty("java.io.tmpdir");
    }

    @Override
    public GorSession create() {
        String requestId = UUID.randomUUID().toString();

        GorSession session = new GorSession(requestId);

        ProjectContext.Builder projectContextBuilder = new ProjectContext.Builder();
        String root = "";
        String configFile = "";
        projectContextBuilder
                .setRoot(root)
                .setCacheDir(this.cacheDir)
                .setConfigFile(configFile)
                .setFileReader(new DriverBackedFileReader("", root, null))
                .setFileCache(new LocalFileCacheClient(Paths.get(this.cacheDir)))
                .setQueryHandler(new GeneralQueryHandler(session.getGorContext(), false))
                .setQueryEvaluator(new SessionBasedQueryEvaluator(session))
                .setRefSeqFactory(new RefSeqRotatingFactory());

        SystemContext.Builder systemContextBuilder = new SystemContext.Builder();
        systemContextBuilder
                .setReportBuilder(new FreemarkerReportBuilder(session))
                .setRunnerFactory(new GenericRunnerFactory())
                .setServer(false)
                .setStartTime(System.currentTimeMillis());

        GorSessionCache cache = GorSessionCacheManager.getCache(requestId);

        session.init(projectContextBuilder.build(),
                systemContextBuilder.build(),
                cache);

        return session;
    }
}
