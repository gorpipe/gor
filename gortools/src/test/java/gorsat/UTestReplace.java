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

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UTestReplace {
    String prefix = "gor ../tests/data/gor/genes.gorz | top 1 | ";
    String norPrefix = "nor ../tests/data/gor/genes.gorz | top 1 | ";

    @Rule
    public ExpectedException expected = ExpectedException.none();


    @Test
    public void replaceSingleColumn() {
        String query = prefix + "replace Gene_symbol if(Gene_symbol='DDX11L1', 'xxx', Gene_symbol)";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\nchr1\t11868\t14412\txxx\n", res);
    }

    @Test
    public void replaceSingleColumnWhenTypesMismatch() {
        String query = prefix + "replace gene_end if(gene_end!='NA', 42, gene_end)";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\nchr1\t11868\t42\tDDX11L1\n", res);
    }

    @Test
    public void replaceOnChromShouldThrowError() {
        String query = prefix + "replace chrom 'xxx'";
        expected.expect(GorParsingException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void replaceOnChromShouldBeAllowedWhenUsingNor() {
        String query = norPrefix + "replace chrom 'xxx'";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("ChromNOR\tPosNOR\tChrom\tgene_start\tgene_end\tGene_Symbol\n" +
                "chrN\t0\txxx\t11868\t14412\tDDX11L1\n", res);
    }

    @Test
    public void replaceOnPosShouldThrowError() {
        String query = prefix + "replace pos 42";
        expected.expect(GorParsingException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void replaceOnPosShouldBeAllowedWhenUsingNor() {
        String query = norPrefix + "replace gene_start 42";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("ChromNOR\tPosNOR\tChrom\tgene_start\tgene_end\tGene_Symbol\n" +
                "chrN\t0\tchr1\t42\t14412\tDDX11L1\n", res);
    }

    @Test
    public void replaceStar() {
        String query = prefix + "replace * 42";
        String res = TestUtils.runGorPipe(query);
        String expected = "Chrom\tgene_start\tgene_end\tGene_Symbol\n" +
                "chr1\t11868\t42\t42\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void replaceStarWhenUsingNor() {
        String query = norPrefix + "replace * 42";
        String res = TestUtils.runGorPipe(query);
        String expected = "ChromNOR\tPosNOR\tChrom\tgene_start\tgene_end\tGene_Symbol\n" +
                "chrN\t0\t42\t42\t42\t42\n";
        Assert.assertEquals(expected, res);
    }
}
