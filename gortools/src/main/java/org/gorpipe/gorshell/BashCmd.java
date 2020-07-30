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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

@CommandLine.Command(
        name="bash",
        description = "Run a bash command"
)
public class BashCmd implements Runnable {
    @CommandLine.ParentCommand
    private Commands parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Parameters
    List<String> params;

    @Override
    public void run() {
        if (params == null) {
            throw new CommandLine.ParameterException(spec.commandLine(), "bash expects some command");
        }

        String command = String.join(" ", params);
        runBashCommand(command, parent, spec);
    }

    static void runBashCommand(String command, Commands commandsInstance, CommandLine.Model.CommandSpec commandSpec) {
        Runtime r = Runtime.getRuntime();
        String[] commands = {"bash", "-c", command};
        try {
            File cwd = new File(System.getProperty("user.dir"));
            Process p = r.exec(commands, null, cwd);

            p.waitFor();
            BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";

            while ((line = b.readLine()) != null) {
                commandsInstance.print(line);
            }

            b.close();
        } catch (Exception e) {
            throw new CommandLine.ExecutionException(commandSpec.commandLine(), "Error", e);
        }
    }
}
