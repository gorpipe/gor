/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package gorsat;

import org.apache.commons.lang3.SystemUtils;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.model.DbConnection;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

/**
 * Created by sigmar on 01/02/16.
 */
public class UTestProcessSource {

    private File gorFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void seUtUp() throws Exception {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
    }

    @Test
    public void testNestedProcess() {
        // No cat on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String query = "cmd {cat <(gor ../tests/data/gor/genes.gor | top 5) <(cmd {tail -n +2 ../tests/data/gor/genes.gor} | top 5)} | top 10";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(10, count);
    }

    @Test
    public void testSimpleProcessSourceAlias() {
        // No gunzip on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String gorcmd = "gorcmd {gunzip -c ../tests/data/gor/dbsnp_test.gor.gz} | top 10";
        Assert.assertEquals(10, TestUtils.runGorPipeCount(gorcmd));
    }


    @Test
    public void testSimpleProcessSource() {
        // No gunzip on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String gorcmd = "cmd {gunzip -c ../tests/data/gor/dbsnp_test.gor.gz} | top 10";
        Assert.assertEquals(10, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testVcfProcessSource() {
        // No gunzip on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String gorcmd = "cmd -s vcf {gunzip -c ../tests/data/external/samtools/test.vcf.gz} | top 10";
        Assert.assertEquals(5, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testSimpleNorProcessSource() throws IOException {
        // No cat on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String gorcmd = "cmd -n {cat " + gorFile.getCanonicalPath() + "} | top 10";
        Assert.assertEquals(9, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need samtools installed and in default path on test machine")
    @Test
    public void testProcessSourceSeek() {
        String gorcmd = "gor -p chr3:60700- <(cmd {-ssam ssh localhost /usr/local/bin/samtools view -h gor/tests/data/external/samtools/serialization_test.bam #(S:chr:pos)})";
        Assert.assertEquals(1, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need samtools installed and in default path on test machine")
    @Test
    public void testWhitelistedSamProcessSource() {
        String gorcmd = "samtools ../tests/data/external/samtools/serialization_test.bam";
        Assert.assertEquals(45, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need samtools installed and in default path on test machine")
    @Test
    public void testWhitelistedSamNestedProcessSource() {
        String gorcmd = "gor <(samtools ../tests/data/external/samtools/serialization_test.bam)";
        Assert.assertEquals(45, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need samtools installed and in default path on test machine")
    @Test
    public void testSamProcessSource() {
        String gorcmd = "gor <(cmd {-ssam samtools view -h ../tests/data/external/samtools/serialization_test.bam})";
        Assert.assertEquals(45, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need samtools installed and in default path on test machine")
    @Test
    public void testUBamProcessSource() {
        String gorcmd = "gor <(cmd -s {bam samtools view -u ../tests/data/external/samtools/serialization_test.bam})";

        Assert.assertEquals(45, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need samtools installed and in default path on test machine")
    @Test
    public void testBamProcessSource() {
        String gorcmd = "gor <(cmd -sbam samtools view -b ../tests/data/external/samtools/serialization_test.bam #(S:chr:pos))";

        Assert.assertEquals(45, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need gorpipe installed and in default path on test machine")
    @Test
    public void testProcessSourceGorpipe() {
        String gorcmd = "gor <(cmd gorpipe 'gor ../tests/data/external/samtools/serialization_test.bam')";
        Assert.assertEquals(45, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need gorpipe installed and in default path on test machine")
    @Test
    public void testProcessSourceGorpipeSeek() {
        String gorcmd = "gor -pchr3:60700- <(cmd gorpipe 'gor #(S:-pchr:pos-) ../tests/data/external/samtools/serialization_test.bam')";

        Assert.assertEquals(1, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testNestedProcessSourceGunzipGor() {
        // No gunzip on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String gorcmd = "gor <(cmd {gunzip -c ../tests/data/gor/dbsnp_test.gor.gz}) | top 10";
        Assert.assertEquals(10, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testNestedProcessSourceGunzipGorWithAnalyzisStep() {
        // No gunzip on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String gorcmd = "gor <(cmd {gunzip -c ../tests/data/gor/dbsnp_test.gor.gz} | top 10)";
        Assert.assertEquals(10, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testProcessSourceCatGor() throws IOException {
        // No cat on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String gorcmd = "gor <(cmd {cat " + gorFile.getCanonicalPath() + "}) | top 5";
        Assert.assertEquals(5, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testProcessSourceCatNor() throws IOException {
        // No cat on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String gorcmd = "nor <(cmd {cat " + gorFile.getCanonicalPath() + "}) | top 5";
        Assert.assertEquals(5, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need tabix installed and in default path on test machine")
    @Test
    public void testProcessSourceTabixGorSeek() {
        String gorcmd = "gor -p chr3:1- <(cmd {tabix -h #(H:-H) ../tests/data/gor/dbsnp_test.gor.gz #(S:chr:pos)})";
        Assert.assertEquals(2, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need tabix installed and in default path on test machine")
    @Test
    public void testProcessSourceTabixVcf() {
        String gorcmd = "gor <(cmd {-svcf tabix #(H:-H) -h ../tests/data/external/samtools/testTabixIndex.vcf.gz #(S:chn:pos)})";
        Assert.assertEquals(2, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need tabix installed and in default path on test machine")
    @Test
    public void testProcessSourceTabixVcfSeek() {
        String gorcmd = "gor -pchr3:1- <(cmd {-svcf tabix -h #(H:-H) ../tests/data/external/samtools/testTabixIndex.vcf.gz #(S:chr:pos)})";
        Assert.assertEquals("Expected line count not correct", 2, TestUtils.runGorPipeCount(gorcmd));
    }

    @Ignore("need GATK installed and in default path on test machine")
    @Test
    public void testProcessSourceGATK() {
        String gorcmd = "gor -b 10 -p chr1:1-200000000 <(cmd {-svcf java -jar GenomeAnalysisTK.jar --analysis_type HaplotypeCaller --input_file ../tests/data/case.bam --reference_sequence Homo_sapiens_assembly19.fasta #(H:--intervals 1:1-1) #(S:--intervals chr:pos-end)}) | top 10";
        Assert.assertEquals("Expected line count not correct", 10, TestUtils.runGorPipeCount(gorcmd));
    }
}
