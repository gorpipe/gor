package gorsat.liftover;

import gorsat.DynIterator;
import gorsat.process.GenericSessionFactory;
import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import gorsat.process.PipeInstance;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.session.GorContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UTestLiftChain {

    private static final String HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore";
    private static final int UNMAPPED = ContigIndex.UNMAPPED;
    private static final int AMBIGUOUS = ContigIndex.AMBIGUOUS;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private GorContext context;
    private ContigIndex chr1;

    @Before
    public void setUp() {
        GorPipeCommands.register();
        GorInputSources.register();
        DynIterator.createGorIterator_$eq(PipeInstance::createGorIterator);
        context = new GenericSessionFactory().create().getGorContext();
        chr1 = ContigIndex.of("chr1", OverlapPolicy.ERROR, LiftFixtures.CHR1_BLOCKS);
    }

    // ---- helpers ----

    private static void assertDataError(String expected, ChainBlock... blocks) {
        assertDataError(expected, OverlapPolicy.BEST, blocks);
    }

    private static void assertDataError(String expected, OverlapPolicy policy, ChainBlock... blocks) {
        try {
            ContigIndex.of("chr1", policy, Arrays.asList(blocks));
            Assert.fail("expected GorDataException containing: " + expected);
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(expected));
        }
    }

    private static String chainText(String header, String chrom, List<ChainBlock> blocks) {
        StringBuilder sb = new StringBuilder(header).append('\n');
        for (ChainBlock b : blocks) {
            sb.append(chrom).append('\t').append(b.tStart()).append('\t').append(b.tEnd()).append('\t')
                    .append(b.qChrom()).append('\t').append(b.qStart()).append('\t').append(b.qEnd()).append('\t')
                    .append(b.strand()).append('\t').append(scoreText(b.score())).append('\n');
        }
        return sb.toString();
    }

    private static String scoreText(double s) {
        return s == Math.rint(s) ? Long.toString((long) s) : Double.toString(s);
    }

    private String writeChain(String name, String text) throws IOException {
        Path p = tmp.getRoot().toPath().resolve(name);
        Files.writeString(p, text);
        return p.toString();
    }

    private String writeChr1Chain(String name) throws IOException {
        return writeChain(name, chainText(HEADER, "chr1", LiftFixtures.CHR1_BLOCKS));
    }

    private static <T> List<T> runConcurrently(int n, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<T>> fs = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                fs.add(pool.submit(() -> {
                    go.await();
                    return task.call();
                }));
            }
            go.countDown();
            List<T> res = new ArrayList<>();
            for (Future<T> f : fs) {
                res.add(f.get());
            }
            return res;
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- point / span lookups ----

    @Test
    public void findPointOnFixture() {
        Assert.assertEquals(UNMAPPED, chr1.findPoint(100));
        Assert.assertEquals(0, chr1.findPoint(101));
        Assert.assertEquals(0, chr1.findPoint(200));
        Assert.assertEquals(1, chr1.findPoint(201));
        Assert.assertEquals(2, chr1.findPoint(400));
        Assert.assertEquals(UNMAPPED, chr1.findPoint(401));
        Assert.assertEquals(3, chr1.findPoint(600));
        Assert.assertEquals(4, chr1.findPoint(601));
    }

    @Test
    public void findSpanOnFixture() {
        Assert.assertEquals(0, chr1.findSpan(197, 200));
        Assert.assertEquals(UNMAPPED, chr1.findSpan(198, 201));
    }

    @Test
    public void findSpanZeroLength() {
        Assert.assertEquals(UNMAPPED, chr1.findSpan(100, 100));
        Assert.assertEquals(0, chr1.findSpan(101, 101));
        Assert.assertEquals(0, chr1.findSpan(199, 199));
        Assert.assertEquals(UNMAPPED, chr1.findSpan(200, 200));
    }

    @Test
    public void findSpanEndBeforeStartIsUnmapped() {
        Assert.assertEquals(UNMAPPED, chr1.findSpan(150, 140));
    }

    @Test
    public void countBlocksOnFixture() {
        Assert.assertEquals(2, chr1.countBlocks(151, 250));
        Assert.assertEquals(0, chr1.countBlocks(401, 500));
        Assert.assertEquals(1, chr1.countBlocks(750, 950));
        Assert.assertEquals(1, chr1.countBlocks(380, 450));
        Assert.assertEquals(1, chr1.countBlocks(150, 150));
    }

    @Test
    public void bestOverlapPicksMaxScore() {
        ContigIndex chr4 = ContigIndex.of("chr4", OverlapPolicy.BEST, LiftFixtures.CHR4_BLOCKS);
        Assert.assertEquals(0, chr4.findPoint(120));
        Assert.assertEquals(1, chr4.findPoint(175));
        Assert.assertEquals(1, chr4.findSpan(194, 204));
        ContigIndex chr5 = ContigIndex.of("chr5", OverlapPolicy.BEST, LiftFixtures.CHR5_BLOCKS);
        Assert.assertEquals(AMBIGUOUS, chr5.findPoint(150));
    }

    @Test
    public void accessorsExposeBlockFields() {
        Assert.assertEquals("chr1", chr1.contig());
        Assert.assertEquals(21, chr1.size());
        Assert.assertEquals(500, chr1.tStart(3));
        Assert.assertEquals(600, chr1.tEnd(3));
        Assert.assertEquals("chr1", chr1.qChrom(3));
        Assert.assertEquals(5000, chr1.qStart(3));
        Assert.assertTrue(chr1.minus(3));
        Assert.assertFalse(chr1.minus(0));
        Assert.assertEquals(10.0, chr1.score(0), 0);
        Assert.assertEquals("chr2", chr1.qChrom(4));
    }

    @Test
    public void emptyIndexFindsNothing() {
        ContigIndex e = ContigIndex.empty("chrZ");
        Assert.assertEquals("chrZ", e.contig());
        Assert.assertEquals(0, e.size());
        Assert.assertEquals(UNMAPPED, e.findPoint(5));
        Assert.assertEquals(UNMAPPED, e.findSpan(1, 5));
        Assert.assertEquals(0, e.allPoint(5).length);
        Assert.assertEquals(0, e.allSpan(1, 5).length);
        Assert.assertEquals(0, e.countBlocks(1, 5));
    }

    // ---- build-time validation ----

    @Test
    public void rejectsZeroLengthBlock() {
        assertDataError("tEnd <= tStart", new ChainBlock(200, 200, "chr1", 0, 0, "+", 10));
    }

    @Test
    public void rejectsGappedBlock() {
        assertDataError("gapped block", new ChainBlock(100, 200, "chr1", 0, 99, "+", 10));
    }

    @Test
    public void rejectsInvalidStrand() {
        assertDataError("invalid strand '0'", LiftFixtures.blk(100, 200, "chr1", 0, "0", 10));
    }

    @Test
    public void rejectsUnsortedBlocks() {
        assertDataError("not sorted by tStart",
                LiftFixtures.blk(200, 300, "chr1", 0, "+", 10), LiftFixtures.blk(100, 150, "chr1", 0, "+", 10));
        assertDataError("not sorted by tStart", OverlapPolicy.ERROR,
                LiftFixtures.blk(200, 300, "chr1", 0, "+", 10), LiftFixtures.blk(100, 150, "chr1", 0, "+", 10));
    }

    @Test
    public void rejectsOverlapUnderError() {
        assertDataError("overlapping blocks", OverlapPolicy.ERROR,
                LiftFixtures.CHR4_BLOCKS.get(0), LiftFixtures.CHR4_BLOCKS.get(1));
    }

    @Test
    public void adjacentBlocksAreNotOverlappingUnderError() {
        ContigIndex idx = ContigIndex.of("chr1", OverlapPolicy.ERROR,
                List.of(LiftFixtures.blk(100, 200, "chr1", 0, "+", 10), LiftFixtures.blk(200, 300, "chr1", 500, "+", 10)));
        Assert.assertEquals(2, idx.size());
    }

    @Test
    public void overlapPolicyParse() {
        Assert.assertEquals(OverlapPolicy.ERROR, OverlapPolicy.parse("error"));
        Assert.assertEquals(OverlapPolicy.BEST, OverlapPolicy.parse("BEST"));
        try {
            OverlapPolicy.parse("worst");
            Assert.fail();
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("worst"));
        }
    }

    @Test
    public void scoreAboveIntMaxLoads() {
        ContigIndex one = ContigIndex.of("chr1", OverlapPolicy.BEST,
                List.of(LiftFixtures.blk(100, 200, "chr1", 0, "+", 12341034620d)));
        Assert.assertEquals(1.234103462E10, one.score(0), 0);
        ContigIndex two = ContigIndex.of("chr1", OverlapPolicy.BEST, List.of(
                LiftFixtures.blk(100, 200, "chr1", 0, "+", 12341034620d),
                LiftFixtures.blk(150, 250, "chr1", 5000, "+", 12341034619d)));
        Assert.assertEquals(0, two.findPoint(175));
    }

    @Test
    public void largeBlock() {
        ContigIndex idx = ContigIndex.of("chr1", OverlapPolicy.ERROR,
                List.of(LiftFixtures.blk(0, 19400000, "chr1", 100, "+", 10)));
        Assert.assertEquals(0, idx.findPoint(10000000));
    }

    // ---- brute force comparison ----

    private static int brutePick(List<ChainBlock> blocks, List<Integer> candidates) {
        int best = UNMAPPED;
        double bestScore = Double.NEGATIVE_INFINITY;
        boolean tie = false;
        for (int i : candidates) {
            double s = blocks.get(i).score();
            if (s > bestScore) {
                best = i;
                bestScore = s;
                tie = false;
            } else if (s == bestScore) {
                tie = true;
            }
        }
        return tie ? AMBIGUOUS : best;
    }

    private static int[] toArray(List<Integer> l) {
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    @Test
    public void bruteForce() {
        Random rnd = new Random(4117);
        List<ChainBlock> randomBlocks = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            int s = 1 + rnd.nextInt(4800);
            int len = 1 + rnd.nextInt(300);
            randomBlocks.add(LiftFixtures.blk(s, s + len, "chr1", 1000 * i, rnd.nextBoolean() ? "+" : "-",
                    1 + rnd.nextInt(3)));
        }
        randomBlocks.sort((x, y) -> Integer.compare(x.tStart(), y.tStart()));

        compareToBruteForce(rnd, LiftFixtures.CHR1_BLOCKS, chr1);
        compareToBruteForce(rnd, randomBlocks, ContigIndex.of("chr1", OverlapPolicy.BEST, randomBlocks));
    }

    private void compareToBruteForce(Random rnd, List<ChainBlock> blocks, ContigIndex idx) {
        for (int n = 0; n < 10000; n++) {
            int s = 1 + rnd.nextInt(5000);
            int e = s + (rnd.nextInt(4) == 0 ? 0 : rnd.nextInt(400));
            String what = "(" + s + "," + e + ")";

            int expectedCount = 0;
            for (ChainBlock b : blocks) {
                if (b.tStart() < e && b.tEnd() >= s) {
                    expectedCount++;
                }
            }
            Assert.assertEquals("countBlocks" + what, expectedCount, idx.countBlocks(s, e));

            List<Integer> pointSet = new ArrayList<>();
            for (int i = 0; i < blocks.size(); i++) {
                if (blocks.get(i).tStart() < s && s <= blocks.get(i).tEnd()) {
                    pointSet.add(i);
                }
            }
            Assert.assertEquals("findPoint(" + s + ")", brutePick(blocks, pointSet), idx.findPoint(s));
            Assert.assertArrayEquals("allPoint(" + s + ")", toArray(pointSet), idx.allPoint(s));

            // the span [s0, e0) is (s, e), a quarter of the draws are junctions (e0 == s0)
            int s0 = s;
            int e0 = e;
            List<Integer> spanSet = new ArrayList<>();
            for (int i = 0; i < blocks.size(); i++) {
                ChainBlock b = blocks.get(i);
                boolean in = e0 > s0 ? (b.tStart() <= s0 && e0 <= b.tEnd()) : (b.tStart() < s0 && s0 < b.tEnd());
                if (in) {
                    spanSet.add(i);
                }
            }
            Assert.assertEquals("findSpan" + what, brutePick(blocks, spanSet), idx.findSpan(s0, e0));
            Assert.assertArrayEquals("allSpan" + what, toArray(spanSet), idx.allSpan(s0, e0));
        }
    }

    // ---- file loader ----

    @Test
    public void resolveMissingFile() {
        try {
            FileChainSource.resolve(tmp.getRoot().toPath().resolve("nope.gor").toString(), context);
            Assert.fail();
        } catch (GorResourceException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("nope.gor"));
        }
    }

    @Test
    public void resolveHeaderWithoutQScore() throws IOException {
        String path = writeChain("noscore.gor", "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\nchr1\t1\t2\tchr1\t1\t2\t+\n");
        try {
            FileChainSource.resolve(path, context);
            Assert.fail();
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("qScore"));
        }
    }

    @Test
    public void resolveMatchesHeaderNamesCaseInsensitively() throws IOException {
        String path = writeChain("mixed.gor", chainText("Chrom\ttstart\tTEND\tqchrom\tqStart\tqEnd\tqStrand\tQSCORE", "chr1",
                LiftFixtures.CHR1_BLOCKS));
        ResolvedChain rc = FileChainSource.resolve(path, context);
        Assert.assertEquals(new ChainColumns(2, 3, 4, 5, 6, 7), rc.cols());
        Assert.assertEquals(path, rc.path());
        Assert.assertNull(rc.nestedQuery());
        Assert.assertEquals(List.of(path), rc.usedFiles());
        Assert.assertEquals(context.getSession().getProjectContext().getRoot(), rc.root());
        Assert.assertFalse(rc.uniqueId().isEmpty());
    }

    @Test
    public void resolveStripsQuotes() throws IOException {
        String path = writeChr1Chain("quoted.gor");
        ResolvedChain rc = FileChainSource.resolve("'" + path + "'", context);
        Assert.assertEquals(path, rc.path());
    }

    @Test
    public void resolveRejectsNestedNor() throws IOException {
        String path = writeChr1Chain("nor.gor");
        try {
            FileChainSource.resolve("<(nor " + path + ")", context);
            Assert.fail();
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("gor query"));
        }
    }

    @Test
    public void nestedChainMatchesFileChain() throws Exception {
        String path = writeChr1Chain("nested.gor");
        ResolvedChain file = FileChainSource.resolve(path, context);
        ResolvedChain nested = FileChainSource.resolve("<(gor " + path + ")", context);
        Assert.assertEquals("gor " + path, nested.nestedQuery());
        Assert.assertTrue(nested.usedFiles().toString(), nested.usedFiles().contains(path));
        Assert.assertEquals("", nested.uniqueId());

        ChainSource fs = FileChainSource.cached(file, OverlapPolicy.ERROR, context);
        NestedChainSource ns = (NestedChainSource) FileChainSource.cached(nested, OverlapPolicy.ERROR, context);
        Assert.assertNotSame(ns, FileChainSource.cached(nested, OverlapPolicy.ERROR, context));

        List<ContigIndex> got = runConcurrently(8, () -> ns.contig("chr1"));
        Assert.assertEquals(1, ns.loadCount());
        for (ContigIndex c : got) {
            Assert.assertSame(got.get(0), c);
        }
        ContigIndex a = fs.contig("chr1");
        ContigIndex b = ns.contig("chr1");
        Assert.assertEquals(a.size(), b.size());
        for (int p = 0; p <= 1000; p++) {
            Assert.assertEquals("findPoint " + p, a.findPoint(p), b.findPoint(p));
            Assert.assertEquals("findSpan " + p, a.findSpan(p, p + 3), b.findSpan(p, p + 3));
            Assert.assertEquals("findSpan0 " + p, a.findSpan(p, p), b.findSpan(p, p));
            Assert.assertArrayEquals("allPoint " + p, a.allPoint(p), b.allPoint(p));
        }
        Assert.assertEquals(0, ns.contig("chrZ").size());
        Assert.assertEquals(1, ns.loadCount());
    }

    @Test
    public void nestedChainWithZeroStrandFailsOnFirstContigCall() throws IOException {
        List<ChainBlock> blocks = List.of(LiftFixtures.blk(100, 200, "chr1", 0, "0", 10));
        String path = writeChain("nested0.gor", chainText(HEADER, "chr1", blocks));
        ResolvedChain nested = FileChainSource.resolve("<(gor " + path + ")", context);
        ChainSource src = FileChainSource.cached(nested, OverlapPolicy.BEST, context);
        try {
            src.contig("chr1");
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("invalid strand '0'"));
        }
    }

    @Test
    public void nestedChainWithNoRowsHasNoBlocks() throws IOException {
        String path = writeChain("nestedempty.gor", HEADER + "\n");
        ResolvedChain nested = FileChainSource.resolve("<(gor " + path + ")", context);
        ChainSource src = FileChainSource.cached(nested, OverlapPolicy.BEST, context);
        try {
            src.contig("chr1");
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("no blocks"));
        }
    }

    @Test
    public void headerOnlyChainHasNoBlocks() throws IOException {
        String path = writeChain("empty.gor", HEADER + "\n");
        FileChainSource src = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.BEST, context);
        try {
            src.contig("chr1");
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("no blocks"));
        }
    }

    @Test
    public void contigAfterAllChainContigsIsEmptyNotAnError() throws IOException {
        String path = writeChr1Chain("onlychr1.gor");
        FileChainSource src = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.ERROR, context);
        ContigIndex z = src.contig("chrZ");
        Assert.assertEquals(0, z.size());
        Assert.assertEquals(UNMAPPED, z.findPoint(150));
        Assert.assertEquals(21, src.contig("chr1").size());
    }

    @Test
    public void contigBeforeChainContigsIsEmptyNotAnError() throws IOException {
        String path = writeChr1Chain("onlychr1b.gor");
        FileChainSource src = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.ERROR, context);
        Assert.assertEquals(0, src.contig("chr0").size());
    }

    @Test
    public void uniqueIdRequiresSourceMetadata() {
        DriverBackedFileReader noSource = new DriverBackedFileReader("") {
            @Override
            public DataSource resolveUrl(String url, boolean writeable) {
                return null;
            }
        };
        try {
            FileChainSource.uniqueId(noSource, "/some/chain.gor");
            Assert.fail();
        } catch (GorResourceException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("no unique id"));
        }
    }

    @Test
    public void sameFileGivesSameUniqueIdAndCachedInstance() throws IOException {
        String path = writeChr1Chain("same.gor");
        ResolvedChain r1 = FileChainSource.resolve(path, context);
        ResolvedChain r2 = FileChainSource.resolve(path, context);
        Assert.assertEquals(r1.uniqueId(), r2.uniqueId());
        Assert.assertSame(FileChainSource.cached(r1, OverlapPolicy.ERROR, context),
                FileChainSource.cached(r2, OverlapPolicy.ERROR, context));
    }

    @Test
    public void cachedKeysOnPolicy() throws IOException {
        String path = writeChr1Chain("policy.gor");
        ResolvedChain r = FileChainSource.resolve(path, context);
        Assert.assertSame(FileChainSource.cached(r, OverlapPolicy.BEST, context),
                FileChainSource.cached(r, OverlapPolicy.BEST, context));
        Assert.assertNotSame(FileChainSource.cached(r, OverlapPolicy.ERROR, context),
                FileChainSource.cached(r, OverlapPolicy.BEST, context));
    }

    @Test
    public void fileChainWithZeroStrandBlockFailsOnLoad() throws IOException {
        List<ChainBlock> blocks = List.of(
                LiftFixtures.blk(100, 200, "chr1", 0, "+", 10),
                LiftFixtures.blk(300, 400, "chr1", 0, "0", 10));
        String path = writeChain("zero.gor", chainText(HEADER, "chr1", blocks));
        FileChainSource src = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.BEST, context);
        try {
            src.contig("chr1");
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Liftover chain"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("invalid strand '0'"));
        }
        // failures are not cached: a second call fails the same way and loads again
        try {
            src.contig("chr1");
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("invalid strand '0'"));
        }
        Assert.assertEquals(2, src.loadCount());
    }

    @Test
    public void fileLoaderGivesSameIndexAsInMemory() throws IOException {
        String path = writeChain("both.gor", chainText(HEADER, "chr1", LiftFixtures.CHR1_BLOCKS)
                + chainText(HEADER, "chr4", LiftFixtures.CHR4_BLOCKS).substring(HEADER.length() + 1));
        FileChainSource src = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.BEST, context);
        ContigIndex loaded = src.contig("chr1");
        Assert.assertEquals(chr1.size(), loaded.size());
        for (int i = 0; i < chr1.size(); i++) {
            Assert.assertEquals(chr1.tStart(i), loaded.tStart(i));
            Assert.assertEquals(chr1.tEnd(i), loaded.tEnd(i));
            Assert.assertEquals(chr1.qChrom(i), loaded.qChrom(i));
            Assert.assertEquals(chr1.qStart(i), loaded.qStart(i));
            Assert.assertEquals(chr1.minus(i), loaded.minus(i));
            Assert.assertEquals(chr1.score(i), loaded.score(i), 0);
        }
        Assert.assertEquals(2, src.contig("chr4").size());
        Assert.assertEquals(1, src.contig("chr4").findPoint(175));
    }

    @Test
    public void concurrentFirstLoadLoadsOnce() throws Exception {
        String path = writeChr1Chain("concurrent.gor");
        FileChainSource src = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.ERROR, context);
        List<ContigIndex> got = runConcurrently(8, () -> src.contig("chr1"));
        for (ContigIndex c : got) {
            Assert.assertSame(got.get(0), c);
        }
        Assert.assertEquals(1, src.loadCount());
    }

    @Test
    public void loadErrorMessageNamesChain() throws IOException {
        // sanity for the message shape used by every load error
        String path = writeChain("gapped.gor", HEADER + "\nchr1\t100\t200\tchr1\t0\t50\t+\t10\n");
        FileChainSource src = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.BEST, context);
        try {
            src.contig("chr1");
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Liftover chain " + path));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("gapped block"));
        }
    }

    @Test
    public void rejectsNonFiniteScore() {
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertDataError("invalid score", LiftFixtures.blk(100, 200, "chr1", 0, "+", bad));
        }
    }

    @Test
    public void fileChainWithEmptyScoreFailsOnLoad() throws IOException {
        String path = writeChain("noscorevalue.gor", HEADER + "\nchr1\t100\t200\tchr1\t0\t100\t+\t\n");
        FileChainSource src = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.BEST, context);
        try {
            src.contig("chr1");
            Assert.fail();
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Liftover chain"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("invalid score"));
        }
    }

    private static final List<ChainBlock> BIG_SCORE_BLOCKS = List.of(
            LiftFixtures.blk(100, 200, "chr1", 0, "+", 12341034620d),
            LiftFixtures.blk(150, 250, "chr1", 5000, "+", 12341034619d));

    @Test
    public void scoreAboveIntMaxLoadsFromFileAndNestedChain() throws IOException {
        String path = writeChain("bigscore.gor", chainText(HEADER, "chr1", BIG_SCORE_BLOCKS));
        ResolvedChain resolved = FileChainSource.resolve(path, context);
        ResolvedChain nested = FileChainSource.resolve("<(gor " + path + ")", context);
        for (ChainSource src : List.of(new FileChainSource(resolved, OverlapPolicy.BEST, context),
                FileChainSource.cached(nested, OverlapPolicy.BEST, context))) {
            ContigIndex idx = src.contig("chr1");
            Assert.assertEquals(src.toString(), 0, idx.findPoint(175));
            Assert.assertEquals(src.toString(), 1.234103462E10, idx.score(0), 0);
            Assert.assertEquals(src.toString(), 1, idx.findPoint(225));
        }
    }

    @Test
    public void nestedChainWithTwoContigs() throws IOException {
        String path = writeChain("twocontigs.gor", chainText(HEADER, "chr1", LiftFixtures.CHR1_BLOCKS.subList(0, 3))
                + chainText(HEADER, "chr4", LiftFixtures.CHR4_BLOCKS).substring(HEADER.length() + 1));
        ChainSource fs = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.BEST, context);
        NestedChainSource ns = (NestedChainSource) FileChainSource.cached(
                FileChainSource.resolve("<(gor " + path + ")", context), OverlapPolicy.BEST, context);
        for (String c : List.of("chr1", "chr4")) {
            ContigIndex a = fs.contig(c);
            ContigIndex b = ns.contig(c);
            Assert.assertEquals(c, a.size(), b.size());
            Assert.assertTrue(c, b.size() > 0);
            for (int p = 0; p <= 450; p++) {
                Assert.assertEquals(c + " findPoint " + p, a.findPoint(p), b.findPoint(p));
                Assert.assertEquals(c + " findSpan " + p, a.findSpan(p, p + 3), b.findSpan(p, p + 3));
                Assert.assertArrayEquals(c + " allPoint " + p, a.allPoint(p), b.allPoint(p));
            }
        }
        Assert.assertEquals(3, ns.contig("chr1").size());
        Assert.assertEquals(1, ns.contig("chr4").findPoint(175));
        Assert.assertEquals(0, ns.contig("chr2").size());
        Assert.assertEquals(1, ns.loadCount());
    }

    // ---- fail-closed chain validation (final review) ----

    @Test
    public void rejectsNegativeTStart() {
        assertDataError("tStart < 0", new ChainBlock(-5, 95, "chr1", 0, 100, "+", 10));
    }

    @Test
    public void rejectsNegativeQStart() {
        assertDataError("qStart < 0", LiftFixtures.blk(100, 200, "chr1", -10, "+", 10));
    }

    @Test
    public void rejectsEmptyOrBlankQChrom() {
        assertDataError("empty qChrom", LiftFixtures.blk(100, 200, "", 0, "+", 10));
        assertDataError("empty qChrom", LiftFixtures.blk(100, 200, "  ", 0, "+", 10));
    }

    private void assertFileChainError(String name, String row, String expected) throws IOException {
        String path = writeChain(name, HEADER + "\n" + row + "\n");
        for (ChainSource src : List.of(new FileChainSource(FileChainSource.resolve(path, context),
                OverlapPolicy.BEST, context), FileChainSource.cached(
                FileChainSource.resolve("<(gor " + path + ")", context), OverlapPolicy.BEST, context))) {
            try {
                src.contig("chr1");
                Assert.fail("expected GorDataException containing '" + expected + "' for " + row.replace('\t', '|'));
            } catch (GorDataException e) {
                Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Liftover chain "));
                Assert.assertTrue(e.getMessage(), e.getMessage().contains(expected));
            }
        }
    }

    @Test
    public void fileChainRejectsEmptyCoordinateFields() throws IOException {
        assertFileChainError("emptytend.gor", "chr1\t100\t\tchr1\t0\t100\t+\t10", "empty tEnd");
        assertFileChainError("emptyqstart.gor", "chr1\t100\t200\tchr1\t\t100\t+\t10", "empty qStart");
        assertFileChainError("emptyqend.gor", "chr1\t100\t200\tchr1\t0\t\t+\t10", "empty qEnd");
        assertFileChainError("emptyqchrom.gor", "chr1\t100\t200\t\t0\t100\t+\t10", "empty qChrom");
        assertFileChainError("blankqchrom.gor", "chr1\t100\t200\t \t0\t100\t+\t10", "empty qChrom");
    }

    @Test
    public void fileChainRejectsSignOnlyCoordinateFields() throws IOException {
        // RowObj.colInt reads a lone '-' as 0, which would turn qStart '-' into a valid block at 0
        assertFileChainError("signtend.gor", "chr1\t100\t-\tchr1\t0\t100\t+\t10", "invalid tEnd '-'");
        assertFileChainError("signqstart.gor", "chr1\t100\t200\tchr1\t-\t100\t+\t10", "invalid qStart '-'");
        assertFileChainError("signqend.gor", "chr1\t100\t200\tchr1\t0\t - \t+\t10", "invalid qEnd ' - '");
    }

    @Test
    public void fileChainRejectsNegativeStarts() throws IOException {
        assertFileChainError("negqstart.gor", "chr1\t100\t200\tchr1\t-10\t90\t+\t10", "qStart < 0");
    }

    @Test
    public void fileChainWithNegativeTStartIsRejectedByTheReader() throws IOException {
        // the gor reader refuses a negative position before the chain loader sees the row, so the message has no
        // chain prefix; the loader's own tStart < 0 check covers every other way to build an index
        String path = writeChain("negtstart.gor", HEADER + "\nchr1\t-5\t95\tchr1\t0\t100\t+\t10\n");
        FileChainSource src = new FileChainSource(FileChainSource.resolve(path, context), OverlapPolicy.BEST, context);
        Assert.assertThrows(GorDataException.class, () -> src.contig("chr1"));
    }

    @Test
    public void nestedChainRejectsAContigThatReappears() throws IOException {
        String text = HEADER + "\nchr1\t100\t200\tchr1\t1000\t1100\t+\t10"
                + "\nchr2\t100\t200\tchr2\t1000\t1100\t+\t10"
                + "\nchr1\t300\t400\tchr1\t2000\t2100\t+\t10\n";
        String path = writeChain("reappear.gor", text);
        ChainSource src = FileChainSource.cached(FileChainSource.resolve("<(gor " + path + ")", context),
                OverlapPolicy.BEST, context);
        try {
            src.contig("chr2");
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertEquals("Liftover chain <(gor " + path + "): contig chr1 is not contiguous", e.getMessage());
        }
    }

    @Test
    public void nestedChainWithContigsInOneRunEachStillLoads() throws IOException {
        String text = HEADER + "\nchr1\t100\t200\tchr1\t1000\t1100\t+\t10"
                + "\nchr1\t300\t400\tchr1\t2000\t2100\t+\t10"
                + "\nchr2\t100\t200\tchr2\t1000\t1100\t+\t10\n";
        String path = writeChain("runs.gor", text);
        ChainSource src = FileChainSource.cached(FileChainSource.resolve("<(gor " + path + ")", context),
                OverlapPolicy.BEST, context);
        Assert.assertEquals(2, src.contig("chr1").size());
        Assert.assertEquals(1, src.contig("chr2").size());
    }
}
