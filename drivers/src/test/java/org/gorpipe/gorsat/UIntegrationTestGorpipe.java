package org.gorpipe.gorsat;

import gorsat.TestUtils;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.test.IntegrationTests;
import org.gorpipe.utils.DriverUtils;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.experimental.categories.Category;

import java.util.Properties;

/**
 * Used to collect needed integration tests for gorpipe cases.
 */
@SuppressWarnings("unused")
@Category(IntegrationTests.class)
public class UIntegrationTestGorpipe {

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Rule
    public SystemErrRule systemErrRule = new SystemErrRule().enableLog().muteForSuccessfulTests();

    private static String S3_KEY;
    private static String S3_SECRET;

    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_KEY");
        S3_SECRET = props.getProperty("S3_SECRET");

        // Ensure no fallback keys
        System.setProperty("aws.accessKeyId", "");
        System.setProperty("aws.secretKey", "");
    }

    /**
     * This test is created to ensure we don't regress on issue GOR-447 that was inadvertently caused when issue
     * GOR-436 was fixed.
     */
    @Ignore // Disable for now as it takes too long and is unlikely to fail.
    @Test
    public void testLargeVcfGzFileGenomeGroupCount() {
        environmentVariables.set("AWS_ACCESS_KEY_ID", S3_KEY);
        environmentVariables.set("AWS_SECRET_ACCESS_KEY", S3_SECRET);

        String path = "s3://gdb-unit-test-data/csa_test_data/data_sets/gor_driver_testfiles/test_vcf_genome_count.vcf.gz";
        String query = String.format("gor %s | group genome -count", path);

        try (GenomicIterator iterator = TestUtils.runGorPipeIterator(query)) {
            Assert.assertTrue(iterator.hasNext());
            String result = iterator.next().toString();
            Assert.assertEquals("chrA	0	1000000000	4784605", result.trim());
        }
    }
    
    @Ignore  // Test is unstable so we are silencing it temporarily while debugging /develop.
    @Test
    public void testFileClosingOnParseError() throws Exception {
        String fileLeft = "../tests/data/gor/dbsnp_test.gorz";
        String fileRight = "../tests/data/dbsnp_test.gor";
        String query = String.format("gor %s | join -snpsnp %s | crap | group genome -count", fileLeft, fileRight);
        // NOTE:  For the gc test to work below we need to run about 100 times.  Can we detect file handle leaks in a different way?

        // TODO:  See comment below.
        //long openAtStart = TestUtils.countOpenFiles();

        for (int i = 0; i < 100; i++) {
            try (GenomicIterator iterator = TestUtils.runGorPipeIterator(query)) {
                iterator.next().toString();
            } catch (Exception ex) {
                //System.out.println(ex.getMessage());
                if (ex.getMessage() == null || !ex.getMessage().toLowerCase().contains("step: crap")) {
                    // Got unexpected error
                    throw ex;
                }
            }
        }
        // not a guarantee that the finalizer will perform correctly, test is also unstable so we are silencing the test temporarily while debugging /develop.

        // TODO:  In theory this is much better way to this test (rather than using the fact the the gc catches the error in the finalize code) but
        //        currently it is not working as the number of open files depends not only on this test/thread but other threads/process as weill.
        //long openAtEnd = TestUtils.countOpenFiles();
        //Assert.assertEquals("Number of files open at start and end of test differ. FD leakage detected.", openAtStart, openAtEnd);
        // this test is forcing garbage collection by looping several times, but it is an unstable test and may be failing due to co-dependencies with tests and/or machine processes that are
        // creating or removing files
        System.gc();
        Assert.assertFalse("Parse error causes file handle leak", systemErrRule.getLog().contains("GenomicOrderRows - not closed on finalize"));
    }
}
