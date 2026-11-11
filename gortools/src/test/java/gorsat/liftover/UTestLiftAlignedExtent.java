package gorsat.liftover;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static gorsat.liftover.LiftFixtures.blk;
import static gorsat.liftover.LiftFixtures.mapChain;

/**
 * {@link LiftEngine#alignedExtent}: how many target bases left and right of a lifted -var position stay inside the
 * same gap-free alignment (ENGKNOW-4207). Left and right are in target orientation, so a minus-strand lift swaps them
 * relative to the source.
 */
public class UTestLiftAlignedExtent {

    private static ChainSource chain(ChainBlock... blocks) {
        return mapChain(Map.of("chr1", ContigIndex.of("chr1", OverlapPolicy.ERROR, List.of(blocks))));
    }

    private static LiftEngine.Extent extent(ChainSource chain, int pos, int refLen) {
        return LiftEngine.alignedExtent(chain, "chr1", pos, refLen);
    }

    private static void assertExtent(int left, int right, LiftEngine.Extent actual) {
        Assert.assertEquals(new LiftEngine.Extent(left, right), actual);
    }

    /**
     * The CG-801 case: source base 28 is deleted in the target (one C fewer in a homopolymer), three bases left of
     * the variant at 31. Only the two bases 29 and 30 are on the variant's side of the indel.
     */
    @Test
    public void snvNextToBuildDeletionStopsAtTheIndel() {
        ChainSource c = chain(blk(0, 27, "chr1", 0, "+", 10), blk(28, 60, "chr1", 27, "+", 10));
        Assert.assertEquals(new LiftEngine.PointResult(LiftEngine.MAPPED, "chr1", 30, "+"),
                LiftEngine.liftVar(() -> c, "chr1", 31, 1, LiftFixtures.NO_POLICY));
        assertExtent(2, 29, extent(c, 31, 1));
    }

    @Test
    public void snvNextToBuildInsertionStopsAtTheIndel() {
        // the target has an extra base right before the lifted variant: the blocks abut in the source only
        ChainSource c = chain(blk(0, 30, "chr1", 0, "+", 10), blk(30, 60, "chr1", 31, "+", 10));
        assertExtent(0, 29, extent(c, 31, 1));
    }

    @Test
    public void singleBlockGivesTheDistanceToItsEdges() {
        ChainSource c = chain(blk(0, 60, "chr1", 100, "+", 10));
        assertExtent(30, 29, extent(c, 31, 1));
        assertExtent(0, 59, extent(c, 1, 1));
        assertExtent(59, 0, extent(c, 60, 1));
    }

    @Test
    public void longerRefIsAnchoredAtItsFirstBaseOnPlus() {
        ChainSource c = chain(blk(0, 60, "chr1", 100, "+", 10));
        // REF 30..32: the target pos is the lifted base 30, the REF bases 31 and 32 count on the right
        assertExtent(29, 30, extent(c, 30, 3));
    }

    @Test
    public void minusStrandSwapsLeftAndRight() {
        ChainSource c = chain(blk(0, 60, "chr1", 100, "-", 10));
        Assert.assertEquals(new LiftEngine.PointResult(LiftEngine.MAPPED, "chr1", 130, "-"),
                LiftEngine.liftVar(() -> c, "chr1", 31, 1, LiftFixtures.NO_POLICY));
        // source bases 32..60 are left of target 130, bases 1..30 right of it
        assertExtent(29, 30, extent(c, 31, 1));
    }

    @Test
    public void longerRefIsAnchoredAtItsLastBaseOnMinus() {
        ChainSource c = chain(blk(0, 60, "chr1", 100, "-", 10));
        // REF 30..32 lifts to target 129, which is source base 32
        Assert.assertEquals(129, LiftEngine.liftVar(() -> c, "chr1", 30, 3, LiftFixtures.NO_POLICY).pos());
        assertExtent(28, 31, extent(c, 30, 3));
    }

    @Test
    public void minusStrandBuildDeletionEndsUpOnTheRight() {
        // the CG-801 deletion of source base 28 on a minus-strand chain: the target is the reverse complement
        ChainSource c = chain(blk(0, 27, "chr1", 32, "-", 10), blk(28, 60, "chr1", 0, "-", 10));
        Assert.assertEquals(new LiftEngine.PointResult(LiftEngine.MAPPED, "chr1", 30, "-"),
                LiftEngine.liftVar(() -> c, "chr1", 31, 1, LiftFixtures.NO_POLICY));
        assertExtent(29, 2, extent(c, 31, 1));
    }

