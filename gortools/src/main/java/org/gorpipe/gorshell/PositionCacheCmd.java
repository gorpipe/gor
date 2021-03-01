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

import org.gorpipe.gor.binsearch.PositionCache;
import picocli.CommandLine;

@CommandLine.Command(
        name="pc",
        description = "PositionCache",
        subcommands = {
                PositionCacheCmd.Info.class,
                PositionCacheCmd.Clear.class
        }
)
public class PositionCacheCmd implements Runnable {
    @CommandLine.ParentCommand
    Commands parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    @CommandLine.Command(
            name="info",
            description = "Get information on the position cache"
    )
    static class Info implements Runnable {
        @CommandLine.ParentCommand
        PositionCacheCmd parent;

        @Override
        public void run() {
            int files = PositionCache.getNumFilesInCache();
            int numKeys = PositionCache.getTotalNumKeysInCache();
            String msg = String.format("Position cache: %d files, %d keys", files, numKeys);
            parent.parent.print(msg);
        }
    }

    @CommandLine.Command(
            name="clear",
            description = "Clear the position cache"
    )
    static class Clear implements Runnable {
        @CommandLine.ParentCommand
        private PositionCacheCmd parent;

        @Override
        public void run() {
            PositionCache.clearGlobalCache();
            parent.parent.print("Position cache cleared");
        }
    }
}
