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

import gorsat.Analysis.TopN;
import gorsat.Commands.Analysis;
import gorsat.process.GenericSessionFactory;
import gorsat.process.ProcessIteratorAdaptor;
import org.apache.commons.lang.SystemUtils;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.session.GorContext;
import gorsat.process.GorSessionFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by sigmar on 13/10/15.
 */
public class UTestGorPipeStepIteratorAdaptor {
    private static final Logger log = LoggerFactory.getLogger(UTestGorPipeStepIteratorAdaptor.class);

    /**
     * Tests the pipeStepIteratorAdaptor class
     */
    @Test
    public void testProcessIteratorAdaptor() throws IOException {
        // head does not exist on windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        GorSessionFactory factory = new GenericSessionFactory();
        GorContext context = factory.create().getGorContext();
        GenomicIterator inputSource = new DynIterator.DynamicRowSource("gor ../tests/data/gor/genes.gorz", context, true);
        Analysis an = new TopN(1000000);
        ProcessIteratorAdaptor pia = new ProcessIteratorAdaptor(context, "head -n 5", "head", inputSource, an, "Chrom\tgene_start\tgene_end\tGene_Symbol", false, Optional.empty(), true, false);

        int count = 0;
        while (pia.hasNext()) {
            count++;
            log.info("{}", pia.next());
        }
        pia.close();

        Assert.assertEquals(4, count);
    }

    @Test
    public void testPipeStepIteratorAdaptorThreadSafety() {
        // grep does not exist on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String query = "gor ../tests/data/gor/genes.gorz | join -segseg <(../tests/data/gor/genes.gorz | top 10000) | cmd {grep chr} | verifyorder | group chrom -count";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals("Incorrect number of lines received from gor command server", 25, count);
    }
}
