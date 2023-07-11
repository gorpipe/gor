package org.gorpipe.gor.manager;

import gorsat.TestUtils;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.lock.NoTableLock;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.test.IntegrationTests;
import org.gorpipe.utils.DriverUtils;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

//@Ignore // Just run manually for now, takes too long.
@Category(IntegrationTests.class)
public class ITestTableManager {

    private static String S3_KEY;
    private static String S3_SECRET;
    private DriverBackedFileReader s3FileReader;

    private String file1 = "s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053023D.wgs.genotypes.gorz";
    private String file2 = "s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053025D.wgs.genotypes.gorz";


    private static String expectedFirst10 = "CHROM\tPOS\tReference\tCall\tCallCopies\tCallRatio\tDepth\tGL_Call\tFILTER\tFS\tformatZip\tSource\n" +
            "chr1\t10403\tACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAAC\tA\t1\t0.471\t17\t277\tPASS\t5.315\tAlt=A:GT=0/1,AD=9,8,DP=17,GQ=99,PL=277,0,318\ta1\n" +
            "chr1\t10616\tCCGCCGTTGCAAAGGCGCGCCG\tC\t2\t1.000\t3\t9\tPASS\t0.0\tAlt=C:GT=1/1,AD=0,3,DP=3,GQ=9,PL=136,9,0\ta2\n" +
            "chr1\t12783\tG\tA\t1\t0.783\t46\t119\tPASS\t0.0\tAlt=A:GT=0/1,AD=10,36,DP=46,GQ=99,PL=949,0,119\ta1\n" +
            "chr1\t12783\tG\tA\t1\t0.787\t61\t99\tPASS\t0.0\tAlt=A:GT=0/1,AD=13,48,DP=61,GQ=99,PL=1263,0,99\ta2\n" +
            "chr1\t13012\tG\tA\t1\t0.132\t68\t73\tPASS\t1.729\tAlt=A:GT=0/1,AD=59,9,DP=68,GQ=73,PL=73,0,1590\ta1\n" +
            "chr1\t13079\tC\tG\t1\t0.188\t85\t210\tPASS\t0.0\tAlt=G:GT=0/1,AD=69,16,DP=85,GQ=99,PL=210,0,1671\ta1\n" +
            "chr1\t13079\tC\tG\t1\t0.107\t122\t134\tPASS\t11.555\tAlt=G:GT=0/1,AD=109,13,DP=122,GQ=99,PL=134,0,2718\ta2\n" +
            "chr1\t13110\tG\tA\t1\t0.215\t93\t321\tPASS\t16.887\tAlt=A:GT=0/1,AD=73,20,DP=93,GQ=99,PL=321,0,3052\ta2\n" +
            "chr1\t13116\tT\tG\t1\t0.652\t46\t582\tPASS\t8.14\tAlt=G:GT=0/1,AD=16,30,DP=46,GQ=99,PGT=0|1,PID=13116_T_G,PL=1200,0,582\ta1\n" +
            "chr1\t13116\tT\tG\t1\t0.674\t86\t1420\tPASS\t26.618\tAlt=G:GT=0/1,AD=28,58,DP=86,GQ=99,PGT=0|1,PID=13116_T_G,PL=2333,0,1420\ta2\n";

    private static String projectRoot = "s3://nextcode-unittest/tmp/some_project";

    // NOTE: Providing system props for classes does usually not work if the prop is ready in static context.
    @Rule
    public final ProvideSystemProperty awsAccessKeyId = new ProvideSystemProperty("aws.accessKeyId", "");

    @Rule
    public final ProvideSystemProperty otherPropertyIsMissing = new ProvideSystemProperty("aws.secretKey", "");


    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();



    @BeforeClass
    static public void setUpClass() throws IOException {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_KEY");
        S3_SECRET = props.getProperty("S3_SECRET");
    }

    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();
        Files.createDirectory(workDirPath.resolve("some_project"));

