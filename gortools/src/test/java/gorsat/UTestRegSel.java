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

package gorsat;

import gorsat.Analysis.RegSelAnalysis;
import gorsat.Commands.CommandParsingResult;
import gorsat.Commands.RegSel;
import org.junit.Assert;
import org.junit.Test;

public class UTestRegSel {

    private RegSelAnalysis getRegSelAnalysis(RegSel obj, String[] args) {
        CommandParsingResult result = obj.processArguments(
                null,
                "",
                args,
                args,
                false,
                "Chrom\tgene_start\tgene_end\tGene_Symbol");

        Assert.assertTrue(result.step() instanceof RegSelAnalysis);
        return (RegSelAnalysis) result.step();
    }

    @Test
    public void regexIsPassedToAnalysisObject() {
        RegSel obj = new RegSel();
        String[] args = {"col1,col2,col3", "Gene_symbol", "test"};
        RegSelAnalysis analysis = getRegSelAnalysis(obj, args);
        Assert.assertEquals("test", analysis.pattern());
    }

    @Test
    public void columnsArePassedToAnalysisObject() {
        RegSel obj = new RegSel();
        String[] args = {"col1,col2,col3", "Gene_symbol", "test"};
        RegSelAnalysis analysis = getRegSelAnalysis(obj, args);
        Assert.assertEquals(3, analysis.columns().length());
        Assert.assertEquals("col1", analysis.columns().apply(0));
        Assert.assertEquals("col2", analysis.columns().apply(1));
        Assert.assertEquals("col3", analysis.columns().apply(2));
    }

    String prefix = "gor ../tests/data/gor/genes.gorz | top 1 | ";

    @Test
    public void whenExpressionIsValidColumnsGetValues() {
        String query = prefix + "regsel col1,col2,col3 Gene_symbol (?<col1>[A-Za-z])(?<col2>[A-Za-z])(?<col3>[A-Za-z])";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\tcol1\tcol2\tcol3\nchr1\t11868\t14412\tDDX11L1\tD\tD\tX\n", res);
    }

    @Test
    public void whenExpressionIsValidWithUnnamedGroupsColumnsGetValues() {
        String query = prefix + "regsel col1,col2,col3 Gene_symbol ([A-Za-z])([A-Za-z])([A-Za-z])";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\tcol1\tcol2\tcol3\nchr1\t11868\t14412\tDDX11L1\tD\tD\tX\n", res);
    }

    @Test
    public void whenExpressionDoesntMatchColumnsAreEmpty() {
        String query = prefix + "regsel col1,col2,col3 Gene_symbol (?<col1>[0-9])(?<col2>[0-9])(?<col3>[0-9])";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\tcol1\tcol2\tcol3\nchr1\t11868\t14412\tDDX11L1\t\t\t\n", res);
    }

    @Test
    public void whenExpressionDoesntMatchColumnsGetGivenEmptyValue() {
        String query = prefix + "regsel col1,col2,col3 Gene_symbol (?<col1>[0-9])(?<col2>[0-9])(?<col3>[0-9]) -e xxx";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\tcol1\tcol2\tcol3\nchr1\t11868\t14412\tDDX11L1\txxx\txxx\txxx\n", res);
    }
}
