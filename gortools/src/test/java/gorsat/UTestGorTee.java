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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 * Created by sigmar on 04/10/2016.
 */
public class UTestGorTee {

    private File gorFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
    }

    @Test
    public void testGorTee() throws IOException {
        Path p = Files.createTempFile("gortee", "test");
        String gorcmd = "gor ../tests/data/gor/dbsnp_test.gor | top 5 | tee >(group genome -count | write " + p.toAbsolutePath().toString() + ")";
        TestUtils.runGorPipeCount(gorcmd);

        List<String> lines = Files.readAllLines(p);
        Assert.assertEquals("Incorrect group size", 2, lines.size());
        Assert.assertEquals("Chrom\tbpStart\tbpStop\tallCount", lines.get(0));
        Assert.assertEquals("chrA\t0\t1000000000\t5", lines.get(1));
    }

    @Test
    public void testNorTee() throws IOException {
        Path p = Files.createTempFile("nortee", "test");
        String gorcmd = "nor " + gorFile.getCanonicalPath() + " | top 15 | tee >(group -count| write " + p.toAbsolutePath().toString() + ")";
        TestUtils.runGorPipeCount(gorcmd);

        List<String> lines = Files.readAllLines(p);
        Assert.assertEquals("Incorrect group size", 2, lines.size());
        Assert.assertEquals("allCount", lines.get(0));
        Assert.assertEquals("9", lines.get(1));
    }

    @Test
    public void testDoesNotSwallow() throws IOException {
        final File teeOut = workDir.newFile("teeOut.gor");
        final File otherOut = workDir.newFile("outherOut.gor");
        final String query = "gor ../tests/data/gor/dbsnp_test.gor | tee >(write " + teeOut.getAbsolutePath() + " ) | write " + otherOut.getAbsolutePath();
        TestUtils.runGorPipe(query);

        Iterator<String> teeLines = Files.readAllLines(teeOut.toPath()).iterator();
        Iterator<String> otherLines = Files.readAllLines(otherOut.toPath()).iterator();

        teeLines.forEachRemaining(line -> {
            Assert.assertTrue(otherLines.hasNext());
            Assert.assertEquals(line, otherLines.next());
        });

        Assert.assertFalse(otherLines.hasNext());
    }
}
