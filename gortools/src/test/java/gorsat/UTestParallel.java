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

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class UTestParallel {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void testNorParallelQuery() {
        String query = "create pnlist = norrows 100 -offset 100 | calc pn 'PN_'+rownum  | signature -timeres 1; parallel -parts [pnlist] <(norrows 100 -offset #{col:rownum} | calc pn '#{col:pn}')";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals("parallel query total lines", 10000, count);
    }

    @Test
    public void testGorParallelQuery() {
        String query = "create pnlist = norrows 100 -offset 100 | calc pn 'PN_'+rownum  | signature -timeres 1; parallel -parts [pnlist] <(gorrows -p chr1:0-#{col:rownum} | calc pn '#{col:pn}')";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals("parallel query total lines", 14950, count);
    }

    @Test
    public void testGorParallelQueryWithNonColReplacement() {
        String query = "create pnlist = norrows 100 -offset 100 | calc pn 'PN_'+rownum  | signature -timeres 1; parallel -parts [pnlist] <(gorrows -p chr1:0-#{col:rownum} | calc pn '#{col:pn}' + '#{foo}') | top 1";
        String[] result = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals("parallel query total lines", 100, result.length);
        Assert.assertTrue(result[0].contains("#{foo}"));
    }

    @Test(expected = GorParsingException.class)
    public void testPArallelQueryWithInvalidREplacementEntry() {
        String query = "create pnlist = norrows 100 -offset 100 | calc pn 'PN_'+rownum  | signature -timeres 1; parallel -parts [pnlist] <(gorrows -p chr1:0-#{rownum} | calc pn '#{col:pn}' | calc foo '#{col:foobar}')";
        TestUtils.runGorPipeCount(query);
    }

    @Test(expected = GorParsingException.class)
    public void testPArallelQueryWithMissingParts() {
        String query = "create pnlist = norrows 100 -offset 100 | calc pn 'PN_'+rownum  | signature -timeres 1; parallel <(gorrows -p chr1:0-#{col:rownum} | calc pn '#{col:pn}')";
        TestUtils.runGorPipeCount(query);
    }

    @Test(expected = GorParsingException.class)
    public void testPArallelQueryWithMissingNestedQuery() {
        String query = "create pnlist = norrows 100 -offset 100 | calc pn 'PN_'+rownum  | signature -timeres 1; parallel -parts [pnlist]";
        TestUtils.runGorPipeCount(query);
    }

    @Test(expected = GorParsingException.class)
    public void testPArallelQueryWithinvalidNestedQuery() {
        String query = "create pnlist =x norrows 100 -offset 100 | calc pn 'PN_'+rownum  | signature -timeres 1; parallel -parts [pnlist] gorrrow chr1,1";
        TestUtils.runGorPipeCount(query);
    }

    @Test(expected = GorParsingException.class)
    public void testGorParallelQueryExceedingLimit() {
        String query = "create pnlist = norrows 100 -offset 100 | calc pn 'PN_'+rownum  | signature -timeres 1; parallel -parts [pnlist] -limit 10 <(gorrows -p chr1:0-#{col:rownum} | calc pn '#{col:pn}')";
        TestUtils.runGorPipeCount(query);
    }

    /**
     * ENGKNOW-3979: when one part of a parallel create fails, a sibling part that is still running gets
     * interrupted. Its partial (header-only) output must not be committed to the result cache, otherwise
     * a rerun after the failure is fixed reuses the empty part and silently drops its rows.
     */
    @Test
    public void testFailedPartDoesNotLeaveSiblingPartInCache() throws IOException {
        Path root = workDir.getRoot().toPath();
        Path cacheDir = Files.createDirectory(root.resolve("result_cache"));
        Path failingPart = root.resolve("a.gor");
        Files.writeString(failingPart, "chrom\tpos\tval\tdelay\nchr1\t1\tbad\t0\n");
        Files.writeString(root.resolve("b.gor"),
                "chrom\tpos\tval\tdelay\nchr2\t1\tok\t500\nchr2\t2\tok\t500\nchr2\t3\tok\t500\n");
        Files.writeString(root.resolve("parts.tsv"), "#name\na\nb\n");

        String query = "create xx = parallel -parts parts.tsv <(gor #{col:name}.gor | calc s sleep(delay) | throwif val = 'bad'); gor [xx]";

        Assert.assertThrows(GorException.class,
                () -> TestUtils.runGorPipe(query, root.toString(), cacheDir.toString(), false, null, null));

        Files.writeString(failingPart, "chrom\tpos\tval\tdelay\nchr1\t1\tok\t0\n");
        Files.setLastModifiedTime(failingPart, FileTime.fromMillis(System.currentTimeMillis() + 10000));

        String result = TestUtils.runGorPipe(query, root.toString(), cacheDir.toString(), false, null, null);
        Assert.assertEquals("All rows from all parts expected on rerun, got:\n" + result,
                5, result.split("\n").length);
    }
}
