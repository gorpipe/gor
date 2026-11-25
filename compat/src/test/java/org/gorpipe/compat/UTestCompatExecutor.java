package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class UTestCompatExecutor {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void createMaterialisesIntoTheCaseRootNotTheSharedTempDir() throws IOException {
        // CREATE and PGOR write their intermediate results to the cache directory,
        // which the engine defaults to java.io.tmpdir. Shared across cases and runs,
        // a case could then be served a result produced by another case or by an
        // earlier engine build. The executor must keep it inside the case root.
        Path root = tmp.getRoot().toPath();
        Files.writeString(root.resolve("in.gor"), "Chrom\tPos\nchr1\t100\nchr2\t200\n");
        CompatResult result = CompatExecutor.run(
                "create ##a## = gor in.gor | top 1; gor [##a##]", root);

        Assert.assertFalse(result.errorMessage, result.failed());
        Assert.assertEquals(1, result.rows.size());
        Path cache = root.resolve(CompatExecutor.CACHE_DIR);
        Assert.assertTrue("no cache directory under the case root", Files.isDirectory(cache));
        try (Stream<Path> files = Files.walk(cache)) {
            Assert.assertTrue("CREATE wrote nothing under the case root",
                    files.anyMatch(Files::isRegularFile));
        }
    }

    @Test
    public void runsNorQueryAndSerialisesOutput() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("norrows 1 | calc X 1+1", root);

        Assert.assertFalse(result.errorMessage, result.failed());
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\tX", result.header);
        Assert.assertEquals(1, result.rows.size());
        Assert.assertEquals("chrN\t0\t0\t2", result.rows.get(0));
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\tX\nchrN\t0\t0\t2\n",
                result.serialised());
    }

    @Test
    public void runsMultiRowNorQuery() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("norrows 2", root);

        Assert.assertFalse(result.errorMessage, result.failed());
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum", result.header);
        Assert.assertEquals(2, result.rows.size());
        Assert.assertEquals("chrN\t0\t0", result.rows.get(0));
        Assert.assertEquals("chrN\t0\t1", result.rows.get(1));
    }

    @Test
    public void runsGorRowQuery() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("gorrow chr1,100 | calc Ref 'A'", root);

        Assert.assertFalse(result.errorMessage, result.failed());
        Assert.assertEquals("chrom\tpos\tRef", result.header);
        Assert.assertEquals("chr1\t100\tA", result.rows.get(0));
    }

    @Test
    public void capturesParsingErrorInsteadOfThrowing() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("norrows 1 | nosuchcommand", root);

        Assert.assertTrue("expected the query to fail", result.failed());
        Assert.assertTrue("actual: " + result.errorMessage,
                result.errorMessage.contains("NOSUCHCOMMAND"));
    }

    @Test
    public void capturesRuntimeExceptionInsteadOfThrowing() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("norrows 1 | where", root);

        Assert.assertTrue("expected the query to fail", result.failed());
        Assert.assertNotNull(result.errorMessage);
        Assert.assertFalse(result.errorMessage.isEmpty());
    }

    @Test
    public void serialisingAFailedResultIsProgrammerError() {
        CompatResult failed = CompatResult.error("boom");
        try {
            failed.serialised();
            Assert.fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("boom"));
        }
    }
}
