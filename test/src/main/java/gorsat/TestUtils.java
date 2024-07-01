package gorsat;
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

import com.sun.management.UnixOperatingSystemMXBean;
import com.zaxxer.hikari.HikariPoolMXBean;
import gorsat.Commands.CommandParseUtilities;
import gorsat.Utilities.AnalysisUtilities;
import gorsat.Utilities.MacroUtilities;
import gorsat.process.*;
import org.apache.commons.lang.SystemUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.session.GorContext;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.gorpipe.gor.util.DataUtil;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.gorpipe.gor.model.GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME;

/**
 * Utility class to help with gor testing.
 *
 *
 * Created by gisli on 15/11/2016.
 */
public class TestUtils {
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

    public static final String LINE_SPLIT_PATTERN = "(?<=\n)";

    private static String[] DEFAULT_WRITE_LOCATIONS = {"test", "user_data", "result_cache"};

    /**
     * Run goripe
     *
     * @param query gorpipe query to run.
     */
    public static String runGorPipe(String query) {
        return runGorPipe(query, false);
    }

    public static String runGorPipe(String query, boolean server) {
        return runGorPipeWithOptions(query, true, server, null);
    }

    public static String runGorPipe(String query, boolean server, String securityContext) {
        return runGorPipeWithOptions(query, true, server, securityContext);
    }
    public static String runGorPipe(String query, String gorroot, boolean server, String securityContext) {
        return runGorPipeWithOptions(query, gorroot, null, true, server, securityContext, DEFAULT_WRITE_LOCATIONS);
    }

    public static String runGorPipe(String query, boolean server, String securityContext, String[] writeLocations) {
        return runGorPipeWithOptions(query, true, server, securityContext, writeLocations);
    }

    public static String runGorPipe(String query, String gorroot, boolean server, String securityContext, String[] writeLocations) {
        return runGorPipeWithOptions(query, gorroot, null, true, server, securityContext, writeLocations);
    }

    public static String runGorPipe(String query, String gorroot, String cacheDir, boolean server, String securityContext, String[] writeLocations) {
        return runGorPipeWithOptions(query, gorroot, cacheDir, true, server, securityContext, writeLocations);
    }

    public static String runGorPipe(String[] args, String whiteCmdListFile, boolean server, String securityContext) {
        return runGorPipe(args, () -> createSession(args, whiteCmdListFile, server, securityContext));
    }

    public static String runGorPipe(String[] args, boolean server) {
        return runGorPipe(args, () -> createSession(args, null, server, (String)null));
    }

