package gorsat.Inputs;

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class UTestMeta {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void metaForGorFileWithNoMetaFile() {
        var results = TestUtils.runGorPipeLines("meta ../tests/data/gor/dbsnp_test.gor");

        Assert.assertEquals(23, results.length);
        Assert.assertEquals("ChrN\tPosN\tname\tvalue\n", results[0]);
    }

    @Test
    public void metaForGorFileWithMetaFile() throws IOException {
        var newFile = workDir.newFile("test.gorz");
        TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write " + newFile.getAbsolutePath());

        var results = TestUtils.runGorPipe("meta " + newFile.getAbsolutePath());

        Assert.assertTrue( results.contains("ChrN\tPosN\tname\tvalue\n"));
        Assert.assertTrue(results.contains("source.name"));
        Assert.assertTrue(results.contains("source.path"));
        Assert.assertTrue(results.contains("source.data.type"));
        Assert.assertTrue(results.contains("source.type"));
        Assert.assertTrue(results.contains("source.protocols"));
        Assert.assertTrue(results.contains("source.remote"));
        Assert.assertTrue(results.contains("source.supported"));
        Assert.assertTrue(results.contains("source.modified"));
        Assert.assertTrue(results.contains("source.id"));
        Assert.assertTrue(results.contains("file.path"));
        Assert.assertTrue(results.contains("file.name"));
        Assert.assertTrue(results.contains("file.type"));
        Assert.assertTrue(results.contains("file.suffix"));
        Assert.assertTrue(results.contains("file.size"));
        Assert.assertTrue(results.contains("file.modified"));
        Assert.assertTrue(results.contains("file.modified.utc"));
        Assert.assertTrue(results.contains("file.id"));
        Assert.assertTrue(results.contains("file.supports.index"));
        Assert.assertTrue(results.contains("file.supports.reference"));
        Assert.assertTrue(results.contains("file.reference"));
        Assert.assertTrue(results.contains("file.index"));
        Assert.assertTrue(results.contains("data.range"));
        Assert.assertTrue(results.contains("data.serial"));
        Assert.assertTrue(results.contains("data.line_count"));
        Assert.assertTrue(results.contains("data.schema"));
        Assert.assertTrue(results.contains("data.query"));
        Assert.assertTrue(results.contains("data.md5"));
        Assert.assertTrue(results.contains("data.tags"));
        Assert.assertTrue(results.contains("data.columns"));
    }

    @Test
    public void metaForUnsupportedDataType() throws IOException {
        var results = TestUtils.runGorPipe("meta ../tests/data/ref_mini/buildsplit.txt");

        Assert.assertTrue( results.contains("ChrN\tPosN\tname\tvalue\n"));
        Assert.assertTrue(results.contains("source.name"));
        Assert.assertTrue(results.contains("source.path"));
        Assert.assertTrue(results.contains("source.data.type"));
        Assert.assertTrue(results.contains("source.type"));
        Assert.assertTrue(results.contains("source.protocols"));
        Assert.assertTrue(results.contains("source.remote"));
        Assert.assertTrue(results.contains("source.supported"));
        Assert.assertTrue(results.contains("source.modified"));
        Assert.assertTrue(results.contains("source.id"));
        Assert.assertTrue(results.contains("file.path"));
        Assert.assertTrue(results.contains("file.name"));
        Assert.assertTrue(results.contains("file.type"));
        Assert.assertTrue(results.contains("file.suffix"));
        Assert.assertTrue(results.contains("file.size"));
        Assert.assertTrue(results.contains("file.modified"));
        Assert.assertTrue(results.contains("file.modified.utc"));
        Assert.assertTrue(results.contains("file.id"));
        Assert.assertTrue(results.contains("file.supports.index"));
        Assert.assertTrue(results.contains("file.supports.reference"));
        Assert.assertTrue(results.contains("file.reference"));
        Assert.assertTrue(results.contains("file.index"));
        Assert.assertFalse(results.contains("data.range"));
        Assert.assertFalse(results.contains("data.serial"));
        Assert.assertFalse(results.contains("data.line_count"));
        Assert.assertFalse(results.contains("data.schema"));
        Assert.assertFalse(results.contains("data.query"));
        Assert.assertFalse(results.contains("data.md5"));
        Assert.assertFalse(results.contains("data.tags"));
        Assert.assertFalse(results.contains("data.columns"));
    }
}
