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

package org.gorpipe.gor.manager;

import de.tototec.cmdoption.CmdCommand;
import de.tototec.cmdoption.CmdOption;
import de.tototec.cmdoption.CmdlineParser;
import de.tototec.cmdoption.handler.AddToCollectionHandler;
import de.tototec.cmdoption.handler.CmdOptionHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.dictionary.DictionaryTableMeta;
import org.gorpipe.gor.table.dictionary.TableFilter;
import org.gorpipe.gor.table.lock.TableTransaction;
import org.gorpipe.gor.table.util.GenomicRange;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.lock.TableLock;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.logging.GorLogbackUtil;
import org.gorpipe.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.gorpipe.gor.table.util.PathUtils.relativize;

/**
 * Command line interface for the table manager.
 * <p>
 * Uses the CMDOption library:  https://github.com/ToToTec/CmdOption#pre-registered-cmdoptionhandlers
 * <p>
 * Created by gisli on 21/08/16.
 */
public class TableManagerCLI {
    private static final Logger log = LoggerFactory.getLogger(TableManagerCLI.class);

    /**
     * Main for gormanager.
     *
     * @param args commandline arguments.
     */
    static public void main(String[] args) {
        GorLogbackUtil.initLog("gormanager");

        log.trace("TableManager starting");
        ConfigUtil.loadConfig("gor");
        log.trace("config loaded");

        GenericOptions genericOpts = new GenericOptions();
        CmdlineParser cp = new CmdlineParser(genericOpts);
        cp.unregisterHandler(AddToCollectionHandler.class);
        cp.registerHandler(new ListAddToCollectionHandler());
        cp.setProgramName("gormanager");
        cp.setAboutLine("Copyright (c) 2017 WuxiNextCode.");
        cp.addObject(
                new CommandInsert(),
                new CommandMultiInsert(),
                new CommandDelete(),
                new CommandSelect(),
                new CommandBucketize(),
                new CommandDeleteBucket(),
                new CommandTest());

        log.trace("CmdLineParser starting");
        String errMessage = null;
        try {
            cp.parse(args);
        } catch (RuntimeException e) {
            errMessage = "ERROR: " + e.getMessage() + "!\n";
        }
        log.trace("CmdLineParser done");

        if (errMessage == null && !genericOpts.help && cp.getParsedCommandName() != null) {
            ((CommandRun) cp.getParsedCommandObject()).run(genericOpts);
        } else if (genericOpts.help && cp.getParsedCommandName() != null) {
            commandUsage(cp);
        } else if (genericOpts.version) {
            System.out.println(String.format("gormanager Copyright (c) 2017 WuxiNextCode.  Version: %s",
                    TableManagerCLI.class.getPackage().getImplementationVersion()));
        } else {
            if (errMessage != null) {
                System.err.println(errMessage);
                System.out.println("Use 'gormanager help' to get full command line help.");
                System.exit(-100);
            } else {
                usage(cp);
            }
        }
    }

    /**
     * Generic options, include the table name and common options.
     */
    private static class GenericOptions {
        @CmdOption(args = {"<table/dictionary file>"}, description = "Table/dictionary to work with.", minCount = 1)
        private String table = "";
        @CmdOption(names = {"-h", "--help", "help"}, description = "Display this help.", isHelp = true)
        private boolean help = false;
        @CmdOption(names = {"--lock_timeout"}, args = {"<value>"}, description = "Maximum time (in seconds) we will wait for acquiring lock an a resource.  Default: 1800 sec.")
        private int lockTimeout = Math.toIntExact(TableManager.DEFAULT_LOCK_TIMEOUT.getSeconds());
        @CmdOption(names = {"--history"}, args = {"<value>"}, description = "Keep history of commands in an action log file.  Default: True.")
        private boolean history = true;
        @CmdOption(names = {"--validate"}, args = {"<value>"}, description = "Should the header of the input files be validate against the dictionary.  Default: True.")
        protected boolean validateFiles = true;
        @CmdOption(names = {"-v", "--version", "version"}, description = "Display versionInfo.", isHelp = true)
        private boolean version = false;
    }


