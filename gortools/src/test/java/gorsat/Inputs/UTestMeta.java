package gorsat.Inputs;

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;

public class UTestMeta {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void metaForGorFileWithNoMetaFile() {
        var results = TestUtils.runGorPipeLines("meta ../tests/data/gor/dbsnp_test.gor");

        Assert.assertEquals(23, results.length);
        Assert.assertEquals("ChrN\tPosN\tsource\tname\tvalue\n", results[0]);
    }

    @Test
    public void metaForGorFileWithMetaFile() throws IOException {
        var newFile = workDir.newFile("test.gorz");
        TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write " + newFile.getAbsolutePath());

        var results = TestUtils.runGorPipe("meta " + newFile.getAbsolutePath());

        Assert.assertTrue( results.contains("ChrN\tPosN\tsource\tname\tvalue\n"));
        Assert.assertTrue(results.contains("SOURCE\tNAME"));
        Assert.assertTrue(results.contains("SOURCE\tPATH"));
        Assert.assertTrue(results.contains("SOURCE\tDATA_TYPE"));
        Assert.assertTrue(results.contains("SOURCE\tTYPE"));
        Assert.assertTrue(results.contains("SOURCE\tPROTOCOLS"));
        Assert.assertTrue(results.contains("SOURCE\tREMOTE"));
        Assert.assertTrue(results.contains("SOURCE\tSUPPORTED"));
        Assert.assertTrue(results.contains("SOURCE\tMODIFIED"));
        Assert.assertTrue(results.contains("SOURCE\tID"));
        Assert.assertTrue(results.contains("FILE\tPATH"));
        Assert.assertTrue(results.contains("FILE\tNAME"));
        Assert.assertTrue(results.contains("FILE\tTYPE"));
        Assert.assertTrue(results.contains("FILE\tSUFFIX"));
        Assert.assertTrue(results.contains("FILE\tSIZE"));
        Assert.assertTrue(results.contains("FILE\tMODIFIED"));
        Assert.assertTrue(results.contains("FILE\tMODIFIED_UTC"));
        Assert.assertTrue(results.contains("FILE\tID"));
        Assert.assertTrue(results.contains("FILE\tSUPPORTS_INDEX"));
        Assert.assertTrue(results.contains("FILE\tSUPPORTS_REFERENCE"));
        Assert.assertTrue(results.contains("FILE\tREFERENCE"));
        Assert.assertTrue(results.contains("FILE\tINDEX"));
        Assert.assertTrue(results.contains("GOR\tRANGE"));
        Assert.assertTrue(results.contains("GOR\tSERIAL"));
        Assert.assertTrue(results.contains("GOR\tLINE_COUNT"));
        Assert.assertTrue(results.contains("GOR\tSCHEMA"));
        Assert.assertTrue(results.contains("GOR\tQUERY"));
        Assert.assertTrue(results.contains("GOR\tMD5"));
        Assert.assertTrue(results.contains("GOR\tTAGS"));
        Assert.assertTrue(results.contains("GOR\tCOLUMNS"));
    }

    @Test
    public void metaForGorLinkFileWithMetaFile() throws IOException {
        var newFile = workDir.newFile("test.gorz");
        TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write " + newFile.getAbsolutePath());

        var newLinkFile = workDir.newFile("test.gor.link");
        Files.writeString(newLinkFile.toPath(), newFile.getAbsolutePath());

        var results = TestUtils.runGorPipe("meta " + newLinkFile.getAbsolutePath());

        Assert.assertTrue( results.contains("ChrN\tPosN\tsource\tname\tvalue\n"));
        Assert.assertTrue(results.contains("SOURCE\tNAME"));
        Assert.assertTrue(results.contains("SOURCE\tPATH"));
        Assert.assertTrue(results.contains("SOURCE\tDATA_TYPE"));
        Assert.assertTrue(results.contains("SOURCE\tTYPE"));
        Assert.assertTrue(results.contains("SOURCE\tPROTOCOLS"));
        Assert.assertTrue(results.contains("SOURCE\tREMOTE"));
        Assert.assertTrue(results.contains("SOURCE\tSUPPORTED"));
        Assert.assertTrue(results.contains("SOURCE\tMODIFIED"));
        Assert.assertTrue(results.contains("SOURCE\tID"));
        Assert.assertTrue(results.contains("FILE\tPATH"));
        Assert.assertTrue(results.contains("FILE\tNAME"));
        Assert.assertTrue(results.contains("FILE\tTYPE"));
        Assert.assertTrue(results.contains("FILE\tSUFFIX"));
        Assert.assertTrue(results.contains("FILE\tSIZE"));
        Assert.assertTrue(results.contains("FILE\tMODIFIED"));
        Assert.assertTrue(results.contains("FILE\tMODIFIED_UTC"));
        Assert.assertTrue(results.contains("FILE\tID"));
        Assert.assertTrue(results.contains("FILE\tSUPPORTS_INDEX"));
        Assert.assertTrue(results.contains("FILE\tSUPPORTS_REFERENCE"));
        Assert.assertTrue(results.contains("FILE\tREFERENCE"));
        Assert.assertTrue(results.contains("FILE\tINDEX"));
        Assert.assertTrue(results.contains("GOR\tRANGE"));
        Assert.assertTrue(results.contains("GOR\tSERIAL"));
        Assert.assertTrue(results.contains("GOR\tLINE_COUNT"));
        Assert.assertTrue(results.contains("GOR\tSCHEMA"));
        Assert.assertTrue(results.contains("GOR\tQUERY"));
        Assert.assertTrue(results.contains("GOR\tMD5"));
        Assert.assertTrue(results.contains("GOR\tTAGS"));
        Assert.assertTrue(results.contains("GOR\tCOLUMNS"));
    }

    @Test
    public void metaForUnsupportedDataType() throws IOException {
        var results = TestUtils.runGorPipe("meta ../tests/data/ref_mini/buildsplit.txt");

        Assert.assertTrue( results.contains("ChrN\tPosN\tsource\tname\tvalue\n"));
        Assert.assertTrue(results.contains("SOURCE\tNAME"));
        Assert.assertTrue(results.contains("SOURCE\tPATH"));
        Assert.assertTrue(results.contains("SOURCE\tDATA_TYPE"));
        Assert.assertTrue(results.contains("SOURCE\tTYPE"));
        Assert.assertTrue(results.contains("SOURCE\tPROTOCOLS"));
        Assert.assertTrue(results.contains("SOURCE\tREMOTE"));
        Assert.assertTrue(results.contains("SOURCE\tSUPPORTED"));
        Assert.assertTrue(results.contains("SOURCE\tMODIFIED"));
        Assert.assertTrue(results.contains("SOURCE\tID"));
        Assert.assertTrue(results.contains("FILE\tPATH"));
        Assert.assertTrue(results.contains("FILE\tNAME"));
        Assert.assertTrue(results.contains("FILE\tTYPE"));
        Assert.assertTrue(results.contains("FILE\tSUFFIX"));
        Assert.assertTrue(results.contains("FILE\tSIZE"));
        Assert.assertTrue(results.contains("FILE\tMODIFIED"));
        Assert.assertTrue(results.contains("FILE\tMODIFIED_UTC"));
        Assert.assertTrue(results.contains("FILE\tID"));
        Assert.assertTrue(results.contains("FILE\tSUPPORTS_INDEX"));
        Assert.assertTrue(results.contains("FILE\tSUPPORTS_REFERENCE"));
        Assert.assertTrue(results.contains("FILE\tREFERENCE"));
        Assert.assertTrue(results.contains("FILE\tINDEX"));
    }
}
