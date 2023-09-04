package org.gorpipe.s3.driver;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import gorsat.TestUtils;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.gor.binsearch.SeekableIterator;
import org.gorpipe.gor.binsearch.StringIntKey;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.test.IntegrationTests;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

/**
 * Created by gisli on 06/10/15.
 */
@Category(IntegrationTests.class)
public class UTestS3SeekableFile {

    static private final Logger log = LoggerFactory.getLogger(UTestS3SeekableFile.class);

    private static String S3_KEY;
    private static String S3_SECRET;
    private static String S3_REGION = "us-west-2";


    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public final ProvideSystemProperty s3AcessKey
            = new ProvideSystemProperty("aws.accessKeyId", S3_KEY);

    @Rule
    public final ProvideSystemProperty s3Secret
            = new ProvideSystemProperty("aws.secretKey", S3_SECRET);


    String gorzFileName = "s3://nextcode-unittest/csa_test_data/data_sets/ref/versions/hg19/dbsnp.gorz";
    String gorFileName = "s3://nextcode-unittest/csa_test_data/data_sets/ref/versions/hg19/dbsnp.gor";

    @BeforeClass
    public static void setUpClass() throws Exception {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_KEY");
        S3_SECRET = props.getProperty("S3_SECRET");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    //@Ignore("Fails on linux")
    @Test
    public void testBasicGor() throws Exception {
        String query = gorFileName + " -p chr2:1000000-1001000";
        long startTime = System.currentTimeMillis();
        String result = TestUtils.runGorPipe(query, true, DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET), null);
        log.info("Basic query on gor file executed in {} ms", System.currentTimeMillis() - startTime);
        Assert.assertEquals("chr2\t1000142\tG\tC\trs567366174", result.split("\n")[6]);
    }

