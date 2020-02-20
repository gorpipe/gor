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

package gorsat;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestFileHeaderCache {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void testHeaderCacheWithColumnCompressedGorz() throws IOException {
        Path tmpFile = workDir.newFile("dbsnp_colcompress.gorz").toPath();
        int count = 0;
        try {
            String colcompressFilePath = tmpFile.toAbsolutePath().normalize().toString();
            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gorz | write -c " + colcompressFilePath);
            String query = "gor "+colcompressFilePath;

            //Fill the cache
            count += TestUtils.runGorPipeCount(query);
            count += TestUtils.runGorPipeCount(query);
        } finally {
            if( Files.exists(tmpFile) ) Files.delete(tmpFile);
        }
        Assert.assertEquals(96, count);
    }
}
