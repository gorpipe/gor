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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * ENGKNOW-4237: a nested (dynamic) source whose path merely contains ".yml", e.g. the bucketize temp table
 * .dict.temp.bucketizing.YMLY9IF6.gord, must not be treated as a yml report (that recursed until StackOverflowError).
 */
public class UTestDynIteratorYmlPath {

    private static final String GENES = "../tests/data/gor/genes.gorz";
    private static final String REPORTS = "../tests/data/reports";

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private String expected;
    private String oldMacroDir;

    @Before
    public void setupTest() {
        oldMacroDir = System.getProperty("dialog.macrodir");
        System.setProperty("dialog.macrodir", Paths.get(REPORTS).toAbsolutePath().normalize().toString());
        expected = TestUtils.runGorPipe("gor " + GENES + " | top 10");
    }

    @After
    public void tearDown() {
        if (oldMacroDir == null) System.clearProperty("dialog.macrodir");
        else System.setProperty("dialog.macrodir", oldMacroDir);
    }

    private String copyGenesTo(String relPath) throws IOException {
        Path target = workDir.getRoot().toPath().resolve(relPath);
        Files.createDirectories(target.getParent());
        Files.copy(Paths.get(GENES), target);
        return target.toString();
    }

    @Test
    public void testNestedSourceWithYmlInFileName() throws IOException {
        String file = copyGenesTo(".dict.temp.bucketizing.YMLY9IF6.gorz");
        Assert.assertEquals(expected, TestUtils.runGorPipe("gor <(gor " + file + ") | top 10"));
    }

    @Test
    public void testNestedSourceWithYmlInDirectoryName() throws IOException {
        String file = copyGenesTo("x.yml_dir/genes.gorz");
        Assert.assertEquals(expected, TestUtils.runGorPipe("gor <(gor " + file + ") | top 10"));
    }

    @Test
    public void testSeekedNestedSourceWithYmlInFileName() throws IOException {
        String file = copyGenesTo("a.Yml.gorz");
        String expectedChr2 = TestUtils.runGorPipe("gor -p chr2 " + GENES + " | top 10");
        Assert.assertEquals(expectedChr2, TestUtils.runGorPipe("gor -p chr2 <(gor " + file + ") | top 10"));
    }

    @Test
    public void testNestedYmlSourceIsExpanded() {
        String direct = TestUtils.runGorPipe("gor " + REPORTS + "/testmacro.yml()");
        Assert.assertEquals(direct, TestUtils.runGorPipe("gor <(gor " + REPORTS + "/testmacro.yml())"));
    }

    @Test
    public void testMergeNestedYmlSourceIsExpanded() {
        // The merge right source is seeked, so it goes through DynamicRowSource.modifiedCommand with a position
        int count = TestUtils.runGorPipeCount("gor " + REPORTS + "/test2.yml(top=7)");
        Assert.assertEquals(2L * count,
                TestUtils.runGorPipeCount("gor " + REPORTS + "/test2.yml(top=7) | merge <(gor " + REPORTS + "/test2.yml(top=7))"));
    }

    @Test
    public void testNestedQuotedYmlSourceIsExpanded() {
        String direct = TestUtils.runGorPipe("gor " + REPORTS + "/testmacro.yml()");
        Assert.assertEquals(direct, TestUtils.runGorPipe("gor <(gor '" + REPORTS + "/testmacro.yml()')"));
    }

    @Test
    public void testNestedBareYmlSourceIsExpanded() {
        String direct = TestUtils.runGorPipe("gor " + REPORTS + "/testmacro.yml()");
        Assert.assertEquals(direct, TestUtils.runGorPipe("gor <(gor " + REPORTS + "/testmacro.yml)"));
    }

