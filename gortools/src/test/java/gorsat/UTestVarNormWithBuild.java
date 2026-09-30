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

import gorsat.Commands.VarNormWithBuild;
import org.gorpipe.exceptions.GorException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * VARNORM_WITH_BUILD normalizes against the reference given as its first argument instead of the
 * project default build.  ref_mini chr1 starts with a TAACCC repeat at 10001, so a one base deletion
 * of the C at 10006 (CC>C at 10005) left-normalizes to AC>A at 10003.
 */
public class UTestVarNormWithBuild {

    private static final String BUILD = "../tests/data/ref_mini/chromSeq";
    private static final String CONFIG = "../tests/data/ref_mini/gor_config.txt";
    private static final String INPUT = "gorrows -p chr1:10005-10006 | calc ref 'CC' | calc alt 'C' | select 1,2,ref,alt";

    @Test
    public void testLeftNormalizesAgainstGivenBuild() {
        String result = TestUtils.runGorPipe(INPUT + " | varnorm_with_build " + BUILD + " ref alt");
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10003\tAC\tA\n", result);
    }

    @Test
    public void testQuotedBuildPath() {
        String result = TestUtils.runGorPipe(INPUT + " | varnorm_with_build '" + BUILD + "' ref alt");
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10003\tAC\tA\n", result);
    }

