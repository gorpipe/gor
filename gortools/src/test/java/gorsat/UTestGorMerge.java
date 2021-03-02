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

import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Created by sigmar on 02/05/16.
 */
public class UTestGorMerge {
    private File gorFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
    }

    private static final Logger log = LoggerFactory.getLogger(UTestGorMerge.class);

    @Test
    public void testMergeWithDifferentColumnNames() {
        String query = "gor ../tests/data/gor/dbsnp_test.gorz ../tests/data/gor/dbsnp_test_different_colnames.gor";

        boolean failed = false;

        try {
            TestUtils.runGorPipeCount(query);
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertTrue("Merging gor files should fail on files with different column names", failed);
    }

    @Test
    public void testMergeWithNor() {
        String query = "nor ../tests/data/gor/dbsnp_test.gorz | merge ../tests/data/gor/dbsnp_test.gorz";

        try (GenomicIterator pi = TestUtils.runGorPipeIterator(query)) {
            while (pi.hasNext()) {
                String next = pi.next().toString();
                Assert.assertEquals("Merging with nor failed", 7, next.split("\t").length);
            }
        }
    }

    @Test
    public void testMergeWithNestedNor() throws IOException {
        String fileCont = "#A\tB\nC\tD\nE\tF\n";
        Path p = Paths.get("test.tsv");
        String query = "nor <(nor test.tsv | merge test.tsv)";
        Files.write(p, fileCont.getBytes());
        try (GenomicIterator pi = TestUtils.runGorPipeIterator(query)) {
            while (pi.hasNext()) {
                String next = pi.next().toString();
                Assert.assertEquals("Merging with nor failed", 4, next.split("\t").length);
            }
        } finally {
            Files.deleteIfExists(p);
        }
    }

    @Test
    public void testMergeWithNorNonGorFile() throws IOException {
        String query = "nor " + gorFile.getCanonicalPath() + " | merge " + gorFile.getCanonicalPath() + "";

        try (GenomicIterator pi = TestUtils.runGorPipeIterator(query)) {
            while (pi.hasNext()) {
                String next = pi.next().toString();
                Assert.assertEquals("Merging with nor failed", 6, next.split("\t").length);
            }
        }
    }

    @Test
    public void testMergeNorWithNorFileAndSortColumns() throws IOException {
        File leftFile = FileTestUtils.createTempFile(workDir.getRoot(), "left.tsv",
                "#First\tSecond\tThird\n" +
                        "0000000000\tMWFTR\tAA\n" +
                        "0000000000\tH\tCJHA\n" +
                        "0000000000\tAHGDK\tEVZ\n" +
                        "0000000000\tVAW\tGUEBV\n" +
                        "0000000000\tTSO\tGYNEB\n");
        File rightFile = FileTestUtils.createTempFile(workDir.getRoot(), "right.tsv",
                "#First\tSecond\tThird\n" +
                        "0000000000\tMVNJ\tB\n" +
                        "0000000000\tXT\tBC\n" +
                        "0000000000\tLZFG\tBK\n" +
                        "0000000000\tHV\tLUR\n" +
                        "0000000000\tPB\tRN");

        String query = String.format("nor %s | merge -c First,Third %s", leftFile.getAbsoluteFile(), rightFile.getAbsoluteFile());
        String result = TestUtils.runGorPipe(query);

        String expected = "ChromNOR\tPosNOR\tFirst\tSecond\tThird\n" +
                "chrN\t0\t0000000000\tMWFTR\tAA\n" +
                "chrN\t0\t0000000000\tMVNJ\tB\n" +
                "chrN\t0\t0000000000\tXT\tBC\n" +
                "chrN\t0\t0000000000\tLZFG\tBK\n" +
                "chrN\t0\t0000000000\tH\tCJHA\n" +
                "chrN\t0\t0000000000\tAHGDK\tEVZ\n" +
                "chrN\t0\t0000000000\tVAW\tGUEBV\n" +
                "chrN\t0\t0000000000\tTSO\tGYNEB\n" +
                "chrN\t0\t0000000000\tHV\tLUR\n" +
                "chrN\t0\t0000000000\tPB\tRN\n";

        Assert.assertEquals(expected, result);
    }
}
