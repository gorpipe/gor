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

import gorsat.Commands.Analysis;
import gorsat.Analysis.ColTypeAnalysis;
import gorsat.Analysis.InferColumnTypes;
import gorsat.Commands.RowHeader;
import org.junit.Test;

public class UTestColTypeAnalysis {
    @Test
    public void colTypeChromPosOnly() {
        RowHeader header = new RowHeader(new String[]{"Chrom", "Pos"}, new String[]{"S", "I"});
        String[] input = {"chr1\t1"};
        String[] output = {"chr1\t1"};

        performTest(input, output, header);
    }

    @Test
    public void colTypeChromPosData() {
        RowHeader header = new RowHeader(new String[]{"Chrom", "Pos", "data"}, new String[]{"S", "I", "S"});
        String[] input = {"chr1\t1\tabc"};
        String[] output = {"chr1\t1\tS(abc)"};

        performTest(input, output, header);
    }

    @Test
    public void colTypeMultipleColumns() {
        RowHeader header = new RowHeader(new String[]{"Chrom", "Pos", "data1", "data2", "data3", "data4"}, new String[]{"S", "I", "S", "I", "D", "I"});
        String[] input = {"chr1\t1\tabc\t123\t3.14\t"};
        String[] output = {"chr1\t1\tS(abc)\tI(123)\tD(3.14)\tI()"};

        performTest(input, output, header);
    }

    private void performTest(String[] input, String[] output, RowHeader header) {
        Analysis analysis = new InferColumnTypes();
        analysis.$bar(new ColTypeAnalysis());

        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output, header);
    }
}
