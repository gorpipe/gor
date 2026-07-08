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

import org.gorpipe.test.SlowTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

@Category(SlowTests.class)
public class UTestTableServiceLoad {

    private long retainedDeltaForFileCount(int fileCount) throws Exception {
        Path root = Files.createTempDirectory("memtest-scale-" + fileCount);
        MemoryLoadConfig config = new MemoryLoadConfig(fileCount, 20, 100, 4, 4, 70, 3);
        long baseline = MemorySampler.measureRetainedHeapBytes();
        DictionaryFixture fixture = new DictionaryFixture(root);
        var tables = fixture.createTables(config);
        TableServiceLoadDriver driver = new TableServiceLoadDriver(tables, fixture, config);
        driver.run(); // pre-opens + retains all tables in driver.tableCache
        long retained = MemorySampler.measureRetainedHeapBytes();
        long delta = retained - baseline;
        // keep driver (and its retained tables) alive until AFTER measurement:
        assertTrue("driver retained", driver != null);
        return delta;
    }

    @Test
    public void retainedHeapScalesWithDictionarySize() throws Exception {
        long small = retainedDeltaForFileCount(500);
        long large = retainedDeltaForFileCount(8000);
        assertTrue("retained heap should grow with dictionary size: small=" + small + " large=" + large,
                large > small * 1.5);
    }
}
