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

package org.gorpipe.model.genome.files.gor;

import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.Row;
import org.junit.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class UTestChrPosBinIterator {

    private final String VCF_FILE = "../tests/data/external/samtools/dbsnp_135.b37.1000.vcf";
    private final int VCF_FILE_LINECOUNT = 99;

    private GenomicIterator getGenomicIterator(String filename) {
        Path path = Paths.get(filename);
        String absolutePath = path.toAbsolutePath().toString();
        SourceReference sourceReference = new SourceReference(absolutePath);

        GenomicIterator iterator = null;
        try {
            iterator = GorDriverFactory.fromConfig().createIterator(sourceReference);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertNotNull(iterator);

        iterator.init(null);

        // This is done in LineSource - next doesn't work correctly without this
        iterator.setColnum(iterator.getHeader().split("\t").length - 2);

        return iterator;
    }

    @Test
    public void getHeader_WhenFileIsVcf() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);
        String[] header = iterator.getHeader().split("\t");
        assertEquals(8, header.length);
        assertEquals("CHROM", header[0]);
        assertEquals("POS", header[1]);
        assertEquals("ID", header[2]);
        assertEquals("REF", header[3]);
        assertEquals("ALT", header[4]);
        assertEquals("QUAL", header[5]);
        assertEquals("FILTER", header[6]);
        assertEquals("INFO", header[7]);
    }

    @Test
    public void next_FillingLine_WhenFileIsVcf_GetFirstLineOnly() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);
        assertTrue(iterator.hasNext());
        assertEquals("chr1\t10144\trs144773400\tTA\tT\t.\tPASS\tASP;RSPOS=10145;SAO=0;SSR=0;VC=DIV;VP=050000000004000000000200;WGT=0;dbSNPBuildID=134", iterator.next().getAllCols().toString());
    }

    @Test
    public void next_ReturningRow_WhenFileIsVcf_GetFirstLineOnly() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);

        // next won't work properly if hasNext isn't called
        assertTrue(iterator.hasNext());

        Row r = iterator.next();
        assertNotNull(r);
        assertEquals("chr1\t10144\trs144773400\tTA\tT\t.\tPASS\tASP;RSPOS=10145;SAO=0;SSR=0;VC=DIV;VP=050000000004000000000200;WGT=0;dbSNPBuildID=134", r.getAllCols().toString());
    }

    @Test
    public void hasNext_WhenFileIsVcf_ShouldReturnTrueAtStart() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);
        assertTrue(iterator.hasNext());
    }

    @Test
    public void hasNext_WhenFileIsVcf_ShouldReturnTrueAtStartWhenCalledRepeatedly() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);

        // Make sure we call this more times than there are lines in the file to ensure
        // that hasNext isn't advancing the file
        for (int i = 0; i < VCF_FILE_LINECOUNT + 5; i++) {
            assertTrue(iterator.hasNext());
        }
    }

    @Test
    public void hasNext_WhenFileIsVcf_ShouldReturnFalseWhenFileIsExhausted() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);
        int count = 0;
        while(iterator.hasNext()) {
            count++;
            if(count > VCF_FILE_LINECOUNT) {
                break;
            }
            iterator.next();
        }
        assertFalse(iterator.hasNext());
        assertEquals(VCF_FILE_LINECOUNT, count);
    }

    @Test
    public void seek_WhenFileIsVcf_SeekToMiddleReturnsTrueWhenPositionExists() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);
        boolean result = iterator.seek("chr1", 10234);
        assertTrue(result);
    }

    @Test
    public void seek_WhenFileIsVcf_NextFillingLineWorksAfterSeekToMiddle() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);
        iterator.seek("chr1", 54676);

        assertTrue(iterator.hasNext());
        String expected = "chr1\t54676\trs2462492\tC\tT\t.\tPASS\tASP;GMAF=0.191956124314442;GNO;HD;KGPilot123;RSPOS=54676;SAO=0;SSR=0;VC=SNV;VLD;VP=050000000004040510000100;WGT=0;dbSNPBuildID=100";
        assertEquals(expected, iterator.next().getAllCols().toString());
    }

    @Test
    public void seek_WhenFileIsVcf_NextReturningRowWorksAfterSeekToMiddle() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);
        iterator.seek("chr1", 10492);

        // next won't work properly if hasNext isn't called
        assertTrue(iterator.hasNext());

        Row r = iterator.next();
        assertNotNull(r);
        String expected = "chr1\t10492\trs55998931\tC\tT\t.\tPASS\tASP;GENEINFO=LOC100652771:100652771;GMAF=0.0617001828153565;RSPOS=10492;SAO=0;SSR=0;VC=SNV;VLD;VP=050000000004040000000100;WGT=0;dbSNPBuildID=129";
        assertEquals(expected, r.getAllCols().toString());
    }

    @Test
    public void seek_WhenFileIsVcf_NextFillingLineWorksAfterSeekToLastLine() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);
        iterator.seek("chr1", 10904);

        assertTrue(iterator.hasNext());
        String expected = "chr1\t10904\trs10218493\tG\tA\t.\tPASS\tASP;GENEINFO=LOC100652771:100652771;GNO;RSPOS=10904;SAO=0;SSR=0;VC=SNV;VP=050000000004000100000100;WGT=0;dbSNPBuildID=119";
        assertEquals(expected, iterator.next().getAllCols().toString());
    }

    @Test
    public void seek_WhenFileIsVcf_NextReturningRowWorksAfterSeekToLine() {
        GenomicIterator iterator = getGenomicIterator(VCF_FILE);
        iterator.seek("chr1", 14889);

        // next won't work properly if hasNext isn't called
        assertTrue(iterator.hasNext());

        Row r = iterator.next();
        assertNotNull(r);
        String expected = "chr1\t14889\trs142444908\tG\tA\t.\tPASS\tASP;RSPOS=14889;SAO=0;SSR=0;VC=SNV;VP=050000000004000000000100;WGT=0;dbSNPBuildID=134";
        assertEquals(expected, r.getAllCols().toString());
    }
}
