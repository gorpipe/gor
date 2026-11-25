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

package org.gorpipe.gor.function;

import gorsat.Commands.CommandParseUtilities;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.session.GorSession;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of function sources, e.g. report(file.yml, top=10). See {@link FunctionSource}.
 */
public final class FunctionSources {

    private static final Map<String, FunctionSource> sources = new ConcurrentHashMap<>();

    static {
        register(new ReportFunctionSource());
    }

    private FunctionSources() {
    }

    public static void register(FunctionSource source) {
        sources.put(source.name().toUpperCase(), source);
    }

    public static boolean isFunctionSource(String input) {
        return find(input.trim()).isPresent();
    }

    /**
     * Resolves a function source, e.g. report(file.yml, top=10), to the query it reads from.
     */
    public static String parse(String input, GorSession session) {
        Call call = resolve(input);
        return call.source().query(call.args(), session);
    }

    /**
     * Resolves a function source, e.g. report(file.yml, mystep), to the pipe steps it reads.
     */
    public static String parsePipeSteps(String input, GorSession session) {
        Call call = resolve(input);
        return call.source().pipeSteps(call.args(), session);
    }

    private record Call(FunctionSource source, String[] args) {
    }

    private static Call resolve(String input) {
        String trimmed = input.trim();
        FunctionSource source = find(trimmed)
                .orElseThrow(() -> new GorParsingException("Not a function source: " + trimmed));
        String argString = trimmed.substring(source.name().length() + 1, trimmed.length() - 1);
        String[] args = Arrays.stream(CommandParseUtilities.quoteSquareBracketsSafeSplit(argString, ','))
                .map(String::trim)
                .filter(arg -> !arg.isEmpty())
                .toArray(String[]::new);
        return new Call(source, args);
    }

    private static Optional<FunctionSource> find(String trimmed) {
        int open = trimmed.indexOf('(');
        if (open <= 0 || !trimmed.endsWith(")")) {
            return Optional.empty();
        }
        return Optional.ofNullable(sources.get(trimmed.substring(0, open).toUpperCase()))
                .filter(source -> CommandParseUtilities.findEndBracketPos(open + 1, trimmed) == trimmed.length() - 1);
    }
}
