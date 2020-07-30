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

import picocli.CommandLine;

@CommandLine.Command(
        name="set",
        description = "Set various settings for the shell",
        subcommands = {
                SetCmd.FileCache.class,
                SetCmd.RequestStats.class,
                SetCmd.Timing.class,
                SetCmd.DisplayResults.class,
                SetCmd.ConfigFile.class
        }
)
public class SetCmd implements Runnable {
    enum State {
        OFF,
        ON
    }

    @CommandLine.ParentCommand
    Commands parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    GorShell getShell() {
        return parent.getShell();
    }

    @Override
    public void run() {
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    @CommandLine.Command(
            name="fc",
            description = "Manipulate file cache"
    )
    static class FileCache implements Runnable {
        @CommandLine.ParentCommand
        private SetCmd parent;

        @CommandLine.Parameters(index = "0")
        private final
        State state = State.OFF;

        @Override
        public void run() {
            parent.getShell().setFileCacheEnabled(state == State.ON);
        }
    }

    @CommandLine.Command(
            name="rs",
            description = "Request stats"
    )
    static public class RequestStats implements Runnable {
        @CommandLine.ParentCommand
        private SetCmd parent;

        @CommandLine.Parameters(index = "0")
        private final
        State state = State.OFF;

        @Override
        public void run() {
            parent.getShell().setRequestStatsEnabled(state == State.ON);
        }
    }

    @CommandLine.Command(
            name="timing",
            description = "Enable/disable timing for gor queries"
    )
    static public class Timing implements Runnable {
        @CommandLine.ParentCommand
        private SetCmd parent;

        @CommandLine.Parameters(index = "0")
        private final
        State state = State.OFF;

        @Override
        public void run() {
            parent.getShell().setTimingEnabled(state == State.ON);
        }
    }

    @CommandLine.Command(
            name="dr",
            description = "Enable/disable display of results - not displaying results can be useful when timing"
    )
    static public class DisplayResults implements Runnable {
        @CommandLine.ParentCommand
        private SetCmd parent;

        @CommandLine.Parameters(index = "0")
        private final
        State state = State.OFF;

        @Override
        public void run() {
            parent.getShell().setDisplayResults(state == State.ON);
        }
    }

    @CommandLine.Command(
            name="config",
            description = "Set the config file for GOR"
    )
    static public class ConfigFile implements Runnable {
        @CommandLine.ParentCommand
        private SetCmd parent;

        @CommandLine.Parameters(index = "0", arity = "0..1")
        private String configFile;

        @Override
        public void run() {
            parent.getShell().setConfigFile(configFile);
        }
    }

}
