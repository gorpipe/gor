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

package gorsat.process;

import gorsat.QueryHandlers.GeneralQueryHandler;
import org.gorpipe.gor.*;
import org.gorpipe.gor.clients.LocalFileCacheClient;
import org.gorpipe.model.genome.files.gor.DriverBackedFileReader;

import java.nio.file.Paths;
import java.util.UUID;

/**
 * Factory class for creating default/generic gor sessions. This factory replaces the createDefaultSession() method.
 */
public class GenericSessionFactory extends GorSessionFactory {

    private String root = "";
    private final String cacheDir;
    private String configFile = "";

    public GenericSessionFactory() {
        cacheDir = System.getProperty("java.io.tmpdir");
    }

    public GenericSessionFactory(String root, String cacheDir) {
        this.root = root;
        this.cacheDir = cacheDir;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    @Override
    public GorSession create() {
        String requestId = UUID.randomUUID().toString();

        GorSession session = new GorSession(requestId);

        ProjectContext.Builder projectContextBuilder = new ProjectContext.Builder();
        projectContextBuilder
                .setRoot(this.root)
                .setCacheDir(this.cacheDir)
                .setConfigFile(this.configFile)
                .setFileReader(new DriverBackedFileReader("", this.root, null))
                .setFileCache(new LocalFileCacheClient(Paths.get(this.cacheDir)))
                .setQueryHandler(new GeneralQueryHandler(session.getGorContext(), false))
                .setQueryEvaluator(new SessionBasedQueryEvaluator(session));

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
