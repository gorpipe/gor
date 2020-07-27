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

package org.gorpipe.gor.cli.query;

import ch.qos.logback.classic.Level;
import gorsat.process.CLIGorExecutionEngine;
import gorsat.process.GorExecutionEngine;
import gorsat.process.PipeOptions;
import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.model.genome.files.gor.DbSource;
import org.gorpipe.model.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@CommandLine.Command(name = "query", aliases = {"q"},
        description="Execute a gor query, script or template",
        header = "Execute a gor query, script or template")
public class QueryCommand extends HelpOptions implements Runnable{

    private static final Logger consoleLogger = LoggerFactory.getLogger("console." + QueryCommand.class);

    @CommandLine.Option(defaultValue = "false", names={"-t","--stacktrace"}, description = "Displays stack trace for errors.")
    private boolean showStackTrace;

    @CommandLine.Option(defaultValue = "false", names={"-s","--scriptfile"}, description = "The input query is a script file.")
    private boolean inputIsScript;

    @CommandLine.Option(names={"-c","--config"}, description = "Loads configuration from external file.")
    private File configFile;

    @CommandLine.Option(names={"-a","--aliases"}, description = "Loads aliases from external file.")
    private File aliasFile;

    @CommandLine.Option(names={"-d","--cachedir"}, description = "Path to cache directory for the current gor query.")
    private final Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"));

    @CommandLine.Option(defaultValue = "", names={"-p","--projectroot"}, description = "Sets the project root for the current gor query.")
    private Path projectRoot;

    @CommandLine.Option(names={"-r","--requestid"}, description = "Sets a request id for the current gor query, used to identify logs and errors.")
    private final String requestId = UUID.randomUUID().toString();

    @CommandLine.Option(names={"-l","--loglevel"}, defaultValue = "warn", description = "Sets the log level to use for the current gor query. Available levels are none, debug, info, warn or error")
    private String logLevel;

    @CommandLine.Option(defaultValue = "0", names={"-w","--workers"}, description = "Number of workers to execute the current gor query.")
    private final int workers = 0;

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "InputQuery", description = "Queries to execute. Queries can be direct gor query, files containing gor script or gor report template.")
    private String query;

    @Override
    public void run() {

        setLogLevel();

        // Parse the input parameters
        PipeOptions commandlineOptions = new PipeOptions();

        if (cacheDir != null)
            commandlineOptions.cacheDir_$eq(cacheDir.toString());
        if (aliasFile != null)
            commandlineOptions.aliasFile_$eq(aliasFile.toString());
        if (configFile != null)
            commandlineOptions.configFile_$eq(configFile.toString());
        if (projectRoot != null)
            commandlineOptions.gorRoot_$eq(projectRoot.toString());
        commandlineOptions.requestId_$eq(requestId);
        commandlineOptions.showStackTrace_$eq(showStackTrace);
        commandlineOptions.workers_$eq(workers);

        ExceptionUtilities.setShowStackTrace(showStackTrace);

        int exitCode = 0;
        //todo find a better way to construct

        try {
            // Load the input query
            commandlineOptions.query_$eq(loadQuery(query, inputIsScript));

            // Initialize config
            ConfigUtil.loadConfig("gor");

            // Initialize database connections
            DbSource.initInConsoleApp();

            GorExecutionEngine executionEngine = new CLIGorExecutionEngine(commandlineOptions, null, null);
            executionEngine.execute();
        } catch (GorException ge) {
            consoleLogger.error(ExceptionUtilities.gorExceptionToString(ge));
            exitCode = -1;
        } catch (Exception e) {
            consoleLogger.error("Unexpected error, please report if you see this.\n" + e.getMessage(), e);
            exitCode = -1;
        }

        System.exit(exitCode);
    }

    private void setLogLevel() {
        String logName = "ROOT";

        try {
            Level logLevel = ch.qos.logback.classic.Level.toLevel(this.logLevel, Level.WARN);
            Logger mainLogger = LoggerFactory.getLogger(logName);
            ((ch.qos.logback.classic.Logger)mainLogger).setLevel(logLevel);
        } catch (Exception e) {
            consoleLogger.warn("Failed to set {} log level to {}", logName, logLevel);
        }
    }

    private String loadQuery(String query, boolean inputIsScript) {

        String[] args;
        if (inputIsScript) {
            args = new String[]{"-script " + query};
        } else {
            args = new String[]{query};
        }
        return PipeOptions.getQueryFromArgs(args);
    }
}
