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

import org.gorpipe.gor.GorSession;
import org.gorpipe.model.gor.iterators.RowSource;
import com.sun.management.UnixOperatingSystemMXBean;
import gorsat.Commands.CommandParseUtilities;
import gorsat.process.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utility class to help with gor testing.
 *
 *
 * Created by gisli on 15/11/2016.
 */
public class TestUtils {
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Run goripe
     * @param query   gorpipe query to run.
     */
    public static String runGorPipe(String query) {
        return runGorPipe(query, false);
    }

    public static String runGorPipe(String query, boolean server) {
        return runGorPipeWithOptions(query, true, server);
    }

    private static final String LINE_SPLIT_PATTERN = "(?<=\n)";

    public static String runGorPipeNoHeader(String query) {
        return runGorPipeWithOptions(query, false, false);
    }

    public static String[] runGorPipeLines(String query) {
        return runGorPipe(query).split(LINE_SPLIT_PATTERN);
    }

    public static String[] runGorPipeLinesNoHeader(String query) {
        return runGorPipeNoHeader(query).split(LINE_SPLIT_PATTERN);
    }

    private static String runGorPipeWithOptions(String query, boolean includeHeader, boolean server) {

        try (PipeInstance pipe = createPipeInstance(server)) {
            String queryToExecute = processQuery(query, pipe.getSession());
            pipe.init(queryToExecute, null);
            StringBuilder result = new StringBuilder();
            if (includeHeader) {
                result.append(pipe.getHeader());
                result.append("\n");
            }
            while (pipe.hasNext()) {
                result.append(pipe.next());
                result.append("\n");
            }
            return result.toString();
        }
    }

    private static String processQuery(String query, GorSession session) {
        AnalysisUtilities.checkAliasNameReplacement(CommandParseUtilities.quoteSafeSplitAndTrim(query, ';'),
                AnalysisUtilities.loadAliases(session.getProjectContext().getGorAliasFile(), session, "gor_aliases.txt")); //needs a test
        return MacroUtilities.replaceAllAliases(query,
                AnalysisUtilities.loadAliases(session.getProjectContext().getGorAliasFile(), session, "gor_aliases.txt"));
    }

    private static PipeInstance createPipeInstance(boolean server) {
        return PipeInstance.createGorIterator(createSession(server).getGorContext());
    }

    public static String runGorPipe(String... args) {

        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        CLISessionFactory factory = new CLISessionFactory(options, null);

        try (PipeInstance pipe = new PipeInstance(factory.create().getGorContext())) {
            String queryToExecute = processQuery(options.query(), pipe.getSession());
            pipe.init(queryToExecute, false, "");
            StringBuilder result = new StringBuilder();
            result.append(pipe.getHeader());
            result.append("\n");
            while (pipe.hasNext()) {
                result.append(pipe.next());
                result.append("\n");
            }
            return result.toString();
        }
    }

    public static RowSource runGorPipeIterator(String query) {
        PipeInstance pipe = createPipeInstance(false);
        pipe.init(query, null);
        return pipe.theIterator();
    }


    public static void runGorPipeIteratorOnMain(String query) {
        String[] args = {query};
        runGorPipeIteratorOnMain(args);
    }

    public static void runGorPipeIteratorOnMain(String[] args) {
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        CLIGorExecutionEngine engine = new CLIGorExecutionEngine(options, null, null);
        engine.execute();
    }

    public static int runGorPipeCount(String query) {
        return runGorPipeCount(query, false);
    }

    public static int runGorPipeCount(String query, String projectRoot) {
        String[] args = {query, "-gorroot", projectRoot};
        return runGorPipeCount(args, false);
    }

    public static int runGorPipeCount(String query, boolean server) {
        String[] args = {query};
        return runGorPipeCount(args, server);
    }

    public static int runGorPipeCount(String... args) {
        return runGorPipeCount(args, false);
    }

    public static int runGorPipeCount(String[] args, boolean server) {
        return runGorPipeCount(args, () -> createSession(args, null, server));
    }

    public static int runGorPipeCountCLI(String[] args) {
        return runGorPipeCount(args, ()->createCLISession(args));
    }

    public static int runGorPipeCount(String[] args, Supplier<GorSession> sessionSupplier) {
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        try (PipeInstance pipe = new PipeInstance(sessionSupplier.get().getGorContext())) {
            String queryToExecute = processQuery(options.query(), pipe.getSession());
            pipe.init(queryToExecute, false, "");
            int count = 0;
            while (pipe.hasNext()) {
                pipe.next();
                count++;
            }
            return count;
        }
    }

    public static int runGorPipeCountWithWhitelist(String query, Path whitelistFile) {
        String[] args = {query};
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        try (PipeInstance pipe = new PipeInstance(createSession(args, whitelistFile.toAbsolutePath().toString(), false).getGorContext())) {
            String queryToExecute = processQuery(options.query(), pipe.getSession());
            pipe.init(queryToExecute, false, "");
            int count = 0;
            while (pipe.hasNext()) {
                pipe.next();
                count++;
            }
            return count;
        }
    }

