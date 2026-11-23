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
 * ENGKNOW-4226: LIFTOVER -var -haplotype -pad lifts the rows a plain lift leaves unmapped, because their REF span
 * runs into a chain gap or sits inside one, the way BCFtools +liftover does: extend the alleles, lift the anchors,
 * pad an anchor that falls in a gap to the next block and read the target reference over the span.
 * <p>
 * Each fixture is a real window of the reference around one ClinVar variant, with its chain blocks moved into window
 * coordinates, and every expected row is BCFtools 1.24 +liftover's on the same fasta and chain:
 * <ul>
 * <li>SWAP_DEL: hg38 chr11:72138960-72139262 to hg19 chr11:71850006-71850306. ClinVar 768461, hg38 chr11:72139110
 * CTA&gt;C, is base 151: its C ends the first block and TA is a source-only gap. BCFtools: chr1 151 C CTA SWAP=1.</li>
 * <li>INGAP_SNV: hg19 chr12:7080062-7080362 to hg38 chr12:6970899-6971200. ClinVar 1206208, hg19 chr12:7080212 T&gt;G,
 * is base 151, which a 1/2 gap holds: no block lifts it. BCFtools: chr1 151 G T SWAP=1.</li>
 * <li>NOVEL_REF: hg38 chr1:235489320-235489621 to hg19 chr1:235652625-235652927. ClinVar 678121, hg38 chr1:235489470
 * CT&gt;C, is base 151, and hg19 has one more T after it, a target-only insertion. BCFtools: chr1 151 CTT CT,C
 * SWAP=-1.</li>
 * <li>DEL_19: hg19 chr11:6629878-6630179 to hg38 chr11:6608648-6608948, ClinVar 201786 (hg19 chr11:6630028 TC&gt;T).
 * </li>
 * <li>INS_38: hg38 chr2:31580661-31580961 to hg19 chr2:31805731-31806030, ClinVar 97389 (hg38 chr2:31580811 G&gt;GA).
 * </li>
 * <li>INS_19: hg19 chr7:100371324-100371624 to hg38 chr7:100773701-100774002, ClinVar 403617 (hg19 chr7:100371474
 * G&gt;GT).</li>
 * </ul>
 */
public class UTestGorLiftoverPad {

    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    private static final String CHAIN_HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore\n";

    static final String SWAP_DEL_SRC = "GTGCAGTCCCTGGAAGAAGAATGCCTGCTGCACGGCCAGCACCAGCCAGGAGCTGCACAAGGACACCTCCCGCCT"
            + "GTACAACTTTAACTGGGATCACTGTGGTAAGATGGAACCCACCTGCAAGCGCCACTTTATCCAGGACAGCTGTCTCTATGAGTGCTCACCCAACCTGGGG"
            + "CCCTGGATCCGGCAGGTATGAGTGCTGTTCCCACAAACATTAACCTCAGCAGAGGGCGGAGCCTGCCAGTTGCTGGCAGGGAGGGCTTGGTCCAGGAATT"
            + "CGGGTCTGAGGGTGGTGGACGCCCTGCC";
    static final String SWAP_DEL_TGT = SWAP_DEL_SRC.substring(0, 151) + SWAP_DEL_SRC.substring(153);

    static final String INGAP_SNV_SRC = "GCGCAACTTCCGGTATTGTTGCAAGATGGCCGCGCCCAGTGATGGATTCAAGCCTCGTGAACGAAGCGGTGGGG"
            + "AGCAGGCACAGGACTGGGATGCTCTGCCACCCAAGCGGCCCCGACTAGGGGCAGGAAACAAGATCGGAGGCCGTAGTTTATTGTGGTGCTGGAAGGGGCC"
            + "AGTCTGGAGACAGTCAAGGTAGTTTGGGACAGGAAGTGGAGAAGTAGTAAATCGATAGGTTGGGACTCCGTGGAATGAGGGTAAGGGGCCCAGAGTGGAT"
            + "GTAGAAAGCAGAGAGGGGTGAAAGATG";
    static final String INGAP_SNV_TGT = INGAP_SNV_SRC.substring(0, 150) + "GC" + INGAP_SNV_SRC.substring(151);

