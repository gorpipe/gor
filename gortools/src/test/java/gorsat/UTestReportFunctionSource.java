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

import gorsat.Commands.CommandParseUtilities;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Paths;

/**
 * Tests for the report(...) function source, which renders a yml report builder and reads it as a nested query.
 */
public class UTestReportFunctionSource {

    private static final String REPORTS = "../tests/data/reports";

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setupTest() {
        String macroPath = Paths.get(REPORTS).toAbsolutePath().normalize().toString();
        System.setProperty("dialog.macrodir", macroPath);
    }

    @Test
    public void testIsNestedCommand() {
        Assert.assertTrue(CommandParseUtilities.isNestedCommand("report(x.yml)"));
        Assert.assertTrue(CommandParseUtilities.isNestedCommand(" REPORT( x.yml , top=1 ) "));
        Assert.assertTrue(CommandParseUtilities.isNestedCommand("report(template://org/cat/name/latest.yml, a='b,c')"));
        Assert.assertFalse(CommandParseUtilities.isNestedCommand("myreport(x.yml)"));
        Assert.assertFalse(CommandParseUtilities.isNestedCommand("report.gor"));
        Assert.assertFalse(CommandParseUtilities.isNestedCommand("report(x.yml) | top 1"));
        Assert.assertFalse(CommandParseUtilities.isNestedCommand("report(x.yml"));
    }

    @Test
    public void testReportWithoutArguments() {
        String expected = TestUtils.runGorPipe("gor " + REPORTS + "/testmacro.yml()");
        String result = TestUtils.runGorPipe("gor report(" + REPORTS + "/testmacro.yml)");
        Assert.assertEquals(expected, result);
        Assert.assertEquals(5, TestUtils.runGorPipeCount("gor report(" + REPORTS + "/testmacro.yml)"));
    }

    @Test
    public void testReportWithArguments() {
        Assert.assertEquals(7, TestUtils.runGorPipeCount("gor report(" + REPORTS + "/test2.yml, top=7)"));
    }

    @Test
    public void testReportWithWhitespaceAndUpperCase() {
        Assert.assertEquals(3, TestUtils.runGorPipeCount("gor REPORT(  " + REPORTS + "/test2.yml ,  top = 3 )"));
    }

    @Test
    public void testReportSelectQueryEntry() {
        String expected = TestUtils.runGorPipe("gor " + REPORTS + "/test2.yml(query2)");
        String result = TestUtils.runGorPipe("gor report(" + REPORTS + "/test2.yml, query2)");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testReportNamedReport() {
        String args = "top=2, CASEs=['rs544101329','rs28970552']";
        String expected = TestUtils.runGorPipe("gor " + REPORTS + "/test3.yml::TestReport(" + args + ")");
        String result = TestUtils.runGorPipe("gor report(" + REPORTS + "/test3.yml::TestReport, " + args + ")");
        Assert.assertEquals(expected, result);
        Assert.assertEquals(2, TestUtils.runGorPipeCount("gor report(" + REPORTS + "/test3.yml::TestReport, " + args + ")"));
    }

    @Test
    public void testReportUrlResource() {
        // Urls with a scheme, such as template://, contain ':' which must not be taken as a report name separator
        String url = "file://" + Paths.get(REPORTS, "test2.yml").toAbsolutePath().normalize();
        Assert.assertEquals(3, TestUtils.runGorPipeCount("gor report(" + url + ", top=3)"));
    }

    @Test
    public void testReportRenderingNorQuery() throws Exception {
        File tsv = FileTestUtils.createTempFile(workDir.getRoot(), "data.tsv", "#a\tb\n1\tx\n2\ty\n3\tz\n");
        File yml = FileTestUtils.createTempFile(workDir.getRoot(), "norreport.yml",
                "NorReport:\n" +
                        "    query: |\n" +
                        "        nor " + tsv.getAbsolutePath() + " | top ${top.val}\n" +
                        "    arguments:\n" +
                        "      - name: top\n" +
                        "        type: string\n" +
                        "        default: \"10\"\n");

        String result = TestUtils.runGorPipe("nor report(" + yml.getAbsolutePath() + ", top=2)");
        Assert.assertEquals("ChromNOR\tPosNOR\ta\tb\nchrN\t0\t1\tx\nchrN\t0\t2\ty\n", result);
    }

    @Test
    public void testReportRenderingGorQueryInNor() {
        Assert.assertEquals(4, TestUtils.runGorPipeCount("nor report(" + REPORTS + "/test2.yml, top=4)"));
    }

    @Test
    public void testReportAsJoinSource() {
        String left = "gor ../tests/data/gor/dbsnp_test.gorz | join -snpsnp ";
        String expected = TestUtils.runGorPipe(left + "<(gor " + REPORTS + "/test2.yml(top=5))");
        String result = TestUtils.runGorPipe(left + "report(" + REPORTS + "/test2.yml, top=5)");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testReportAsMergeSource() {
        String query = "gor report(" + REPORTS + "/test2.yml, top=3) | merge report(" + REPORTS + "/test2.yml, top=2)";
        Assert.assertEquals(5, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testReportInSeekedNestedQuery() {
        // A range on a nested query rewrites it with a start selector, including function sources in it
        String report = REPORTS + "/test2.yml";
        String expected = TestUtils.runGorPipe("gor -p chr1 <(gor <(gor " + report + "(query2)) | merge <(gor " + report + "(top=2)))");
        String result = TestUtils.runGorPipe("gor -p chr1 <(gor report(" + report + ", query2) | merge report(" + report + ", top=2))");
        Assert.assertEquals(expected, result);
        Assert.assertTrue(TestUtils.runGorPipeCount("gor -p chr1 <(gor report(" + report + ", query2) | merge report(" + report + ", top=2))") > 0);
    }

    @Test
    public void testReportInPgor() {
        String expected = TestUtils.runGorPipe("gor report(" + REPORTS + "/test2.yml, query2)");
        String result = TestUtils.runGorPipe("pgor report(" + REPORTS + "/test2.yml, query2)");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testReportAsPipeSteps() {
        String left = "gor " + REPORTS + "/lib.yml(default_query) | pipesteps ";
        String expected = TestUtils.runGorPipe(left + REPORTS + "/lib.yml(mystep,top=11)");
        String result = TestUtils.runGorPipe(left + "report(" + REPORTS + "/lib.yml, mystep, top=11)");
        Assert.assertEquals(expected, result);
        Assert.assertEquals(11, TestUtils.runGorPipeCount(left + "report(" + REPORTS + "/lib.yml, mystep, top=11)"));
    }

    @Test
    public void testReportWithoutResource() {
        try {
            TestUtils.runGorPipe("gor report()");
            Assert.fail("report() without a resource should fail");
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("report"));
        }
    }

    @Test
    public void testReportResourceMustBeYml() {
        try {
            TestUtils.runGorPipe("gor report(../tests/data/gor/genes.gor)");
            Assert.fail("report() of a non yml resource should fail");
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("yml"));
        }
    }
}
