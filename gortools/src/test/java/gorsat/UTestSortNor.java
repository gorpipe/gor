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

import gorsat.Analysis.SortAnalysis;
import gorsat.Analysis.TopN;
import gorsat.Commands.Analysis;
import gorsat.Commands.Sort;
import gorsat.Iterators.FastGorSource;
import gorsat.Iterators.PipeStepIteratorAdaptor;
import gorsat.process.GenericSessionFactory;
import gorsat.process.GorSessionFactory;
import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import scala.jdk.javaapi.CollectionConverters;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;

/**
 * Created by sigmar on 21/12/15.
 */
public class UTestSortNor {
    private String norData = "Col1\tCol2\tCol3\n" +
            "AA\t1\tb\n" +
            "AA\t100\tb\n" +
            "AA\t100\tc\n" +
            "AA\t2\tc\n" +
            "AB\t12\td\n";
    private File norFile;
    private Path dir;
    private GorSession session;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        dir = Files.createTempDirectory("sorttest");
        GorSessionFactory factory = new GenericSessionFactory("", dir.toAbsolutePath().toString());
        session = factory.create();
        norFile = FileTestUtils.createTempFile(workDir.getRoot(), "norFile.tsv", norData);
    }

    @Test
    public void testSortNorFirstColumn() {
        String gorcmd = "nor " + norFile.toString() + " -h" +
                "| sort -c #1-";

        var result = TestUtils.runGorPipe(gorcmd, "-nor");

        Assert.assertArrayEquals(("ChromNOR\tPosNOR\tCol1\tCol2\tCol3\n" +
                "chrN\t0\tAA\t1\tb\n" +
                "chrN\t0\tAA\t100\tb\n" +
                "chrN\t0\tAA\t100\tc\n" +
                "chrN\t0\tAA\t2\tc\n" +
                "chrN\t0\tAB\t12\td").split("\n"),
                result.split("\n"));
    }

    @Test
    public void testSortNorWithOpenEnd() {
        String gorcmd = "norrows 1000 | calc ab if(random()<0.5,'a','b') | calc r random() | calc c if(random()<0.1,'c',if(random()<0.5,'a','b')) | sort -c ab-c";

        var result = TestUtils.runGorPipe(gorcmd, "-nor");

        Assert.assertTrue(true);
    }

    @Test
    public void testSortColumnParsingNoRange() {
        String[] args = {"-c", "a,c"};
        var result = Sort.parseSortColumns(args, true, "ChromNOR\tPosNOR\ta\tb\tc\td\te" );

        Assert.assertEquals(2, result.length);
        Assert.assertEquals(2, result[0].getSortColumn());
        Assert.assertEquals(4, result[1].getSortColumn());
    }

    @Test
    public void testSortColumnParsingWithRange() {
        String[] args = {"-c", "a-c"};
        var result = Sort.parseSortColumns(args, true, "ChromNOR\tPosNOR\ta\tb\tc\td\te" );

        Assert.assertEquals(3, result.length);
        Assert.assertEquals(2, result[0].getSortColumn());
        Assert.assertEquals(3, result[1].getSortColumn());
        Assert.assertEquals(4, result[2].getSortColumn());
    }

    @Test
    public void testSortColumnParsingWithRangeOpenEnded() {
        String[] args = {"-c", "c-"};
        var result = Sort.parseSortColumns(args, true, "ChromNOR\tPosNOR\ta\tb\tc\td\te" );

        Assert.assertEquals(3, result.length);
        Assert.assertEquals(4, result[0].getSortColumn());
        Assert.assertEquals(5, result[1].getSortColumn());
        Assert.assertEquals(6, result[2].getSortColumn());
    }

    @Test
    public void testSortColumnParsingWithRangeButMixedOrder() {
        String[] args = {"-c", "c-e,a"};
        var result = Sort.parseSortColumns(args, true, "ChromNOR\tPosNOR\ta\tb\tc\td\te" );

        Assert.assertEquals(4, result.length);
        Assert.assertEquals(4, result[0].getSortColumn());
        Assert.assertEquals(5, result[1].getSortColumn());
        Assert.assertEquals(6, result[2].getSortColumn());
        Assert.assertEquals(2, result[3].getSortColumn());
    }

    @Test
    public void testIndexedSortColumnParsingWithRangeButMixedOrder() {
        String[] args = {"-c", "#3-#5,#1"};
        var result = Sort.parseSortColumns(args, true, "ChromNOR\tPosNOR\ta\tb\tc\td\te" );

        Assert.assertEquals(4, result.length);
        Assert.assertEquals(4, result[0].getSortColumn());
        Assert.assertEquals(5, result[1].getSortColumn());
        Assert.assertEquals(6, result[2].getSortColumn());
        Assert.assertEquals(2, result[3].getSortColumn());
    }

    @Test
    public void testSortColumnParsingOrderAndType() {
        String[] args = {"-c", "b:nr,a:n,c-:r"};
        var result = Sort.parseSortColumns(args, true, "ChromNOR\tPosNOR\ta\tb\tc\td\te" );

        // Test indexes
        Assert.assertEquals(5, result.length);
        Assert.assertEquals(3, result[0].getSortColumn());
        Assert.assertEquals(2, result[1].getSortColumn());
        Assert.assertEquals(4, result[2].getSortColumn());
        Assert.assertEquals(5, result[3].getSortColumn());
        Assert.assertEquals(6, result[4].getSortColumn());

        // Test sort order
        Assert.assertEquals(Row.SortOrder.REVERSE, result[0].getSortOrder());
        Assert.assertEquals(Row.SortOrder.FORWARD, result[1].getSortOrder());
        Assert.assertEquals(Row.SortOrder.REVERSE, result[2].getSortOrder());
        Assert.assertEquals(Row.SortOrder.REVERSE, result[3].getSortOrder());
        Assert.assertEquals(Row.SortOrder.REVERSE, result[4].getSortOrder());

        // Test sort type
        Assert.assertEquals(Row.SortType.NUMBERIC, result[0].getSortType());
        Assert.assertEquals(Row.SortType.NUMBERIC, result[1].getSortType());
        Assert.assertEquals(Row.SortType.STRING, result[2].getSortType());
        Assert.assertEquals(Row.SortType.STRING, result[3].getSortType());
        Assert.assertEquals(Row.SortType.STRING, result[4].getSortType());
    }
}
