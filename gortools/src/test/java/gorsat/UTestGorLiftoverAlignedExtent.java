package gorsat;

import org.gorpipe.exceptions.GorException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ENGKNOW-4207: LIFTOVER -var -aligned and LIFTOVER_ANNOTATE -var -aligned report how many target bases left and
 * right of the lifted position stay inside the same gap-free alignment, so a caller comparing the reference windows
 * around a variant can leave out the bases a nearby build indel shifts.
 * <p>
 * The fixture is the CG-801 case (hg19 chr11:6630032): the 21-base source window TCCATCTCCCCCATCCCCTAG around the
 * variant, and a target build with one C fewer in the homopolymer three bases left of it. Position by position the
 * windows differ in 7 bases, over the clean-lift limit of 6, although the variant lifted correctly.
 */
public class UTestGorLiftoverAlignedExtent {

    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    private static final String CHAIN_HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore\n";

    /** Source chr1, 60 bases: the window is bases 21..41 and the variant base 31 (a C in the CCCCC run 28..32). */
    private static final String SOURCE = "ACGTACGTACGTACGTACGG" + "TCCATCTCCCCCATCCCCTAG" + "ACGTTGCAACGTTGCAACG";
    /** The target build: SOURCE without base 28. */
    private static final String TARGET = SOURCE.substring(0, 27) + SOURCE.substring(28);

    private static String deletionChain;
    private static String minusDeletionChain;
    private static String singleBlockChain;
    private static String tieChain;
    private static String variants;

    @BeforeClass
    public static void setUp() throws IOException {
        Assert.assertEquals(60, SOURCE.length());
        Assert.assertEquals('C', SOURCE.charAt(27));
        // base 28 is deleted in the target: source [0,27) -> [0,27), source [28,60) -> [27,59)
        deletionChain = write("deletion.gor", CHAIN_HEADER
                + "chr1\t0\t27\tchr1\t0\t27\t+\t100\n"
                + "chr1\t28\t60\tchr1\t27\t59\t+\t100\n");
        // the same alignment against the reverse complement of the target
        minusDeletionChain = write("minus.gor", CHAIN_HEADER
                + "chr1\t0\t27\tchr1\t32\t59\t-\t100\n"
                + "chr1\t28\t60\tchr1\t0\t32\t-\t100\n");
        singleBlockChain = write("single.gor", CHAIN_HEADER + "chr1\t0\t60\tchr1\t0\t60\t+\t100\n");
        tieChain = write("tie.gor", CHAIN_HEADER
                + "chr1\t0\t60\tchr1\t0\t60\t+\t100\n"
                + "chr1\t0\t60\tchr2\t0\t60\t+\t100\n");
        variants = write("variants.gor", "Chrom\tPos\tRef\tAlt\n"
                + "chr1\t28\tC\tA\n"
                + "chr1\t31\tC\tT\n"
                + "chr1\t45\tT\tG\n");
    }

    private static String write(String name, String content) throws IOException {
        Path p = TMP.getRoot().toPath().resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toAbsolutePath().toString();
    }

    /** The data rows of a result, without the header. */
    private static String[] rows(String result) {
        String[] lines = result.split("\n");
        String[] r = new String[lines.length - 1];
        System.arraycopy(lines, 1, r, 0, r.length);
        return r;
    }

    private static String header(String result) {
        return result.substring(0, result.indexOf('\n'));
    }

    /** 1-based inclusive bases from..to of a sequence. */
    private static String bases(String seq, int from, int to) {
        return seq.substring(from - 1, to);
    }

    /** The template's count: positions where the two windows do not carry the same base. */
    private static int diff(String a, String b) {
        Assert.assertEquals(a.length(), b.length());
        int d = 0;
        for (int k = 0; k < a.length(); k++) {
            if (a.charAt(k) != b.charAt(k)) {
                d++;
            }
        }
        return d;
    }

