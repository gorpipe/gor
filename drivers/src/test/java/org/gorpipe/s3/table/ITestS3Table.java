package org.gorpipe.s3.table;

import gorsat.TestUtils;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.table.lock.NoTableLock;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.s3.shared.ITestS3Shared;
import org.gorpipe.test.IntegrationTests;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.gor.manager.BucketManager;
import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static gorsat.TestUtils.runGorPipeServer;
import static org.gorpipe.gor.manager.BucketManager.HEADER_BUCKET_DIRS_KEY;
import static org.gorpipe.gor.manager.BucketManager.HEADER_BUCKET_DIRS_LOCATION_KEY;
import static org.gorpipe.gor.model.GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME;
import static org.gorpipe.utils.DriverUtils.awsSecurityContext;

/**
 * Note, there are S3Shared integration tests in gor-services (ITestS3Shared).
 */
@Category(IntegrationTests.class)
public class ITestS3Table {

    private static String S3_KEY;
    private static String S3_SECRET;
    private DriverBackedFileReader fileReader;

    // NOTE: Providing system props for classes does usually not work if the prop is ready in static context.
    @Rule
    public final ProvideSystemProperty awsAccessKeyId = new ProvideSystemProperty("aws.accessKeyId", "");

    @Rule
    public final ProvideSystemProperty otherPropertyIsMissing = new ProvideSystemProperty("aws.secretKey", "");


    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private Path gordFile;

    public ITestS3Table() throws IOException {
    }

    @BeforeClass
    static public void setUpClass() throws IOException {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_KEY");
        S3_SECRET = props.getProperty("S3_SECRET");

        ITestS3Shared.setUpClass();
    }

    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();
        Files.createDirectory(workDirPath.resolve("some_project"));

        String s3dataSecurityContext = DriverUtils.createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String awsSecurityContext = awsSecurityContext(S3_KEY, S3_SECRET);;
        BundledCredentials.Builder b = new BundledCredentials.Builder()
                .addCredentials(BundledCredentials.fromSecurityContext(s3dataSecurityContext))
                .addCredentials(BundledCredentials.fromSecurityContext(awsSecurityContext));
        BundledCredentials bundleCreds = b.build();
        String securityContext = bundleCreds.addToSecurityContext(null);

