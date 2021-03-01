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

package gorsat.analysis;

import gorsat.Analysis.MergeGenotypes;
import org.junit.Test;

public class UTestMergeGenotypes {

    @Test
    public void mergeGenotypesWithREfAndAlleleNormalized() {
        String header = "Chrom\tPos\tRef\tAlt";
        String[] input = {
                "chr1\t1\tA\tC",
                "chr1\t10\tAGC\tCCG"};

        String[] output = {
                "chr1\t1\tA\tC",
                "chr1\t10\tAGC\tCCG"};

        performTest(header, input, output, false, true);
    }


    @Test
    public void mergeGenotypesWithREfAndAlleleNonNormalized() {
        String header = "Chrom\tPos\tRef\tAlt";
        String[] input = {
                "chr1\t1\tA\tC",
                "chr1\t10\tAGC\tCCG"};

        String[] output = {
                "chr1\t1\tACGTACGTAAGC\tCCGTACGTAAGC",
                "chr1\t1\tACGTACGTAAGC\tACGTACGTACCG"};

        performTest(header, input, output, false, false);
    }

    @Test
    public void mergeGenotypesWithREfAndAlleleNonNormalizedExceedingMergeSpan() {
        String header = "Chrom\tPos\tRef\tAlt";
        String[] input = {
                "chr1\t1\tA\tC",
                "chr1\t10\tAGC\tCCG",
                "chr1\t1000\tG\tT",
                "chr1\t1080\tG\tG"};

        String[] output = {
                "chr1\t1\tACGTACGTAAGC\tCCGTACGTAAGC",
                "chr1\t1\tACGTACGTAAGC\tACGTACGTACCG",
                "chr1\t1000\tGACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGG\tTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGG",
                "chr1\t1000\tGACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGG\tGACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGG"};

        performTest(header, input, output, false, false);
    }

    @Test
    public void mergeGenotypesWithREfAndAlleleNormalizedExceedingMergeSpan() {
        String header = "Chrom\tPos\tRef\tAlt";
        String[] input = {
                "chr1\t1\tA\tC",
                "chr1\t10\tAGC\tCCG",
                "chr1\t1000\tG\tT",
                "chr1\t1080\tG\tG"};

        String[] output = {
                "chr1\t1\tACGTACGTAAGC\tCCGTACGTAAGC",
                "chr1\t1\tACGTACGTAAGC\tACGTACGTACCG",
                "chr1\t1000\tGACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGG\tTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGG",
                "chr1\t1000\tGACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGG\tGACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGG"};

        performTest(header, input, output, false, false);
    }

    @Test
    public void mergeGenotypesWithREfAndAlleleNormalizedMultiAllele() {
        String header = "Chrom\tPos\tRef\tAlt";
        String[] input = {
                "chr1\t1\tA\tC,TTT",
                "chr1\t10\tAGC\tCCG,ACG"};

        String[] output = {
                "chr1\t1\tA\tC,TTT",
                "chr1\t1\tACGTACGTAAGC\tACGTACGTACCG,ACGTACGTAACG"};

        performTest(header, input, output, false, true);

    }

    @Test
    public void mergeGenotypesWithREfAndAlleleNonNormalizedSameAsReference() {
        String header = "Chrom\tPos\tRef\tAlt";
        String[] input = {
                "chr1\t1\tCCGTACGTACGT\tCCGTACGTACGT",
                "chr1\t10\tCGT\tCGT"};

        String[] output = {
                "chr1\t1\tCCGTACGTACGT\tCCGTACGTACGT",
                "chr1\t1\tCCGTACGTACGT\tCCGTACGTACGT"};

        performTest(header, input, output, false, false);
    }

    @Test
    public void mergeSegmentGenotypesWithREfAndAlleleNormalized() {
        String header = "Chrom\tPos\tRef\tAlt";
        String[] input = {
                "chr1\t1\t2\tA\tC",
                "chr1\t10\t13\tAGC\tCCG"};

        String[] output = {
                "chr1\t1\t2\tA\tC",
                "chr1\t9\t12\tAAGC"}; // This cannot be true, different number of output columns!

        performTest(header, input, output, true, true);
    }

    private void performTest(String header, String[] input, String[] output, boolean segment, boolean normalize) {
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        MergeGenotypes analysis = new MergeGenotypes(2, 3, segment, header, normalize, 100, sessionFactory.create());
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
