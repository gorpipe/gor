package gorsat;

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
        var subquery = "create xxx = ../tests/data/gor/dbsnp_test.gorz; gor [xxx]";
        var subpath = workDir.getRoot().toPath().resolve("subquery.gorq");
        Files.writeString(subpath, subquery);
        var query = "def sim = 'sim'; include "+subpath.toAbsolutePath()+"; create yyy = gor [xxx] | group chrom -count; gor [yyy] | top 1";
        var result = TestUtils.runGorPipe(query);
        Assert.assertEquals("Chrom\tbpStart\tbpStop\tallCount\n" +
                "chr1\t0\t250000000\t2\n", result);
    }
}
