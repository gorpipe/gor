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

package gorsat.Script;

import gorsat.DynIterator;
import gorsat.QueryHandlers.GeneralQueryHandler;
import gorsat.TestUtils;
import gorsat.process.GenericSessionFactory;
import gorsat.process.GorPipeCommands;
import gorsat.process.GorPipeMacros;
import gorsat.process.PipeInstance;
import org.gorpipe.gor.model.GorParallelQueryHandler;
import org.gorpipe.gor.monitor.GorMonitor;
import org.gorpipe.gor.session.GorContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Creates with identical query text in one script get the same signature and target the same cache file.  They
 * must run once, not concurrently: two concurrent writers race on the cache file (ENGKNOW-4296).
 */
public class UTestScriptDuplicateCreates {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUp() {
        DynIterator.createGorIterator_$eq(PipeInstance::createGorIterator);
        GorPipeCommands.register();
        GorPipeMacros.register();
    }

    /** Wraps a query handler and records every command it is asked to run. */
    private static class RecordingQueryHandler implements GorParallelQueryHandler {
        private final GorParallelQueryHandler inner;
        final List<String> createNames = Collections.synchronizedList(new ArrayList<>());

        RecordingQueryHandler(GorParallelQueryHandler inner) {
            this.inner = inner;
        }

        @Override
        public String[] executeBatch(String[] fingerprints, String[] commandsToExecute, String[] batchGroupNames,
                                     String[] cacheFiles, GorMonitor cancelMonitor) {
            Collections.addAll(createNames, batchGroupNames);
            return inner.executeBatch(fingerprints, commandsToExecute, batchGroupNames, cacheFiles, cancelMonitor);
        }

        @Override
        public void setForce(boolean force) {
            inner.setForce(force);
        }

        @Override
        public long getWaitTime() {
            return inner.getWaitTime();
        }
    }

    /** What a script run sent to the query handlers, and the files its creates resolved to. */
    private static class ScriptRun {
        final int executedCommands;
        final Map<String, String> createdFiles;

        ScriptRun(int executedCommands, Map<String, String> createdFiles) {
            this.executedCommands = executedCommands;
            this.createdFiles = createdFiles;
        }
    }

    /** Runs the script in a fresh project root and cache, recording the commands that reach the query handlers. */
    private ScriptRun runScript(String[] commands) throws IOException {
        File root = workDir.newFolder();
        Assert.assertTrue(new File(root, "result_cache").mkdir());
        GorContext context = new GenericSessionFactory(root.getAbsolutePath(), "result_cache").create().getGorContext();
        RecordingQueryHandler handler = new RecordingQueryHandler(new GeneralQueryHandler(context, false));
        RecordingQueryHandler localHandler = new RecordingQueryHandler(new GeneralQueryHandler(context, false));
        ScriptExecutionEngine engine = new ScriptExecutionEngine(handler, localHandler, context);
        engine.execute(commands, true, false, "");
        return new ScriptRun(handler.createNames.size() + localHandler.createNames.size(), engine.getCreatedFiles());
    }

    /** Runs the script with gorpipe in a fresh project root, so no create is served from an earlier run's cache. */
    private String runGorPipeFresh(String script) throws IOException {
        return TestUtils.runGorPipe(script, workDir.newFolder().getAbsolutePath(), false, null);
    }

    @Test
    public void duplicateCreatesRunOnce() throws IOException {
        ScriptRun run = runScript(new String[] {
                "create #a# = gorrows -p chr1:1-100",
                "create #b# = gorrows -p chr1:1-100",
                "create #c# = gorrows -p chr1:1-50",
                "create #d# = gorrows -p chr1:1-100",
                "gor [#a#] [#b#] [#c#] [#d#]"
        });

        Assert.assertEquals("Each distinct create runs once", 2, run.executedCommands);

        Map<String, String> created = run.createdFiles;
        String a = created.get("[#a#]");
        Assert.assertNotNull("Created files: " + created, a);
        Assert.assertEquals("Duplicate creates share the result file", a, created.get("[#b#]"));
        Assert.assertEquals("Duplicate creates share the result file", a, created.get("[#d#]"));
        Assert.assertNotNull(created.get("[#c#]"));
        Assert.assertNotEquals(a, created.get("[#c#]"));
    }

    /** pgor expands a create into split queries plus a dictionary gather query; duplicates share them. */
    @Test
    public void duplicatePgorCreatesRunOnce() throws IOException {
        String genes = new File("../tests/data/gor/genes.gorz").getCanonicalPath();
        String pgor = "pgor -split 5 " + genes + " | group chrom -count";

        int singleCount = runScript(new String[] {"create #a# = " + pgor, "gor [#a#]"}).executedCommands;
        Assert.assertTrue("pgor should expand into several commands, got " + singleCount, singleCount > 1);
        Assert.assertEquals(singleCount,
                runScript(new String[] {"create #a# = " + pgor, "create #b# = " + pgor, "gor [#a#] [#b#]"}).executedCommands);

        String once = runGorPipeFresh("create #a# = " + pgor + "; gor [#a#] | group genome -sum -ic allCount");
        String twice = runGorPipeFresh("create #a# = " + pgor + "; create #b# = " + pgor
                + "; gor [#a#] [#b#] | group genome -sum -ic allCount");
        long onceSum = Long.parseLong(once.split("\n")[1].split("\t")[3]);
        long twiceSum = Long.parseLong(twice.split("\n")[1].split("\t")[3]);
        Assert.assertTrue(once, onceSum > 0);
        Assert.assertEquals(2 * onceSum, twiceSum);
    }

    @Test
    public void duplicateCreatesGiveSameResultAsDistinct() throws IOException {
        String duplicates = "create #a# = gorrows -p chr1:1-100; "
                + "create #b# = gorrows -p chr1:1-100; "
                + "create #c# = gorrows -p chr1:1-50; "
                + "gor [#a#] [#b#] [#c#] | group chrom -count";
        // Same rows, but #b# has different query text so it is a separate create.
        String distinct = duplicates.replace("create #b# = gorrows -p chr1:1-100;", "create #b# = gorrows -p chr1:1-100 | top 1000;");
        Assert.assertNotEquals(duplicates, distinct);

        String expected = runGorPipeFresh(distinct);
        Assert.assertTrue(expected, expected.contains("\t247\n")); // 99 + 99 + 49 rows
        Assert.assertEquals(expected, runGorPipeFresh(duplicates));
    }
}
