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

import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureCodecHeader;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.tribble.readers.AsciiLineReader;
import htsjdk.tribble.readers.AsciiLineReaderIterator;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.tribble.util.LittleEndianOutputStream;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import org.gorpipe.util.collection.extract.Extract;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sigmar on 26/10/15.
 */
public class UTestGorTabix {

    private void doTest(String gor, String file, int expected) {
        String query = gor + " " + file + " | top 10";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testGorTabix() {
        doTest("gor", "../tests/data/gor/dbsnp_test.gor.gz", 10);
    }

    @Test
    public void testGorTabixSeek() {
        doTest("gor -p chr2:1-", "../tests/data/gor/dbsnp_test.gor.gz", 2);
    }

    @Test
    public void testVcfTabixSeek() {
        doTest("gor -p chr2:1-", "../tests/data/gor/dbsnp_test.gor.gz", 2);
    }

    @Test
    public void testOutput() {
        String query = "gor ../tests/data/gor/dbsnp_test.gor.gz  | top 10";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("C5F078A2E93799FC4B294EAEE835B035", Extract.md5(res));
    }

    @Test
    public void testCompareOutputs(){
        String resGor = TestUtils.runGorPipeNoHeader("gor  ../tests/data/gor/dbsnp_test.gor | top 10");
        String resGorGz = TestUtils.runGorPipeNoHeader("gor ../tests/data/gor/dbsnp_test.gor.gz | top 10");
        Assert.assertEquals(Extract.md5(resGor),Extract.md5(resGorGz));
    }

    @Test
    public void testIndelInVcf() throws IOException {
        Path p = Paths.get("indel.vcf.gz");
        Path pi = Paths.get("indel.vcf.gz.tbi");
        try {
            BlockCompressedOutputStream bos = new BlockCompressedOutputStream(p.toFile());
            bos.write("##fileformat=VCFv4.2\n".getBytes());
            bos.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tPN\n".getBytes());
            bos.write("chr1\t9\t.\tAA\tC\t0.0\t.\t.\t.\tpn\n".getBytes());
            bos.write("chr1\t10\t.\tA\tC\t0.0\t.\t.\t.\tpn\n".getBytes());
            bos.close();
            TabixIndexCreator tbi = new TabixIndexCreator(TabixFormat.VCF);
            BlockCompressedInputStream inputStream = new BlockCompressedInputStream(Files.newInputStream(p));
            LittleEndianOutputStream outputStream = new LittleEndianOutputStream(new BlockCompressedOutputStream(pi.toFile()));

            VCFCodec codec = new VCFCodec();
            AsciiLineReader lineReader = new AsciiLineReader(inputStream);
            AsciiLineReaderIterator iterator = new AsciiLineReaderIterator(lineReader);
            codec.readActualHeader(iterator);
            while (iterator.hasNext()) {
                final long position = iterator.getPosition();
                VariantContext currentContext = codec.decode(iterator.next());
                tbi.addFeature(currentContext, position);
            }
            iterator.close();
            Index index = tbi.finalizeIndex(iterator.getPosition());
            index.write(outputStream);
            outputStream.close();

            String res = TestUtils.runGorPipe("gor -p chr1:10-20 indel.vcf.gz");
            Assert.assertEquals("Wrong result from vcf tabix","CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tPN\n" +
                    "chr1\t10\t.\tA\tC\t0.0\t.\t.\t.\tpn\n",res);
        } finally {
            if(Files.exists(p)) Files.delete(p);
            if(Files.exists(pi)) Files.delete(pi);
        }
    }
}