    //@Ignore("Fails on linux")
    @Test
    public void testBasicGorz() throws Exception {
        String query = gorzFileName +  " -p chr2:1000000-1001000";
        long startTime = System.currentTimeMillis();
        String result = TestUtils.runGorPipe(query, true, DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET), null);
        log.info("Basic query on gorz file executed in {} ms", System.currentTimeMillis() - startTime);
        Assert.assertEquals("chr2\t1000142\tG\tC\trs567366174", result.split("\n")[6]);
    }

    @Test
    public void testBasicSeekStream() throws Exception {
        String query = "gor " + gorzFileName +  " -p chr2:1000000- | top 100000 | group chrom -count";
        long startTime = System.currentTimeMillis();
        String result = TestUtils.runGorPipe(query, true, DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET), null);
        log.info("Basic seek/stream on gorz file executed in {} ms", System.currentTimeMillis() - startTime);
        Assert.assertEquals("chr2\t0\t250000000\t100000", result.split("\n")[1]);
    }

    //@Ignore("Fails on linux")
    @Test
    public void testBasicBam() throws Exception {
        String query = "s3://nextcode-unittest/csa_test_data/data_sets/bvl_min/bam/BVL_FATHER_SLC52A2.bam -p chr8:1285160-";
        long startTime = System.currentTimeMillis();
        String result = TestUtils.runGorPipe(query, true, DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET), null);
        log.info("Basic query on bam file executed in {} ms", System.currentTimeMillis() - startTime);
        Assert.assertEquals(
                "chr8	1285160	1285260	WPHISEQ02:158:D0E1CACXX:6:2205:21405:138412	163	29	101M	92A8	8	1285333	282	AGGAGCAGAGGCAATGAGTCTCTAGAATAGTCATTGGTGATCAGTACATAGTAACCAGTAATGACAGTGTGTGACATGATGAGAAGATAGAGTGGAGGAGG	@@@DFFDEFHHGA:C>ABEHHHG9FA<AFECGDGIIFJICHBGCG??BFGGIHGHIGHJGIJIGIGEHGIJJGGIIIEEHCA=)7;37;B3@.;(.55(,3	X0=1 X1=0 RG=111129_HiSeq02_0158_BD0E1CACXX.s_6.012 XG=0 AM=29 NM=1 SM=29 XM=1 XO=0 MQ=29 XT=U RB=hs37d5",
                result.split("\n")[1]);
    }

    @Ignore("Must update to new gordriver")
    @Test
    public void testFilePositionCache() throws Exception {

        // Connect to amazon account providing credentials and configuring proxies as needed
        final AWSCredentials myCredentials = new BasicAWSCredentials(System.getProperty("gor.s3.access.key"), System.getProperty("gor.s3.secret.key"));
        final ClientConfiguration cc = new ClientConfiguration();
        cc.setProtocol(Protocol.HTTP);
        cc.setConnectionTimeout(120 * 1000);
        cc.setMaxErrorRetry(15);
        final AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(myCredentials))
                .withClientConfiguration(cc)
                .build();

        /*
         * TODO: Use SeekableFile adapter from new gordriver before reactivating these test.
         */
        String fileName = null;
        StreamSourceSeekableFile file = null; //new S3SeekableFile(s3Client, "gor-speed-uswest", "test.gor", 0);


        // Do random seeks to test cache and performance.

        try {
            long[] seeds = {System.nanoTime()};
            for (long seed : seeds) {
                log.info("testFilePositionCache - Random seed: {}", seed);
                Random randomGen = new Random(seed);
                // Ran
                //   gorpipe "gor s3://gor-speed-uswest/test.gor?profile=default | group chrome -ic end -max"
                // to find max end.
                doRandomSeeks(file,10, new int[]{1}, 19513143, randomGen);
            }
        } finally {
            file.close();
        }
    }

    /**
     * Helper method to test cache seeks.  The seeks are tested by doing cache and non-cache seeks
     * and compare the results.
     *
     * @param file       file to seek into.
     * @param seekCount  number of seeks to perform.
     * @param chrs       array with the chromosome to seek into.
     * @param rowsPerChr rows per chromosome.
     */
    private void doRandomSeeks(StreamSourceSeekableFile file, int seekCount, int[] chrs, int rowsPerChr, Random randomGen) throws IOException {
        StringIntKey[] keys = new StringIntKey[seekCount];
        String[] cachedResults = new String[seekCount];
        String[] noCachedResults = new String[seekCount];

        // Create keys.

        for (int seekCounter = 0; seekCounter < seekCount; seekCounter++) {
            String chr = "chr" + chrs[randomGen.nextInt(chrs.length)];
            int pos = randomGen.nextInt(rowsPerChr) + 1;
            keys[seekCounter] = new StringIntKey(0, 1, chr, pos, StringIntKey.cmpLexico);
            log.debug("Key: {}", keys[seekCounter]);
        }

        // Run with cache.


        SeekableIterator cbis = new SeekableIterator(file, true);
        long startTime = System.currentTimeMillis();
        try {
            for (int seekCounter = 0; seekCounter < seekCount; seekCounter++) {
                log.trace("Cache seeking: {}", keys[seekCounter]);
                cbis.seek(keys[seekCounter]);
                cachedResults[seekCounter] = cbis.getNextAsString();

            }
        } finally {
            cbis.close();
        }
        log.info("Ran {} s3 seeks using cache in {} ms", seekCount, System.currentTimeMillis() - startTime);
        //log.info("Average file seeks {} per seek (total seeks {}).", cbis.getAverageSeekCount(), cbis.getTotalFileSeekCount());

        // Run with cache, known locations.
        cbis = new SeekableIterator(file, true);
        startTime = System.currentTimeMillis();
        try {
            for (int seekCounter = 0; seekCounter < seekCount; seekCounter++) {
                log.trace("Cache seeking (known location): {}", keys[seekCounter]);
                cbis.seek(keys[seekCounter]);
                Assert.assertTrue(cbis.hasNext());
            }
        } finally {
            cbis.close();
        }

        log.info("Ran {} s3 seeks using cache to known locations in {} ms", seekCount, System.currentTimeMillis() - startTime);
        //log.info("Average file seeks {} per seek (total seeks {}).", cbis.getAverageSeekCount(), cbis.getTotalFileSeekCount());

        // Run without cache.
        SeekableIterator ncbis = new SeekableIterator(file, true);
        startTime = System.currentTimeMillis();
        try {
            for (int seekCounter = 0; seekCounter < seekCount; seekCounter++) {
                log.trace("No cache seeking: {}", keys[seekCounter]);
                ncbis.seek(keys[seekCounter]);
                noCachedResults[seekCounter] = ncbis.getNextAsString();

            }
        } finally {
            ncbis.close();
        }
        log.info("Ran {} s3 seeks no cache in {} ms", seekCount, System.currentTimeMillis() - startTime);
        //log.info("Average file seeks %d per seek (total seeks {}).", ncbis.getAverageSeekCount(), ncbis.getTotalFileSeekCount());

        // Compare results.
        for (int seekCounter = 0; seekCounter < seekCount; seekCounter++) {
            StringIntKey key = keys[seekCounter];
            String noCacheRow = noCachedResults[seekCounter];
            String cacheRow = cachedResults[seekCounter];
            log.debug("Key: {}", key);
            log.debug("No cache: {}", noCacheRow);
            log.debug("Cache: {}", cacheRow);
            // Check that cache row is not larger less than the key.
            if (noCacheRow != null) {
                String[] cols = noCacheRow.split("\t", 3);
                StringIntKey foundKey = new StringIntKey(0, 1, cols[0], Integer.parseInt(cols[1]), StringIntKey.cmpLexico);
                Assert.assertTrue(String.format("Compare search key (%s) to found row (%s) (no cache)", key, foundKey),
                        key.compareTo(foundKey) <= 0);
            }
            // Check if cacheRow is correct.
            Assert.assertEquals("Compare no cache row to cache row", noCacheRow, cacheRow);
        }

    }

    public static void main(String... args) {
        System.out.println(System.getProperty("logback.configurationFile"));
        try {
            setUpClass();

            UTestS3SeekableFile tester = new UTestS3SeekableFile();

            //tester.testBasicGor();
            //tester.testBasicGorz();
            //tester.testBasicBam();

            tester.testFilePositionCache();

            tearDownClass();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