    static final String NOVEL_REF_SRC = "GCACGTTTCCTCATGCCCATTTCAGAGCTTTTACTTTGAGACACAAGTGTTTATGAGGATTAATTCTCTACCTT"
            + "TATTCTTCTAGTCAGACAAAATGGGGGAACAAGTAAACTGAAAGTTGCTAACTACCAAGTTACTTATGCATGAGTCCTTATTGCAATGCTATATTAAAGC"
            + "AAGTATGAATATGTCTGGCCTCGCTACAAGCAAATGTTCATATCATTTAAAAGCAAACTATAGTAAAGTCAGTTCTCATGCCCATACCTAAAAACATATC"
            + "TATATCTAAAATTTTCCTTTGCTGCCTT";
    static final String NOVEL_REF_TGT = NOVEL_REF_SRC.substring(0, 151) + "T" + NOVEL_REF_SRC.substring(151);

    static final String DEL_19_SRC = "CAGCGAGGTAGCAGTGGCTCTCATCATAATGGCCTTTTCATTCCAGGACCTGGTGGCAAATGGGGCCCTTGTCAGCATCTGTAACAAGTATGGAGAGATG"
            + "CCTGTGGACAAAGCCAAGGCACCCCTGAGAGAGCTTCTCCGAGGTCCATCTCCCCCATCCCCTAGCTTGTGTCCTCTCGTCCCTTCCCACCTGTCTTCTC"
            + "CCTCTGTACCACAGCTTAGGTTGTTTTTCTTCCCTAGAGCGGGCAGAGAAGATGGGCCAGAATCTCAACCGTATTCCATACAAGGACACATTCTGGAAGG"
            + "GG";
    static final String DEL_19_TGT = DEL_19_SRC.substring(0, 155) + DEL_19_SRC.substring(156);

    static final String INS_38_SRC = "CCCAGGTGGCCCGAAGAGGGAGAGGGGCTGCCGGGCGAGGATCCCCGCGGGCACCGCGAAGGAAGGCAGCTCCTGCAGGAACCAGGCGGCGCGGGCTGGC"
            + "AGGCGGGTAGCCGCCGGCTTCAGGCTCTCCGTGTGCTTCCCGTAGCCGGAGGGCTTCGCGACGTACAAGGCCAGTGCCCCAAGGGCGACCAAAGTGGCGC"
            + "TGCCTGCCAGCACTGGGCTCTGCTGGCACTGAACCTGCATCGCGCCGTGTTCCTCGCCGGTGGCCGCTGCCCTCCCAGAAGAGAGCGCGGCCCCCGCAAC"
            + "C";
    static final String INS_38_TGT = INS_38_SRC.substring(0, 152) + INS_38_SRC.substring(153);

    static final String INS_19_SRC = "CTGATCCCTGTGGCCCACAGGTCGGGGAGCGCTGGTACACAGAGAACACCTGCACCAGGCTCTGCACCTGCTCCGTCCACAACAACATCACCTGCTTCCA"
            + "GAGCACCTGCAAACCCAACCAGATATGCTGGGCCCTGGATGGGCTGCTCCGTTTCGGGCCTCAGGTAGGAGGACCACGGTGATGGGGGGACTCCACAGCC"
            + "CTGAGGCCCACTCTCCCAACTCCCCAACAGACTCCCTGCTGCCTGTAGCACTGGGTTTCAGGTTTTGACTGGTAACTCTGCTGCAACCAAGAGCTTCTGG"
            + "T";
    static final String INS_19_TGT = INS_19_SRC.substring(0, 150) + "ATTG" + INS_19_SRC.substring(153);

    /**
     * Synthetic flanks for find_reference's SNP branch: source FLANK_L A T C FLANK_R; the target has two bases where
     * the source has the T (a 1/2 gap after the A).
     */
    static final String FLANK_L = "CACGGGAGTCTCCCGTGCTTTCCCTAAAACCGAAAGCTTAATTGCGCTGCTTGTTGGGCA";
    static final String FLANK_R = "TGTAAAAGAGCTAGACAGGTGACTATGTTATTCCACCTCACCGAGAAGGCATGTGTTGGC";

    private static World swapDel;
    private static World minus;
    private static World rightMatch;
    private static World twoMatches;
    private static World ingapSnv;
    private static World novelRef;
    private static World del19;
    private static World ins38;
    private static World ins19;
    private static World gap3;

