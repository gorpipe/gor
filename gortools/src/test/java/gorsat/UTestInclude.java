package gorsat;

import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.util.DataUtil;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;

public class UTestInclude {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void testInclude() throws IOException {
        var subquery = "def hey = 'sim';\n def sim2 = 'sim';\n create xxx = ../tests/data/gor/dbsnp_test.gorz\n| skip -1;\n gor [xxx]";
        var subpath = workDir.getRoot().toPath().resolve(DataUtil.toFile("subquery", DataType.GORQ));
        Files.writeString(subpath, subquery);
        var query = "def sim = sim;\n include "+subpath.toAbsolutePath()+";\n create yyy = gor [xxx] \n| group chrom -count;\n gor [yyy] | top 1";
        var result = TestUtils.runGorPipe(query);
        Assert.assertEquals("Chrom\tbpStart\tbpStop\tallCount\n" +
                "chr1\t0\t250000000\t2\n", result);
    }
}
