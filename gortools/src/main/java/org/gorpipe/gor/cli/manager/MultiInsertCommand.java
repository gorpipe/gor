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

package org.gorpipe.gor.cli.manager;

import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.dictionary.DictionaryTableMeta;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTableMeta;
import org.gorpipe.gor.table.lock.TableTransaction;
import org.gorpipe.gor.table.util.GenomicRange;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryEntry;
import org.gorpipe.gor.table.util.PathUtils;
import picocli.CommandLine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.gorpipe.gor.table.util.PathUtils.relativize;

@CommandLine.Command(name = "multiinsert",
        aliases = {"m"},
        description="Inserts multiple sets of gor files into the dictionary/table.  All options apply to all the file sets.",
        header="Inserts multiple gor files into a dictionary/table.")
public class MultiInsertCommand extends CommandBucketizeOptions implements Runnable{

    @CommandLine.Parameters(index = "1..*",
            arity="0..*",
            paramLabel = "FILE",
            description = "Files to insert, absolute path or relative to the table dir. Values are specified as comma separated list.")
    List<String> files = new ArrayList<>();

    @CommandLine.Option(names = {"-t", "--tags"},
            split=",",
            description = "Specify tags to use.  Values are specified as comma separated list, where length of the list must match the number of files and element i applies to the ith file.  Empty values are allowed.")
    private List<String> tags = new ArrayList<>();

    @CommandLine.Option(names = {"-a", "--aliases"},
            split=",",
            description = "Aliases to use.  Values are specified as comma separated list, where length of the list must match the number of files and element i applies to the ith file.  Empty values are allowed.")
    private List<String> aliases = new ArrayList<>();

    @CommandLine.Option(names = {"-r", "-p", "--ranges"},
            split=",",
            description = "Specify range to use.  Values are specified as comma separated list, where length of the list must match the number of files and element i applies to the ith file.  Empty values are allowed.  Each values is specified as <chrom start>[:<poststart>][-[<chrom stop>:][<pos stop>]].")
    private List<String> ranges = new ArrayList<>();

    @CommandLine.Option(names = {"-s", "--source"},
            description = "Column used for tag filtering. Defaults to 'PN'")
    private String source = "PN";

    @Override
    public void run() {
        // Validate input.

        if (tags.size() != 0 && tags.size() != files.size()) {
            throw new IllegalArgumentException("Length of tag list must be the same as number of files (" + files.size() + ")");
        }
        if (aliases.size() != 0 && aliases.size() != files.size()) {
            throw new IllegalArgumentException("Length of alias list must be the same as number of files (" + files.size() + ")");
        }
        if (ranges.size() != 0 && ranges.size() != files.size()) {
            throw new IllegalArgumentException("Length of range list must be the same as number of files (" + files.size() + ")");
        }

        // Fill in for missing input.

        tags = tags.size() != 0 ? tags : Arrays.asList(new String[files.size()]);
        aliases = aliases.size() != 0 ? aliases : Arrays.asList(new String[files.size()]);
        ranges = ranges.size() != 0 ? ranges : Arrays.asList(new String[files.size()]);

        // Run

        TableManager tm = TableManager.newBuilder()
                .minBucketSize(minBucketSize).bucketSize(bucketSize).lockTimeout(Duration.ofSeconds(lockTimeout)).build();

        DictionaryTable table = tm.initTable(dictionaryFile.toString());
        try (TableTransaction trans = TableTransaction.openWriteTransaction(tm.getLockType(), table, table.getName(), tm.getLockTimeout())) {
            if (source != null && !source.equals(table.getProperty(DictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY))) {
                table.setProperty(GorDictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY, source);
            }

            table.insert(IntStream.range(0, files.size())
                    .mapToObj(i -> new GorDictionaryEntry.Builder<>(PathUtils.relativize(table.getRootPath(), files.get(i)), table.getRootPath())
                            .range(GenomicRange.parseGenomicRange(ranges.get(i)))
                            .alias(aliases.get(i))
                            .tags(new String[]{tags.get(i)})
                            .build()).toArray(GorDictionaryEntry[]::new));
            trans.commit();
        }
    }
}
