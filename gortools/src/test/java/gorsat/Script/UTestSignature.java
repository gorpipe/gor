package gorsat.Script;

import gorsat.TestUtils;
import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import gorsat.process.PipeOptions;
import gorsat.process.TestSessionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class UTestSignature {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private Path workPath;

    @Before
    public void setUp() {
        workPath = workDir.getRoot().toPath();

        GorPipeCommands.register();
        GorInputSources.register();
    }

    @Test
    public void testQuerySignatureAfterUpdate() throws IOException, InterruptedException {
        var cachePath = workPath.resolve("cache");
        Files.createDirectory(cachePath);

        var dataPath1 = workPath.resolve("data1.gor");
        Files.writeString(dataPath1, "#Chrom\tPos\nchr1\t1\n");

        var query = "create abc = gor " + dataPath1 + " | top 10; gor [abc]";

        var res = TestUtils.runGorPipe(query, workPath.toString(), cachePath.toString(), false, "", null).split("\n");
        Assert.assertEquals(2, res.length);

        Files.writeString(dataPath1, "chr1\t2\n", StandardOpenOption.APPEND);

        res = TestUtils.runGorPipe(query, workPath.toString(), cachePath.toString(), false, "", null).split("\n");
        Assert.assertEquals(3, res.length);
    }

    @Test
    public void testQuerySignatureAfterUpdateWithLink() throws IOException, InterruptedException {
        var cachePath = workPath.resolve("cache");
        Files.createDirectory(cachePath);

        var dataPath1 = workPath.resolve("data1.gor");
        Files.writeString(dataPath1, "#Chrom\tPos\nchr1\t1\n");

        var linkFile = workPath.resolve("link1.gor.link");
        String linkContentV1 = """
                        ## VERSION = 1
                        #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
                        data1.gor\t1734304890790\t\t1\t
                        """;
        Files.writeString(linkFile, linkContentV1);

        var query = "create abc = gor " + linkFile + " | top 10; gor [abc]";

        var res = TestUtils.runGorPipe(query, workPath.toString(), cachePath.toString(), false, "", null).split("\n");
        Assert.assertEquals(2, res.length);

        Files.writeString(dataPath1, "chr1\t2\n", StandardOpenOption.APPEND);

        res = TestUtils.runGorPipe(query, workPath.toString(), cachePath.toString(), false, "", null).split("\n");
        Assert.assertEquals(3, res.length);
    }

    @Test
    public void testQuerySignatureAfterUpdateWithDynamicLink() throws IOException, InterruptedException {
        var cachePath = workPath.resolve("cache");
        Files.createDirectory(cachePath);

        var dataPath1 = workPath.resolve("data1.gor");
        Files.writeString(dataPath1, "#Chrom\tPos\nchr1\t1\n");

        var firstlinkFile = workPath.resolve("first.gor.link");
        Files.writeString(firstlinkFile, """
                        ## VERSION = 1
                        #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
                        second.gor.link\t1734304890790\t\t1\t
                        """);

        var secondlinkFile = workPath.resolve("second.gor.link");
        Files.writeString(secondlinkFile, """
                        ## VERSION = 1
                        #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
                        data1.gor\t1734304890790\t\t1\t
                        """);

        var query = "create abc = gor " + firstlinkFile + " | top 10; gor [abc]";

        var res = TestUtils.runGorPipe(query, workPath.toString(), cachePath.toString(), false, "", null).split("\n");
        Assert.assertEquals(2, res.length);

        var dataPath2 = workPath.resolve("data2.gor");
        Files.writeString(dataPath2, "#Chrom\tPos\nchr2\t2\nchr2\t3\n");

        Files.writeString(secondlinkFile, "data2.gor\t1734404890790\t\t1\t", StandardOpenOption.APPEND);

        res = TestUtils.runGorPipe(query, workPath.toString(), cachePath.toString(), false, "", null).split("\n");
        Assert.assertEquals(3, res.length);
    }

    @Test
    public void testSignatureAfterUpdate() throws IOException, InterruptedException {
        var cachePath = workPath.resolve("cache");
        Files.createDirectory(cachePath);

        var dataPath1 = workPath.resolve("data1.gor");
        Files.writeString(dataPath1, "#Chrom\tPos\nchr1\t1\n");

        var query = "gor " + dataPath1 + " | top 10";

        var signatureAfter = "";
        var signatureBefore = "";

        var options = new PipeOptions();
        options.gorRoot_$eq(workDir.toString());
        options.cacheDir_$eq(cachePath.toString());
        options.requestId_$eq("test");
        var factory = new TestSessionFactory(options, null, false, null, null);

        try (var session = factory.create()) {
            var engine = ScriptEngineFactory.create(session.getGorContext());

            var usedFiles = engine.getUsedFiles(query, session);
            signatureBefore = engine.getFileSignatureAndUpdateSignatureMap(session, query, usedFiles);

            Assert.assertNotNull(signatureBefore);
        }

        Files.writeString(dataPath1, "chr1\t2\n", StandardOpenOption.APPEND);

        try (var session = factory.create()) {
            var engine = ScriptEngineFactory.create(session.getGorContext());

            var usedFiles = engine.getUsedFiles(query, session);
            signatureAfter = engine.getFileSignatureAndUpdateSignatureMap(session, query, usedFiles);

            Assert.assertNotNull(signatureAfter);
        }

        Assert.assertNotEquals(signatureBefore, signatureAfter);
    }

    @Test
    public void testSignatureWithVersionedLinkFile() throws IOException, InterruptedException {
        var cachePath = workPath.resolve("cache");
        Files.createDirectory(cachePath);

        var dataPath1 = workPath.resolve("data1.gor");
        Files.writeString(dataPath1, "#Chrom\tPos\nchr1\t1\n");

        var linkFile = workPath.resolve("link1.gor.link");
        String linkContentV1 = """
                        ## VERSION = 1
                        #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
                        data1.gor\t1734304890790\t\t1\t
                        """;
        Files.writeString(linkFile, linkContentV1);

        var query = "gor " + linkFile + " | top 10";

        var signatureAfter = "";
        var signatureBefore = "";

        var options = new PipeOptions();
        options.gorRoot_$eq(workDir.toString());
        options.cacheDir_$eq(cachePath.toString());
        options.requestId_$eq("test");
        var factory = new TestSessionFactory(options, null, false, null, null);

        try (var session = factory.create()) {
            var engine = ScriptEngineFactory.create(session.getGorContext());

            var usedFiles = engine.getUsedFiles(query, session);
            signatureBefore = engine.getFileSignatureAndUpdateSignatureMap(session, query, usedFiles);

            Assert.assertNotNull(signatureBefore);
        }

        Files.writeString(dataPath1, "chr1\t2\n", StandardOpenOption.APPEND);

        try (var session = factory.create()) {
            var engine = ScriptEngineFactory.create(session.getGorContext());

            var usedFiles = engine.getUsedFiles(query, session);
            signatureAfter = engine.getFileSignatureAndUpdateSignatureMap(session, query, usedFiles);

            Assert.assertNotNull(signatureAfter);
        }

        Assert.assertNotEquals(signatureBefore, signatureAfter);
    }


}
