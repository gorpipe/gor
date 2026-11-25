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

package gorsat.process;

import gorsat.TestUtils;
import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A query whose pipeline fails to build (a later step does not parse) must close the files the steps built before it
 * opened, such as the right-hand source of a JOIN. Before ENGKNOW-4294 only the input source was closed, so those
 * files stayed open until the GC finalizer closed them ("Datasource closed via finalize method").
 */
public class UTestPipeBuildFailureClosesSources {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path root;
    private Path part1;
    private Path part2;
    private Path right;

    @Before
    public void setUp() throws IOException {
        root = tmp.getRoot().toPath();
        Files.createDirectories(root.resolve("source/var"));
        Files.writeString(root.resolve("source/var/left.gor"), "chrom\tpos\tref\nchr1\t1\tA\nchr1\t5\tC\n");
        right = Files.writeString(root.resolve("source/var/right.gor"), "chrom\tpos\tref\nchr1\t1\tG\nchr2\t3\tT\n");

        // A cached create: a dictionary of result parts, as joined in production.
        Path cache = Files.createDirectories(root.resolve("result_cache/cache/abc"));
        part1 = Files.writeString(cache.resolve("p1_md5.gor"), "chrom\tpos\tref\nchr1\t1\tA\n");
        part2 = Files.writeString(cache.resolve("p2_md5.gor"), "chrom\tpos\tref\nchr2\t3\tT\n");
        Files.writeString(cache.resolve("d.gord"),
                "p1_md5.gor\t1\tchr1\t0\tchr1\t1000\np2_md5.gor\t2\tchr2\t0\tchr2\t1000\n");
    }

    @Test
    public void joinRightSourceIsClosedWhenALaterStepFailsToParse() throws Exception {
        assertFailsToParse("gor source/var/left.gor | join -snpsnp source/var/right.gor | select F");

        assertClosed(right);
    }

    @Test
    public void nestedJoinOnCachedDictionaryIsClosedWhenALaterStepFailsToParse() throws Exception {
        assertFailsToParse("gor -p chr1:1-10 <(source/var/left.gor | join -snpsnp -f 1 -ic result_cache/cache/abc/d.gord"
                + " | varjoin -norm -r -l result_cache/cache/abc/d.gord | rename overlapcount oc | select 1,2,F)");

        assertClosed(part1);
        assertClosed(part2);
    }

    @Test
    public void writeBeforeAStepThatFailsToParseLeavesExistingOutputAlone() throws Exception {
        Path out = Files.writeString(root.resolve("result_cache/out.gor"), "chrom\tpos\nchr1\t9\n");
        Path link = root.resolve("result_cache/out.gor.link");

        assertFailsToParse("gor source/var/left.gor | write result_cache/out.gor -link result_cache/out.gor.link | select F");

        Assert.assertEquals("chrom\tpos\nchr1\t9\n", Files.readString(out));
        Assert.assertFalse("link written for a query that failed", Files.exists(link));
    }

    @Test
    public void teeWriteBeforeAStepThatFailsToParseWritesNothing() throws Exception {
        Path out = root.resolve("result_cache/tee.gor");

        assertFailsToParse("gor source/var/left.gor | tee >(write result_cache/tee.gor) | select F");

        Assert.assertFalse("tee output written for a query that failed", Files.exists(out));
    }

    @Test
    public void openFilesSeesAFileThisProcessHasOpen() throws Exception {
        // Guards the leak tests against an lsof that lists nothing: they would then pass without the fix.
        try (var in = Files.newInputStream(right)) {
            Assert.assertTrue(openFiles().contains(right.toRealPath().toString()));
        }
        assertClosed(right);
    }

    @Test
    public void queryThatParsesStillRuns() throws Exception {
        String result = TestUtils.runGorPipe("gor source/var/left.gor | join -snpsnp source/var/right.gor | select 1,2",
                root.toString(), true);

        Assert.assertEquals("chrom\tpos\nchr1\t1\n", result);
        assertClosed(right);
    }

    private void assertFailsToParse(String query) {
        Assert.assertThrows(GorParsingException.class, () -> TestUtils.runGorPipe(query, root.toString(),
                root.resolve("result_cache").toString(), true, null, new String[]{"result_cache"}));
    }

    private void assertClosed(Path file) throws Exception {
        String path = file.toRealPath().toString();
        Assert.assertFalse("still open: " + file.getFileName(), openFiles().stream().anyMatch(path::equals));
    }

    /**
     * The files this JVM has open. Asks lsof about this process only: a system-wide lsof (TestUtils.isFileClosed)
     * can stall for minutes on a busy machine.
     */
    private List<String> openFiles() throws Exception {
        Path out = tmp.newFile().toPath();
        Process lsof = new ProcessBuilder("lsof", "-w", "-Fn", "-p", String.valueOf(ProcessHandle.current().pid()))
                .redirectErrorStream(true).redirectOutput(out.toFile()).start();
        Assert.assertTrue("lsof timed out", lsof.waitFor(60, TimeUnit.SECONDS));
        Assert.assertEquals("lsof failed: " + Files.readString(out), 0, lsof.exitValue());
        return Files.readAllLines(out).stream()
                .filter(line -> line.startsWith("n"))
                .map(line -> line.substring(1))
                .collect(Collectors.toList());
    }
}
