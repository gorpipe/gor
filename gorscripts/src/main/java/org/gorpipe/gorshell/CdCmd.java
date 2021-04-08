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

import java.io.File;
import java.io.IOException;

@CommandLine.Command(
        name="cd",
        description = "Change the current working directory"
)
public class CdCmd implements Runnable {
    @CommandLine.ParentCommand
    private Commands parent;

    @CommandLine.Parameters(index = "0")
    private String directory_name;

    @Override
    public void run() {
        File directory = new File(directory_name).getAbsoluteFile();
        if (directory.exists()) {
            try {
                String canonicalPath = directory.getCanonicalPath();
                System.setProperty("user.dir", canonicalPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            parent.print("cd: no such file or directory: " + directory_name);
        }
    }
}
