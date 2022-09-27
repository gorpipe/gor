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

import org.apache.commons.lang3.ArrayUtils;
import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.dictionary.BaseDictionaryTable;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import picocli.CommandLine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "select",
        aliases = {"s"},
        description="Looks up entries in a table or dictionary.",
        header="Look up entries in a table or dictionary.")
public class SelectCommand extends FilterOptions implements Runnable{

    @CommandLine.Parameters(index = "1..*",
            arity="0..*",
            paramLabel = "FILE",
            description = "List of files to delete, given as absolute path or relative to the table dir.  Values are specified as comma separated list.  Alternative to using -f.")
    private List<String> inputFiles = new ArrayList<>();

    @Override
    public void run() {
        String[] allFiles = (String[]) ArrayUtils.addAll(this.inputFiles.toArray(new String[0]), this.files.toArray(new String[0]));
        TableManager tm = TableManager.newBuilder().useHistory(!nohistory).lockTimeout(Duration.ofSeconds(lockTimeout)).build();
        BaseDictionaryTable table = tm.initTable(dictionaryFile.toPath());

        final List<? extends DictionaryEntry> lines = table.filter()
                .files(allFiles.length > 0 ? allFiles : null)
                .aliases(aliases.size() > 0 ? aliases.toArray(new String[0]) : null)
                .tags(tags.size() > 0 ? tags.toArray(new String[0]) : null)
                .buckets(this.buckets.size() > 0 ? this.buckets.toArray(new String[0]) : null)
                .chrRange(range)
                .includeDeleted(this.includeDeleted)
                .get();
        for (DictionaryEntry line : lines) {
            System.out.print(line.formatEntry());
        }
    }
}
