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
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryFilter;
import picocli.CommandLine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "delete",
        aliases = {"d"},
        description="Deletes matching entries.",
        header="Delete entries from a dictionary/table.")
public class DeleteCommand extends FilterOptions implements Runnable{

    @CommandLine.Parameters(index = "1..*",
            arity="0..*",
            paramLabel = "FILE",
            description = "List of files to delete, given as absolute path or relative to the table dir.  Values are specified as comma separated list.  Alternative to using -f.")
    private List<String> inputFiles = new ArrayList<>();

    @Override
    public void run() {
        // We support taking files both as -f option and generic arguments, simply combine those two before running.
        TableManager tm = TableManager.newBuilder().lockTimeout(Duration.ofSeconds(lockTimeout)).build();

        String[] allFiles = (String[]) ArrayUtils.addAll(this.inputFiles.toArray(new String[0]), this.files.toArray(new String[0]));
        DictionaryTable table = tm.initTable(dictionaryFile.toString());
        var filter = table.filter()
                .files(allFiles.length > 0 ? allFiles : null)
                .aliases(aliases.size() > 0 ? aliases.toArray(new String[0]) : null)
                .tags(tags.size() > 0 ? tags.toArray(new String[0]) : null)
                .buckets(this.buckets.size() > 0 ? this.buckets.toArray(new String[0]) : null)
                .includeDeleted(this.includeDeleted);
        if (filter instanceof GorDictionaryFilter) {
            ((GorDictionaryFilter) filter).chrRange(range);
        }

        tm.delete(dictionaryFile.toString(), filter);
    }
}
