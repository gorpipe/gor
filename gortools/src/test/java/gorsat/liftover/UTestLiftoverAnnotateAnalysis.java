package gorsat.liftover;

import gorsat.Analysis.AnalysisSink;
import gorsat.Commands.Analysis;
import gorsat.Commands.RowHeader;
import gorsat.liftover.LiftFixtures.SpanCase;
import gorsat.process.GenericSessionFactory;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.model.DefaultEventLogger;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.session.GorContext;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.stats.StatsCollector;
import org.gorpipe.model.gor.RowObj;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class UTestLiftoverAnnotateAnalysis {

    private static final ContigPolicy NO_POLICY = LiftFixtures.NO_POLICY;

    private static LiftoverAnnotateConfig snp(ContigPolicy policy, Set<String> failOn) {
        return new LiftoverAnnotateConfig(LiftMode.SNP, 0, 1, -1, -1, -1, policy, 0.05, false, failOn);
    }

    private static LiftoverAnnotateConfig var(ContigPolicy policy) {
        return new LiftoverAnnotateConfig(LiftMode.VAR, 0, 1, -1, 2, 3, policy, 0.05, false, Set.of());
    }

    private static LiftoverAnnotateConfig seg(LiftMode mode, ContigPolicy policy, boolean diag) {
        return new LiftoverAnnotateConfig(mode, 0, 1, 2, -1, -1, policy, 0.05, diag, Set.of());
    }

    private static final class Run {
        final AnalysisSink sink = new AnalysisSink();
        final Analysis pipe;

        Run(LiftoverAnnotateConfig cfg, Supplier<ChainSource> chain, GorContext ctx, String inHeader) {
            RowHeader out = LiftoverAnnotateAnalysis.outgoingHeader(inHeader, cfg.mode(), "liftover_", cfg.diag());
            pipe = new LiftoverAnnotateAnalysis(cfg, out, chain, ctx).$bar(sink);
            pipe.setRowHeader(RowHeader.apply(inHeader, headerTypes(inHeader)));
        }

        Run(LiftoverAnnotateConfig cfg, Supplier<ChainSource> chain) {
            this(cfg, chain, null, headerFor(cfg));
        }

        void feed(String... rows) {
            for (String r : rows) {
                pipe.process(RowObj.apply(r));
            }
        }

        List<String> finish() {
            pipe.securedFinish(null);
            List<String> r = new ArrayList<>();
            scala.collection.Iterator<Row> it = sink.rows().iterator();
            while (it.hasNext()) {
                r.add(it.next().toString());
            }
            return r;
        }
    }

    private static String headerFor(LiftoverAnnotateConfig cfg) {
        switch (cfg.mode()) {
            case SNP:
                return "chrom\tpos\tx";
            case VAR:
                return "chrom\tpos\tref\talt\tx";
            default:
                return "chrom\tstart\tend";
        }
    }

    private static String[] headerTypes(String header) {
        String[] cols = header.split("\t");
        String[] types = new String[cols.length];
        Arrays.fill(types, "S");
        if (types.length > 1) {
            types[1] = "I";
        }
        return types;
    }

    private static Supplier<ChainSource> engine() {
        return () -> LiftFixtures.ENGINE_CHAIN;
    }

    private static String suffix(String row, String input) {
        Assert.assertTrue(row + " should start with " + input, row.startsWith(input + "\t"));
        return row.substring(input.length());
    }

    @Test
    public void oneToOnePrefix() {
        String[] in = {"chr1\t101", "chr1\t401", "chr1\t600", "chr4\t175", "chr5\t150", "chrM\t100"};
        String[] exp = {"mapped\tchr1\t1001\t+", "unmapped\t\t0\t", "mapped\tchr1\t5001\t-",
                "mapped\tchr4\t5025\t+", "ambiguous\t\t0\t", "mapped\tchrM\t102\t+"};
        Run run = new Run(snp(NO_POLICY, Set.of()), engine());
        run.feed(in);
        List<String> out = run.finish();
        Assert.assertEquals(6, out.size());
        for (int i = 0; i < in.length; i++) {
            Assert.assertEquals("\t" + exp[i], suffix(out.get(i), in[i]));
        }
    }

    @Test
    public void fourIdenticalUnmappedStayFour() {
        Run run = new Run(var(NO_POLICY), engine());
        String row = "chr1\t450\tA\tG\tdup";
        run.feed(row, row, row, row);
        List<String> out = run.finish();
        Assert.assertEquals(4, out.size());
        for (String s : out) {
            Assert.assertTrue(s, s.endsWith("\tunmapped\t\t0\t\t\t"));
        }
    }

    @Test
    public void tieGivesOneAmbiguousRow() {
        Run run = new Run(var(NO_POLICY), engine());
        run.feed("chr5\t150\tA\tG\tt");
        List<String> out = run.finish();
        Assert.assertEquals(1, out.size());
        Assert.assertTrue(out.get(0), out.get(0).endsWith("\tambiguous\t\t0\t\t\t"));
    }

    @Test
    public void unsortedInputKeepsOrder() {
        Run run = new Run(snp(NO_POLICY, Set.of()), engine());
        run.feed("chr1\t600", "chr1\t101", "chr1\t401");
        List<String> out = run.finish();
        Assert.assertEquals(3, out.size());
        Assert.assertTrue(out.get(0).startsWith("chr1\t600\t"));
        Assert.assertTrue(out.get(1).startsWith("chr1\t101\t"));
        Assert.assertTrue(out.get(2).startsWith("chr1\t401\t"));
    }

    @Test
    public void varMinusPerAllele() {
        Run run = new Run(var(NO_POLICY), engine());
        run.feed("chr1\t511\tACG\tT,GA", "chr1\t511\tA\t<DEL>", "chr1\t600\t\tG");
        List<String> out = run.finish();
        Assert.assertEquals(3, out.size());
        Assert.assertEquals("\tmapped\tchr1\t5088\tCGT\tA,TC\t-", suffix(out.get(0), "chr1\t511\tACG\tT,GA"));
        Assert.assertEquals("\tmapped\tchr1\t5090\tT\t<DEL>\t-", suffix(out.get(1), "chr1\t511\tA\t<DEL>"));
        Assert.assertEquals("\tmapped\tchr1\t5002\t\tC\t-", suffix(out.get(2), "chr1\t600\t\tG"));
    }

    @Test
    public void passthroughNeverTouchesChain() {
        ContigPolicy pol = LiftFixtures.PASSTHROUGH_CHRM;
        int[] calls = {0};
        Supplier<ChainSource> counting = () -> {
            calls[0]++;
            return LiftFixtures.ENGINE_CHAIN;
        };
        Run run = new Run(snp(pol, Set.of()), counting);
        run.feed("chrM\t10", "chrM\t20", "chrM\t30");
        Assert.assertEquals(0, calls[0]);
        List<String> out = run.finish();
        Assert.assertEquals("\tpassthrough\tchrM\t10\t", suffix(out.get(0), "chrM\t10"));
        Assert.assertEquals("\tpassthrough\tchrM\t20\t", suffix(out.get(1), "chrM\t20"));
        Assert.assertEquals("\tpassthrough\tchrM\t30\t", suffix(out.get(2), "chrM\t30"));

        Run run2 = new Run(snp(pol, Set.of()), counting);
        run2.feed("chrM\t10", "chr1\t101", "chr1\t401");
        Assert.assertEquals(1, calls[0]);

        Run run3 = new Run(snp(pol, Set.of()), () -> LiftFixtures.THROWING_CHAIN);
        run3.feed("chrM\t10");
        Assert.assertThrows(IllegalStateException.class, () -> run3.feed("chr1\t101"));
    }

    @Test
    public void passthroughVarKeepsInputAlleles() {
        Run run = new Run(var(LiftFixtures.PASSTHROUGH_CHRM), () -> LiftFixtures.THROWING_CHAIN);
        run.feed("chrM\t10\tACG\tT,GA");
        List<String> out = run.finish();
        Assert.assertEquals("\tpassthrough\tchrM\t10\tACG\tT,GA\t", suffix(out.get(0), "chrM\t10\tACG\tT,GA"));
    }

    @Test
    public void unsupportedSourceNeverTouchesChain() {
        ContigPolicy pol = new ContigPolicy(Set.of(), Set.of("chr1"));
        Run run = new Run(snp(pol, Set.of()), () -> LiftFixtures.THROWING_CHAIN);
        run.feed("chr9\t100");
        List<String> out = run.finish();
        Assert.assertEquals("\tunsupported_contig\t\t0\t", suffix(out.get(0), "chr9\t100"));
    }

    @Test
    public void failOn() {
        Run run = new Run(snp(NO_POLICY, Set.of("unmapped")), engine());
        run.feed("chr1\t101", "chr1\t600");
        GorDataException e = Assert.assertThrows(GorDataException.class, () -> run.feed("chr1\t401"));
        Assert.assertEquals("LIFTOVER_ANNOTATE: status 'unmapped' at chr1:401", e.getMessage());
    }

    @Test
    public void segEqualsSeg1Shifted() {
        for (SpanCase c : LiftFixtures.SPAN_CASES) {
            Run seg = new Run(seg(LiftMode.SEG, c.policy(), false), () -> c.chain());
            Run seg1 = new Run(seg(LiftMode.SEG1, c.policy(), false), () -> c.chain());
            String segIn = c.chrom() + "\t" + (c.s1() - 1) + "\t" + c.e1();
            String seg1In = c.chrom() + "\t" + c.s1() + "\t" + c.e1();
            seg.feed(segIn);
            seg1.feed(seg1In);
            String[] a = suffix(seg.finish().get(0), segIn).substring(1).split("\t", -1);
            String[] b = suffix(seg1.finish().get(0), seg1In).substring(1).split("\t", -1);
            String msg = "case " + c.number();
            // status, chrom, start, stop, strand, chain_blocks
            Assert.assertEquals(msg, 6, a.length);
            Assert.assertEquals(msg, b[0], a[0]);
            Assert.assertEquals(msg, b[1], a[1]);
            Assert.assertEquals(msg, b[3], a[3]);
            Assert.assertEquals(msg, b[4], a[4]);
            Assert.assertEquals(msg, b[5], a[5]);
            if (LiftEngine.HAS_TARGET.contains(a[0])) {
                Assert.assertEquals(msg, Integer.parseInt(b[2]) - 1, Integer.parseInt(a[2]));
            } else {
                Assert.assertEquals(msg, "0", a[2]);
                Assert.assertEquals(msg, "0", b[2]);
            }
        }
    }

    @Test
    public void segDiagColumns() {
        Run run = new Run(seg(LiftMode.SEG1, NO_POLICY, true), engine());
        run.feed("chr1\t450\t520");
        List<String> out = run.finish();
        Assert.assertEquals("\tstart_unmapped\t\t0\t0\t\t1\t70\t0\t0.0\t\t0\t\tchr1\t5081\t-",
                suffix(out.get(0), "chr1\t450\t520"));
    }

    @Test
    public void zeroLengthSegIsJunction() {
        Run seg = new Run(seg(LiftMode.SEG, NO_POLICY, false), engine());
        seg.feed("chr1\t150\t150", "chr1\t100\t100");
        List<String> out = seg.finish();
        Assert.assertEquals("\tmapped\tchr1\t1050\t1050\t+\t1", suffix(out.get(0), "chr1\t150\t150"));
        Assert.assertEquals("\tunmapped\t\t0\t0\t\t0", suffix(out.get(1), "chr1\t100\t100"));

        Run seg1 = new Run(seg(LiftMode.SEG1, NO_POLICY, false), engine());
        seg1.feed("chr1\t151\t150");
        Assert.assertEquals("\tmapped\tchr1\t1051\t1050\t+\t1", suffix(seg1.finish().get(0), "chr1\t151\t150"));
    }

    @Test
    public void reversedIntervalThrows() {
        Run seg = new Run(seg(LiftMode.SEG, NO_POLICY, false), engine());
        GorDataException e = Assert.assertThrows(GorDataException.class, () -> seg.feed("chr1\t150\t149"));
        Assert.assertTrue(e.getMessage(),
                e.getMessage().startsWith("LIFTOVER_ANNOTATE: invalid interval [150,149] at chr1:150"));
        Run seg1 = new Run(seg(LiftMode.SEG1, NO_POLICY, false), engine());
        GorDataException e1 = Assert.assertThrows(GorDataException.class, () -> seg1.feed("chr1\t151\t149"));
        Assert.assertTrue(e1.getMessage(),
                e1.getMessage().startsWith("LIFTOVER_ANNOTATE: invalid interval [151,149] at chr1:151"));
    }

    @Test
    public void nonIntegerCoordinateFailsWithRow() {
        Run seg = new Run(seg(LiftMode.SEG, NO_POLICY, false), engine());
        GorDataException e = Assert.assertThrows(GorDataException.class, () -> seg.feed("chr1\t150\tNA"));
        Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("LIFTOVER_ANNOTATE: "));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Row: chr1\t150\tNA"));

        LiftoverAnnotateConfig cfg = new LiftoverAnnotateConfig(LiftMode.SNP, 2, 3, -1, -1, -1, NO_POLICY, 0.05,
                false, Set.of());
        Run snp = new Run(cfg, engine(), null, "chrom\tpos\tc\tp");
        GorDataException e2 = Assert.assertThrows(GorDataException.class, () -> snp.feed("chrN\t0\tchr1\tNA"));
        Assert.assertTrue(e2.getMessage(), e2.getMessage().startsWith("LIFTOVER_ANNOTATE: "));
        Assert.assertTrue(e2.getMessage(), e2.getMessage().contains("Row: chrN\t0\tchr1\tNA"));
    }

    @Test
    public void contigNamingMismatchIsUnmapped() {
        Run run = new Run(snp(NO_POLICY, Set.of()), engine());
        run.feed("1\t150");
        Assert.assertEquals("\tunmapped\t\t0\t", suffix(run.finish().get(0), "1\t150"));
    }

    @Test
    public void typesPropagate() {
        LiftoverAnnotateConfig cfg = snp(NO_POLICY, Set.of());
        Run run = new Run(cfg, engine());
        String[] types = run.sink.rowHeader().columnTypes();
        Assert.assertArrayEquals(new String[] {"S", "S", "I", "S"}, Arrays.copyOfRange(types, types.length - 4,
                types.length));
        // input types are carried over from the incoming header
        Assert.assertEquals("I", types[1]);
        Assert.assertEquals("liftover_status", run.sink.rowHeader().columnNames()[3]);
    }

    @Test
    public void outputColumnsPerMode() {
        Assert.assertEquals(List.of("status", "chrom", "pos", "strand"), names(LiftMode.SNP, "", false));
        Assert.assertEquals(List.of("status", "chrom", "pos", "ref", "alt", "strand"), names(LiftMode.VAR, "", false));
        Assert.assertEquals(List.of("status", "chrom", "start", "stop", "strand", "chain_blocks"),
                names(LiftMode.SEG, "", false));
        Assert.assertEquals(15, LiftoverAnnotateAnalysis.outputColumns(LiftMode.SEG1, "", true).size());
        List<LiftoverAnnotateAnalysis.OutputColumn> cols = LiftoverAnnotateAnalysis.outputColumns(LiftMode.SEG, "", true);
        Assert.assertEquals("D", cols.get(8).type());
        Assert.assertEquals("size_ratio", cols.get(8).name());
        Assert.assertEquals("I", cols.get(5).type());
    }

    private static List<String> names(LiftMode mode, String prefix, boolean diag) {
        List<String> r = new ArrayList<>();
        for (LiftoverAnnotateAnalysis.OutputColumn c : LiftoverAnnotateAnalysis.outputColumns(mode, prefix, diag)) {
            r.add(c.name());
        }
        return r;
    }

    @Test
    public void diagColumnNamesMatchTemplate() {
        List<String> n = names(LiftMode.SEG, "lifted_", true);
        Assert.assertEquals(List.of("lifted_start_chrom", "lifted_start_pos", "lifted_start_strand",
                "lifted_stop_chrom", "lifted_stop_pos", "lifted_stop_strand"), n.subList(n.size() - 6, n.size()));
    }

    @Test
    public void statsCountPerStatus() {
        StatsCollector collector = new StatsCollector();
        GorSession session = new GenericSessionFactory().create();
        session.init(session.getProjectContext(), session.getSystemContext(), session.getCache(),
                new DefaultEventLogger() {
                    @Override
                    public StatsCollector getStatsCollector() {
                        return collector;
                    }
                });
        GorContext ctx = new GorContext(session);
        Run run = new Run(snp(LiftFixtures.PASSTHROUGH_CHRM, Set.of()), engine(), ctx, "chrom\tpos\tx");
        run.feed("chrM\t1", "chrM\t2", "chrM\t3", "chr1\t101");
        run.finish();
        Map<String, Double> stats = collector.getStats().get("LiftoverAnnotate:");
        Assert.assertNotNull(collector.getStats().toString(), stats);
        Assert.assertEquals(3.0, stats.get("passthrough"), 0.0);
        Assert.assertEquals(1.0, stats.get("mapped"), 0.0);
    }
}