    @Test
    public void contiguousBlocksAreOneAlignment() {
        ChainSource plus = chain(blk(0, 30, "chr1", 0, "+", 10), blk(30, 60, "chr1", 30, "+", 10));
        assertExtent(30, 29, extent(plus, 31, 1));
        assertExtent(9, 50, extent(plus, 10, 1));
        ChainSource minus = chain(blk(0, 30, "chr1", 130, "-", 10), blk(30, 60, "chr1", 100, "-", 10));
        assertExtent(29, 30, extent(minus, 31, 1));
        assertExtent(50, 9, extent(minus, 10, 1));
    }

    @Test
    public void abuttingBlocksOnAnotherContigOrStrandAreNotJoined() {
        ChainSource otherContig = chain(blk(0, 30, "chr2", 0, "+", 10), blk(30, 60, "chr1", 30, "+", 10));
        assertExtent(0, 29, extent(otherContig, 31, 1));
        ChainSource otherStrand = chain(blk(0, 30, "chr1", 30, "-", 10), blk(30, 60, "chr1", 30, "+", 10));
        assertExtent(0, 29, extent(otherStrand, 31, 1));
    }

    @Test
    public void equalGapIsAlignedButAnotherChainEndsTheRun() {
        // one base unaligned on both sides (a substitution run, ENGKNOW-4208): the run continues across it
        ChainSource equalGap = chain(blk(0, 27, "chr1", 0, "+", 10), blk(28, 60, "chr1", 28, "+", 10));
        assertExtent(30, 29, extent(equalGap, 31, 1));
        assertExtent(9, 50, extent(equalGap, 10, 1));
        // the same blocks with different chain scores belong to two chains
        ChainSource twoChains = chain(blk(0, 30, "chr1", 0, "+", 10), blk(30, 60, "chr1", 30, "+", 20));
        assertExtent(0, 29, extent(twoChains, 31, 1));
    }

    @Test
    public void variantInsideAnEqualGapLiftsThroughTheGapBlock() {
        // source base 28 is unaligned on both sides; the gap block lifts it by offset and is part of the run
        ChainSource plus = chain(blk(0, 27, "chr1", 0, "+", 10), blk(28, 60, "chr1", 28, "+", 10));
        Assert.assertEquals(new LiftEngine.PointResult(LiftEngine.MAPPED, "chr1", 28, "+"),
                LiftEngine.liftVar(() -> plus, "chr1", 28, 1, LiftFixtures.NO_POLICY));
        assertExtent(27, 32, extent(plus, 28, 1));
        ChainSource minus = chain(blk(0, 27, "chr1", 33, "-", 10), blk(28, 60, "chr1", 0, "-", 10));
        // the target is 61 - p throughout: block 28..60 gives 1..32, the gap base 28 gives 33
        Assert.assertEquals(new LiftEngine.PointResult(LiftEngine.MAPPED, "chr1", 33, "-"),
                LiftEngine.liftVar(() -> minus, "chr1", 28, 1, LiftFixtures.NO_POLICY));
        assertExtent(32, 27, extent(minus, 28, 1));
    }

    @Test
    public void anotherChainBetweenTwoBlocksEndsTheRun() {
        // chain A abuts at 30 in both builds, but chain B's block starts between A's blocks in tStart order, so A's
        // blocks are not linked: only blocks next to each other in tStart order are (ENGKNOW-4208)
        ChainSource interleaved = mapChain(Map.of("chr1", ContigIndex.of("chr1", OverlapPolicy.BEST, List.of(
                blk(0, 30, "chr1", 0, "+", 10), blk(20, 40, "chr2", 500, "+", 5), blk(30, 60, "chr1", 30, "+", 10)))));
        assertExtent(14, 15, extent(interleaved, 45, 1));
        // an equal gap that holds another chain's block is not filled either
        ChainSource gapWithBlock = chain(blk(0, 27, "chr1", 0, "+", 10), blk(27, 28, "chr2", 500, "+", 5),
                blk(28, 60, "chr1", 28, "+", 10));
        assertExtent(2, 29, extent(gapWithBlock, 31, 1));
    }

    @Test
    public void unmappedVariantHasNoExtent() {
        ChainSource c = chain(blk(0, 27, "chr1", 0, "+", 10), blk(28, 60, "chr1", 27, "+", 10));
        Assert.assertEquals(LiftEngine.Extent.NONE, extent(c, 28, 1));
        Assert.assertEquals(LiftEngine.Extent.NONE, extent(c, 26, 3));
        Assert.assertEquals(LiftEngine.Extent.NONE, LiftEngine.alignedExtent(c, "chr9", 31, 1));
    }
}