    /** One fixture: the source and target chromSeq builds, the chain and the input rows. */
    private record World(String src, String tgt, String srcSeq, String tgtSeq, String chain, String variants) {
    }

    @BeforeClass
    public static void setUp() throws IOException {
        Assert.assertEquals(303, SWAP_DEL_SRC.length());
        Assert.assertEquals("CTA", SWAP_DEL_SRC.substring(150, 153));
        Assert.assertEquals(301, INGAP_SNV_SRC.length());
        Assert.assertEquals("T", INGAP_SNV_SRC.substring(150, 151));
        Assert.assertEquals(302, NOVEL_REF_SRC.length());
        Assert.assertEquals("CT", NOVEL_REF_SRC.substring(150, 152));
        Assert.assertEquals("TC", DEL_19_SRC.substring(150, 152));
        Assert.assertEquals("G", INS_38_SRC.substring(150, 151));
        Assert.assertEquals("G", INS_19_SRC.substring(150, 151));
        swapDel = world("swap_del", SWAP_DEL_SRC, SWAP_DEL_TGT,
                "chr1\t0\t151\tchr1\t0\t151\t+\t1000\n"
                        + "chr1\t153\t303\tchr1\t151\t301\t+\t1000\n",
                "chr1\t151\tCTA\tC\tswap_del\n"
                        + "chr1\t151\tCT\tGG\tmnv\n"
                        + "chr1\t151\tGTA\tC\twrong_ref\n");
        // the same alignment against the reverse complement of the target. BCFtools: chr1 149 C CAT FLIP;SWAP=1 and
        // chr1 151 G AG,CC FLIP;SWAP=-1
        minus = world("minus", SWAP_DEL_SRC, revcomp(SWAP_DEL_TGT),
                "chr1\t0\t151\tchr1\t150\t301\t-\t1000\n"
                        + "chr1\t153\t303\tchr1\t0\t150\t-\t1000\n",
                "chr1\t151\tCTA\tC\tswap_del\n"
                        + "chr1\t151\tCT\tGG\tmnv\n");
        // the target's CG matches the extended ALT's end: BCFtools chr1 63 G T SWAP=1, one base on
        rightMatch = world("rmatch", FLANK_L + "ATC" + FLANK_R, FLANK_L + "ACGC" + FLANK_R,
                "chr1\t0\t61\tchr1\t0\t61\t+\t1000\n"
                        + "chr1\t62\t123\tchr1\t63\t124\t+\t1000\n",
                "chr1\t62\tT\tG\tsnv\n");
        // GG matches the extended ALT at both ends: no single match, so a new REF. BCFtools chr1 62 GG T,G SWAP=-1
        twoMatches = world("twomatch", FLANK_L + "ATC" + FLANK_R, FLANK_L + "AGGC" + FLANK_R,
                "chr1\t0\t61\tchr1\t0\t61\t+\t1000\n"
                        + "chr1\t62\t123\tchr1\t63\t124\t+\t1000\n",
                "chr1\t62\tT\tG\tsnv\n");
        ingapSnv = world("ingap_snv", INGAP_SNV_SRC, INGAP_SNV_TGT,
                "chr1\t0\t150\tchr1\t0\t150\t+\t1000\n"
                        + "chr1\t151\t301\tchr1\t152\t302\t+\t1000\n",
                "chr1\t151\tT\tG\tingap_snv\n");
        novelRef = world("novel_ref", NOVEL_REF_SRC, NOVEL_REF_TGT,
                "chr1\t0\t151\tchr1\t0\t151\t+\t1000\n"
                        + "chr1\t151\t302\tchr1\t152\t303\t+\t1000\n",
                "chr1\t151\tCT\tC\tnovel_ref\n");
        del19 = world("del_19", DEL_19_SRC, DEL_19_TGT,
                "chr1\t0\t151\tchr1\t0\t151\t+\t1000\n"
                        + "chr1\t152\t302\tchr1\t151\t301\t+\t1000\n",
                "chr1\t151\tTC\tT\tdel_19\n");
        ins38 = world("ins_38", INS_38_SRC, INS_38_TGT,
                "chr1\t0\t150\tchr1\t0\t150\t+\t1000\n"
                        + "chr1\t151\t301\tchr1\t150\t300\t+\t1000\n",
                "chr1\t151\tG\tGA\tins_38\n");
        ins19 = world("ins_19", INS_19_SRC, INS_19_TGT,
                "chr1\t0\t150\tchr1\t0\t150\t+\t1000\n"
                        + "chr1\t153\t301\tchr1\t154\t302\t+\t1000\n",
                "chr1\t151\tG\tGT\tins_19\n");
        // ENGKNOW-4224's chr18 world turned round: hg38 carries TAA (bases 102-104) the hg19 chain gap skips, a 3-base
        // source-only gap. BCFtools: chr1 103 A>G (mid) is rejected (UnmappedAnchors); chr1 102 T>G (edge) is
        // chr1 101 T TT,TG SWAP=-1.
        String hg38 = UTestGorLiftoverHaplotype.TARGET;
        gap3 = world("gap3", hg38, UTestGorLiftoverHaplotype.SOURCE,
                "chr1\t0\t101\tchr1\t0\t101\t+\t1000\n"
                        + "chr1\t104\t204\tchr1\t101\t201\t+\t1000\n",
                "chr1\t50\tTA\tT\tnogap\n"
                        + "chr1\t99\tTA\tT\trev_del\n"
                        + "chr1\t100\tA\tAT\trev_ins\n"
                        + "chr1\t102\tT\tG\tedge\n"
                        + "chr1\t103\tA\tG\tmid\n");
    }

