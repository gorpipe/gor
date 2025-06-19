package gorsat;

import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTableMeta;
import org.junit.*;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gorpipe.gor.model.GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME;


public class UTestGorWriteExplicit {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    public static final String WRONG_RESULT = "Wrong results in write folder";
    static final String GROUP_CHROM_COUNT = " | group chrom -count";

    public static String GENE_PGOR_TOP1 = "Chrom\tgene_start\tgene_end\tGene_Symbol\n" +
            "chr1\t11868\t14412\tDDX11L1\n" +
            "chr1\t142553292\t142559170\tAL583842.1\n" +
            "chr10\t60000\t60544\tRP11-631M21.1\n" +
            "chr10\t42644757\t42644997\tAL031601.3\n" +
            "chr11\t75779\t76143\tRP11-304M2.1\n" +
            "chr11\t55029657\t55038595\tTRIM48\n" +
            "chr12\t67606\t69138\tRP11-598F7.1\n" +
            "chr12\t37935813\t37936188\tRP11-125N22.4\n" +
            "chr13\t19041311\t19059588\tZNF962P\n" +
            "chr14\t19109938\t19118336\tRP11-754I20.1\n" +
            "chr15\t20083768\t20093074\tRP11-79C23.1\n" +
            "chr16\t61552\t64093\tDDX11L10\n" +
            "chr17\t4960\t5048\tAC108004.1\n" +
            "chr18\t11102\t15928\tAP005530.1\n" +
            "chr19\t60104\t70966\tAC008993.5\n" +
            "chr2\t38813\t46870\tFAM110C\n" +
            "chr2\t95391418\t95391680\tCNN2P11\n" +
            "chr20\t68350\t77174\tDEFB125\n" +
            "chr21\t9683190\t9683272\tCR381670.1\n" +
            "chr22\t16062156\t16063236\tLA16c-4G1.3\n" +
            "chr3\t65430\t66175\tAY269186.1\n" +
            "chr3\t93591880\t93692910\tPROS1\n" +
            "chr4\t48990\t50018\tZ95704.4\n" +
            "chr4\t52709165\t52783003\tDCUN1D4\n" +
            "chr5\t58312\t59030\tRP11-811I15.1\n" +
            "chr5\t49692025\t49739082\tEMB\n" +
            "chr6\t105918\t106856\tRP1-24O22.1\n" +
            "chr6\t61996402\t61996816\tRP1-91N13.1\n" +
            "chr7\t19756\t35479\tAC093627.6\n" +
            "chr7\t61821868\t61822186\tRP11-715L17.1\n" +
            "chr8\t14090\t14320\tAC144568.4\n" +
            "chr8\t47172181\t47172472\tAC113134.1\n" +
            "chr9\t11055\t11620\tAL928970.1\n" +
            "chr9\t65467782\t65469026\tRP11-101E5.1\n" +
            "chrM\t576\t647\tJ01415.2\n" +
            "chrX\t170409\t172712\tLINC00108\n" +
            "chrX\t61998719\t61999796\tRP11-3D23.1\n" +
            "chrY\t2654895\t2655740\tSRY\n";
    public static String GENE_GROUP_CHROM_TOP1 = "Chrom\tbpStart\tbpStop\tallCount\n" +
            "chr1\t0\t250000000\t2\n" +
            "chr10\t0\t150000000\t2\n" +
            "chr11\t0\t150000000\t2\n" +
            "chr12\t0\t150000000\t2\n" +
            "chr13\t0\t150000000\t1\n" +
            "chr14\t0\t150000000\t1\n" +
            "chr15\t0\t150000000\t1\n" +
            "chr16\t0\t100000000\t1\n" +
            "chr17\t0\t100000000\t1\n" +
            "chr18\t0\t100000000\t1\n" +
            "chr19\t0\t100000000\t1\n" +
            "chr2\t0\t250000000\t2\n" +
            "chr20\t0\t100000000\t1\n" +
            "chr21\t0\t100000000\t1\n" +
            "chr22\t0\t100000000\t1\n" +
            "chr3\t0\t200000000\t2\n" +
            "chr4\t0\t200000000\t2\n" +
            "chr5\t0\t200000000\t2\n" +
            "chr6\t0\t200000000\t2\n" +
            "chr7\t0\t200000000\t2\n" +
            "chr8\t0\t150000000\t2\n" +
            "chr9\t0\t150000000\t2\n" +
            "chrM\t0\t20000\t1\n" +
            "chrX\t0\t200000000\t2\n" +
            "chrY\t0\t100000000\t1\n";

