package gorsat.QueryHandlers;

import gorsat.DynIterator;
import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import gorsat.process.PipeInstance;
import gorsat.process.PipeOptions;
import gorsat.process.TestSessionFactory;
import org.gorpipe.exceptions.GorCancelledException;
import org.gorpipe.gor.session.GorSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * ENGKNOW-3979: output of a cancelled parallel part must never be committed, whether or not the part's
 * thread still has its interrupt flag set.
 */
public class UTestGeneralQueryHandler {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private Path workPath;
    private GorSession session;

    @Before
    public void setUp() {
        GorPipeCommands.register();
        GorInputSources.register();
        DynIterator.createGorIterator_$eq(PipeInstance::createGorIterator);
        workPath = workDir.getRoot().toPath();
        var options = new PipeOptions();
        options.gorRoot_$eq(workPath.toString());
        options.cacheDir_$eq(workPath.resolve("cache").toString());
        options.requestId_$eq("test");
        session = new TestSessionFactory(options, null, false, null, null).create();
    }

    @After
    public void tearDown() {
        if (session != null) session.close();
    }

    @Test
    public void cancelledQueryIsNotCommitted() throws IOException {
        var outfile = workPath.resolve("out.gor");

        Assert.assertThrows(GorCancelledException.class, () -> GeneralQueryHandler.runCommand(
                session.getGorContext(), "gorrows -p chr1:1-100", outfile.toString(), false, false, () -> true));

        Assert.assertFalse("Cancelled output must not be committed", Files.exists(outfile));
        assertNoFilesLeft();
    }

    @Test
    public void cancelledDictionaryIsNotCommitted() throws IOException {
        Files.writeString(workPath.resolve("a.tsv"), "#col\n1\n");
        Files.writeString(workPath.resolve("b.tsv"), "#col\n2\n");
        var outfile = workPath.resolve("out.nord");

        Assert.assertThrows(GorCancelledException.class, () -> GeneralQueryHandler.runCommand(
                session.getGorContext(), "NORDICT a.tsv a b.tsv b", outfile.toString(), false, false, () -> true));

        Assert.assertFalse("Cancelled dictionary must not be committed", Files.exists(outfile));
    }

    @Test
    public void notCancelledQueryIsCommitted() {
        var outfile = workPath.resolve("out.gor");

        GeneralQueryHandler.runCommand(
                session.getGorContext(), "gorrows -p chr1:1-100", outfile.toString(), false, false, () -> false);

        Assert.assertTrue(Files.exists(outfile));
    }

    private void assertNoFilesLeft() throws IOException {
        try (Stream<Path> files = Files.list(workPath)) {
            var left = files.filter(p -> p.getFileName().toString().startsWith("out")).toList();
            Assert.assertTrue("Temp output must be removed, found: " + left, left.isEmpty());
        }
    }
}
