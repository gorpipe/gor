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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ENGKNOW-4237: a nested (dynamic) source whose path merely contains ".yml", e.g. the bucketize temp table
 * .dict.temp.bucketizing.YMLY9IF6.gord, must not be treated as a yml report (that recursed until StackOverflowError).
 */
public class UTestDynIteratorYmlPath {

    private static final String GENES = "../tests/data/gor/genes.gorz";

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private String expected;

    @Before
    public void setupTest() {
        expected = TestUtils.runGorPipe("gor " + GENES + " | top 10");
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
    public void testMergeNestedSourceWithYmlInFileName() throws IOException {
        String file = copyGenesTo("b.yml.data.gorz");
        int count = TestUtils.runGorPipeCount("gor " + GENES + " | merge <(gor " + file + ")");
        Assert.assertEquals(2 * TestUtils.runGorPipeCount("gor " + GENES), count);
    }
}