        fileReader = new DriverBackedFileReader(securityContext, workDirPath.resolve("some_project").toString(), new Object[]{});

    }

    private void insertIntoTableGordFile() throws IOException {
        gordFile = workDirPath.resolve("some_project").resolve("dict.gord");
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).fileReader(fileReader).build();

        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053023D.wgs.genotypes.gorz", dict.getRootPath()).alias("D3_WGC053023D").build());
        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053033D.wgs.genotypes.gorz", dict.getRootPath()).alias("D3_WGC053033D").build());
        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053043D.wgs.genotypes.gorz", dict.getRootPath()).alias("D3_WGC053043D").build());
        dict.save();
    }

    @Test
    public void testInsertLocalTableS3DataBasic() throws IOException {
        insertIntoTableGordFile();

        String[] result = runGorPipeServer("gor " + gordFile.toString(),
                workDirPath.resolve("some_project").toString(), fileReader.getSecurityContext()).split("\n");
        Assert.assertEquals("Did not retreive the correct data", 759,  result.length);
    }
    
    @Test
    public void testBucketizeLocalTableS3DataLocalBuckets() throws IOException {
        insertIntoTableGordFile();

        DictionaryTable table = new DictionaryTable.Builder<>(gordFile).fileReader(fileReader).validateFiles(false).build();

        TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(1).fileReader(fileReader).build();
        man.bucketize(table.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);

        table.reload();
        Assert.assertEquals("New lines should not be bucketized", 0, table.needsBucketizing().size());

        String bucket = table.getBuckets().get(0).toString();
        Path bucketFullPath = workDirPath.resolve("some_project").resolve(bucket);
        Assert.assertTrue(Files.exists(bucketFullPath));

       String[] bucketResult = runGorPipeServer("gor " + bucketFullPath.toString(),
                workDirPath.resolve("some_project").toString(), fileReader.getSecurityContext()).split("\n");
        Assert.assertEquals("Did not retrieve the correct data", 759,  bucketResult.length);
    }
    
    @Test
    public void testBucketizeLocalTableS3DataS3Buckets()  throws IOException {
        insertIntoTableGordFile();

        DictionaryTable table = new DictionaryTable.Builder<>(gordFile).fileReader(fileReader).build();
        table.setProperty(HEADER_BUCKET_DIRS_KEY, "s3://nextcode-unittest/tmp/buckets/");
        table.save();
        TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(1).fileReader(fileReader).build();

        List<String> buckets = null;
        try {
            man.bucketize(table.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);

            table.reload();
            Assert.assertEquals("New lines should not be bucketized", 0, table.needsBucketizing().size());

            buckets = table.getBuckets();
            String bucket = buckets.get(0);
            Assert.assertTrue(table.getFileReader().exists(bucket));

            String[] bucketResult = runGorPipeServer("gor " +bucket,
                    workDirPath.resolve("some_project").toString(), fileReader.getSecurityContext()).split("\n");
            Assert.assertEquals("Did not retrieve the correct data", 759, bucketResult.length);
        } finally {
            // Manual cleanup as this is S3.
            if (buckets != null) {
                man.deleteBuckets(table, true, buckets.toArray(new String[buckets.size()]));
            }
        }
    }
    
    @Test
    public void testBucketizeLocalTableS3DataS3DataBuckets() throws IOException {
        insertIntoTableGordFile();

        fileReader.createDirectoryIfNotExists("s3data://project/user_data/buckets/");

        DictionaryTable table = new DictionaryTable.Builder<>(gordFile).fileReader(fileReader).build();
        table.setProperty(HEADER_BUCKET_DIRS_KEY, "s3data://project/user_data/buckets/");
        table.save();
        TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(1).fileReader(fileReader).build();

        List<String> buckets = null;
        try {
            man.bucketize(table.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);

            table.reload();
            Assert.assertEquals("New lines should be bucketized", 0, table.needsBucketizing().size());

            buckets = table.getBuckets();
            String bucket = buckets.get(0);
            Assert.assertTrue(table.getFileReader().exists(bucket));

            String[] bucketResult = runGorPipeServer("gor "
                            + workDirPath.resolve("some_project").resolve(bucket.substring("s3data://project/".length())).toString(),
                    workDirPath.resolve("some_project").toString(), fileReader.getSecurityContext()).split("\n");
            Assert.assertEquals("Did not retrieve the correct data", 759, bucketResult.length);
        } finally {
            // Manual cleanup as this is S3.
            if (buckets != null) {
                man.deleteBuckets(table, true, buckets.toArray(new String[buckets.size()]));
            }
        }
    }

    @Test
    public void testBucketizeLocalTableS3DataS3DataBucketsRelative() throws IOException {
        insertIntoTableGordFile();

        //fileReader.createDirectoryIfNotExists("s3data://project/user_data/buckets/");

        DictionaryTable table = new DictionaryTable.Builder<>(gordFile).fileReader(fileReader).build();
        table.setProperty(HEADER_BUCKET_DIRS_LOCATION_KEY, "s3data://project");
        table.save();
        TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(1).fileReader(fileReader).build();

        List<String> buckets = null;
        try {
            man.bucketize(table.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);

            table.reload();
            Assert.assertEquals("New lines should not be bucketized", 0, table.needsBucketizing().size());

            buckets = table.getBuckets();
            String bucket = buckets.get(0);
            Assert.assertTrue(table.getFileReader().exists(bucket));

            String localBucketFile = PathUtils.resolve(table.getRootPath(), DataUtil.toFile( bucket, DataType.LINK));

            Assert.assertEquals("s3data://project/" +  bucket + "\n", Files.readString(Path.of(localBucketFile)));

            String[] bucketResult = runGorPipeServer("gor " + localBucketFile,
                    workDirPath.resolve("some_project").toString(), fileReader.getSecurityContext()).split("\n");
            Assert.assertEquals("Did not retrieve the correct data", 759, bucketResult.length);
        } finally {
            // Manual cleanup as this is S3.
            if (buckets != null) {
                man.deleteBuckets(table, true, buckets.toArray(new String[buckets.size()]));
            }
        }
    }

    private String createDictionary(String parentPath, boolean useHistory) throws IOException {
        String dictPath = parentPath +  "/dict.gord";
        fileReader.createDirectories(parentPath);
        DictionaryTable dict = new DictionaryTable.Builder<>(dictPath).useHistory(useHistory).fileReader(fileReader).build();

        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053023D.wgs.genotypes.gorz", dict.getRootPath()).alias("D3_WGC053023D").build());
        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053033D.wgs.genotypes.gorz", dict.getRootPath()).alias("D3_WGC053033D").build());
        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053043D.wgs.genotypes.gorz", dict.getRootPath()).alias("D3_WGC053043D").build());
        dict.save();

        return dictPath;
    }

    private String createDictionaryFolder(String parentPath, boolean useHistory) throws IOException {
        String dictPath = parentPath +  "/dict.gord/";
        fileReader.createDirectories(dictPath);
        DictionaryTable dict = new DictionaryTable.Builder<>(dictPath + DEFAULT_FOLDER_DICTIONARY_NAME).useHistory(useHistory).fileReader(fileReader).build();

        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053023D.wgs.genotypes.gorz", dict.getRootPath()).alias("D3_WGC053023D").build());
        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053033D.wgs.genotypes.gorz", dict.getRootPath()).alias("D3_WGC053033D").build());
        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/sim20-micro/source/var/D3_WGC053043D.wgs.genotypes.gorz", dict.getRootPath()).alias("D3_WGC053043D").build());
        dict.save();

        return dictPath;
    }

    @Test
    public void testBucketizeS3TableS3DataS3Buckets() throws IOException {
        String name = "testBucketizeS3TableS3DataS3Buckets";
        String remoteTestDir = "s3://nextcode-unittest/tmp/" + name + "_" + UUID.randomUUID();
        String dictPath = createDictionary(remoteTestDir, false);

        try {
            DictionaryTable table = new DictionaryTable.Builder<>(dictPath).fileReader(fileReader).useHistory(false).build();
            Assert.assertEquals(remoteTestDir, table.getRootPath());

            TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(1).lockType(NoTableLock.class).fileReader(fileReader).build();
            man.bucketize(table.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);

            table.reload();
            Assert.assertEquals("New lines should not be bucketized", 0, table.needsBucketizing().size());

            List<String> buckets = table.getBuckets();
            Assert.assertEquals(1, buckets.size());
            String bucket = buckets.get(0);
            Assert.assertTrue(table.getFileReader().exists(PathUtils.resolve(table.getRootPath(), bucket)));

            String res = TestUtils.runGorPipeServer("gor " + dictPath + " | top 1", workDirPath.toString(), fileReader.getSecurityContext());
            Assert.assertEquals("CHROM\tPOS\tReference\tCall\tCallCopies\tCallRatio\tDepth\tGL_Call\tFILTER\tFS\tformatZip\tSource\n" +
                    "chr1\t10403\tACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAAC\tA\t1\t0.471\t17\t277\tPASS\t5.315\tAlt=A:GT=0/1,AD=9,8,DP=17,GQ=99,PL=277,0,318\tD3_WGC053023D\n", res);
        } finally {
            // Manual cleanup as this is S3.
            fileReader.deleteDirectory(remoteTestDir);
        }
    }

    @Test
    public void testBucketizeS3TableFolderS3DataS3Bucket() throws IOException {
        String name = "testBucketizeS3TableFolderS3DataS3Bucket";
        String remoteTestDir = "s3://nextcode-unittest/tmp/" + name + "_" + UUID.randomUUID();
        String dictPath = createDictionaryFolder(remoteTestDir, false);

        try {
            DictionaryTable table = new DictionaryTable.Builder<>(dictPath).fileReader(fileReader).useHistory(false).build();
            Assert.assertEquals(dictPath, PathUtils.markAsFolder(table.getRootPath()));

            TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(1).lockType(NoTableLock.class).fileReader(fileReader).build();
            man.bucketize(table.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);

            table.reload();
            Assert.assertEquals("New lines should not be bucketized", 0, table.needsBucketizing().size());

            List<String> buckets = table.getBuckets();
            Assert.assertEquals(1, buckets.size());
            String bucket = buckets.get(0);
            Assert.assertTrue(table.getFileReader().exists(PathUtils.resolve(table.getRootPath(), bucket)));

            String res = TestUtils.runGorPipeServer("gor " + dictPath + " | top 1", workDirPath.toString(), fileReader.getSecurityContext());
            Assert.assertEquals("CHROM\tPOS\tReference\tCall\tCallCopies\tCallRatio\tDepth\tGL_Call\tFILTER\tFS\tformatZip\tSource\n" +
                    "chr1\t10403\tACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAAC\tA\t1\t0.471\t17\t277\tPASS\t5.315\tAlt=A:GT=0/1,AD=9,8,DP=17,GQ=99,PL=277,0,318\tD3_WGC053023D\n", res);
        } finally {
            // Manual cleanup as this is S3.
            fileReader.deleteDirectory(remoteTestDir);
        }
    }

    @Test
    public void testBucketizeS3TableFolderS3DataS3BucketUsingLink() throws IOException {
        String name = "testBucketizeS3TableFolderS3DataS3BucketUsingLink";
        String remoteTestDir = "s3://nextcode-unittest/tmp/" + name + "_" + UUID.randomUUID();
        String dictPath = createDictionaryFolder(remoteTestDir, false);

        try {
            Path linkPath = workDirPath.resolve("local.gord.link");
            Files.writeString(linkPath, dictPath);

            DictionaryTable table = new DictionaryTable.Builder<>(linkPath).fileReader(fileReader).useHistory(false).build();
            Assert.assertEquals(dictPath, PathUtils.markAsFolder(table.getRootPath()));

            TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(1).lockType(NoTableLock.class).fileReader(fileReader).build();
            man.bucketize(table.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);

            table.reload();
            Assert.assertEquals("New lines should not be bucketized", 0, table.needsBucketizing().size());

            List<String> buckets = table.getBuckets();
            Assert.assertEquals(1, buckets.size());
            String bucket = buckets.get(0);
            Assert.assertTrue(table.getFileReader().exists(PathUtils.resolve(table.getRootPath(), bucket)));

            String res = TestUtils.runGorPipeServer("gor " + linkPath + " | top 1", workDirPath.toString(), fileReader.getSecurityContext());
            Assert.assertEquals("CHROM\tPOS\tReference\tCall\tCallCopies\tCallRatio\tDepth\tGL_Call\tFILTER\tFS\tformatZip\tSource\n" +
                    "chr1\t10403\tACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAAC\tA\t1\t0.471\t17\t277\tPASS\t5.315\tAlt=A:GT=0/1,AD=9,8,DP=17,GQ=99,PL=277,0,318\tD3_WGC053023D\n", res);
        } finally {
            // Manual cleanup as this is S3.
            fileReader.deleteDirectory(remoteTestDir);
        }
    }
}
