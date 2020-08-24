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

import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.model.gor.iterators.RowSource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Created by sigmar on 26/10/15.
 */
public class UTestGorVcfWithHtsjdk {
    private String doTest(String gor, String file, int top, int expected) {
        String curdir = new File(".").getAbsolutePath();

        String query = gor + " " + curdir.substring(0, curdir.length() - 1) + file + " | top " + top;

        StringBuilder ret = new StringBuilder();
        RowSource rs = TestUtils.runGorPipeIterator(query);
        int count = 0;
        while (rs.hasNext()) {
            Row row = rs.next();
            ret.append(row).append("\n");
            count++;
        }
        rs.close();

        Assert.assertEquals(expected, count);

        return ret.toString();
    }

    @AfterClass
    public static void cleanup() {
        System.gc();
    }

    @Test
    public void testTabixChrVcf() {
        doTest("gor -p chr20:1-", "../tests/data/external/samtools/test.vcf", 10, 5);
    }

    @Test
    public void testTabixChrVcfNoSeek() {
        doTest("gor", "../tests/data/external/samtools/test.vcf.gz", 10, 5);
    }

    @Test
    public void testChrVcf() {
        doTest("gor -p chr20", "../tests/data/external/samtools/test.vcf", 10, 5);
    }

    @Test
    public void testEmptyTabix() {
        doTest("gor", "../tests/data/external/samtools/emptytest.vcf", 10, 0);
    }

    @Test
    public void testTabixVcfNoSeek() {
        doTest("gor", "../tests/data/external/samtools/testTabixIndex.vcf.gz", 10, 10);
    }

    @Test
    public void testTabixVcf() {
        doTest("gor -p chr4:1-", "../tests/data/external/samtools/testTabixIndex.vcf", 10, 2);
    }

    @Test
    public void testTabixVcfChrX() {
        doTest("gor -p chrX:1-", "../tests/data/external/samtools/testTabixIndex.vcf", 10, 2);
    }

    @Test
    public void testVcfWithoutIndex() {
        doTest("gor", "../tests/data/external/samtools/VcfThatLacksAnIndex.vcf.gz", 10, 5);
    }

    @Test
    public void testUncompressedVcf() {
        doTest("gor", "../tests/data/external/samtools/test.vcf", 10, 5);
    }

    @Test
    public void testUncompressedVcfSeek() {
        doTest("gor -p chr20", "../tests/data/external/samtools/test.vcf", 10, 5);
    }

    @Test
    public void testCustomContigVcfSeek() {
        doTest("gor -p chrM", "../tests/data/external/samtools/testTabixIndexCustomContig.vcf", 10, 1);
    }

    @Test
    public void testCustomContigVcf() {
        String res = doTest("gor", "../tests/data/external/samtools/testTabixIndexCustomContig.vcf", 200, 27);
        String gzres = doTest("gor", "../tests/data/external/samtools/testTabixIndexCustomContig.vcf.gz | sort genome", 200, 27);

        Assert.assertEquals(res, gzres);
    }

    @Test
    public void testCompareOutputs() {
        String tabix = doTest("gor", "../tests/data/external/samtools/testTabixIndex.vcf", 10, 10);
        String orig = doTest("gor", "../tests/data/external/samtools/testTabixIndexUnsorted.vcf | sort genome", 10, 10);

        Assert.assertEquals(orig, tabix);
    }

    @Test
    public void testCompareVcfOutput() throws IOException {
        String path = "../tests/data/external/samtools/testTabixIndex.vcf.gz";
        String tabix = doTest("gor", path, 2, 2);
        Stream<String> lines = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(path))))).lines();
        String orig = lines.filter(line -> !line.startsWith("#")).limit(2).map(line -> "chr" + line).collect(Collectors.joining("\n", "", "\n"));
        lines.close();

        Assert.assertEquals(orig, tabix);
    }

    @Test
    public void testNoHeaderVcf() {
        doTest("gor", "../tests/data/external/samtools/noheader.vcf", 100, 5);
    }

    @Test
    public void testGVcf() {
        doTest("gor", "../tests/data/external/gvcf/test.gvcf", 100, 31);
    }

    @Test
    public void testGVcfContent() {
        String[] lines = TestUtils.runGorPipeLines("../tests/data/external/gvcf/test.gvcf");
        Assert.assertEquals(32, lines.length);

        String[] header = lines[0].split("\t");
        Assert.assertEquals(10, header.length);

        Assert.assertTrue(lines[1].contains("20\t10000000\t.\tT\t<NON_REF>"));
        Assert.assertTrue(lines[2].contains("20\t10000117\t.\tC\tT,<NON_REF>"));

    }

    @Test
    public void testGVcfgz() {
        doTest("gor", "../tests/data/external/gvcf/test.gvcf", 100, 31);
    }

    @Test
    public void testGVcfgzContent() {
        String[] lines = TestUtils.runGorPipeLines("../tests/data/external/gvcf/test.gvcf");
        Assert.assertEquals(32, lines.length);

        String[] header = lines[0].split("\t");
        Assert.assertEquals(10, header.length);

        Assert.assertTrue(lines[1].contains("20\t10000000\t.\tT\t<NON_REF>"));
        Assert.assertTrue(lines[2].contains("20\t10000117\t.\tC\tT,<NON_REF>"));

    }

}
