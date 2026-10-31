package gorsat.liftover;

import gorsat.Analysis.AnalysisSink;
import gorsat.Commands.Analysis;
import gorsat.Commands.RowHeader;
import gorsat.DynIterator;
import gorsat.process.GenericSessionFactory;
import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import gorsat.process.PipeInstance;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceMetadata;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.session.GorContext;
import org.gorpipe.model.gor.RowObj;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests for the lines and branches of the liftover engine that the other suites do not reach (ENGKNOW-4117,
 * Task 12 coverage gate): defensive guards, the loader's concurrency and failure paths, and the clone and header
 * handling of the LIFTOVER step.
 */
public class UTestLiftoverCoverageGaps {

    private static final String HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private GorContext context;

    @Before
    public void setUp() {
        GorPipeCommands.register();
        GorInputSources.register();
        DynIterator.createGorIterator_$eq(PipeInstance::createGorIterator);
        context = new GenericSessionFactory().create().getGorContext();
    }

    private String writeChain(String name, String body) throws IOException {
        Path p = tmp.getRoot().toPath().resolve(name);
        Files.writeString(p, HEADER + "\n" + body);
        return p.toString();
    }

    private FileChainSource source(String path) {
        return new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.ERROR, context);
    }

    // ---- small guards ----

    @Test
    public void overlapPolicyParseRejectsNull() {
        try {
            OverlapPolicy.parse(null);
            Assert.fail();
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Invalid overlap policy 'null'"));
        }
        Assert.assertEquals(OverlapPolicy.BEST, OverlapPolicy.parse(" Best "));
    }

    @Test
    public void annotateConfigFailOnDefaultsToEmpty() {
        LiftoverAnnotateConfig cfg = new LiftoverAnnotateConfig(LiftMode.SNP, 0, 1, -1, -1, -1,
                LiftFixtures.NO_POLICY, 0.05, false, null);
        Assert.assertEquals(Set.of(), cfg.failOn());
    }

    @Test
    public void allSpanOfAnInvertedIntervalIsEmpty() {
        ContigIndex ci = ContigIndex.of("chr1", OverlapPolicy.ERROR, LiftFixtures.CHR1_BLOCKS);
        Assert.assertEquals(0, ci.allSpan(150, 140).length);
        Assert.assertEquals(1, ci.allSpan(150, 160).length);
    }

    @Test
    public void builderSizeCountsBlocks() {
        ContigIndexBuilder b = new ContigIndexBuilder("chr1", OverlapPolicy.ERROR, "test");
        Assert.assertEquals(0, b.size());
        b.add(LiftFixtures.CHR1_BLOCKS.get(0));
        b.add(LiftFixtures.CHR1_BLOCKS.get(1));
        Assert.assertEquals(2, b.size());
    }

    /** liftMate without a block (-1) looks the mate up, as it does for a mate outside the read's block. */
    @Test
    public void liftMateWithoutBlockFindsTheBestBlock() {
        ContigIndex ci = ContigIndex.of("chr1", OverlapPolicy.ERROR, LiftFixtures.CHR1_BLOCKS);
        Assert.assertEquals(1050, LiftEngine.liftMate(ci, -1, 150, "chr1"));
        Assert.assertEquals(1050, LiftEngine.liftMate(ci, 0, 150, "chr1"));
        Assert.assertEquals(0, LiftEngine.liftMate(ci, -1, 150, "chr2"));
        Assert.assertEquals(0, LiftEngine.liftMate(ci, -1, 450, "chr1"));
    }

    // ---- FileChainSource ----

    @Test
    public void nestedQueryNamedNorIsRejectedWhatEverFollows() throws IOException {
        String path = writeChain("c.gor", "chr1\t100\t200\tchr1\t1000\t1100\t+\t1000\n");
        for (String q : new String[]{"<(nor)", "<(NOR " + path + ")", "<(nor\t" + path + ")"}) {
            try {
                FileChainSource.resolve(q, context);
                Assert.fail(q);
            } catch (GorParsingException e) {
                Assert.assertTrue(e.getMessage(), e.getMessage().contains("must be a gor query"));
            }
        }
        // a command that only starts with nor is not nor
        try {
            FileChainSource.resolve("<(norrows 1)", context);
            Assert.fail("norrows has no chain columns");
        } catch (GorParsingException e) {
            // it ran as a gor query and was refused for its columns, not for being nor
            Assert.assertEquals("Liftover chain <(norrows 1) has no tEnd column, header: ChromNOR,PosNOR,RowNum",
                    e.getMessage());
        }
    }

    private static DriverBackedFileReader readerWith(DataSource source) {
        return new DriverBackedFileReader("") {
            @Override
            public DataSource resolveUrl(String url, boolean writeable) {
                return source;
            }
        };
    }

    private static void assertNoUniqueId(DataSource source) {
        try {
            FileChainSource.uniqueId(readerWith(source), "/some/chain.gor");
            Assert.fail();
        } catch (GorResourceException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("no unique id"));
        }
        Mockito.verify(source).close();
    }

    @Test
    public void uniqueIdNeedsMetadataWithAnId() throws IOException {
        DataSource noMetadata = Mockito.mock(DataSource.class);
        Mockito.when(noMetadata.getSourceMetadata()).thenReturn(null);
        assertNoUniqueId(noMetadata);

        for (String id : new String[]{null, ""}) {
            SourceMetadata metadata = Mockito.mock(SourceMetadata.class);
            Mockito.when(metadata.getUniqueId()).thenReturn(id);
            DataSource source = Mockito.mock(DataSource.class);
            Mockito.when(source.getSourceMetadata()).thenReturn(metadata);
            assertNoUniqueId(source);
        }

        SourceMetadata metadata = Mockito.mock(SourceMetadata.class);
        Mockito.when(metadata.getUniqueId()).thenReturn("abc123");
        DataSource source = Mockito.mock(DataSource.class);
        Mockito.when(source.getSourceMetadata()).thenReturn(metadata);
        Assert.assertEquals("abc123", FileChainSource.uniqueId(readerWith(source), "/some/chain.gor"));
        Mockito.verify(source).close();
    }

    private static Object await(CompletableFuture<ContigIndex> future) throws Throwable {
        Method m = FileChainSource.class.getDeclaredMethod("await", CompletableFuture.class);
        m.setAccessible(true);
        try {
            return m.invoke(null, future);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /** What a caller sees when it waits for a load that another thread failed. */
    @Test
    public void awaitRethrowsTheFailureOfAnotherLoad() throws Throwable {
        CompletableFuture<ContigIndex> runtime = new CompletableFuture<>();
        runtime.completeExceptionally(new GorDataException("bad chain"));
        try {
            await(runtime);
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertEquals("bad chain", e.getMessage());
        }

        CompletableFuture<ContigIndex> error = new CompletableFuture<>();
        error.completeExceptionally(new StackOverflowError("deep"));
        try {
            await(error);
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertTrue(e.toString(), e instanceof StackOverflowError);
        }

        CompletableFuture<ContigIndex> checked = new CompletableFuture<>();
        checked.completeExceptionally(new IOException("disk"));
        try {
            await(checked);
            Assert.fail();
        } catch (GorSystemException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("load failed"));
            Assert.assertTrue(e.getCause() instanceof IOException);
        }

        ContigIndex ci = ContigIndex.of("chr1", OverlapPolicy.ERROR, LiftFixtures.CHR1_BLOCKS);
        CompletableFuture<ContigIndex> done = CompletableFuture.completedFuture(ci);
        Assert.assertSame(ci, await(done));
    }

    @Test
    public void awaitKeepsTheInterrupt() throws Throwable {
        Thread.currentThread().interrupt();
        try {
            await(new CompletableFuture<>());
            Assert.fail();
        } catch (GorSystemException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Interrupted"));
            Assert.assertTrue("the interrupt is kept", Thread.interrupted());
        }
    }

    /** Two threads load the same contig: the loser of the race waits for the winner's load (the future in the map). */
    @Test
    public void contigWaitsForALoadThatIsAlreadyRunning() throws Exception {
        String path = writeChain("race.gor", "chr1\t100\t200\tchr1\t1000\t1100\t+\t1000\n");
        FileChainSource src = source(path);
        ContigIndex winner = ContigIndex.of("chr1", OverlapPolicy.ERROR, LiftFixtures.CHR1_BLOCKS);
        // the map looks empty to get() and has the other thread's future by the time putIfAbsent runs
        ConcurrentHashMap<String, CompletableFuture<ContigIndex>> raced = new ConcurrentHashMap<>() {
            @Override
            public CompletableFuture<ContigIndex> get(Object key) {
                return null;
            }

            @Override
            public CompletableFuture<ContigIndex> putIfAbsent(String key, CompletableFuture<ContigIndex> value) {
                return CompletableFuture.completedFuture(winner);
            }
        };
        Field f = FileChainSource.class.getDeclaredField("loaded");
        f.setAccessible(true);
        f.set(src, raced);
        Assert.assertSame(winner, src.contig("chr1"));
        Assert.assertEquals("this thread did not load", 0, src.loadCount());
    }

    @Test
    public void rowWithANumberThatIsNoNumberFailsTheLoad() throws IOException {
        String path = writeChain("nan.gor", "chr1\t100\t200\tchr1\tabc\t1100\t+\t1000\n");
        try {
            source(path).contig("chr1");
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Liftover chain "));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("invalid number in row chr1:100"));
        }
    }

    // ---- LiftoverRelocateAnalysis ----

    private static final String OUT_HEADER = "chrom\tpos\tx\thg_chrom\thg_pos\thg_qStrand\thg_liftoverStatus";

    private static LiftoverRelocateAnalysis relocate() {
        RowHeader out = RowHeader.apply(OUT_HEADER, new String[]{"S", "I", "S", "S", "I", "S", "S"});
        return new LiftoverRelocateAnalysis(RelocateMode.SNP, false, RelocateColumns.NONE, out,
                () -> LiftFixtures.ENGINE_CHAIN);
    }

    private static final RowHeader IN_HEADER = RowHeader.apply("chrom\tpos\tx", new String[]{"S", "I", "S"});

    private static List<String> lift(Analysis step, String... rows) {
        AnalysisSink sink = new AnalysisSink();
        step.$bar(sink);
        for (String r : rows) {
            step.process(RowObj.apply(r));
        }
        step.securedFinish(null);
        List<String> out = new java.util.ArrayList<>();
        scala.collection.Iterator<Row> it = sink.rows().iterator();
        while (it.hasNext()) {
            out.add(it.next().toString());
        }
        return out;
    }

    private static LiftoverRelocateAnalysis relocate(RelocateMode mode) {
        RowHeader out = RowHeader.apply(OUT_HEADER, new String[]{"S", "I", "S", "S", "I", "S", "S"});
        return new LiftoverRelocateAnalysis(mode, false, RelocateColumns.NONE, out, () -> LiftFixtures.ENGINE_CHAIN);
    }

    private static void assertRelocateEmptyCell(RelocateMode mode, String row, String what, String at) {
        LiftoverRelocateAnalysis step = relocate(mode);
        step.setRowHeader(IN_HEADER);
        try {
            lift(step, row);
            Assert.fail("expected GorDataException for " + row.replace('\t', '|'));
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("LIFTOVER: "));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(what + " is empty"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(" at " + at));
        }
    }

    @Test
    public void relocateEmptyCoordinateCellsAreInvalid() {
        assertRelocateEmptyCell(RelocateMode.SNP, "chr1\t\tx", "column #2", "chr1:");
        assertRelocateEmptyCell(RelocateMode.SNP, "chr1\t \tx", "column #2", "chr1: ");
        // seg: an empty start must not lift [0,end), an empty end must not lift [start,0)
        assertRelocateEmptyCell(RelocateMode.SEG, "chr1\t\t200\tx", "column #2", "chr1:");
        assertRelocateEmptyCell(RelocateMode.SEG, "chr1\t100\t\tx", "column #3", "chr1:100");
        assertRelocateEmptyCell(RelocateMode.BAM, "chr1\t\t200\tx", "column #2", "chr1:");
        assertRelocateEmptyCell(RelocateMode.BAM, "chr1\t100\t\tx", "column #3", "chr1:100");
    }

    private static void assertRelocateSignOnlyCell(RelocateMode mode, String row, String what, String at) {
        LiftoverRelocateAnalysis step = relocate(mode);
        step.setRowHeader(IN_HEADER);
        try {
            lift(step, row);
            Assert.fail("expected GorDataException for " + row.replace('\t', '|'));
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("LIFTOVER: "));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Illegal " + what + ": '"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(" at " + at));
        }
    }

    @Test
    public void relocateSignOnlyCoordinateCellsAreInvalid() {
        // RowObj.colInt reads a lone '-' as 0: a -seg start of '-' must not lift [0,end)
        assertRelocateSignOnlyCell(RelocateMode.SNP, "chr1\t-\tx", "start position in column #2", "chr1:-");
        assertRelocateSignOnlyCell(RelocateMode.SEG, "chr1\t-\t200\tx", "start position in column #2", "chr1:-");
        assertRelocateSignOnlyCell(RelocateMode.BAM, "chr1\t100\t-\tx", "stop position in column #3", "chr1:100");
    }

    @Test
    public void relocateNonIntegerEndNamesChromAndPos() {
        LiftoverRelocateAnalysis step = relocate(RelocateMode.SEG);
        step.setRowHeader(IN_HEADER);
        try {
            lift(step, "chr1\t150\tNA\tx");
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Illegal stop position in column #3: 'NA'"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(" at chr1:150"));
        }
    }

    @Test
    public void relocateStepWithoutANextStepTakesTheHeader() {
        LiftoverRelocateAnalysis step = relocate();
        step.setRowHeader(IN_HEADER);
        Assert.assertEquals(IN_HEADER.toString(), step.getHeader());
    }

    @Test
    public void relocateCloneIsAWorkingCopy() {
        LiftoverRelocateAnalysis original = relocate();
        original.setRowHeader(IN_HEADER);
        Analysis copy = original.clone();
        Assert.assertTrue("clone must stay a LiftoverRelocateAnalysis", copy instanceof LiftoverRelocateAnalysis);
        Assert.assertNotSame(original, copy);
        Assert.assertEquals(original.getHeader(), copy.getHeader());
        List<String> expected = List.of("chr1\t1001\tx\tchr1\t101\t+\tmapped",
                "chr1\t0\tx\tchr1\t401\t\tunmapped");
        String[] in = {"chr1\t101\tx", "chr1\t401\tx"};
        Assert.assertEquals(expected, lift(original, in));
        Assert.assertEquals(expected, lift(copy, in));
    }

    @Test
    public void relocateCloneOfAConnectedStepFailsLoudly() {
        LiftoverRelocateAnalysis downstream = relocate();
        downstream.$bar(new AnalysisSink());
        LiftoverRelocateAnalysis upstream = relocate();
        new AnalysisSink().$bar(upstream);
        for (LiftoverRelocateAnalysis connected : List.of(downstream, upstream)) {
            try {
                connected.clone();
                Assert.fail();
            } catch (UnsupportedOperationException e) {
                Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("LIFTOVER cannot be cloned"));
            }
        }
    }

    @Test
    public void annotateCloneOfAStepWithAnUpstreamStepFailsLoudly() {
        LiftoverAnnotateConfig cfg = new LiftoverAnnotateConfig(LiftMode.SNP, 0, 1, -1, -1, -1,
                LiftFixtures.NO_POLICY, 0.05, false, Set.of());
        RowHeader out = LiftoverAnnotateAnalysis.outgoingHeader("chrom\tpos\tx", cfg.mode(), "liftover_", false);
        LiftoverAnnotateAnalysis step = new LiftoverAnnotateAnalysis(cfg, out, () -> LiftFixtures.ENGINE_CHAIN, null);
        new AnalysisSink().$bar(step);
        try {
            step.clone();
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("LIFTOVER_ANNOTATE cannot be cloned"));
        }
    }
}
