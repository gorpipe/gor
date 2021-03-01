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

package gorsat.process;

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class UTestPipeOptions {
    private static final String SIMPLE_EXTRA_SPACE = "gor   test.gor";
    private static final String SIMPLE = "gor test.gor";
    private static final String SOME_PIPE_STEPS = "gorrow 1,1,1 | calc col 1 | rename col col2 | group 1 -gc col2";
    private static final String SOME_PIPE_STEPS_EXTRA_SPACES = "gorrow 1,1,1    |  calc col 1 | rename col  col2 | group 1  -gc col2";
    private static final String SOME_PIPE_STEPS_EXTRA_SPACES_AND_NEWLINES = "gorrow 1,1,1\n    |  calc col 1\n | rename col  col2 \n| group 1  -gc col2";
    private static final String NESTED = "gor <( gor test.gor | calc col 1 ) | rename col col2";
    private static final String NESTED_NEWLINES = "gor <(\ngor test.gor | calc col 1\n) \n| rename col col2";
    private static final String QUOTES = "replace disease replace(replace(disease,\"'\",\"\"),' ','_')";


    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void getQueryFromArgs_noArgs() {
        String[] args = {};
        final String query = PipeOptions.getQueryFromArgs(args);
        assertEquals("", query);
    }

    @Test
    public void getQueryFromArgs_simpleQueryNotQuoted() {
        String[] args = {"gor", "test.gor"};
        final String query = PipeOptions.getQueryFromArgs(args);
        assertEquals(SIMPLE, query);
    }

    @Test
    public void getQueryFromArgs_simple() {
        verifyQuoted(SIMPLE, SIMPLE);
    }

    @Test
    public void getQueryFromArgs_simpleExtraWhitespace() {
        verifyQuoted(SIMPLE_EXTRA_SPACE, SIMPLE);
    }

    @Test
    public void getQueryFromArgs_quotes() {
        verifyQuoted(QUOTES, QUOTES);
    }

    @Test
    public void getQueryFromArgs_somePipeSteps() {
        verifyQuoted(SOME_PIPE_STEPS, SOME_PIPE_STEPS);
    }

    @Test
    public void getQueryFromArgs_somePipeStepsExtraWhitespace() {
        verifyQuoted(SOME_PIPE_STEPS_EXTRA_SPACES, SOME_PIPE_STEPS);
    }

    @Test
    public void getQueryFromArgs_scriptSimple() throws IOException {
        verifyScript(SIMPLE, SIMPLE);
    }

    @Test
    public void getQueryFromArgs_scriptSimpleExtraWhitespace() throws IOException {
        verifyScript(SIMPLE_EXTRA_SPACE, SIMPLE);
    }

    @Test
    public void getQueryFromArgs_scriptSomePipeSteps() throws IOException {
        verifyScript(SOME_PIPE_STEPS, SOME_PIPE_STEPS);
    }

    @Test
    public void getQueryFromArgs_scriptSomePipeStepsExtraWhitespace() throws IOException {
        verifyScript(SOME_PIPE_STEPS_EXTRA_SPACES, SOME_PIPE_STEPS);
    }

    @Test
    public void getQueryFromArgs_scriptSomePipeStepsExtraWhitespaceAndNewlines() throws IOException {
        verifyScript(SOME_PIPE_STEPS_EXTRA_SPACES_AND_NEWLINES, SOME_PIPE_STEPS);
    }

    @Test
    public void getQueryFromArgs_scriptNested() throws IOException {
        verifyScript(NESTED, NESTED);
    }

    @Test
    public void getQueryFromArgs_scriptNestedNewlines() throws IOException {
        verifyScript(NESTED_NEWLINES, NESTED);
    }

    @Test
    public void getQueryFromArgs_nestedNewlines() throws IOException {
        verifyQuoted(NESTED_NEWLINES, NESTED);
    }

    @Test
    public void scriptAndArgs() {
        String[] args = {SIMPLE, "-script", "dummy.txt"};
        expected.expect(GorParsingException.class);
        PipeOptions.getQueryFromArgs(args);
    }

    private void verifyQuoted(String input, String expected) {
        String[] args = {input};
        final String query = PipeOptions.getQueryFromArgs(args);
        assertEquals(expected, query);
    }

    private void verifyScript(String input, String expected) throws IOException {
        final File scriptFile = FileTestUtils.createTempFile(workDir.getRoot(), "script.txt", input);
        String[] args = {"-script", scriptFile.getAbsolutePath()};
        final String query = PipeOptions.getQueryFromArgs(args);
        assertEquals(expected, query);
    }
}