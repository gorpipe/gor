/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "help", header = "Displays help information about the specified command",
        synopsisHeading = "%nUsage: ", helpCommand = true,
        description = {"%nWhen no COMMAND is given, the usage help for the main command is displayed.",
                "If a COMMAND is specified, the help for that command is shown.%n"})
public class HelpCmd implements CommandLine.IHelpCommandInitializable2, Runnable{
    private static final Logger log = LoggerFactory.getLogger(HelpCmd.class);

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, descriptionKey = "helpCommand.help",
            description = "Show usage help for the help command and exit.")
    private boolean helpRequested;

    @CommandLine.Parameters(paramLabel = "COMMAND", descriptionKey = "helpCommand.command",
            description = "The COMMAND to display the usage help message for.")
    private final String[] commands = new String[0];

    private CommandLine self;
    private PrintWriter outWriter;
    private PrintWriter errWriter;
    private CommandLine.Help.ColorScheme colorScheme;
    private Map<String, String> gorHelpMap;

    @Override
    public void run() {
        if (gorHelpMap == null) {
            gorHelpMap = loadHelpFiles();
        }

        CommandLine parent = self == null ? null : self.getParent();
        if (parent == null) {
            return;
        }
        if (commands.length > 0) {
            CommandLine subcommand = parent.getSubcommands().get(commands[0]);
            if (subcommand != null) {
                subcommand.usage(outWriter, colorScheme);
            } else {
                String cmd = commands[0].toUpperCase();
                if (gorHelpMap != null && gorHelpMap.containsKey(cmd)) {
                    String helpMsg = gorHelpMap.get(cmd);
                    outWriter.println(helpMsg);
                } else {
                    throw new CommandLine.ParameterException(parent, "Unknown subcommand '" + commands[0] + "'.", null, commands[0]);
                }
            }
        } else {
            parent.usage(outWriter, colorScheme);

            outWriter.println("\nGOR input sources:");
            String[] inputSources = GorInputSources.getInputSources();
            outWriter.println(String.join(", ", inputSources));

            outWriter.println("\nGOR pipe commands:");
            String[] pipeCommands = GorPipeCommands.getGorCommands();
            outWriter.println(String.join(", ", pipeCommands));
        }
    }

    @Override
    public void init(CommandLine helpCommandLine, CommandLine.Help.ColorScheme colorScheme, PrintWriter outWriter, PrintWriter errWriter) {
        this.self        = helpCommandLine;
        this.colorScheme = colorScheme;
        this.outWriter   = outWriter;
        this.errWriter   = errWriter;
    }

    private Map<String, String> loadHelpFiles() {
        List<String> helpList = new ArrayList<>();
        String[] helpFiles = {"gor_commands_help.txt", "gor_functions_help.txt"};

        for (String helpFileEntry : helpFiles) {
            URI helpJarURL;
            try {
                helpJarURL = this.getClass().getClassLoader().getResource(helpFileEntry).toURI();
            } catch (URISyntaxException e) {
                log.warn("Failed loading help files", e);
                return null;
            }
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            try(FileSystem fileSystem = java.nio.file.FileSystems.newFileSystem(helpJarURL, env)) {
                helpList.addAll(java.nio.file.Files.readAllLines(java.nio.file.Paths.get(helpJarURL), StandardCharsets.ISO_8859_1));
            } catch (Exception e) {
                log.warn("Failed loading help files", e);
                return null;
            }
        }
        Map<String, String> map = new HashMap<>();
        listToMap(helpList.toArray(new String[0]), map);

        return map;
    }

    static void listToMap(String[] helpStrings, Map<String, String> helpMap) {
        String name = null;
        StringBuilder contents = null;
        for (String helpLine: helpStrings) {
            if (helpLine.startsWith("->")) {
                if (name != null) {
                    helpMap.put(name, contents.toString());
                }
                name = helpLine.substring(2).toUpperCase();
                contents = new StringBuilder();
            } else if (contents != null) {
                contents.append(helpLine);
                contents.append("\n");
            }
        }
        if(name != null) {
            helpMap.put(name, contents.toString());
        }
    }
}
