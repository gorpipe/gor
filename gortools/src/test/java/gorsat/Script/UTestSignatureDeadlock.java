package gorsat.Script;

import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import gorsat.process.PipeOptions;
import gorsat.process.TestSessionFactory;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.session.GorSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ENGKNOW-4267: the query server deadlocked when many creates computed file signatures in
 * parallel. The fingerprint was computed inside {@code ConcurrentHashMap.computeIfAbsent}, and
 * loading a dictionary for it ran a parallel stream on the ForkJoin common pool while the map's
 * bin lock was held. While waiting for that stream, a pool worker ran other creates' signature
 * tasks; those needed the same bin lock ("Recursive update") or a lock held by another such
 * worker (hang).
 *
 * <p>The failure itself depends on thread timing, so these tests check the invariants that
 * prevent it, using a file reader that parks the first signature call:
 * <ul>
 *   <li>a key that shares the map bin of a key being computed does not wait for it;</li>
 *   <li>the computing thread can re-enter its own key (as a stolen task would) without
 *       deadlocking on itself.</li>
 * </ul>
 * They also check that concurrent callers for the same key share one computation, so
 * identical creates load a dictionary once, and that callers waiting for it on ForkJoin workers
 * do not make the pool add threads.
 */
public class UTestSignatureDeadlock {

    private static final int WAIT_SECONDS = 10;

    /** "Aa" and "BB" have the same String hash, so keys that differ only by them share a map bin. */
    private static final String TAG_A = "Aa";
    private static final String TAG_B = "BB";

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private Path workPath;
    private Path gord;

    private final CountDownLatch firstCallEntered = new CountDownLatch(1);
    private final CountDownLatch releaseFirstCall = new CountDownLatch(1);
    private final AtomicBoolean firstCall = new AtomicBoolean(true);
    private final AtomicInteger signatureCalls = new AtomicInteger();

    /** Runs inside the first signature call, on the computing thread, before it parks. */
    private volatile Runnable insideFirstCall = () -> {};

    /** Makes the first signature call fail after it is released. */
    private volatile boolean failFirstCall = false;

    /** Makes the first signature call return null after it is released. */
    private volatile boolean nullFirstCall = false;

    /** Parks the first dictionary signature call until the test releases it; counts all calls. */
    private class ParkingFileReader extends DriverBackedFileReader {
        ParkingFileReader(String securityContext, String commonRoot, long queryTime) {
            super(securityContext, commonRoot, queryTime);
        }

