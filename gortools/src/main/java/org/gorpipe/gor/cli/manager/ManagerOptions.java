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

package org.gorpipe.gor.cli.manager;

import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.manager.TableManager;
import picocli.CommandLine;

import java.io.File;

public abstract class ManagerOptions extends HelpOptions {

    @CommandLine.Option(names = {"--lock_timeout"},
            description = "Maximum time (in seconds) we will wait for acquiring lock an a resource.  Default: 1800 sec.")
    protected int lockTimeout = Math.toIntExact(TableManager.DEFAULT_LOCK_TIMEOUT.getSeconds());

    @CommandLine.Option(names = {"--nohistory"},
            description = "Don't keep history of gord files in the dictionary folder.  If not set we only keep the last one.  Default: true.")
    protected boolean nohistory;

    @CommandLine.Parameters(index = "0",
            arity = "1",
            paramLabel = "DICTIONARY",
            description = "Table/dictionary to work with.")
    File dictionaryFile;
}
