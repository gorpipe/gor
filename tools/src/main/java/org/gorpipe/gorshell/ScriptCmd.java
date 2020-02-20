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

import java.io.IOException;

@CommandLine.Command(
        name="script",
        description = "Run a GOR script from a file"
)
public class ScriptCmd implements Runnable {
    @CommandLine.ParentCommand
    private Commands parent;

    @CommandLine.Parameters(arity="0..1")
    private String scriptFile;

    @CommandLine.Option(names = "-s", description = "Saves the current script to the given file")
    private boolean save;

    @Override
    public void run() {
        if (scriptFile != null) {
            if (save) {
                parent.getShell().saveScript(scriptFile);
            } else {
                parent.getShell().script(scriptFile);
            }
        } else {
            parent.getShell().showScript();
        }
    }
}
