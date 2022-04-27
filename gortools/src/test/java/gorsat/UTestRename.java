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

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.file.Files;

public class UTestRename {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testSimpleRename() {
        String query = "gorrow chr1,1,1 | calc Foo_0001 0 | rename Foo_0001 Bar_CAR | top 0";
        String result = TestUtils.runGorPipe(query);

        Assert.assertTrue(result.contains("Bar_CAR"));
    }

    @Test
    public void testColumnNames() throws IOException {
        var tmpath = Files.createTempFile("tmp","tsv");
        try {
            var query = "norrows 1|calc pn 'pn1'|calc pn2 'pn2'|calc bucket 'bucket1'|hide rownum|select #2,bucket|rename #1 pn|write "+ tmpath.toAbsolutePath();
            TestUtils.runGorPipe(query);
            var result = Files.readString(tmpath);
            Assert.assertEquals("#pn\tbucket\npn2\tbucket1\n",result);
        } finally {
            Files.deleteIfExists(tmpath);
        }
    }

    @Test
    public void testSimpleRenameWhereColumnIsNotFound() {
        exception.expect(GorParsingException.class);
        String query = "gorrow chr1,1,1 | calc Foo_0001 0 | rename Foo_0002 Bar_CAR | top 0";
        TestUtils.runGorPipe(query);
    }

    @Test
    public void testRenameWithCaseSensitivity() {
        String query = "gorrow chr1,1,1 | calc Foo_0001 0 | calc Foo_0002 1 | calc Foo_0003 2 |  calc foo_0004 3 | rename Foo_(.*) Bar_#{1} | top 0";
        String result = TestUtils.runGorPipe(query);

        Assert.assertTrue(result.contains("Bar_0001"));
        Assert.assertTrue(result.contains("Bar_0002"));
        Assert.assertTrue(result.contains("Bar_0003"));
        Assert.assertTrue(result.contains("Bar_0004"));
    }

    @Test
    public void testRenameWithMultipleGroups() {
        String query = "gorrow chr1,1,1 | calc A_Foo_0001 0 | calc B_Foo_0002 1 | rename (.*)_Foo_(.*) Bar_#{2}_#{1} | top 0";
        String result = TestUtils.runGorPipe(query);

        Assert.assertTrue(result.contains("Bar_0001_A"));
        Assert.assertTrue(result.contains("Bar_0002_B"));
    }

    @Test
    public void renameWithOneGroup() {
        String query = "gorrow chr1,1 | calc A_Foo 0 | calc B_Foo 1 | rename (.*)_Foo_(.*) Bar_#{1} | top 0";
        String result = TestUtils.runGorPipe(query);

        Assert.assertEquals("chrom\tpos\tA_Foo\tB_Foo\n", result);
    }

    @Test
    public void renameWithTwoGroups() {
        String query = "gorrow chr1,1 | calc A_Foo_1 0 | calc B_Bar_2 1 | rename (.*)_Foo_(.*) Bar_#{1}_#{2} | top 0";
        String result = TestUtils.runGorPipe(query);

        Assert.assertEquals("chrom\tpos\tBar_A_1\tB_Bar_2\n", result);
    }

    @Test
    public void renameWithThreeGroups() {
        String query = "gorrow chr1,1 | calc A_Foo_1_x 0 | calc B_Bar_2_y 1 | rename (.*)_Foo_(.*)_(.*) Bar_#{1}_#{2}_#{3} | top 0";
        String result = TestUtils.runGorPipe(query);

        Assert.assertEquals("chrom\tpos\tBar_A_1_x\tB_Bar_2_y\n", result);
    }

    @Test
    public void renameWithFourGroups() {
        String query = "gorrow chr1,1 | calc A_Foo_1_x_a 0 | calc B_Bar_2_y 1 | rename (.*)_Foo_(.*)_(.*)_(.*) Bar_#{1}_#{2}_#{3}#{4} | top 0";
        String result = TestUtils.runGorPipe(query);

        Assert.assertEquals("chrom\tpos\tBar_A_1_xa\tB_Bar_2_y\n", result);
    }

    @Test
    public void renameWithFiveGroups() {
        String query = "gorrow chr1,1 | calc A_Foo_1_x_a_1 0 | calc B_Bar_2_y 1 | rename (.*)_Foo_(.*)_(.*)_(.*)_(.*) Bar_#{1}_#{2}_#{3}#{4}_#{5} | top 0";
        String result = TestUtils.runGorPipe(query);

        Assert.assertEquals("chrom\tpos\tBar_A_1_xa_1\tB_Bar_2_y\n", result);
    }

    @Test
    public void renameWithSixGroups() {
        String query = "gorrow chr1,1 | calc A_Foo_1_x_a_1_z 0 | calc B_Bar_2_y 1 | rename (.*)_Foo_(.*)_(.*)_(.*)_(.*)_(.*) Bar_#{1}_#{2}_#{3}#{4}_#{5}#{6} | top 0";
        String result = TestUtils.runGorPipe(query);

        Assert.assertEquals("chrom\tpos\tBar_A_1_xa_1z\tB_Bar_2_y\n", result);
    }

    @Test
    public void renameWithInvalidRegexBinding() {
        exception.expect(GorParsingException.class);
        String query = "gorrow chr1,1 | calc A_Foo_1 0 | calc B_Bar_2 1 | rename (.*)_Foo_(.*) Bar_#{1}_#{3} | top 0";
        String result = TestUtils.runGorPipe(query);
    }

    @Test
    public void renameStrictWithNoMatch() {
        exception.expect(GorParsingException.class);
        String query = "gorrow chr1,1 | calc A_Foo_1 0 | calc B_Bar_2 1 | rename -s C_Foo_(.*) Bar_#{1} | top 0";
        String result = TestUtils.runGorPipe(query);
    }
}