        String securityContext = DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET);
        s3FileReader = new DriverBackedFileReader(securityContext, projectRoot);
    }

    @Test
    public void testManCreateS3Dictionary() throws IOException {
        String name = "testManCreateS3Dictionary";
        String testFolder = PathUtils.resolve(projectRoot, name);


        String dictPath = PathUtils.resolve( testFolder, "testdict.gord");

        long time = System.currentTimeMillis();

        s3FileReader.createDirectory(testFolder);

        System.out.println(String.format("Create dir %d ms", (-time + (time = System.currentTimeMillis()))));

        TableManager man = TableManager.newBuilder()
                .fileReader(s3FileReader)
                .lockType(NoTableLock.class)   // For S3 assume no locking.
                .minBucketSize(2)
                .build();

        DictionaryTable table = man.initTable(dictPath);
        table.setUseHistory(false);
        table.setValidateFiles(false);

        System.out.println(String.format("Init table %d ms", (-time + (time = System.currentTimeMillis()))));

        try {
            table.save();
            System.out.println(String.format("Save table %d ms", (-time + (time = System.currentTimeMillis()))));
            Assert.assertTrue(s3FileReader.exists(dictPath));
            System.out.println(String.format("Checked table existence %d ms", (-time + (time = System.currentTimeMillis()))));

            table.insert(file1 + "\ta1", file2 + "\ta2");
            System.out.println(String.format("Insert two lines %d ms", (-time + (time = System.currentTimeMillis()))));
            table.save();
            System.out.println(String.format("Save table %d ms", (-time + (time = System.currentTimeMillis()))));
            Assert.assertEquals(String.format("%s\ta1\n%s\ta2", file1, file2), Arrays.stream(s3FileReader.readAll(dictPath)).filter(l -> !l.startsWith("#")).collect(Collectors.joining("\n")));
            System.out.println(String.format("Read all lines %d ms", (-time + (time = System.currentTimeMillis()))));

            Collection<? extends DictionaryEntry> entries = man.selectAll(dictPath);
            System.out.println(String.format("Select all %d ms", (-time + (time = System.currentTimeMillis()))));
            Assert.assertEquals(2, entries.size());

            String content = TestUtils.runGorPipeServer("gor " + dictPath + " | top 10", projectRoot, s3FileReader.getSecurityContext());
            Assert.assertEquals(expectedFirst10, content);
            System.out.println(String.format("Read top 10 with gor %d ms", (-time + (time = System.currentTimeMillis()))));
        } finally {
            try {
                if (s3FileReader.exists(testFolder)) s3FileReader.deleteDirectory(testFolder);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Test
    public void testS3DictionaryRead() throws IOException {
        String name = "testS3DictionaryRead";
        String testFolder = PathUtils.resolve(projectRoot, name);
        try {
            String dictPath = createSmallS3Dict(testFolder);
            long time = System.currentTimeMillis();

            String content = TestUtils.runGorPipeServer("gor " + dictPath + " | top 10", projectRoot, s3FileReader.getSecurityContext());
            Assert.assertEquals(expectedFirst10, content);
            System.out.println(String.format("Read top 10 with local gor link %d ms", (-time + (time = System.currentTimeMillis()))));
        } finally {
            try {
                if (s3FileReader.exists(testFolder)) s3FileReader.deleteDirectory(testFolder);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Test
    public void testS3DictionaryReadLocalLink() throws IOException {
        String name = "testS3DictionaryLocalLink";
        String testFolder = PathUtils.resolve(projectRoot, name);
        try {
            String dictPath = createSmallS3Dict(testFolder);
            long time = System.currentTimeMillis();
            // Test read through dict link
            Path linkPath = workDirPath.resolve("some_project").resolve("dictlink.gord.link");
            Files.writeString(linkPath, dictPath);

            String content = TestUtils.runGorPipeServer("gor " + workDirPath.resolve("some_project").resolve("dictlink.gord") + " | top 10", workDirPath.resolve("some_project").toString(), s3FileReader.getSecurityContext());
            Assert.assertEquals(expectedFirst10, content);
            System.out.println(String.format("Read top 10 with local gor link %d ms", (-time + (time = System.currentTimeMillis()))));
        } finally {
            try {
                if (s3FileReader.exists(testFolder)) s3FileReader.deleteDirectory(testFolder);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Test
    public void testManS3DictionaryBucketize() throws IOException {
        String name = "testManS3DictionaryBucketize";
        String testFolder = PathUtils.resolve(projectRoot, name);
        try {
            String dictPath = createSmallS3Dict(testFolder);
            new DictionaryTable.Builder<>(dictPath).fileReader(s3FileReader).useHistory(false).build().save();

            long time = System.currentTimeMillis();

            TableManager man = TableManager.newBuilder()
                    .fileReader(s3FileReader)
                    .lockType(NoTableLock.class)   // For S3 assume no locking.
                    .minBucketSize(2)
                    .build();

            man.bucketize(dictPath, BucketManager.BucketPackLevel.CONSOLIDATE, 1, 1, List.of());
            System.out.println(String.format("Buckettize %d ms", (-time + (time = System.currentTimeMillis()))));

            String content = TestUtils.runGorPipeServer("gor " + dictPath + " | top 10", projectRoot, s3FileReader.getSecurityContext());
            Assert.assertEquals(expectedFirst10, content);
            System.out.println(String.format("Read top 10 with gor %d ms", (-time + (time = System.currentTimeMillis()))));

            Assert.assertTrue(Arrays.stream(s3FileReader.readAll(dictPath)).filter(l -> !l.startsWith("#")).collect(Collectors.joining("\n"))
                    .startsWith("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053023D.wgs.genotypes.gorz|.testdict/buckets/testdict_bucket_"));
            System.out.println(String.format("Read all lines %d ms", (-time + (time = System.currentTimeMillis()))));

        } finally {
            try {
                if (s3FileReader.exists(testFolder)) s3FileReader.deleteDirectory(testFolder);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private String createSmallS3Dict(String testFolder) throws IOException {
        String dictPath = PathUtils.resolve( testFolder, "testdict.gord");

        long time = System.currentTimeMillis();
        s3FileReader.createDirectory(testFolder);
        System.out.println(String.format("Init driver / Create dir %d ms", (-time + (time = System.currentTimeMillis()))));

        try(OutputStream os = s3FileReader.getOutputStream(dictPath)) {
            os.write((file1 + "\ta1\n" + file2 + "\ta2\n").getBytes());
        }
        System.out.println(String.format("Create dict %d ms", (-time + (time = System.currentTimeMillis()))));

        Assert.assertEquals(String.format("%s\ta1\n%s\ta2", file1, file2), Arrays.stream(s3FileReader.readAll(dictPath)).filter(l -> !l.startsWith("#")).collect(Collectors.joining("\n")));
        System.out.println(String.format("Read all lines %d ms", (-time + (time = System.currentTimeMillis()))));

        return dictPath;
    }

}
