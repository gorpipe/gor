package org.gorpipe.gor.driver.providers.stream.datatypes;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.utils.TestUtils;
import org.gorpipe.gor.model.GenomicIterator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class BvlTestSuite {
    // All available names are: "BVL_FATHER_SLC52A2", "BVL_INDEX_SLC52A2", "BVL_MOTHER_SLC52A2", "BVL_SISTER_SLC52A2"
    // but we only use one of them to speed up the tests
    protected String[] names = {"BVL_INDEX_SLC52A2"};

    protected class ReadResults {
        String result;
        int lines;

        public ReadResults(String result, int lines) {
            this.result = result;
            this.lines = lines;
        }
    }

    @Before
    public void setup() throws Exception {

    }

    protected String securityContext() throws IOException {
        return null;
    }

    @Test
    public void testUnknownSourceProvidesFileInfo() throws IOException {
        String source = getSourcePath("bam/not-there.bam");

        try {
            GenomicIterator iterator = TestUtils.gorDriver.createIterator(new SourceReference(source));
            StringBuilder builder = new StringBuilder();
            TestUtils.addHeader(builder, iterator);
            Assert.fail("Should get exception on nonexisting file");
        } catch (GorResourceException ge) {
            if (ge.getMessage() == null) {
                throw new RuntimeException("Null message", ge);
            }
            if (!ge.getUri().contains(source)) {
                throw ge;
            }
        } catch (Exception e) {
            if (e.getMessage() == null) {
                throw new RuntimeException("Null message", e);
            }
            if (!e.getMessage().contains(source)) {
                throw e;
            }
        }

    }

    @Test
    public void testBam() throws IOException {
        for (String name : names) {
            String source = getSourcePath("bam/" + name + ".bam");
            String testFile = "bvl_min/derived/raw_bam_to_gor/" + name + ".bam.gor";

            TestUtils.assertFullGor(securityContext(), source, readTestFile(testFile));

            ReadResults expectedRange = readTestFileRange(testFile, "chr8", 145583099, 10);
            assertSeek(source, "chr8", 145583099, expectedRange.lines, expectedRange.result);
        }
    }

    @Test
    public void testVcfGz() throws IOException {
        for (String name : names) {
            String source = getSourcePath("vcf/" + name + ".vcf.gz");
            String testFile = "bvl_min/derived/raw_vcf_to_gor/" + name + ".vcf.gz.gor";

            TestUtils.assertFullGor(securityContext(), source, readTestFile(testFile));

            ReadResults expectedRange = readTestFileRange(testFile, "chr8", 145579932, 5);
            assertSeek(source, "chr8", 145579932, expectedRange.lines, expectedRange.result);
        }
    }

    @Test
    public void testVcf() throws IOException {
        for (String name : names) {
            String source = getSourcePath("derived/raw_vcf/" + name + ".vcf");
            String testFile = "bvl_min/derived/raw_vcf_to_gor/" + name + ".vcf.gz.gor";

            TestUtils.assertFullGor(securityContext(), source, readTestFile(testFile));
        }
    }

    @Test
    public void testGor() throws IOException {
        for (String name : names) {
            String source = getSourcePath("derived/raw_bam_to_gor/" + name + ".bam.gor");
            String testFile = "bvl_min/derived/raw_bam_to_gor/" + name + ".bam.gor";

            TestUtils.assertFullGor(securityContext(), source, readTestFile(testFile));

            ReadResults expectedRange = readTestFileRange(testFile, "chr8", 145583099, 10);
            assertSeek(source, "chr8", 145583099, expectedRange.lines, expectedRange.result);
        }
    }

    @Test
    public void testGorz() throws IOException {
        for (String name : names) {
            String source = getSourcePath("derived/raw_bam_to_gor/" + name + ".bam.gorz");
            String testFile = "bvl_min/derived/raw_bam_to_gor/" + name + ".bam.gor";

            TestUtils.assertFullGor(securityContext(), source, readTestFile(testFile));

            ReadResults expectedRange = readTestFileRange(testFile, "chr8", 145583099, 10);
            assertSeek(source, "chr8", 145583099, expectedRange.lines, expectedRange.result);
        }
    }

    protected abstract String getSourcePath(String name);

    protected String readTestFile(String testFile) throws IOException {
        Path testDataPath = Paths.get("../tests/data/" + testFile);
        return TestUtils.readFile(testDataPath.toFile());
    }

    protected ReadResults readTestFileRange(String name, String chrom, int pos, int count) throws IOException {
        String[] lines = readTestFile(name).split("\n");
        StringBuilder result = new StringBuilder();
        result.append(lines[0]);
        result.append("\n");
        boolean found = false;
        int read = 0;
        for (int i = 1; i < lines.length; i++) {
            String[] cols = lines[i].split("\t");
            if (found || (chrom.equals(cols[0]) && Integer.valueOf(cols[1]) >= pos)) {
                found = true;
                result.append(lines[i]);
                result.append("\n");
                read++;
            }
            if (read >= count)
                break;
        }
        if (read == 0) {
            Assert.fail("Test file range(" + name + "," + chrom + "," + pos + "," + count + ") didnt find any lines");
        }
        return new ReadResults(result.toString(), read);
    }

    /**
     * Seek on source gor on source and compare to expected data. Data should include header
     */
    protected void assertSeek(String source, String chr, int pos, int lineCount, String expectedData) throws IOException {
        GenomicIterator iterator = TestUtils.gorDriver.createIterator(sourceRef(source));
        iterator.init(null);

        StringBuilder builder = new StringBuilder();

        TestUtils.addHeader(builder, iterator);
        iterator.seek(chr, pos);
        TestUtils.addLines(builder, iterator, lineCount);

        Assert.assertEquals(expectedData, builder.toString());
    }

    protected SourceReference sourceRef(String source) throws IOException {
        return new SourceReferenceBuilder(source).securityContext(securityContext()).build();
    }
}
