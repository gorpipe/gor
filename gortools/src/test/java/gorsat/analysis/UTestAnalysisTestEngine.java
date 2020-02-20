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

package gorsat.analysis;

import gorsat.Analysis.WithIn;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.genetics.Chromosome;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class UTestAnalysisTestEngine {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void testSuccessfulRunWithStringInput() {
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(new WithIn("chr1", 0, Integer.MAX_VALUE),
                AnalysisTestData.DBSNP_ALL_CHROMOSOMES,
                Arrays.copyOfRange(AnalysisTestData.DBSNP_ALL_CHROMOSOMES, 0, 2));
    }

    @Test
    public void testSuccessfulRunWithFileInput() throws IOException {

        File sourceFile = workDir.newFile("source.txt");
        File resultFile = workDir.newFile("result.txt");

        FileUtils.writeStringToFile(sourceFile, String.join("\n", AnalysisTestData.GENES_ALL_CHROMOSOMES), Charset.defaultCharset());
        FileUtils.writeStringToFile(resultFile, String.join("\n", Arrays.copyOfRange(AnalysisTestData.GENES_ALL_CHROMOSOMES, 0, 2)), Charset.defaultCharset());

        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(new WithIn("chr1", 0, Integer.MAX_VALUE),
                sourceFile,
                resultFile);
    }

    @Test(expected = AssertionError.class)
    public void testDifferentResultSizesTooSmall() {
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(new WithIn("chr1", 0, Integer.MAX_VALUE),
                AnalysisTestData.DBSNP_ALL_CHROMOSOMES,
                Arrays.copyOfRange(AnalysisTestData.DBSNP_ALL_CHROMOSOMES, 0, 1));
    }

    @Test(expected = AssertionError.class)
    public void testDifferentResultSizesTooLarge() {
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(new WithIn("chr1", 0, Integer.MAX_VALUE),
                AnalysisTestData.DBSNP_ALL_CHROMOSOMES,
                Arrays.copyOfRange(AnalysisTestData.DBSNP_ALL_CHROMOSOMES, 0, 3));
    }

    @Test(expected = AssertionError.class)
    public void testFailureInResult() {
        String[] result = {
                "chr1\t10179\tC\tCC\trs367896724",
                "chr1\t10250\tA\tC\trs199706087"};
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(new WithIn("chr1", 0, Integer.MAX_VALUE),
                AnalysisTestData.DBSNP_ALL_CHROMOSOMES,
                result);
    }

    @Test
    public void testFailureText() {
        try {
            String[] result = {"chr1\t10179\tC\tCC\trs367896725"};
            AnalysisTestEngine engine = new AnalysisTestEngine();
            engine.run(new WithIn("chr1", 0, Integer.MAX_VALUE),
                    AnalysisTestData.DBSNP_ALL_CHROMOSOMES,
                    result);
        } catch (AssertionError ae) {
            String errorString = ae.toString();
            Assert.assertTrue("Expected error message", errorString.startsWith("org.junit.ComparisonFailure: Row mismatch"));
            Assert.assertTrue("Expected error message", errorString.contains("expected:<...0179\tC\tCC\trs36789672[5]>"));
            Assert.assertTrue("Expected error message", errorString.contains("but was:<...0179\tC\tCC\trs36789672[4]>"));
        }
    }
}