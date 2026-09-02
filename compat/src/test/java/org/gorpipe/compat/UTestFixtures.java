package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestFixtures {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void writesReferenceBuildLayout() {
        Path root = tmp.getRoot().toPath();
        Path config = Fixtures.writeReferenceBuild(root);

        Assert.assertTrue(Files.exists(root.resolve("chromSeq/chr1.txt")));
        Assert.assertTrue(Files.exists(root.resolve("chromSeq/chr2.txt")));
        Assert.assertTrue(Files.exists(root.resolve("buildsize.gor")));
        Assert.assertTrue(Files.exists(root.resolve("buildsplit.txt")));
        Assert.assertEquals(root.resolve("gor_config.txt"), config);
    }

    @Test
    public void sizeAndSplitFilesHaveNoHeaderLine() throws IOException {
        // gor's parser reads these as bare contig/value pairs. A header line
        // produces NumberFormatException: For input string: "Size".
        Path root = tmp.getRoot().toPath();
        Fixtures.writeReferenceBuild(root);

        String firstSizeLine = Files.readAllLines(root.resolve("buildsize.gor")).get(0);
        Assert.assertTrue(firstSizeLine, firstSizeLine.startsWith("chr"));
        Assert.assertFalse(firstSizeLine.toLowerCase().contains("size"));

        String firstSplitLine = Files.readAllLines(root.resolve("buildsplit.txt")).get(0);
        Assert.assertTrue(firstSplitLine, firstSplitLine.startsWith("chr"));
    }

    @Test
    public void sequenceFilesAreOneBytePerBaseWithNoNewlines() throws IOException {
        Path root = tmp.getRoot().toPath();
        Fixtures.writeReferenceBuild(root);

        String chr1 = Files.readString(root.resolve("chromSeq/chr1.txt"));
        Assert.assertEquals(Fixtures.referenceSequence("chr1"), chr1);
        Assert.assertFalse("sequence must contain no newlines", chr1.contains("\n"));
    }

    @Test
    public void engineReadsTheSyntheticReferenceBuild() {
        Path root = tmp.getRoot().toPath();
        Path config = Fixtures.writeReferenceBuild(root);

        CompatResult r = CompatExecutor.run("gorrow chr1,10 | calc R refbase(chrom,pos)",
                root, config);

        Assert.assertFalse(r.errorMessage, r.failed());
        Assert.assertEquals("chrom\tpos\tR", r.header);

        char expectedBase = Fixtures.referenceSequence("chr1").charAt(9);
        Assert.assertEquals("chr1\t10\t" + expectedBase, r.rows.get(0));
    }

    @Test
    public void carriesTheShapesTheReadAndStatisticsCommandsNeed() {
        // A command that requires a CIGAR, Flag or p-value column cannot be
        // exercised by the primary fixture, and errored on its input shape instead
        // of on the flag being tested.
        java.util.Map<String, String> byPath = new java.util.HashMap<>();
        for (CompatInput in : Fixtures.canonicalInputs()) {
            byPath.put(in.path, in.content);
        }
        Assert.assertTrue("reads.gor must carry a CIGAR column",
                byPath.getOrDefault("reads.gor", "").contains("CIGAR"));
        Assert.assertTrue("reads.gor must carry a Flag column",
                byPath.getOrDefault("reads.gor", "").contains("Flag"));
        Assert.assertTrue("reads.gor must carry SEQ and QUAL columns",
                byPath.getOrDefault("reads.gor", "").contains("SEQ")
                        && byPath.getOrDefault("reads.gor", "").contains("QUAL"));
        Assert.assertTrue("pvalues.gor must carry a PVal column",
                byPath.getOrDefault("pvalues.gor", "").contains("PVal"));
    }

    @Test
    public void canonicalInputsAreSelfConsistent() {
        for (CompatInput in : Fixtures.canonicalInputs()) {
            Assert.assertNotNull(in.path);
            Assert.assertNotNull("canonical fixture " + in.path + " has no content", in.content);
            Assert.assertTrue("canonical fixture " + in.path + " must be tab separated",
                    in.content.contains("\t"));
        }
    }
}
