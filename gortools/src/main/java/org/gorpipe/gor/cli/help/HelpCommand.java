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

package org.gorpipe.gor.cli.help;

import org.gorpipe.gor.cli.HelpOptions;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "help",
        aliases = {"h"},
        description="Gives help on gor commands and functions.",
        header="Gives help on gor commands and functions.")
public class HelpCommand extends HelpOptions implements Runnable{

    @CommandLine.Option(names = {"-f", "--helpfile"},
            description = "File containing the help text for all commands and functions.")
    private File helpFile;

    @CommandLine.Parameters(defaultValue = "COMMANDS",
            index = "0",
            arity = "1",
            paramLabel = "command",
            description = "View help for the selected command or function.")
    private String command;

    @Override
    public void run() {
        List<String> helpList;
        StringBuilder message = new StringBuilder();
        boolean toPrint = false;

        try {
            helpList = loadHelpList(helpFile);
            String helpCommandUpperCase = command.toUpperCase();

            for (String helpLine : helpList) {
                if (helpLine.startsWith("->")) {
                    if (helpLine.substring(2).toUpperCase().startsWith(helpCommandUpperCase)) {
                        toPrint = true;
                        message.append("\n")
                                .append(helpLine.substring(2))
                                .append("\n")
                                .append("=====================", 2, helpLine.length())
                                .append("\n\n");
                    } else {
                        toPrint = false;
                    }
                } else if (toPrint) {
                    message.append(helpLine).append("\n");
                }
            }
        } catch (Exception e) {
            message.append("Failed to load help for command ").append(command);
        }

        System.out.println(message);
    }

    private List<String> loadHelpList(File helpFile) throws IOException, URISyntaxException {
        List<String> helpList = null;

        if (helpFile != null) {
            helpList = FileUtils.readLines(helpFile, Charset.defaultCharset());
        } else {
            helpList = loadHelpFiles();
        }

        return helpList;
    }

    private List<String> loadHelpFiles() throws IOException, URISyntaxException {
        List<String> helpList = new ArrayList<>();
        String[] helpFiles = {"gor_commands_help.txt", "gor_functions_help.txt"};

        for (String helpFileEntry : helpFiles) {
            URI helpJarURL = this.getClass().getClassLoader().getResource(helpFileEntry).toURI();
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            try(FileSystem fileSystem = java.nio.file.FileSystems.newFileSystem(helpJarURL, env)) {
                helpList.addAll(java.nio.file.Files.readAllLines(java.nio.file.Paths.get(helpJarURL), java.nio.charset.Charset.forName("ISO-8859-1")));
            }
        }

        return helpList;
    }
}
