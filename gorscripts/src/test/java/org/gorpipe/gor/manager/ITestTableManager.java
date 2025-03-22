package org.gorpipe.gor.manager;

import gorsat.TestUtils;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryEntry;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
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

    private final String file1 = "s3://gdb-unit-test-data/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053023D.wgs.genotypes.gor";
    private final String file2 = "s3://gdb-unit-test-data/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053025D.wgs.genotypes.gor";

    private static final String EXPECTED_FIRST_10 = "CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tHUUUART\tSource\n" +
            "chr8\t145577824\trs880701\tC\tG\t200.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=1.236;DB;DP=18;Dels=0.00;FS=0.000;HaplotypeScore=0.0000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=0.706;QD=11.15;ReadPosRankSum=0.795\tGT:AD:DP:GQ:PL\t0/1:9,9:18:99:229,0,206\ta1\n" +
            "chr8\t145577824\trs880701\tC\tG\t200.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=1.236;DB;DP=18;Dels=0.00;FS=0.000;HaplotypeScore=0.0000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=0.706;QD=11.15;ReadPosRankSum=0.795\tGT:AD:DP:GQ:PL\t0/1:9,9:18:99:229,0,206\ta2\n" +
            "chr8\t145577829\trs880702\tA\tC\t132.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=-2.507;DB;DP=20;Dels=0.00;FS=1.740;HaplotypeScore=0.0000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=0.532;QD=6.64;ReadPosRankSum=1.292\tGT:AD:DP:GQ:PL\t0/1:11,9:20:99:161,0,305\ta1\n" +
            "chr8\t145577829\trs880702\tA\tC\t132.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=-2.507;DB;DP=20;Dels=0.00;FS=1.740;HaplotypeScore=0.0000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=0.532;QD=6.64;ReadPosRankSum=1.292\tGT:AD:DP:GQ:PL\t0/1:11,9:20:99:161,0,305\ta2\n" +
            "chr8\t145577999\trs2272664\tT\tA\t12.05\tLowQual\tAC=1;AF=0.500;AN=2;BaseQRankSum=-1.067;DB;DP=8;Dels=0.00;FS=0.000;HaplotypeScore=0.8667;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=1.067;QD=1.51;ReadPosRankSum=-1.067\tGT:AD:DP:GQ:PL\t0/1:6,2:8:40:40,0,168\ta1\n" +
            "chr8\t145577999\trs2272664\tT\tA\t12.05\tLowQual\tAC=1;AF=0.500;AN=2;BaseQRankSum=-1.067;DB;DP=8;Dels=0.00;FS=0.000;HaplotypeScore=0.8667;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=1.067;QD=1.51;ReadPosRankSum=-1.067\tGT:AD:DP:GQ:PL\t0/1:6,2:8:40:40,0,168\ta2\n" +
            "chr8\t145578296\trs2272663\tA\tG\t24.78\tLowQual\tAC=1;AF=0.500;AN=2;BaseQRankSum=-1.754;DB;DP=7;Dels=0.00;FS=0.000;HaplotypeScore=0.0000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=1.754;QD=3.54;ReadPosRankSum=-0.550\tGT:AD:DP:GQ:PL\t0/1:4,3:7:53:53,0,108\ta1\n" +
            "chr8\t145578296\trs2272663\tA\tG\t24.78\tLowQual\tAC=1;AF=0.500;AN=2;BaseQRankSum=-1.754;DB;DP=7;Dels=0.00;FS=0.000;HaplotypeScore=0.0000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=1.754;QD=3.54;ReadPosRankSum=-0.550\tGT:AD:DP:GQ:PL\t0/1:4,3:7:53:53,0,108\ta2\n" +
            "chr8\t145579931\trs200026961\tC\tG\t27.77\tLowQual\tAC=1;AF=0.500;AN=2;BaseQRankSum=-3.473;DB;DP=31;Dels=0.00;FS=0.000;HaplotypeScore=1.8218;MLEAC=1;MLEAF=0.500;MQ=48.52;MQ0=0;MQRankSum=-3.520;QD=0.90;ReadPosRankSum=-2.150\tGT:AD:DP:GQ:PL\t0/1:24,7:31:56:56,0,656\ta1\n" +
            "chr8\t145579931\trs200026961\tC\tG\t27.77\tLowQual\tAC=1;AF=0.500;AN=2;BaseQRankSum=-3.473;DB;DP=31;Dels=0.00;FS=0.000;HaplotypeScore=1.8218;MLEAC=1;MLEAF=0.500;MQ=48.52;MQ0=0;MQRankSum=-3.520;QD=0.90;ReadPosRankSum=-2.150\tGT:AD:DP:GQ:PL\t0/1:24,7:31:56:56,0,656\ta2\n";

    private static final String projectRoot = "s3://gdb-unit-test-data/tmp/some_project";

    // NOTE: Providing system props for classes does usually not work if the prop is ready in static context.
    @Rule
    public final ProvideSystemProperty awsAccessKeyId = new ProvideSystemProperty("aws.accessKeyId", "");

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

    @Ignore("Needs more access to be able  to use ")
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

        DictionaryTable<DictionaryEntry> table = man.initTable(dictPath);
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
            Assert.assertEquals(EXPECTED_FIRST_10, content);
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
            Assert.assertEquals(EXPECTED_FIRST_10, content);
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
            Assert.assertEquals(EXPECTED_FIRST_10, content);
            System.out.println(String.format("Read top 10 with local gor link %d ms", (-time + (time = System.currentTimeMillis()))));
        } finally {
            try {
                if (s3FileReader.exists(testFolder)) s3FileReader.deleteDirectory(testFolder);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Ignore("Needs more access to be able  to use ")
    @Test
    public void testManS3DictionaryBucketize() throws IOException {
        String name = "testManS3DictionaryBucketize";
        String testFolder = PathUtils.resolve(projectRoot, name);
        try {
            String dictPath = createSmallS3Dict(testFolder);
            new GorDictionaryTable.Builder<>(dictPath).fileReader(s3FileReader).useHistory(false).build().save();

            long time = System.currentTimeMillis();

            TableManager man = TableManager.newBuilder()
                    .fileReader(s3FileReader)
                    .lockType(NoTableLock.class)   // For S3 assume no locking.
                    .minBucketSize(2)
                    .build();

            man.bucketize(dictPath, BucketManager.BucketPackLevel.CONSOLIDATE, 1, 1, List.of());
            System.out.println(String.format("Buckettize %d ms", (-time + (time = System.currentTimeMillis()))));

            String content = TestUtils.runGorPipeServer("gor " + dictPath + " | top 10", projectRoot, s3FileReader.getSecurityContext());
            Assert.assertEquals(EXPECTED_FIRST_10, content);
            System.out.println(String.format("Read top 10 with gor %d ms", (-time + (time = System.currentTimeMillis()))));

            Assert.assertTrue(Arrays.stream(s3FileReader.readAll(dictPath)).filter(l -> !l.startsWith("#")).collect(Collectors.joining("\n"))
                    .startsWith("s3://gdb-unit-test-data/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053023D.wgs.genotypes.gor|.testdict/b/b_"));
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
