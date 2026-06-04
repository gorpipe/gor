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

package org.gorpipe.gor.cli.server;

import gorsat.process.GorTestServer;
import gorsat.process.PipeOptions;
import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.model.DbConnection;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "test-server",
        description = "Start an HTTP server that accepts GOR queries via POST /query",
        header = "Start a GOR test server")
public class TestServerCommand extends HelpOptions implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TestServerCommand.class);

    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "4242",
            description = "Port to listen on (default: 4242)")
    private int port;

    @CommandLine.Option(names = {"-d", "--cachedir"},
            description = "Path to cache directory for query execution")
    private String cacheDir;

    @CommandLine.Option(names = {"-w", "--workers"}, defaultValue = "0",
            description = "Number of parallel workers for query execution")
    private int workers;

    @Override
    public void run() {
        try {
            ConfigUtil.loadConfig("gor");
            DbConnection.initInConsoleApp();

            PipeOptions opts = new PipeOptions();
            opts.cacheDir_$eq(cacheDir != null ? cacheDir : ProjectContext.DEFAULT_CACHE_DIR);
            opts.workers_$eq(workers);
            opts.color_$eq("none");

            GorTestServer.start(port, opts);
        } catch (Exception e) {
            log.error("Failed to start test server: {}", e.getMessage(), e);
            System.exit(-1);
        }
    }
}
