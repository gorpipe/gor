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

import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Consumer query shapes and round trips of LIFTOVER and LIFTOVER_ANNOTATE (ENGKNOW-4117, N18).
 * <p>
 * The consumer shapes are copied from gdb-sequence-miner: {@code GridViewerEvent.createLiftoverGor},
 * {@code createLiftoverForPosGor}, {@code createLiftover2ForPosGor} (position lookups, with a {@code -snp} fallback)
 * and {@code BamParser.createLiftoverGor} ({@code -bam -all}). The round trips lift random inputs through a chain
 * whose targets do not overlap and back through its inverse (t and q swapped, strand kept, rows re-sorted). The
 * expectations come from a small independent model of the chain ({@link Block}), not from the engine.
 */
public class UTestGorLiftoverConsumers {

    private static final String BAM = "../tests/data/bvl_min/bam/BVL_INDEX_SLC52A2.bam";
    private static final String CHAIN_HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore";
    private static final int ROUND_TRIP_ROWS = 5_000;
    private static final long SEED = 4117;

    /** The Task 1 chain without overlapping blocks (UTestGorLiftoverAnnotate.CHAIN_UNIQUE is private). */
    private static final List<Block> CHAIN_UNIQUE = List.of(
            new Block("chr1", 100, 200, "chr1", 1100, "+", 1000),
            new Block("chr1", 200, 300, "chr1", 1200, "+", 1000),
            new Block("chr1", 400, 500, "chr1", 5000, "-", 900),
            new Block("chr1", 600, 700, "chr2", 2100, "+", 800),
            new Block("chr1", 800, 900, "chr1", 8800, "+", 500),
            new Block("chrM", 300, 3100, "chrM", 298, "+", 100));

    private static Path tmpdir;

    @BeforeClass
    public static void setUp() throws IOException {
        GorPipeCommands.register();
        GorInputSources.register();
        UTestGorLiftoverCharacterization.setUp(); // CHAIN, SNP, VAR, SEG
        tmpdir = Files.createTempDirectory("liftover-consumers");
        tmpdir.toFile().deleteOnExit();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        UTestGorLiftoverCharacterization.tearDown();
        FileUtils.deleteDirectory(tmpdir.toFile());
    }

    // ------------------------------------------------------------------------------------------------
    // Model of a chain block and helpers
    // ------------------------------------------------------------------------------------------------

    /** A chain block: 0-based half-open on the source contig, q on the forward strand of the target. */
    record Block(String tChrom, int tStart, int tEnd, String qChrom, int qStart, String strand, double score) {
        int qEnd() {
            return qStart + tEnd - tStart;
        }

        boolean minus() {
            return "-".equals(strand);
        }

        /** An interval is contained entirely, a junction (s0 == e0) strictly inside (N12). */
        boolean contains(int s0, int e0) {
            return s0 == e0 ? tStart < s0 && s0 < tEnd : tStart <= s0 && e0 <= tEnd;
        }

        /** The 0-based half-open target of [s0, e0), which the block must contain. */
        int[] lift(int s0, int e0) {
            return minus() ? new int[]{qStart + tEnd - e0, qStart + tEnd - s0}
                    : new int[]{qStart + s0 - tStart, qStart + e0 - tStart};
        }

        Block inverse() {
            return new Block(qChrom, qStart, qEnd(), tChrom, tStart, strand, score);
        }
    }

    private static List<Block> inverse(List<Block> chain) {
        return chain.stream().map(Block::inverse).collect(Collectors.toList());
    }

    private static List<Block> containing(List<Block> chain, String chrom, int s0, int e0) {
        List<Block> r = new ArrayList<>();
        for (Block b : chain) {
            if (b.tChrom().equals(chrom) && b.contains(s0, e0)) {
                r.add(b);
            }
        }
        return r;
    }

    /** The only block that contains the interval, on a chain whose targets do not overlap; null for none. */
    private static Block find(List<Block> chain, String chrom, int s0, int e0) {
        List<Block> r = containing(chain, chrom, s0, e0);
        Assert.assertTrue(r.size() <= 1);
        return r.isEmpty() ? null : r.get(0);
    }

    private static String chainFile(String name, List<Block> chain) throws IOException {
        List<Block> sorted = new ArrayList<>(chain);
        sorted.sort(Comparator.comparing(Block::tChrom).thenComparingInt(Block::tStart));
        StringBuilder sb = new StringBuilder(CHAIN_HEADER).append('\n');
        for (Block b : sorted) {
            sb.append(b.tChrom()).append('\t').append(b.tStart()).append('\t').append(b.tEnd()).append('\t')
                    .append(b.qChrom()).append('\t').append(b.qStart()).append('\t').append(b.qEnd()).append('\t')
                    .append(b.strand()).append('\t').append((long) b.score()).append('\n');
        }
        return write(name, sb.toString());
    }

    private static String write(String name, String content) throws IOException {
        File f = new File(tmpdir.toFile(), name);
        FileUtils.write(f, content, StandardCharsets.UTF_8);
        return f.getAbsolutePath();
    }

