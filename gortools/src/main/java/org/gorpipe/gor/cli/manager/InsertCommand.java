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
import org.gorpipe.gor.table.BaseTable;
import org.gorpipe.gor.table.GenomicRange;
import org.gorpipe.gor.table.TableHeader;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import picocli.CommandLine;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.gorpipe.gor.table.PathUtils.relativize;

@CommandLine.Command(name = "insert",
        aliases = {"i"},
        description="Inserts the given the gor files into the table.  All options apply to all the files.",
        header="Inserts gor files into a dictionary/table.")
public class InsertCommand extends CommandBucketizeOptions implements Runnable{

    @CommandLine.Parameters(index = "1..*",
            arity="0..*",
            paramLabel = "FILE",
            description = "Files to insert into dictionary/table.")
    List<String> files = new ArrayList<>();

    @CommandLine.Option(names = {"-t", "--tags"},
            split=",",
            description = "Specify tags to use.  Values are specified as comma separated list.")
    private List<String> tags = new ArrayList<>();

    @CommandLine.Option(names = {"-a", "--alias"},
            description = "Aliases to use.")
    private String alias;

    @CommandLine.Option(names = {"-r", "-p", "--range"},
            description = "Specify range to use.  Value is specified as <chrom start>[:<poststart>][-[<chrom stop>:][<pos stop>]].")
    private String range;

    @CommandLine.Option(names = {"-s", "--source"},
            description = "Column used for tag filtering. Defaults to 'PN'")
    private String source = "PN";

    @CommandLine.Option(names = {"--tagskey"},
            description = "All tags must be unique. The usage of the same tag twice is disallowed. If used on a dictionary with multiple files using the same tag this option will result in an error.")
    private boolean tagskey;

    @Override
    public void run() {
        TableManager tm = TableManager.newBuilder()
                .useHistory(!nohistory).minBucketSize(minBucketSize).bucketSize(bucketSize).lockTimeout(Duration.ofSeconds(lockTimeout)).build();

        BaseTable table = tm.initTable(dictionaryFile.toPath());
        if (source != null && !source.equals(table.getProperty(TableHeader.HEADER_SOURCE_COLUMN_KEY))) {
            table.setProperty(TableHeader.HEADER_SOURCE_COLUMN_KEY, source);
        }

        if (alias != null && !this.tags.contains(alias)) {
            this.tags.add(alias);
        }

        if(tagskey) {
            table.setUniqueTags(tagskey);
        }
        tm.insert(dictionaryFile.toPath(), bucketPackLevel, workers, this.files.stream()
                .map(f -> relativize(table.getRootPath(), Paths.get(f)))
                .map(p -> new DictionaryEntry.Builder<>(p, table.getRootUri())
                        .range(GenomicRange.parseGenomicRange(this.range))
                        .tags(this.tags)
                        .build()).toArray(DictionaryEntry[]::new));
    }
}
