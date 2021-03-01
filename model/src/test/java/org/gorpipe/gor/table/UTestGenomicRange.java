/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.gor.table;

import org.junit.Assert;
import org.junit.Test;


/**
 * Unit tests for gor table.
 * <p>
 * Created by gisli on 03/01/16.
 */
public class UTestGenomicRange {

    @Test
    public void testGenomicRangeFormatting() {

        // Test toString

        GenomicRange range = new GenomicRange("", -1, "", -1);
        Assert.assertEquals("Empty", "", range.toString());

        range = new GenomicRange("chr10", 100, "chr11", 200);
        Assert.assertEquals("Fully specified, different chromosomes", "chr10:100-chr11:200", range.toString());

        range = new GenomicRange("chr3",100, "chr3",200);
        Assert.assertEquals("Fully specified, same chromosome", "chr3:100-200", range.toString());range = new GenomicRange("chr10",100,"chr10",100);
        Assert.assertEquals("From location specific gene", "chr10:100-100", range.toString());

        range = new GenomicRange("chr10", 100, "", -1);
        Assert.assertEquals("From location", "chr10:100", range.toString());

        range = new GenomicRange(null,-1,"chr10",200);
        Assert.assertEquals("To location", "-chr10:200", range.toString());

        range = new GenomicRange("chr10", 100, null, 200);
        Assert.assertEquals("Skipping to chr", "chr10:100-200", range.toString());

        // test extremes
        range = new GenomicRange("chrM",0,"chrY",1000000);
        Assert.assertEquals("Full range, different chromosomes", "chrM:0-chrY:1000000", range.toString());

        range = new GenomicRange("chrM",0,"",-1);
        Assert.assertEquals("From location: from very beginning", "chrM:0", range.toString());

        range = new GenomicRange("chrY",1000000,null,-1);
        Assert.assertEquals("From location: from the very end", "chrY:1000000", range.toString());

        // test out of bounds, I assume that gor handles these as invalid chromosones
        range = new GenomicRange(null,0,"",-1);
        Assert.assertEquals("Invalid start range no end defined", ":0", range.toString());

        range = new GenomicRange(null,-1,"",100000);
        Assert.assertEquals("Invalid end range no beginning specified", ":-100000", range.toString());

        range = new GenomicRange("",0,"",100000);
        Assert.assertEquals("Invalid start and end range", ":0-100000", range.toString());


        // Test format

        range = new GenomicRange("", -1, "", -1);
        Assert.assertEquals("Format: Empty", "\t\t\t", range.format());

        range = new GenomicRange("chr10", 100, "chr11", 200);
        Assert.assertEquals("Format: Fully specified, different chromosomes", "chr10\t100\tchr11\t200", range.format());

        range = new GenomicRange("chr10", 100, "chr10", 200);
        Assert.assertEquals("Format: Fully specified, same chromosome", "chr10\t100\tchr10\t200", range.format());

        range = new GenomicRange("chr10", 100, "", -1);
        Assert.assertEquals("Format: From location", "chr10\t100\t\t", range.format());

        range = new GenomicRange("", -1, "chr11", 200);
        Assert.assertEquals("Format: To location", "\t\tchr11\t200", range.format());

        range = new GenomicRange("chr10", 100, "", 200);
        Assert.assertEquals("Format: Skipping to chr", "chr10\t100\t\t200", range.format());

        // test extremes
        range = new GenomicRange("chrM",0,"chrY",10000000);
        Assert.assertEquals("Format: Full range", "chrM\t0\tchrY\t10000000", range.format());

        range = new GenomicRange("chrM",0,"",-1);
        Assert.assertEquals("Format: From beginning no end specified", "chrM\t0\t\t", range.format());

        range = new GenomicRange("",-1,"chrY",10000000);
        Assert.assertEquals("Format: To end no beginning specified", "\t\tchrY\t10000000", range.format());

        // out of bounds
        range = new GenomicRange("",0,"",-1);
        Assert.assertEquals("Format: Invalid beginning no end specified", "\t0\t\t", range.format());

        range = new GenomicRange("",-1,"",100000000);
        Assert.assertEquals("Format: Skipping to chr", "\t\t\t100000000", range.format());

        range = new GenomicRange("",0,"",10000000);
        Assert.assertEquals("Format: Skipping to chr", "\t0\t\t10000000", range.format());
    }

    @Test
    public void testGenomicRangeParsing() {

        // Parse : - separated.
        Assert.assertEquals("Fully specified, different chromosomes", "chr10:100-chr11:200",
                GenomicRange.parseGenomicRange("chr10:100-chr11:200").toString());

        Assert.assertEquals("Fully specified, same chromosome", "chr10:100-200",
                GenomicRange.parseGenomicRange("chr10:100-chr10:200").toString());

        Assert.assertEquals("No pos", "chr10-chr11",
                GenomicRange.parseGenomicRange("chr10-chr11").toString());

        Assert.assertEquals("From location, without hyphen", "chr10:100-",
                GenomicRange.parseGenomicRange("chr10:100").toString());

        Assert.assertEquals("From location, with hyphen", "chr10:100-",
                GenomicRange.parseGenomicRange("chr10:100-").toString());

        Assert.assertEquals("Skipping to chr", "chr10:100-200",
                GenomicRange.parseGenomicRange("chr10:100-200").toString());

        Assert.assertEquals("New chromosome with range", "chr100:100-200",
                GenomicRange.parseGenomicRange("chr100:100-200").toString());

        Assert.assertEquals("No start location with defined stop location", "-chr11:200",
                GenomicRange.parseGenomicRange("-chr11:200").toString());

        Assert.assertEquals("Empty, throws exception", "",
                GenomicRange.parseGenomicRange("").toString());


        // Parse tab separeated.
        Assert.assertEquals("Fully specified, different chromosomes", "chr10:100-chr11:200",
                GenomicRange.parseGenomicRange("chr10\t100\tchr11\t200").toString());

        Assert.assertEquals("Fully specified, same chromosome", "chr10:100-200",
                GenomicRange.parseGenomicRange("chr10\t100\tchr10\t200").toString());

        Assert.assertEquals("From location", "chr10:100-",
                GenomicRange.parseGenomicRange("chr10\t100\t\t").toString());

        Assert.assertEquals("Skipping to chr", "chr10:100-200",
                GenomicRange.parseGenomicRange("chr10\t100\t\t200").toString());

        Assert.assertEquals("To location with no start chromosome", "-chr11:200",
                GenomicRange.parseGenomicRange("\t\tchr11\t200").toString());
    }


    @Test (expected = RuntimeException.class)
    public void testInvalidInputGenomicRangeParsing() {
        // This should throw exception as the input is just not valid
        Assert.assertEquals("Invalid start input, throws exception", "",
                GenomicRange.parseGenomicRange("asdasd:dfs sdlf sfsd f").toString());
    }

    @Test (expected = RuntimeException.class)
    public void testInvalidNumberOfTabsGenomicRangeParsing() {
        Assert.assertEquals("Format: Invalid number of tabs", "",
                GenomicRange.parseGenomicRange("chr1\t10\tchr2").toString());
    }
    
}
