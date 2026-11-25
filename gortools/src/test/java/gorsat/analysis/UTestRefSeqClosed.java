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

import gorsat.Analysis.CalcAnalysis;
import gorsat.Analysis.VerifyVariantAnalysis;
import gorsat.Commands.RowHeader;
import gorsat.parser.ParseArith;
import org.gorpipe.gor.driver.providers.stream.datatypes.cram.CramIterator;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.model.gor.iterators.RefSeq;
import org.gorpipe.model.gor.iterators.RefSeqFactory;
import org.gorpipe.model.gor.iterators.RefSeqRotating;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Every RefSeq created from the project context must be closed by its owner. An unclosed RefSeqFromChromSeq
 * holds open chromSeq file handles until the GC finalizer reclaims them (ENGKNOW-4144).
 */
public class UTestRefSeqClosed {

    @Test
    public void verifyVariantAnalysisClosesRefSeq() {
        TrackingRefSeqFactory factory = new TrackingRefSeqFactory();
        VerifyVariantAnalysis analysis = new VerifyVariantAnalysis(createSession(factory, ""), 2, 3, 100);

        analysis.finish();

        factory.assertAllClosed(1);
    }

    @Test
    public void parseArithClosesRefSeq() {
        TrackingRefSeqFactory factory = new TrackingRefSeqFactory();
        ParseArith filter = createFilter(factory);
        filter.compileCalculation("refbase(chrom,pos)");
        Assert.assertEquals("A", filter.evalStringFunction(RowObj.apply("chr1\t1")));

        filter.close();

        factory.assertAllClosed(1);
    }

    @Test
    public void parseArithClosesRefSeqForBuild() {
        TrackingRefSeqFactory factory = new TrackingRefSeqFactory();
        ParseArith filter = createFilter(factory);
        filter.compileCalculation("refbases_with_build(chrom,pos,pos,'other_build')");
        Assert.assertEquals("A", filter.evalStringFunction(RowObj.apply("chr1\t1")));

        filter.close();

        factory.assertAllClosed(2);
    }

    @Test
    public void calcAnalysisClosesRefSeqsWhenHeaderIsSetAgain() {
        TrackingRefSeqFactory factory = new TrackingRefSeqFactory();
        CalcAnalysis analysis = new CalcAnalysis(createSession(factory, "").getGorContext(), false,
                new String[]{"refbase(chrom,pos)"}, "chrom\tpos", new String[]{"ref"});
        RowHeader header = RowHeader.apply(new String[]{"chrom", "pos"}, new String[]{"S", "I"});

        analysis.setRowHeader(header);
        analysis.setRowHeader(header);
        analysis.finish();

        factory.assertAllClosed(2);
    }

    @Test
    public void cramIteratorClosesRefSeq() {
        TrackingRefSeqFactory factory = new TrackingRefSeqFactory();
        CramIterator iterator = new CramIterator(null, null, null);
        iterator.init(createSession(factory, "gor_config.txt"));

        iterator.close();

        factory.assertAllClosed(1);
    }

    private static ParseArith createFilter(TrackingRefSeqFactory factory) {
        ParseArith filter = new ParseArith(null);
        filter.setContext(createSession(factory, "").getGorContext(), false);
        filter.setColumnNamesAndTypes(new String[]{"chrom", "pos"}, new String[]{"S", "I"});
        return filter;
    }

    private static GorSession createSession(RefSeqFactory factory, String configFile) {
        return new AnalysisTestSessionFactory(factory, configFile).create();
    }

    private static class TrackingRefSeq extends RefSeqRotating {
        boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class TrackingRefSeqFactory extends RefSeqFactory {
        final List<TrackingRefSeq> created = new ArrayList<>();

        @Override
        public RefSeq create() {
            TrackingRefSeq refSeq = new TrackingRefSeq();
            created.add(refSeq);
            return refSeq;
        }

        @Override
        public RefSeq create(String altPath) {
            return create();
        }

        void assertAllClosed(int expectedCount) {
            Assert.assertEquals("RefSeq instances created", expectedCount, created.size());
            for (TrackingRefSeq refSeq : created) {
                Assert.assertTrue("RefSeq not closed", refSeq.closed);
            }
        }
    }
}
