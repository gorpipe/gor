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

import gorsat.process.CLIGorExecutionEngine;
import gorsat.process.PipeOptions;
import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Buffering analysis steps (group, sort, ...) must not flush their buffered rows at finish
 * when an upstream step has thrown. Otherwise a failing query emits a partial result row
 * before the error is reported.
 */
public class UTestBufferedAnalysisErrorState {

    private static class Outcome {
        List<String> rows;
        Throwable error;
    }

    /**
     * Runs the query the way the command line does, pushing rows through the pipeline into an output
     * step, and returns the data rows written (header excluded) along with any error.
     */
    private static Outcome run(String query) {
        Outcome outcome = new Outcome();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PipeOptions options = PipeOptions.parseInputArguments(new String[]{query});
        try {
            new CLIGorExecutionEngine(options, null, null, out).execute();
        } catch (Throwable t) {
            outcome.error = t;
        }
        String[] lines = out.toString(StandardCharsets.UTF_8).split("\n");
        outcome.rows = Arrays.stream(lines).skip(1).filter(l -> !l.isEmpty()).toList();
        return outcome;
    }

    private static void assertOnlyError(String query) {
        Outcome outcome = run(query);
        Assert.assertNotNull("Expected an error for: " + query, outcome.error);
        Assert.assertTrue("Expected GorDataException but got " + outcome.error,
                hasCause(outcome.error, GorDataException.class));
        Assert.assertEquals("No data rows expected for: " + query, List.of(), outcome.rows);
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) return true;
        }
        return false;
    }

    @Test
    public void groupCountEmitsNothingWhenUpstreamThrows() {
        assertOnlyError("nor <(norrows 5) | throwif #1 = 3 | group -count");
    }

    @Test
    public void groupListEmitsNothingWhenUpstreamThrows() {
        assertOnlyError("nor <(norrows 5) | throwif #1 = 3 | group -lis -sc #1");
    }

    @Test
    public void sortEmitsNothingWhenUpstreamThrows() {
        assertOnlyError("nor <(norrows 5) | throwif #1 = 3 | sort -c #1:nr");
    }

    @Test
    public void gorGroupEmitsNothingWhenUpstreamThrows() {
        assertOnlyError("gorrows -p chr1:1-6 | throwif pos = 3 | group chrom -count");
    }

    @Test
    public void gorSortEmitsNothingWhenUpstreamThrows() {
        assertOnlyError("gorrows -p chr1:1-6 | throwif pos = 3 | sort chrom -c pos:nr");
    }

    @Test
    public void sortRemovesSpillFilesWhenUpstreamThrows() {
        String oldBatchSize = System.getProperty("gor.sort.batchSize");
        System.setProperty("gor.sort.batchSize", "24");
        try {
            Set<String> before = sortSpillFiles();
            assertOnlyError("gorrows -p chr1:1-100 | throwif pos = 90 | sort chrom -c pos:nr");
            Set<String> left = sortSpillFiles();
            left.removeAll(before);
            Assert.assertEquals("Sort spill files left behind", Set.of(), left);
        } finally {
            if (oldBatchSize == null) System.clearProperty("gor.sort.batchSize");
            else System.setProperty("gor.sort.batchSize", oldBatchSize);
        }
    }

    private static Set<String> sortSpillFiles() {
        String[] names = new File(System.getProperty("java.io.tmpdir")).list((dir, name) -> name.startsWith("gorsort"));
        return names == null ? new HashSet<>() : new HashSet<>(Arrays.asList(names));
    }

    @Test
    public void groupOutputUnchangedWithoutError() {
        Outcome outcome = run("nor <(norrows 5) | throwif #1 = 99 | group -count -lis -sc #1");
        Assert.assertNull(outcome.error);
        Assert.assertEquals(List.of("5\t0,1,2,3,4"), outcome.rows);
    }

    @Test
    public void sortOutputUnchangedWithoutError() {
        Outcome outcome = run("nor <(norrows 3) | throwif #1 = 99 | sort -c #1:nr");
        Assert.assertNull(outcome.error);
        Assert.assertEquals(List.of("2", "1", "0"), outcome.rows);
    }
}
