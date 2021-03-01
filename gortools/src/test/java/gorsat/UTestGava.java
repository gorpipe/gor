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

import gorsat.Analysis.VaastAnalysis;
import gorsat.Commands.CommandInfo;
import gorsat.Commands.CommandParsingResult;
import gorsat.process.GenericSessionFactory;
import gorsat.process.GorPipeCommands;
import gorsat.process.GorSessionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by sigmar on 12/06/2017.
 */
public class UTestGava {

    @Before
    public void registerCommands() {
        GorPipeCommands.register();
    }

    @Test
    public void testGavaParsing1() {
        CommandInfo info = GorPipeCommands.getInfo("GAVA");
        GorSessionFactory factory = new GenericSessionFactory();

        CommandParsingResult result =  info.init(factory.create().getGorContext(), false, "Chrom\tstart\tstop\tGENE_SYMBOL\tPOS\tALT\tREF\tPN\tCALLCOPIES\tPHASE\tSCORE",
                "", "100 -caselist PN1,PN2 -ctrllist PN3,PN4".split(" "), null );

        Assert.assertNotNull(result.step());

        VaastAnalysis vaarStep = (VaastAnalysis)result.step();

        int[] expectedResult1 = {0,1,2,3};
        Assert.assertArrayEquals( expectedResult1,  vaarStep.geneSelArray());

        int[] expectedResult2 = {0,4, 6, 5};
        Assert.assertArrayEquals( expectedResult2,  vaarStep.varIdArray());

        int[] expectedResult3 = {7 ,8, 9, 10};
        Assert.assertArrayEquals( expectedResult3,  vaarStep.varSelArray());
    }

    @Test
    public void testGavaParsing2() {
        CommandInfo info = GorPipeCommands.getInfo("GAVA");
        GorSessionFactory factory = new GenericSessionFactory();

        CommandParsingResult result =  info.init(factory.create().getGorContext(), false, "Chrom\tstart\tstop\tGENE_SYMBOL\tPOS\tALT\tREF\tPN\tCALLCOPIES\tSCORE",
                "", "100 -caselist PN1,PN2 -ctrllist PN3,PN4".split(" "), null );

        Assert.assertNotNull(result.step());

        VaastAnalysis vaarStep = (VaastAnalysis)result.step();

        int[] expectedResult1 = {0,1,2,3};
        Assert.assertArrayEquals( expectedResult1,  vaarStep.geneSelArray());

        int[] expectedResult2 = {0,4, 6, 5};
        Assert.assertArrayEquals( expectedResult2,  vaarStep.varIdArray());

        int[] expectedResult3 = {7 ,8, 10, 9};
        Assert.assertArrayEquals( expectedResult3,  vaarStep.varSelArray());
    }
}