        @Override
        public String getDictionarySignature(String dictionary, String[] tags) throws IOException {
            signatureCalls.incrementAndGet();
            if (firstCall.compareAndSet(true, false)) {
                insideFirstCall.run();
                firstCallEntered.countDown();
                try {
                    if (!releaseFirstCall.await(60, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("first signature call was never released");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                if (failFirstCall) {
                    throw new IOException("first signature call fails");
                }
                if (nullFirstCall) {
                    return null;
                }
            }
            return super.getDictionarySignature(dictionary, tags);
        }
    }

    @Before
    public void setUp() throws IOException {
        workPath = workDir.getRoot().toPath();
        GorPipeCommands.register();
        GorInputSources.register();

        Assert.assertEquals(TAG_A.hashCode(), TAG_B.hashCode());
        Files.writeString(workPath.resolve("a.gor"), "#Chrom\tPos\nchr1\t1\n");
        Files.writeString(workPath.resolve("b.gor"), "#Chrom\tPos\nchr1\t2\n");
        gord = workPath.resolve("varcalls.gord");
        Files.writeString(gord, "a.gor\t" + TAG_A + "\nb.gor\t" + TAG_B + "\n");
    }

    @After
    public void tearDown() {
        releaseFirstCall.countDown();
    }

    private GorSession createSession() throws IOException {
        var cachePath = workPath.resolve("cache");
        Files.createDirectories(cachePath);
        var options = new PipeOptions();
        options.gorRoot_$eq(workPath.toString());
        options.cacheDir_$eq(cachePath.toString());
        options.requestId_$eq("ENGKNOW-4267");
        return new TestSessionFactory(options, null, false, null, null) {
            @Override
            public DriverBackedFileReader createFileReader(String gorRoot, String securityContext, boolean server,
                                                           List<String> writeLocations, long startTime) {
                return new ParkingFileReader(securityContext, gorRoot, startTime);
            }
        }.create();
    }

    private interface SignatureCall {
        String call(String tag);
    }

    /** A call running on its own thread, so the test can see whether it is waiting. */
    private static class Call {
        final FutureTask<String> task;
        final Thread thread;

        Call(SignatureCall call, String tag) {
            task = new FutureTask<>(() -> call.call(tag));
            thread = new Thread(task, "signature-" + tag);
            thread.setDaemon(true);
            thread.start();
        }

        String get() throws Exception {
            try {
                return task.get(WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throw new AssertionError("signature call failed", e.getCause());
            }
        }
    }

    private Call startFirstCall(SignatureCall call, String tag) throws InterruptedException {
        var first = new Call(call, tag);
        Assert.assertTrue("first signature call never reached the file reader",
                firstCallEntered.await(WAIT_SECONDS, TimeUnit.SECONDS));
        return first;
    }

    /**
     * While the first call computes the {@link #TAG_A} key, a call for the {@link #TAG_B} key, which
     * shares its map bin, must finish without waiting for it.
     */
    private void assertSameBinKeyNotBlocked(SignatureCall call) throws Exception {
        var first = startFirstCall(call, TAG_A);
        var second = new Call(call, TAG_B);
        try {
            second.task.get(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("A signature call waited on a map lock held while another thread computed a "
                    + "different key; on the query server this deadlocks the ForkJoin pool (ENGKNOW-4267)");
        } finally {
            releaseFirstCall.countDown();
        }
        Assert.assertNotEquals(first.get(), second.get());
    }

    /**
     * A second call for the key being computed waits for that computation instead of repeating
     * it, and both get the same signature.
     */
    private void assertSameKeyComputedOnce(SignatureCall call) throws Exception {
        var first = startFirstCall(call, TAG_A);
        var second = new Call(call, TAG_A);
        try {
            awaitWaitingForSignature(second);
        } finally {
            releaseFirstCall.countDown();
        }
        var signature = first.get();
        Assert.assertEquals(signature, second.get());
        Assert.assertEquals("concurrent callers for one key should share one dictionary load",
                1, signatureCalls.get());
        Assert.assertEquals("later calls reuse the cached signature", signature, call.call(TAG_A));
        Assert.assertEquals(1, signatureCalls.get());
    }

    /**
     * When the computation a caller waits for fails, the failure is not cached: the waiting caller
     * computes the signature itself.
     */
    private void assertWaiterRetriesAfterFailure(SignatureCall call) throws Exception {
        failFirstCall = true;
        var first = startFirstCall(call, TAG_A);
        var second = new Call(call, TAG_A);
        try {
            awaitWaitingForSignature(second);
        } finally {
            releaseFirstCall.countDown();
        }
        AssertionError failure = null;
        try {
            first.get();
        } catch (AssertionError e) {
            failure = e;
        }
        Assert.assertNotNull("first signature call should fail", failure);
        Assert.assertNotNull(failure.getCause());
        var signature = second.get();
        Assert.assertNotNull(signature);
        Assert.assertEquals(2, signatureCalls.get());
        Assert.assertEquals("the retried signature is cached", signature, call.call(TAG_A));
        Assert.assertEquals(2, signatureCalls.get());
    }

    /**
     * When the computation a caller waits for returns null, nothing is cached: the waiting caller
     * computes the signature itself, as computeIfAbsent would.
     */
    private void assertWaiterRecomputesAfterNull(SignatureCall call) throws Exception {
        nullFirstCall = true;
        var first = startFirstCall(call, TAG_A);
        var second = new Call(call, TAG_A);
        try {
            awaitWaitingForSignature(second);
        } finally {
            releaseFirstCall.countDown();
        }
        Assert.assertNull(first.get());
        var signature = second.get();
        Assert.assertNotNull(signature);
        Assert.assertEquals(2, signatureCalls.get());
        Assert.assertEquals("the recomputed signature is cached", signature, call.call(TAG_A));
        Assert.assertEquals(2, signatureCalls.get());
    }

    /**
     * Callers waiting on ForkJoin workers must not make the pool add a spare thread per blocked
     * worker; the common pool throws RejectedExecutionException past 256 spares.
     */
    private void assertWaitersDoNotGrowPool(SignatureCall call) throws Exception {
        var pool = new ForkJoinPool(2);
        try {
            // The computing call runs outside the pool, so every pool worker is a waiting caller.
            var first = startFirstCall(call, TAG_A);
            var tasks = new ArrayList<ForkJoinTask<String>>();
            for (int i = 0; i < 20; i++) {
                tasks.add(pool.submit(() -> call.call(TAG_A)));
            }
            int maxPoolSize = 0;
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
            while (System.nanoTime() < deadline) {
                maxPoolSize = Math.max(maxPoolSize, pool.getPoolSize());
                Thread.sleep(5);
            }
            releaseFirstCall.countDown();
            var signature = first.get();
            for (var task : tasks) {
                Assert.assertEquals(signature, task.get(WAIT_SECONDS, TimeUnit.SECONDS));
            }
            Assert.assertEquals(1, signatureCalls.get());
            Assert.assertTrue("waiting callers made the ForkJoin pool add threads: " + maxPoolSize,
                    maxPoolSize <= 2);
        } finally {
            releaseFirstCall.countDown();
            pool.shutdownNow();
        }
    }

    /**
     * Waits until the call is parked waiting for another thread's signature computation.  Fails if
     * it finishes first, i.e. it computed the signature itself instead of waiting.
     */
    private static void awaitWaitingForSignature(Call call) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (call.task.isDone()) {
                Assert.fail("second signature call finished without waiting for the first");
            }
            for (var frame : call.thread.getStackTrace()) {
                if (frame.getClassName().endsWith("BaseScriptExecutionEngine$Signature")
                        && frame.getMethodName().equals("await")) {
                    return;
                }
            }
            Thread.sleep(5);
        }
        Assert.fail("second signature call never waited for the first");
    }

    /**
     * The computing thread asks for its own key again before finishing, as a ForkJoin worker does
     * when it runs another create's task while it waits. That must not deadlock on itself.
     */
    private void assertReentrantSameKeyCompletes(SignatureCall call) throws Exception {
        var nested = new AtomicInteger();
        var nestedSignature = new String[1];
        insideFirstCall = () -> {
            nested.incrementAndGet();
            nestedSignature[0] = call.call(TAG_A);
        };
        releaseFirstCall.countDown();
        var first = new Call(call, TAG_A);
        String signature;
        try {
            signature = first.task.get(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("A signature call re-entered from inside its own computation waited on itself");
            return;
        }
        Assert.assertEquals(1, nested.get());
        Assert.assertEquals(signature, nestedSignature[0]);
    }

    private SignatureCall fingerPrintCall(GorSession session) {
        var engine = ScriptEngineFactory.create(session.getGorContext());
        return tag -> engine.fileFingerPrint("#gordict#" + gord + "#gortags#" + tag, session);
    }

    private SignatureCall fileSignatureCall(GorSession session) {
        var engine = ScriptEngineFactory.create(session.getGorContext());
        return tag -> engine.getFileSignatureAndUpdateSignatureMap(session,
                "gor -s PN -f " + tag + " " + gord,
                List.of("#gordict#" + gord + "#gortags#" + tag));
    }

    @Test
    public void fileFingerPrintDoesNotBlockKeyInSameBin() throws Exception {
        try (var session = createSession()) {
            assertSameBinKeyNotBlocked(fingerPrintCall(session));
        }
    }

    @Test
    public void fileSignatureDoesNotBlockKeyInSameBin() throws Exception {
        try (var session = createSession()) {
            assertSameBinKeyNotBlocked(fileSignatureCall(session));
        }
    }

    @Test
    public void fileFingerPrintComputesSameKeyOnce() throws Exception {
        try (var session = createSession()) {
            assertSameKeyComputedOnce(fingerPrintCall(session));
        }
    }

    @Test
    public void fileSignatureComputesSameKeyOnce() throws Exception {
        try (var session = createSession()) {
            assertSameKeyComputedOnce(fileSignatureCall(session));
        }
    }

    @Test
    public void fileFingerPrintWaiterRetriesAfterFailure() throws Exception {
        try (var session = createSession()) {
            assertWaiterRetriesAfterFailure(fingerPrintCall(session));
        }
    }

    @Test
    public void fileSignatureWaiterRetriesAfterFailure() throws Exception {
        try (var session = createSession()) {
            assertWaiterRetriesAfterFailure(fileSignatureCall(session));
        }
    }

    @Test
    public void fileFingerPrintWaiterRecomputesAfterNull() throws Exception {
        try (var session = createSession()) {
            assertWaiterRecomputesAfterNull(fingerPrintCall(session));
        }
    }

    @Test
    public void fileFingerPrintWaitersDoNotGrowPool() throws Exception {
        try (var session = createSession()) {
            assertWaitersDoNotGrowPool(fingerPrintCall(session));
        }
    }

    @Test
    public void fileSignatureWaitersDoNotGrowPool() throws Exception {
        try (var session = createSession()) {
            assertWaitersDoNotGrowPool(fileSignatureCall(session));
        }
    }

    @Test
    public void fileFingerPrintReentrantSameKeyCompletes() throws Exception {
        try (var session = createSession()) {
            assertReentrantSameKeyCompletes(fingerPrintCall(session));
        }
    }

    @Test
    public void fileSignatureReentrantSameKeyCompletes() throws Exception {
        try (var session = createSession()) {
            assertReentrantSameKeyCompletes(fileSignatureCall(session));
        }
    }
}
