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

package gorsat.parser;

import gorsat.TestUtils;
import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestGenomicFunctions {
    @Test
    public void testHaplDiff() {
        TestUtils.assertCalculated("hapldiff('', '')", "0");
        TestUtils.assertCalculated("hapldiff('abcdef', 'abcdef')", "0");
        TestUtils.assertCalculated("hapldiff('abcdefg', 'abcdef')", "1");
        TestUtils.assertCalculated("hapldiff('abgdef', 'abcdef')", "1");
    }

    @Test
    public void testVarSig() {
        TestUtils.assertCalculated("varsig('', '')", "A0C0G0T0");
        // todo test with meaningful input
    }

    @Test
    public void testRevCompl() {
        TestUtils.assertCalculated("revcompl('')", "");
        TestUtils.assertCalculated("revcompl('abc')", "gbt");
        TestUtils.assertCalculated("revcompl('gbt')", "abc");
        // todo test with meaningful input
    }

    @Test
    public void testRC() {
        TestUtils.assertCalculated("rc('')", "");
        TestUtils.assertCalculated("rc('abc')", "gbt");
        TestUtils.assertCalculated("rc('gbt')", "abc");
        // todo test with meaningful input
    }

    @Test
    public void testRevCigar() {
        TestUtils.assertCalculated("revcigar('')", "");
        TestUtils.assertCalculated("revcigar('abcd')", "dcba");
        TestUtils.assertCalculated("revcigar('1234abcd')", "dcb1234a");
        TestUtils.assertCalculated("revcigar('32a63b1c1004d')", "1004d1c63b32a");

        // This is probably a bug rather than expected behavior
        // TestUtils.assertCalculated("revcigar('32a63b1c1004d544')", "\u0000\u0000\u00001004d1c63b32a");
    }

    @Test
    public void testRefBase() throws IOException {
        Path tmpgorfile = Files.createTempFile("test",".gor");
        Files.write(tmpgorfile, "chrM\t1000000\nchr1\t249000\n".getBytes());
        tmpgorfile.toFile().deleteOnExit();
        String[] args = new String[]{"gor " + tmpgorfile.toAbsolutePath().normalize().toString() + "|calc r refbase(#1,#2)", "-config", "../tests/data/ref_mini/gor_config.txt"};
        String lines = TestUtils.runGorPipe(args);
        Assert.assertEquals("Wrong reference base", 't', lines.charAt(lines.length()-2) );
    }

    @Test
    public void testRefBases() {
        TestUtils.assertCalculated("refbases('', 0, 0)", "N");
        // todo test with meaningful input
    }

    @Test
    public void testBamTag() {
        TestUtils.assertCalculated("bamtag('', '')", "NOT_FOUND");
        // todo test with meaningful input
    }

    @Test
    public void testGffTag() {
        TestUtils.assertCalculated("gfftag('', '')", "NOT_FOUND");
        // todo test with meaningful input
    }

    @Test
    public void testVcfFormatTag() {
        TestUtils.assertCalculated("vcfformattag('', '', '')", "NOT_FOUND");
        TestUtils.assertCalculated("vcfformattag('GT:AD:DP:GQ:PL', '0/0:1,0:1:3:0,3,28', 'XX')", "NOT_FOUND");
        TestUtils.assertCalculated("vcfformattag('GT:AD:DP:GQ:PL', '0/0:1,0:1:3:0,3,28', 'GT')", "0/0");
        TestUtils.assertCalculated("vcfformattag('GT:AD:DP:GQ:PL', '0/0:1,0:1:3:0,3,28', 'DP')", "1");
        TestUtils.assertCalculated("vcfformattag('GT:AD:DP:GQ:PL', '0/0:1,0:1:3:0,3,28', 'PL')", "0,3,28");
    }

    @Test
    public void testTag() {
        TestUtils.assertCalculated("tag('', '', '')", "NOT_FOUND");
        TestUtils.assertCalculated("tag('a=a;aa=aa', 'a', ';')", "a");
        TestUtils.assertCalculated("tag('aa=aa;a=a', 'a', ';')", "a");
        // todo test with meaningful input
    }

    @Test
    public void testIHA() {
        TestUtils.assertCalculated("iha('', '')", "1");
        TestUtils.assertCalculated("iha('A', 'A')", "1");
        // todo test with meaningful input
    }

    @Ignore
    @Test(expected = GorDataException.class)
    public void testIHABadInput() {
        TestUtils.assertCalculated("iha('CD', 'A')", "1");
    }

    @Test
    public void testIUPACFA() {
        TestUtils.assertCalculated("iupacfa('','','')", "A");
        // todo test with meaningful input
    }

    @Test
    public void testIUPACMA() {
        TestUtils.assertCalculated("iupacma('','','')", "C");
        // todo test with meaningful input
    }

    @Test
    public void testIupac2Gt() {
        TestUtils.assertCalculated("iupac2gt('')", "A/C/G/T");
        TestUtils.assertCalculated("iupac2gt('A')", "A/A");
        // todo test with meaningful input
    }

    @Test
    public void testGtShare() {
        TestUtils.assertCalculated("gtshare('', 0, '', '', 0, '', '')", "1");
        // todo test with meaningful input
    }

    @Test
    public void testGtStat() {
        TestUtils.assertCalculated("gtstat('', 0, '', '', 0, '', '', 0, '', '')", "2");
        // todo test with meaningful input
    }

    @Test
    public void testGtFA() {
        TestUtils.assertCalculated("gtfa('', 0, '', '', 0, '', '', 0, '', '')", "");
        // todo test with meaningful input
    }

    @Test
    public void testGtMA() {
        TestUtils.assertCalculated("gtma('', 0, '', '', 0, '', '', 0, '', '')", "");
        // todo test with meaningful input
    }

    @Ignore
    @Test(expected = GorDataException.class)
    public void testInDAGWithEmptyInputs() {
        TestUtils.assertCalculated("if('' indag('', ''), 'true', 'false)", "");
        // todo test with meaningful input
    }

    @Test
    public void testCodons2Aminos() {
        TestUtils.assertCalculated("codons2aminos('acg')", "Thr");
        // todo test with meaningful input
    }

    @Test
    public void testCodons2ShortAminos() {
        TestUtils.assertCalculated("codons2shortaminos('acg')", "T");
        // todo test with meaningful input
    }

    @Test
    public void testChars2Gt() {
        TestUtils.assertCalculated("chars2gt(' ',0)", "3");
        TestUtils.assertCalculated("chars2gt('ab',0)", "2");
        // todo test with meaningful input
    }

    @Test
    public void testCharsPhased2Gt() {
        TestUtils.assertCalculated("charsphased2gt(' ',0)", "3");
        TestUtils.assertCalculated("charsphased2gt('ab',0)", "2");
        // todo test with meaningful input
    }

    @Test
    public void VcfGtItem() {
        TestUtils.assertCalculated("vcfgtitem('0|1', '1,2,3,4')", "2");
        TestUtils.assertCalculated("vcfgtitem('1|1', '1,2,3,4')", "3");
    }
}
