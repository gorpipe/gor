package org.gorpipe.s3.driver;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.gor.binsearch.SeekableIterator;
import org.gorpipe.gor.binsearch.StringIntKey;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.test.IntegrationTests;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

/**
 * Created by gisli on 06/10/15.
 */
@Category(IntegrationTests.class)
public class UTestS3SeekableFile {

    static private final Logger log = LoggerFactory.getLogger(UTestS3SeekableFile.class);

    private static String S3_KEY_3;
    private static String S3_SECRET_3;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY_3 = props.getProperty("S3_KEY_3");
        S3_SECRET_3 = props.getProperty("S3_SECRET_3");

        System.setProperty("gor.s3.access.key", S3_KEY_3);
        System.setProperty("gor.s3.secret.key", S3_SECRET_3);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Ignore("Fails on linux")
    @Test
    public void testBasicGor() throws Exception {
        File result = File.createTempFile("TestS3SeekableFile", ".gor");
        result.deleteOnExit();

        String query = "s3://gor-speed-uswest/test.gor -p 1:1000000-1001000 | write " + result.getCanonicalPath();
        long startTime = System.currentTimeMillis();
        TestS3SeekableFile.runGorPipe(query);
        log.info("Basic query on gor file executed in {} ms", System.currentTimeMillis() - startTime);
        Assert.assertEquals(
                "chr1\t1000805\t1000897\tHWI-ST302:221:C0KVJACXX:5:1210:5521:20914\t147\t60\t93M\t\t1\t1000733\t-165\tAGCGGGAAGGCCAGGCAGGGCTTCTGGGTGGAGTTCAAGGTGCATCCTGACCGCTGTCACCTTCAGACTCTGTCCCCTGGGGCTGGGGCAAGT\t>=9ADDCCDDDEEEDEDDDCCACBBDCACDDB@@@BACBABBC@@AAA@?A;AA@?@A?AA?@@A@?A?A@>@@AA@@???AA@@??A@<>=>\tBD=PNMLOOPQQQOPOPPPOLOPNKMOMLNIMLLMKJMKLMMHMNLLKNNLMMLMONHMMHMNMJMNKMNLLNHMKKKNNLKLOPONMMQQOMNLL PG=MarkDuplicates RG=13403.mo.1 BI=SPNMMNQSSTPSRRQSRNQSQLPRNMPLMINONKOONPOKPOLOLOQNOOLMQPJNNINMNJNPMOLNNPJNKLLNPLKKPRQMMMQQQOPNN NM=0 MQ=60 AS=93 XS=0 RB=hg19",
                getLine(result, 7));
    }

    @Ignore("Fails on linux")
    @Test
    public void testBasicGorz() throws Exception {
        File result = File.createTempFile("TestS3SeekableFile", ".gor");
        result.deleteOnExit();

        String query = "s3://gor-speed-uswest/test.gorz -p 1:1000000-1001000 | write " + result.getCanonicalPath();
        long startTime = System.currentTimeMillis();
        TestS3SeekableFile.runGorPipe(query);
        log.info("Basic query on gorz file executed in {} ms", System.currentTimeMillis() - startTime);
        Assert.assertEquals(
                "chr1\t1000805\t1000897\tHWI-ST302:221:C0KVJACXX:5:1210:5521:20914\t147\t60\t93M\t\t1\t1000733\t-165\tAGCGGGAAGGCCAGGCAGGGCTTCTGGGTGGAGTTCAAGGTGCATCCTGACCGCTGTCACCTTCAGACTCTGTCCCCTGGGGCTGGGGCAAGT\t>=9ADDCCDDDEEEDEDDDCCACBBDCACDDB@@@BACBABBC@@AAA@?A;AA@?@A?AA?@@A@?A?A@>@@AA@@???AA@@??A@<>=>\tBD=PNMLOOPQQQOPOPPPOLOPNKMOMLNIMLLMKJMKLMMHMNLLKNNLMMLMONHMMHMNMJMNKMNLLNHMKKKNNLKLOPONMMQQOMNLL PG=MarkDuplicates RG=13403.mo.1 BI=SPNMMNQSSTPSRRQSRNQSQLPRNMPLMINONKOONPOKPOLOLOQNOOLMQPJNNINMNJNPMOLNNPJNKLLNPLKKPRQMMMQQQOPNN NM=0 MQ=60 AS=93 XS=0 RB=hg19",
                getLine(result, 7));
    }

    @Ignore("Fails on linux")
    @Test
    public void testBasicBam() throws Exception {
        File result = File.createTempFile("TestS3SeekableFile", ".gor");
        result.deleteOnExit();

        String query = "s3://gor-speed-uswest/chunk.bam -p 1:1000000-1001000 | write " + result.getCanonicalPath();
        long startTime = System.currentTimeMillis();
        TestS3SeekableFile.runGorPipe(query);
        log.info("Basic query on bam file executed in {} ms", System.currentTimeMillis() - startTime);
        Assert.assertEquals(
                "chr1\t1000805\t1000897\tHWI-ST302:221:C0KVJACXX:5:1210:5521:20914\t147\t60\t93M\t\t1\t1000733\t-165\tAGCGGGAAGGCCAGGCAGGGCTTCTGGGTGGAGTTCAAGGTGCATCCTGACCGCTGTCACCTTCAGACTCTGTCCCCTGGGGCTGGGGCAAGT\t>=9ADDCCDDDEEEDEDDDCCACBBDCACDDB@@@BACBABBC@@AAA@?A;AA@?@A?AA?@@A@?A?A@>@@AA@@???AA@@??A@<>=>\tBD=PNMLOOPQQQOPOPPPOLOPNKMOMLNIMLLMKJMKLMMHMNLLKNNLMMLMONHMMHMNMJMNKMNLLNHMKKKNNLKLOPONMMQQOMNLL PG=MarkDuplicates RG=13403.mo.1 BI=SPNMMNQSSTPSRRQSRNQSQLPRNMPLMINONKOONPOKPOLOLOQNOOLMQPJNNINMNJNPMOLNNPJNKLLNPLKKPRQMMMQQQOPNN NM=0 MQ=60 AS=93 XS=0 RB=hg19",
                getLine(result, 7));
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
     * Helper method to get specific lin from file.
     *
     * @param file
     * @param lineNum
     * @return
     * @throws IOException
     */
    private String getLine(File file, int lineNum) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            for (int i = 0; i < lineNum - 1; i++)
                br.readLine();
            return br.readLine();
        } finally {
            if (br != null) br.close();
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