    @Test
    public void positionByPositionWindowFailsTheCleanSnvNextToABuildIndel() {
        // the variant lifts to target 30; the template compares source 21..41 with target 20..40
        Assert.assertEquals(7, diff(bases(SOURCE, 21, 41), bases(TARGET, 20, 40)));
    }

    @Test
    public void liftoverReportsTheAlignedExtentAroundTheVariant() {
        String result = TestUtils.runGorPipe("gor " + variants + " | liftover " + deletionChain
                + " -var -build hg19 -aligned");
        Assert.assertEquals("Chrom\tPos\tRef\tAlt\thg19_Chrom\thg19_Pos\thg19_qStrand\thg19_liftoverStatus"
                + "\thg19_alignedLeft\thg19_alignedRight", header(result));
        Assert.assertArrayEquals(new String[]{
                "chr1\t0\tC\tA\tchr1\t28\t\tunmapped\t0\t0",
                "chr1\t30\tC\tT\tchr1\t31\t+\tmapped\t2\t29",
                "chr1\t44\tT\tG\tchr1\t45\t+\tmapped\t16\t15"}, rows(result));
    }

    @Test
    public void windowCutToTheAlignedExtentNoLongerDiverges() {
        String row = rows(TestUtils.runGorPipe("gor " + variants + " | liftover " + deletionChain
                + " -var -build hg19 -aligned | where hg19_Pos = 31"))[0];
        String[] c = row.split("\t");
        int tpos = Integer.parseInt(c[1]);
        int left = Math.min(Integer.parseInt(c[8]), 10);
        int right = Math.min(Integer.parseInt(c[9]), 10);
        Assert.assertEquals(2, left);
        Assert.assertEquals(10, right);
        Assert.assertEquals(0, diff(bases(SOURCE, 31 - left, 31 + right), bases(TARGET, tpos - left, tpos + right)));
    }

    @Test
    public void realDivergenceInsideTheAlignedWindowStillShows() {
        // no indel: the window is the whole block, so six substituted bases are all compared
        StringBuilder diverged = new StringBuilder(SOURCE);
        for (int p : new int[]{24, 26, 29, 33, 36, 39}) {
            diverged.setCharAt(p - 1, SOURCE.charAt(p - 1) == 'A' ? 'G' : 'A');
        }
        String row = rows(TestUtils.runGorPipe("gor " + variants + " | liftover " + singleBlockChain
                + " -var -build hg19 -aligned | where hg19_Pos = 31"))[0];
        String[] c = row.split("\t");
        int left = Math.min(Integer.parseInt(c[8]), 10);
        int right = Math.min(Integer.parseInt(c[9]), 10);
        Assert.assertEquals(10, left);
        Assert.assertEquals(10, right);
        Assert.assertEquals(6, diff(bases(SOURCE, 31 - left, 31 + right), bases(diverged.toString(), 31 - left,
                31 + right)));
    }

    @Test
    public void minusStrandLiftPutsTheIndelOnTheRight() {
        String row = rows(TestUtils.runGorPipe("gor " + variants + " | liftover " + minusDeletionChain
                + " -var -build hg19 -aligned | where hg19_Pos = 31"))[0];
        // REF and ALT are reverse complemented; source bases 32..60 lie left of target 30
        Assert.assertEquals("chr1\t30\tG\tA\tchr1\t31\t-\tmapped\t29\t2", row);
    }

    @Test
    public void liftoverAnnotateReportsTheSameExtent() {
        String result = TestUtils.runGorPipe("gor " + variants + " | liftover_annotate " + deletionChain
                + " -var -aligned");
        Assert.assertEquals("Chrom\tPos\tRef\tAlt\tliftover_status\tliftover_chrom\tliftover_pos\tliftover_ref"
                + "\tliftover_alt\tliftover_strand\tliftover_aligned_left\tliftover_aligned_right", header(result));
        Assert.assertArrayEquals(new String[]{
                "chr1\t28\tC\tA\tunmapped\t\t0\t\t\t\t0\t0",
                "chr1\t31\tC\tT\tmapped\tchr1\t30\tC\tT\t+\t2\t29",
                "chr1\t45\tT\tG\tmapped\tchr1\t44\tT\tG\t+\t16\t15"}, rows(result));
    }