    private static World world(String name, String src, String tgt, String blocks, String rows) throws IOException {
        Path dir = TMP.newFolder(name).toPath();
        return new World(src, tgt, chromSeq(dir.resolve("src"), src), chromSeq(dir.resolve("tgt"), tgt),
                write(dir.resolve("chain.gor"), CHAIN_HEADER + blocks),
                write(dir.resolve("variants.gor"), "Chrom\tPos\tRef\tAlt\tId\n" + rows));
    }

    private static String write(Path p, String content) throws IOException {
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toAbsolutePath().toString();
    }

    private static String revcomp(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append("ACGT".charAt(3 - "ACGT".indexOf(s.charAt(i))));
        }
        return sb.toString();
    }

    /** A chromSeq build directory holding chr1. */
    private static String chromSeq(Path dir, String seq) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("chr1.txt"), seq, StandardCharsets.US_ASCII);
        return dir.toAbsolutePath().toString();
    }

    private static String liftover(World w, String options) {
        return TestUtils.runGorPipe("gor " + w.variants() + " | liftover " + w.chain() + " -var " + options);
    }

    private static String haplotype(World w) {
        return liftover(w, "-haplotype -srcseq " + w.srcSeq() + " -tgtseq " + w.tgtSeq());
    }

    private static String pad(World w) {
        return liftover(w, "-haplotype -pad -srcseq " + w.srcSeq() + " -tgtseq " + w.tgtSeq());
    }

    /** The row of result whose Id (column 5) is id. */
    private static String row(String result, String id) {
        String[] lines = result.split("\n");
        for (String r : Arrays.copyOfRange(lines, 1, lines.length)) {
            if (r.split("\t")[4].equals(id)) {
                return r;
            }
        }
        throw new AssertionError("no row " + id + " in\n" + result);
    }

    @Test
    public void trueSwapDeletionAcrossASourceOnlyGap() {
        // the target carries the source ALT: lifted with the source alleles at BCFtools' position, for the swap check
        Assert.assertEquals("chr1\t151\tCTA\tC\tswap_del\tchr1\t151\t+\tmapped\tswap", row(pad(swapDel), "swap_del"));
    }

    @Test
    public void snvInsideAnUnequalGap() {
        Assert.assertEquals("chr1\t151\tT\tG\tingap_snv\tchr1\t151\t+\tmapped\tswap", row(pad(ingapSnv), "ingap_snv"));
    }

    @Test
    public void novelReferenceIsRewritten() {
        // BCFtools' new REF and the source ALT, as -haplotype writes a rewritten row
        Assert.assertEquals("chr1\t151\tCTT\tC\tnovel_ref\tchr1\t151\t+\tmapped\trewritten",
                row(pad(novelRef), "novel_ref"));
    }

    @Test
    public void mnvAcrossTheGapIsRewritten() {
        // BCFtools: chr1 151 C CT,GG SWAP=-1
        Assert.assertEquals("chr1\t151\tC\tGG\tmnv\tchr1\t151\t+\tmapped\trewritten", row(pad(swapDel), "mnv"));
    }

    @Test
    public void minusStrand() {
        String result = pad(minus);
        Assert.assertEquals("chr1\t149\tCAT\tC\tswap_del\tchr1\t151\t-\tmapped\tswap", row(result, "swap_del"));
        Assert.assertEquals("chr1\t151\tG\tCC\tmnv\tchr1\t151\t-\tmapped\trewritten", row(result, "mnv"));
    }

    @Test
    public void snvMatchingTheEndOfALongerTargetSpan() {
        Assert.assertEquals("chr1\t63\tT\tG\tsnv\tchr1\t62\t+\tmapped\tswap", row(pad(rightMatch), "snv"));
    }

    @Test
    public void snvMatchingBothEndsGetsANewReference() {
        Assert.assertEquals("chr1\t62\tGG\tG\tsnv\tchr1\t62\t+\tmapped\trewritten", row(pad(twoMatches), "snv"));
    }

    @Test
    public void aRefThatIsNotTheSourceStaysUnmapped() {
        Assert.assertTrue(row(pad(swapDel), "wrong_ref").endsWith("\tunmapped\tref_mismatch"));
    }

    @Test
    public void withAlignedALiftedRowHasNoExtent() {
        String result = liftover(swapDel, "-aligned -haplotype -pad -srcseq " + swapDel.srcSeq() + " -tgtseq "
                + swapDel.tgtSeq());
        Assert.assertEquals("chr1\t151\tCTA\tC\tswap_del\tchr1\t151\t+\tmapped\t0\t0\tswap", row(result, "swap_del"));
    }

    @Test
    public void failOnUnmappedSparesTheLiftedRows() {
        String seqs = " -srcseq " + ingapSnv.srcSeq() + " -tgtseq " + ingapSnv.tgtSeq();
        // ingap_snv is unmapped without -pad and lifted with it
        assertFails("gor " + ingapSnv.variants() + " | liftover " + ingapSnv.chain() + " -var -failon unmapped"
                + " -haplotype" + seqs, "unmapped");
        Assert.assertTrue(row(liftover(ingapSnv, "-failon unmapped -haplotype -pad" + seqs), "ingap_snv")
                .endsWith("\tmapped\tswap"));
        assertFails("gor " + gap3.variants() + " | liftover " + gap3.chain() + " -var -failon unmapped -haplotype -pad"
                + " -srcseq " + gap3.srcSeq() + " -tgtseq " + gap3.tgtSeq(), "unmapped");
    }

    @Test
    public void trueSwapDeletionHg19ToHg38() {
        // ClinVar 201786, hg19 chr11:6630028 TC>T: the C is a source-only gap. BCFtools: chr1 151 T TC SWAP=1
        Assert.assertEquals("chr1\t151\tTC\tT\tdel_19\tchr1\t151\t+\tmapped\tswap", row(pad(del19), "del_19"));
    }

    @Test
    public void insertionOnASourceOnlyBase() {
        // ClinVar 97389, hg38 chr2:31580811 G>GA, whose G hg19 does not have: BCFtools left-aligns it and keeps its
        // alleles (chr1 149 G GA, no SWAP)
        Assert.assertEquals("chr1\t149\tG\tGA\tins_38\tchr1\t151\t+\tmapped\tnone", row(pad(ins38), "ins_38"));
    }

    @Test
    public void insertionInAnUnequalGapIsRewritten() {
        // ClinVar 403617, hg19 chr7:100371474 G>GT, inside a 3/4 gap. BCFtools: chr1 153 TG T,TT SWAP=-1
        Assert.assertEquals("chr1\t153\tTG\tTT\tins_19\tchr1\t151\t+\tmapped\trewritten", row(pad(ins19), "ins_19"));
    }

    @Test
    public void anAnchorInTheGapIsPaddedToTheNextBlock() {
        String result = pad(gap3);
        Assert.assertEquals("chr1\t101\tT\tTG\tedge\tchr1\t102\t+\tmapped\trewritten", row(result, "edge"));
    }

    @Test
    public void rowsBcftoolsRejectsStayUnmapped() {
        // both extended anchors (102 and 104) fall in the gap
        Assert.assertTrue(row(pad(gap3), "mid").endsWith("\tunmapped\tunresolved"));
    }

    @Test
    public void liftedAllelesGiveTheSourceHaplotype() {
        // the ClinVar worlds whose only build difference is the one the lift covers (in gap3 BCFtools' rewrite, like
        // ENGKNOW-4224's, covers only part of the three-base gap, and INS_19's 3/4 gap also holds a base difference
        // the rewritten span does not reach)
        for (World w : new World[]{swapDel, ingapSnv, novelRef, del19}) {
            String result = pad(w);
            String[] lines = result.split("\n");
            for (String line : Arrays.copyOfRange(lines, 1, lines.length)) {
                String[] c = line.split("\t");
                // the synthetic MNV is BCFtools' representation too, but across the TA gap it drops a source base
                if (!"mapped".equals(c[8]) || "none".equals(c[9]) || "mnv".equals(c[4])) {
                    continue;
                }
                int pos = Integer.parseInt(c[1]);
                int sPos = Integer.parseInt(c[6]);
                String[] in = row(TestUtils.runGorPipe("gor " + w.variants()), c[4]).split("\t");
                String sourceAlt = w.src().substring(0, sPos - 1) + in[3] + w.src().substring(sPos - 1 + in[2].length());
                if ("swap".equals(c[9])) {
                    // the target reference carries ALT; with REF in its place it is the source reference. Not for
                    // the SNV in the 1/2 gap: hg38 has GC where hg19 has T, and BCFtools still calls it SWAP=1
                    Assert.assertEquals(c[4], c[3], w.tgt().substring(pos - 1, pos - 1 + c[3].length()));
                    if (w != ingapSnv) {
                        Assert.assertEquals(c[4], w.src(),
                                w.tgt().substring(0, pos - 1) + c[2] + w.tgt().substring(pos - 1 + c[3].length()));
                    }
                } else {
                    Assert.assertEquals(c[4], c[2], w.tgt().substring(pos - 1, pos - 1 + c[2].length()));
                    Assert.assertEquals(c[4], sourceAlt,
                            w.tgt().substring(0, pos - 1) + c[3] + w.tgt().substring(pos - 1 + c[2].length()));
                }
            }
        }
    }

    @Test
    public void withoutPadTheseRowsStayUnmapped() {
        String[] ids = {"swap_del", "ingap_snv", "novel_ref", "del_19", "ins_38", "ins_19"};
        World[] worlds = {swapDel, ingapSnv, novelRef, del19, ins38, ins19};
        for (int i = 0; i < ids.length; i++) {
            String plain = liftover(worlds[i], "");
            String hap = haplotype(worlds[i]);
            Assert.assertTrue(plain, row(plain, ids[i]).contains("\tunmapped"));
            Assert.assertTrue(hap, row(hap, ids[i]).endsWith("\tunmapped\tnone"));
        }
    }

    @Test
    public void rowsThePlainLiftMapsKeepTheHaplotypeResult() {
        // -pad only touches rows the plain lift leaves unmapped: an indel away from the gap, and ENGKNOW-4224's two
        // rows next to it (rev_ins rewritten, rev_del none)
        String without = haplotype(gap3);
        String withPad = pad(gap3);
        for (String id : new String[]{"nogap", "rev_ins", "rev_del"}) {
            Assert.assertTrue(without, row(without, id).contains("\tmapped\t"));
            Assert.assertEquals(row(without, id), row(withPad, id));
        }
        // the SNVs in the gap are unmapped under -haplotype alone
        Assert.assertTrue(without, row(without, "edge").contains("\tunmapped"));
        Assert.assertTrue(without, row(without, "mid").contains("\tunmapped"));
    }

    @Test
    public void optionErrors() {
        assertFails("gor " + swapDel.variants() + " | liftover " + swapDel.chain() + " -var -pad",
                "-pad is only valid with -haplotype");
        assertFails("gor " + swapDel.variants() + " | liftover " + swapDel.chain() + " -var -nogapfill -haplotype -pad"
                + " -srcseq " + swapDel.srcSeq() + " -tgtseq " + swapDel.tgtSeq(), "cannot be combined with -nogapfill");
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
