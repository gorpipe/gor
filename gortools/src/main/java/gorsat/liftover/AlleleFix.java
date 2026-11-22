package gorsat.liftover;

import gorsat.parser.ParseUtilities;

import java.util.Locale;

/**
 * LIFTOVER -var -haplotype (ENGKNOW-4224): rewrites the alleles of an indel that sits next to a build difference (a
 * chain gap) so that the target reference plus the lifted variant is the source haplotype, as BCFtools
 * {@code +liftover} does. A plain lift keeps the source alleles, which then describe a different haplotype on the
 * target build.
 * <p>
 * The steps follow BCFtools +liftover 1.24 ({@code liftover.c}), with its defaults ({@code --max-indel-inc 250},
 * {@code --max-snp-gap 1}), so the rewritten alleles are the ones BCFtools writes:
 * <ol>
 * <li>extend the alleles in the source until the indel is unambiguous ({@code extend_alleles});</li>
 * <li>lift the first and the last base of the extended REF, and when one of them falls in a chain gap, pad to the
 * next block of the chain ({@code liftover_indel}) and realign the pad against the target with Needleman-Wunsch
 * ({@code clip_pad});</li>
 * <li>read the target reference over the lifted span; when it is neither the source REF nor the ALT the target
 * reference is added as the new REF ({@code find_reference}, SWAP=-1);</li>
 * <li>left-align with the new REF, the source REF and the ALT as the alleles, unless they already are
 * ({@code is_left_aligned}, {@code trim_right}, {@code trim_left}, {@code shift_left}).</li>
 * </ol>
 * The rewritten row carries the new REF and the ALT; the source REF, which BCFtools keeps as a second ALT, is
 * dropped. Only indels (REF and ALT of different length, single ALT, plain bases) whose extended span crosses a chain
 * gap are rewritten; everything else keeps the plain lift.
 */
public final class AlleleFix {

    /** No build difference in the indel's window, or not an indel: the plain lift stands. */
    public static final String NONE = "none";
    /** The alleles were rewritten across a build difference. */
    public static final String REWRITTEN = "rewritten";
    /** The target reference over the window is the ALT (BCFtools SWAP=1); the alleles are not rewritten. */
    public static final String SWAP = "swap";
    /** REF is not the source reference at the variant; the alleles are not rewritten. */
    public static final String REF_MISMATCH = "ref_mismatch";
    /** A rewrite was needed but could not be made (BCFtools would reject the row); the plain lift stands. */
    public static final String UNRESOLVED = "unresolved";

    /** BCFtools --max-indel-inc: how far an indel's anchors may move or be padded. */
    static final int MAX_INDEL_INC = 250;
    /** BCFtools --max-snp-gap: equal double-sided gaps up to this size are part of the block. */
    static final int MAX_SNP_GAP = 1;
    /** The longest allele extension into a repeat; a longer one (e.g. an N run) is unresolved. */
    static final int MAX_EXTENSION = 10_000;
    /**
     * The largest pad realignment (cells of one Needleman-Wunsch matrix, about 2000 x 2000 bases); a larger one, which
     * only very long indels next to a build indel need, is unresolved rather than risking the heap.
     */
    static final long MAX_NW_CELLS = 4_000_000L;

    /** Reference bases: 1-based inclusive start..end on chrom, 'N' outside the sequence. */
    @FunctionalInterface
    public interface Bases {
        String get(String chrom, int start1, int end1);
    }

    /** The outcome: the fix and, for {@link #REWRITTEN}, the target location and alleles; otherwise nulls and 0. */
    public record Result(String fix, String chrom, int pos, String ref, String alt) {
        static Result of(String fix) {
            return new Result(fix, null, 0, null, null);
        }
    }

    private AlleleFix() {
    }

