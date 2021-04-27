package gorsat;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UTestCalcFilePath {
    @Test
    public void testFilePath() throws IOException {
        var linkpath = Paths.get("test.tsv.link");
        try {
            TestUtils.runGorPipe("create xxx = norrows 1; nor [xxx] | calc f filepath([xxx]) | select f | write -noheader "+linkpath);
            String res = TestUtils.runGorPipe("nor test.tsv.link");
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
}
