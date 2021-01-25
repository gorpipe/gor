package gorsat;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class UTestGorDictionaryFolder {
    public void deleteFolder(Path folderpath) {
        try {
            Files.walk(folderpath).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    // ignore
                }
            });
        } catch (IOException e) {
            // ignore
        }
    }

    @Test
    public void testWriteToFolder() {
        Path folderpath = Paths.get("folder1.gord");
        try {
            TestUtils.runGorPipe("gor -p chr21 ../tests/data/gor/genes.gor | write -d "+folderpath);
            TestUtils.runGorPipe("gor -p chr22 ../tests/data/gor/genes.gor | write -d "+folderpath);
            String results = TestUtils.runGorPipe("gor "+folderpath+" | group chrom -count");
            Assert.assertEquals("Wrong results in write folder", "Chrom\tbpStart\tbpStop\tallCount\n" +
                    "chr21\t0\t100000000\t669\n" +
                    "chr22\t0\t100000000\t1127\n", results);
        } finally {
            deleteFolder(folderpath);
        }
    }

    @Test
    public void testCreateWriteFolder() {
        Path folderpath = Paths.get("folder2.gord");
        try {
            String results = TestUtils.runGorPipe("create a = gor -p chr21 ../tests/data/gor/genes.gor | write -d " + folderpath +
                    "; create b = gor -p chr22 ../tests/data/gor/genes.gor | write -d " + folderpath +
                    "; gor " + folderpath + " | group chrom -count");
            Assert.assertEquals("Wrong results in write folder", "Chrom\tbpStart\tbpStop\tallCount\n" +
                    "chr21\t0\t100000000\t669\n" +
                    "chr22\t0\t100000000\t1127\n", results);
        } finally {
            deleteFolder(folderpath);
        }
    }

    @Test
    public void testWritePassThrough() {
        Path path = Paths.get("gorfile.gorz");
        String results = TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | top 1 | write -p " + path);
        Assert.assertEquals("Wrong results in write folder", "Chrom\tgene_start\tgene_end\tGene_Symbol\n" +
                "chr1\t11868\t14412\tDDX11L1\n" , results);
        try {
            Files.delete(path);
        } catch (IOException e) {
            // Ignore
        }
    }

    @Test
    public void testCreateWrite() {
        Path path = Paths.get("gorfile.gorz");
        String results = TestUtils.runGorPipe("create a = gor -p chr21 ../tests/data/gor/genes.gor | write " + path + "; gor [a] | group chrom -count");
        Assert.assertEquals("Wrong results in write folder", "Chrom\tbpStart\tbpStop\tallCount\n" +
                "chr21\t0\t100000000\t669\n" , results);
        try {
            Files.delete(path);
        } catch (IOException e) {
            // Ignore
        }
    }

    @Test
    public void testPgorWriteFolder() {
        Path folderpath = Paths.get("folder3.gord");
        try {
            String results = TestUtils.runGorPipe("create a = pgor ../tests/data/gor/genes.gor | write -d " + folderpath +
                    "; gor [a] | group chrom -count");
                    //"; gor [a] | group chrom -count");
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
            deleteFolder(folderpath);
        }
    }
}
