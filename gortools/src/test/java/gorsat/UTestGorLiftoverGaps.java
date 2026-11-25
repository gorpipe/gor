package gorsat;

import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorParsingException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Query level tests for the parsing branches of LIFTOVER and LIFTOVER_ANNOTATE that the other suites do not reach
 * (ENGKNOW-4117, Task 12 coverage gate).
 */
public class UTestGorLiftoverGaps {

    private static Path tmpdir;
    private static String chain;
    private static String input;

    @BeforeClass
    public static void setUp() throws IOException {
        GorPipeCommands.register();
        GorInputSources.register();
        tmpdir = Files.createTempDirectory("liftover-gaps");
        chain = write("chain.gor", "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore\n"
                + "chr1\t100\t200\tchr1\t1100\t1200\t+\t1000\n");
        input = write("in.gor", "Chrom\tStart\tEnd\nchr1\t120\t180\n");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(tmpdir.toFile());
    }

    private static String write(String name, String content) throws IOException {
        File f = new File(tmpdir.toFile(), name);
        FileUtils.write(f, content, StandardCharsets.UTF_8);
        return f.getAbsolutePath();
    }

    private static String error(String query) {
        try {
            TestUtils.runGorPipe(query);
        } catch (GorParsingException e) {
            return e.getMessage();
        }
        throw new AssertionError("expected a GorParsingException: " + query);
    }

    /** A span mode needs the end column; the message names the flag, or -seg when LIFTOVER defaulted to it. */
    @Test
    public void spanModeNeedsAnEndColumn() {
        String twoColumns = "gor " + input + " | select 1,2 | liftover " + chain;
        Assert.assertTrue(error(twoColumns).contains("LIFTOVER -seg requires an end column (#3)"));
        Assert.assertTrue(error(twoColumns + " -seg").contains("LIFTOVER -seg requires an end column (#3)"));
        Assert.assertTrue(error(twoColumns + " -bam").contains("LIFTOVER -bam requires an end column (#3)"));
        // a point mode does not
        Assert.assertEquals("Chrom\tStart\thgold_Chrom\thgold_Start\thgold_qStrand\thgold_liftoverStatus\n"
                        + "chr1\t1120\tchr1\t120\t+\tmapped\n",
                TestUtils.runGorPipe("gor " + input + " | select 1,2 | liftover " + chain + " -snp"));
    }

    /** A step before LIFTOVER can leave a header with one column; it is refused when the query is parsed. */
    @Test
    public void inputNeedsAtLeastTwoColumns() {
        Assert.assertTrue(error("gor " + input + " | select 1 | liftover " + chain + " -snp")
                .contains("Number of columns for left header needs to be at least 2"));
    }

    @Test
    public void annotateToleranceMustBeInZeroToOne() {
        for (String t : new String[]{"-0.1", "1", "1.5"}) {
            Assert.assertTrue(t, error("gor " + input + " | liftover_annotate " + chain + " -seg -tolerance " + t)
                    .contains("Invalid -tolerance"));
        }
        Assert.assertTrue(TestUtils.runGorPipe("gor " + input + " | liftover_annotate " + chain
                + " -seg -tolerance 0.5").contains("mapped"));
    }
}