    interface CommandRun {
        void run(GenericOptions genericOpts);
    }


    @CmdCommand(names = {"insert"}, description = "Inserts the given the gor files into the table.  All options apply to all the files.")
    private static class CommandInsert extends CommandBucketize implements CommandRun {
        @CmdOption(args = {"[<files> ...]"}, description = "Files to insert, absolute path or relative to the table dir. Values are specified as comma separated list.", minCount = 0, maxCount = -1)
        // Files to insert.
        private List<String> files = new ArrayList<>();

        @CmdOption(names = {"--tags", "-t"}, args = {"<list>"}, description = "Specify tags to use.  Values are specified as comma separated list.")
        private List<String> tags = new ArrayList<>();
        @CmdOption(names = {"--alias", "-a"}, args = {"<value>"}, description = "Aliases to use.")
        private String alias = "";
        @CmdOption(names = {"--range", "-r", "-p"}, args = {"<value>"}, description = "Specify range to use.  Value is specified as <chrom start>[:<poststart>][-[<chrom stop>:][<pos stop>]].")
        private String range = null;
        @CmdOption(names = {"--source", "-s"}, args = {"<value>"}, description = "Column used for tag filtering.")
        private String source = "PN";
        @CmdOption(names = {"--tagskey"}, description = "All tags must be unique. The usage of the same tag twice is disallowed. If used on a dictionary with multiple files using the same tag this option will result in an error.")
        protected boolean tagskey = false;


        public void run(GenericOptions genericOpts) {
            TableManager tm = TableManager.newBuilder()
                    .minBucketSize(minBucketSize).bucketSize(bucketSize)
                    .lockTimeout(Duration.ofSeconds(genericOpts.lockTimeout)).build();

            DictionaryTable table = tm.initTable(genericOpts.table);
            try (TableTransaction trans = TableTransaction.openWriteTransaction(tm.getLockType(), table, table.getName(), tm.getLockTimeout())) {
                if (source != null && !source.equals(table.getProperty(DictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY))) {
                    table.setProperty(DictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY, source);
                }

                if (tagskey) {
                    table.setUniqueTags(tagskey);
                }
                table.insert(this.files.stream()
                        .map(f -> PathUtils.relativize(table.getRootPath(), f))
                        .map(p -> new DictionaryEntry.Builder<>(p, table.getRootPath())
                                .range(GenomicRange.parseGenomicRange(this.range))
                                .alias(alias)
                                .tags(this.tags)
                                .build()).toArray(DictionaryEntry[]::new));
                trans.commit();
            }
        }
    }

    @CmdCommand(names = {"multiinsert"}, description = "Inserts the given the gor files into the table.")
    private static class CommandMultiInsert extends CommandBucketize implements CommandRun {
        @CmdOption(args = {"[<files> ...]"}, description = "Files to insert, absolute path or relative to the table dir. Values are specified as comma separated list.", minCount = 0, maxCount = -1)
        // Files to insert.
        private List<String> files = new ArrayList<>();

        @CmdOption(names = {"--tags", "-t"}, args = {"<list>"}, description = "Specify tags to use.  Values are specified as comma separated list, where length of the list must match the number of files and element i applies to the ith file.  Empty values are allowed.")
        private List<String> tags = new ArrayList<>();
        @CmdOption(names = {"--aliases", "-a"}, args = {"<list>"}, description = "Aliases to use.  Values are specified as comma separated list, where length of the list must match the number of files and element i applies to the ith file.  Empty values are allowed.")
        private List<String> aliases = new ArrayList<>();
        @CmdOption(names = {"--ranges", "-r", "-p"}, args = {"<list>"}, description = "Specify range to use.  Values are specified as comma separated list, where length of the list must match the number of files and element i applies to the ith file.  Empty values are allowed.  Each values is specified as <chrom start>[:<poststart>][-[<chrom stop>:][<pos stop>]].")
        private List<String> ranges = new ArrayList<>();
        @CmdOption(names = {"--source", "-s"}, args = {"<value>"}, description = "Column used for tag filtering.")
        private String source = "PN";

