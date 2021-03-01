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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class UTestVerifyVariant {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void badRef() throws IOException {
        String contents = "Chrom\tPOS\tReference\tCall\tCallCopies\tCallRatio\tDepth\tGL_Call Filter\tPN\n" +
                "chr9\t136754857\tG\tT\t2\t0.8999999761581421\t30\t54\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754883\tC\tA\t1\t0.5\t30\t52\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754884\tA\tAC\t2\t0.9\t30\t47\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754884\tA\tACA\t2\t0.9\t30\t57\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754885\tA\tAC\t1\t0.5\t30\t52\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754885\tC\tAC\t1\t0.5\t30\t52\tPASS\tGSC_FN00110_SAMPLE\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | verifyvariant", file.getAbsolutePath());

        thrown.expect(GorDataException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void goodRef() throws IOException {
        String contents = "Chrom\tPOS\tReference\tCall\tCallCopies\tCallRatio\tDepth\tGL_Call Filter\tPN\n" +
                "chr9\t136754857\tN\tT\t2\t0.8999999761581421\t30\t54\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754883\tN\tA\t1\t0.5\t30\t52\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754884\tN\tAC\t2\t0.9\t30\t47\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754884\tN\tACA\t2\t0.9\t30\t57\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754885\tN\tAC\t1\t0.5\t30\t52\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754885\tN\tAC\t1\t0.5\t30\t52\tPASS\tGSC_FN00110_SAMPLE\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | verifyvariant", file.getAbsolutePath());

        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(contents, result);
    }

    @Test
    public void goodRefAltExceedsLength() throws IOException {
        String contents = "Chrom\tPOS\tReference\tCall\tCallCopies\tCallRatio\tDepth\tGL_Call Filter\tPN\n" +
                "chr9\t136754857\tN\tT\t2\t0.8999999761581421\t30\t54\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754883\tN\tA\t1\t0.5\t30\t52\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754884\tN\tAC\t2\t0.9\t30\t47\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754884\tN\tACAAAACCCAACCCAAACCCCCAAAAACCCACCCA\t2\t0.9\t30\t57\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754885\tN\tAC\t1\t0.5\t30\t52\tPASS\tGSC_FN00110_SAMPLE\n" +
                "chr9\t136754885\tN\tAC\t1\t0.5\t30\t52\tPASS\tGSC_FN00110_SAMPLE\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | verifyvariant", file.getAbsolutePath());

        thrown.expect(GorDataException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void noRefColumn() throws IOException {
        String contents = "Chrom\tPOS\tCall\tCallCopies\tCallRatio\tDepth\tGL_Call Filter\tPN\n" +
                "chr9\t136754857\tT\t2\t0.8999999761581421\t30\t54\tPASS\tGSC_FN00110_SAMPLE\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | verifyvariant", file.getAbsolutePath());

        thrown.expect(GorParsingException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void refColumnNotInDefaultLocation() throws IOException {
        String contents = "Chrom\tPOS\tCall\tCallCopies\tCallRatio\tReference\tDepth\tGL_Call Filter\tPN\n" +
                "chr9\t136754857\tT\t2\t0.8999999761581421\tN\t30\t54\tPASS\tGSC_FN00110_SAMPLE\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | verifyvariant", file.getAbsolutePath());

        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(contents, result);
    }

    @Test
    public void refColumnNotInDefaultLocationWithDifferentName() throws IOException {
        String contents = "Chrom\tPOS\tCall\tCallCopies\tCallRatio\tBingoReference\tDepth\tGL_Call Filter\tPN\n" +
                "chr9\t136754857\tT\t2\t0.8999999761581421\tN\t30\t54\tPASS\tGSC_FN00110_SAMPLE\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | verifyvariant -ref BingoReference", file.getAbsolutePath());

        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(contents, result);
    }

    @Test
    public void altColumnNotInDefaultLocation() throws IOException {
        String contents = "Chrom\tPOS\tCallCopies\tCallRatio\tReference\tAlt\tDepth\tGL_Call Filter\tPN\n" +
                "chr9\t136754857\t2\t0.8999999761581421\tN\tT\t30\t54\tPASS\tGSC_FN00110_SAMPLE\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | verifyvariant", file.getAbsolutePath());

        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(contents, result);
    }

    @Test
    public void altColumnNotInDefaultLocationWithDifferentName() throws IOException {
        String contents = "Chrom\tPOS\tCallCopies\tCallRatio\tReference\tBingoAlt\tDepth\tGL_Call Filter\tPN\n" +
                "chr9\t136754857\t2\t0.8999999761581421\tN\tT\t30\t54\tPASS\tGSC_FN00110_SAMPLE\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | verifyvariant -alt BingoAlt", file.getAbsolutePath());

        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(contents, result);
    }
}
