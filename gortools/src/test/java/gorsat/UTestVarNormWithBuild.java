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
import org.junit.Assert;
import org.junit.Test;

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
        // No -config: the project has no usable default build, so plain varnorm cannot shift the deletion.
        String plain = TestUtils.runGorPipe(INPUT + " | varnorm ref alt");
        Assert.assertNotEquals("chrom\tpos\tref\talt\nchr1\t10003\tAC\tA\n", plain);

        String withBuild = TestUtils.runGorPipe(INPUT + " | varnorm_with_build " + BUILD + " ref alt");
        Assert.assertEquals("chrom\tpos\tref\talt\nchr1\t10003\tAC\tA\n", withBuild);
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
}
