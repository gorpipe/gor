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

package org.gorpipe.gor;

import org.antlr.v4.runtime.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UTestGorScriptParser {
    @Test
    public void empty() {
        Assert.assertFalse(parse(""));
    }

    @Test
    public void simple() {
        Assert.assertTrue(parse("gor 1.mem"));
    }

    @Test
    public void nonsense() {
        Assert.assertFalse(parse("gor (' bingo|"));
    }

    @Test
    public void def() {
        String input = "def #x# = 1.mem; gor #x#";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void create() {
        String input = "create xxx = gor 1.mem; gor [xxx]";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void pgor() {
        String input = "pgor 1.mem";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void nor() {
        String input = "nor test.tsv";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void simplePipe() {
        String input = "gor 1.mem | top 10 | group chrom -count";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void optionsOnInputSource() {
        String input = "gor -p chr4 dbsnp.gorz";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void nestedQuery() {
        String input = "gor <(gor bingo.gor | calc x 42) | top 10";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void where() {
        String input = "gor bingo.gor | where a > b";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void replace() {
        String input = "gor bingo.gor | replace a 64";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void select() {
        String input = "gor bingo.gor | select chrom,pos,a,b";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void hide() {
        String input = "gor bingo.gor | hide a,b";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void rename() {
        String input = "gor bingo.gor | rename a b";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void columnRange() {
        String input = "gor bingo.gor | select chrom,pos,a-b";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void columnWildCard() {
        String input = "gor bingo.gor | select chrom,pos,a*";
        Assert.assertTrue(parse(input));
    }

    @Test
    public void columnNumber() {
        String input = "gor bingo.gor | select 1,2,3,4";
        Assert.assertTrue(parse(input));
    }

    class ErrorListener extends BaseErrorListener {
        List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            errors.add(msg);
        }
    }

    private boolean parse(String input) {
        CodePointCharStream stream = CharStreams.fromString(input);
        GorScriptLexer lexer = new GorScriptLexer(stream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GorScriptParser parser = new GorScriptParser(tokenStream);
        parser.removeErrorListeners();
        ErrorListener listener = new ErrorListener();
        parser.addErrorListener(listener);
        GorScriptParser.ScriptContext scriptContext = parser.script();
        return listener.errors.isEmpty();
    }
}
