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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by sigmar on 02/11/2016.
 */
public class UTestDistinct {
    private File distinctGor;
    private File testFileGor;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        distinctGor = FileTestUtils.createTempFile(workDir.getRoot(), "distinct.gor",
                "Chrom\tPos\tsome\tnull\tfloat\tnext\n" +
                        "chr1\t1\ta\t0.0\t1.10101\t1.2222\n" +
                        "chr1\t1\ta\t0.0\t1.10101\t1.2222\n"
        );

        testFileGor = FileTestUtils.createTempFile(workDir.newFolder("testDistinct").getCanonicalFile(), "testFile.gor",
                createTestFileGor()
        );
    }

    private static String createTestFileGor() {
        StringBuilder sb = new StringBuilder();
        sb.append("CHROM\tPOS\tnumbers\n");
        for (int i = 0; i < 1000; i++) {
            sb.append("chr1\t0\t" + (int) (10 * Math.random()) + "\n");
        }
        return sb.toString();
    }

    @Test
    public void testDistinct() throws IOException {
        String query = "gor " + distinctGor.getCanonicalPath() + " | distinct";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(1, count);
    }

    @Test
    public void testDistinctWithHide() throws IOException {
        String query = "gor " + distinctGor.getCanonicalPath() + " | hide some,float | distinct";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(1, count);
    }

    @Test
    public void testDistinctOnNor() throws IOException {
        final String result = TestUtils.runGorPipe("nor " + testFileGor.getCanonicalPath() + " | select numbers | sort -c numbers | distinct");
        final String[] resultVals = Arrays.stream(result.split("\n")).skip(1).map(line -> line.substring(line.lastIndexOf('\t') + 1, line.length())).toArray(String[]::new);
        final String[] assumedVals = Arrays.stream(result.split("\n")).skip(1).map(line -> line.substring(line.lastIndexOf('\t') + 1, line.length())).distinct().sorted().toArray(String[]::new);
        assert resultVals.length == assumedVals.length;
        final int len = resultVals.length;
        for (int i = 0; i < len; ++i) assert resultVals[i].equals(assumedVals[i]);
    }
}
