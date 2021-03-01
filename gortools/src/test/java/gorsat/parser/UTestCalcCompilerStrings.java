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

import org.junit.Test;

public class UTestCalcCompilerStrings extends TestCalcCompilerBase {
    @Test
    public void stringVariables() {
        ColumnValue[] cols = {
                new ColumnValue("x", "S", "bingo"),
                new ColumnValue("y", "S", "bongo"),
        };

        assertResult("x", cols, "bingo");
        assertResult("y", cols, "bongo");
    }


    @Test
    public void stringLiteral() {
        assertResult("'test'", "test");
    }

    @Test
    public void expressionsWithStringLiterals() {
        assertResult("'bingo' + 'bongo'", "bingobongo");
    }

    @Test
    public void expressionsWithStringVariables() {
        ColumnValue[] cols = {
                new ColumnValue("x", "S", "bingo"),
                new ColumnValue("y", "S", "bongo"),
        };
        assertResult("x+y", cols, "bingobongo");
        assertResult("'bingo'+y", cols, "bingobongo");
        assertResult("'bingo'+upper(y)", cols, "bingoBONGO");
        assertResult("x+'bongo'", cols, "bingobongo");
        assertResult("upper(x)+'bongo'", cols, "BINGObongo");
        assertResult("upper(x)+y", cols, "BINGObongo");
        assertResult("x+upper(y)", cols, "bingoBONGO");
        assertResult("upper(x)+upper(y)", cols, "BINGOBONGO");
    }

    @Test
    public void addingStringsToVariables() {
        ColumnValue[] cols = {
                new ColumnValue("s", "S", "bingo"),
                new ColumnValue("i", "I", 10),
                new ColumnValue("l", "L", 123456789012345L),
                new ColumnValue("d", "D", 3.14),
        };

        // This is questionable behavior from the old parser

        assertResult("'bingo'+s", cols, "bingobingo");
        assertResult("'bingo'+i", cols, "bingo10");
        assertResult("'bingo'+l", cols, "bingo123456789012345");
        assertResult("'bingo'+d", cols, "bingo3.14");
        assertResult("s+'bingo'", cols, "bingobingo");
        assertResult("i+'bingo'", cols, "10bingo");
        assertResult("l+'bingo'", cols, "123456789012345bingo");
        assertResult("d+'bingo'", cols, "3.14bingo");
        assertResult("s+i", cols, "bingo10");
        assertResult("s+l", cols, "bingo123456789012345");
        assertResult("s+d", cols, "bingo3.14");
        assertResult("i+s", cols, "10bingo");
        assertResult("l+s", cols, "123456789012345bingo");
        assertResult("d+s", cols, "3.14bingo");
    }
}
