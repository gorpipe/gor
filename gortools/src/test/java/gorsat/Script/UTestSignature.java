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
import java.nio.file.attribute.FileTime;
import java.util.Arrays;

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

    private String extractSingleValue(String output) {
        var lines = Arrays.stream(output.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toArray(String[]::new);

        Assert.assertEquals("Expected a header row and one data row", 2, lines.length);
        var values = lines[1].split("\t");
        return values[values.length - 1];
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
    public void testQuerySignatureAfterUpdateWithMetaVirtualRelation() throws IOException {
        var cachePath = workPath.resolve("cache");
        Files.createDirectory(cachePath);

        var dataPath = workPath.resolve("data_meta.gor");
        Files.writeString(dataPath, "#Chrom\tPos\nchr1\t1\n");

        var query = "create meta_size = meta " + dataPath
                + " | where source='FILE' and name='SIZE' | rename value file_size | select file_size; nor [meta_size]";

        var before = extractSingleValue(TestUtils.runGorPipe(query, workPath.toString(), cachePath.toString(), false, "", null));
        Assert.assertEquals(String.valueOf(Files.size(dataPath)), before);

        Files.writeString(dataPath, "chr1\t2\n", StandardOpenOption.APPEND);

        var after = extractSingleValue(TestUtils.runGorPipe(query, workPath.toString(), cachePath.toString(), false, "", null));
        Assert.assertEquals("META virtual relation should be invalidated when the source file size changes",
                String.valueOf(Files.size(dataPath)), after);
        Assert.assertNotEquals("META virtual relation should not reuse stale cached results", before, after);
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
        Files.setLastModifiedTime(dataPath1, FileTime.fromMillis(Files.getLastModifiedTime(dataPath1).toMillis() + 1000));

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
