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

import org.gorpipe.gor.driver.meta.DataType;
import picocli.CommandLine;

import java.io.File;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "touch",
        aliases = {"t"},
        description="Touch files in cache.")
public class TouchCommand extends FilterOptions implements  Runnable{

    private int fileCounter;
    private String extra = "";

    @Override
    public void run() {
        this.extra = dryRun ? " (dryrun)" : "";
        touchCache(this.cachePath);
    }

    private void touchCache(File cachePath) {
        touchFiles(cachePath);

        err().printf("Touched %1$d files%2$s%n", fileCounter, extra);
    }

    private void touchFiles(File parentFile) {
        // get all the files from a directory
        File[] fileArray = parentFile.listFiles();

        if (fileArray == null) return;

        long timestamp = System.currentTimeMillis();

        for (File file : fileArray) {
            if (file.isFile()) {
                DataType type = DataType.fromFileName(file.getName());

                if (type != null && file.setLastModified(timestamp)) {
                    fileCounter++;
                    if (verbose) {
                        err().println("Touching file: " + file.toString() + this.extra);
                    }
                }
            } else if (file.isDirectory()) {
                touchFiles(file);
            }
        }
    }
}
