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

package org.gorpipe.gor.cli;

import java.io.PrintStream;
import java.nio.file.Path;

import org.gorpipe.gor.cli.cache.CacheCommand;
import org.gorpipe.gor.cli.files.FilesCommand;
import org.gorpipe.gor.cli.git.GitCommand;
import org.gorpipe.gor.cli.help.HelpCommand;
import org.gorpipe.gor.cli.index.IndexCommand;
import org.gorpipe.gor.cli.info.InfoCommand;
import org.gorpipe.gor.cli.link.LinkCommand;
import org.gorpipe.gor.cli.manager.ManagerCommand;
import org.gorpipe.gor.cli.render.RenderCommand;
import org.gorpipe.logging.GorLogbackUtil;

import picocli.CommandLine;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name="gor",
        version="version 1.0",
        description = "Command line interface for gor query language and processes.",
        subcommands = {HelpCommand.class, ManagerCommand.class, IndexCommand.class,
                CacheCommand.class, RenderCommand.class, InfoCommand.class,
                LinkCommand.class, GitCommand.class, FilesCommand.class})
public class GorExecCLI extends HelpOptions implements CommandSupport, Runnable {

    public static void main(String[] args) {
        GorLogbackUtil.initLog("gor");
        CommandLine cmd = new CommandLine(new GorExecCLI());
        cmd.parseWithHandlers(
                new CommandLine.RunLast(),
                CommandLine.defaultExceptionHandler().andExit(-1),
                args);
    }

    private PrintStream stdOut;
    private PrintStream stdErr;

    @CommandLine.Option(names = {"-v", "--version"},
            versionHelp = true,
            description = "Print version information and exits.")
    boolean versionHelpRequested;

    @CommandLine.Option(defaultValue = "", names={"-p","--projectRoot"}, description = "Sets the project root for the current gor session.")
    private Path projectRoot;

    @CommandLine.Option(defaultValue = "", names={"--securityContext"}, description = "Sets the security context for the current gor session.")
    private String securityContext;

    public GorExecCLI() {
        stdOut = System.out;
        stdErr = System.err;
    }

    public GorExecCLI(PrintStream out, PrintStream err) {
        this.stdOut = out;
        this.stdErr = err;
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }

    public String getProjectRoot() {
        return projectRoot.toString();
    }

    public String getSecurityContext() {
        return securityContext;
    }

    public PrintStream getStdOut() {
        return stdOut;
    }

    public PrintStream getStdErr() {
        return stdErr;
    }

    @Override
    public void exit(int status) {
        if (System.err.equals(getStdErr())) {
            System.exit(status);
        } else {
            throw new CommandLine.ExecutionException(new CommandLine(this),
                    "Exit with status: " + status);
        }
    }
}
