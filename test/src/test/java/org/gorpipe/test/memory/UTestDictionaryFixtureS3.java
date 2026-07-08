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

import org.junit.Assume;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class UTestDictionaryFixtureS3 {

    @Test
    public void createsTablesOnS3WhenBucketConfigured() throws Exception {
        String s3Root = System.getenv("GOR_MEMTEST_S3_ROOT"); // e.g. s3://my-dev-bucket/memtest
        Assume.assumeTrue("GOR_MEMTEST_S3_ROOT not set; skipping S3 fixture test", s3Root != null);

        Path localRoot = Files.createTempDirectory("memtest-s3-local");
        MemoryLoadConfig config = new MemoryLoadConfig(20, 10, 10, 1, 2, 50, 2);
        DictionaryFixture fixture = new DictionaryFixture(localRoot, s3Root);

        List<Path> tables = fixture.createTablesOnS3(config);

        assertTrue("created at least one S3 table", tables.size() >= 1);
    }
}
