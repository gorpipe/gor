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

package org.gorpipe.gor.tools;

import org.gorpipe.logging.GorLogbackUtil;
import org.gorpipe.model.genome.files.binsearch.GorIndexFile;
import org.gorpipe.model.genome.files.binsearch.GorIndexType;
import org.gorpipe.model.util.ConfigUtil;
import de.tototec.cmdoption.CmdCommand;
import de.tototec.cmdoption.CmdOption;
import de.tototec.cmdoption.CmdlineParser;
import de.tototec.cmdoption.handler.AddToCollectionHandler;
import de.tototec.cmdoption.handler.CmdOptionHandler;
import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import gorsat.process.GorPipeMacros;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@SuppressWarnings("squid:S106") //Ignore since this is a command line tool
public class GorToolsCLI {
    private static final Logger log = LoggerFactory.getLogger(GorToolsCLI.class);

    /**
     * Main for gortool.
     *
     * @param args commandline arguments.
     */
    static public void main(String[] args) {
        GorLogbackUtil.initLog("gortool");
        log.trace("GorToolsCLI starting");
        try {
            runCLI(args);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage() + "!\n");
            e.printStackTrace();
            System.out.println("Use 'gortool help' to get full command line help.");
            System.exit(-100);
        }
    }

    private static void usage(CmdlineParser cp) {
        final StringBuilder output = new StringBuilder();
        cp.usage(output);
        System.out.print(output.toString()
                .replace("[parameter]", "<url>")
                .replace("[command]", "<command>"));
    }

    private static void commandUsage(CmdlineParser cp) {
        // Capture the standard help.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        System.setOut(ps);
        cp.commandUsage(cp.getParsedCommandObject().getClass());
        System.out.flush();
        System.setOut(old);

        // Show what happened
        System.out.println(baos.toString().replace("Usage: gortool", "Usage: gortool <command>"));
    }

    @CmdCommand(names = {"commands"}, description = "Output info on GOR commands.")
    private static class GorCommandInfo implements Runnable {
        @Override
        public void run() {
            try {
                GorPipeCommands.register();
                System.out.print(GorPipeCommands.getCommandInfoTable());
            } catch (Exception e) {
                System.err.println("Error: \n");
                System.err.println(e.getMessage());
            }
        }
    }

    @CmdCommand(names = {"inputSources"}, description = "Output info on GOR input sources.")
    private static class GorInputSourceInfo implements Runnable {
        @Override
        public void run() {
            try {
                GorInputSources.register();
                System.out.print(GorInputSources.getInputSourceInfoTable());
            } catch (Exception e) {
                System.err.println("Error: \n");
                System.err.println(e.getMessage());
            }
        }
    }

    @CmdCommand(names = {"macros"}, description = "Output info on GOR macros.")
    private static class GorMacroInfo implements Runnable {
        @Override
        public void run() {
            try {
                GorPipeMacros.register();
                System.out.print(GorPipeMacros.getMacroInfoTable());
            } catch (Exception e) {
                System.err.println("Error: \n");
                System.err.println(e.getMessage());
            }
        }
    }

    @CmdCommand(names = {"index"}, description = "Index a gor file.")
    private static class IndexFile implements Runnable {
        @CmdOption(args = {"<gorfile>"}, description = "The gor file.", minCount = 1, maxCount = 1)
        private String gorfile;

        @CmdOption(names = {"--fullindex", "-f"}, description = "Full index.")
        protected boolean fullindex;

        @Override
        public void run() {
            int fi = gorfile.lastIndexOf('/')+1;
            String filename =  gorfile.substring(fi);
            Path gorindex = Paths.get(filename+".gori");
            GorIndexType indexType = fullindex ? GorIndexType.FULLINDEX : GorIndexType.CHROMINDEX;
            try (GorIndexFile file = new GorIndexFile(gorindex.toFile(), indexType)) {
                file.generateForGorz(gorfile);
            } catch (IOException e) {
                throw new RuntimeException("gor file index failed",e);
            }
        }
    }

    /**
     * * Add an one-arg option argument to a mutable collection of strings, the arg can be comma separated list.
     */
    private static class ListAddToCollectionHandler extends AddToCollectionHandler implements CmdOptionHandler {
        public void applyParams(final Object config, final AccessibleObject element, final String[] args, final String optionName) {
            try {
                final Field field = (Field) element;
                @SuppressWarnings("unchecked") final Collection<String> collection = (Collection<String>) field.get(config);
                collection.addAll(Arrays.stream(args[0].split("[,]")).map(String::trim).collect(Collectors.toList()));
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void runCLI(String ... args) throws IOException {
        ConfigUtil.loadConfig("gor");

        GenericOptions genericOpts = new GenericOptions();
        CmdlineParser cp = new CmdlineParser(genericOpts);
        cp.unregisterHandler(AddToCollectionHandler.class);
        cp.registerHandler(new ListAddToCollectionHandler());
        cp.setProgramName("gortool");
        cp.setAboutLine("Copyright (c) 2018 WuxiNextCode.");
        IndexFile idxFile = new IndexFile();
        cp.addObject(idxFile);
        GorCommandInfo gorCommandInfo = new GorCommandInfo();
        cp.addObject(gorCommandInfo);
        GorMacroInfo gorMacroInfo = new GorMacroInfo();
        cp.addObject(gorMacroInfo);
        GorInputSourceInfo gorInputSourceInfo = new GorInputSourceInfo();
        cp.addObject(gorInputSourceInfo);

        log.trace("CmdLineParser starting");
        cp.parse(false, true, args);
        log.trace("CmdLineParser done");

        String cmdName = cp.getParsedCommandName();
        if (genericOpts.version) {
            System.out.println(String.format("Gor File Indexer - Copyright (c) 2018 WuxiNextCode.  Version: %s",
                    GorToolsCLI.class.getPackage().getImplementationVersion()));
        } else if (genericOpts.help) {
            if (cmdName != null) {
                commandUsage(cp);
            } else {
                usage(cp);
            }
        } else if (cmdName != null) {
            Object obj = cp.getParsedCommandObject();
            ((Runnable)obj).run();
        }  else {
            usage(cp);
        }
    }

    /**
     * Generic options, include the table name and common options.
     */
    private static class GenericOptions {
        @CmdOption(names = {"-h", "--help", "help"}, description = "Display this help.", isHelp = true)
        private boolean help = false;
        @CmdOption(names = {"-v", "--version", "version"}, description = "Display version info.", isHelp = true)
        private boolean version = false;
    }
}