    public static String GENE_GROUP_CHROM = "Chrom\tbpStart\tbpStop\tallCount\n" +
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

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private Path workDirPath;
    private Path cachePath;

    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();
        cachePath = workDirPath.resolve("result_cache");
        Files.createDirectory(cachePath);

        var genespath = Path.of("../tests/data/gor/genes.gor");
        Files.copy(genespath, workDirPath.resolve(genespath.getFileName()));
    }

    @Test
    @Ignore("Passthrough not supported yet")
    public void testWritePassThrough() {
        Path path = workDirPath.resolve("gorfile.gorz");
        String results = TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | top 1 | write -p " + path);
        Assert.assertEquals(WRONG_RESULT, "Chrom\tgene_start\tgene_end\tGene_Symbol\n" +
                "chr1\t11868\t14412\tDDX11L1\n" , results);
    }


    @Test
    public void testCreateWrite() {
        Path path = workDirPath.resolve("gorfile.gorz");
        String results = TestUtils.runGorPipe("create a = gor -p chr21 ../tests/data/gor/genes.gor | write " + path + "; gor "+path+GROUP_CHROM_COUNT);
        Assert.assertEquals(WRONG_RESULT, "Chrom\tbpStart\tbpStop\tallCount\n" +
                "chr21\t0\t100000000\t669\n" , results);
    }

    @Test
    public void testGorCardinalityColumn() throws IOException {
        Path path = workDirPath.resolve("gorfile.gorz");
        TestUtils.runGorPipe("gor -p chr21 ../tests/data/gor/genes.gor | calc c substr(gene_symbol,0,1) | write -card c " + path);
        Path metapath = path.getParent().resolve("gorfile.gorz.meta");
        try(Stream<String> stream = Files.lines(metapath).filter(l -> !l.startsWith("## QUERY:"))) {
            String metainfo = stream.collect(Collectors.joining("\n"));
            final var WRONG_RES_IN_META = "Wrong results in meta file";
            Assert.assertTrue(WRONG_RES_IN_META, metainfo.contains("## RANGE = chr21\t9683190\tchr21\t48110675"));
            Assert.assertTrue(WRONG_RES_IN_META, metainfo.contains("## MD5 = 162498408aa03202fa1d2327b2cf9c4f"));
            Assert.assertTrue(WRONG_RES_IN_META, metainfo.contains("## LINE_COUNT = 669"));
            Assert.assertTrue(WRONG_RES_IN_META, metainfo.contains("## CARDCOL = [c]: A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,R,S,T,U,V,W,Y,Z"));
        }
    }

    @Test
    @Ignore("Not supported")
    public void testPgorDictFolderNoWrite() throws IOException {
        var query = "create a = pgor -gordfolder dict ../tests/data/gor/genes.gor; gor [a] | group chrom -count";
        var results = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RESULT, GENE_GROUP_CHROM, results);
    }

    @Test
    @Ignore("Not supported")
    public void testParallelPgorDictFolderNoWrite() throws IOException {
        var query = "create a = parallel -gordfolder -parts <(norrows 2) <(pgor ../tests/data/gor/genes.gor | rownum | where mod(rownum,2)=#{col:RowNum}); gor [a] | group chrom -count";
        var results = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RESULT, GENE_GROUP_CHROM, results);
    }

    @Test
    public void testParallelPgorDictFolderWrite() {
        var query = "create a = parallel -parts <(norrows 2) <(pgor genes.gor | rownum | calc modrow mod(rownum,2) | where modrow=#{col:RowNum} | write mu.gord/#{fork}_#{CHROM}_#{BPSTART}_#{BPSTOP}.gorz -f modrow -card modrow); gor mu.gord/"+DEFAULT_FOLDER_DICTIONARY_NAME+GROUP_CHROM_COUNT;
        var results = TestUtils.runGorPipe(query,"-gorroot",workDirPath.toString(),"-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RESULT, GENE_GROUP_CHROM, results);
    }

    @Test
    public void testParallelPgorDictFolderWriteServerMode() {
        var query = "create a = parallel -parts <(norrows 2) <(pgor genes.gor | rownum | calc modrow mod(rownum,2) | where modrow=#{col:RowNum} | write test/mu.gord/#{fork}_#{CHROM}_#{BPSTART}_#{BPSTOP}.gorz -f modrow -card modrow); norrows 1";
        var args = new String[] {query,"-gorroot",workDirPath.toString(),"-cachedir",cachePath.toString()};
        var results = TestUtils.runGorPipeCount(args, true);
        Assert.assertEquals(WRONG_RESULT, 1, results);
    }

    @Test
    public void testPgorWriteGordFolder() {
        var folderpath = workDirPath.resolve("folder.gord");

        String results = TestUtils.runGorPipe("create a = pgor ../tests/data/gor/genes.gor | top 1 | write " + folderpath +
                "; gor [a] ","-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RESULT, GENE_PGOR_TOP1, results);

        Assert.assertEquals("Nor-ing the folder with -asdict should be the same as noring the dict",
                TestUtils.runGorPipe("nor -asdict " + folderpath.resolve(DEFAULT_FOLDER_DICTIONARY_NAME)),
                TestUtils.runGorPipe("nor -asdict " + folderpath));
    }

    @Test
    public void testPgorForkWrite() {
        var folderpath = workDirPath.resolve("folder.gord");

        String results = TestUtils.runGorPipe("create a = pgor ../tests/data/gor/genes.gor | rownum | calc mod mod(rownum,2) | hide rownum | write -f mod " + folderpath + "/file_#{CHROM}_#{BPSTART}_#{BPSTOP}_#{fork}.gorz;" +
                "gor [a] | group chrom -count","-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RESULT, GENE_GROUP_CHROM, results);
    }

    @Test
    public void testPgorWriteExplicitFilename() {
        var folderpath = workDirPath.resolve("folder.gord");
        String results = TestUtils.runGorPipe("create a = pgor ../tests/data/gor/genes.gor | write " + folderpath + "/file_#{CHROM}_#{BPSTART}_#{BPSTOP}.gorz;" +
                "gor [a] | group chrom -count","-cachedir",cachePath.toString());
        //"; gor [a] | group chrom -count");
        Assert.assertEquals(WRONG_RESULT, GENE_GROUP_CHROM, results);
    }

    @Test
    public void testPgorWriteFolderWithCardinality() throws IOException {
        Path folderpath = workDirPath.resolve("folder.gord");
        TestUtils.runGorPipe("create a = pgor ../tests/data/gor/genes.gor | where chrom = 'chrM' | calc c substr(gene_symbol,0,1) | write -card c " + folderpath +
                "; gor [a] | group chrom -count");
        String thedict = Files.readString(folderpath.resolve(DEFAULT_FOLDER_DICTIONARY_NAME)).trim();
        var dictsplit = thedict.split("\n");
        var last = dictsplit[dictsplit.length-1];
        Assert.assertEquals("Wrong results in dictionary",
                "1\tchrM\t576\tchrM\t15955\tJ,M",
                last.substring(last.indexOf('\t')+1));
    }

    @Test
    public void testPgorWriteCacheFolderWithCardinality() throws IOException {
        var folderpath = workDirPath.resolve("result_cache");
        Files.createDirectories(folderpath);
        TestUtils.runGorPipe("create a = pgor genes.gor | where chrom = 'chrM' | calc c substr(gene_symbol,0,1) | write -card c;" +
                "gor [a] | group chrom -count", "-gorroot", workDirPath.toAbsolutePath().toString(), "-cachedir", "result_cache");
        try(var thedictstream = Files.walk(folderpath, FileVisitOption.FOLLOW_LINKS)) {
            var thedict = thedictstream.filter(p -> p.getFileName().toString().equals(DEFAULT_FOLDER_DICTIONARY_NAME)).map(p -> {
                try {
                    return Files.readString(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).findFirst();

            Assert.assertTrue(thedict.isPresent());
            var dictsplit = thedict.get().split("\n");
            var last = dictsplit[dictsplit.length-1];
            Assert.assertEquals("Wrong results in dictionary",
                    "1\tchrM\t576\tchrM\t15955\tJ,M",
                    last.substring(last.indexOf('\t')+1));
        }
    }

    @Test
    public void testPartGorWrite() throws IOException {
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

        final var WRONG_RES_PARTGOR = "Wrong result from partgor query";

        int partsize = 3;
        var folderpath = workDirPath.resolve("folder4.gord");
        var query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(gor "+variantDictFile+" -nf -s Source -f #{tags} | write -tags #{tags} "+folderpath+");" +
                "gor [#s1#] | sort 1 -c Source";
        var result = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RES_PARTGOR, expected, result);

        var header = new GorDictionaryTableMeta();
        header.loadAndMergeMeta(folderpath.resolve(DEFAULT_FOLDER_DICTIONARY_NAME));
        Assert.assertEquals("false", header.getProperty(GorDictionaryTableMeta.HEADER_LINE_FILTER_KEY));

        partsize = 4;
        folderpath = workDirPath.resolve("folder3.gord");
        query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(gor "+variantDictFile+" -nf -s Source -f #{tags} | write -tags #{tags} "+folderpath+");" +
                "gor [#s1#] | sort 1 -c Source";
        result = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RES_PARTGOR, expected, result);

        partsize = 3;
        folderpath = workDirPath.resolve("folder4p.gord");
        query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(pgor "+variantDictFile+" -nf -s Source -f #{tags} | write -tags #{tags} "+folderpath+");" +
                "gor [#s1#] | sort 1 -c Source";
        result = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RES_PARTGOR, expected, result);

        partsize = 4;
        folderpath = workDirPath.resolve("folder3p.gord");
        query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(pgor "+variantDictFile+" -nf -s Source -f #{tags} | write -tags #{tags} "+folderpath+");" +
                "gor [#s1#] | sort 1 -c Source";
        result = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RES_PARTGOR, expected, result);
    }

    @Test
    public void testPartGorPgorDictionaryHeader() throws IOException {
        var bucketFile = workDirPath.resolve("buckets.tsv");
        Path variantBucketFile1 = workDirPath.resolve("variants1.gor");
        Path variantBucketFile2 = workDirPath.resolve("variants2.gor");
        Path variantBucketFile3 = workDirPath.resolve("variants3.gor");
        Path variantBucketFile4 = workDirPath.resolve("variants4.gor");
        var variantDictFile = workDirPath.resolve("variants.gord");
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
        Files.writeString(variantDictFile,"variants1.gor\ta\n"+
                "variants2.gor\tb\n"+
                "variants3.gor\tc\n"+
                "variants4.gor\td\n");

        var expected = "Chrom\tpos\tref\talt\tbucket\tvalues\tSource\n";
        final var WRONG_RES_PARTGOR = "Wrong result from partgor query";

        int partsize = 2;
        var query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(gor "+variantDictFile+" -nf -s Source -f #{tags} | top 0);" +
                "gor [#s1#] | top 0";
        var result = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RES_PARTGOR, expected, result);

        query = "create #s1# = partgor -dict "+variantDictFile+" -partsize "+partsize+" <(pgor "+variantDictFile+" -nf -s Source -f #{tags} | top 0);" +
                "gor [#s1#] | top 0";
        result = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertEquals(WRONG_RES_PARTGOR, expected, result);
    }

    @Test
    public void testExplicitWrite() throws IOException {
        var query = "create a = gor ../tests/data/gor/genes.gor | top 1 | write "+ workDirPath.resolve("test.gor").toAbsolutePath() +"; gor [a] | group chrom -count";
        var results = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertTrue(Files.walk(cachePath).filter(p -> p.toString().endsWith(".gor")).allMatch(p -> p.endsWith(".link")));
        Assert.assertEquals(WRONG_RESULT, "Chrom\tbpStart\tbpStop\tallCount\nchr1\t0\t250000000\t1\n", results);
    }

    @Test
    public void testExplicitWriteFolder() throws IOException {
        var query = "create a = pgor ../tests/data/gor/genes.gor | top 1 | write "+ workDirPath.resolve("test.gord").toAbsolutePath() +"; gor [a] | group chrom -count";
        var results = TestUtils.runGorPipe(query,"-cachedir",cachePath.toString());
        Assert.assertTrue(Files.walk(cachePath).filter(p -> p.toString().endsWith(".gord")).allMatch(p -> p.endsWith(".link")));
        Assert.assertEquals(WRONG_RESULT, GENE_GROUP_CHROM_TOP1, results);

        String[] result = TestUtils.runGorPipe("nor -asdict " + workDirPath.resolve("test.gord")).split("\n");
        Assert.assertEquals("Header should be same lenght as lines", result[0].split("\t", -1).length, result[1].split("\t", -1).length);
    }

    @Test
    public void testGordFolder() {
        var query = "create #variants# = gorrows -p chr1:1-1000 | merge <(gorrows -p chr10:1-1000) | merge <(gorrows -p chr2:1-1000) | merge <(gorrows -p chr21:1-1000) | calc ref 'A' | calc alt 'C';\n" +
                "create #pns# = norrows 10000 | calc PN 'PN_'+right('000000'+str(#1),5) | select PN | top 1000;\n" +
                "create #genotypes# = pgor [#variants#] \n" +
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
        TestUtils.runGorPipe(query,"-gorroot",workDirPath.toString(),"-cachedir",cachePath.toString());
        Assert.assertTrue(Files.exists(workDirPath.resolve("test.gord").resolve(DEFAULT_FOLDER_DICTIONARY_NAME)));

        Assert.assertEquals("Nor-ing the folder with -asdict should be the same as noring the dict",
                TestUtils.runGorPipe("nor -asdict " + workDirPath.resolve("test.gord").resolve(DEFAULT_FOLDER_DICTIONARY_NAME)),
                TestUtils.runGorPipe("nor -asdict " + workDirPath.resolve("test.gord")));

        String[] result = TestUtils.runGorPipe("nor -asdict " + workDirPath.resolve("test.gord")).split("\n");
        Assert.assertEquals("Header should be same lenght as lines", result[0].split("\t", -1).length, result[1].split("\t", -1).length);
    }

    @Test
    public void testCreateWriteSignature() throws IOException {
        System.setProperty("gor.gorpipe.sideeffects.force_run", "true");

        Path path = workDirPath.resolve("gorfile.gorz");
        TestUtils.runGorPipe(
                "create a = gor -p chr21 ../tests/data/gor/genes.gor | write " + path + "; " +
                      "gor "+path+GROUP_CHROM_COUNT);

        Files.deleteIfExists(path);
        TestUtils.runGorPipe(
                "create a = gor -p chr21 ../tests/data/gor/genes.gor | write " + path + "; " +
                        "gor "+path+GROUP_CHROM_COUNT);

        Assert.assertTrue("File should be recreated", Files.exists(path));
    }
}
