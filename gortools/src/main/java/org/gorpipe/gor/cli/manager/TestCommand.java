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

import org.gorpipe.gor.cli.BaseSubCommand;
import picocli.CommandLine;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "test",
        aliases = {"t"},
        description="Test various aspects of the table manager.",
        header="Test table manager.",
        subcommands = {TestReadLockCommand.class, TestWriteLockCommand.class,
            TestIsLockCommand.class})
public class TestCommand extends BaseSubCommand implements Runnable{

    @Override
    public void run() { CommandLine.usage(new TestCommand(), getStdErr()); }
}
