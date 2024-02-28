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


import gorsat.Analysis.SegWhereAnalysis;
import gorsat.Commands.RowHeader;

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Test;

public class UTestSegWhere {

    private final RowHeader header = RowHeader.apply("Chrom\tPOS", "S\tI");
    private final RowHeader header_with_string = RowHeader.apply("Chrom\tPOS\tVAL", "S\tI\tS");
    private final RowHeader header_with_number = RowHeader.apply("Chrom\tPOS\tVAL", "S\tI\tD");

    @Test
    public void testSingleSegmentAllTrue() {
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1",
                "chr1\t10",
                "chr1\t100",
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t1\t100"
        };

        performTest(INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS, header, "pos > 0");
    }


    @Test
    public void testComplexCondition() {
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1",
                "chr1\t100",
                "chr1\t1000",
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t1\t100",
        };

        performTest(INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS, header, "pos > 0 and pos < 500");
    }

    @Test
    public void testNewChr() {
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1",
                "chr1\t10",
                "chr2\t12",
                "chr2\t20",
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t1\t10",
                "chr2\t12\t20"
        };

        performTest(INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS, header, "pos > 0");
    }

    @Test
    public void testWithCondition() {
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1\tb",
                "chr1\t4\ta",
                "chr1\t10\ta",
                "chr1\t14\ta",
                "chr1\t30\tb",
                "chr1\t200\tb",
                "chr1\t220\ta",
                "chr1\t230\ta",
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t4\t14",
                "chr1\t220\t230"
        };

        performTest(INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS, header_with_string, "val == 'a'");
    }

    @Test
    public void testWithNumericalCondition() {
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1\t0",
                "chr1\t2\t1",
                "chr1\t4\t2",
                "chr1\t10\t3",
                "chr1\t14\t4",
                "chr1\t30\t5",
                "chr1\t200\t6",
                "chr1\t220\t7",
                "chr1\t230\t8",
                "chr2\t1\t0",
                "chr2\t10\t1",
                "chr2\t40\t2",
                "chr2\t80\t4",
                "chr2\t160\t5",
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t2\t14",
                "chr2\t10\t80"
        };

        performTest(INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS, header_with_number, "val >= 1 and val < 5");
    }

    @Test
    public void testWithNumericalConditionHalfStartAndHalfEnd() {
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1\t0",
                "chr1\t2\t1",
                "chr1\t4\t2",
                "chr1\t10\t3",
                "chr1\t14\t4",
                "chr1\t30\t5",
                "chr1\t200\t6",
                "chr1\t220\t7",
                "chr1\t230\t8",
                "chr2\t1\t0",
                "chr2\t10\t1",
                "chr2\t40\t2",
                "chr2\t80\t4",
                "chr2\t160\t5",
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t7\t22",
                "chr2\t60\t120"
        };

        performTest(INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS, header_with_number, "val >= 3 and val < 5", -1, true, true);
    }

    @Test
    public void testWithNumericalConditionAndMinSeg() {
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1\t0",
                "chr1\t2\t1",
                "chr1\t4\t2",
                "chr1\t10\t3",
                "chr1\t14\t4",
                "chr1\t30\t5",
                "chr1\t200\t6",
                "chr1\t220\t7",
                "chr1\t230\t8",
                "chr2\t1\t0",
                "chr2\t10\t1",
                "chr2\t40\t2",
                "chr2\t80\t4",
                "chr2\t160\t5",
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr2\t60\t120"
        };

        performTest(INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS, header_with_number, "val >= 3 and val < 5", 40, true, true);
    }


    private void performTest(String[] input, String[] output, RowHeader inputHeader, String whereStatement, int minseg, boolean startHalf, boolean endHalf) {
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();


        try(var session = sessionFactory.create()) {
            SegWhereAnalysis analysis = new SegWhereAnalysis(session.getGorContext(), minseg, startHalf, endHalf,
                    whereStatement, inputHeader.toString(), false);
            AnalysisTestEngine engine = new AnalysisTestEngine();
            engine.run(analysis, input, output, inputHeader);
        }

    }

    private void performTest(String[] input, String[] output, RowHeader inputHeader, String whereStatement) {
        performTest(input, output, inputHeader, whereStatement, -1, false, false);
    }

    @Test
    public void testInvalidWhereCondition() {
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1",
                "chr1\t10",
                "chr1\t100",
        };
        String[] OUTPUT_DATA_3_COLUMNS = {};

        Assert.assertThrows(GorParsingException.class, () -> performTest(INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS, header, "foo > 0"));
    }

    @Test
    public void testNonBoolWhereCondition() {
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1",
                "chr1\t10",
                "chr1\t100",
        };
        String[] OUTPUT_DATA_3_COLUMNS = {};

        Assert.assertThrows(GorParsingException.class, () -> performTest(INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS, header, "foo + 10"));
    }
}
