package gorsat;

import org.gorpipe.gor.table.TableHeader;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UTestGorDictionaryFolder {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    static String GENE_GROUP_CHROM = "Chrom\tbpStart\tbpStop\tallCount\n" +
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
            "chrY\t0\t100000000\t480\n";

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
        Path folderpath = workDir.getRoot().toPath().resolve("folder1.gord");
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
        Path folderpath = workDir.getRoot().toPath().resolve("folder2.gord");
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
    @Ignore("Passthrough not supported yet")
    public void testWritePassThrough() {
        Path path = workDir.getRoot().toPath().resolve("gorfile.gorz");
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
        Path path = workDir.getRoot().toPath().resolve("gorfile.gorz");
        String results = TestUtils.runGorPipe("create a = gor -p chr21 ../tests/data/gor/genes.gor | write " + path + "; gor "+path+" | group chrom -count");
        Assert.assertEquals("Wrong results in write folder", "Chrom\tbpStart\tbpStop\tallCount\n" +
                "chr21\t0\t100000000\t669\n" , results);
        try {
            Files.delete(path);
        } catch (IOException e) {
            // Ignore
        }
    }

    @Test
    public void testGorCardinalityColumn() throws IOException {
        Path path = workDir.getRoot().toPath().resolve("gorfile.gorz");
        TestUtils.runGorPipe("gor -p chr21 ../tests/data/gor/genes.gor | calc c substr(gene_symbol,0,1) | write -card c " + path);
        Path metapath = path.getParent().resolve("gorfile.gorz.meta");
        try(Stream<String> stream = Files.lines(metapath).filter(l -> !l.startsWith("## QUERY:"))) {
            String metainfo = stream.collect(Collectors.joining("\n"));
            Assert.assertEquals("Wrong results in meta file", "## RANGE: chr21\t9683190\tchr21\t48110675\n" +
                            "## MD5: 162498408aa03202fa1d2327b2cf9c4f\n" +
                            "## LINES: 669\n" +
                            "## CARDCOL[c]: A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,R,S,T,U,V,W,Y,Z",
                    metainfo);
            try {
                Files.delete(path);
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    @Test
    public void testPgorDictFolderNoWrite() throws IOException {
        var workDirPath = workDir.getRoot().toPath();
        var cache = workDirPath.resolve("result_cache");
        Files.createDirectory(cache);
        var query = "create a = pgor -gordfolder dict ../tests/data/gor/genes.gor; gor [a] | group chrom -count";
        var results = TestUtils.runGorPipe(query,"-cachedir",cache.toString());
        Assert.assertEquals("Wrong results in write folder", GENE_GROUP_CHROM, results);
    }

    @Test
    public void testParallelPgorDictFolderNoWrite() throws IOException {
        var workDirPath = workDir.getRoot().toPath();
        var cache = workDirPath.resolve("result_cache");
        Files.createDirectory(cache);
        var query = "create a = parallel -gordfolder -parts <(norrows 2) <(pgor ../tests/data/gor/genes.gor | rownum | where mod(rownum,2)=#{col:RowNum}); gor [a] | group chrom -count";
        var results = TestUtils.runGorPipe(query,"-cachedir",cache.toString());
        Assert.assertEquals("Wrong results in write folder", GENE_GROUP_CHROM, results);
    }

    @Test
    public void testParallelPgorDictFolderWrite() throws IOException {
        var workDirPath = workDir.getRoot().toPath();
        var cache = workDirPath.resolve("result_cache");
        Files.createDirectory(cache);
        var genes = "../tests/data/gor/genes.gor";
        var genespath = Paths.get(genes);
        var genesdest = workDirPath.resolve(genespath.getFileName());
        Files.copy(genespath,genesdest);
        var query = "create a = parallel -parts <(norrows 2) <(pgor genes.gor | rownum | calc modrow mod(rownum,2) | where modrow=#{col:RowNum} | write mu.gord/#{fork}_#{CHROM}_#{BPSTART}_#{BPSTOP}.gorz -f modrow -card modrow); gor mu.gord/thedict.gord | group chrom -count";
        var results = TestUtils.runGorPipe(query,"-gorroot",workDirPath.toString(),"-cachedir",cache.toString());
        Assert.assertEquals("Wrong results in write folder", GENE_GROUP_CHROM, results);
    }

    @Test
    public void testParallelPgorDictFolderWriteServerMode() throws IOException {
        var workDirPath = workDir.getRoot().toPath();
        var cache = workDirPath.resolve("result_cache");
        Files.createDirectory(cache);
        var genes = "../tests/data/gor/genes.gor";
        var genespath = Paths.get(genes);
        var genesdest = workDirPath.resolve(genespath.getFileName());
        Files.copy(genespath,genesdest);
        var query = "create a = parallel -parts <(norrows 2) <(pgor genes.gor | rownum | calc modrow mod(rownum,2) | where modrow=#{col:RowNum} | write test/mu.gord/#{fork}_#{CHROM}_#{BPSTART}_#{BPSTOP}.gorz -f modrow -card modrow); norrows 1";
        var args = new String[] {query,"-gorroot",workDirPath.toString(),"-cachedir",cache.toString()};
        var results = TestUtils.runGorPipeCount(args, true);
        Assert.assertEquals("Wrong results in write folder", 1, results);
    }

    @Test
    public void testPgorWriteFolder() throws IOException {
        var workDirPath = workDir.getRoot().toPath();
        var folderpath = workDirPath.resolve("folder.gord");
        var cachedir = workDirPath.resolve("result_cache");
        Files.createDirectory(cachedir);
        try {
            String results = TestUtils.runGorPipe("create a = pgor ../tests/data/gor/genes.gor | write -d " + folderpath +
                    "; gor [a] | group chrom -count","-cachedir",cachedir.toString());
                    //"; gor [a] | group chrom -count");
            Assert.assertEquals("Wrong results in write folder", GENE_GROUP_CHROM, results);
        } finally {
            deleteFolder(folderpath);
        }
    }

    @Test
    public void testPgorWriteGord() {
        Path gordpath = workDir.getRoot().toPath().resolve("my.gord").resolve("my.gord");
        try {
            var query = "create a = pgor ../tests/data/gor/genes.gor | top 1 | write -d " + gordpath.getParent() + " " + gordpath +
                    "; gor ../tests/data/gor/genes.gor | group chrom -count";
            String results = TestUtils.runGorPipe(query);
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
            deleteFolder(gordpath);
        }
    }

    @Test
    public void testPgorWriteFolderWithCardinality() throws IOException {
        Path folderpath = workDir.getRoot().toPath().resolve("folder.gord");
        try {
            TestUtils.runGorPipe("create a = pgor ../tests/data/gor/genes.gor | where chrom = 'chrM' | calc c substr(gene_symbol,0,1) | write -card c -d " + folderpath +
                    "; gor [a] | group chrom -count");
            String thedict = Files.readString(folderpath.resolve("thedict.gord"));
            Assert.assertEquals("Wrong results in dictionary",
                    "## SERIAL = 0\n" +
                            "## COLUMNS = Chrom,gene_start,gene_end,Gene_Symbol,c\n" +
                            "# filepath\talias\tstartchrom\tstartpos\tendchrom\tendpos\ttags\n" +
                            "dd02aed74a26d4989a91f3619ac8dc20.gorz\t1\tchrM\t576\tchrM\t15955\tJ,M\n",
                    thedict);
        } finally {
            deleteFolder(folderpath);
        }
    }

    @Test
    public void testPartGorWrite() throws IOException {
        var workDirPath = workDir.getRoot().toPath();
        var bucketFile = workDirPath.resolve("buckets.tsv");
        Path variantBucketFile1 = workDirPath.resolve("variants1.gor");
        Path variantBucketFile2 = workDirPath.resolve("variants2.gor");
        Path variantBucketFile3 = workDirPath.resolve("variants3.gor");
        Path variantBucketFile4 = workDirPath.resolve("variants4.gor");
        Path variantBucketFile5 = workDirPath.resolve("variants5.gor");
        Path variantBucketFile6 = workDirPath.resolve("variants6.gor");
        Path variantBucketFile7 = workDirPath.resolve("variants7.gor");
        Path variantBucketFile8 = workDirPath.resolve("variants8.gor");
        Path variantBucketFile9 = workDirPath.resolve("variants9.gor");
        Path variantBucketFile10 = workDirPath.resolve("variants10.gor");
        Path variantBucketFile11 = workDirPath.resolve("variants11.gor");
        Path variantBucketFile12 = workDirPath.resolve("variants12.gor");
        var variantDictFile = workDirPath.resolve("variants.gord");
        var pnpath = workDirPath.resolve("pns.txt");
        Files.writeString(pnpath,"a\nb\nc\nd\ne\nf\ng\nh\ni\nj\nk\nl\n");
        Files.writeString(bucketFile, "a\t1\nb\t1\nc\t1\nd\t1\ne\t2\nf\t2\ng\t2\nh\t2\ni\t3\nj\t3\nk\t3\nl\t3\n");
        Files.writeString(variantBucketFile1,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t1\t0\ta\n"+
                "chr1\t2\tG\tC\t1\t0\ta\n"+
                "chr1\t3\tA\tC\t1\t0\ta\n"+
                "chr1\t4\tG\tC\t1\t0\ta\n");
        Files.writeString(variantBucketFile2,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t1\t0\tb\n"+
                "chr1\t2\tG\tC\t1\t2\tb\n"+
                "chr1\t3\tA\tC\t1\t0\tb\n"+
                "chr1\t4\tG\tC\t1\t2\tb\n");
        Files.writeString(variantBucketFile3,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t1\t1\tc\n"+
                "chr1\t2\tG\tC\t1\t0\tc\n"+
                "chr1\t3\tA\tC\t1\t1\tc\n"+
                "chr1\t4\tG\tC\t1\t0\tc\n");
        Files.writeString(variantBucketFile4,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t1\t1\td\n"+
                "chr1\t2\tG\tC\t1\t1\td\n"+
                "chr1\t3\tA\tC\t1\t1\td\n"+
                "chr1\t4\tG\tC\t1\t1\td\n");
        Files.writeString(variantBucketFile5,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t2\t0\te\n"+
                "chr1\t2\tG\tC\t2\t0\te\n"+
                "chr1\t3\tA\tC\t2\t0\te\n"+
                "chr2\t4\tG\tC\t2\t0\te\n");
        Files.writeString(variantBucketFile6,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t2\t1\tf\n"+
                "chr1\t2\tG\tC\t2\t2\tf\n"+
                "chr1\t3\tA\tC\t2\t1\tf\n"+
                "chr2\t4\tG\tC\t2\t2\tf\n");
        Files.writeString(variantBucketFile7,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t2\t0\tg\n"+
                "chr1\t2\tG\tC\t2\t2\tg\n"+
                "chr1\t3\tA\tC\t2\t0\tg\n"+
                "chr2\t4\tG\tC\t2\t2\tg\n");
        Files.writeString(variantBucketFile8,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t2\t2\th\n"+
                "chr1\t2\tG\tC\t2\t1\th\n"+
                "chr1\t3\tA\tC\t2\t2\th\n"+
                "chr2\t4\tG\tC\t2\t1\th\n");
        Files.writeString(variantBucketFile9,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t3\t0\ti\n"+
                "chr1\t2\tG\tC\t3\t1\ti\n"+
                "chr1\t3\tA\tC\t3\t1\ti\n"+
                "chr2\t4\tG\tC\t3\t0\ti\n");
        Files.writeString(variantBucketFile10,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t3\t1\tj\n"+
                "chr1\t2\tG\tC\t3\t2\tj\n"+
                "chr1\t3\tA\tC\t3\t1\tj\n"+
                "chr2\t4\tG\tC\t3\t0\tj\n");
        Files.writeString(variantBucketFile11,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t3\t2\tk\n"+
                "chr1\t2\tG\tC\t3\t0\tk\n"+
                "chr1\t3\tA\tC\t3\t2\tk\n"+
                "chr2\t4\tG\tC\t3\t0\tk\n");
        Files.writeString(variantBucketFile12,"Chrom\tpos\tref\talt\tbucket\tvalues\n"+
                "chr1\t1\tA\tC\t3\t2\tl\n"+
                "chr1\t2\tG\tC\t3\t1\tl\n"+
                "chr1\t3\tA\tC\t3\t2\tl\n"+
                "chr2\t4\tG\tC\t3\t1\tl\n");
        Files.writeString(variantDictFile,"variants1.gor\ta\n"+
                "variants2.gor\tb\n"+
                "variants3.gor\tc\n"+
                "variants4.gor\td\n"+
                "variants5.gor\te\n"+
                "variants6.gor\tf\n"+
                "variants7.gor\tg\n"+
                "variants8.gor\th\n"+
                "variants9.gor\ti\n"+
                "variants10.gor\tj\n"+
                "variants11.gor\tk\n"+
                "variants12.gor\tl\n");

        var expected = "Chrom\tpos\tref\talt\tbucket\tvalues\tSource\n" +
                "chr1\t1\tA\tC\t1\t0\ta\n" +
                "chr1\t1\tA\tC\t1\t0\tb\n" +
                "chr1\t1\tA\tC\t1\t1\tc\n" +
                "chr1\t1\tA\tC\t1\t1\td\n" +
                "chr1\t1\tA\tC\t2\t0\te\n" +
                "chr1\t1\tA\tC\t2\t1\tf\n" +
                "chr1\t1\tA\tC\t2\t0\tg\n" +
                "chr1\t1\tA\tC\t2\t2\th\n" +
                "chr1\t1\tA\tC\t3\t0\ti\n" +
                "chr1\t1\tA\tC\t3\t1\tj\n" +
                "chr1\t1\tA\tC\t3\t2\tk\n" +
                "chr1\t1\tA\tC\t3\t2\tl\n" +
                "chr1\t2\tG\tC\t1\t0\ta\n" +
                "chr1\t2\tG\tC\t1\t2\tb\n" +
                "chr1\t2\tG\tC\t1\t0\tc\n" +
                "chr1\t2\tG\tC\t1\t1\td\n" +
                "chr1\t2\tG\tC\t2\t0\te\n" +
                "chr1\t2\tG\tC\t2\t2\tf\n" +
                "chr1\t2\tG\tC\t2\t2\tg\n" +
                "chr1\t2\tG\tC\t2\t1\th\n" +
                "chr1\t2\tG\tC\t3\t1\ti\n" +
                "chr1\t2\tG\tC\t3\t2\tj\n" +
                "chr1\t2\tG\tC\t3\t0\tk\n" +
                "chr1\t2\tG\tC\t3\t1\tl\n" +
                "chr1\t3\tA\tC\t1\t0\ta\n" +
                "chr1\t3\tA\tC\t1\t0\tb\n" +
                "chr1\t3\tA\tC\t1\t1\tc\n" +
                "chr1\t3\tA\tC\t1\t1\td\n" +
                "chr1\t3\tA\tC\t2\t0\te\n" +
                "chr1\t3\tA\tC\t2\t1\tf\n" +
                "chr1\t3\tA\tC\t2\t0\tg\n" +
                "chr1\t3\tA\tC\t2\t2\th\n" +
                "chr1\t3\tA\tC\t3\t1\ti\n" +
                "chr1\t3\tA\tC\t3\t1\tj\n" +
                "chr1\t3\tA\tC\t3\t2\tk\n" +
                "chr1\t3\tA\tC\t3\t2\tl\n" +
                "chr1\t4\tG\tC\t1\t0\ta\n" +
                "chr1\t4\tG\tC\t1\t2\tb\n" +
                "chr1\t4\tG\tC\t1\t0\tc\n" +
                "chr1\t4\tG\tC\t1\t1\td\n" +
                "chr2\t4\tG\tC\t2\t0\te\n" +
                "chr2\t4\tG\tC\t2\t2\tf\n" +
                "chr2\t4\tG\tC\t2\t2\tg\n" +
                "chr2\t4\tG\tC\t2\t1\th\n" +
                "chr2\t4\tG\tC\t3\t0\ti\n" +
                "chr2\t4\tG\tC\t3\t0\tj\n" +
                "chr2\t4\tG\tC\t3\t0\tk\n" +
                "chr2\t4\tG\tC\t3\t1\tl\n";

        var cache = workDirPath.resolve("result_cache");
        Files.createDirectory(cache);

        int partsize = 3;
        var folderpath = workDirPath.resolve("folder4.gord");
        var query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(gor "+variantDictFile+" -nf -s Source -f #{tags} | write -tags #{tags} -d "+folderpath+");" +
                "gor [#s1#] | sort 1 -c Source";
        var result = TestUtils.runGorPipe(query,"-cachedir",cache.toString());
        Assert.assertEquals("Wrong result from partgor query", expected, result);

        var header = new TableHeader();
        header.load(folderpath.resolve("thedict.gord"), null);
        Assert.assertEquals("false", header.getProperty(TableHeader.HEADER_LINE_FILTER_KEY));

        partsize = 4;
        folderpath = workDirPath.resolve("folder3.gord");
        query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(gor "+variantDictFile+" -nf -s Source -f #{tags} | write -tags #{tags} -d "+folderpath+");" +
                "gor [#s1#] | sort 1 -c Source";
        result = TestUtils.runGorPipe(query,"-cachedir",cache.toString());
        Assert.assertEquals("Wrong result from partgor query", expected, result);

        partsize = 3;
        folderpath = workDirPath.resolve("folder4p.gord");
        query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(pgor "+variantDictFile+" -nf -s Source -f #{tags} | write -tags #{tags} -d "+folderpath+");" +
                "gor [#s1#] | sort 1 -c Source";
        result = TestUtils.runGorPipe(query,"-cachedir",cache.toString());
        Assert.assertEquals("Wrong result from partgor query", expected, result);

        partsize = 4;
        folderpath = workDirPath.resolve("folder3p.gord");
        query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(pgor "+variantDictFile+" -nf -s Source -f #{tags} | write -tags #{tags} -d "+folderpath+");" +
                "gor [#s1#] | sort 1 -c Source";
        result = TestUtils.runGorPipe(query,"-cachedir",cache.toString());
        Assert.assertEquals("Wrong result from partgor query", expected, result);

        query = "create #s1# = partgor -gordfolder -dict "+variantDictFile+" -partsize "+partsize+" <(pgor "+variantDictFile+" -nf -s Source -f #{tags} | write -tags #{tags});" +
                "gor [#s1#] | sort 1 -c Source";
        result = TestUtils.runGorPipe(query,"-cachedir",cache.toString());
        Assert.assertEquals("Wrong result from partgor query", expected, result);
    }

    @Test
    public void testGordFolder() throws IOException {
        var workDirPath = workDir.getRoot().toPath();
        var cache = workDirPath.resolve("result_cache");
        if(!Files.exists(cache)) Files.createDirectory(cache);

        var query = "create #variants# = gorrows -p chr1:1-1000 | merge <(gorrows -p chr10:1-1000) | merge <(gorrows -p chr2:1-1000) | merge <(gorrows -p chr21:1-1000) | calc ref 'A' | calc alt 'C';\n" +
                "create #pns# = norrows 10000 | calc PN 'PN_'+right('000000'+str(#1),5) | select PN | top 1000;\n" +
                "create #genotypes# = pgor -gordfolder dict [#variants#] \n" +
                "| calc ID '.'\n" +
                "| calc Info 'info'\n" +
                "| multimap -cartesian [#pns#] | calc gt mod(pos+int(right(pn,5)),4)\n" +
                "| sort 1 -c PN\n" +
                "| pivot -gc ref,alt,id,info -vf [#pns#] pn\n" +
                ";\n" +
                "def #bucksize# = 99;\n" +
                "def #first_gt_col# = 7;\n" +
                "create #buck2tags# = nor <(gor [#genotypes#]\n" +
                "| top 1 | unpivot #first_gt_col#- )| select col_name\n" +
                "| rownum | calc bucket 'b_'+str(1+div(rownum-1,#bucksize#))\n" +
                "| replace col_name replace(col_name,'_gt','')\n" +
                "| group -gc bucket -lis -sc col_name -len 100000 | rename lis_col_name tags;\n" +
                "create #segparts# = gor [#variants#] | group 100 -count | seghist 100;\n" +
                "create #empty# = pgor -split <(gor [#segparts#] | top 2) [#genotypes#] \n" +
                "| cols2list #first_gt_col#- values -gc ref,alt  -map \"left(x,3)\" -sep ''\n" +
                "| bucketsplit values #bucksize# -vs 1\n" +
                "| select chrom,pos,ref,alt,bucket,values\n" +
                "/*\n" +
                "| write test/#{fork}_#{CHROM}_#{BPSTART}_#{BPSTOP}.gorz -f bucket -card bucket;\n" +
                "\n*/" +
                "| write -f bucket -card bucket test.gord/#{fork}_#{CHROM}_#{BPSTART}_#{BPSTOP}.gorz;\n" +
                "gor [#empty#] | top 1";
        TestUtils.runGorPipe(query,"-gorroot",workDirPath.toString(),"-cachedir",cache.toString());
        Assert.assertTrue(Files.exists(workDirPath.resolve("test.gord").resolve("thedict.gord")));

        Assert.assertEquals("Nor-ing the folder with -asdict should be the same as noring the dict",
                TestUtils.runGorPipe("nor -asdict " + workDirPath.resolve("test.gord").resolve("thedict.gord")),
                TestUtils.runGorPipe("nor -asdict " + workDirPath.resolve("test.gord")));
    }
}
