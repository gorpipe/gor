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

import gorsat.Analysis.CigarVarSegs;
import org.gorpipe.exceptions.GorDataException;
import org.junit.Test;

public class UTestCigarVarSegs {

    private static final int[] NO_GROUPING = {};

    @Test
    public void testModificationCigarSegNoGrouping() {
        String[] input = {"chr1\t1\t14M1D6M\tCGTACGTACGTACGTACGTACGTACGTACGTA\t...."};
        String[] output = {
                "chr1\t1\tCGTACGTACGTACG\t1",
                "chr1\t16\tTACGTA\t1"};

        performTest(input, output, NO_GROUPING, false, false);
    }

    @Test
    public void testModificationCigarSegWiGrouping() {
        String[] input = {"chr1\t1\t14M1D6M\tCGTACGTACGTACGTACGTACGTACGTACGTA\t...."};
        String[] output = {
                "chr1\t1\tCGTACGTACGTACG\t1\tchr1\t14M1D6M",
                "chr1\t16\tTACGTA\t1\tchr1\t14M1D6M"};
        int[] groups = {0,2};

        performTest(input, output, groups, false, false);
    }

    @Test(expected = GorDataException.class)
    public void testInvalidCigarSeg() {
        String[] input = {"chr1\t1\t14J1K6L\tCGTACGTACGTACGTACGTACGTACGTACGTA\t...."};
        String[] output = {};

        performTest(input, output, NO_GROUPING, false, false);
    }

    @Test(expected = GorDataException.class)
    public void testInvalidCigarNoCharSeg() {
        String[] input = {"chr1\t1\t14\tCGTACGTACGTACGTACGTACGTACGTACGTA\t...."};
        String[] output = {};

        performTest(input, output, NO_GROUPING, false, false);
    }

    @Test(expected = StringIndexOutOfBoundsException.class) // This should be GorDataException
    public void testShortReference() {
        String[] input = {"chr1\t1\t14M1D6M\tCGTAC\t...."};
        String[] output = {};

        performTest(input, output, NO_GROUPING, false, false);
    }

    @Test
    public void testShortQuality() {
        String[] input = {"chr1\t1\t4M1D2M\tCGTACGTACGTACGTACGTACGTACGTACGTA\t...."};
        String[] output = {
                "chr1\t1\tA\tC\t0\t13\t1",
                "chr1\t2\tC\tG\t1\t13\t1",
                "chr1\t3\tG\tT\t2\t13\t1",
                "chr1\t4\tT\tA\t3\t13\t1",
                "chr1\t5\tA\t\t4\t0\t1"};

        performTest(input, output, NO_GROUPING, true, false);
    }

    @Test
    public void testModificationCigarSegNoGroupingWithRef() {
        // "Chrom\tPos\tRef\tBase\tReadPos\tBaseQual\tMDI"
        String[] input = {"chr1\t1\t14M1D6M\tCGTACGTACGTACGTACGTACGTACGTACGTA\t....,,,,aaaaaaaaaaaaaaaaaaaaaa"};
        String[] output = {
                "chr1\t1\tA\tC\t0\t13\t1",
                "chr1\t2\tC\tG\t1\t13\t1",
                "chr1\t3\tG\tT\t2\t13\t1",
                "chr1\t4\tT\tA\t3\t13\t1",
                "chr1\t5\tA\tC\t4\t11\t1",
                "chr1\t6\tC\tG\t5\t11\t1",
                "chr1\t7\tG\tT\t6\t11\t1",
                "chr1\t8\tT\tA\t7\t11\t1",
                "chr1\t9\tA\tC\t8\t64\t1",
                "chr1\t10\tC\tG\t9\t64\t1",
                "chr1\t11\tG\tT\t10\t64\t1",
                "chr1\t12\tT\tA\t11\t64\t1",
                "chr1\t13\tA\tC\t12\t64\t1",
                "chr1\t14\tC\tG\t13\t64\t1",
                "chr1\t15\tG\t\t14\t64\t1"}; // Stops on delete, as the rest is a match

        performTest(input, output, NO_GROUPING, true, false);
    }

    @Test
    public void testModificationCigarSegNoGroupingWithOuputBases() {
        // "Chrom\tPos\tRef\tBase\tReadPos\tBaseQual\tMDI"
        String[] input = {"chr1\t1\t14M1D6X\tCGTACGTACGTACGTACGTACGTACGTACGTA\t....,,,,aaaaaaaaaaaaaaaaaaaaaa"};
        String[] output = {
                "chr1\t1\tA\tC\t0\t13\tM",
                "chr1\t2\tC\tG\t1\t13\tM",
                "chr1\t3\tG\tT\t2\t13\tM",
                "chr1\t4\tT\tA\t3\t13\tM",
                "chr1\t5\tA\tC\t4\t11\tM",
                "chr1\t6\tC\tG\t5\t11\tM",
                "chr1\t7\tG\tT\t6\t11\tM",
                "chr1\t8\tT\tA\t7\t11\tM",
                "chr1\t9\tA\tC\t8\t64\tM",
                "chr1\t10\tC\tG\t9\t64\tM",
                "chr1\t11\tG\tT\t10\t64\tM",
                "chr1\t12\tT\tA\t11\t64\tM",
                "chr1\t13\tA\tC\t12\t64\tM",
                "chr1\t14\tC\tG\t13\t64\tM",
                "chr1\t15\tG\t\t14\t64\tD",
                "chr1\t16\tT\tT\t14\t64\tM",
                "chr1\t17\tA\tA\t15\t64\tM",
                "chr1\t18\tC\tC\t16\t64\tM",
                "chr1\t19\tG\tG\t17\t64\tM",
                "chr1\t20\tT\tT\t18\t64\tM",
                "chr1\t21\tA\tA\t19\t64\tM"};

        performTest(input, output, NO_GROUPING, true, true);
    }

    @Test
    public void testAllCigarCharsNoGroupingWithRefAndOuputBases() {
        // "Chrom\tPos\tRef\tBase\tReadPos\tBaseQual\tMDI"
        String[] input = {"chr1\t1\t2M2N2X2S2M2IPH\tCGTACGTACGTACGTACGTACGTACGTACGTA\t....,,,,aaaaaaaaaaaaaaaaaaaaaa"};
        String[] output = {
                "chr1\t1\tA\tC\t0\t13\tM",
                "chr1\t2\tC\tG\t1\t13\tM",
                "chr1\t5\tA\tT\t2\t13\tM",
                "chr1\t6\tC\tA\t3\t13\tM",
                "chr1\t7\tG\tT\t6\t11\tM",
                "chr1\t8\tT\tA\t7\t11\tM",
                "chr1\t8\t\tC\t8\t64\tI",
                "chr1\t9\t\tG\t9\t64\tI"};

        performTest(input, output, NO_GROUPING, true, true);
    }

    private void performTest(String[] input, String[] output, int[] grouping, boolean useRef, boolean ouputBases) {
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        CigarVarSegs analysis = new CigarVarSegs(2, grouping,
                useRef, ouputBases, 3, 4, sessionFactory.create());
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