    @Test
    public void testSeekedNestedYmlSourceIsExpanded() {
        // -p seeks the nested source, so the rendered yml query goes through modifiedCommand with a start selector
        String rendered = "create xxx = gor ../tests/data/gor/dbsnp_test.gorz | calc semic ';' | top 100; gor [xxx] | top 50";
        String expectedChr1 = TestUtils.runGorPipe("gor -p chr1 <(" + rendered + ")");
        String result = TestUtils.runGorPipe("gor -p chr1 <(gor " + REPORTS + "/test2.yml(top=50))");
        Assert.assertTrue(TestUtils.runGorPipeCount("gor -p chr1 <(gor " + REPORTS + "/test2.yml(top=50))") > 0);
        Assert.assertEquals(expectedChr1, result);
    }

    @Test
    public void testCyclicYmlReportsFailWithParsingError() throws IOException {
        Path dir = workDir.getRoot().toPath();
        Path a = dir.resolve("a.yml");
        Path b = dir.resolve("b.yml");
        Files.write(a, List.of("Test Report:", "    query: |", "        gor " + b + "()"));
        Files.write(b, List.of("Test Report:", "    query: |", "        gor " + a + "()"));
        assertCyclicYmlError("gor -p chr1 <(gor " + a + "())");
    }

    @Test
    public void testCyclicYmlReportsThroughMergeFailWithParsingError() throws IOException {
        // Each report merges in the other, so the cycle goes through the merge right source of modifiedCommand
        Path dir = workDir.getRoot().toPath();
        Path a = dir.resolve("a.yml");
        Path b = dir.resolve("b.yml");
        String genes = Paths.get(GENES).toAbsolutePath().normalize().toString();
        Files.write(a, List.of("Test Report:", "    query: |", "        gor " + genes + " | merge <(gor " + b + "())"));
        Files.write(b, List.of("Test Report:", "    query: |", "        gor " + genes + " | merge <(gor " + a + "())"));
        assertCyclicYmlError("gor -p chr1 <(gor " + a + "())");
    }

    private static void assertCyclicYmlError(String query) {
        try {
            TestUtils.runGorPipe(query);
            Assert.fail("Cyclic yml reports should fail");
        } catch (Throwable e) {
            Throwable t = e;
            while (t != null && !(t instanceof GorParsingException)) t = t.getCause();
            Assert.assertNotNull("Expected GorParsingException, got " + e, t);
            Assert.assertTrue(t.getMessage(), t.getMessage().toLowerCase().contains("cyclic"));
        }
    }

    @Test
    public void testSourceIndexesUseTokenPositions() {
        // "x.yml" first appears inside the -s value, but the yml source token itself comes after the nested source
        String step = "gor -s ax.ymlz <(gor g.gorz) x.yml";
        DynIterator.SourceIndexes idx = DynIterator.sourceIndexes(step);
        Assert.assertEquals(step.indexOf("<("), idx.nested());
        Assert.assertEquals(step.lastIndexOf("x.yml"), idx.yml());

        // Same for a function source whose text also appears earlier inside another token
        step = "gor -s xreport(a.yml) report(a.yml)";
        idx = DynIterator.sourceIndexes(step);
        Assert.assertEquals(step.lastIndexOf("report(a.yml)"), idx.nested());

        step = "gor /tmp/.dict.temp.bucketizing.YMLY9IF6.gord -s Special";
        idx = DynIterator.sourceIndexes(step);
        Assert.assertEquals(step.length(), idx.nested());
        Assert.assertEquals(step.length(), idx.yml());

        step = "gor x.yml(top=5)";
        idx = DynIterator.sourceIndexes(step);
        Assert.assertEquals(4, idx.yml());
        Assert.assertEquals(step.length(), idx.nested());
    }

    @Test
    public void testMergeNestedSourceWithYmlInFileName() throws IOException {
        String file = copyGenesTo("b.yml.data.gorz");
        int count = TestUtils.runGorPipeCount("gor " + GENES + " | merge <(gor " + file + ")");
        Assert.assertEquals(2L * TestUtils.runGorPipeCount("gor " + GENES), count);
    }
}