        public void run(GenericOptions genericOpts) {

            // Validate input.

            if (tags.size() != 0 && tags.size() != files.size()) {
                throw new RuntimeException("Length of tag list must be the same as number of files (" + files.size() + ")");
            }
            if (aliases.size() != 0 && aliases.size() != files.size()) {
                throw new RuntimeException("Length of alias list must be the same as number of files (" + files.size() + ")");
            }
            if (ranges.size() != 0 && ranges.size() != files.size()) {
                throw new RuntimeException("Length of range list must be the same as number of files (" + files.size() + ")");
            }

            // Fill in for missing input.

            tags = tags.size() != 0 ? tags : Arrays.asList(new String[files.size()]);
            aliases = aliases.size() != 0 ? aliases : Arrays.asList(new String[files.size()]);
            ranges = ranges.size() != 0 ? ranges : Arrays.asList(new String[files.size()]);

            // Run

            TableManager tm = TableManager.newBuilder()
                    .minBucketSize(minBucketSize)
                    .bucketSize(bucketSize)
                    .lockTimeout(Duration.ofSeconds(genericOpts.lockTimeout)).build();

            DictionaryTable table = tm.initTable(genericOpts.table);

            try (TableTransaction trans = TableTransaction.openWriteTransaction(tm.getLockType(), table, table.getName(), tm.getLockTimeout())) {
                if (source != null && !source.equals(table.getProperty(DictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY))) {
                    table.setProperty(DictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY, source);
                }

                table.insert(IntStream.range(0, files.size())
                        .mapToObj(i -> new DictionaryEntry.Builder<>(PathUtils.relativize(table.getRootPath(), files.get(i)), table.getRootPath())
                                .range(GenomicRange.parseGenomicRange(ranges.get(i)))
                                .alias(aliases.get(i))
                                .tags(new String[]{tags.get(i)})
                                .build()).toArray(DictionaryEntry[]::new));
                trans.commit();
            }
        }
    }

    @CmdCommand(names = {"delete"}, description = "Deletes matching entries.")
    private static class CommandDelete extends SelectionArgs implements CommandRun {

        @CmdOption(args = {"[<files> ...]"}, description = "List of files to delete, given as absolute path or relative to the table dir.  Values are specified as comma separated list.  Alternative to using -f.", minCount = 0, maxCount = -1)
        // Files to delete, alternative to -f
        private List<String> argsFiles = new ArrayList<>();

        public void run(GenericOptions genericOpts) {
            // We support taking files both as -f option and generic arguments, simply combine those two before running.
            TableManager tm = TableManager.newBuilder().lockTimeout(Duration.ofSeconds(genericOpts.lockTimeout)).build();
            final TableFilter lines = getBucketableTableEntries(genericOpts, this, tm);
            DictionaryTable table = tm.initTable(genericOpts.table);
            tm.delete(genericOpts.table, lines);
        }
    }

    @CmdCommand(names = {"bucketize"}, description = "Bucketize the table")
    private static class CommandBucketize implements CommandRun {

        @CmdOption(names = {"-w", "--workers"}, args = {"<value>"}, description = "Number of workers/threads to use.  Default: " + BucketCreatorGorPipe.DEFAULT_NUMBER_WORKERS)
        protected int workers = BucketCreatorGorPipe.DEFAULT_NUMBER_WORKERS;
        @CmdOption(names = {"--min_bucket_size"}, args = {"<value>"}, description = "Minimum number of files in a bucket.  Can never be larger than the bucket size.  Default: " + BucketManager.DEFAULT_MIN_BUCKET_SIZE)
        protected int minBucketSize = BucketManager.DEFAULT_MIN_BUCKET_SIZE;
        @CmdOption(names = {"--bucket_size"}, args = {"<value>"}, description = "Preferred number of files in a bucket (effective maximum).  Default: " + BucketManager.DEFAULT_BUCKET_SIZE)
        protected int bucketSize = BucketManager.DEFAULT_BUCKET_SIZE;
        @CmdOption(names = {"-c", "--pack_level"}, args = {"<value>"}, description = "Should we pack/compress the buckets.  NO_PACKING = No packing.  CONSOLIDATE = Merge small buckets into larger ones as needed.  FULL_PACKING = Full packing (rebucketize all small buckets and rebucketize partially deleted buckets).  Default: CONSOLIDATE")
        protected BucketManager.BucketPackLevel bucketPackLevel = BucketManager.DEFAULT_BUCKET_PACK_LEVEL;
        @CmdOption(names = {"-d", "--bucket_dirs"}, args = {"<list>"}, description = "Directories to put the bucket files in, either absolute path or relative to the table dir.  " +
                "The directories must exists and be writable.  Values are specified as comma separated list.  " +
                "Dafault: .<table name>.buckets",
                minCount = 0, maxCount = -1)
        protected List<String> bucketDirs = new ArrayList<>();
        @CmdOption(names = {"--max_bucket_count"}, args = {"<value>"}, description = "Maximum number of buckets created in this call to bucketize.  No limit if less than 0. Default: " + BucketManager.DEFAULT_MAX_BUCKET_COUNT)
        protected int maxBucketCount = BucketManager.DEFAULT_MAX_BUCKET_COUNT;

