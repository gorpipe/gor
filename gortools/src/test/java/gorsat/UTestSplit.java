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

import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class UTestSplit {
    @Test
    public void splitSingleColumn() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data \"1,2,3\" | split data");
        Assert.assertEquals(4, lines.length);
        Assert.assertEquals("chrom\tpos\tdata\n", lines[0]);
        Assert.assertEquals("chr1\t1\t1\n", lines[1]);
        Assert.assertEquals("chr1\t1\t2\n", lines[2]);
        Assert.assertEquals("chr1\t1\t3\n", lines[3]);
    }

    @Test
    public void splitSingleColumnInfersTypeAfterSplit() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data \"1,2,3\" | split data | calc sum 4+data");
        Assert.assertEquals(4, lines.length);
        Assert.assertEquals("chrom\tpos\tdata\tsum\n", lines[0]);
        Assert.assertEquals("chr1\t1\t1\t5\n", lines[1]);
        Assert.assertEquals("chr1\t1\t2\t6\n", lines[2]);
        Assert.assertEquals("chr1\t1\t3\t7\n", lines[3]);
    }

    @Test
    public void splitDualColumn() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data1 \"1,2,3\" | calc data2 \"a,b,c\" | split data1,data2");
        Assert.assertEquals(4, lines.length);
        Assert.assertEquals("chrom\tpos\tdata1\tdata2\n", lines[0]);
        Assert.assertEquals("chr1\t1\t1\ta\n", lines[1]);
        Assert.assertEquals("chr1\t1\t2\tb\n", lines[2]);
        Assert.assertEquals("chr1\t1\t3\tc\n", lines[3]);
    }

    @Test
    public void splitDualColumnInfersTypeAfterSplit() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data1 \"1,2,3\" | calc data2 \"a,b,c\" | split data1,data2 | calc sum 4+data1");
        Assert.assertEquals(4, lines.length);
        Assert.assertEquals("chrom\tpos\tdata1\tdata2\tsum\n", lines[0]);
        Assert.assertEquals("chr1\t1\t1\ta\t5\n", lines[1]);
        Assert.assertEquals("chr1\t1\t2\tb\t6\n", lines[2]);
        Assert.assertEquals("chr1\t1\t3\tc\t7\n", lines[3]);
    }

    @Test
    public void splitDualColumnWhenColumnsSizeDoesntMatch() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data1 \"1,2,3\" | calc data2 \"a,b,c,d,e,f\" | split data1,data2");
        Assert.assertEquals(7, lines.length);
        Assert.assertEquals("chrom\tpos\tdata1\tdata2\n", lines[0]);
        Assert.assertEquals("chr1\t1\t1\ta\n", lines[1]);
        Assert.assertEquals("chr1\t1\t2\tb\n", lines[2]);
        Assert.assertEquals("chr1\t1\t3\tc\n", lines[3]);
        Assert.assertEquals("chr1\t1\t\td\n", lines[4]);
        Assert.assertEquals("chr1\t1\t\te\n", lines[5]);
        Assert.assertEquals("chr1\t1\t\tf\n", lines[6]);
    }

    @Test
    public void icelandicChars() throws IOException {
        assertUtfCharactersInSplit("Þetta er próf - áéíóúýö");
    }

    @Test
    public void allKindsOfChars() throws IOException {
        assertUtfCharactersInSplit("Þetta er próf 이것은 시험이다. これはテストです 这是一个测试 Это тест");
    }

    private void assertUtfCharactersInSplit(String specialChars) throws IOException {
        String contents =
                "Chrom\tPos\tSplit\tData\n" +
                        "chr1\t1\tA,B,C\t" + specialChars + "\n";

        File directory = FileTestUtils.createTempDirectory("UTestSplit");
        File tempFile = FileTestUtils.createTempFile(directory, "test.gor", contents);

        String query = String.format("gor %s | split Split", tempFile.getAbsolutePath());
        String[] lines = TestUtils.runGorPipeLines(query);
        Assert.assertEquals(4, lines.length);
        Assert.assertEquals("chr1\t1\tA\t" + specialChars + "\n", lines[1]);
        Assert.assertEquals("chr1\t1\tB\t" + specialChars + "\n", lines[2]);
        Assert.assertEquals("chr1\t1\tC\t" + specialChars + "\n", lines[3]);
    }
}
