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

import gorsat.liftover.ChainBlock;
import gorsat.liftover.LegacyLiftoverQuery;
import gorsat.liftover.LiftEngine;
import gorsat.liftover.LiftFixtures.NaiveLift;
import gorsat.liftover.LiftFixtures.RandomChains;
import gorsat.parser.ParseUtilities;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.test.SlowTests;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Old-vs-new differential for LIFTOVER (ENGKNOW-4117, Task 10, N17).
 * <p>
 * "Old" is the frozen pre-P4 expansion ({@link LegacyLiftoverQuery}) run as a plain query, "new" is the
 * {@code liftover} command. Both run on seeded random chains and inputs ({@link RandomChains}); the outputs are
 * grouped by Id ({@code QName} for bam) and every Id's difference must be one named category:
 * <ul>
 * <li>{@code D1-collapse}: the dup run; old has the row once, new three times, rows otherwise equal.</li>
 * <li>{@code D2-tie}: not {@code -all}; old is exactly the k &gt;= 2 tied max-score rows of the old model, new one
 * {@code ambiguous} row at {@code (src chrom, 0[, 1])} (bam MPOS 0), and the naive model under the new rules is
 * ambiguous.</li>
 * <li>{@code D3-allele-revcomp}: var on {@code -}; rows equal except REF/ALT, new REF/ALT =
 * {@code revcompAlleles(input)}.</li>
 * <li>{@code D4-bam-minus}: bam on {@code -}; new target Pos and End are old + 1, everything else equal except
 * MPOS (old the old shift, new per N15).</li>
 * <li>{@code D5-bam-block-start}: bam; old mapped through a block with {@code tStart == Pos}, new does not use
 * that block. Without {@code -all} the new row must equal the naive model's row and every other old row must pair
 * with it; with {@code -all} the other old rows pair with the new rows as same/D4/D6, or new is the single
 * {@code unmapped} row.</li>
 * <li>{@code D6-bam-mpos}: bam, mapped rows; rows equal except MPOS (old the old shift, new per N15).</li>
 * <li>{@code D7-endpoint-tie}: seg/bam, not {@code -all}; old one {@code unmapped} row, new one {@code ambiguous}
 * row at the same placement, every other column equal (bam MPOS: old shifted, new 0); no block contains the
 * interval and its first or last base is in an exact score tie (naive model).</li>
 * <li>{@code D8-bam-last-base}: bam, a 1-base read ({@code Pos == End}); old equals the old model (strict
 * {@code tStart < Pos < tEnd}), new equals the new model (N15, {@code tStart < Pos <= tEnd}, best or ambiguous
 * without {@code -all}), and every block in the new set only has {@code tEnd == Pos}.</li>
 * <li>{@code D9-bam-unmapped-mpos}: bam, unmapped/ambiguous rows; old MPOS is the old {@code MPOS - Pos} shift,
 * new MPOS is 0 (N15), everything else equal.</li>
 * </ul>
 * Anything else is unexplained and fails. Before P4 (Task 11) every Id was {@code same}, which proved the harness
 * and the oracle; {@link #oracleMatchesFrozenLegacy} keeps the oracle pinned to the frozen pre-P4 output.
 */
public class UTestGorLiftoverDifferential {

    static final String SAME = "same";
    static final String D1 = "D1-collapse";
    static final String D2 = "D2-tie";
    static final String D3 = "D3-allele-revcomp";
    static final String D4 = "D4-bam-minus";
    static final String D5 = "D5-bam-block-start";
    static final String D6 = "D6-bam-mpos";
    static final String D7 = "D7-endpoint-tie";
    static final String D8 = "D8-bam-last-base";
    static final String D9 = "D9-bam-unmapped-mpos";

    private static final int ROWS = 2_000;
    private static final int SLOW_ROWS = 50_000;
    private static final int SEED_UNIQUE = 4117;
    private static final int SEED_OVERLAP = 4118;
    private static final List<String> MODES = List.of("snp", "var", "seg", "bam");
    private static final int MAX_PRINT = 20;

    private static final String MAPPED = "mapped";
    private static final String UNMAPPED = "unmapped";
    private static final String AMBIGUOUS = "ambiguous";

    // bam input columns (RandomChains layout Chrom Pos End QName SEQ QUAL CIGAR MPOS)
    private static final int SEQ = 4;
    private static final int QUAL = 5;
    private static final int CIGAR = 6;
    private static final int MPOS = 7;

    private static Path tmpdir;
    /** Outcomes of the normal-suite differentials, keyed by config, so categoriesExpectedAfterP4 can reuse them. */
    private static final Map<String, Outcome> OUTCOMES = new HashMap<>();

    @BeforeClass
    public static void setUp() throws IOException {
        tmpdir = Files.createTempDirectory("liftover-differential");
        UTestGorLiftoverCharacterization.setUp();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        UTestGorLiftoverCharacterization.tearDown();
        if (tmpdir != null) {
            try (Stream<Path> walk = Files.walk(tmpdir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Oracle faithfulness
    // ------------------------------------------------------------------------------------------------

    /**
     * The oracle equals the frozen pre-P4 LIFTOVER output: every Task 1 query with a {@code *_LEGACY} constant, and
     * the Task 1 error cases. It does not run LIFTOVER, so it stays green after Task 11.
     */
    @Test
    public void oracleMatchesFrozenLegacy() {
        String chain = UTestGorLiftoverCharacterization.CHAIN;
        String ovl = UTestGorLiftoverCharacterization.CHAIN_OVL;
        String zero = UTestGorLiftoverCharacterization.CHAIN_ZERO;
        String snp = UTestGorLiftoverCharacterization.SNP;
        String var = UTestGorLiftoverCharacterization.VAR;
        String seg = UTestGorLiftoverCharacterization.SEG;
        String seg3 = UTestGorLiftoverCharacterization.SEG3;
        String refopt = UTestGorLiftoverCharacterization.REFOPT;
        String bam = UTestGorLiftoverCharacterization.BAM;
        String ovlin = UTestGorLiftoverCharacterization.OVLIN;
        String coll = UTestGorLiftoverCharacterization.COLL;
        Object[][] frozen = {
                {chain, snp, "-snp", UTestGorLiftoverCharacterization.SNP_DEFAULT_LEGACY},
                {chain, snp, "-snp -build hg19",
                        UTestGorLiftoverCharacterization.SNP_DEFAULT_LEGACY.replace("hgold_", "hg19_")},
                {chain, snp, "-snp -all", UTestGorLiftoverCharacterization.SNP_ALL_LEGACY},
                {"<(gor " + chain + ")", snp, "-snp", UTestGorLiftoverCharacterization.SNP_DEFAULT_LEGACY},
                {chain, var, "-var", UTestGorLiftoverCharacterization.VAR_DEFAULT_LEGACY},
                {chain, var, "-var -all", UTestGorLiftoverCharacterization.VAR_ALL_LEGACY},
                {chain, seg, "-seg", UTestGorLiftoverCharacterization.SEG_DEFAULT_LEGACY},
                {chain, seg, "-seg -all", UTestGorLiftoverCharacterization.SEG_ALL_LEGACY},
                {chain, seg3, "-seg", UTestGorLiftoverCharacterization.SEG3_DEFAULT_LEGACY},
                {chain, seg3, "-seg -all", UTestGorLiftoverCharacterization.SEG3_ALL_LEGACY},
                {chain, refopt, "-var", UTestGorLiftoverCharacterization.REFOPT_DEFAULT_LEGACY},
                {chain, refopt, "-var -ref LongRef", UTestGorLiftoverCharacterization.REFOPT_LONGREF_LEGACY},
                {chain, UTestGorLiftoverCharacterization.MA, "-var", UTestGorLiftoverCharacterization.MA_LEGACY},
                {ovl, bam, "-bam", UTestGorLiftoverCharacterization.BAM_DEFAULT_LEGACY},
                {ovl, bam, "-bam -all", UTestGorLiftoverCharacterization.BAM_ALL_LEGACY},
                {zero, ovlin, "-snp", UTestGorLiftoverCharacterization.ZERO_STRAND_LEGACY},
                {zero, ovlin, "-snp -all", UTestGorLiftoverCharacterization.ZERO_STRAND_ALL_LEGACY},
                {chain, coll, "-snp -build hg19", UTestGorLiftoverCharacterization.COLL_NOTE_LEGACY},
        };
        for (Object[] f : frozen) {
            String in = (String) f[1];
            Assert.assertEquals("oracle " + f[0] + " " + f[2] + " on " + in, f[3],
                    TestUtils.runGorPipe("gor " + in + " | "
                            + LegacyLiftoverQuery.expand((String) f[0], (String) f[2], header(Path.of(in)))));
        }
        // the Task 1 error cases, frozen
        assertOracleThrows(chain, snp, "", GorDataException.class, "Illegal stop position in column #3");
        assertOracleThrows(chain, snp, "-snp -var", GorParsingException.class, "specify only one liftover type");
        assertOracleThrows(chain, coll, "-snp", GorDataException.class, "Duplicate column name 'hgold_POS'");
        System.out.println("oracleMatchesFrozenLegacy: " + frozen.length + " frozen outputs and 3 errors matched");
    }

    private static void assertOracleThrows(String chain, String input, String opts, Class<?> type, String text) {
        try {
            TestUtils.runGorPipe("gor " + input + " | "
                    + LegacyLiftoverQuery.expand(chain, opts, header(Path.of(input))));
            Assert.fail("expected " + type.getSimpleName() + " for " + opts);
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getClass().getName(), type.isInstance(e));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(text));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Differential
    // ------------------------------------------------------------------------------------------------

    @Test
    public void differential_snp_default_unique() {
        check("snp", false, SEED_UNIQUE, false);
    }

    @Test
    public void differential_snp_all_unique() {
        check("snp", true, SEED_UNIQUE, false);
    }

    @Test
    public void differential_snp_default_overlapping() {
        check("snp", false, SEED_OVERLAP, true);
    }

    @Test
    public void differential_snp_all_overlapping() {
        check("snp", true, SEED_OVERLAP, true);
    }

    @Test
    public void differential_var_default_unique() {
        check("var", false, SEED_UNIQUE, false);
    }

    @Test
    public void differential_var_all_unique() {
        check("var", true, SEED_UNIQUE, false);
    }

    @Test
    public void differential_var_default_overlapping() {
        check("var", false, SEED_OVERLAP, true);
    }

    @Test
    public void differential_var_all_overlapping() {
        check("var", true, SEED_OVERLAP, true);
    }

    @Test
    public void differential_seg_default_unique() {
        check("seg", false, SEED_UNIQUE, false);
    }

    @Test
    public void differential_seg_all_unique() {
        check("seg", true, SEED_UNIQUE, false);
    }

    @Test
    public void differential_seg_default_overlapping() {
        check("seg", false, SEED_OVERLAP, true);
    }

    @Test
    public void differential_seg_all_overlapping() {
        check("seg", true, SEED_OVERLAP, true);
    }

    @Test
    public void differential_bam_default_unique() {
        check("bam", false, SEED_UNIQUE, false);
    }

    @Test
    public void differential_bam_all_unique() {
        check("bam", true, SEED_UNIQUE, false);
    }

    @Test
    public void differential_bam_default_overlapping() {
        check("bam", false, SEED_OVERLAP, true);
    }

    @Test
    public void differential_bam_all_overlapping() {
        check("bam", true, SEED_OVERLAP, true);
    }

    @Test
    public void differential_nested() {
        Outcome o = differential("snp", false, SEED_UNIQUE, false, ROWS, true);
        assertOutcome(o, ROWS);
    }

    @Category(SlowTests.class)
    @Test
    public void differential_slow() {
        Map<String, Integer> total = new TreeMap<>();
        List<String> failures = new ArrayList<>();
        for (int seed = 4117; seed <= 4126; seed++) {
            for (boolean overlapping : new boolean[]{false, true}) {
                for (String mode : MODES) {
                    for (boolean all : new boolean[]{false, true}) {
                        Outcome o = differential(mode, all, seed, overlapping, SLOW_ROWS, false);
                        o.counts.forEach((k, v) -> total.merge(k, v, Integer::sum));
                        try {
                            assertOutcome(o, SLOW_ROWS);
                        } catch (AssertionError e) {
                            failures.add(e.getMessage());
                        }
                    }
                }
            }
        }
        System.out.println("differential_slow total: " + total);
        Assert.assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    /** Keeps the differential from passing vacuously after P4: every expected category occurs. */
    @Test
    public void categoriesExpectedAfterP4() {
        Map<String, Integer> unique = new TreeMap<>();
        Map<String, Integer> overlapping = new TreeMap<>();
        for (String mode : MODES) {
            for (boolean all : new boolean[]{false, true}) {
                outcome(mode, all, SEED_UNIQUE, false).counts.forEach((k, v) -> unique.merge(k, v, Integer::sum));
                outcome(mode, all, SEED_OVERLAP, true).counts.forEach((k, v) -> overlapping.merge(k, v, Integer::sum));
            }
        }
        System.out.println("categoriesExpectedAfterP4 unique chain: " + unique);
        System.out.println("categoriesExpectedAfterP4 overlapping chain: " + overlapping);
        for (String c : List.of(D1, D3, D4, D5, D6, D8, D9)) {
            Assert.assertTrue(c + " never occurs on the non-overlapping chain: " + unique,
                    unique.getOrDefault(c, 0) > 0);
        }
        for (String c : List.of(D2, D7)) {
            Assert.assertTrue(c + " never occurs on the overlapping chain: " + overlapping,
                    overlapping.getOrDefault(c, 0) > 0);
        }
    }

    private static void check(String mode, boolean all, int seed, boolean overlapping) {
        assertOutcome(outcome(mode, all, seed, overlapping), ROWS);
    }

    private static synchronized Outcome outcome(String mode, boolean all, int seed, boolean overlapping) {
        String key = mode + "|" + all + "|" + seed + "|" + overlapping;
        return OUTCOMES.computeIfAbsent(key, k -> differential(mode, all, seed, overlapping, ROWS, false));
    }

    /** One old-vs-new comparison: per input Id, its category, or a failure. */
    static final class Outcome {
        final String name;
        final Map<String, Integer> counts = new TreeMap<>();
        final List<String> failures = new ArrayList<>();
        int ids;
        int oldMapped;
        int oldUnmapped;
        int oldMinus;
        int absentContig;
        int modelMismatches;

        Outcome(String name) {
            this.name = name;
        }
    }

    private static void assertOutcome(Outcome o, int rows) {
        System.out.println(o.name + ": " + o.counts + " (" + o.ids + " Ids, old rows: " + o.oldMapped + " mapped, "
                + o.oldUnmapped + " unmapped, " + o.oldMinus + " on -; " + o.absentContig
                + " Ids on contigs absent from the chain)");
        if (!o.failures.isEmpty()) {
            StringBuilder sb = new StringBuilder(o.name + ": " + o.failures.size() + " unexplained Ids, first "
                    + Math.min(MAX_PRINT, o.failures.size()) + ":\n");
            o.failures.stream().limit(MAX_PRINT).forEach(f -> sb.append(f).append('\n'));
            System.out.println(sb);
            Assert.fail(sb.toString());
        }
        // rows - 3 unique rows plus the dup run: the comparison covered every input Id
        Assert.assertEquals(o.name + ": compared Ids", rows - 2, o.ids);
        Assert.assertTrue(o.name + ": no mapped old rows", o.oldMapped > 0);
        Assert.assertTrue(o.name + ": no unmapped old rows", o.oldUnmapped > 0);
        Assert.assertTrue(o.name + ": no old rows on the - strand", o.oldMinus > 0);
        Assert.assertTrue(o.name + ": no Ids on contigs absent from the chain", o.absentContig > 0);
    }

    private static Outcome differential(String mode, boolean all, int seed, boolean overlapping, int rows,
                                        boolean nested) {
        Path chain = RandomChains.chain(tmpdir, seed, overlapping);
        Path input = RandomChains.input(tmpdir, chain, seed, mode, rows);
        String chainArg = nested ? "<(gor " + chain + ")" : chain.toString();
        String opts = "-" + mode + (all ? " -all" : "");
        String name = "differential " + opts + " seed " + seed + (overlapping ? " overlapping" : " unique")
                + (nested ? " nested" : "") + " rows " + rows;

        String oldOut = TestUtils.runGorPipe("gor " + input + " | "
                + LegacyLiftoverQuery.expand(chainArg, opts, header(input)));
        String newOut = TestUtils.runGorPipe("gor " + input + " | liftover " + chainArg + " " + opts);
        return compare(name, mode, all, input, oldOut, newOut, RandomChains.blocks(chain));
    }

    private static Outcome compare(String name, String mode, boolean all, Path input, String oldOut,
                                   String newOut, Map<String, List<ChainBlock>> chain) {
        Outcome o = new Outcome(name);
        String idName = mode.equals("bam") ? "QName" : "Id";
        List<String> inLines = readLines(input);
        int inId = column(inLines.get(0), idName);
        Map<String, String> inputById = new LinkedHashMap<>();
        for (String line : inLines.subList(1, inLines.size())) {
            inputById.putIfAbsent(line.split("\t", -1)[inId], line);
        }

        String[] oldLines = oldOut.split("\n");
        String[] newLines = newOut.split("\n");
        if (!oldLines[0].equals(newLines[0])) {
            o.failures.add("header differs: old " + oldLines[0] + " new " + newLines[0]);
            return o;
        }
        Map<String, List<String>> oldById = group(oldLines, idName);
        Map<String, List<String>> newById = group(newLines, idName);

        int status = column(oldLines[0], "_liftoverStatus");
        int strand = column(oldLines[0], "_qStrand");
        for (int i = 1; i < oldLines.length; i++) {
            String[] c = oldLines[i].split("\t", -1);
            if (MAPPED.equals(c[status])) {
                o.oldMapped++;
            } else if (UNMAPPED.equals(c[status])) {
                o.oldUnmapped++;
            }
            if ("-".equals(c[strand])) {
                o.oldMinus++;
            }
        }

        for (Map.Entry<String, String> e : inputById.entrySet()) {
            String id = e.getKey();
            List<String> oldRows = oldById.get(id);
            List<String> newRows = newById.get(id);
            // self-check: the classifier's old model reproduces the oracle's rows for every Id
            if (oldRows != null) {
                String[] in = e.getValue().split("\t", -1);
                List<String[]> model = oldModelRows(in, new Layout(mode, in.length), all,
                        chain.getOrDefault(in[0], List.of()));
                if (!sameRows(model, split(oldRows))) {
                    List<String> m = new ArrayList<>();
                    model.forEach(r -> m.add(String.join("\t", r)));
                    o.failures.add(id + ": old model mismatch, input " + e.getValue() + ", model " + m
                            + ", oracle " + oldRows);
                    o.modelMismatches++;
                }
            }
            String category = oldRows == null || newRows == null ? null
                    : classify(oldRows, newRows, e.getValue(), mode, chain);
            if (category == null) {
                o.failures.add(id + ", input " + e.getValue() + ", old " + oldRows + ", new " + newRows);
            } else {
                o.counts.merge(category, 1, Integer::sum);
            }
            o.ids++;
            if (!chain.containsKey(e.getValue().split("\t", -1)[0])) {
                o.absentContig++;
            }
        }
        Set<String> extra = new HashSet<>(oldById.keySet());
        extra.addAll(newById.keySet());
        extra.removeAll(inputById.keySet());
        for (String id : extra) {
            o.failures.add(id + ": not an input Id, old " + oldById.get(id) + ", new " + newById.get(id));
        }
        return o;
    }

    // ------------------------------------------------------------------------------------------------
    // Classifier
    // ------------------------------------------------------------------------------------------------

    /**
     * The category of one Id's difference without the chain: {@code same}, D1 or D3. Every other category is
     * checked against the chain, so this returns {@code null} (unexplained) for them.
     */
    static String classify(List<String> oldRows, List<String> newRows, String inputRow, String mode) {
        return classify(oldRows, newRows, inputRow, mode, null);
    }

    /**
     * The category of one Id's difference: {@code same} when the row multisets are equal, a category name, or
     * {@code null} when the difference is not exactly one of the categories. Row layouts are the RandomChains
     * layouts. {@code chain} is the chain's blocks per source contig.
     */
    static String classify(List<String> oldRows, List<String> newRows, String inputRow, String mode,
                           Map<String, List<ChainBlock>> chain) {
        if (sameMultiset(oldRows, newRows)) {
            return SAME;
        }
        String[] in = inputRow.split("\t", -1);
        Layout l = new Layout(mode, in.length);
        List<String[]> o = split(oldRows);
        List<String[]> n = split(newRows);
        Boolean all = l.all(o, n);
        if (all == null) {
            return null;
        }
        if (isCollapse(o, n, in, l)) {
            return D1;
        }
        if (mode.equals("var") && chain == null) {
            return varRevcomp(o, n, in, l);
        }
        if (chain == null) {
            return null;
        }
        List<ChainBlock> blocks = chain.getOrDefault(in[0], List.of());
        if (!all && isTie(o, n, in, l, blocks)) {
            return D2;
        }
        if (mode.equals("var")) {
            return varRevcomp(o, n, in, l);
        }
        if (!all && l.span() && isEndpointTie(o, n, in, l, blocks)) {
            return D7;
        }
        if (mode.equals("bam")) {
            String c = bam(o, n, in, l, all, blocks);
            if (c != null && !SAME.equals(c)) {
                return c;
            }
            if (isLastBase(o, n, in, l, all, blocks)) {
                return D8;
            }
        }
        return null;
    }

    /** Column layout of an output row for an input row of n columns. */
    private record Layout(String mode, int n) {
        boolean span() {
            return mode.equals("seg") || mode.equals("bam");
        }

        boolean bam() {
            return mode.equals("bam");
        }

        int id() {
            return span() ? 3 : 4;
        }

        int firstInputCol() {
            return span() ? 3 : 2;
        }

        int strand() {
            return n + (span() ? 3 : 2);
        }

        int status() {
            return strand() + 1;
        }

        int score() {
            return strand() + 2;
        }

        /** true/false for -all when every row has the matching width, null otherwise. */
        Boolean all(List<String[]> o, List<String[]> m) {
            int w = status() + 1;
            Boolean all = null;
            for (List<String[]> rows : List.of(o, m)) {
                for (String[] r : rows) {
                    boolean a;
                    if (r.length == w) {
                        a = false;
                    } else if (r.length == w + 1) {
                        a = true;
                    } else {
                        return null;
                    }
                    if (all != null && all != a) {
                        return null;
                    }
                    all = a;
                }
            }
            return all;
        }
    }

    private static List<String[]> split(List<String> rows) {
        List<String[]> r = new ArrayList<>(rows.size());
        for (String s : rows) {
            r.add(s.split("\t", -1));
        }
        return r;
    }

    private static boolean sameMultiset(List<String> a, List<String> b) {
        List<String> x = new ArrayList<>(a);
        List<String> y = new ArrayList<>(b);
        x.sort(null);
        y.sort(null);
        return x.equals(y);
    }

    private static boolean sameRows(List<String[]> a, List<String[]> b) {
        List<String> x = new ArrayList<>();
        List<String> y = new ArrayList<>();
        for (String[] r : a) {
            if (r == null) {
                return false;
            }
            x.add(String.join("\t", r));
        }
        b.forEach(r -> y.add(String.join("\t", r)));
        return sameMultiset(x, y);
    }

    /** The row new LIFTOVER emits for an input that does not map: (src chrom, 0[, 1]), strand '', bam MPOS 0. */
    private static String[] placeholder(String[] in, Layout l, String status, boolean all) {
        List<String> r = new ArrayList<>();
        r.add(in[0]);
        r.add("0");
        if (l.span()) {
            r.add("1");
        }
        for (int k = l.firstInputCol(); k < in.length; k++) {
            r.add(l.bam() && k == MPOS ? "0" : in[k]);
        }
        r.add(in[0]);
        r.add(in[1]);
        if (l.span()) {
            r.add(in[2]);
        }
        r.add("");
        r.add(status);
        if (all) {
            r.add("0.0");
        }
        return r.toArray(new String[0]);
    }

    /** The old bam MPOS: {@code MPOS-#2+liftover_nStart} when MPOS is an integer, else unchanged. */
    private static String oldMpos(String[] in, int nStart) {
        try {
            return String.valueOf(Integer.parseInt(in[MPOS]) - Integer.parseInt(in[1]) + nStart);
        } catch (NumberFormatException e) {
            return in[MPOS];
        }
    }

    /** The old (pre-P4) unmapped row: the placeholder, with the old bam MPOS shift {@code MPOS - Pos}. */
    private static String[] oldUnmapped(String[] in, Layout l, boolean all) {
        String[] r = placeholder(in, l, UNMAPPED, all);
        if (l.bam()) {
            r[MPOS] = oldMpos(in, 0);
        }
        return r;
    }

    // ---- old model: the pre-P4 rules, written from the expansion's arithmetic ----

    /**
     * Blocks the old expansion maps the input through, as NaiveLift hits: snp a point; var the REF interval, or
     * the junction at pos-1 for an empty REF; seg and bam the 0-based [#2, #3), a junction (strict) when empty.
     */
    private static List<NaiveLift.Hit> oldHits(String[] in, Layout l, List<ChainBlock> blocks) {
        NaiveLift nl = new NaiveLift(blocks);
        int p = Integer.parseInt(in[1]);
        switch (l.mode()) {
            case "snp":
                return nl.pointHits(p);
            case "var": {
                int len = in[2].length();
                return len == 0 ? nl.junctionHits(p - 1) : nl.intervalHits(p, p + len - 1);
            }
            default: {
                int e = Integer.parseInt(in[2]);
                return p == e ? nl.junctionHits(p) : nl.intervalHits(p + 1, e);
            }
        }
    }

    /** The old mapped row through a hit (target, whole-string revcompl of REF/ALT, bam transforms and MPOS shift). */
    private static String[] oldRow(String[] in, Layout l, NaiveLift.Hit h, boolean all) {
        boolean minus = "-".equals(h.strand());
        List<String> r = new ArrayList<>();
        r.add(h.chrom());
        switch (l.mode()) {
            case "snp" -> r.add(String.valueOf(h.end0()));
            case "var" -> r.add(String.valueOf(h.start0() + 1));
            default -> {
                r.add(String.valueOf(h.start0()));
                r.add(String.valueOf(h.end0()));
            }
        }
        for (int k = l.firstInputCol(); k < in.length; k++) {
            String v = in[k];
            if (minus && l.mode().equals("var") && (k == 2 || k == 3)) {
                v = ParseUtilities.reverseComplement(v);
            } else if (l.bam()) {
                v = bamColumn(in, k, minus, oldMpos(in, h.start0()));
            }
            r.add(v);
        }
        r.add(in[0]);
        r.add(in[1]);
        if (l.span()) {
            r.add(in[2]);
        }
        r.add(h.strand());
        r.add(MAPPED);
        if (all) {
            r.add(String.valueOf(h.score()));
        }
        return r.toArray(new String[0]);
    }

    /** Input column k of a bam row on the given strand: SEQ revcomp, QUAL reversed, CIGAR revCigar, MPOS given. */
    private static String bamColumn(String[] in, int k, boolean minus, String mpos) {
        return switch (k) {
            case SEQ -> minus ? ParseUtilities.reverseComplement(in[SEQ]) : in[SEQ];
            case QUAL -> minus ? new StringBuilder(in[QUAL]).reverse().toString() : in[QUAL];
            case CIGAR -> minus ? ParseUtilities.revCigar(in[CIGAR]) : in[CIGAR];
            case MPOS -> mpos;
            default -> in[k];
        };
    }

    /** The old output rows: every hit under -all, the max-score hits (ties kept) otherwise, or the unmapped row. */
    private static List<String[]> oldModelRows(String[] in, Layout l, boolean all, List<ChainBlock> blocks) {
        List<NaiveLift.Hit> hits = oldHits(in, l, blocks);
        List<String[]> rows = new ArrayList<>();
        if (hits.isEmpty()) {
            rows.add(oldUnmapped(in, l, all));
            return rows;
        }
        double max = hits.stream().mapToDouble(NaiveLift.Hit::score).max().orElseThrow();
        for (NaiveLift.Hit h : hits) {
            if (all || h.score() == max) {
                rows.add(oldRow(in, l, h, all));
            }
        }
        return rows;
    }

    // ---- categories ----

    /** D1: the dup run, once in old, three times in new. */
    private static boolean isCollapse(List<String[]> o, List<String[]> n, String[] in, Layout l) {
        if (!in[l.id()].startsWith("dup") || o.size() != 1 || n.size() != 3) {
            return false;
        }
        for (String[] r : n) {
            if (!Arrays.equals(r, o.get(0))) {
                return false;
            }
        }
        return true;
    }

    /**
     * D2: old is exactly the k &gt;= 2 tied max-score rows of the old model, new is the one ambiguous placeholder,
     * and the naive model under the new rules is ambiguous.
     */
    private static boolean isTie(List<String[]> o, List<String[]> n, String[] in, Layout l, List<ChainBlock> blocks) {
        if (o.size() < 2 || n.size() != 1 || !Arrays.equals(n.get(0), placeholder(in, l, AMBIGUOUS, false))) {
            return false;
        }
        for (String[] r : o) {
            if (!MAPPED.equals(r[l.status()])) {
                return false;
            }
        }
        return sameRows(oldModelRows(in, l, false, blocks), o)
                && AMBIGUOUS.equals(naiveStatus(in, l, blocks));
    }

    /** The naive model's status for the input row under the new rules (N8, N12, N15). */
    private static String naiveStatus(String[] in, Layout l, List<ChainBlock> blocks) {
        NaiveLift nl = new NaiveLift(blocks);
        int p = Integer.parseInt(in[1]);
        return switch (l.mode()) {
            case "snp" -> nl.point(p).status();
            case "var" -> nl.var(p, in[2].length()).status();
            case "seg" -> nl.span(p + 1, Integer.parseInt(in[2])).status();
            default -> nl.span(p, Integer.parseInt(in[2])).status();
        };
    }

    /** D3: rows pair up equal except REF/ALT, the changed ones on '-' with REF/ALT = revcompAlleles(input). */
    private static String varRevcomp(List<String[]> o, List<String[]> n, String[] in, Layout l) {
        if (o.size() != n.size()) {
            return null;
        }
        Comparator<String[]> byKey = Comparator.<String[], String>comparing(r -> varKey(r))
                .thenComparing(r -> String.join("\t", r));
        List<String[]> a = new ArrayList<>(o);
        List<String[]> b = new ArrayList<>(n);
        a.sort(byKey);
        b.sort(byKey);
        boolean changed = false;
        for (int i = 0; i < a.size(); i++) {
            String[] x = a.get(i);
            String[] y = b.get(i);
            if (Arrays.equals(x, y)) {
                continue;
            }
            if (!varKey(x).equals(varKey(y)) || !"-".equals(y[l.strand()]) || !MAPPED.equals(y[l.status()])) {
                return null;
            }
            if (!y[2].equals(LiftEngine.revcompAlleles(in[2])) || !y[3].equals(LiftEngine.revcompAlleles(in[3]))) {
                return null;
            }
            changed = true;
        }
        return changed ? D3 : null;
    }

    private static String varKey(String[] r) {
        String[] k = r.clone();
        k[2] = "";
        k[3] = "";
        return String.join("\t", k);
    }

    /**
     * D7: seg/bam without -all. Old is the one old unmapped row, new the one ambiguous placeholder; the interval is
     * not a point (s1 &lt; e1), the old model has no hit, no block contains it under the new rule, and its first or
     * last base is an exact score tie.
     */
    private static boolean isEndpointTie(List<String[]> o, List<String[]> n, String[] in, Layout l,
                                         List<ChainBlock> blocks) {
        if (o.size() != 1 || n.size() != 1 || !Arrays.equals(o.get(0), oldUnmapped(in, l, false))
                || !Arrays.equals(n.get(0), placeholder(in, l, AMBIGUOUS, false))) {
            return false;
        }
        int a = Integer.parseInt(in[1]);
        int s1 = l.mode().equals("seg") ? a + 1 : a;
        int e1 = Integer.parseInt(in[2]);
        if (s1 >= e1 || !oldHits(in, l, blocks).isEmpty()) {
            return false;
        }
        NaiveLift nl = new NaiveLift(blocks);
        return nl.intervalHits(s1, e1).isEmpty()
                && (NaiveLift.AMBIGUOUS.equals(nl.point(s1).status())
                || NaiveLift.AMBIGUOUS.equals(nl.point(e1).status()));
    }

    /**
     * D8: a 1-base bam read (Pos == End). Old equals the old model (strict tStart &lt; Pos &lt; tEnd), new equals
     * the new model (N15, tStart &lt; Pos &lt;= tEnd; best or ambiguous without -all, scored rows with -all), at
     * least one block is in the new set only, and every such block has tEnd == Pos.
     */
    private static boolean isLastBase(List<String[]> o, List<String[]> n, String[] in, Layout l, boolean all,
                                      List<ChainBlock> blocks) {
        int pos = Integer.parseInt(in[1]);
        if (pos != Integer.parseInt(in[2])) {
            return false;
        }
        int added = 0;
        for (ChainBlock b : blocks) {
            boolean inNew = b.tStart() < pos && pos <= b.tEnd();
            boolean inOld = b.tStart() < pos && pos < b.tEnd();
            if (inNew && !inOld) {
                // inNew && !inOld means pos == tEnd: every block only the new rule adds ends at Pos
                added++;
            }
        }
        return added > 0 && sameRows(oldModelRows(in, l, all, blocks), o)
                && sameRows(newModelBamRows(in, l, all, blocks), n);
    }

    // ---- bam pairing: D5, D4, D6, D9 ----

    /**
     * D5, else D4/D6/D9 by pairing old and new rows, and the new rows must equal the naive new model. Returns SAME
     * when every pair is equal, null when unexplained.
     */
    private static String bam(List<String[]> o, List<String[]> n, String[] in, Layout l, boolean all,
                              List<ChainBlock> blocks) {
        String c = bamPairs(o, n, in, l, all, blocks);
        if (c == null || SAME.equals(c)) {
            return c;
        }
        return sameRows(newModelBamRows(in, l, all, blocks), n) ? c : null;
    }

    private static String bamPairs(List<String[]> o, List<String[]> n, String[] in, Layout l, boolean all,
                                   List<ChainBlock> blocks) {
        int pos = Integer.parseInt(in[1]);
        int end = Integer.parseInt(in[2]);
        List<String[]> kept = new ArrayList<>();
        int dropped = 0;
        for (String[] r : o) {
            if (MAPPED.equals(r[l.status()])) {
                List<ChainBlock> via = oldBlocks(r, pos, end, l, all, blocks);
                if (via.isEmpty()) {
                    return null; // an old mapped row the chain does not explain
                }
                if (via.stream().allMatch(b -> b.tStart() == pos)) {
                    dropped++;
                    continue;
                }
            }
            kept.add(r);
        }
        if (dropped == 0) {
            return pairBam(o, n, in, l, all, blocks);
        }
        if (!all) {
            // the new row is the model's; every old row not dropped must pair with it
            String[] expected = expectedNewBamRow(in, l, blocks);
            if (expected == null || n.size() != 1 || !Arrays.equals(n.get(0), expected)) {
                return null;
            }
            return kept.isEmpty() || pairBam(kept, n, in, l, false, blocks) != null ? D5 : null;
        }
        if (kept.isEmpty()) {
            return n.size() == 1 && Arrays.equals(n.get(0), placeholder(in, l, UNMAPPED, true)) ? D5 : null;
        }
        return pairBam(kept, n, in, l, true, blocks) != null ? D5 : null;
    }

    /**
     * Pairs old and new bam rows (sorted by everything but target Pos, End and MPOS, then target Pos). A pair is
     * equal, D4 ('-' and mapped, Pos and End + 1), D6 (mapped, only MPOS differs; for both, old MPOS is the old
     * shift and new MPOS follows N15)
     * or D9 (unmapped/ambiguous, only MPOS differs, old the {@code MPOS - Pos} shift, new 0). Returns D4 if any pair
     * is D4, else D6, else D9, else SAME; null when a pair is unexplained.
     */
    private static String pairBam(List<String[]> o, List<String[]> n, String[] in, Layout l, boolean all,
                                  List<ChainBlock> blocks) {
        if (o.size() != n.size()) {
            return null;
        }
        Comparator<String[]> cmp = Comparator.<String[], String>comparing(r -> bamKey(r))
                .thenComparingInt(r -> Integer.parseInt(r[1]))
                .thenComparing(r -> String.join("\t", r));
        List<String[]> a = new ArrayList<>(o);
        List<String[]> b = new ArrayList<>(n);
        a.sort(cmp);
        b.sort(cmp);
        boolean d4 = false;
        boolean d6 = false;
        boolean d9 = false;
        for (int i = 0; i < a.size(); i++) {
            String[] x = a.get(i);
            String[] y = b.get(i);
            if (Arrays.equals(x, y)) {
                continue;
            }
            if (!bamKey(x).equals(bamKey(y))) {
                return null;
            }
            boolean samePlace = x[1].equals(y[1]) && x[2].equals(y[2]);
            if (!MAPPED.equals(y[l.status()])) {
                if (!samePlace || !x[MPOS].equals(oldMpos(in, 0)) || !"0".equals(y[MPOS])) {
                    return null;
                }
                d9 = true;
                continue;
            }
            boolean shifted = !samePlace && "-".equals(y[l.strand()])
                    && Integer.parseInt(y[1]) == Integer.parseInt(x[1]) + 1
                    && Integer.parseInt(y[2]) == Integer.parseInt(x[2]) + 1;
            if (!samePlace && !shifted) {
                return null;
            }
            // old MPOS is the old shift, new MPOS follows N15
            if (!x[MPOS].equals(oldMpos(in, Integer.parseInt(x[1])))
                    || !y[MPOS].equals(expectedMpos(in, y, l, all, blocks))) {
                return null;
            }
            if (shifted) {
                d4 = true;
            } else {
                d6 = true;
            }
        }
        return d4 ? D4 : d6 ? D6 : d9 ? D9 : SAME;
    }

    private static String bamKey(String[] r) {
        String[] k = r.clone();
        k[1] = "";
        k[2] = "";
        k[MPOS] = "";
        return String.join("\t", k);
    }

    private static boolean plus(ChainBlock b) {
        return "+".equals(b.strand());
    }

    private static boolean rowMatches(ChainBlock b, String[] r, Layout l, boolean all, int tPos, int tEnd) {
        return b.qChrom().equals(r[0]) && b.strand().equals(r[l.strand()])
                && Integer.parseInt(r[1]) == tPos && Integer.parseInt(r[2]) == tEnd
                && (!all || Double.parseDouble(r[l.score()]) == b.score());
    }

    /** Blocks an old mapped bam row can have come through: {@code tStart <= Pos}, {@code End <= tEnd}. */
    private static List<ChainBlock> oldBlocks(String[] r, int pos, int end, Layout l, boolean all,
                                              List<ChainBlock> blocks) {
        List<ChainBlock> via = new ArrayList<>();
        for (ChainBlock b : blocks) {
            if (pos >= b.tStart() && end <= b.tEnd()) {
                int s = plus(b) ? b.qStart() + pos - b.tStart() : b.qStart() + b.tEnd() - end;
                int e = plus(b) ? b.qStart() + end - b.tStart() : b.qStart() + b.tEnd() - pos;
                if (rowMatches(b, r, l, all, s, e)) {
                    via.add(b);
                }
            }
        }
        return via;
    }

    /** New-rule (1-based inclusive, N15) target of Pos..End through b. */
    private static int[] newTarget(ChainBlock b, int pos, int end) {
        return plus(b)
                ? new int[]{b.qStart() + pos - b.tStart(), b.qStart() + end - b.tStart()}
                : new int[]{b.qStart() + b.tEnd() - end + 1, b.qStart() + b.tEnd() - pos + 1};
    }

    /** Blocks a new mapped bam row can have come through: {@code tStart <= Pos - 1}, {@code End <= tEnd}. */
    private static List<ChainBlock> newBlocks(String[] r, int pos, int end, Layout l, boolean all,
                                              List<ChainBlock> blocks) {
        List<ChainBlock> via = new ArrayList<>();
        for (ChainBlock b : blocks) {
            if (b.tStart() <= pos - 1 && end <= b.tEnd()) {
                int[] t = newTarget(b, pos, end);
                if (rowMatches(b, r, l, all, t[0], t[1])) {
                    via.add(b);
                }
            }
        }
        return via;
    }

    /**
     * N15 MPOS of a new bam row: 0 for a row that does not map; otherwise MPOS lifted as a point through the row's
     * block when it contains MPOS, else through the best block, kept only on the row's target chrom, else 0.
     * null when it cannot be decided (MPOS not an integer &gt;= 0, or no unique row block).
     */
    private static String expectedMpos(String[] in, String[] row, Layout l, boolean all, List<ChainBlock> blocks) {
        if (!MAPPED.equals(row[l.status()])) {
            return "0";
        }
        int m;
        try {
            m = Integer.parseInt(in[MPOS]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (m < 0) {
            return null;
        }
        if (m == 0) {
            return "0";
        }
        Set<String> values = new HashSet<>();
        for (ChainBlock b : newBlocks(row, Integer.parseInt(in[1]), Integer.parseInt(in[2]), l, all, blocks)) {
            values.add(mposVia(b, m, row[0], blocks));
        }
        return values.size() == 1 ? values.iterator().next() : null;
    }

    private static String mposVia(ChainBlock b, int m, String targetChrom, List<ChainBlock> blocks) {
        if (b.tStart() < m && m <= b.tEnd()) {
            return String.valueOf(plus(b) ? b.qStart() + m - b.tStart() : b.qStart() + b.tEnd() - m + 1);
        }
        NaiveLift.Result r = new NaiveLift(blocks).point(m);
        if (NaiveLift.MAPPED.equals(r.status()) && r.hit().chrom().equals(targetChrom)) {
            return String.valueOf(r.hit().end0());
        }
        return "0";
    }

    // ---- new model for bam (N8, N12, N14, N15) ----

    /** The naive model's new bam row without -all, or null when it cannot be decided. */
    private static String[] expectedNewBamRow(String[] in, Layout l, List<ChainBlock> blocks) {
        NaiveLift.Result r = new NaiveLift(blocks).span(Integer.parseInt(in[1]), Integer.parseInt(in[2]));
        if (NaiveLift.AMBIGUOUS.equals(r.status())) {
            return placeholder(in, l, AMBIGUOUS, false);
        }
        if (!NaiveLift.MAPPED.equals(r.status())) {
            return placeholder(in, l, UNMAPPED, false);
        }
        return bamRowFromHit(in, l, r.hit(), false, blocks);
    }

    /** The naive model's new bam rows: the one row without -all; with -all one per containing block, or unmapped. */
    private static List<String[]> newModelBamRows(String[] in, Layout l, boolean all, List<ChainBlock> blocks) {
        List<String[]> rows = new ArrayList<>();
        if (!all) {
            rows.add(expectedNewBamRow(in, l, blocks));
            return rows;
        }
        List<NaiveLift.Hit> hits = new NaiveLift(blocks).intervalHits(Integer.parseInt(in[1]),
                Integer.parseInt(in[2]));
        if (hits.isEmpty()) {
            rows.add(placeholder(in, l, UNMAPPED, true));
        }
        for (NaiveLift.Hit h : hits) {
            rows.add(bamRowFromHit(in, l, h, true, blocks));
        }
        return rows;
    }

    /** The new bam row for a NaiveLift hit: target, - strand read transforms, N15 MPOS, score under -all. */
    private static String[] bamRowFromHit(String[] in, Layout l, NaiveLift.Hit h, boolean all,
                                          List<ChainBlock> blocks) {
        boolean minus = "-".equals(h.strand());
        List<String> row = new ArrayList<>(List.of(h.chrom(), String.valueOf(h.start0() + 1),
                String.valueOf(h.end0())));
        for (int k = l.firstInputCol(); k < in.length; k++) {
            row.add(bamColumn(in, k, minus, ""));
        }
        row.addAll(List.of(in[0], in[1], in[2], h.strand(), MAPPED));
        if (all) {
            row.add(String.valueOf(h.score()));
        }
        String[] r = row.toArray(new String[0]);
        String mpos = expectedMpos(in, r, l, all, blocks);
        if (mpos == null) {
            return null;
        }
        r[MPOS] = mpos;
        return r;
    }

    // ------------------------------------------------------------------------------------------------
    // Classifier unit checks on hand-made chains
    // ------------------------------------------------------------------------------------------------

    // chr1 [100,200) -> chr1 1000 +, [200,300) -> chr3 5000 -
    private static final Map<String, List<ChainBlock>> HAND_CHAIN = Map.of("chr1", List.of(
            new ChainBlock(100, 200, "chr1", 1000, 1100, "+", 1000),
            new ChainBlock(200, 300, "chr3", 5000, 5100, "-", 1000)));
    // chr1 [100,200) -> chr1 1000 + and chr3 3000 +, an exact tie
    private static final Map<String, List<ChainBlock>> TIE_CHAIN = Map.of("chr1", List.of(
            new ChainBlock(100, 200, "chr1", 1000, 1100, "+", 1000),
            new ChainBlock(100, 200, "chr3", 3000, 3100, "+", 1000)));
    // chr1 [100,200) -> chr1 1000 + and chr3 3000 + (an exact tie), [200,300) -> chr3 5000 -
    private static final Map<String, List<ChainBlock>> TIE_OVL_CHAIN = Map.of("chr1", List.of(
            new ChainBlock(100, 200, "chr1", 1000, 1100, "+", 1000),
            new ChainBlock(100, 200, "chr3", 3000, 3100, "+", 1000),
            new ChainBlock(200, 300, "chr3", 5000, 5100, "-", 1000)));
    // chr1 [100,200) -> chr1 1000 + and [150,200) -> chr3 3000 +, equal scores: both end at 200
    private static final Map<String, List<ChainBlock>> TIE_END_CHAIN = Map.of("chr1", List.of(
            new ChainBlock(100, 200, "chr1", 1000, 1100, "+", 1000),
            new ChainBlock(150, 200, "chr3", 3000, 3050, "+", 1000)));
    // chr1 A [100,300) -> chr1 1000 + score 900 contains 200 strictly; B [100,200) -> chr3 3000 + score 1000 ends at 200
    private static final Map<String, List<ChainBlock>> MIXED_CHAIN = Map.of("chr1", List.of(
            new ChainBlock(100, 300, "chr1", 1000, 1200, "+", 900),
            new ChainBlock(100, 200, "chr3", 3000, 3100, "+", 1000)));
    // chr1 [200,300) -> chr3 5000 - and K [190,260) -> chr1 7000 +, equal scores
    private static final Map<String, List<ChainBlock>> KEPT_CHAIN = Map.of("chr1", List.of(
            new ChainBlock(190, 260, "chr1", 7000, 7070, "+", 1000),
            new ChainBlock(200, 300, "chr3", 5000, 5100, "-", 1000)));
    // chr1 P [100,200) -> chr1 1000 + score 1000, M [100,300) -> chr3 5000 - score 900
    private static final Map<String, List<ChainBlock>> MULTI_CHAIN = Map.of("chr1", List.of(
            new ChainBlock(100, 200, "chr1", 1000, 1100, "+", 1000),
            new ChainBlock(100, 300, "chr3", 5000, 5200, "-", 900)));

    /** Drives classify directly: per category positive examples and near misses that must be null. */
    @Test
    public void classifyUnitTests() {
        // same
        String snp = "chr1\t150\tA\tG\tr1";
        String snpRow = "chr1\t1050\tA\tG\tr1\tchr1\t150\t+\tmapped";
        Assert.assertEquals(SAME, classify(List.of(snpRow), List.of(snpRow), snp, "snp"));
        Assert.assertNull("moved snp", classify(List.of(snpRow),
                List.of("chr1\t1051\tA\tG\tr1\tchr1\t150\t+\tmapped"), snp, "snp", HAND_CHAIN));

        // D1
        String dup = "chr2\t5000\tA\tG\tdup1";
        String dupRow = "chr2\t0\tA\tG\tdup1\tchr2\t5000\t\tunmapped";
        Assert.assertEquals(D1, classify(List.of(dupRow), List.of(dupRow, dupRow, dupRow), dup, "snp", HAND_CHAIN));
        Assert.assertNull("dup run twice", classify(List.of(dupRow), List.of(dupRow, dupRow), dup, "snp",
                HAND_CHAIN));

        // D2
        List<String> tieOld = List.of("chr1\t1050\tA\tG\tr1\tchr1\t150\t+\tmapped",
                "chr3\t3050\tA\tG\tr1\tchr1\t150\t+\tmapped");
        List<String> tieNew = List.of("chr1\t0\tA\tG\tr1\tchr1\t150\t\tambiguous");
        Assert.assertEquals(D2, classify(tieOld, tieNew, snp, "snp", TIE_CHAIN));
        Assert.assertNull("no tie in the chain", classify(tieOld, tieNew, snp, "snp", HAND_CHAIN));
        Assert.assertNull("tie becomes unmapped", classify(tieOld,
                List.of("chr1\t0\tA\tG\tr1\tchr1\t150\t\tunmapped"), snp, "snp", TIE_CHAIN));
        Assert.assertNull("old rows are not the tied blocks' rows", classify(
                List.of("chr1\t1050\tA\tG\tr1\tchr1\t150\t+\tmapped", "chr3\t3051\tA\tG\tr1\tchr1\t150\t+\tmapped"),
                tieNew, snp, "snp", TIE_CHAIN));
        Assert.assertNull("D2 needs the chain", classify(tieOld, tieNew, snp, "snp"));

        // D3
        String var = "chr1\t250\tA\tA,TC\tr1";
        String varOld = "chr3\t5051\tT\tGA,T\tr1\tchr1\t250\t-\tmapped";
        Assert.assertEquals(D3, classify(List.of(varOld), List.of("chr3\t5051\tT\tT,GA\tr1\tchr1\t250\t-\tmapped"),
                var, "var", HAND_CHAIN));
        Assert.assertNull("ALT not per allele", classify(List.of(varOld),
                List.of("chr3\t5051\tT\tTC,A\tr1\tchr1\t250\t-\tmapped"), var, "var", HAND_CHAIN));

        // D4: - strand read 250..259, MPOS 255 inside the block: old 5041..5050, new 5042..5051, MPOS 5046
        String minus = "chr1\t250\t259\tq1\tAACCGGTTAC\tABCDEFGHIJ\t3S7M\t255";
        String minusOld = "chr3\t5041\t5050\tq1\tGTAACCGGTT\tJIHGFEDCBA\t7M3S\t5046\tchr1\t250\t259\t-\tmapped";
        String minusNew = "chr3\t5042\t5051\tq1\tGTAACCGGTT\tJIHGFEDCBA\t7M3S\t5046\tchr1\t250\t259\t-\tmapped";
        Assert.assertEquals(D4, classify(List.of(minusOld), List.of(minusNew), minus, "bam", HAND_CHAIN));
        Assert.assertEquals(D4, classify(List.of(minusOld + "\t1000.0"), List.of(minusNew + "\t1000.0"), minus,
                "bam", HAND_CHAIN));
        Assert.assertNull("shift by 2", classify(List.of(minusOld),
                List.of("chr3\t5043\t5052\tq1\tGTAACCGGTT\tJIHGFEDCBA\t7M3S\t5046\tchr1\t250\t259\t-\tmapped"),
                minus, "bam", HAND_CHAIN));

        // D5: read 200..209 starts on the - block's tStart; new spans both blocks and is unmapped
        String start = "chr1\t200\t209\tq2\tAACCGGTTAC\tABCDEFGHIJ\t10M\t205";
        String startOld = "chr3\t5091\t5100\tq2\tGTAACCGGTT\tJIHGFEDCBA\t10M\t5096\tchr1\t200\t209\t-\tmapped";
        String startNew = "chr1\t0\t1\tq2\tAACCGGTTAC\tABCDEFGHIJ\t10M\t0\tchr1\t200\t209\t\tunmapped";
        Assert.assertEquals(D5, classify(List.of(startOld), List.of(startNew), start, "bam", HAND_CHAIN));
        Assert.assertEquals(D5, classify(List.of(startOld + "\t1000.0"), List.of(startNew + "\t0.0"), start, "bam",
                HAND_CHAIN));
        Assert.assertNull("ambiguous where the model says unmapped", classify(List.of(startOld),
                List.of("chr1\t0\t1\tq2\tAACCGGTTAC\tABCDEFGHIJ\t10M\t0\tchr1\t200\t209\t\tambiguous"),
                start, "bam", HAND_CHAIN));
        // old shows the tie of the tStart block and K; new maps through K alone, and the K row pairs as same
        String keptK = "chr1\t7010\t7019\tq2\tAACCGGTTAC\tABCDEFGHIJ\t10M\t7015\tchr1\t200\t209\t+\tmapped";
        Assert.assertEquals(D5, classify(List.of(startOld, keptK), List.of(keptK), start, "bam", KEPT_CHAIN));
        Assert.assertNull("kept old row with a wrong MPOS", classify(List.of(startOld,
                        "chr1\t7010\t7019\tq2\tAACCGGTTAC\tABCDEFGHIJ\t10M\t7016\tchr1\t200\t209\t+\tmapped"),
                List.of(keptK), start, "bam", KEPT_CHAIN));
        Assert.assertNull("an old row no block explains", classify(List.of(startOld,
                        "chr1\t1100\t1109\tq2\tAACCGGTTAC\tABCDEFGHIJ\t10M\t1105\tchr1\t200\t209\t+\tmapped"),
                List.of(startNew), start, "bam", HAND_CHAIN));

        // D6: + strand read, mate at 250 lifts to chr3, not the read's chr1: new MPOS 0, old shifted 1150
        String mate = "chr1\t150\t159\tq3\tAACCGGTTAC\tABCDEFGHIJ\t10M\t250";
        String mateOld = "chr1\t1050\t1059\tq3\tAACCGGTTAC\tABCDEFGHIJ\t10M\t1150\tchr1\t150\t159\t+\tmapped";
        String mateNew = "chr1\t1050\t1059\tq3\tAACCGGTTAC\tABCDEFGHIJ\t10M\t0\tchr1\t150\t159\t+\tmapped";
        Assert.assertEquals(D6, classify(List.of(mateOld), List.of(mateNew), mate, "bam", HAND_CHAIN));
        Assert.assertEquals(D6, classify(List.of(mateOld + "\t1000.0"), List.of(mateNew + "\t1000.0"), mate, "bam",
                HAND_CHAIN));
        Assert.assertNull("MPOS not per N15", classify(List.of(mateOld),
                List.of("chr1\t1050\t1059\tq3\tAACCGGTTAC\tABCDEFGHIJ\t10M\t7\tchr1\t150\t159\t+\tmapped"),
                mate, "bam", HAND_CHAIN));
        Assert.assertNull("D6 needs the chain", classify(List.of(mateOld), List.of(mateNew), mate, "bam"));

        // D4 and D6 on one Id under -all: P (+) changes MPOS only, M (-) shifts by one
        Assert.assertEquals(D4, classify(List.of(
                        mateOld + "\t1000.0",
                        "chr3\t5141\t5150\tq3\tGTAACCGGTT\tJIHGFEDCBA\t10M\t5241\tchr1\t150\t159\t-\tmapped\t900.0"),
                List.of(mateNew + "\t1000.0",
                        "chr3\t5142\t5151\tq3\tGTAACCGGTT\tJIHGFEDCBA\t10M\t5051\tchr1\t150\t159\t-\tmapped\t900.0"),
                mate, "bam", MULTI_CHAIN));
        Assert.assertNull("one row of the Id unexplained", classify(List.of(
                        mateOld + "\t1000.0",
                        "chr3\t5141\t5150\tq3\tGTAACCGGTT\tJIHGFEDCBA\t10M\t5241\tchr1\t150\t159\t-\tmapped\t900.0"),
                List.of(mateNew + "\t1000.0",
                        "chr3\t5142\t5151\tq3\tGTAACCGGTT\tJIHGFEDCBA\t10M\t5241\tchr1\t150\t159\t-\tmapped\t900.0"),
                mate, "bam", MULTI_CHAIN));

        // D9: an unmapped read, old MPOS 520-500 = 20, new 0
        String off = "chr1\t500\t509\tq0\tAACCGGTTAC\tABCDEFGHIJ\t10M\t520";
        String offNew = "chr1\t0\t1\tq0\tAACCGGTTAC\tABCDEFGHIJ\t10M\t0\tchr1\t500\t509\t\tunmapped";
        Assert.assertEquals(D9, classify(
                List.of("chr1\t0\t1\tq0\tAACCGGTTAC\tABCDEFGHIJ\t10M\t20\tchr1\t500\t509\t\tunmapped"),
                List.of(offNew), off, "bam", HAND_CHAIN));
        Assert.assertNull("a last-base read left unmapped is not D9", classify(
                List.of("chr1\t0\t1\tq4\tA\tI\t1M\t-50\tchr1\t200\t200\t\tunmapped"),
                List.of("chr1\t0\t1\tq4\tA\tI\t1M\t0\tchr1\t200\t200\t\tunmapped"),
                "chr1\t200\t200\tq4\tA\tI\t1M\t150", "bam", HAND_CHAIN));
        Assert.assertNull("old MPOS is not the shift", classify(
                List.of("chr1\t0\t1\tq0\tAACCGGTTAC\tABCDEFGHIJ\t10M\t21\tchr1\t500\t509\t\tunmapped"),
                List.of(offNew), off, "bam", HAND_CHAIN));

        // D7: seg [150,250) is in no block and its first base 151 is in the exact tie
        String seg = "chr1\t150\t250\tg1";
        Assert.assertEquals(D7, classify(List.of("chr1\t0\t1\tg1\tchr1\t150\t250\t\tunmapped"),
                List.of("chr1\t0\t1\tg1\tchr1\t150\t250\t\tambiguous"), seg, "seg", TIE_OVL_CHAIN));
        String bamTie = "chr1\t151\t250\tq7\tAC\tII\t2M\t160";
        Assert.assertEquals(D7, classify(List.of("chr1\t0\t1\tq7\tAC\tII\t2M\t9\tchr1\t151\t250\t\tunmapped"),
                List.of("chr1\t0\t1\tq7\tAC\tII\t2M\t0\tchr1\t151\t250\t\tambiguous"), bamTie, "bam",
                TIE_OVL_CHAIN));
        Assert.assertNull("a block contains the interval", classify(
                List.of("chr1\t0\t1\tg2\tchr1\t210\t280\t\tunmapped"),
                List.of("chr1\t0\t1\tg2\tchr1\t210\t280\t\tambiguous"), "chr1\t210\t280\tg2", "seg",
                TIE_OVL_CHAIN));
        Assert.assertNull("no tie at either end", classify(
                List.of("chr1\t0\t1\tg3\tchr1\t250\t350\t\tunmapped"),
                List.of("chr1\t0\t1\tg3\tchr1\t250\t350\t\tambiguous"), "chr1\t250\t350\tg3", "seg",
                TIE_OVL_CHAIN));
        Assert.assertNull("ambiguous bam row keeps MPOS", classify(
                List.of("chr1\t0\t1\tq7\tAC\tII\t2M\t9\tchr1\t151\t250\t\tunmapped"),
                List.of("chr1\t0\t1\tq7\tAC\tII\t2M\t9\tchr1\t151\t250\t\tambiguous"), bamTie, "bam",
                TIE_OVL_CHAIN));

        // D8: 1-base read on the + block's last base 200; old unmapped (MPOS 150-200), new 1100..1100, MPOS 1050
        String last = "chr1\t200\t200\tq4\tA\tI\t1M\t150";
        String lastOld = "chr1\t0\t1\tq4\tA\tI\t1M\t-50\tchr1\t200\t200\t\tunmapped";
        Assert.assertEquals(D8, classify(List.of(lastOld),
                List.of("chr1\t1100\t1100\tq4\tA\tI\t1M\t1050\tchr1\t200\t200\t+\tmapped"), last, "bam", HAND_CHAIN));
        // the same on the - block's last base 300 under -all: 5001..5001, SEQ revcomp, MPOS 250 -> 5051
        String lastMinus = "chr1\t300\t300\tq5\tA\tI\t1M\t250";
        Assert.assertEquals(D8, classify(
                List.of("chr1\t0\t1\tq5\tA\tI\t1M\t-50\tchr1\t300\t300\t\tunmapped\t0.0"),
                List.of("chr3\t5001\t5001\tq5\tT\tI\t1M\t5051\tchr1\t300\t300\t-\tmapped\t1000.0"), lastMinus,
                "bam", HAND_CHAIN));
        // on a tie: both blocks end at 200 with equal scores, so new is the one ambiguous placeholder
        String tiedLast = "chr1\t200\t200\tq8\tA\tI\t1M\t150";
        Assert.assertEquals(D8, classify(List.of("chr1\t0\t1\tq8\tA\tI\t1M\t-50\tchr1\t200\t200\t\tunmapped"),
                List.of("chr1\t0\t1\tq8\tA\tI\t1M\t0\tchr1\t200\t200\t\tambiguous"), tiedLast, "bam", TIE_END_CHAIN));
        // mixed: A contains 200 strictly (old maps through it), B ends at 200 and wins without -all
        String mixed = "chr1\t200\t200\tq10\tA\tI\t1M\t150";
        String mixedOldA = "chr1\t1100\t1100\tq10\tA\tI\t1M\t1050\tchr1\t200\t200\t+\tmapped";
        Assert.assertEquals(D8, classify(List.of(mixedOldA),
                List.of("chr3\t3100\t3100\tq10\tA\tI\t1M\t3050\tchr1\t200\t200\t+\tmapped"), mixed, "bam",
                MIXED_CHAIN));
        Assert.assertEquals(D8, classify(List.of(mixedOldA + "\t900.0"),
                List.of(mixedOldA + "\t900.0", "chr3\t3100\t3100\tq10\tA\tI\t1M\t3050\tchr1\t200\t200\t+\tmapped\t1000.0"),
                mixed, "bam", MIXED_CHAIN));
        Assert.assertNull("an added row through a block not ending at Pos", classify(List.of(mixedOldA + "\t900.0"),
                List.of(mixedOldA + "\t900.0", "chr3\t3100\t3100\tq10\tA\tI\t1M\t3050\tchr1\t200\t200\t+\tmapped\t1000.0",
                        "chr1\t1100\t1100\tq10\tA\tI\t1M\t1050\tchr1\t200\t200\t+\tmapped\t900.0"),
                mixed, "bam", MIXED_CHAIN));
        Assert.assertNull("read on tEnd - 1", classify(
                List.of("chr1\t0\t1\tq6\tA\tI\t1M\t-49\tchr1\t199\t199\t\tunmapped"),
                List.of("chr1\t1099\t1099\tq6\tA\tI\t1M\t1050\tchr1\t199\t199\t+\tmapped"),
                "chr1\t199\t199\tq6\tA\tI\t1M\t150", "bam", HAND_CHAIN));
        Assert.assertNull("tied 1-base read not on tEnd", classify(
                List.of("chr1\t0\t1\tq9\tA\tI\t1M\t-30\tchr1\t180\t180\t\tunmapped"),
                List.of("chr1\t0\t1\tq9\tA\tI\t1M\t0\tchr1\t180\t180\t\tambiguous"),
                "chr1\t180\t180\tq9\tA\tI\t1M\t150", "bam", TIE_END_CHAIN));
        Assert.assertNull("tied 1-base read on tEnd keeps MPOS", classify(
                List.of("chr1\t0\t1\tq8\tA\tI\t1M\t-50\tchr1\t200\t200\t\tunmapped"),
                List.of("chr1\t0\t1\tq8\tA\tI\t1M\t-50\tchr1\t200\t200\t\tambiguous"), tiedLast, "bam", TIE_END_CHAIN));
        Assert.assertNull("wrong target", classify(List.of(lastOld),
                List.of("chr1\t1101\t1101\tq4\tA\tI\t1M\t1050\tchr1\t200\t200\t+\tmapped"), last, "bam", HAND_CHAIN));
        Assert.assertNull("- strand read not transformed", classify(
                List.of("chr1\t0\t1\tq5\tA\tI\t1M\t-50\tchr1\t300\t300\t\tunmapped\t0.0"),
                List.of("chr3\t5001\t5001\tq5\tA\tI\t1M\t5051\tchr1\t300\t300\t-\tmapped\t1000.0"), lastMinus,
                "bam", HAND_CHAIN));
    }

    // ------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------

    private static Map<String, List<String>> group(String[] lines, String idName) {
        int id = column(lines[0], idName);
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            m.computeIfAbsent(lines[i].split("\t", -1)[id], k -> new ArrayList<>()).add(lines[i]);
        }
        return m;
    }

    /** Index of the column named name (case-insensitive), or ending with name when it starts with '_'. */
    private static int column(String header, String name) {
        String[] cols = header.split("\t", -1);
        for (int i = 0; i < cols.length; i++) {
            if (name.startsWith("_") ? cols[i].toLowerCase().endsWith(name.toLowerCase())
                    : cols[i].equalsIgnoreCase(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("no column " + name + " in " + header);
    }

    /** The first line of a gor file. */
    private static String header(Path file) {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return r.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
