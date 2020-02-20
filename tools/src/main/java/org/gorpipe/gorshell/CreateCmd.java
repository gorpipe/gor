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

import java.util.List;

@CommandLine.Command(
        name="create",
        customSynopsis = "CREATE <name> = <GOR statement>",
        description = "A CREATE statement for a GOR script"
)
public class CreateCmd implements Runnable {
    @CommandLine.ParentCommand
    private Commands parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Parameters
    List<String> params;

    @Override
    public void run() {
        if (params == null) {
            parent.getShell().showCreates();
            return;
        }

        String line = String.join(" ", params);
        String[] parts = line.split("=", 2);

        if (parts.length == 2) {
            parent.getShell().create(parts[0].trim(), parts[1].trim());
        } else {
            parent.getShell().showCreate(parts[0].trim());
        }
    }
}
