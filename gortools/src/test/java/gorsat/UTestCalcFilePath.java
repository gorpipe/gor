package gorsat;

import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.util.DataUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UTestCalcFilePath {
    @Test
    public void testFilePath() throws IOException {
        var linkpath = Paths.get(DataUtil.toLinkFile("test", DataType.TSV));
        try {
            TestUtils.runGorPipe("create xxx = norrows 1; nor [xxx] | calc f filepath([xxx]) | select f | write -noheader "+linkpath);
            String res = TestUtils.runGorPipe("nor " + linkpath);
            Assert.assertEquals("Wrong result from link file", "ChromNOR\tPosNOR\tRowNum\nchrN\t0\t0\n", res);
        } finally {
            Files.deleteIfExists(linkpath);
        }
    }

    @Test
    public void testFileContent() {
        String res = TestUtils.runGorPipe("create xxx = norrows 1; norrows 1 | calc f filecontent([xxx]) | select f");
        Assert.assertEquals("Wrong result from link file", "ChromNOR\tPosNOR\tf\nchrN\t0\t0\n", res);
    }

    @Test
    public void testFileInfoNotExists() {
        String res = TestUtils.runGorPipe("norrows 1 | calc f fileinfo('notexists.txt') | select f");
        var resplit = res.split(",");
        Assert.assertEquals("Wrong result from link file", "false", resplit[resplit.length-1].trim());
    }

    @Test
    public void testFileInfoColumn() {
        String res = TestUtils.runGorPipe("norrows 1 | calc m '../tests/data/gor/genes.gor' | calc f fileinfo(m) | select f");
        var resplit = res.split(",");
        Assert.assertEquals("Wrong result from link file", "true", resplit[resplit.length-1].trim());
    }
}