    @Test
    public void passthroughRowHasNoExtent() {
        String result = TestUtils.runGorPipe("gor " + variants + " | liftover_annotate " + deletionChain
                + " -var -aligned -passthrough chr1");
        Assert.assertEquals("chr1\t31\tC\tT\tpassthrough\tchr1\t31\tC\tT\t\t0\t0", rows(result)[1]);
    }

    @Test
    public void liftoverAnnotateMinusStrand() {
        String row = rows(TestUtils.runGorPipe("gor " + variants + " | liftover_annotate " + minusDeletionChain
                + " -var -aligned"))[1];
        Assert.assertEquals("chr1\t31\tC\tT\tmapped\tchr1\t30\tG\tA\t-\t29\t2", row);
    }

    @Test
    public void unsupportedContigAndAmbiguousRowsHaveNoExtent() {
        String unsupported = rows(TestUtils.runGorPipe("gor " + variants + " | liftover_annotate " + deletionChain
                + " -var -aligned -mapped chr2"))[1];
        Assert.assertEquals("chr1\t31\tC\tT\tunsupported_contig\t\t0\t\t\t\t0\t0", unsupported);
        // two blocks with the same best score contain every position
        String tie = rows(TestUtils.runGorPipe("gor " + variants + " | liftover_annotate " + tieChain
                + " -var -aligned -overlap best"))[1];
        Assert.assertEquals("chr1\t31\tC\tT\tambiguous\t\t0\t\t\t\t0\t0", tie);
        String relocated = rows(TestUtils.runGorPipe("gor " + variants + " | liftover " + deletionChain
                + " -var -build hg19 -aligned -mapped chr2 | where hg19_Pos = 31"))[0];
        Assert.assertEquals("chr1\t0\tC\tT\tchr1\t31\t\tunsupported_contig\t0\t0", relocated);
    }

    @Test
    public void liftoverAnnotateNorInput() {
        String result = TestUtils.runGorPipe("nor " + variants + " | liftover_annotate " + deletionChain
                + " -var -aligned -chrom Chrom -pos Pos");
        String row = rows(result)[1];
        Assert.assertTrue(row, row.endsWith("\tchr1\t31\tC\tT\tmapped\tchr1\t30\tC\tT\t+\t2\t29"));
    }

    @Test
    public void withoutTheOptionTheOutputIsUnchanged() {
        String with = TestUtils.runGorPipe("gor " + variants + " | liftover " + deletionChain + " -var -build hg19"
                + " -aligned | hide hg19_alignedLeft,hg19_alignedRight");
        String without = TestUtils.runGorPipe("gor " + variants + " | liftover " + deletionChain + " -var -build hg19");
        Assert.assertEquals(without, with);
    }

    @Test
    public void allModeReportsTheExtentOfEachBlock() {
        String result = TestUtils.runGorPipe("gor " + variants + " | liftover " + deletionChain
                + " -var -build hg19 -aligned -all | where hg19_Pos = 31");
        Assert.assertEquals("chr1\t30\tC\tT\tchr1\t31\t+\tmapped\t2\t29\t100.0", rows(result)[0]);
    }

    @Test
    public void alignedIsOnlyValidWithVar() {
        for (String cmd : new String[]{"liftover " + deletionChain + " -snp -aligned",
                "liftover " + deletionChain + " -seg -aligned",
                "liftover_annotate " + deletionChain + " -snp -aligned",
                "liftover_annotate " + deletionChain + " -seg -aligned"}) {
            try {
                TestUtils.runGorPipe("gor " + variants + " | " + cmd);
                Assert.fail("expected -aligned to be rejected: " + cmd);
            } catch (GorException e) {
                Assert.assertTrue(cmd + ": " + e.getMessage(), e.getMessage().contains("-aligned is only valid with -var"));
            }
        }
    }
}
