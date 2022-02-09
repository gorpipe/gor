package org.gorpipe.s3.table;

import org.gorpipe.utils.DriverUtils;
import gorsat.TestUtils;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.gor.manager.BucketManager;
import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.Properties;

/**
 * Note, there are S3Shared integration tests in gor-services (ITestS3Shared).
 */
public class UTestS3Table {

    private static String S3_KEY;
    private static String S3_SECRET;

    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue
            = new ProvideSystemProperty("aws.accessKeyId", S3_KEY);

    @Rule
    public final ProvideSystemProperty otherPropertyIsMissing
            = new ProvideSystemProperty("aws.secretKey", S3_SECRET);

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private Path gordFile;

    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_KEY");
        S3_SECRET = props.getProperty("S3_SECRET");
    }

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    private void prepareTableGordFile() {
        gordFile = workDirPath.resolve("dict.gord");
        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).sourceColumn("Special").build();

        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/bvl_min_gor/derived/raw_vcf_to_gor/BVL_FATHER_SLC52A2.vcf.gz.gor", dict.getRootUri()).alias("BVL_FATHER_SLC52A2").build());
        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/bvl_min_gor/derived/raw_vcf_to_gor/BVL_MOTHER_SLC52A2.vcf.gz.gor", dict.getRootUri()).alias("BVL_MOTHER_SLC52A2").build());
        dict.insert(new DictionaryEntry.Builder<>("s3://nextcode-unittest/csa_test_data/data_sets/bvl_min_gor/derived/raw_vcf_to_gor/BVL_INDEX_SLC52A2.vcf.gz.gor", dict.getRootUri()).alias("BVL_INDEX_SLC52A2").build());
        dict.save();
    }

    @Ignore
    @Test
    public void testBasicInsert() {
        prepareTableGordFile();

        String[] result = TestUtils.runGorPipeLines("gor " + gordFile.toString());
        Assert.assertEquals("Did not retreive the correct data", 10,  result.length);

    }

    @Ignore
    @Test
    public void testBucketizeLocal() {
        prepareTableGordFile();

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).sourceColumn("Special").build();

        TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(2).build();
        man.bucketize(dict.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);
    }

    @Ignore
    @Test
    public void testBucketizeS3() {
        prepareTableGordFile();

        DictionaryTable dict = new DictionaryTable.Builder<>(gordFile).sourceColumn("Special").build();

        TableManager man = TableManager.newBuilder().bucketSize(3).minBucketSize(2).build();
        man.bucketize(dict.getPath(), BucketManager.BucketPackLevel.NO_PACKING, 1, 1000, null);
    }


    private String createSecurityContext(String service, String bucket, Credentials.OwnerType ownerType, String owner) {
        Credentials creds = new Credentials.Builder().service(service).lookupKey(bucket).ownerType(ownerType).ownerId(owner)
                .set("key", "dummykey")
                .set("secret", "dummysecret")
                .build();
        BundledCredentials bundleCreds = new BundledCredentials.Builder().addCredentials(creds).build();
        return bundleCreds.addToSecurityContext("");

    }
}
