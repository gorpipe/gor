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
import org.junit.rules.TemporaryFolder;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by sigmar on 17/05/2017.
 */
public class UTestRownum {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void testRownum() {
        String query = "gor ../tests/data/gor/genes.gor | rownum | where mod(rownum,1000) = 0";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(51, count);
    }

    @Test
    public void testRownumWithGroups() throws IOException {

        try {
            var file = workDir.newFile("group_test.nor");
            try(var writer = new FileWriter(file)) {
                writer.write("C1\tC2\n");
                writer.write("A\tA\n");
                writer.write("A\tA\n");
                writer.write("A\tB\n");
                writer.write("A\tA\n");
                writer.write("B\tA\n");
                writer.write("B\tB\n");
                writer.write("B\tA\n");
                writer.write("B\tB\n");
            }

            String query = "nor -h " + file.getAbsolutePath() + " |  rownum -gc c1,c2";

            var lines = TestUtils.runGorPipeLines(query);
            Assert.assertEquals("chrN\t0\tA\tA\t1\n", lines[1]);
            Assert.assertEquals("chrN\t0\tA\tA\t2\n", lines[2]);
            Assert.assertEquals("chrN\t0\tA\tB\t1\n", lines[3]);
            Assert.assertEquals("chrN\t0\tA\tA\t3\n", lines[4]);
            Assert.assertEquals("chrN\t0\tB\tA\t1\n", lines[5]);
            Assert.assertEquals("chrN\t0\tB\tB\t1\n", lines[6]);
            Assert.assertEquals("chrN\t0\tB\tA\t2\n", lines[7]);
            Assert.assertEquals("chrN\t0\tB\tB\t2\n", lines[8]);

        }
        finally {
            workDir.delete();
        }
    }

    @Test
    public void testRownumWithInvalidGroups() throws IOException {
        String query = "gor ../tests/data/gor/genes.gor | rownum -gc gene";
        var e = Assert.assertThrows(GorParsingException.class, () -> TestUtils.runGorPipeCount(query));
        Assert.assertTrue(e.getMessage().contains("1: CHROM"));
    }
}
