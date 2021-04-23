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

import org.apache.commons.lang3.SystemUtils;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sigmar on 25/04/16.
 */
public class UTestNestedQuery {

    private static final Logger log = LoggerFactory.getLogger(UTestNestedQuery.class);

    private File gorFile;
    private File dictionaryFile;
    private File pnFile;
    private File dictionaryFileFullPath;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
        dictionaryFile = FileTestUtils.createGenericDictionaryFile(workDir.getRoot(), gorFile.getName(), "dictionary1.gord");
        dictionaryFileFullPath = FileTestUtils.createGenericDictionaryFile(workDir.getRoot(), gorFile.getCanonicalPath(), "dictionary2.gord");
        pnFile = FileTestUtils.createPNTxtFile(workDir.getRoot());
    }

    @Test
    public void testNestedQueryInFilterDictionary() throws IOException {
        String query = "gor " + dictionaryFileFullPath.getCanonicalPath() + " -ff <(nor " + pnFile.getCanonicalPath() + " | top 1)";
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals(9, count);
    }

    @Test
    public void testNestedQueryInFilterDictionaryWithGroup() throws IOException {
        String query = "gor " + dictionaryFile.getCanonicalPath() + " -ff <(nor " + pnFile.getCanonicalPath() + " | top 1) | group chrom -count";
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals(1, count);
    }

    @Test
    public void testNestedQueryGorNorQuery() {
        String query = "gor <(nor ../tests/data/gor/dbsnp_test.gor)";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = testIterator(rs, 5);
            Assert.assertEquals(48, count);
        }
    }

    private int testIterator(GenomicIterator rs, int expectedValue) {
        int count = 0;
        while (rs.hasNext()) {
            String line = rs.next().toString();
            String[] split = line.split("\t");
            Assert.assertEquals(expectedValue, split.length);
            count++;
        }

        return count;
    }

    @Test
    public void testNestedQueryGorNorCmdQuery() {
        // No cat on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String query = "gor <(cmd {cat ../tests/data/gor/dbsnp_test.gor})";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = testIterator(rs, 5);
            Assert.assertEquals(48, count);
        }
    }

    @Test
    public void testNestedQueryGorNorNestedNorCmdQuery() {
        // No cat on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String query = "gor <(nor <(cmd -n {cat ../tests/data/gor/dbsnp_test.gor}))";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = testIterator(rs, 5);
            Assert.assertEquals(48, count);
        }
    }

    @Test
    public void testNestedQueryGorNorNestedCmdQuery() {
        // No cat on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String query = "gor <(nor <(cmd {cat ../tests/data/gor/dbsnp_test.gor}))";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = testIterator(rs, 5);
            Assert.assertEquals(48, count);
        }
    }

    @Test
    public void testNestedQueryGorNorGorQuery() throws IOException {
        String query = "gor <(nor <(gor <(gorrow chr1,1,2)))";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = testIterator(rs, 3);
            Assert.assertEquals(1, count);
        }
    }

    @Test
    public void testNestedWithinNestedQueryInFilterDictionary() throws IOException {
        String query = "gor " + dictionaryFileFullPath.getCanonicalPath() + " -ff <(nor " + pnFile.getCanonicalPath() + " | top 1 | rename #1 PN | merge <(nor ../tests/data/gor/dbsnp_test.gorz | top 1 | calc PN 'b' | select PN)) ";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(18, count);
    }

    @Test
    public void testNestedQueryInFilterGorFile() throws IOException {
        Path leftjoin = Paths.get("../tests/data/gor/dbsnp_test.gorz");
        String query = "gor " + leftjoin.toString() + " -ff <(nor " + pnFile.getCanonicalPath() + " | top 1)";

        // filtering on non .gord file should not fail
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals(0, count);
    }

    @Test
    public void testNestedGorQuery() throws IOException {
        String query = "gor <(gor " + gorFile.getCanonicalPath() + ")";
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals(9, count);
    }

    @Test
    public void testNestedGorQueryWithoutGorPrefix() throws IOException {
        String query = "gor <(" + gorFile.getCanonicalPath() + ")";
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals(9, count);
    }

    @Test
    public void testNestedVarjoinWithoutGorPrefix() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + "| varjoin <(" + gorFile.getCanonicalPath() + ")";
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals(9, count);
    }

    @Test
    public void testNestedProcessIterator() {
        // No cat on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String query = "gor ../tests/data/gor/genes.gorz | top 0 | cmd {cat <(gor ../tests/data/gor/genes.gorz | top 10)}";
        int count = 0;

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            while (rs.hasNext()) {
                rs.next();
                count++;
            }
        }

        Assert.assertEquals("Wrong number of lines in result", 10, count);
    }

    @Test
    public void testCompareGorRow() {
        String query = "gor -p chr16:0-100000000 <(gorrow chrX,1,1)";
        String query2 = "gor -p chrX:0-100000000 <(gorrow chr16,1,1)";
        String res = TestUtils.runGorPipe(query);
        String res2 = TestUtils.runGorPipe(query2);
        Assert.assertEquals(res, res2);
    }

    @Test
    public void testComparePipestepsAfterNestedGroupGenomeWithSeekVsRange() {
        String query = "gor -p chr16:0-100000000 <(gor ../tests/data/gor/genes.gorz | group genome -count)";
        String query2 = "gor -p chr16:0-100000000 <(gor ../tests/data/gor/genes.gorz | group genome -count) | log 1000000";
        TestUtils.assertTwoGorpipeResults(query, query2);
    }

    @Test
    public void testComparePipestepsAfterNestedGroupGenomeWithSeekVsRangeCountLines() {
        String query = "gor -p chr16:0-100000000 <(gor ../tests/data/gor/genes.gorz | group genome -count) | log 1000000";
        String query2 = "gor -p chrX:0-100000000 <(gor ../tests/data/gor/genes.gorz | group genome -count) | log 1000000";
        Assert.assertEquals(TestUtils.runGorPipeCount(query), TestUtils.runGorPipeCount(query2));
    }
}