        public void run(GenericOptions genericOpts) {
            log.trace("Calling command bucketize");
            TableManager tm = TableManager.newBuilder().minBucketSize(this.minBucketSize).bucketSize(this.bucketSize)
                    .lockTimeout(Duration.ofSeconds(genericOpts.lockTimeout))
                    .build();
            tm.bucketize(genericOpts.table, this.bucketPackLevel, this.workers, this.maxBucketCount, bucketDirs.stream().collect(Collectors.toList()));
        }
    }

    @CmdCommand(names = {"delete_bucket"}, description = "Delete the given bucket")
    private static class CommandDeleteBucket implements CommandRun {

        @CmdOption(args = {"[<buckets> ...]"}, description = "Buckets to delete, absolute path or relative to the table dir.  Values are specified as comma separated list.", minCount = 0, maxCount = -1)
        private List<String> argsBuckets = new ArrayList<>();

        public void run(GenericOptions genericOpts) {
            TableManager tm = TableManager.newBuilder().lockTimeout(Duration.ofSeconds(genericOpts.lockTimeout)).build();
            tm.deleteBuckets(genericOpts.table, argsBuckets.stream().toArray(String[]::new));
        }
    }

    @CmdCommand(names = {"select"}, description = "Looks up entries in a table or dictionary.")
    private static class CommandSelect extends SelectionArgs implements CommandRun {

        @CmdOption(args = {"[<files> ...]"}, description = "List of files to select, given as absolute path or relative to the table dir.  Values are specified as comma separated list.  Alternative to using -f."
                , minCount = 0, maxCount = -1)  // Files to select, alternative to -f
        private List<String> argsFiles = new ArrayList<>();

        public void run(GenericOptions genericOpts) {
            TableManager tm = TableManager.newBuilder().lockTimeout(Duration.ofSeconds(genericOpts.lockTimeout)).build();
            final TableFilter lines = getBucketableTableEntries(genericOpts, this, tm);
            tm.print(lines);
        }
    }

    @CmdCommand(names = {"test"}, description = "Test command")
    private static class CommandTest implements CommandRun {

        @CmdOption(args = {"subcommand [<subcommand args> ...]"}, description = "Available subcommands: \n    readlock <lockname> <period>\n    writelock <lockname> <period>\n    islock <lockname>\n    lockInfo <lockName>"
                , minCount = 0, maxCount = -1)
        private List<String> subArgs = new ArrayList<>();

