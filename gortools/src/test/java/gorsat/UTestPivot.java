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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class UTestPivot {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void pivotSinglePos() {
        final String result = TestUtils.runGorPipe("gorrows -p chr1:1-4 | calc data pos | calc x 42 | pivot data -v 1,2,3");
        final String expected = "chrom\tpos\t1_x\t2_x\t3_x\n" +
                "chr1\t1\t42\t?\t?\n" +
                "chr1\t2\t?\t42\t?\n" +
                "chr1\t3\t?\t?\t42\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void pivotSinglePosCustomEmpty() {
        final String result = TestUtils.runGorPipe("gorrows -p chr1:1-4 | calc data pos | calc x 42 | pivot data -v 1,2,3, -e 'empty'");
        final String expected = "chrom\tpos\t1_x\t2_x\t3_x\n" +
                "chr1\t1\t42\tempty\tempty\n" +
                "chr1\t2\tempty\t42\tempty\n" +
                "chr1\t3\tempty\tempty\t42\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void pivotMultiplePos() throws IOException {
        String contents = "Chrom\tPos\tCategory\tValue\n" +
                "chr1\t1\t1\t1\n" +
                "chr1\t1\t2\t11\n" +
                "chr1\t1\t3\t111\n" +
                "chr1\t2\t1\t2\n" +
                "chr1\t2\t2\t22\n" +
                "chr1\t2\t3\t222";

        final File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | pivot Category -v 1,2,3", tempFile.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);

        final String expected = "Chrom\tPos\t1_Value\t2_Value\t3_Value\n" +
                "chr1\t1\t1\t11\t111\n" +
                "chr1\t2\t2\t22\t222\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void pivotMultiplePosWithGroupingColumn() throws IOException {
        String contents = "Chrom\tPos\tGrp\tCategory\tValue\n" +
                "chr1\t1\tA\t1\tA\n" +
                "chr1\t1\tA\t2\tAA\n" +
                "chr1\t1\tA\t3\tAAA\n" +
                "chr1\t1\tB\t1\tB\n" +
                "chr1\t1\tB\t2\tBB\n" +
                "chr1\t1\tB\t3\tBBB\n" +
                "chr1\t2\tA\t1\t2\n" +
                "chr1\t2\tA\t2\t22\n" +
                "chr1\t2\tB\t3\t222";

        final File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | pivot Category -v 1,2,3 -gc Grp", tempFile.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);

        final String expected = "Chrom\tPos\tGrp\t1_Value\t2_Value\t3_Value\n" +
                "chr1\t1\tA\tA\tAA\tAAA\n" +
                "chr1\t1\tB\tB\tBB\tBBB\n" +
                "chr1\t2\tA\t2\t22\t?\n" +
                "chr1\t2\tB\t?\t?\t222\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void pivotNorWithGroupingColumn() throws IOException {
        String contents = "#Grp\tCategory\tValue\n" +
                "A\t1\tA\n" +
                "B\t1\tB\n" +
                "C\t1\t2\n" +
                "A\t2\tAA\n" +
                "A\t3\tAAA\n" +
                "B\t2\tBB\n" +
                "B\t3\tBBB\n" +
                "C\t2\t22\n" +
                "C\t3\t222";

        final File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "test.tsv", contents);
        final String query = String.format("nor %s | pivot Category -v 1,2,3 -gc Grp", tempFile.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);

        final String expected = "ChromNOR\tPosNOR\tGrp\t1_Value\t2_Value\t3_Value\n" +
                "chrN\t0\tA\tA\tAA\tAAA\n" +
                "chrN\t0\tB\tB\tBB\tBBB\n" +
                "chrN\t0\tC\t2\t22\t222\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void pivotNorWithGroupingColumnOrdered() throws IOException {
        String contents = "#Grp\tCategory\tValue\n" +
                "A\t1\tA\n" +
                "A\t2\tAA\n" +
                "A\t3\tAAA\n" +
                "B\t1\tB\n" +
                "B\t2\tBB\n" +
                "B\t3\tBBB\n" +
                "C\t1\t2\n" +
                "C\t2\t22\n" +
                "C\t3\t222";

        final File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "test.tsv", contents);
        final String query = String.format("nor %s | pivot Category -v 1,2,3 -gc Grp -ordered", tempFile.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);

        final String expected = "ChromNOR\tPosNOR\tGrp\t1_Value\t2_Value\t3_Value\n" +
                "chrN\t0\tA\tA\tAA\tAAA\n" +
                "chrN\t0\tB\tB\tBB\tBBB\n" +
                "chrN\t0\tC\t2\t22\t222\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void pivotMultiplePosValuesFromFile() throws IOException {
        String contents = "Chrom\tPos\tCategory\tValue\n" +
                "chr1\t1\t1\t1\n" +
                "chr1\t1\t2\t11\n" +
                "chr1\t1\t3\t111\n" +
                "chr1\t2\t1\t2\n" +
                "chr1\t2\t2\t22\n" +
                "chr1\t2\t3\t222";

        String values = "#Value\n1\n2\n3\n";

        final File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final File valuesFile = FileTestUtils.createTempFile(workDir.getRoot(), "values.tsv", values);
        final String query = String.format("gor %s | pivot Category -vf %s",
                tempFile.getAbsolutePath(), valuesFile.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);

        final String expected = "Chrom\tPos\t1_Value\t2_Value\t3_Value\n" +
                "chr1\t1\t1\t11\t111\n" +
                "chr1\t2\t2\t22\t222\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void pivotMultiplePosValuesFromNestedQuery() throws IOException {
        String contents = "Chrom\tPos\tCategory\tValue\n" +
                "chr1\t1\t1\t1\n" +
                "chr1\t1\t2\t11\n" +
                "chr1\t1\t3\t111\n" +
                "chr1\t2\t1\t2\n" +
                "chr1\t2\t2\t22\n" +
                "chr1\t2\t3\t222";

        String values = "#Some\trandom\tstuff\tValue\n" +
                "Ignore\tto\there\t1\n" +
                "Ignore\tto\there\t2\n" +
                "Ignore\tto\there\t3\n";

        final File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final File valuesFile = FileTestUtils.createTempFile(workDir.getRoot(), "values.tsv", values);
        final String query = String.format("gor %s | pivot Category -vf <(nor %s | select Value)",
                tempFile.getAbsolutePath(), valuesFile.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);

        final String expected = "Chrom\tPos\t1_Value\t2_Value\t3_Value\n" +
                "chr1\t1\t1\t11\t111\n" +
                "chr1\t2\t2\t22\t222\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void pivotMultiplePosValuesAndPrefixesFromFile() throws IOException {
        String contents = "Chrom\tPos\tCategory\tValue\n" +
                "chr1\t1\t1\t1\n" +
                "chr1\t1\t2\t11\n" +
                "chr1\t1\t3\t111\n" +
                "chr1\t2\t1\t2\n" +
                "chr1\t2\t2\t22\n" +
                "chr1\t2\t3\t222";

        String values = "#Value\n1\n2\n3\n";
        String prefixes = "#Prefix\nFirst\nSecond\nThird\n";

        final File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final File valuesFile = FileTestUtils.createTempFile(workDir.getRoot(), "values.tsv", values);
        final File prefixesFile = FileTestUtils.createTempFile(workDir.getRoot(), "prefixes.tsv", prefixes);

        final String query = String.format("gor %s | pivot Category -vf %s -vp %s",
                tempFile.getAbsolutePath(), valuesFile.getAbsolutePath(), prefixesFile.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);

        final String expected = "Chrom\tPos\tFirst_Value\tSecond_Value\tThird_Value\n" +
                "chr1\t1\t1\t11\t111\n" +
                "chr1\t2\t2\t22\t222\n";
        Assert.assertEquals(expected, result);
    }

}
