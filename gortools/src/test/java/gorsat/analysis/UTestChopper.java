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

import gorsat.Analysis.Chopper;
import org.gorpipe.gor.session.GorSession;
import org.junit.Test;

public class UTestChopper {

    @Test
    public void testChopperWith3Columns() {
        String HEADER_3_COLUMNS = "Chrom\tStart\tStop";
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1\t10000", "chr1\t1000000\t2000000"
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t1\tsta",
                "chr1\t10000\tsto",
                "chr1\t1000000\tsta",
                "chr1\t2000000\tsto"
        };
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        Chopper analysis = new Chopper(0, HEADER_3_COLUMNS, sessionFactory.create());
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS);
    }

    @Test
    public void testChopperWith3ColumnsAndFuzzyFactor1000() {
        String HEADER_3_COLUMNS = "Chrom\tStart\tStop";
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1\t10000", "chr1\t1000000\t2000000"
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t0\tsta",
                "chr1\t11000\tsto",
                "chr1\t999000\tsta",
                "chr1\t2001000\tsto"
        };
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        Chopper analysis = new Chopper(1000, HEADER_3_COLUMNS, sessionFactory.create());
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS);
    }

    @Test
    public void testChopperWith3ColumnsAndFuzzyFactorMinus1000() {
        String HEADER_3_COLUMNS = "Chrom\tStart\tStop";
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t1\t10000", "chr1\t1000000\t2000000"
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t1001\tsta",
                "chr1\t9000\tsto",
                "chr1\t1001000\tsta",
                "chr1\t1999000\tsto"
        };
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        Chopper analysis = new Chopper(-1000, HEADER_3_COLUMNS, sessionFactory.create());
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS);
    }

    @Test
    public void testChopperWith4Columns() {
        String HEADER_4_COLUMNS = "Chrom\tStart\tStop\tExtra";
        String[] INPUT_DATA_4_COLUMNS = {
                "chr1\t1\t10000\tA", "chr1\t1000000\t2000000\tB"
        };
        String[] OUTPUT_DATA_4_COLUMNS = {
                "chr1\t1\tsta\tA",
                "chr1\t10000\tsto\tA",
                "chr1\t1000000\tsta\tB",
                "chr1\t2000000\tsto\tB",
        };
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        Chopper analysis = new Chopper(0, HEADER_4_COLUMNS, sessionFactory.create());
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INPUT_DATA_4_COLUMNS, OUTPUT_DATA_4_COLUMNS);
    }

    @Test
    public void testChopperWithRAngeExtendingBuildSize() {
        String HEADER_3_COLUMNS = "Chrom\tStart\tStop";
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t100000\t1000000", "chr1\t1000000\t350000000", "chrM\t0\t1000000"
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t100000\tsta",
                "chr1\t1000000\tsto",
                "chr1\t1000000\tsta",
                "chr1\t250000000\tsto", // Upper limit on chr1
                "chrM\t0\tsta",
                "chrM\t20000\tsto" // Upper limit on chrM
        };
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        Chopper analysis = new Chopper(0, HEADER_3_COLUMNS, sessionFactory.create());
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS);
    }

    @Test
    public void testChopperWithRAngeExtendingBuildSizeAndInvalidSessionReference() {
        String HEADER_3_COLUMNS = "Chrom\tStart\tStop";
        String[] INPUT_DATA_3_COLUMNS = {
                "chr1\t100000\t1000000", "chr1\t1000000\t350000000", "chrM\t0\t1000000"
        };
        String[] OUTPUT_DATA_3_COLUMNS = {
                "chr1\t100000\tsta",
                "chr1\t1000000\tsto",
                "chr1\t1000000\tsta",
                "chr1\t350000000\tsto", // Upper limit ignored as session is invalid
                "chrM\t0\tsta",
                "chrM\t1000000\tsto" // Upper limit ignored as session is invalid
        };
        GorSession session = new GorSession("");
        Chopper analysis = new Chopper(0, HEADER_3_COLUMNS, session);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INPUT_DATA_3_COLUMNS, OUTPUT_DATA_3_COLUMNS);
    }
}
