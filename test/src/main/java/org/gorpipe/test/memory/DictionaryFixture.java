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

package org.gorpipe.test.memory;

import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.gorpipe.test.GorDictionarySetup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds Gor dictionaries at prod-like scale for the memory harness.
 * Wraps {@link GorDictionarySetup} to generate data files + a .gord dictionary per table.
 */
public class DictionaryFixture {
    private final Path root;

    public DictionaryFixture(Path root) {
        this.root = root;
    }

    public List<Path> createTables(MemoryLoadConfig config) throws IOException {
        List<Path> tables = new ArrayList<>();
        int[] chrs = {1, 2, 3};
        for (int t = 0; t < config.tableCount; t++) {
            String name = "memtest_table_" + t;
            String[] sources = new String[config.fileCount];
            for (int i = 0; i < config.fileCount; i++) sources[i] = "PN" + i;
            Map<String, List<String>> data = GorDictionarySetup.createDataFilesMap(
                    name, root, config.fileCount, chrs, config.rowsPerChr, "PN", true, sources);

            Path gord = root.resolve(name + ".gord");
            GorDictionaryTable table = new GorDictionaryTable.Builder<>(gord).build();
            table.insert(data);
            table.save();
            tables.add(gord);
        }
        return tables;
    }

    public GorDictionaryTable openTable(Path gordPath) {
        return new GorDictionaryTable.Builder<>(gordPath).build();
    }
}
