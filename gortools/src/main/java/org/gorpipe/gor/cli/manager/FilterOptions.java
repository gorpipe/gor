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

import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public abstract class FilterOptions extends ManagerOptions {

    @CommandLine.Option(names = {"-f", "--files"},
            split = ",",
            description = "Files to filter by.  Values are specified as comma separated list.  Files are specified absolute or relative to the table dir.  Values are specified as comma separated list.")
    protected List<String> files = new ArrayList<>();

    @CommandLine.Option(names = {"-t", "--tags"},
            split = ",",
            description = "Tags to filter by.  Values are specified as comma separated list.")
    protected List<String> tags = new ArrayList<>();

    @CommandLine.Option(names = {"-b", "--buckets"},
            split = ",",
            description = "Buckets to filter by.  Values are specified as comma separated list.")
    protected List<String> buckets = new ArrayList<>();

    @CommandLine.Option(names = {"-a", "--aliases"},
            split = ",",
            description = "Aliases to filter by.  Values are specified as comma separated list.")
    protected List<String> aliases = new ArrayList<>();

    @CommandLine.Option(names = {"-r", "-p", "--range"},
            description = "Range to filter by.  Value is specified as <chrom start>[:<poststart>][-[<chrom stop>:][<pos stop>]].\n" +
                    "Note:  When seleting table lines based on ranges they always have to match exactly.")
    protected String range = null;

    @CommandLine.Option(names = {"--include_deleted"},
            description = "Should deleted files be included.  Mostly for debugging purposes.")
    protected boolean includeDeleted = false;
}
