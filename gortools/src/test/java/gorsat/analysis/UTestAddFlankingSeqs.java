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

import gorsat.Analysis.AddFlankingSeqs;
import gorsat.process.GorSessionFactory;
import org.junit.Test;

public class UTestAddFlankingSeqs {

    private String[] data = {
            "chr1\t1\t1\t15",
            "chr1\t100\t2\t25"};

    @Test
    public void testDefaultOptions() {
        int[] indices = {1};
        int length = 10;
        String[] result = {
                data[0] + "\t(A)CGTACGTACG",
                data[1] + "\tCGTACGTACG(T)ACGTACGTAC"};

        performTest(length, indices, result);
    }

    @Test
    public void testWithLengthOfThree() {
        int[] indices = {1};
        int length = 3;
        String[] result = {
                data[0] + "\t(A)CGT",
                data[1] + "\tACG(T)ACG"};

        performTest(length, indices, result);
    }

    @Test
    public void testWithLengthOfOneOtherColumn() {
        int[] indices = {2};
        int length = 1;
        String[] result = {
                data[0] + "\t(A)C",
                data[1] + "\tA(C)G"};

        performTest(length, indices, result);
    }

    @Test
    public void testWithLengthOfOneWithTwoOtherColumn() {
        int[] indices = {2,3};
        int length = 1;
        String[] result = {
                data[0] + "\t(A)C\tC(G)T",
                data[1] + "\tA(C)G\tT(A)C"};

        performTest(length, indices, result);
    }

    private void performTest(int length, int[] indices, String[] result) {
        GorSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        AddFlankingSeqs analysis = new AddFlankingSeqs(sessionFactory.create(),length, indices, AnalysisTestEngine.ROW_HEADER);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, data, result);
    }
}
