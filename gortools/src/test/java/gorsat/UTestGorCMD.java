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
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sigmar on 30/10/15.
 */
public class UTestGorCMD {
    private File tempFile;
    private Path tempFilePath;
    private String tempFileCanonicalPath;

    private File pnsTxt;
    private String pnsTxtCanonicalPath;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "allowedCmds.txt",
                "head -h\t[head]\n" +
                        "top10 -h\t[head -n 10]\n" +
                        "fullpathhead -h\t[/usr/bin/head]\n" +
                        "samtools -ssam\t[samtools view -h]\n" +
                        "users\t[sql select * from users]\n" +
                        "mydate -c   [date]\n"
        );

        tempFilePath = tempFile.toPath();
        tempFileCanonicalPath = tempFile.getCanonicalPath();
        pnsTxt = FileTestUtils.createPNTxtFile(workDir.getRoot());
        pnsTxtCanonicalPath = pnsTxt.getCanonicalPath();
    }

    static void doTest(String whitelistedCmdFile, String gor, String file, int expected) {
        int count = TestUtils.runGorPipeCountWithWhitelist(gor + " " + file + " | top 10", Paths.get(whitelistedCmdFile));
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testCmdWithNestedQuery() {
        int count = TestUtils.runGorPipeCountWithWhitelist("gor ../tests/data/gor/genes.gorz | top 0 | cmd {cat <(gor ../tests/data/gor/genes.gor | top 5) <(cmd {tail -n +100 ../tests/data/gor/genes.gor} | top 4)}",
                tempFile.toPath());
        Assert.assertEquals(10, count);
    }

    @Test
    public void testGorCalcSystem() {
        int count = TestUtils.runGorPipeCountWithWhitelist("gor ../tests/data/gor/genes.gorz | top 10 | calc sys system('date')",
                tempFilePath);
        Assert.assertEquals(10, count);
    }

    @Test
    public void testGorCalcWhitelistedSystem() {
        int count = TestUtils.runGorPipeCountWithWhitelist("gor ../tests/data/gor/genes.gorz | top 10 | calc sys system('mydate')",
                tempFilePath);
        Assert.assertEquals(10, count);
    }

    @Test
    public void testCmdWithFilter() {
        Path whitelistPath = tempFilePath;
        String query = "cmd -p chr22 -f genes.gor {cat ../tests/data/gor/#(F:filter)} | group chrom -count";
        int count = TestUtils.runGorPipeCountWithWhitelist(query, whitelistPath);
        Assert.assertEquals(25, count);
    }

    @Test
    public void testGorCMDCalc() {
        doTest(tempFileCanonicalPath, "gor", "../tests/data/gor/dbsnp_test.gorz | calc m 'm' | cmd -h {head -n 5}", 5);
    }

    @Test
    public void testGorCMD() {
        doTest(tempFileCanonicalPath, "gor", "../tests/data/gor/dbsnp_test.gorz | cmd -h {head -n 5}", 5);
    }

    @Test
    public void testGorCMDWithParameters() {
        doTest(tempFileCanonicalPath, "gor", "../tests/data/gor/dbsnp_test.gorz | top10", 10);
    }

    @Test
    public void testGorCMDWhiteListed() throws IOException {
        doTest(tempFile.getCanonicalPath(), "gor", "../tests/data/gor/dbsnp_test.gorz | head -n 5", 5);
    }

    @Test
    public void testGorCMDWhiteListedWithFullPath() {
        doTest(tempFileCanonicalPath, "gor", "../tests/data/gor/dbsnp_test.gorz | fullpathhead -n 5", 5);
    }

    @Test
    @Ignore("This test is not valid after removing the security manager. Ignored, Simmi told me to do it.")
    public void testDisallowedGorCMD() {
        boolean exceptionOccurred = false;
        try {
            doTest(tempFileCanonicalPath, "gor", "../tests/data/gor/dbsnp_test.gorz | cmd -h {tail -n 5}", 5);
        } catch (Exception e) {
            exceptionOccurred = true;
        }

        //Should get exception
        Assert.assertTrue(exceptionOccurred);
    }

    @Test
    public void testGorCMDNotWhitelisted() {
        boolean exceptionOccurred = false;
        try {
            doTest(tempFileCanonicalPath, "gor", "../tests/data/gor/dbsnp_test.gorz | tail -n 5", 5);
        } catch (Exception e) {
            exceptionOccurred = true;
        }

        //Should get exception
        Assert.assertTrue(exceptionOccurred);
    }

    @Test
    public void testNorCMD() {
        doTest(tempFileCanonicalPath, "nor", pnsTxtCanonicalPath + " | cmd -h {head -n 1}", 1);
        doTest(tempFileCanonicalPath, "nor", pnsTxtCanonicalPath + " | cmd -h -s 1 {head -n 1}", 0);
        doTest(tempFileCanonicalPath, "nor", pnsTxtCanonicalPath + " | head -n 1", 1);
    }

    @Ignore("Add to /etc/sudoers: [user] ALL=(ALL) NOPASSWD: /usr/sbin/chroot")
    @Test
    public void testGorChroot() {
        doTest(tempFileCanonicalPath, "gor", "../tests/data/gor/dbsnp_test.gorz | cmd -h {sudo chroot [PROJECT_ROOT] head -n 5}", 5);
    }
}
