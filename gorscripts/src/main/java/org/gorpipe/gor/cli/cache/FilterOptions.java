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

package org.gorpipe.gor.cli.cache;

import org.gorpipe.gor.cli.HelpOptions;
import picocli.CommandLine;

import java.io.File;

abstract class FilterOptions extends HelpOptions {

    @CommandLine.Option(names = {"-v", "--verbose"},
            description = "Display path of all files touched or purged.")
    boolean verbose;

    @CommandLine.Option(names = {"-d", "--dryrun"},
            description = "Give summary of deleted files but do not delete any cache files.")
    boolean dryRun;

    @CommandLine.Parameters(index = "0",
            arity = "1",
            paramLabel = "CachePath",
            description = "Path to cache.")
    File cachePath;
}