    /**
     * The allele fix of the -var row chrom:pos ref&gt;alt, which the plain lift mapped through ci. src and tgt read the
     * source and the target reference.
     */
    public static Result rewrite(ContigIndex ci, String chrom, int pos, String ref, String alt, Bases src,
                                 Bases tgt) {
        String r = ref.toUpperCase(Locale.ROOT);
        String a = alt.toUpperCase(Locale.ROOT);
        if (r.length() == a.length()) {
            return Result.of(NONE);
        }
        if (!plainBases(r) || !plainBases(a)) {
            return Result.of(UNRESOLVED);
        }
        Seq s = new Seq(src, chrom);
        if (!s.range(pos, pos + r.length() - 1).equals(r)) {
            return Result.of(REF_MISMATCH);
        }
        try {
            return new Rewrite(ci, chrom, pos - 1, r, a, s, tgt).run();
        } catch (Unresolved e) {
            return Result.of(UNRESOLVED);
        }
    }

    private static boolean plainBases(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != 'A' && c != 'C' && c != 'G' && c != 'T' && c != 'N') {
                return false;
            }
        }
        return true;
    }

    /** A rewrite that cannot be made. */
    private static final class Unresolved extends RuntimeException {
        Unresolved() {
            super(null, null, false, false);
        }
    }

    /** Uppercase reference bases of one contig, read through a small window cache. */
    private static final class Seq {
        private static final int WINDOW = 256;
        private final Bases bases;
        private final String chrom;
        private int cacheStart = 1;
        private String cache = "";

        Seq(Bases bases, String chrom) {
            this.bases = bases;
            this.chrom = chrom;
        }

        /** Base p (1-based); 'N' before the contig start. */
        char at(int p) {
            if (p < 1) {
                return 'N';
            }
            if (p < cacheStart || p >= cacheStart + cache.length()) {
                cacheStart = Math.max(1, p - WINDOW / 2);
                cache = bases.get(chrom, cacheStart, cacheStart + WINDOW - 1).toUpperCase(Locale.ROOT);
            }
            int k = p - cacheStart;
            return k < cache.length() ? cache.charAt(k) : 'N';
        }

        /** Bases a..b (1-based inclusive); empty when b &lt; a. */
        String range(int a, int b) {
            StringBuilder sb = new StringBuilder(Math.max(0, b - a + 1));
            for (int p = a; p <= b; p++) {
                sb.append(at(p));
            }
            return sb.toString();
        }
    }

    /** A lifted anchor base: the block it lifted through and its 1-based forward-strand target position. */
    private record Anchor(int block, String chrom, int pos, boolean minus) {
    }

    /** The lifted span of the extended alleles (liftover_indel), 1-based inclusive. */
    private record Span(String chrom, int dst5, int dst3, boolean minus, int npad, boolean sameRun) {
    }

    private static final class Rewrite {
        private final ContigIndex ci;
        private final Seq src;
        private final Bases tgtBases;
        /** 0-based position of the first allele base (BCFtools rec->pos). */
        private int pos0;
        private StringBuilder ref;
        private StringBuilder alt;

        Rewrite(ContigIndex ci, String chrom, int pos0, String ref, String alt, Seq src, Bases tgt) {
            this.ci = ci;
            this.src = src;
            this.tgtBases = tgt;
            this.pos0 = pos0;
            this.ref = new StringBuilder(ref);
            this.alt = new StringBuilder(alt);
        }

        Result run() {
            if (extensionNeeded(ref, alt)) {
                extend();
            }
            int srcPos5 = pos0 + 1;
            int srcPos3 = pos0 + ref.length();
            Span span = liftIndel(srcPos5, srcPos3);
            if (span.npad() == 0 && span.sameRun() && span.dst3() - span.dst5() == srcPos3 - srcPos5) {
                // the extended alleles lie in one gap-free alignment: no build indel to carry
                return Result.of(NONE);
            }
            int npad = span.npad();
            String pad = "";
            if (npad != 0) {
                int a = npad < 0 ? srcPos5 + npad : srcPos3 + 1;
                int b = npad < 0 ? srcPos5 - 1 : srcPos3 + npad;
                pad = src.range(a, b);
            }
            String r0 = ref.toString();
            String a0 = alt.toString();
            if (span.minus()) {
                r0 = revcomp(r0);
                a0 = revcomp(a0);
                pad = revcomp(pad);
                npad = -npad;
            }
            Seq tgt = new Seq(tgtBases, span.chrom());
            int dst5 = span.dst5();
            int dst3 = span.dst3();
            if (npad != 0 && dst3 > dst5) {
                int shift = clipShift(new String[]{r0, a0}, tgt.range(dst5, dst3), pad, npad);
                if (npad < 0) {
                    dst5 += shift;
                } else {
                    dst3 += shift;
                }
                if (dst5 > dst3 || dst5 < 1) {
                    throw new Unresolved();
                }
            }
            String tref = tgt.range(dst5, dst3);
            if (tref.chars().allMatch(c -> c == 'N')) {
                // no target reference here (or the wrong path): nothing to rewrite against
                throw new Unresolved();
            }
            StringBuilder[] als = {new StringBuilder(r0), new StringBuilder(a0)};
            int swap = findReference(als, tref);
            if (swap == 0) {
                return Result.of(NONE);
            }
            if (swap == 1) {
                return Result.of(SWAP);
            }
            StringBuilder[] three = {new StringBuilder(tref), als[0], als[1]};
            int p0 = dst5 - 1;
            if (!isLeftAligned(three)) {
                p0 = leftAlign(three, p0, tgt);
            }
            return new Result(REWRITTEN, span.chrom(), p0 + 1, three[0].toString(), three[2].toString());
        }

        // ---- source extension (extend_alleles) ----

        private static boolean extensionNeeded(CharSequence a, CharSequence b) {
            if (a.charAt(0) != b.charAt(0) || a.charAt(a.length() - 1) != b.charAt(b.length() - 1)) {
                return true;
            }
            if (a.length() == b.length()) {
                return false;
            }
            String l = (a.length() > b.length() ? a : b).toString();
            String s = (a.length() > b.length() ? b : a).toString();
            return l.startsWith(s) || l.endsWith(s);
        }

        private void extend() {
            if (ref.charAt(0) != alt.charAt(0)) {
                padLeft(1);
            }
            if (ref.charAt(ref.length() - 1) != alt.charAt(alt.length() - 1)) {
                padRight(1);
            }
            if (ref.length() == alt.length()) {
                return;
            }
            StringBuilder l = ref.length() > alt.length() ? ref : alt;
            StringBuilder s = ref.length() > alt.length() ? alt : ref;
            int d = l.length() - s.length();
            if (l.substring(0, s.length()).equals(s.toString())) {
                padFromRight(l.substring(s.length()), d);
            }
            if (l.substring(l.length() - s.length()).equals(s.toString())) {
                padFromLeft(l.toString(), s.length(), d);
            }
        }

        private void padLeft(int n) {
            if (pos0 - n < 0) {
                throw new Unresolved();
            }
            String p = src.range(pos0 - n + 1, pos0);
            ref.insert(0, p);
            alt.insert(0, p);
            pos0 -= n;
        }

        private void padRight(int n) {
            int from = pos0 + ref.length() + 1;
            String p = src.range(from, from + n - 1);
            ref.append(p);
            alt.append(p);
        }

        /**
         * Appends the reference after REF up to and including the first base where it stops repeating: the extra
         * sequence e of the longer allele, then the reference after REF again.
         */
        private void padFromRight(String e, int d) {
            int end1 = pos0 + ref.length() + 1;
            int k = 0;
            while (true) {
                char r = src.at(end1 + k);
                char s = k < d ? e.charAt(k) : src.at(end1 + k - d);
                if (r != s) {
                    break;
                }
                if (++k > MAX_EXTENSION) {
                    throw new Unresolved();
                }
            }
            String p = src.range(end1, end1 + k);
            ref.append(p);
            alt.append(p);
        }

        /** The mirror of {@link #padFromRight}: prepends the reference before REF. */
        private void padFromLeft(String l, int sLen, int d) {
            int k = 0;
            while (true) {
                char r = src.at(pos0 - k);
                char s = k < d ? l.charAt(l.length() - sLen - 1 - k) : src.at(pos0 - (k - d));
                if (r != s) {
                    break;
                }
                if (++k > MAX_EXTENSION || pos0 - k < 1) {
                    throw new Unresolved();
                }
            }
            String p = src.range(pos0 - k, pos0);
            ref.insert(0, p);
            alt.insert(0, p);
            pos0 -= k + 1;
        }

        // ---- lifting the anchors (liftover_bp, liftover_indel) ----

        /** Lifts the 1-based source base p, or null when no block (or a merged gap of at most 1 base) holds it. */
        private Anchor anchor(int p) {
            int i = ci.findPoint(p);
            if (i < 0) {
                return null;
            }
            if (ci.isGap(i) && ci.tEnd(i) - ci.tStart(i) > MAX_SNP_GAP) {
                return null;
            }
            LiftEngine.Target t = LiftEngine.liftAt(ci, i, p - 1, p);
            return new Anchor(i, t.chrom(), t.start0() + 1, ci.minus(i));
        }

        private boolean sameChain(int i, int j) {
            return ci.qChrom(i).equals(ci.qChrom(j)) && ci.minus(i) == ci.minus(j) && ci.score(i) == ci.score(j);
        }

        /** The 0-based half-open target end of block i. */
        private int qEnd(int i) {
            return ci.qStart(i) + ci.tEnd(i) - ci.tStart(i);
        }

        /** An equal double-sided gap of at most MAX_SNP_GAP between blocks a (before) and b of one chain. */
        private boolean merged(int a, int b) {
            int gt = ci.tStart(b) - ci.tEnd(a);
            int gq = ci.minus(a) ? ci.qStart(a) - qEnd(b) : ci.qStart(b) - qEnd(a);
            return gt == gq && gt >= 0 && gt <= MAX_SNP_GAP;
        }

        /** The real block of chain i just before it in tStart order that ends at or before its start, or -1. */
        private int prevReal(int i) {
            for (int j = Math.min(i, ci.size()) - 1; j >= 0; j--) {
                if (ci.tStart(i) - ci.tStart(j) > 10 * MAX_INDEL_INC + ci.tEnd(j) - ci.tStart(j)) {
                    return -1;
                }
                if (sameChain(i, j) && ci.tEnd(j) <= ci.tStart(i)) {
                    return j;
                }
            }
            return -1;
        }

        /** The real block of chain i just after it in tStart order that starts at or after its end, or -1. */
        private int nextReal(int i) {
            for (int j = ci.isGap(i) ? 0 : i + 1; j < ci.size(); j++) {
                if (ci.tStart(j) < ci.tEnd(i)) {
                    continue;
                }
                if (ci.tStart(j) - ci.tEnd(i) > 10 * MAX_INDEL_INC) {
                    return -1;
                }
                if (sameChain(i, j)) {
                    return j;
                }
            }
            return -1;
        }

        /** BCFtools prev_block: the block before i's merged block (equal gaps of at most 1 base merged), or -1. */
        private int prevBlock(int i) {
            int cur = ci.isGap(i) ? ci.prevInChain(i) : i;
            if (cur < 0) {
                return -1;
            }
            while (true) {
                int j = prevReal(cur);
                if (j < 0 || !merged(j, cur)) {
                    return j;
                }
                cur = j;
            }
        }

        /** BCFtools next_block, the mirror of {@link #prevBlock}. */
        private int nextBlock(int i) {
            int cur = ci.isGap(i) ? ci.nextInChain(i) : i;
            if (cur < 0) {
                return -1;
            }
            while (true) {
                int j = nextReal(cur);
                if (j < 0 || !merged(cur, j)) {
                    return j;
                }
                cur = j;
            }
        }

        private Span liftIndel(int srcPos5, int srcPos3) {
            Anchor a5 = anchor(srcPos5);
            Anchor a3 = anchor(srcPos3);
            if (a5 == null && a3 == null) {
                throw new Unresolved();
            }
            int len = srcPos3 - srcPos5;
            int npad = 0;
            String chrom;
            boolean minus;
            int pos5;
            int pos3;
            if (a5 == null) {
                chrom = a3.chrom();
                minus = a3.minus();
                pos3 = a3.pos();
                int aux = prevBlock(a3.block());
                npad = aux >= 0 ? ci.tEnd(aux) - srcPos5 : -MAX_INDEL_INC;
                if (npad < -MAX_INDEL_INC || npad >= 0) {
                    npad = -MAX_INDEL_INC;
                    if (npad > -len) {
                        throw new Unresolved();
                    }
                }
                if (srcPos5 + npad < 1) {
                    npad = 1 - srcPos5;
                }
                Anchor b5 = anchor(srcPos5 + npad);
                pos5 = b5 == null ? -1 : b5.pos();
                int dstNpad = minus ? pos5 - pos3 : pos3 - pos5;
                if (b5 == null || !b5.chrom().equals(chrom) || b5.minus() != minus || dstNpad < 0
                        || dstNpad > MAX_INDEL_INC) {
                    pos5 = minus ? pos3 - npad : pos3 - len + npad;
                }
            } else if (a3 == null) {
                chrom = a5.chrom();
                minus = a5.minus();
                pos5 = a5.pos();
                int aux = nextBlock(a5.block());
                npad = aux >= 0 ? ci.tStart(aux) + 1 - srcPos3 : MAX_INDEL_INC;
                if (npad > MAX_INDEL_INC || npad <= 0) {
                    npad = MAX_INDEL_INC;
                    if (npad < len) {
                        throw new Unresolved();
                    }
                }
                Anchor b3 = anchor(srcPos3 + npad);
                pos3 = b3 == null ? -1 : b3.pos();
                int dstNpad = minus ? pos5 - pos3 : pos3 - pos5;
                if (b3 == null || !b3.chrom().equals(chrom) || b3.minus() != minus || dstNpad < 0
                        || dstNpad > MAX_INDEL_INC) {
                    pos3 = minus ? pos5 - npad : pos5 + len + npad;
                }
            } else {
                if (!sameChain(a5.block(), a3.block()) || !a5.chrom().equals(a3.chrom())
                        || a5.minus() != a3.minus()) {
                    throw new Unresolved();
                }
                chrom = a5.chrom();
                minus = a5.minus();
                pos5 = a5.pos();
                pos3 = a3.pos();
                if (Math.abs(pos3 - pos5) > len + MAX_INDEL_INC) {
                    throw new Unresolved();
                }
            }
            int dst5 = minus ? pos3 : pos5;
            int dst3 = minus ? pos5 : pos3;
            if (dst5 > dst3 || dst5 < 1) {
                throw new Unresolved();
            }
            boolean sameRun = a5 != null && a3 != null && ci.runStart(a5.block()) == ci.runStart(a3.block());
            return new Span(chrom, dst5, dst3, minus, npad, sameRun);
        }

        // ---- realigning the pad (nw, get_shift, clip_pad) ----

        private static int clipShift(String[] alleles, String tref, String pad, int npad) {
            int score = Integer.MIN_VALUE;
            int shift = 0;
            String s = tref.substring(1, tref.length() - 1);
            for (String allele : alleles) {
                String str = npad < 0 ? pad + allele : allele + pad;
                if (str.length() < 2) {
                    continue;
                }
                if ((long) (s.length() + 1) * (str.length() - 1) > MAX_NW_CELLS) {
                    throw new Unresolved();
                }
                int[] path = new int[s.length() + str.length()];
                int[] len = new int[1];
                int newScore = nw(s, str.substring(1, str.length() - 1), path, len);
                if (newScore > score) {
                    score = newScore;
                    shift = shift(path, len[0], npad);
                }
            }
            return shift;
        }

        private static final int M = 0;
        private static final int D = 1;
        private static final int I = 2;
        private static final int NEG = Integer.MIN_VALUE / 2;

        /** BCFtools MAX_IND: the best of the three states, ties to M, then I or D as the C macro orders them. */
        private static int maxInd(int[] v) {
            return v[D] < v[I] ? (v[I] < v[M] ? M : I) : (v[D] < v[M] ? M : D);
        }

        /**
         * BCFtools nw: global alignment of s against t with affine gaps (bwa mem scores), deletions and insertions
         * left-aligned. Writes the state path (M, D or I per step) to path[0..len[0]) and returns the score.
         */
        static int nw(String s, String t, int[] path, int[] len) {
            int n = s.length() + 1;
            int m = t.length() + 1;
            int a = 1;
            int b = -4;
            int o = -6;
            int e = -1;
            int[][] sc = new int[3][n * m];
            byte[][] bt = new byte[3][n * m];
            sc[M][0] = 0;
            sc[D][0] = NEG;
            sc[I][0] = NEG;
            for (int j = 1; j < m; j++) {
                sc[M][j] = NEG;
                sc[D][j] = NEG;
                sc[I][j] = o + e * j;
                bt[I][j] = I;
            }
            int[] v = new int[3];
            for (int i = 1; i < n; i++) {
                sc[M][i * m] = NEG;
                sc[D][i * m] = o + e * i;
                bt[D][i * m] = D;
                sc[I][i * m] = NEG;
                for (int j = 1; j < m; j++) {
                    int cell = i * m + j;
                    v[M] = sc[M][cell - m - 1];
                    v[D] = sc[D][cell - m - 1];
                    v[I] = sc[I][cell - m - 1];
                    int ind = maxInd(v);
                    char x = s.charAt(i - 1);
                    char y = t.charAt(j - 1);
                    sc[M][cell] = (x == y || x == 'N' || y == 'N' ? a : b) + v[ind];
                    bt[M][cell] = (byte) ind;

                    v[M] = o + sc[M][cell - m];
                    v[D] = sc[D][cell - m];
                    v[I] = o + sc[I][cell - m];
                    ind = maxInd(v);
                    sc[D][cell] = e + v[ind];
                    bt[D][cell] = (byte) ind;

                    v[M] = o + sc[M][cell - 1];
                    v[D] = o + sc[D][cell - 1];
                    v[I] = sc[I][cell - 1];
                    ind = maxInd(v);
                    sc[I][cell] = e + v[ind];
                    bt[I][cell] = (byte) ind;
                }
            }
            int cell = m * n - 1;
            v[M] = sc[M][cell];
            v[D] = sc[D][cell];
            v[I] = sc[I][cell];
            int ind = maxInd(v);
            int ret = v[ind];
            int k = n + m - 2;
            int end = k;
            while (cell > 0) {
                path[--k] = ind;
                int step = (ind == M || ind == D ? m : 0) + (ind == M || ind == I ? 1 : 0);
                ind = bt[ind][cell];
                cell -= step;
            }
            int l = end - k;
            System.arraycopy(path, k, path, 0, l);
            len[0] = l;
            return ret;
        }

        /** BCFtools get_shift: how far the padded anchor moves in the target. */
        static int shift(int[] path, int len, int npad) {
            int shift = 0;
            if (npad < 0) {
                for (int k = 0; npad < 0 && k < len; k++) {
                    int ind = path[k];
                    if (ind == M || ind == D) {
                        shift++;
                    }
                    if (ind == M || ind == I) {
                        npad++;
                    }
                }
            } else {
                for (int k = len - 1; npad > 0 && k >= 0; k--) {
                    int ind = path[k];
                    if (ind == M || ind == D) {
                        shift--;
                    }
                    if (ind == M || ind == I) {
                        npad--;
                    }
                }
            }
            return shift;
        }

        // ---- the target reference (find_reference) and left alignment ----

        /**
         * BCFtools find_reference for an indel: the allele with the target reference's length whose inner bases
         * (all but the first and last) match it, 0 (REF) or 1 (ALT), or -1 when none does. Every allele's first and
         * last base becomes the target reference's.
         */
        private static int findReference(StringBuilder[] als, String tref) {
            int refLen = tref.length();
            int swap = -1;
            for (int i = 0; i < als.length; i++) {
                String a = als[i].toString();
                if (a.length() != refLen) {
                    continue;
                }
                boolean match = refLen == 1 ? tref.charAt(0) == a.charAt(0)
                        : tref.regionMatches(true, 1, a, 1, refLen - 2);
                if (match) {
                    if (swap >= 0) {
                        break;
                    }
                    swap = i;
                }
            }
            if (refLen > 1) {
                for (StringBuilder a : als) {
                    a.setCharAt(0, tref.charAt(0));
                    a.setCharAt(a.length() - 1, tref.charAt(refLen - 1));
                }
            }
            return swap;
        }

        /** BCFtools is_left_aligned (Tan et al. 2015). */
        static boolean isLeftAligned(StringBuilder[] als) {
            boolean minLenOne = false;
            for (StringBuilder a : als) {
                if (a.length() == 1) {
                    minLenOne = true;
                }
            }
            char last = als[0].charAt(als[0].length() - 1);
            boolean lastDiffer = false;
            for (StringBuilder a : als) {
                if (a.charAt(a.length() - 1) != last) {
                    lastDiffer = true;
                }
            }
            if (!lastDiffer) {
                return false;
            }
            if (!minLenOne) {
                char first = als[0].charAt(0);
                for (StringBuilder a : als) {
                    if (a.charAt(0) != first) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }

        /** BCFtools trim_right, trim_left and shift_left on the target; returns the new 0-based position. */
        private static int leftAlign(StringBuilder[] als, int p0, Seq tgt) {
            // trim_right
            int i = 0;
            if (als[0].length() > 1) {
                outer:
                for (; i < als[0].length() - 1; i++) {
                    char last = als[0].charAt(als[0].length() - 1 - i);
                    for (int j = 1; j < als.length; j++) {
                        if (i >= als[j].length() - 1 || als[j].charAt(als[j].length() - 1 - i) != last) {
                            break outer;
                        }
                    }
                }
                for (StringBuilder a : als) {
                    a.setLength(a.length() - i);
                }
            }
            // trim_left
            i = 0;
            if (als[0].length() > 1) {
                outer:
                for (; i < als[0].length() - 1; i++) {
                    char first = als[0].charAt(i);
                    for (int j = 1; j < als.length; j++) {
                        if (i >= als[j].length() - 1 || als[j].charAt(i) != first) {
                            break outer;
                        }
                    }
                }
                for (StringBuilder a : als) {
                    a.delete(0, i);
                }
                p0 += i;
            }
            // shift_left
            int guard = 0;
            while (p0 > 0) {
                char last = als[0].charAt(als[0].length() - 1);
                for (int j = 1; j < als.length; j++) {
                    if (als[j].charAt(als[j].length() - 1) != last) {
                        return p0;
                    }
                }
                char before = tgt.at(p0);
                for (StringBuilder a : als) {
                    a.insert(0, before);
                    a.setLength(a.length() - 1);
                }
                p0--;
                if (++guard > MAX_EXTENSION) {
                    throw new Unresolved();
                }
            }
            return p0;
        }

        private static String revcomp(String s) {
            return ParseUtilities.reverseComplement(s);
        }
    }
}
