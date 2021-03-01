/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class UTestSelWhere {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void basic() throws IOException {
        String contents = "Chrom\tPos\tData\n" +
                "chr1\t1\tbingo1\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | selwhere colnum <= 2", file.getAbsoluteFile());
        final String result = TestUtils.runGorPipe(query);
        final String expected = "Chrom\tPos\n" +
                "chr1\t1\n";

        Assert.assertEquals(expected, result);
    }

    @Test
    public void basicUsingColname() throws IOException {
        String contents = "Chrom\tPos\tData\n" +
                "chr1\t1\tbingo1\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | selwhere colname='Chrom' or colname='Pos'", file.getAbsoluteFile());
        final String result = TestUtils.runGorPipe(query);
        final String expected = "Chrom\tPos\n" +
                "chr1\t1\n";

        Assert.assertEquals(expected, result);
    }

    @Test
    public void checkprefix() throws IOException {
        String contents = "Chrom\tPos\tData\tx0\tx1\tx2\ty0\ty1\ty2\n" +
                "chr1\t1\tbingo1\tx0\tx1\tx2\ty0\ty1\ty2\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | selwhere colnum <= 2 or left(colname,1)='x'", file.getAbsoluteFile());
        final String result = TestUtils.runGorPipe(query);
        final String expected = "Chrom\tPos\tx0\tx1\tx2\n" +
                "chr1\t1\tx0\tx1\tx2\n";

        Assert.assertEquals(expected, result);
    }
}
