package gorsat;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.nanohttpd.protocols.http.TestFileHttpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UTestDictionary {
    private TestFileHttpServer server;
    private int port;
    private Path dbsnpgord = Paths.get("dbsnp.gord");
    private Path dbsnplocalgord = Paths.get("dbsnplocal.gord");
    private Path dbsnpremotegord = Paths.get("dbsnp.gord");
    private Path gordRemotePath;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {

        server = new TestFileHttpServer(getFreePort(), workDir.getRoot());
        port = server.getPort();

        Path gordPath = workDir.getRoot().toPath().resolve(dbsnpgord);
        Path dbsnppath = Paths.get("../tests/data/gor/dbsnp_test.gorz");
        Files.copy(dbsnppath,workDir.getRoot().toPath().resolve("dbsnp.gorz"));
        Files.writeString(gordPath,"dbsnp.gorz\tu\n");

        Path gordLocalPath = workDir.getRoot().toPath().resolve(dbsnplocalgord);
        Files.writeString(gordLocalPath, dbsnppath.toAbsolutePath() +"\tu\n");

        gordRemotePath = workDir.getRoot().toPath().resolve(dbsnpremotegord);
        Files.writeString(gordRemotePath,"http://localhost:"+port+"/dbsnp.gorz\tuu\n");
    }

    @After
    public void tearDown() {
        server.stop();
        server = null;
    }

    @Test
    public void testReadRemoteDictionaryWithServerFileReader() {
        String[] args = {"gor http://localhost:"+port+"/"+dbsnpgord,"-gorroot", workDir.getRoot().getPath()};
        int count = TestUtils.runGorPipeCount(args, true);
        Assert.assertEquals("", 48, count);
    }

    @Test
    public void testReadRemoteDictionaryRemotePathWithServerFileReader() {
        String[] args = {"gor http://localhost:"+port+"/"+dbsnpremotegord,"-gorroot", workDir.getRoot().getPath()};
        int count = TestUtils.runGorPipeCount(args, true);
        Assert.assertEquals("", 48, count);
    }

    @Test
    public void testReadRemoteDictionaryLocalPathWithServerFileReader() {
        String[] args = {"gor http://localhost:"+port+"/"+dbsnplocalgord,"-gorroot", workDir.getRoot().getPath()};
        boolean failed = false;
        try {
            TestUtils.runGorPipeCount(args, true);
        } catch(Exception e) {
            failed = true;
        }
        Assert.assertTrue("Local absolutepath in remote dictionary not alloed", failed);
    }

    @Test
    public void testReadDictionaryRemotePathWithServerFileReader() throws IOException {
        String[] args = {"gor "+gordRemotePath.getFileName().toString(),"-gorroot", workDir.getRoot().getPath()};
        int count = TestUtils.runGorPipeCount(args, true);
        Assert.assertEquals("", 48, count);
    }

    private int getFreePort() throws IOException {
        int freePort;
        try(ServerSocket ss = new ServerSocket(0)) {
            ss.setReuseAddress(true);
            freePort = ss.getLocalPort();
        }
        if (freePort > 0) {
            return freePort;
        } else {
            throw new IOException("Could not find a free port");
        }
    }
}
