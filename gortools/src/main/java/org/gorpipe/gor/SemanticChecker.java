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

import gorsat.Commands.CommandInfo;
import gorsat.Commands.CommandParseUtilities;
import gorsat.process.GorPipeCommands;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.gorpipe.exceptions.GorParsingException;

import java.util.HashSet;
import java.util.Set;

public class SemanticChecker extends GorScriptBaseVisitor<Boolean> {
    static {
        GorPipeCommands.register();
    }

    private final Set<String> virtualRelationsCreated = new HashSet<>();
    private final Set<String> virtualRelationsReferenced = new HashSet<>();

    public void validate(String input) {
        SyntaxChecker syntaxChecker = new SyntaxChecker();
        ParserRuleContext scriptContext = syntaxChecker.parse(input);
        scriptContext.accept(this);

        virtualRelationsCreated.forEach(name -> {
            if (!virtualRelationsReferenced.contains(name)) {
                throw new GorParsingException(String.format("Create statement '%s' is never referenced", name), 0, 0);
            }
        });
    }

    @Override
    public Boolean visitVirtual_relation_name(GorScriptParser.Virtual_relation_nameContext ctx) {
        TerminalNode nameNode = (TerminalNode) ctx.getChild(0);
        String name = nameNode.toString().toUpperCase();
        if (virtualRelationsCreated.contains(name)) {
            throw new GorParsingException(String.format("Duplicate name of create statement: %s", name), 0, 0);
        }

        virtualRelationsCreated.add(name.toUpperCase());

        return Boolean.TRUE;
    }

    @Override
    public Boolean visitGeneric_command(GorScriptParser.Generic_commandContext ctx) {
        TerminalNode cmdNode = (TerminalNode) ctx.getChild(0);
        String cmd = cmdNode.toString().toUpperCase();

        CommandInfo commandInfo = GorPipeCommands.getInfo(cmd);
        if (commandInfo == null) {
            throw new GorParsingException(String.format("Invalid command %s", cmd), ctx.start.getLine(), ctx.start.getCharPositionInLine());
        }

        GorScriptParser.Options_and_argsContext optionsAndArgs = (GorScriptParser.Options_and_argsContext) ctx.getChild(1);
        int a = optionsAndArgs.start.getStartIndex();
        int b = optionsAndArgs.stop.getStopIndex();
        String optionsAndArgsText = "";
        if (a < b) {
            Interval interval = new Interval(a,b);
            optionsAndArgsText = optionsAndArgs.start.getInputStream().getText(interval);
        }

        String[] args = CommandParseUtilities.quoteSafeSplit(optionsAndArgsText, ' ');

        try {
            commandInfo.validateArguments(args);
        } catch (GorParsingException gpe) {
            throw new GorParsingException(gpe.getMessage(), optionsAndArgs.start.getLine(), optionsAndArgs.start.getCharPositionInLine());
        }

        return super.visitGeneric_command(ctx);
    }

    @Override
    public Boolean visitVirtual_relation(GorScriptParser.Virtual_relationContext ctx) {
        TerminalNode nameNode = (TerminalNode) ctx.getChild(1);
        String name = nameNode.toString().toUpperCase();
        virtualRelationsReferenced.add(name);
        return Boolean.TRUE;
    }
}
