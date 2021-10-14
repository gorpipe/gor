package gorsat;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class UTestWriteMeta {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void testGorMetaWrite() throws IOException {
        Path path = workDir.getRoot().toPath().resolve("gorfile.gorz");
        TestUtils.runGorPipe("gor -p chr21 ../tests/data/gor/genes.gor | calc c substr(gene_symbol,0,1) | write -card c " + path);
        Path metapath = path.getParent().resolve(path.getFileName().toString()+".meta");
        String metainfo = Files.lines(metapath).filter(l -> !l.startsWith("## QUERY:")).collect(Collectors.joining("\n"));
        Assert.assertEquals("Wrong results in meta file", "## RANGE: chr21\t9683190\tchr21\t48110675\n" +
                        "## MD5: 162498408aa03202fa1d2327b2cf9c4f\n" +
                        "## LINES: 669\n" +
                        "## CARDCOL[c]: A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,R,S,T,U,V,W,Y,Z",
                metainfo);
    }
}