        public void run(GenericOptions genericOpts) {
            try {

                String subCommand = subArgs.get(0);
                String name = subArgs.get(1);
                long period;

                Duration lockTimeout = Duration.ofSeconds(genericOpts.lockTimeout);
                TableManager tm = TableManager.newBuilder().lockTimeout(lockTimeout).build();
                DictionaryTable table = tm.initTable(genericOpts.table);


                switch (subCommand.toLowerCase()) {

                    case "readlock":
                        period = Long.parseLong(subArgs.get(2));

                        try (TableLock lock = TableLock.acquireRead(tm.getLockType(), table, name, tm.getLockTimeout())) {
                            Thread.sleep(period);
                        }

                        break;
                    case "writelock":
                        period = Long.parseLong(subArgs.get(2));

                        try (TableLock lock = TableLock.acquireWrite(tm.getLockType(), table, name, tm.getLockTimeout())) {
                            Thread.sleep(period);
                        }

                        break;

                    case "islock":
                        try (TableLock lock = TableLock.acquireWrite(tm.getLockType(), table, name, Duration.ZERO)) {
                            System.out.println(lock.isValid() ? "Unlocked" : "Locked " + lock.reservedTo());
                        }

                        break;

                    case "lockinfo":
                        System.out.println(tm.getLockType().getSimpleName() + ":" + tm.getLockTimeout());

                        break;

                    default:
                        throw new IllegalArgumentException("Unknown command " + subCommand);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Generic selection arguments, applies to commands that take list of entries as input.
     */
    private static class SelectionArgs {
        @CmdOption(names = {"--files", "-f"}, args = {"<list>"}, description = "Files to filter by.  Values are specified as comma separated list.  Files are specified absolute or relative to the table dir.  Values are specified as comma separated list.", minCount = 0, maxCount = -1)
        protected List<String> files = new ArrayList<>();
        @CmdOption(names = {"--tags", "-t"}, args = {"<list>"}, description = "Tags to filter by.  Values are specified as comma separated list.", minCount = 0, maxCount = -1)
        protected List<String> tags = new ArrayList<>();
        @CmdOption(names = {"--buckets", "-b"}, args = {"<list>"}, description = "Buckets to filter by.  Values are specified as comma separated list.", minCount = 0, maxCount = -1)
        protected List<String> buckets = new ArrayList<>();
        @CmdOption(names = {"--aliases", "-a"}, args = {"<list>"}, description = "Aliases to filter by.  Values are specified as comma separated list.", minCount = 0, maxCount = -1)
        protected List<String> aliases = new ArrayList<>();
        @CmdOption(names = {"--range", "-r", "-p"}, args = {"<range>"}, description = "Range to filter by.  Value is specified as <chrom start>[:<poststart>][-[<chrom stop>:][<pos stop>]].  " +
                "Note:  When seleting table lines based on ranges they always have to match exactly.")
        protected String range = null;
        @CmdOption(names = {"--include_deleted"}, description = "Should deleted files be included.  Mostly for debugging purposes.")
        protected boolean includeDeleted = false;
    }

    /**
     * * Add an one-arg option argument to a mutable collection of strings, the arg can be comma seprated list.
     */
    private static class ListAddToCollectionHandler extends AddToCollectionHandler implements CmdOptionHandler {

        public void applyParams(final Object config, final AccessibleObject element, final String[] args,
                                final String optionName) {
            try {
                final Field field = (Field) element;
                @SuppressWarnings("unchecked") final Collection<String> collection = (Collection<String>) field.get(config);
                collection.addAll(Arrays.stream(args[0].split("[,]")).map(String::trim).collect(Collectors.toList()));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void usage(CmdlineParser cp) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos)) {
            cp.usage(ps);
            System.out.print(new String(baos.toByteArray())
                    .replace("[parameter]", "<table>")
                    .replace("[command]", "<command>"));
        }

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
        System.out.println(baos.toString().replace("Usage: gormanager", "Usage: gormanager <table>"));
    }

    private static TableFilter getBucketableTableEntries(GenericOptions genericOpts, SelectionArgs args, TableManager tm) {
        String[] allFiles = ArrayUtils.addAll(args.files.toArray(new String[0]), args.files.toArray(new String[0]));
        DictionaryTable table = tm.initTable(genericOpts.table);

        return table.filter()
                .files(allFiles.length > 0 ? allFiles : null)
                .aliases(args.aliases.size() > 0 ? args.aliases.toArray(new String[0]) : null)
                .tags(args.tags.size() > 0 ? args.tags.toArray(new String[0]) : null)
                .buckets(args.buckets.size() > 0 ? args.buckets.toArray(new String[0]) : null)
                .chrRange(args.range)
                .includeDeleted(args.includeDeleted);
    }

}