    @Test
    public void testSameAsVarnormWhenBuildIsProjectDefault() {
        // Guard: the config really makes ref_mini the default build, so the comparison below is meaningful.
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10003\tAC\tA\n",
                TestUtils.runGorPipe(INPUT + " | varnorm ref alt", "-config", CONFIG));
        for (String options : new String[]{"", "-left", "-right", "-trim", "-right -trim"}) {
            String expected = TestUtils.runGorPipe(INPUT + " | varnorm ref alt " + options, "-config", CONFIG);
            String result = TestUtils.runGorPipe(INPUT + " | varnorm_with_build " + BUILD + " ref alt " + options, "-config", CONFIG);
            Assert.assertEquals("Options: '" + options + "'", expected, result);
        }
    }

    @Test
    public void testBuildOverridesProjectDefault() {
        // No -config: the project has no usable default build, so plain varnorm reads the anchor as N and cannot shift.
        String plain = TestUtils.runGorPipe(INPUT + " | varnorm ref alt");
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10005\tNC\tN\n", plain);

        String withBuild = TestUtils.runGorPipe(INPUT + " | varnorm_with_build " + BUILD + " ref alt");
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10003\tAC\tA\n", withBuild);
    }

    @Test
    public void testInsertionLeftAndRight() {
        // Inserting a C into the CCC run at 10004-10006.
        String ins = "gorrows -p chr1:10005-10006 | calc ref 'C' | calc alt 'CC' | select 1,2,ref,alt";
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10003\tA\tAC\n",
                TestUtils.runGorPipe(ins + " | varnorm_with_build " + BUILD + " ref alt"));
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10006\tC\tCC\n",
                TestUtils.runGorPipe(ins + " | varnorm_with_build " + BUILD + " ref alt -right"));
    }

    @Test
    public void testRightNormalizationShiftsAgainstGivenBuild() {
        // The left-normalized AC>A at 10003 shifts right to the end of the CCC run.
        String del = "gorrows -p chr1:10003-10004 | calc ref 'AC' | calc alt 'A' | select 1,2,ref,alt";
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10005\tCC\tC\n",
                TestUtils.runGorPipe(del + " | varnorm_with_build " + BUILD + " ref alt -right"));
    }

    @Test
    public void testMultipleRowsAndContigs() {
        // chrM starts GATCACAGG, so the GG>G at 8 left-normalizes to AG>A at 7.
        String query = INPUT + " | merge <(gorrows -p chrM:8-9 | calc ref 'GG' | calc alt 'G' | select 1,2,ref,alt)"
                + " | varnorm_with_build " + BUILD + " ref alt";
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10003\tAC\tA\nchrM\t7\tAG\tA\n",
                TestUtils.runGorPipe(query));
    }

    @Test
    public void testIndelAtContigStart() {
        // Deleting the A at chrM:2, there is nothing upstream to shift into.
        String query = "gorrows -p chrM:1-2 | calc ref 'GA' | calc alt 'G' | select 1,2,ref,alt"
                + " | varnorm_with_build " + BUILD + " ref alt";
        Assert.assertEquals("chrom\tpos\tref\talt\nchrM\t1\tGA\tG\n", TestUtils.runGorPipe(query));
    }

    @Test
    public void testSegmentForm() {
        String seg = "gorrows -p chr1:10005-10006 | calc stop 10006 | calc ref 'CC' | calc alt 'C' | select 1,2,stop,ref,alt";
        String expected = TestUtils.runGorPipe(seg + " | varnorm ref alt -seg", "-config", CONFIG);
        String result = TestUtils.runGorPipe(seg + " | varnorm_with_build " + BUILD + " ref alt -seg");
        Assert.assertEquals("chrom\tpos\tstop\tref\talt\nchr1\t10003\t10005\tAC\tA\n", result);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testServerModeBuildOutsideProject() {
        // The reference is read with the unsecure reader (as refbases_with_build), so the existence check must agree.
        String absoluteBuild = java.nio.file.Paths.get(BUILD).toAbsolutePath().normalize().toString();
        String result = TestUtils.runGorPipe(INPUT + " | varnorm_with_build " + absoluteBuild + " ref alt", true);
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10003\tAC\tA\n", result);
    }

    @Test
    public void testMissingBuildFailsWithClearError() {
        try {
            TestUtils.runGorPipe(INPUT + " | varnorm_with_build ../tests/data/ref_mini/noSuchChromSeq ref alt");
            Assert.fail("Expected missing build to fail");
        } catch (GorException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Reference build ../tests/data/ref_mini/noSuchChromSeq does not exist"));
        }
    }
    @Test
    public void testFolderWithoutChromosomeFilesFailsWithClearError() {
        // ref_mini exists as a folder but holds no chromosome files, so it is not a usable build.
        try {
            TestUtils.runGorPipe(INPUT + " | varnorm_with_build ../tests/data/ref_mini ref alt");
            Assert.fail("Expected build without chromosome files to fail");
        } catch (GorException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Reference build ../tests/data/ref_mini does not exist or has no chromosome files (expected one of ../tests/data/ref_mini/chr1.txt, ../tests/data/ref_mini/1.txt)"));
        }
    }

    @Test
    public void testBuildValidationProbesChromosomeFileNotFolder() {
        // Object stores (S3, OCI) report a folder without a trailing slash as missing, while the chromosome
        // files RefSeqFromChromSeq reads (<build>/<chrom>.txt) are there. Only the files may be probed.
        String build = "s3://bucket/ref/chromSeq";
        Set<String> existing = Set.of(build + "/chr1.txt");
        Assert.assertTrue(VarNormWithBuild.buildExists(build, existing::contains));
        Assert.assertTrue(VarNormWithBuild.buildExists(build + "/", existing::contains));
    }

    @Test
    public void testBuildValidationAcceptsChromosomeNamesWithoutChrPrefix() {
        String build = "s3://bucket/ref/chromSeq";
        Set<String> existing = Set.of(build + "/1.txt");
        Assert.assertTrue(VarNormWithBuild.buildExists(build, existing::contains));
    }

    @Test
    public void testBuildValidationRejectsFolderWithoutChromosomeFiles() {
        String build = "s3://bucket/ref/chromSeq";
        // Even if the folder itself "exists" (Azure always says so), no chromosome file means no usable build.
        Set<String> existing = Set.of(build, build + "/");
        Assert.assertFalse(VarNormWithBuild.buildExists(build, existing::contains));
    }
}
