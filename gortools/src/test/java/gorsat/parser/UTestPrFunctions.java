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

package gorsat.parser;

import gorsat.TestUtils;
import org.gorpipe.exceptions.GorDataException;
import org.junit.Ignore;
import org.junit.Test;

public class UTestPrFunctions {
    @Test
    public void testChar2Pr() {
        TestUtils.assertCalculated("char2pr('a')", 0.312);
        TestUtils.assertCalculated("char2pr('abc')", 0.312);
        TestUtils.assertCalculated("char2pr(' ')", "0;0;0");
    }

    @Ignore
    @Test(expected = GorDataException.class)
    public void testChar2PrWithAnEmptyString() {
        TestUtils.assertCalculated("char2pr('')", 0.312);
    }

    @Test
    public void Pr2Char() {
        TestUtils.assertCalculated("pr2char(0)", "~");
        TestUtils.assertCalculated("pr2char(0.312)", "a");
        TestUtils.assertCalculated("pr2char(1)", "!");
    }

    @Test
    public void testChars2PrPr() {
        TestUtils.assertCalculated("chars2prpr(' ')", "0;0;0");
        TestUtils.assertCalculated("chars2prpr('aa')", "0.312;0.312");

        TestUtils.assertCalculated("chars2prhom('  ')", 0.0);
        TestUtils.assertCalculated("chars2prhet('  ')", 0.0);
        TestUtils.assertCalculated("chars2dose('  ')", 0.0);

        TestUtils.assertCalculated("chars2prhom('!~')", 1.0);
        TestUtils.assertCalculated("chars2prhet('!~')", 0.0);
        TestUtils.assertCalculated("chars2dose('!~')", 1.0);
    }

    @Ignore
    @Test(expected = GorDataException.class)
    public void testChars2PrPrWithAnEmptyString() {
        TestUtils.assertCalculated("chars2prpr('')", 0.312);
    }

    @Test
    public void testPrPr2Chars() {
        TestUtils.assertCalculated("prpr2chars('0;0')", "~~");
        TestUtils.assertCalculated("prpr2chars('0.312;0.312')", "aa");
        TestUtils.assertCalculated("prpr2chars('1;1')", "!!");
    }

    @Test
    public void testPrPr2CharsWithCustomSeparator() {
        TestUtils.assertCalculated("prpr2chars('0x0', 'x')", "~~");
        TestUtils.assertCalculated("prpr2chars('0.312x0.312', 'x')", "aa");
        TestUtils.assertCalculated("prpr2chars('1x1', 'x')", "!!");
    }

    @Test
    public void testChars2PrPrPr() {
        TestUtils.assertCalculated("chars2prprpr(' ')", "0;0;0");
        TestUtils.assertCalculated("chars2prprpr('aaa')", "0.376;0.312;0.312");
    }

    @Ignore
    @Test(expected = GorDataException.class)
    public void testChars2PrPrPrWithAnEmptyString() {
        TestUtils.assertCalculated("chars2prprpr('')", 0.312);
    }

    @Test
    public void testPrPrPr2Chars() {
        TestUtils.assertCalculated("prprpr2chars('0;0;0')", "  ");

        // todo verify if this is expected - looks like a bug
        TestUtils.assertCalculated("prprpr2chars('0.312;0.312;0.312')", "aa");
        TestUtils.assertCalculated("prprpr2chars('1;1;1')", "!!");
    }

    @Test
    public void testPrPrPr2CharsWithCustomSeparator() {
        TestUtils.assertCalculated("prprpr2chars('0x0x0', 'x')", "  ");

        // todo verify if this is expected - looks like a bug
        TestUtils.assertCalculated("prprpr2chars('0.312x0.312x0.312', 'x')", "aa");
        TestUtils.assertCalculated("prprpr2chars('1x1x1', 'x')", "!!");
    }
}
