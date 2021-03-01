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

package org.gorpipe.gorshell;

import picocli.CommandLine;

@CommandLine.Command(
        name="",
        description = {"Enter a GOR statement, or any of the commands listed below.\n"},
        subcommands = {
                HelpCmd.class,
                ExitCmd.class,
                SetCmd.class,
                PositionCacheCmd.class,
                DefCmd.class,
                CreateCmd.class,
                ScriptCmd.class,
                PwdCmd.class,
                CdCmd.class,
                BashCmd.class,
                LsCmd.class
        }
)
public class Commands implements Runnable {
    public GorShell getShell() {
        return shell;
    }

    private final GorShell shell;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    Commands(GorShell sh) {
        shell = sh;
    }

    void print(String msg) {
        shell.lineReader.printAbove(msg);
    }

    @Override
    public void run() {
        // Everything is done subcommands
    }
}