    public static String runGorPipe(String[] args, Supplier<GorSession> sessionSupplier) {
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        try (PipeInstance pipe = new PipeInstance(new GorContext(sessionSupplier.get()))) {
            String queryToExecute = processQuery(options.query(), pipe.getSession());
            pipe.subProcessArguments(queryToExecute, false, null, false, false, "");
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

    public static String runGorPipe(String... args) {
        return runGorPipe(args, () -> {
            PipeOptions options = new PipeOptions();
            options.parseOptions(args);
            return new CLISessionFactory(options, null).create();
        });
    }

    private static String processQuery(String query, GorSession session) {
        AnalysisUtilities.checkAliasNameReplacement(CommandParseUtilities.quoteSafeSplitAndTrim(query, ';'),
                AnalysisUtilities.loadAliases(session.getProjectContext().getGorAliasFile(), session, DataUtil.toFile("gor_aliases", DataType.TXT))); //needs a test
        return MacroUtilities.replaceAllAliases(query,
                AnalysisUtilities.loadAliases(session.getProjectContext().getGorAliasFile(), session, DataUtil.toFile("gor_aliases", DataType.TXT)));
    }


    public static String getTestFile(String name) {
        String canonicalPath = getCanonicalPath(name);
        if (canonicalPath == null) {
            canonicalPath = getCanonicalPath(DataUtil.toFile("dummy", DataType.GOR));
        }
        return canonicalPath;
    }

    private static String getCanonicalPath(String name) {
        Path testDataPath = Paths.get("../tests/data/" + name);
        String canonicalPath = null;
        try {
            canonicalPath = testDataPath.toFile().getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return canonicalPath;
    }

    // Helper method to check if file has an open file handle.
    public static boolean isFileClosed(File file) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            // TODO: Windows does not have lsof so this will not be working on windows.
            return true;
        }

        Process plsof = null;
        BufferedReader reader = null;
        try {
            plsof = new ProcessBuilder(new String[]{"lsof", "|", "grep", file.getAbsolutePath()}).start();
            reader = new BufferedReader(new InputStreamReader(plsof.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(file.getAbsolutePath())) {
                    return false;
                }
            }
        } finally {
            if (reader != null) reader.close();
            if (plsof != null) plsof.destroy();
        }
        return true;
    }

    public static int getActivePoolConnections(String dbUrl, String user) throws MalformedObjectNameException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + dbUrl.replace(":", "-") + "-" + user + ")");
        HikariPoolMXBean poolProxy = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);
        return poolProxy.getActiveConnections();
    }

    public static int runGorPipeCount(String query) {
        return runGorPipeCount(query, false);
    }

    public static int runGorPipeCount(String query, boolean server) {
        String[] args = {query};
        return runGorPipeCount(args, server);
    }

    public static int runGorPipeCount(String[] args, boolean server) {
        return runGorPipeCount(args, () -> createSession(args, null, server, (String)null));
    }

    public static int runGorPipeCount(String[] args, Supplier<GorSession> sessionSupplier) {
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        try (PipeInstance pipe = new PipeInstance(new GorContext(sessionSupplier.get()))) {
            String queryToExecute = processQuery(options.query(), pipe.getSession());
            pipe.subProcessArguments(queryToExecute, false, null, false, false, "");
            int count = 0;
            while (pipe.hasNext()) {
                pipe.next();
                count++;
            }
            return count;
        }
    }

    private static GorSession createSession(String[] args, String whiteCmdListFile, boolean server, String securityContext) {
        return createSession(args, whiteCmdListFile, server, securityContext, DEFAULT_WRITE_LOCATIONS);
    }

    private static GorSession createSession(String[] args, String whiteCmdListFile, boolean server, String securityContext, String[] writeLocations) {
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        TestSessionFactory factory = new TestSessionFactory(options, whiteCmdListFile, server, securityContext, writeLocations);
        return factory.create();
    }

    private static GorSession createSession(boolean server, String securityContext) {
        return createSession(server, securityContext, DEFAULT_WRITE_LOCATIONS);
    }

    private static GorSession createSession(boolean server, String securityContext, String[] writeLocations) {
        String[] args = {};
        return createSession(args, null, server, securityContext, writeLocations);
    }

    public static String[] runGorPipeLines(String query) {
        return runGorPipe(query).split(LINE_SPLIT_PATTERN);
    }

    public static String[] runGorPipeLinesNoHeader(String query) {
        return runGorPipeNoHeader(query).split(LINE_SPLIT_PATTERN);
    }

    public static String runGorPipeNoHeader(String query) {
        return runGorPipeWithOptions(query, false, false, null);
    }

    public static String runGorPipeWithOptions(String query, boolean includeHeader, boolean server, String securityContext) {
        return runGorPipeWithOptions(query, includeHeader, server, securityContext, DEFAULT_WRITE_LOCATIONS);
    }

    public static String runGorPipeWithOptions(String query, boolean includeHeader, boolean server, String securityContext, String[] writeLocations) {
        return runGorPipeWithOptions(query, ".", null, includeHeader, server, securityContext, writeLocations);
    }

    public static String runGorPipeWithOptions(String query, String gorroot, String cacheDir, boolean includeHeader, boolean server, String securityContext, String[] writeLocations) {
        try (PipeInstance pipe = createPipeInstance(server, gorroot, cacheDir, securityContext, writeLocations)) {
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

    private static PipeInstance createPipeInstance(boolean server, String securityContext) {
        return PipeInstance.createGorIterator(new GorContext(createSession(server, securityContext)));
    }

    private static PipeInstance createPipeInstance(boolean server, String securityContext, String[] writeLocations) {
        return PipeInstance.createGorIterator(new GorContext(createSession(server, securityContext, writeLocations)));
    }

    private static PipeInstance createPipeInstance(boolean server, String gorroot, String cacheDir, String securityContext, String[] writeLocations) {
        List<String> options = new ArrayList<>();
        if (gorroot != null) {
            options.add("-gorroot");
            options.add(gorroot);
        }
        if (cacheDir != null) {
            options.add("-cachedir");
            options.add(cacheDir);
        }
        return PipeInstance.createGorIterator(new GorContext(createSession(options.toArray(String[]::new), null, server, securityContext, writeLocations)));
    }

    public static GenomicIterator runGorPipeIterator(String query) {
        PipeInstance pipe = createPipeInstance(false, null);
        pipe.init(query, null);
        return pipe.getIterator();
    }

    private static String runGorPipeWithOptions(String query, boolean includeHeader, boolean server) {

        try (PipeInstance pipe = createPipeInstance(server)) {
            return runPipeInstanceQueryToString(pipe, query, includeHeader);
        }
    }

    private static PipeInstance createPipeInstance(boolean server) {
        return PipeInstance.createGorIterator(createSession(server).getGorContext());
    }

    public static String runPipeInstanceQueryToString(PipeInstance pipe, String query, boolean includeHeader) {
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

    public static int runGorPipeCount(String query, String projectRoot) {
        String[] args = {query, "-gorroot", projectRoot};
        return runGorPipeCount(args, false);
    }

    public static int runGorPipeCount(String... args) {
        return runGorPipeCount(args, false);
    }

    public static int runGorPipeCountCLI(String[] args) {
        return runGorPipeCount(args, ()->createCLISession(args));
    }

    public static int runGorPipeCountWithWhitelist(String query, Path whiteCmdListFile) {
        return runGorPipeCountWithWhitelist(query, whiteCmdListFile, null);
    }

    public static int runGorPipeCountWithWhitelist(String query, Path whiteCmdListFile, String securityContext) {
        String[] args = {query};
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        try (PipeInstance pipe = new PipeInstance(createSession(args, whiteCmdListFile.toAbsolutePath().toString(), false, securityContext).getGorContext())) {
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

    public static String runGorPipeServer(String query, String projectRoot, String securityContext) {
        PipeOptions options = new PipeOptions();
        options.parseOptions(new String[]{"-gorroot", projectRoot, "-cachedir", projectRoot + "/result_cache"});
        TestSessionFactory factory = new TestSessionFactory(options, null, true, securityContext, DEFAULT_WRITE_LOCATIONS);

        try (PipeInstance pipe = PipeInstance.createGorIterator(new GorContext(factory.create()))) {
            pipe.init(query, null);

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

    public static String runGorPipeCLI(String query, String gorRoot, String securityContext) {
        PipeOptions opts = new PipeOptions();
        opts.gorRoot_$eq(gorRoot);
        GorSessionFactory sessionFactory = new CLISessionFactory(opts, securityContext);
        try (PipeInstance pipe = PipeInstance.createGorIterator(new GorContext(sessionFactory.create()))) {
            pipe.init(query, null);

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

    public static void assertTwoGorpipeResults(String desc, String[] query1, String[] query2) {
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
        return createFile(prefix, DataType.GOR.suffix, lines);
    }

    public static File createTsvFile(String prefix, String[] lines) {
        return createFile(prefix, DataType.TSV.suffix, lines);
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

    public static GorSession createSession(boolean server) {
        String[] args = {};
        return createSession(args, null, server);
    }

    public static GorSession createSession(String[] args, String whiteListFile, boolean server) {
        return createSession(args, whiteListFile, server, DEFAULT_WRITE_LOCATIONS);
    }

    public static GorSession createSession(String[] args, String whiteListFile, boolean server, String[] writeLocations) {
        return createSession(args, whiteListFile, server, writeLocations, null);
    }

    public static GorSession createSession(String[] args, String whiteListFile, boolean server, String[] writeLocations, String securityContext) {
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);

        TestSessionFactory factory = new TestSessionFactory(options, whiteListFile, server, securityContext, writeLocations);
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

    /**
     * Create or update dictionary.
     *
     * @param name     name of the dictionary.
     * @param rootPath root path
     * @param data     map with alias to files, to be add to the dictionary.
     * @return new table created with the given data.
     */
    public static GorDictionaryTable createDictionaryWithData(String name, Path rootPath, Map<String, List<String>> data) {
        Path tablePath = rootPath.resolve(DataUtil.toFile(name, DataType.GORD));
        if (Files.exists(tablePath)) {
            throw new GorSystemException("Table already exists:  " + tablePath, null);
        }
        GorDictionaryTable table = new GorDictionaryTable.Builder<>(tablePath.toString()).useHistory(true).validateFiles(false).build();
        table.insert(data);
        table.setBucketize(true);
        table.save();
        return table;
    }

    /**
     * Create or update dictionary.
     *
     * @param name     name of the dictionary.
     * @param rootPath root path
     * @param data     map with alias to files, to be add to the dictionary.
     * @return new table created with the given data.
     */
    public static GorDictionaryTable createFolderDictionaryWithData(String name, Path rootPath, Map<String, List<String>> data) {
        Path tablePath = rootPath.resolve(DataUtil.toFile(name, DataType.GORD));
        if (Files.exists(tablePath.resolve(DEFAULT_FOLDER_DICTIONARY_NAME))) {
            throw new GorSystemException("Table already exists:  " + tablePath, null);
        }
        try {
            Files.createDirectories(tablePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GorDictionaryTable table = new GorDictionaryTable.Builder<>(tablePath.resolve(DEFAULT_FOLDER_DICTIONARY_NAME).toString())
                .useHistory(true).validateFiles(false).build();
        table.insert(data);
        table.setBucketize(true);
        table.save();
        return new GorDictionaryTable(tablePath.toString());
    }

    public static String SECRETS_FILE_NAME = "../tests/config/secrets.env";

    public static Properties loadSecrets() {
        Properties prop = new Properties();
        if (Files.exists(Paths.get(SECRETS_FILE_NAME))) {
            try (InputStream inputStream = new FileInputStream(SECRETS_FILE_NAME)) {
                prop.load(inputStream);
            } catch (IOException e) {
                // Do nothing
            }
        }

        prop.putAll(System.getenv());

        return prop;
    }
}