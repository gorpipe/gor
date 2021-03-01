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
import org.gorpipe.exceptions.GorParsingException;

public class SyntaxChecker extends BaseErrorListener {
    public ParserRuleContext parse(String input) {
        GorScriptParser parser = getGorScriptParser(input);
        return parser.script();
    }

    public ParserRuleContext parseCalc(String input) {
        GorScriptParser parser = getGorScriptParser(input);
        return parser.calc_expression();
    }

    public ParserRuleContext parseFilter(String input) {
        GorScriptParser parser = getGorScriptParser(input);
        return parser.rel_expr();
    }

    private GorScriptParser getGorScriptParser(String input) {
        CodePointCharStream stream = CharStreams.fromString(input);
        GorScriptLexer lexer = new GorScriptLexer(stream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GorScriptParser parser = new GorScriptParser(tokenStream);
        parser.removeErrorListeners();
        parser.addErrorListener(this);
        return parser;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        throw new GorParsingException(msg, line, charPositionInLine);
    }
}
