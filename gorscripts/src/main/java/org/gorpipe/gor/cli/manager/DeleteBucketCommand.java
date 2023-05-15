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

import org.gorpipe.gor.manager.TableManager;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "delete_bucket",
        description="Delete the given bucket.",
        header="Delete a bucket")
public class DeleteBucketCommand extends ManagerOptions implements Runnable{

    @CommandLine.Parameters(index = "1",
            arity = "0..*",
            paramLabel = "BUCKET",
            description = "Buckets to delete, absolute path or relative to the table dir.  Values are specified as comma separated list.")
    private List<String> argsBuckets = new ArrayList<>();

    @Override
    public void run() {
        TableManager tm = TableManager.newBuilder().lockTimeout(Duration.ofSeconds(lockTimeout)).build();
        tm.deleteBuckets(dictionaryFile.toString(), argsBuckets.stream().toArray(String[]::new));
    }
}