    public static void assertTwoGorpipeResults(String query1, String query2) {
        String result1 = runGorPipe(query1);
        String result2 = runGorPipe(query2);
        Assert.assertEquals(result1, result2);
    }

    // TODO: Copied from UTestGorpipeParsing consider sharing.
    public static void assertGorpipeResults(String expected, String query) {
        String result = runGorPipe(query);
        Assert.assertEquals(expected, result);
    }

    public static void assertGorpipeResults(String expected, String... args) {
        String result = runGorPipe(args);
        Assert.assertEquals(expected, result);
    }

    public static void assertTwoGorpipeResults(String desc, String query1, String query2) {
        String result1 = runGorPipe(query1);
        String result2 = runGorPipe(query2);
        Assert.assertEquals(desc, result1, result2);
    }

    public static void assertGorpipeResults(String desc, String expected, String query) {
        String result = runGorPipe(query);
        Assert.assertEquals(desc, expected, result);
    }

    public static void assertGorpipeResults(String desc, String expected, String[] args) {
        String result = runGorPipe(args);
        Assert.assertEquals(desc, expected, result);
    }

    public static String getCalculated(String expression) {
        String query = "gor 1.mem | select 1,2 | top 1 | calc NEWCOL " + expression + " | top 1";
        String[] result = runGorPipe(query, true).split("\t");
        return result[result.length - 1].replace("\n", "");
    }

    public static void assertCalculated(String expression, String expectedResult) {
        String resultValue = getCalculated(expression);
        Assert.assertEquals("Expression: " + expression, expectedResult, resultValue);
    }

    public static void assertCalculated(String expression, Double expectedResult, double delta) {
        double resultValue = Double.parseDouble(getCalculated(expression));
        Assert.assertEquals("Expression: " + expression, expectedResult, resultValue, delta);
    }

    public static void assertCalculatedLong(String expression, Long expectedResult) {
        Long resultValue = Long.parseLong(getCalculated(expression));
        Assert.assertEquals("Expression: " + expression, expectedResult, resultValue);
    }

    public static void assertCalculated(String expression, Double expectedResult) {
        assertCalculated(expression, expectedResult, 0.0001);
    }

    public static void assertCalculated(String expression, Integer expectedResult) {
        Integer resultValue = Integer.parseInt(getCalculated(expression));
        Assert.assertEquals("Expression: " + expression, expectedResult, resultValue);
    }

    public static long countOpenFiles() throws IOException, InterruptedException {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            String pid = name.substring(0,name.indexOf('@'));
            Process p = Runtime.getRuntime().exec("lsof -p "+pid);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = p.getInputStream();
            try {
                int r = is.read();
                while( r != -1 ) {
                    baos.write(r);
                    r = is.read();
                }
            } finally {
                try {
                    is.close();
                } finally {
                    baos.close();
                    p.destroyForcibly();
                    p.waitFor();
                }
            }
            String[] split = baos.toString().split("\n");
            return Arrays.asList(split).stream().filter(ps -> ps.contains("REG") ).collect(Collectors.toList()).size();
        } else {
            log.warn("Unable to do open file count on this platform");
            return -1;
        }
    }

    public static File createGorFile(String prefix, String[] lines) {
        return createFile(prefix, ".gor", lines);
    }

    public static File createTsvFile(String prefix, String[] lines) {
        return createFile(prefix, ".tsv", lines);
    }

    private static File createFile(String prefix, String extension, String[] lines) {
        try {
            File file = File.createTempFile(prefix, extension);
            file.deleteOnExit();

            PrintWriter writer = new PrintWriter(file);

            if (lines.length> 0) {
                writer.println(lines[0]);
                for (int i = 1; i < lines.length-1; i++) {
                    writer.println(lines[i]);
                }
                if(lines.length > 1) {
                    writer.print(lines[lines.length - 1]);
                }
            }
            writer.flush();

            return file;
        } catch (IOException e) {
            Assert.fail("Couldn't create test file");
            return null;
        }
    }

    private static GorSession createSession(boolean server) {
        String[] args = {};
        return createSession(args, null, server);
    }

    private static GorSession createSession(String[] args, String whiteListFile, boolean server) {
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        TestSessionFactory factory = new TestSessionFactory(options, whiteListFile, server, null);
        return factory.create();
    }

    private static GorSession createCLISession(String[] args) {
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        CLISessionFactory factory = new CLISessionFactory(options, null);
        return factory.create();
    }

    public static String[] runGorPipeWithCleanup(String query, File[] files) {
        String[] lines;
        try {
            lines = runGorPipeLines(query);
        } finally {
            for (File f : files) {
                if(!f.delete()) {
                    log.warn("Couldn't delete {}", f.getAbsolutePath());
                }
            }
        }
        return lines;
    }

    static void assertJoinQuery(String[] leftLines, String[] rightLines, String joinQuery, String[] expected) {
        File left = createGorFile("TestUtils", leftLines);
        File right = createGorFile("TestUtils", rightLines);

        assert left != null;
        assert right != null;
        String query = String.format(joinQuery, left.getAbsolutePath(), right.getAbsolutePath());
        String[] lines = runGorPipeWithCleanup(query, new File[]{left, right});

        Assert.assertArrayEquals(expected, lines);
    }
}