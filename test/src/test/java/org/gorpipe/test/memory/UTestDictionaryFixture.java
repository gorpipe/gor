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

import org.gorpipe.gor.table.dictionary.gor.GorDictionaryEntry;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UTestDictionaryFixture {

    @Test
    public void createsReadableTablesAtScale() throws Exception {
        Path root = Files.createTempDirectory("memtest-fixture");
        DictionaryFixture fixture = new DictionaryFixture(root);
        MemoryLoadConfig config = new MemoryLoadConfig(50, 10, 10, 3, 4, 70, 5);

        List<Path> tables = fixture.createTables(config);

        assertEquals(3, tables.size());
        for (Path gord : tables) {
            assertTrue("gord file exists", Files.exists(gord));
            GorDictionaryTable table = fixture.openTable(gord);
            List<? extends GorDictionaryEntry> all = table.filter().get();
            assertTrue("dictionary has entries", all.size() > 0);
        }
    }
}
