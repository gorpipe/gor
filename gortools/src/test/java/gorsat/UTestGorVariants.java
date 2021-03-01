/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test gor variants command.
 */
public class UTestGorVariants {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @BeforeClass
    public static void setUp() {

    }

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }


    @Test
    public void testGorInvalidPosition() throws Exception {
        Path p = workDirPath.resolve("a.gor");
        Files.write(p, ("Chromo\tPos\tEnd\tQName\tFlag\tMapQ\tCigar\tMD\tMRNM\tMPOS\tISIZE\tSEQ\tQUAL\tTAG_VALUES\n" +
                "chrM\t1\t3\tQ1\t83\t60\t3M\t51T7C41\tMT\t16298\t-271\tTTG\t@BH\tX0=1 X1=0 RG=R1 XG=0 AM=37 NM=2 SM=37 XM=2 XO=0 MQ=60 XT=U RB=hs37d5\n" +
                "chrM\t100\t100\tQ2\t83\t60\t1M\t51T7C41\tMT\t16293\t-276\tT\t@\tX0=1 X1=0 RG=R1 XG=0 AM=37 NM=2 SM=37 XM=2 XO=0 MQ=60 XT=U RB=hs37d5\n" +
                "chrM\t10000001\t10000001\tQ3\t97\t37\t1M\t50T7C42\tMT\t49\t-16321\tT\tH\tX0=1 X1=0 RG=R1 XG=0 AM=37 NM=2 SM=37 XM=2 XO=0 MQ=37 XT=U RB=hs37d5\n").getBytes());

        String[] args = new String[]{"gor " + p.toString() + " | variants -count", "-config", "../tests/data/ref_mini/gor_config.txt"};

        long count = TestUtils.countOpenFiles();
        String result = TestUtils.runGorPipe(args);

        // Checking for valid values at 100 (out of chrM but within the buffer) and 1000001 (out of chrM and out of the buffer)
        Assert.assertEquals("Variants command failed", "Chrom\tPos\tRef\tAlt\tvarCount\n" +
                "chrM\t1\tG\tT\t1\n" +
                "chrM\t1\tGA\tTT\t1\n" +
                "chrM\t1\tGAT\tTTG\t1\n" +
                "chrM\t2\tA\tT\t1\n" +
                "chrM\t2\tAT\tTG\t1\n" +
                "chrM\t3\tT\tG\t1\n" +
                "chrM\t100\tN\tT\t1\n" +
                "chrM\t10000001\tN\tT\t1\n", result);

        Assert.assertFalse("Result contains 0 byte", result.contains("\0"));
        long newCount = TestUtils.countOpenFiles();
        Assert.assertTrue("Open files now " + newCount + ", was " + count, newCount <= count);
    }
}
