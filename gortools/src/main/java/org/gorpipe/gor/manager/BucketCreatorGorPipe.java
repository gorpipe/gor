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
import org.gorpipe.gor.table.dictionary.BaseDictionaryTable;
import org.gorpipe.gor.table.dictionary.BucketableTableEntry;
import org.gorpipe.gor.table.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class to create bucket file from bucket description.
 */
public class BucketCreatorGorPipe<T extends BucketableTableEntry> implements BucketCreator<T>  {

    private static final Logger log = LoggerFactory.getLogger(BucketCreatorGorPipe.class);

    public static final int DEFAULT_NUMBER_WORKERS = 4;

    private final int workers;

    public BucketCreatorGorPipe() {
        this(DEFAULT_NUMBER_WORKERS);
    }

    public BucketCreatorGorPipe(int workers) {
        this.workers = workers;
    }

    @Override
    public void createBuckets(BaseDictionaryTable<T> table, Map<String, List<T>> bucketsToCreate, URI absBucketDir)
            throws IOException {

        // Build the gor query (gorpipe)
        String gorPipeCommand = createBucketizeGorCommand(bucketsToCreate, table.getRootUri(), table);
        String[] args = new String[]{
                gorPipeCommand,
                "-workers", String.valueOf(workers),
                "-gorroot", table.getFileReader().getCommonRoot()};
        log.trace("Calling bucketize with command args: {} \"{}\" {} {} {} {}", args);

        PrintStream oldOut = System.out;

        PipeOptions options = new PipeOptions();
        options.parseOptions(args);
        CLIGorExecutionEngine engine = new CLIGorExecutionEngine(options, null, table.getSecurityContext());

        try (PrintStream newPrintStream = new PrintStream(new NullOutputStream())){
            System.setOut(newPrintStream);
            engine.execute();
        } catch (Exception e) {
            log.error("Calling bucketize failed.  Command args: {} \"{}\" {} {} {} {} failed", args);
            throw e;
        } finally {

            System.setOut(oldOut);
        }
    }

    private String createBucketizeGorCommand(Map<String, List<T>> bucketsToCreate, URI rootUri, BaseDictionaryTable<T> table) {
        // NOTE:  Can not use pgor with the write command !! Will only get one chromosome.
        // Tag based, does not work if we are adding more files with same alias, why not?.
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<T>> b2c : bucketsToCreate.entrySet()) {
            String bucket = b2c.getKey();
            String tags = b2c.getValue().stream()
                    .flatMap(e -> Arrays.stream(e.getFilterTags()).distinct())
                    .distinct()
                    .collect(Collectors.joining(","));
            if (tags.length() > 0) {
                sb.append(String.format("create #%s# = gor %s -s %s -f %s %s | write -c %s;%n",
                        bucket, table.getPath(), table.getSourceColumn(), tags,
                        table.getSecurityContext() != null ? table.getSecurityContext() : "",
                        rootUri.resolve(bucket)));
            }
        }

        // Must add no-op gor command as the create commands can not be run on their own.
        sb.append("gor 1.mem| top 1\n");
        return sb.toString();
    }
}
