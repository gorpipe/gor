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

import gorsat.process.CLIGorExecutionEngine;
import gorsat.process.PipeOptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.gorpipe.gor.table.BaseTable;
import org.gorpipe.gor.table.BucketableTableEntry;
import org.gorpipe.gor.table.TableEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.gorpipe.gor.table.PathUtils.resolve;

/**
 * Helper class to create bucket file from bucket description.
 */
public class BucketCreatorGorPipe<T extends BucketableTableEntry> implements BucketCreator<T>  {

    private static final Logger log = LoggerFactory.getLogger(BucketCreatorGorPipe.class);

    public static final int DEFAULT_NUMBER_WORKERS = 4;

    private int workers;

    public BucketCreatorGorPipe() {
        this(DEFAULT_NUMBER_WORKERS);
    }

    public BucketCreatorGorPipe(int workers) {
        this.workers = workers;
    }

    @Override
    public void createBuckets(BaseTable<T> table, Map<Path, List<T>> bucketsToCreate, Path absBucketDir)
            throws IOException {
        // Create common temp directories and folders.
        Path workTempDir = createTempfoldersForCreateBucketFiles(table, bucketsToCreate.keySet(), absBucketDir);

        // Build the gor query (gorpipe)
        String gorPipeCommand = createBucketizeGorCommand(bucketsToCreate, workTempDir, table);
        String[] args = new String[]{
                gorPipeCommand,
                "-cachedir", workTempDir.resolve("cache").toString(),
                "-workers", String.valueOf(workers)};
        log.trace("Calling bucketize with command args: {} \"{}\" {} {} {} {}", (Object[]) args);

        PrintStream oldOut = System.out;

        PipeOptions options = new PipeOptions();
        options.parseOptions(args);
        CLIGorExecutionEngine engine = new CLIGorExecutionEngine(options, null, table.getSecurityContext());

        try (PrintStream newPrintStream = new PrintStream(new NullOutputStream())){
            System.setOut(newPrintStream);
            engine.execute();
        } catch (Exception e) {
            log.error("Calling bucketize failed.  Command args: {} \"{}\" {} {} {} {} failed", (Object[]) args);
            throw e;
        } finally {

            System.setOut(oldOut);
        }

        // Move the bucket files from temp to the bucket folder
        for (Path bucket : bucketsToCreate.keySet()) {
            // Move the bucket files.
            Path targetBucketPath = resolve(table.getRootPath(), bucket);
            Files.move(workTempDir.resolve(bucket), targetBucketPath);
        }

        deleteIfTempBucketizingFolder(workTempDir, table);
    }

    private String createBucketizeGorCommand(Map<Path, List<T>> bucketsToCreate, Path tempRootDir, BaseTable<T> table) {
        // NOTE:  Can not use pgor with the write command !! Will only get one chromosome.
        // Tag based, does not work if we are adding more files with same alias, why not?.
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Path, List<T>> b2c : bucketsToCreate.entrySet()) {
            Path bucket = b2c.getKey();
            String tags = b2c.getValue().stream()
                    .map(TableEntry::getAliasTag)
                    .distinct()
                    .collect(Collectors.joining(","));
            if (tags.length() > 0) {
                sb.append(String.format("create #%s# = gor %s -s %s -f %s %s | log 1000000 | write -c %s;%n",
                        bucket, table.getPath(), table.getTagColumn(), tags,
                        table.getSecurityContext() != null ? table.getSecurityContext() : "",
                        tempRootDir.resolve(bucket.toString())));
            }
        }

        // Must add no-op gor command as the create commands can not be run on their own.
        sb.append("gor 1.mem| top 1\n");
        return sb.toString();
    }

    static void deleteIfTempBucketizingFolder(Path path, BaseTable<? extends BucketableTableEntry> table)
            throws IOException {
        if (path.getFileName().toString().startsWith(getBucketizingFolderPrefix(table))) {
            log.debug("Deleting temp folder: {}", path);
            FileUtils.deleteDirectory(path.toFile());
        }
    }

    static String getBucketizingFolderPrefix(BaseTable<? extends BucketableTableEntry> table) {
        return "bucketizing_" + table.getId();
    }

    /**
     * Create and validate folders, both temp and final.
     *
     * @param table     table we are working with.
     * @param buckets   buckets we are creating.
     * @param workBaseDir path to the work base dir.
     * @return the temp folder created.
     * @throws IOException if there is an error creating the temp folder.
     */
    private Path createTempfoldersForCreateBucketFiles(BaseTable<T> table, Set<Path> buckets, Path workBaseDir)
            throws IOException {
        // Create temp root.
        Path tempRootDir = Files.createDirectory(workBaseDir.resolve(getBucketizingFolderPrefix(table)));

        log.trace("Created temp folder {}", tempRootDir);
        tempRootDir.toFile().deleteOnExit();

        // Validate bucket dirs and create temp folders for the created buckets.  After the bucketization
        // we will copy the buckets from temp dirs to the final dir.
        for (Path bucketDir : buckets.stream().map(Path::getParent).distinct().collect(Collectors.toList())) {
            // Create temp folders.
            Path bucketsRelativePath = bucketDir.isAbsolute() ? workBaseDir.relativize(bucketDir) : bucketDir;
            if (bucketsRelativePath.toString().length() > 0) {
                Files.createDirectories(tempRootDir.resolve(bucketsRelativePath));
            }
        }

        // Cache dir
        Files.createDirectory(tempRootDir.resolve("cache"));

        return tempRootDir;
    }

}
