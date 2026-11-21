package gorsat.Script;

import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import gorsat.process.PipeOptions;
import gorsat.process.TestSessionFactory;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.session.GorSession;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ENGKNOW-4267: the query server deadlocked when many creates computed file signatures in
 * parallel. The fingerprint was computed inside {@code ConcurrentHashMap.computeIfAbsent}, and
 * loading a dictionary for it ran a parallel stream on the ForkJoin common pool while the map's
 * bin lock was held. The pool workers, running other creates, blocked on that lock, so the
 * nested stream never finished.
 *
 * <p>The starvation itself depends on thread timing, so these tests check the invariant that
 * prevents it: no signature map lock is held while a fingerprint is being computed. A file
 * reader parks the first signature call; a second caller for the same key must not wait for it.
 */
public class UTestSignatureDeadlock {

    private static final int WAIT_SECONDS = 10;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private Path workPath;
    private Path gord;

    private final CountDownLatch firstCallEntered = new CountDownLatch(1);
    private final CountDownLatch releaseFirstCall = new CountDownLatch(1);
    private final AtomicBoolean firstCall = new AtomicBoolean(true);

    /** Parks the first dictionary signature call until the test releases it. */
    private class ParkingFileReader extends DriverBackedFileReader {
        ParkingFileReader(String securityContext, String commonRoot, long queryTime) {
            super(securityContext, commonRoot, queryTime);
        }

        @Override
        public String getDictionarySignature(String dictionary, String[] tags) throws IOException {
            if (firstCall.compareAndSet(true, false)) {
                firstCallEntered.countDown();
                try {
                    if (!releaseFirstCall.await(60, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("first signature call was never released");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
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

        Files.writeString(workPath.resolve("pn1.gor"), "#Chrom\tPos\nchr1\t1\n");
        Files.writeString(workPath.resolve("pn2.gor"), "#Chrom\tPos\nchr1\t2\n");
        gord = workPath.resolve("varcalls.gord");
        Files.writeString(gord, "pn1.gor\tpn1\npn2.gor\tpn2\n");
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
        String call();
    }

    /**
     * Runs {@code call} twice concurrently. The first run parks inside the file reader; the second
     * must finish while the first is still parked, i.e. it must not wait on a lock the first holds.
     */
    private void assertSecondCallerNotBlocked(SignatureCall call) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(call::call);
            Assert.assertTrue("first signature call never reached the file reader",
                    firstCallEntered.await(WAIT_SECONDS, TimeUnit.SECONDS));

            Future<String> second = executor.submit(call::call);
            String secondSignature;
            try {
                secondSignature = second.get(WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                Assert.fail("Second signature call waited on a lock held while another thread computed "
                        + "the fingerprint; on the query server this starves the ForkJoin pool (ENGKNOW-4267)");
                return;
            } finally {
                releaseFirstCall.countDown();
            }

            Assert.assertEquals(first.get(WAIT_SECONDS, TimeUnit.SECONDS), secondSignature);
            Assert.assertEquals("later calls reuse the cached signature", secondSignature, call.call());
        } finally {
            releaseFirstCall.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void fileFingerPrintDoesNotHoldMapLockWhileComputing() throws Exception {
        try (var session = createSession()) {
            var engine = ScriptEngineFactory.create(session.getGorContext());
            var dictFile = "#gordict#" + gord + "#gortags#pn1";
            assertSecondCallerNotBlocked(() -> engine.fileFingerPrint(dictFile, session));
        }
    }

    @Test
    public void fileSignatureDoesNotHoldMapLockWhileComputing() throws Exception {
        try (var session = createSession()) {
            var engine = ScriptEngineFactory.create(session.getGorContext());
            var query = "gor -s PN -f pn1,pn2 " + gord;
            var usedFiles = List.of("#gordict#" + gord + "#gortags#pn1,pn2");
            assertSecondCallerNotBlocked(() -> engine.getFileSignatureAndUpdateSignatureMap(session, query, usedFiles));
        }
    }
}
