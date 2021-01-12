package gorsat;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UTestGorDictionaryFolder {
    @Test
    public void testWriteToFolder() {
        Path heypath = Paths.get("folder.gord");
        try {
            TestUtils.runGorPipe("gor -p chr21 ../tests/data/gor/genes.gor | write -d "+heypath);
            TestUtils.runGorPipe("gor -p chr22 ../tests/data/gor/genes.gor | write -d "+heypath);
            String results = TestUtils.runGorPipe("gor "+heypath+" | group chrom -count");
            Assert.assertEquals("Wrong results in write folder", "Chrom\tbpStart\tbpStop\tallCount\n" +
                    "chr21\t0\t100000000\t669\n" +
                    "chr22\t0\t100000000\t1127\n", results);
        } finally {
            try {
                Files.delete(heypath);
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    @Test
    public void testCreateWriteFolder() {
        Path heypath = Paths.get("folder.gord");
        try {
            String results = TestUtils.runGorPipe("create a = gor -p chr21 ../tests/data/gor/genes.gor | write -d " + heypath +
                    "; create b = gor -p chr22 ../tests/data/gor/genes.gor | write -d " + heypath +
                    "; gor " + heypath + " | group chrom -count");
            Assert.assertEquals("Wrong results in write folder", "Chrom\tbpStart\tbpStop\tallCount\n" +
                    "chr21\t0\t100000000\t669\n" +
                    "chr22\t0\t100000000\t1127\n", results);
        } finally {
            try {
                Files.delete(heypath);
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    @Test
    public void testPgorWriteFolder() {
        Path heypath = Paths.get("folder.gord");
        try {
            String results = TestUtils.runGorPipe("create a = pgor ../tests/data/gor/genes.gor | write -d " + heypath +
                    "; gor " + heypath + " | group chrom -count");
            Assert.assertEquals("Wrong results in write folder", "Chrom\tbpStart\tbpStop\tallCount\n" +
                    "chr1\t0\t250000000\t4747\n" +
                    "chr10\t0\t150000000\t2011\n" +
                    "chr11\t0\t150000000\t2982\n" +
                    "chr12\t0\t150000000\t2524\n" +
                    "chr13\t0\t150000000\t1165\n" +
                    "chr14\t0\t150000000\t2032\n" +
                    "chr15\t0\t150000000\t1864\n" +
                    "chr16\t0\t100000000\t2161\n" +
                    "chr17\t0\t100000000\t2659\n" +
                    "chr18\t0\t100000000\t992\n" +
                    "chr19\t0\t100000000\t2748\n" +
                    "chr2\t0\t250000000\t3507\n" +
                    "chr20\t0\t100000000\t1183\n" +
                    "chr21\t0\t100000000\t669\n" +
                    "chr22\t0\t100000000\t1127\n" +
                    "chr3\t0\t200000000\t2648\n" +
                    "chr4\t0\t200000000\t2245\n" +
                    "chr5\t0\t200000000\t2523\n" +
                    "chr6\t0\t200000000\t2557\n" +
                    "chr7\t0\t200000000\t2514\n" +
                    "chr8\t0\t150000000\t2107\n" +
                    "chr9\t0\t150000000\t2156\n" +
                    "chrM\t0\t20000\t37\n" +
                    "chrX\t0\t200000000\t2138\n" +
                    "chrY\t0\t100000000\t480\n", results);
        } finally {
            try {
                Files.delete(heypath);
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
