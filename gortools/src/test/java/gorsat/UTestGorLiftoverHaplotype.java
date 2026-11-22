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
import java.util.Arrays;

/**
 * ENGKNOW-4224: LIFTOVER -var -haplotype rewrites the alleles of an indel next to a build difference, as BCFtools
 * +liftover does, so the target reference plus the lifted variant is the source haplotype.
 * <p>
 * The fixture is the hg19 chr18:28666426-28666626 (SOURCE) and hg38 chr18:31086460-31086663 (TARGET) reference,
 * where hg38 carries three bases (TAA) the hg19 chain gap skips after base 101: source [0,101) maps to target [0,101)
 * and source [101,201) to target [104,204). ClinVar 1227234 (hg19 chr18:28666525 A&gt;AT) is base 100 here and ClinVar
 * 1968261 (chr18:28666526 T&gt;TATGA) base 101. A plain lift keeps their alleles at target base 100 and 101, which on
 * hg38 is another haplotype. Every expected row below is the one BCFtools 1.25 +liftover writes for the same fasta and
 * chain (its new REF and the source ALT; the source REF, which it keeps as a second ALT, is dropped).
 */
public class UTestGorLiftoverHaplotype {

    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    private static final String CHAIN_HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore\n";

    static final String SOURCE = "AATAATGTTTCTAAAATGAGATTCACAACATACTTTTCAAAAACACATCTACAGGAGTTATACAGGTACTATTGAATAGC"
            + "CACGTTATAATCAGGTTTTATTGTTTATGTTACCTCTCTGTCTAGCTGAGATGATGTTGTGGTGATCACGCCTGTAGTTGGATGCATAGAAAATAGGGTGGG"
            + "TGATGGTGGCACCTGCCCA";
    static final String TARGET = SOURCE.substring(0, 101) + "TAA" + SOURCE.substring(101);

    private static String srcSeq;
    private static String tgtSeq;
    private static String tgtMinusSeq;
    private static String fwdChain;
    private static String revChain;
    private static String minusChain;
    private static String variants;

    @BeforeClass
    public static void setUp() throws IOException {
        Assert.assertEquals(201, SOURCE.length());
        Assert.assertEquals("GTTTTATTAATGTTT", TARGET.substring(94, 109));
        srcSeq = chromSeq("src", SOURCE);
        tgtSeq = chromSeq("tgt", TARGET);
        tgtMinusSeq = chromSeq("tgtminus", revcomp(TARGET));
        fwdChain = write("fwd.gor", CHAIN_HEADER
                + "chr1\t0\t101\tchr1\t0\t101\t+\t1000\n"
                + "chr1\t101\t201\tchr1\t104\t204\t+\t1000\n");
        // target -> source: the three target bases are a source-only gap
        revChain = write("rev.gor", CHAIN_HEADER
                + "chr1\t0\t101\tchr1\t0\t101\t+\t1000\n"
                + "chr1\t104\t204\tchr1\t101\t201\t+\t1000\n");
        // the forward alignment against the reverse complement of the target
        minusChain = write("minus.gor", CHAIN_HEADER
                + "chr1\t0\t101\tchr1\t103\t204\t-\t1000\n"
                + "chr1\t101\t201\tchr1\t0\t100\t-\t1000\n");
        variants = write("variants.gor", "Chrom\tPos\tRef\tAlt\tId\n"
                + "chr1\t50\tTA\tT\tnogap\n"
                + "chr1\t100\tA\tAT\tins_a\n"
                + "chr1\t101\tT\tC\tsnv\n"
                + "chr1\t101\tT\tTATGA\tins_b\n"
                + "chr1\t101\tT\tTTAA\tswap\n");
    }

    private static String write(String name, String content) throws IOException {
        Path p = TMP.getRoot().toPath().resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toAbsolutePath().toString();
    }

    /** A chromSeq build directory holding chr1. */
    private static String chromSeq(String name, String seq) throws IOException {
        Path dir = TMP.newFolder(name).toPath();
        Files.writeString(dir.resolve("chr1.txt"), seq, StandardCharsets.US_ASCII);
        return dir.toAbsolutePath().toString();
    }