    /** Writes a GOR file; rows are sorted by (chrom, pos) on columns 0 and 1. */
    private static String gorFile(String name, String header, List<String[]> rows) throws IOException {
        List<String[]> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.<String[], String>comparing(r -> r[0]).thenComparingInt(r -> Integer.parseInt(r[1])));
        StringBuilder sb = new StringBuilder(header).append('\n');
        for (String[] r : sorted) {
            sb.append(String.join("\t", r)).append('\n');
        }
        return write(name, sb.toString());
    }

    /** Query output: header and tab separated rows. */
    private static final class Table {
        final String[] header;
        final List<String[]> rows = new ArrayList<>();

        Table(String output) {
            String[] ls = output.split("\n");
            header = ls[0].split("\t", -1);
            for (int i = 1; i < ls.length; i++) {
                rows.add(ls[i].split("\t", -1));
            }
        }

        int col(String name) {
            for (int i = 0; i < header.length; i++) {
                if (header[i].equalsIgnoreCase(name)) {
                    return i;
                }
            }
            throw new AssertionError("no column " + name + " in " + String.join(",", header));
        }

        /** Rows by the value of the column, which must be unique. */
        Map<String, String[]> by(String name) {
            int c = col(name);
            Map<String, String[]> m = new LinkedHashMap<>();
            for (String[] r : rows) {
                Assert.assertNull("duplicate " + r[c], m.put(r[c], r));
            }
            return m;
        }
    }

    private static Table run(String query) {
        return new Table(TestUtils.runGorPipe(query));
    }

    private static String dataRows(String output) {
        String[] ls = output.split("\n");
        return String.join("\n", java.util.Arrays.copyOfRange(ls, 1, ls.length));
    }

    private static String revComp(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            switch (c) {
                case 'A':
                    sb.append('T');
                    break;
                case 'T':
                    sb.append('A');
                    break;
                case 'C':
                    sb.append('G');
                    break;
                case 'G':
                    sb.append('C');
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final Pattern CIGAR_OP = Pattern.compile("\\d+[MIDNSHP=X]");

    private static String revCigar(String cigar) {
        List<String> ops = new ArrayList<>();
        Matcher m = CIGAR_OP.matcher(cigar);
        while (m.find()) {
            ops.add(m.group());
        }
        Collections.reverse(ops);
        return String.join("", ops);
    }

    private static String revString(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    /** Each comma separated allele reverse-complemented, symbolic alleles kept (N16). Test alleles only use ACGT. */
    private static String revCompAlleles(String alleles) {
        String[] as = alleles.split(",", -1);
        for (int i = 0; i < as.length; i++) {
            as[i] = as[i].startsWith("<") ? as[i] : revComp(as[i]);
        }
        return String.join(",", as);
    }

    // ------------------------------------------------------------------------------------------------
    // Step 1: consumer query shapes
    // ------------------------------------------------------------------------------------------------

    /** GridViewerEvent.createLiftoverForPosGor, for chrom, start, stop on UTestGorLiftoverCharacterization.CHAIN. */
    private static String posLookupSeg(String chr, int start, int stop) {
        return "gorrow " + chr + "," + start + "," + stop + " | liftover " + UTestGorLiftoverCharacterization.CHAIN
                + "  -seg | where hgold_liftoverstatus = 'mapped' | select 1-3 | top 1";
    }

    /** GridViewerEvent.createLiftover2ForPosGor, the fallback of the position lookup. */
    private static String posLookupSnp(String chr, int start, int stop) {
        return "gorrow " + chr + "," + start + "," + stop + " | calc delta #3-#2 | liftover "
                + UTestGorLiftoverCharacterization.CHAIN
                + "  -snp | where hgold_liftoverstatus = 'mapped' | replace #3 #2+delta | select 1-3 | top 1";
    }

    @Test
    public void seqMinerPosLookupSeg() {
        // a junction: no exception, start == stop
        Assert.assertEquals("chr1\t1150\t1150", dataRows(TestUtils.runGorPipe(posLookupSeg("chr1", 150, 150))));
        Assert.assertEquals("chr1\t1150\t1180", dataRows(TestUtils.runGorPipe(posLookupSeg("chr1", 150, 180))));
        // a '-' strand block
        Assert.assertEquals("chr1\t5010\t5050", dataRows(TestUtils.runGorPipe(posLookupSeg("chr1", 450, 490))));
        // ambiguous (a score tie): no rows, so the caller falls back to -snp
        Assert.assertEquals("", dataRows(TestUtils.runGorPipe(posLookupSeg("chr1", 860, 880))));
        // the header the caller drops
        Assert.assertEquals(1, TestUtils.runGorPipe(posLookupSeg("chr1", 860, 880)).split("\n").length);
    }

    @Test
    public void seqMinerPosLookupSnp() {
        Assert.assertEquals("chr1\t1150\t1160", dataRows(TestUtils.runGorPipe(posLookupSnp("chr1", 150, 160))));
    }

    /** -seg finds nothing for a span over two blocks, the caller's -snp lookup of the start then answers. */
    @Test
    public void seqMinerPosLookupFallbackRuns() {
        Assert.assertEquals("", dataRows(TestUtils.runGorPipe(posLookupSeg("chr1", 190, 210))));
        Assert.assertEquals("chr1\t1190\t1210", dataRows(TestUtils.runGorPipe(posLookupSnp("chr1", 190, 210))));
        // unsupported contig: neither answers
        Assert.assertEquals("", dataRows(TestUtils.runGorPipe(posLookupSeg("chr9", 100, 200))));
        Assert.assertEquals("", dataRows(TestUtils.runGorPipe(posLookupSnp("chr9", 100, 200))));
    }

    /** GridViewerEvent.createLiftoverGor: {@code gor <file> | liftover <chain> -all -build <b> <type>}. */
    @Test
    public void seqMinerReportAll() {
        String chain = UTestGorLiftoverCharacterization.CHAIN;
        Assert.assertEquals(allBuild(UTestGorLiftoverCharacterization.VAR_ALL_P4),
                TestUtils.runGorPipe("gor " + UTestGorLiftoverCharacterization.VAR + " | liftover " + chain
                        + " -all -build hg19 -var"));
        Assert.assertEquals(allBuild(UTestGorLiftoverCharacterization.SNP_ALL_P4),
                TestUtils.runGorPipe("gor " + UTestGorLiftoverCharacterization.SNP + " | liftover " + chain
                        + " -all -build hg19 -snp"));
        Assert.assertEquals(allBuild(UTestGorLiftoverCharacterization.SEG_ALL_P4),
                TestUtils.runGorPipe("gor " + UTestGorLiftoverCharacterization.SEG + " | liftover " + chain
                        + " -all -build hg19 -seg"));
    }

    private static String allBuild(String hgoldLiteral) {
        return hgoldLiteral.replace("hgold_", "hg19_");
    }

    // ---- real BAM, BamParser.createLiftoverGor ------------------------------------------------------

    private static final class Bam {
        final Table table;
        final int chrom;
        final int pos;
        final int end;
        final int qname;
        final int cigar;
        final int mrnm;
        final int mpos;
        final int seq;
        final int qual;

        Bam() {
            table = run("gor " + BAM);
            Assert.assertTrue("the real BAM has reads", table.rows.size() > 100);
            chrom = 0;
            pos = table.col("Pos");
            end = table.col("End");
            qname = table.col("QName");
            cigar = table.col("Cigar");
            mrnm = table.col("MRNM");
            mpos = table.col("MPOS");
            seq = table.col("SEQ");
            qual = table.col("QUAL");
            Assert.assertEquals("Chromo", table.header[0]);
        }
    }

    /**
     * Blocks of 1,000 bases with alternating strand and gaps of 0 and 10 bases over the read range of every contig,
     * all targeting target at a fixed offset. With a second target the chain gets a second set of blocks shifted by
     * 500 bases, which overlap the first.
     */
    private static List<Block> bamChain(Bam bam, String target, String secondTarget) {
        Map<String, int[]> range = new LinkedHashMap<>();
        for (String[] r : bam.table.rows) {
            int[] mm = range.computeIfAbsent(r[bam.chrom], k -> new int[]{Integer.MAX_VALUE, 0});
            mm[0] = Math.min(mm[0], Integer.parseInt(r[bam.pos]) - 1);
            mm[1] = Math.max(mm[1], Integer.parseInt(r[bam.end]));
        }
        List<Block> chain = new ArrayList<>();
        for (Map.Entry<String, int[]> e : range.entrySet()) {
            int start = e.getValue()[0];
            int i = 0;
            while (start < e.getValue()[1]) {
                int end = start + 1000;
                chain.add(new Block(e.getKey(), start, end, target, start + 10_000_000, i % 2 == 0 ? "+" : "-", 1000));
                if (secondTarget != null) {
                    chain.add(new Block(e.getKey(), start + 500, end + 500, secondTarget, start + 20_000_000,
                            i % 2 == 0 ? "-" : "+", 700));
                }
                start = end + (i % 2 == 0 ? 0 : 10);
                i++;
            }
        }
        chain.sort(Comparator.comparing(Block::tChrom).thenComparingInt(Block::tStart));
        return chain;
    }

    /** The expected -bam -all rows: one per containing block, or one unmapped row. */
    private static List<String> expectedBamAll(Bam bam, List<Block> chain, boolean mateIsChecked) {
        return expectedBamAll(bam, chain, mateIsChecked, null);
    }

    /** As above; with a list, the (input MPOS, expected MPOS) of every mapped row is added to it. */
    private static List<String> expectedBamAll(Bam bam, List<Block> chain, boolean mateIsChecked,
                                               List<String[]> matePairs) {
        List<String> expected = new ArrayList<>();
        for (String[] r : bam.table.rows) {
            int s0 = Integer.parseInt(r[bam.pos]) - 1;
            int e0 = Integer.parseInt(r[bam.end]);
            List<Block> blocks = containing(chain, r[bam.chrom], s0, e0);
            if (blocks.isEmpty()) {
                String[] row = r.clone();
                row[bam.pos] = "0";
                row[bam.end] = "1";
                String m = r[bam.mpos];
                row[bam.mpos] = mateValue(m) > 0 ? "0" : m;
                expected.add(String.join("\t", row) + "\t" + r[bam.chrom] + "\t" + r[bam.pos] + "\t" + r[bam.end]
                        + "\t\tunmapped\t0.0");
                continue;
            }
            for (Block b : blocks) {
                String[] row = r.clone();
                int[] t = b.lift(s0, e0);
                row[bam.chrom] = b.qChrom();
                row[bam.pos] = Integer.toString(t[0] + 1);
                row[bam.end] = Integer.toString(t[1]);
                if (b.minus()) {
                    row[bam.seq] = revComp(r[bam.seq]);
                    row[bam.qual] = revString(r[bam.qual]);
                    row[bam.cigar] = revCigar(r[bam.cigar]);
                }
                if (mateIsChecked) {
                    row[bam.mpos] = mate(chain, b, r[bam.chrom], r[bam.mrnm], r[bam.mpos], bam.mrnm >= 0);
                    if (matePairs != null) {
                        matePairs.add(new String[]{r[bam.mpos], row[bam.mpos]});
                    }
                }
                expected.add(String.join("\t", row) + "\t" + r[bam.chrom] + "\t" + r[bam.pos] + "\t" + r[bam.end]
                        + "\t" + b.strand() + "\tmapped\t" + Double.toString(b.score()));
            }
        }
        return expected;
    }

    /** An MPOS that is an integer greater than 0 (a mate position), else -1 (R18). */
    private static int mateValue(String mpos) {
        try {
            int m = Integer.parseInt(mpos);
            return m > 0 ? m : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** MRNM names a contig as the BAM does, 8 for chr8: a leading chr and the case are ignored, M is MT. */
    private static String contigName(String contig) {
        String c = contig.toUpperCase();
        c = c.startsWith("CHR") ? c.substring(3) : c;
        return c.equals("M") ? "MT" : c;
    }

    /** N15 and R18 for a chain whose targets do not overlap: MPOS is lifted on its own, through its own block. */
    private static String mate(List<Block> chain, Block read, String readChrom, String mrnm, String mpos,
                               boolean hasMrnm) {
        int m = mateValue(mpos);
        if (m < 0) {
            return mpos;
        }
        if (hasMrnm && !mrnm.equals("=") && !contigName(mrnm).equals(contigName(readChrom))) {
            return "0";
        }
        Block b = find(chain, readChrom, m - 1, m);
        if (b == null || !b.qChrom().equals(read.qChrom())) {
            return "0";
        }
        return Integer.toString(b.lift(m - 1, m)[0] + 1);
    }

    /** The first two rows, without the long SEQ, QUAL and tag columns. */
    private static List<String> abbreviate(List<String> rows) {
        return rows.stream().limit(2).map(r -> {
            String[] c = r.split("\t", -1);
            return String.join("\t", java.util.Arrays.copyOfRange(c, 0, 11)) + "\t...\t"
                    + String.join("\t", java.util.Arrays.copyOfRange(c, 14, c.length));
        }).collect(Collectors.toList());
    }

    private void bamAll(String chainName, List<Block> chain, Bam bam, boolean mateIsChecked) throws IOException {
        String chainFile = chainFile(chainName, chain);
        Table out = run("gor " + BAM + " | liftover " + chainFile + " -bam -all -build hg19");
        Assert.assertArrayEquals(new String[]{"Chromo", "Pos", "End", "QName", "Flag", "MapQ", "Cigar", "MD", "MRNM",
                "MPOS", "ISIZE", "SEQ", "QUAL", "TAG_VALUES", "hg19_Chromo", "hg19_Pos", "hg19_End", "hg19_qStrand",
                "hg19_liftoverStatus", "hg19_score"}, out.header);
        List<String> actual = out.rows.stream().map(r -> String.join("\t", r)).sorted().collect(Collectors.toList());
        List<String> expected = expectedBamAll(bam, chain, mateIsChecked);
        if (!mateIsChecked) {
            // MPOS is only pinned for the chain whose targets do not overlap
            int mpos = out.col("MPOS");
            actual = out.rows.stream().map(r -> {
                String[] c = r.clone();
                c[mpos] = "";
                return String.join("\t", c);
            }).sorted().collect(Collectors.toList());
            expected = expected.stream().map(l -> {
                String[] c = l.split("\t", -1);
                c[mpos] = "";
                return String.join("\t", c);
            }).collect(Collectors.toList());
        }
        Collections.sort(expected);
        List<String> onlyExpected = new ArrayList<>(expected);
        actual.forEach(onlyExpected::remove);
        List<String> onlyActual = new ArrayList<>(actual);
        expected.forEach(onlyActual::remove);
        String diff = "expected only (" + onlyExpected.size() + "): " + abbreviate(onlyExpected)
                + ", actual only (" + onlyActual.size() + "): " + abbreviate(onlyActual);
        Assert.assertTrue(diff, onlyExpected.isEmpty() && onlyActual.isEmpty());
        Assert.assertEquals(expected, actual);

        // rows per (QName, hg19_Pos) are the containing blocks, at least one
        int qn = out.col("QName");
        int hp = out.col("hg19_Pos");
        int st = out.col("hg19_liftoverStatus");
        Map<String, Integer> perKey = new HashMap<>();
        for (String[] r : out.rows) {
            perKey.merge(r[qn] + ":" + r[hp], 1, Integer::sum);
            Assert.assertTrue(r[st].equals("mapped") || r[st].equals("unmapped"));
        }
        Map<String, Integer> expectedPerKey = new HashMap<>();
        for (String[] r : bam.table.rows) {
            int n = Math.max(1, containing(chain, r[bam.chrom], Integer.parseInt(r[bam.pos]) - 1,
                    Integer.parseInt(r[bam.end])).size());
            expectedPerKey.merge(r[bam.qname] + ":" + r[bam.pos], n, Integer::sum);
        }
        Assert.assertEquals(expectedPerKey, perKey);
    }

    @Test
    public void seqMinerBamAll() throws IOException {
        Bam bam = new Bam();
        List<Block> chain = bamChain(bam, "chr1", null);
        bamAll("bam_chain.gor", chain, bam, true);

        // the mate lift is not trivial on the real reads: some mates are lifted to a position other than the input
        List<String[]> matePairs = new ArrayList<>();
        expectedBamAll(bam, chain, true, matePairs);
        long lifted = matePairs.stream().filter(p -> mateValue(p[1]) > 0 && !p[1].equals(p[0])).count();
        Assert.assertTrue("lifted mates: " + lifted, lifted > 0);

        // the chain is a real test: both statuses, and both strands, occur
        String out = TestUtils.runGorPipe("gor " + BAM + " | liftover " + chainFile("bam_chain.gor", chain)
                + " -bam -all -build hg19");
        Assert.assertTrue(out.contains("\tmapped\t"));
        Assert.assertTrue(out.contains("\tunmapped\t"));
        Assert.assertTrue(out.contains("\t+\tmapped\t"));
        Assert.assertTrue(out.contains("\t-\tmapped\t"));
    }

    @Test
    public void seqMinerBamAllOverlappingBlocks() throws IOException {
        Bam bam = new Bam();
        List<Block> chain = bamChain(bam, "chr1", "chr2");
        bamAll("bam_chain_ovl.gor", chain, bam, false);
        Table out = run("gor " + BAM + " | liftover " + chainFile("bam_chain_ovl.gor", chain)
                + " -bam -all -build hg19");
        // a read inside the overlap of two blocks gives a row for each, on both targets
        Assert.assertTrue(out.rows.size() > bam.table.rows.size());
        Assert.assertTrue(out.rows.stream().anyMatch(r -> r[0].equals("chr1")));
        Assert.assertTrue(out.rows.stream().anyMatch(r -> r[0].equals("chr2")));
    }

    // ------------------------------------------------------------------------------------------------
    // Hand computed anchors, independent of the model above
    // ------------------------------------------------------------------------------------------------

    /**
     * One '-' strand block, chr1:400-500 (0-based half-open, the source bases 401..500 in 1-based) to chr1:5000-5100,
     * and its inverse (t and q swapped, the strand kept). Expected values were walked base by base, not derived from
     * the model's formula. On the '-' strand the first source base lands on the LAST target base:
     * <pre>
     *   1-based source base p (401..500)  ->  1-based target base 5501 - p
     *   401 -> 5100   410 -> 5091   411 -> 5090   415 -> 5086   420 -> 5081   450 -> 5051   500 -> 5001
     * </pre>
     * so a run of source bases a..b lands on 5501-b .. 5501-a, and a junction before 0-based base x on 5500-x.
     */
    private static final String MINUS_CHAIN = CHAIN_HEADER + "\nchr1\t400\t500\tchr1\t5000\t5100\t-\t900\n";
    private static final String MINUS_CHAIN_INVERSE = CHAIN_HEADER + "\nchr1\t5000\t5100\tchr1\t400\t500\t-\t900\n";

    private static String lift(String chain, String header, String rows, String opts) throws IOException {
        String c = write("anchor_chain_" + Math.abs(chain.hashCode()) + ".gor", chain);
        String in = write("anchor_in_" + Math.abs((header + rows).hashCode()) + ".gor", header + "\n" + rows);
        return TestUtils.runGorPipe("gor " + in + " | liftover " + c + " " + opts);
    }

    @Test
    public void minusStrandSnpAnchors() throws IOException {
        // 401 -> 5100, 410 -> 5091, 500 -> 5001; the output is sorted by the lifted position
        Assert.assertEquals(String.join("\n",
                "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus",
                "chr1\t5001\tA\tG\ts3\tchr1\t500\t-\tmapped",
                "chr1\t5091\tA\tG\ts2\tchr1\t410\t-\tmapped",
                "chr1\t5100\tA\tG\ts1\tchr1\t401\t-\tmapped", ""),
                lift(MINUS_CHAIN, "Chrom\tPos\tRef\tAlt\tId",
                        "chr1\t401\tA\tG\ts1\nchr1\t410\tA\tG\ts2\nchr1\t500\tA\tG\ts3", "-snp"));
    }

    @Test
    public void minusStrandVarAnchor() throws IOException {
        // REF AC at 410: source bases 410, 411 -> target bases 5091, 5090, so the variant starts at 5090 and both
        // alleles are reverse-complemented: AC -> GT, G -> C
        Assert.assertEquals(String.join("\n",
                "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus",
                "chr1\t5090\tGT\tC\tv1\tchr1\t410\t-\tmapped", ""),
                lift(MINUS_CHAIN, "Chrom\tPos\tRef\tAlt\tId", "chr1\t410\tAC\tG\tv1", "-var"));
    }

    @Test
    public void minusStrandSegAnchors() throws IOException {
        // [410,450) is the source bases 411..450 (1-based) -> 5501-450 .. 5501-411 = 5051..5090, GOR [5050,5090).
        // The junction before 0-based base 450 (between the source bases 450 and 451, 1-based) is between
        // the target bases 5051 and 5050: the cut at 5050.
        Assert.assertEquals(String.join("\n",
                "Chrom\tStart\tEnd\tId\thgold_Chrom\thgold_Start\thgold_End\thgold_qStrand\thgold_liftoverStatus",
                "chr1\t5050\t5050\tj1\tchr1\t450\t450\t-\tmapped",
                "chr1\t5050\t5090\tg1\tchr1\t410\t450\t-\tmapped", ""),
                lift(MINUS_CHAIN, "Chrom\tStart\tEnd\tId", "chr1\t410\t450\tg1\nchr1\t450\t450\tj1", "-seg"));
    }

    @Test
    public void minusStrandBamAnchorsAndTheirInverse() throws IOException {
        String header = "Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMPOS";
        // r2: 1-based Pos 411, End 420 = source bases 411..420 -> 5501-420 .. 5501-411 = 5081..5090.
        //   SEQ AACGTTGCAT: reversed TACGTTGCAA, complemented ATGCAACGTT. QUAL ABCDEFGHIJ -> JIHGFEDCBA.
        //   CIGAR 3S5M2I (the read's own order) -> 2I5M3S. MPOS 415 -> 5501-415 = 5086 (on chr1, the read's target).
        // r1: a 1-base read on the block's last base (500) -> 5001, SEQ C -> G, MPOS 0 is no mate: unchanged.
        String out = lift(MINUS_CHAIN, header,
                "chr1\t411\t420\tr2\tAACGTTGCAT\tABCDEFGHIJ\t3S5M2I\t415\nchr1\t500\t500\tr1\tC\tI\t1M\t0",
                "-bam");
        String expected = String.join("\n",
                "Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMPOS\thgold_Chrom\thgold_Pos\thgold_End\thgold_qStrand"
                        + "\thgold_liftoverStatus",
                "chr1\t5001\t5001\tr1\tG\tI\t1M\t0\tchr1\t500\t500\t-\tmapped",
                "chr1\t5081\t5090\tr2\tATGCAACGTT\tJIHGFEDCBA\t2I5M3S\t5086\tchr1\t411\t420\t-\tmapped", "");
        Assert.assertEquals(expected, out);

        // and back through the inverse chain, written out by hand: the original reads, with MPOS 415 again
        String back = lift(MINUS_CHAIN_INVERSE, header,
                "chr1\t5001\t5001\tr1\tG\tI\t1M\t0\nchr1\t5081\t5090\tr2\tATGCAACGTT\tJIHGFEDCBA\t2I5M3S\t5086",
                "-bam");
        Assert.assertEquals(String.join("\n",
                "Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMPOS\thgold_Chrom\thgold_Pos\thgold_End\thgold_qStrand"
                        + "\thgold_liftoverStatus",
                "chr1\t411\t420\tr2\tAACGTTGCAT\tABCDEFGHIJ\t3S5M2I\t415\tchr1\t5081\t5090\t-\tmapped",
                "chr1\t500\t500\tr1\tC\tI\t1M\t0\tchr1\t5001\t5001\t-\tmapped", ""), back);
    }

    // ------------------------------------------------------------------------------------------------
    // Step 2: round trips through the inverse chain
    // ------------------------------------------------------------------------------------------------

    /** Blocks on chr1 and chr4 of 1-300 bases with gaps of 0-30 bases, targets on chr1-chr3 that do not overlap. */
    private static List<Block> randomUniqueChain(Random rnd) {
        Map<String, Integer> cursor = new HashMap<>();
        List<Block> chain = new ArrayList<>();
        for (String source : List.of("chr1", "chr4")) {
            int t = 50;
            for (int i = 0; i < 120; i++) {
                int len = i % 17 == 0 ? 1 : 1 + rnd.nextInt(300);
                String q = "chr" + (1 + rnd.nextInt(3));
                int qStart = cursor.merge(q, 0, (a, b) -> a) + rnd.nextInt(31);
                chain.add(new Block(source, t, t + len, q, qStart, rnd.nextBoolean() ? "+" : "-",
                        100 + rnd.nextInt(900)));
                cursor.put(q, qStart + len);
                t += len + (rnd.nextInt(4) == 0 ? 0 : rnd.nextInt(31));
            }
        }
        return chain;
    }

    /** A 0-based start near a block of the chain; chr9 has no blocks. */
    private static String[] anyInterval(Random rnd, List<Block> chain, int maxLen, boolean allowEmpty) {
        if (rnd.nextInt(30) == 0) {
            int s = rnd.nextInt(1000);
            return new String[]{"chr9", Integer.toString(s), Integer.toString(s + rnd.nextInt(50))};
        }
        Block b = chain.get(rnd.nextInt(chain.size()));
        int len = b.tEnd() - b.tStart();
        int s0;
        switch (rnd.nextInt(4)) {
            case 0: // at the start of the block
                s0 = b.tStart() - rnd.nextInt(3);
                break;
            case 1: // at the end of the block
                s0 = b.tEnd() - rnd.nextInt(Math.min(len, maxLen) + 1);
                break;
            default:
                s0 = b.tStart() + rnd.nextInt(len);
        }
        s0 = Math.max(0, s0);
        int e0 = s0 + (allowEmpty ? rnd.nextInt(maxLen + 1) : 1 + rnd.nextInt(maxLen));
        if (rnd.nextInt(3) > 0) {
            e0 = Math.min(e0, b.tEnd()); // mostly within the block
            e0 = Math.max(e0, allowEmpty ? s0 : s0 + 1);
        }
        return new String[]{b.tChrom(), Integer.toString(s0), Integer.toString(e0)};
    }

    private static List<List<Block>> roundTripChains() {
        return List.of(CHAIN_UNIQUE, randomUniqueChain(new Random(SEED)));
    }

    @Test
    public void roundTripSnp() throws IOException {
        int chainNo = 0;
        for (List<Block> chain : roundTripChains()) {
            String suffix = "_snp" + chainNo++;
            String fwd = chainFile("rt" + suffix + ".gor", chain);
            String inv = chainFile("rt_inv" + suffix + ".gor", inverse(chain));
            Random rnd = new Random(SEED);
            List<String[]> in = new ArrayList<>();
            for (int i = 0; i < ROUND_TRIP_ROWS; i++) {
                String[] iv = anyInterval(rnd, chain, 1, false);
                in.add(new String[]{iv[0], Integer.toString(Integer.parseInt(iv[1]) + 1), "A", "G", "id" + i});
            }
            String input = gorFile("in" + suffix + ".gor", "Chrom\tPos\tRef\tAlt\tId", in);
            Table out = run("gor " + input + " | liftover " + fwd + " -snp");
            Assert.assertEquals(ROUND_TRIP_ROWS, out.rows.size());
            Map<String, String[]> byId = out.by("Id");
            List<String[]> back = new ArrayList<>();
            for (String[] r : in) {
                int s0 = Integer.parseInt(r[1]) - 1;
                Block b = find(chain, r[0], s0, s0 + 1);
                String[] o = byId.get(r[4]);
                if (b == null) {
                    Assert.assertEquals(r[4], "unmapped", o[out.col("hgold_liftoverStatus")]);
                    continue;
                }
                int[] t = b.lift(s0, s0 + 1);
                Assert.assertEquals(r[4], b.qChrom() + ":" + (t[0] + 1), o[0] + ":" + o[1]);
                Assert.assertEquals(r[4], "mapped", o[out.col("hgold_liftoverStatus")]);
                Assert.assertEquals(r[4], b.strand(), o[out.col("hgold_qStrand")]);
                back.add(new String[]{o[0], o[1], "A", "G", r[4]});
            }
            Assert.assertTrue("enough inputs map: " + back.size(), back.size() > ROUND_TRIP_ROWS / 4);
            Table rt = run("gor " + gorFile("back" + suffix + ".gor", "Chrom\tPos\tRef\tAlt\tId", back)
                    + " | liftover " + inv + " -snp");
            Map<String, String[]> original = new HashMap<>();
            in.forEach(r -> original.put(r[4], r));
            Assert.assertEquals(back.size(), rt.rows.size());
            for (String[] o : rt.rows) {
                String[] r = original.get(o[rt.col("Id")]);
                Assert.assertEquals(r[4], r[0] + ":" + r[1], o[0] + ":" + o[1]);
                Assert.assertEquals(r[4], "mapped", o[rt.col("hgold_liftoverStatus")]);
            }
        }
    }

    private static String randomAllele(Random rnd, int maxLen) {
        StringBuilder sb = new StringBuilder();
        int n = rnd.nextInt(maxLen + 1);
        for (int i = 0; i < n; i++) {
            sb.append("ACGT".charAt(rnd.nextInt(4)));
        }
        return sb.toString();
    }

    private static String randomAlt(Random rnd) {
        int k = rnd.nextInt(10);
        if (k == 0) {
            return "<DEL>";
        }
        String a = randomAllele(rnd, 6);
        a = a.isEmpty() ? "G" : a;
        return k < 3 ? a + "," + randomAllele(rnd, 4) + "T" : a;
    }

    @Test
    public void roundTripVar() throws IOException {
        int chainNo = 0;
        for (List<Block> chain : roundTripChains()) {
            String suffix = "_var" + chainNo++;
            String fwd = chainFile("rt" + suffix + ".gor", chain);
            String inv = chainFile("rt_inv" + suffix + ".gor", inverse(chain));
            Random rnd = new Random(SEED);
            List<String[]> in = new ArrayList<>();
            for (int i = 0; i < ROUND_TRIP_ROWS; i++) {
                String[] iv = anyInterval(rnd, chain, 1, false);
                int s0 = Integer.parseInt(iv[1]);
                String ref = randomAllele(rnd, 6); // empty: an insertion, a junction (N12)
                in.add(new String[]{iv[0], Integer.toString(s0 + 1), ref, randomAlt(rnd), "id" + i});
            }
            String header = "Chrom\tPos\tRef\tAlt\tId";
            Table out = run("gor " + gorFile("in" + suffix + ".gor", header, in) + " | liftover " + fwd + " -var");
            Assert.assertEquals(ROUND_TRIP_ROWS, out.rows.size());
            Map<String, String[]> byId = out.by("Id");
            List<String[]> back = new ArrayList<>();
            int empty = 0;
            for (String[] r : in) {
                int s0 = Integer.parseInt(r[1]) - 1;
                Block b = find(chain, r[0], s0, s0 + r[2].length());
                String[] o = byId.get(r[4]);
                if (b == null) {
                    Assert.assertEquals(r[4], "unmapped", o[out.col("hgold_liftoverStatus")]);
                    continue;
                }
                empty += r[2].isEmpty() ? 1 : 0;
                int[] t = b.lift(s0, s0 + r[2].length());
                String ref = b.minus() ? revCompAlleles(r[2]) : r[2];
                String alt = b.minus() ? revCompAlleles(r[3]) : r[3];
                Assert.assertEquals(r[4], b.qChrom() + ":" + (t[0] + 1) + ":" + ref + ":" + alt,
                        o[0] + ":" + o[1] + ":" + o[2] + ":" + o[3]);
                Assert.assertEquals(r[4], "mapped", o[out.col("hgold_liftoverStatus")]);
                back.add(new String[]{o[0], o[1], o[2], o[3], r[4]});
            }
            Assert.assertTrue("enough inputs map: " + back.size(), back.size() > ROUND_TRIP_ROWS / 4);
            Assert.assertTrue("insertions map: " + empty, empty > 10);
            Table rt = run("gor " + gorFile("back" + suffix + ".gor", header, back) + " | liftover " + inv + " -var");
            Map<String, String[]> original = new HashMap<>();
            in.forEach(r -> original.put(r[4], r));
            Assert.assertEquals(back.size(), rt.rows.size());
            for (String[] o : rt.rows) {
                String[] r = original.get(o[rt.col("Id")]);
                Assert.assertEquals(r[4], String.join(":", r[0], r[1], r[2], r[3]),
                        String.join(":", o[0], o[1], o[2], o[3]));
                Assert.assertEquals(r[4], "mapped", o[rt.col("hgold_liftoverStatus")]);
            }
        }
    }

    private static List<String[]> randomSegs(Random rnd, List<Block> chain, boolean oneBased) {
        List<String[]> in = new ArrayList<>();
        for (int i = 0; i < ROUND_TRIP_ROWS; i++) {
            // 0-based half-open [s0, e0): empty ones are junctions; -seg1 gives them as end == start - 1
            String[] iv = anyInterval(rnd, chain, 150, true);
            int s0 = Integer.parseInt(iv[1]);
            int e0 = Integer.parseInt(iv[2]);
            in.add(oneBased ? new String[]{iv[0], Integer.toString(s0 + 1), Integer.toString(e0), "id" + i}
                    : new String[]{iv[0], Integer.toString(s0), Integer.toString(e0), "id" + i});
        }
        return in;
    }

    @Test
    public void roundTripSeg() throws IOException {
        int chainNo = 0;
        for (List<Block> chain : roundTripChains()) {
            String suffix = "_seg" + chainNo++;
            String fwd = chainFile("rt" + suffix + ".gor", chain);
            String inv = chainFile("rt_inv" + suffix + ".gor", inverse(chain));
            List<String[]> in = randomSegs(new Random(SEED), chain, false);
            String header = "Chrom\tStart\tEnd\tId";
            Table out = run("gor " + gorFile("in" + suffix + ".gor", header, in) + " | liftover " + fwd + " -seg");
            Assert.assertEquals(ROUND_TRIP_ROWS, out.rows.size());
            Map<String, String[]> byId = out.by("Id");
            List<String[]> back = new ArrayList<>();
            int junctions = 0;
            for (String[] r : in) {
                int s0 = Integer.parseInt(r[1]);
                int e0 = Integer.parseInt(r[2]);
                Block b = find(chain, r[0], s0, e0);
                String[] o = byId.get(r[3]);
                if (b == null) {
                    Assert.assertEquals(r[3], "unmapped", o[out.col("hgold_liftoverStatus")]);
                    continue;
                }
                junctions += s0 == e0 ? 1 : 0;
                int[] t = b.lift(s0, e0);
                Assert.assertEquals(r[3], b.qChrom() + ":" + t[0] + ":" + t[1], o[0] + ":" + o[1] + ":" + o[2]);
                Assert.assertEquals(r[3], "mapped", o[out.col("hgold_liftoverStatus")]);
                Assert.assertEquals(r[3], b.strand(), o[out.col("hgold_qStrand")]);
                back.add(new String[]{o[0], o[1], o[2], r[3]});
            }
            Assert.assertTrue("enough inputs map: " + back.size(), back.size() > ROUND_TRIP_ROWS / 4);
            Assert.assertTrue("junctions map: " + junctions, junctions > 10);
            Table rt = run("gor " + gorFile("back" + suffix + ".gor", header, back) + " | liftover " + inv + " -seg");
            Map<String, String[]> original = new HashMap<>();
            in.forEach(r -> original.put(r[3], r));
            Assert.assertEquals(back.size(), rt.rows.size());
            for (String[] o : rt.rows) {
                String[] r = original.get(o[rt.col("Id")]);
                Assert.assertEquals(r[3], String.join(":", r[0], r[1], r[2]), String.join(":", o[0], o[1], o[2]));
                Assert.assertEquals(r[3], "mapped", o[rt.col("hgold_liftoverStatus")]);
            }
        }
    }

    @Test
    public void roundTripAnnotateSeg1() throws IOException {
        roundTripAnnotate("-seg1", true);
    }

    @Test
    public void roundTripAnnotateSeg() throws IOException {
        roundTripAnnotate("-seg", false);
    }

    /** The lifted start and stop of liftover_annotate are the next input. */
    private void roundTripAnnotate(String mode, boolean oneBased) throws IOException {
        int chainNo = 0;
        for (List<Block> chain : roundTripChains()) {
            String suffix = "_ann" + mode + chainNo++;
            String fwd = chainFile("rt" + suffix + ".gor", chain);
            String inv = chainFile("rt_inv" + suffix + ".gor", inverse(chain));
            List<String[]> in = randomSegs(new Random(SEED), chain, oneBased);
            String header = "Chrom\tStart\tEnd\tId";
            Table out = run("gor " + gorFile("in" + suffix + ".gor", header, in) + " | liftover_annotate " + fwd
                    + " " + mode);
            Assert.assertEquals(ROUND_TRIP_ROWS, out.rows.size());
            Map<String, String[]> byId = out.by("Id");
            int status = out.col("liftover_status");
            int chrom = out.col("liftover_chrom");
            int start = out.col("liftover_start");
            int stop = out.col("liftover_stop");
            List<String[]> back = new ArrayList<>();
            int junctions = 0;
            for (String[] r : in) {
                int s0 = Integer.parseInt(r[1]) - (oneBased ? 1 : 0);
                int e0 = Integer.parseInt(r[2]);
                Block b = find(chain, r[0], s0, e0);
                String[] o = byId.get(r[3]);
                if (b == null) {
                    Assert.assertNotEquals(r[3], "mapped", o[status]);
                    continue;
                }
                junctions += s0 == e0 ? 1 : 0;
                int[] t = b.lift(s0, e0);
                int ts = t[0] + (oneBased ? 1 : 0);
                Assert.assertEquals(r[3], "mapped:" + b.qChrom() + ":" + ts + ":" + t[1] + ":" + b.strand(),
                        String.join(":", o[status], o[chrom], o[start], o[stop], o[out.col("liftover_strand")]));
                back.add(new String[]{o[chrom], o[start], o[stop], r[3]});
            }
            Assert.assertTrue("enough inputs map: " + back.size(), back.size() > ROUND_TRIP_ROWS / 4);
            Assert.assertTrue("junctions map: " + junctions, junctions > 10);
            Table rt = run("gor " + gorFile("back" + suffix + ".gor", header, back) + " | liftover_annotate " + inv
                    + " " + mode);
            Map<String, String[]> original = new HashMap<>();
            in.forEach(r -> original.put(r[3], r));
            Assert.assertEquals(back.size(), rt.rows.size());
            for (String[] o : rt.rows) {
                String[] r = original.get(o[rt.col("Id")]);
                Assert.assertEquals(r[3], "mapped:" + r[0] + ":" + r[1] + ":" + r[2],
                        String.join(":", o[rt.col("liftover_status")], o[rt.col("liftover_chrom")],
                                o[rt.col("liftover_start")], o[rt.col("liftover_stop")]));
            }
        }
    }

    private static final String QUALS = "!#+05:?AFIJ";
    private static final String[] CIGARS = {"%dM", "2S%dM", "%dM3S", "1S%dM1S", "%dM2I", "3M1D%dM"};

    @Test
    public void roundTripBam() throws IOException {
        int chainNo = 0;
        for (List<Block> chain : roundTripChains()) {
            String suffix = "_bam" + chainNo++;
            String fwd = chainFile("rt" + suffix + ".gor", chain);
            String inv = chainFile("rt_inv" + suffix + ".gor", inverse(chain));
            Random rnd = new Random(SEED);
            List<String[]> in = new ArrayList<>();
            for (int i = 0; i < ROUND_TRIP_ROWS; i++) {
                String[] iv = anyInterval(rnd, chain, 100, false); // 1-base reads included, 1-based Pos, End
                int pos = Integer.parseInt(iv[1]) + 1;
                int end = Integer.parseInt(iv[2]);
                int len = end - pos + 1;
                StringBuilder seq = new StringBuilder();
                StringBuilder qual = new StringBuilder();
                for (int k = 0; k < len; k++) {
                    seq.append("ACGTN".charAt(rnd.nextInt(5)));
                    qual.append(QUALS.charAt(rnd.nextInt(QUALS.length())));
                }
                String mpos;
                switch (rnd.nextInt(8)) {
                    case 0:
                        mpos = "0";
                        break;
                    case 1:
                        mpos = "-1";
                        break;
                    case 2:
                        mpos = "*";
                        break;
                    default: // near the read, in the chain
                        mpos = Integer.toString(Math.max(1, pos - 200 + rnd.nextInt(500)));
                }
                in.add(new String[]{iv[0], Integer.toString(pos), Integer.toString(end), "r" + i, seq.toString(),
                        qual.toString(), String.format(CIGARS[rnd.nextInt(CIGARS.length)], len), mpos});
            }
            String header = "Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMPOS";
            Table out = run("gor " + gorFile("in" + suffix + ".gor", header, in) + " | liftover " + fwd + " -bam");
            Assert.assertEquals(ROUND_TRIP_ROWS, out.rows.size());
            Map<String, String[]> byId = out.by("QName");
            List<String[]> back = new ArrayList<>();
            int oneBase = 0;
            int mateKept = 0;
            for (String[] r : in) {
                int s0 = Integer.parseInt(r[1]) - 1;
                int e0 = Integer.parseInt(r[2]);
                Block b = find(chain, r[0], s0, e0);
                String[] o = byId.get(r[3]);
                if (b == null) {
                    Assert.assertEquals(r[3], "unmapped", o[out.col("hgold_liftoverStatus")]);
                    continue;
                }
                oneBase += s0 + 1 == e0 ? 1 : 0;
                int[] t = b.lift(s0, e0);
                String expectedMpos = mate(chain, b, r[0], "", r[7], false);
                String[] expected = {b.qChrom(), Integer.toString(t[0] + 1), Integer.toString(t[1]), r[3],
                        b.minus() ? revComp(r[4]) : r[4], b.minus() ? revString(r[5]) : r[5],
                        b.minus() ? revCigar(r[6]) : r[6], expectedMpos};
                Assert.assertEquals(r[3], String.join(":", expected), String.join(":", java.util.Arrays.copyOf(o, 8)));
                Assert.assertEquals(r[3], "mapped", o[out.col("hgold_liftoverStatus")]);
                Assert.assertEquals(r[3], b.strand(), o[out.col("hgold_qStrand")]);
                mateKept += mateValue(o[7]) > 0 ? 1 : 0;
                back.add(java.util.Arrays.copyOf(o, 8));
            }
            Assert.assertTrue("enough reads map: " + back.size(), back.size() > ROUND_TRIP_ROWS / 4);
            Assert.assertTrue("1-base reads map: " + oneBase, oneBase > 10);
            Assert.assertTrue("mates are lifted: " + mateKept, mateKept > 10);
            Table rt = run("gor " + gorFile("back" + suffix + ".gor", header, back) + " | liftover " + inv + " -bam");
            Map<String, String[]> original = new HashMap<>();
            in.forEach(r -> original.put(r[3], r));
            Map<String, String[]> lifted = new HashMap<>();
            back.forEach(r -> lifted.put(r[3], r));
            Assert.assertEquals(back.size(), rt.rows.size());
            for (String[] o : rt.rows) {
                String[] r = original.get(o[rt.col("QName")]);
                String[] l = lifted.get(r[3]);
                Assert.assertEquals(r[3], "mapped", o[rt.col("hgold_liftoverStatus")]);
                for (int c = 0; c < 7; c++) { // everything but MPOS
                    Assert.assertEquals(r[3] + " column " + c, r[c], o[c]);
                }
                // MPOS is lifted on its own: kept in both directions when it is a mate position that stays on the
                // contig, and passed through when it is no mate position (R18)
                if (mateValue(r[7]) < 0 || mateValue(l[7]) > 0) {
                    Assert.assertEquals(r[3] + " MPOS", r[7], o[7]);
                } else {
                    Assert.assertEquals(r[3] + " MPOS lost on the first lift", "0", l[7]);
                    Assert.assertEquals(r[3] + " MPOS stays lost", "0", o[7]);
                }
            }
        }
    }
}