    private static String revcomp(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append("ACGT".charAt(3 - "ACGT".indexOf(s.charAt(i))));
        }
        return sb.toString();
    }

    private static String liftover(String input, String chain, String options) {
        return TestUtils.runGorPipe("gor " + input + " | liftover " + chain + " -var " + options);
    }

    private static String haplotype(String input, String chain, String src, String tgt) {
        return liftover(input, chain, "-haplotype -srcseq " + src + " -tgtseq " + tgt);
    }

    private static String[] rows(String result) {
        String[] lines = result.split("\n");
        return Arrays.copyOfRange(lines, 1, lines.length);
    }

    /** The row of result whose Id (column 5) is id. */
    private static String row(String result, String id) {
        for (String r : rows(result)) {
            if (r.split("\t")[4].equals(id)) {
                return r;
            }
        }
        throw new AssertionError("no row " + id + " in\n" + result);
    }

    @Test
    public void rewritesTheChr18RecordsAsBcftools() {
        String result = haplotype(variants, fwdChain, srcSeq, tgtSeq);
        Assert.assertEquals("Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus"
                + "\thgold_alleleFix", result.substring(0, result.indexOf('\n')));
        // BCFtools: chr1 101 TTAA T,TT SWAP=-1 and chr1 101 TTAA T,TATGA SWAP=-1
        Assert.assertEquals("chr1\t101\tTTAA\tTT\tins_a\tchr1\t100\t+\tmapped\trewritten", row(result, "ins_a"));
        Assert.assertEquals("chr1\t101\tTTAA\tTATGA\tins_b\tchr1\t101\t+\tmapped\trewritten", row(result, "ins_b"));
    }

    @Test
    public void rewrittenAllelesGiveTheSourceHaplotype() {
        String result = haplotype(variants, fwdChain, srcSeq, tgtSeq);
        for (String id : new String[]{"ins_a", "ins_b"}) {
            String[] c = row(result, id).split("\t");
            int pos = Integer.parseInt(c[1]);
            String target = TARGET.substring(0, pos - 1) + c[3] + TARGET.substring(pos - 1 + c[2].length());
            int sPos = Integer.parseInt(c[6]);
            String[] in = row(TestUtils.runGorPipe("gor " + variants), id).split("\t");
            String source = SOURCE.substring(0, sPos - 1) + in[3] + SOURCE.substring(sPos - 1 + in[2].length());
            Assert.assertEquals(id, source, target);
        }
    }

    @Test
    public void otherRowsKeepThePlainLift() {
        String plain = liftover(variants, fwdChain, "");
        String result = haplotype(variants, fwdChain, srcSeq, tgtSeq);
        // an indel away from the gap, and an SNV next to it: only indels are rewritten
        Assert.assertEquals(row(plain, "nogap") + "\tnone", row(result, "nogap"));
        Assert.assertEquals(row(plain, "snv") + "\tnone", row(result, "snv"));
        // the target reference is the ALT (BCFtools SWAP=1): reported, not rewritten
        Assert.assertEquals(row(plain, "swap") + "\tswap", row(result, "swap"));
    }

    @Test
    public void withoutTheOptionTheOutputIsUnchanged() {
        String plain = liftover(variants, fwdChain, "");
        Assert.assertEquals("Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus",
                plain.substring(0, plain.indexOf('\n')));
        Assert.assertEquals("chr1\t100\tA\tAT\tins_a\tchr1\t100\t+\tmapped", row(plain, "ins_a"));
        Assert.assertEquals("chr1\t101\tT\tTATGA\tins_b\tchr1\t101\t+\tmapped", row(plain, "ins_b"));
    }

    @Test
    public void minusStrand() {
        String result = haplotype(variants, minusChain, srcSeq, tgtMinusSeq);
        // BCFtools: chr1 99 CATT C,CA and chr1 100 ATTA A,ATCAT (FLIP, SWAP=-1)
        Assert.assertEquals("chr1\t99\tCATT\tCA\tins_a\tchr1\t100\t-\tmapped\trewritten", row(result, "ins_a"));
        Assert.assertEquals("chr1\t100\tATTA\tATCAT\tins_b\tchr1\t101\t-\tmapped\trewritten", row(result, "ins_b"));
        Assert.assertTrue(row(result, "swap").endsWith("\tswap"));
        Assert.assertTrue(row(result, "snv").endsWith("\tnone"));
    }

    @Test
    public void anchorInASourceOnlyGapIsPaddedToTheNextBlock() throws IOException {
        // target -> source: A>AT at 100 extends to ATTA, whose last base is in the gap, so it is padded to the next
        // block and realigned. BCFtools: chr1 100 A AT,ATT SWAP=-1.
        String input = write("rev_variants.gor", "Chrom\tPos\tRef\tAlt\tId\n"
                + "chr1\t99\tTA\tT\trev_del\n"
                + "chr1\t100\tA\tAT\trev_ins\n");
        String result = haplotype(input, revChain, tgtSeq, srcSeq);
        Assert.assertEquals("chr1\t100\tA\tATT\trev_ins\tchr1\t100\t+\tmapped\trewritten", row(result, "rev_ins"));
        Assert.assertEquals("chr1\t99\tTA\tT\trev_del\tchr1\t99\t+\tmapped\tnone", row(result, "rev_del"));
    }

    @Test
    public void refMismatchAndUnsupportedAllelesAreNotRewritten() throws IOException {
        String input = write("odd_variants.gor", "Chrom\tPos\tRef\tAlt\tId\n"
                + "chr1\t100\tG\tGT\twrong_ref\n"
                + "chr1\t100\tA\tAT,ATT\tmulti\n"
                + "chr1\t100\tA\t<INS>\tsymbolic\n"
                + "chr1\t100\ta\tat\tlower\n");
        String result = haplotype(input, fwdChain, srcSeq, tgtSeq);
        Assert.assertEquals("chr1\t100\tG\tGT\twrong_ref\tchr1\t100\t+\tmapped\tref_mismatch", row(result, "wrong_ref"));
        Assert.assertTrue(row(result, "multi").endsWith("\tunresolved"));
        Assert.assertTrue(row(result, "symbolic").endsWith("\tunresolved"));
        // lower case alleles are read as upper case and written as BCFtools writes them
        Assert.assertEquals("chr1\t101\tTTAA\tTT\tlower\tchr1\t100\t+\tmapped\trewritten", row(result, "lower"));
    }

    @Test
    public void noTargetSequenceIsUnresolved() throws IOException {
        String empty = TMP.newFolder("empty").toPath().toAbsolutePath().toString();
        String result = haplotype(variants, fwdChain, srcSeq, empty);
        Assert.assertTrue(row(result, "ins_a").endsWith("\tmapped\tunresolved"));
        Assert.assertTrue(row(result, "nogap").endsWith("\tnone"));
    }

    @Test
    public void withAlignedTheFixIsLast() {
        String result = liftover(variants, fwdChain, "-aligned -haplotype -srcseq " + srcSeq + " -tgtseq " + tgtSeq);
        Assert.assertTrue(result.startsWith("Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand"
                + "\thgold_liftoverStatus\thgold_alignedLeft\thgold_alignedRight\thgold_alleleFix\n"));
        Assert.assertEquals("chr1\t101\tTTAA\tTT\tins_a\tchr1\t100\t+\tmapped\t99\t1\trewritten",
                row(result, "ins_a"));
    }

    @Test
    public void optionErrors() {
        String seqs = " -srcseq " + srcSeq + " -tgtseq " + tgtSeq;
        assertFails("gor " + variants + " | liftover " + fwdChain + " -snp -haplotype" + seqs, "only valid with -var");
        assertFails("gor " + variants + " | liftover " + fwdChain + " -var -all -haplotype" + seqs,
                "cannot be combined with -all");
        assertFails("gor " + variants + " | liftover " + fwdChain + " -var -haplotype -srcseq " + srcSeq,
                "requires -srcseq and -tgtseq");
        assertFails("gor " + variants + " | liftover " + fwdChain + " -var -srcseq " + srcSeq,
                "-srcseq is only valid with -haplotype");
        assertFails("gor " + variants + " | liftover " + fwdChain + " -var -haplotype -srcseq " + srcSeq
                + " -tgtseq " + srcSeq + "_missing", "does not exist");
        assertFails("gor " + variants + " | liftover " + fwdChain + " -var -haplotype" + seqs + " -srcseq " + srcSeq,
                "given more than once");
    }

    private static void assertFails(String query, String message) {
        try {
            TestUtils.runGorPipe(query);
            Assert.fail("expected an error for " + query);
        } catch (GorException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(message));
        }
    }
}
