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

package gorsat;

import org.junit.Assert;
import org.junit.Test;

public class UTestGtTranspose {
    String prologue = "def #pns# = 11;\n" +
            "def #p# = 0.3;\n" +
            "def #q# = 0.2;\n" +
            "def #D# = 0.1; /* Should ideally be smaller than all of: p*q, (1-p)*(1-q), (1-p)*q, p*(1-q) */\n" +
            "\n" +
            "create #buckets# = norrows #pns# | calc pn 'pn'+right('00000'+rownum,5) | select pn | calc bucket 'b1';\n" +
            "create #gt# = gor <(norrows #pns#  | calc p #p# | calc q #q# | calc D #D#\n" +
            "| calc h1 if(random()<p,if(random()<(p*q+D)/p,'0_0','0_1'),if(random()<((1-p)*q-D)/(1-p),'1_0','1_1'))\n" +
            "| calc h2 if(random()<p,if(random()<(p*q+D)/p,'0_0','0_1'),if(random()<((1-p)*q-D)/(1-p),'1_0','1_1'))\n" +
            "| colsplit h1 2 snp_f -s '_'\n" +
            "| colsplit h2 2 snp_m -s '_'\n" +
            "| calc gt1 decode(snp_f_1+'_'+snp_m_1,'0_0,0,0_1,1,1_0,1,1_1,2')\n" +
            "| calc gt2 decode(snp_f_2+'_'+snp_m_2,'0_0,0,0_1,1,1_0,1,1_1,2')\n" +
            "| group -lis -sc gt1,gt2 -s '' -len 1000000\n" +
            "| calc values lis_gt1+','+lis_gt2\n" +
            "| hide lis_*\n" +
            "| split values\n" +
            "| rownum\n" +
            "| rename rownum pos\n" +
            "| calc chrom 'chr1'\n" +
            "| calc ref 'A'\n" +
            "| calc alt 'T'\n" +
            "| calc bucket 'b1'\n" +
            ")\n" +
            "| multimap -cartesian <(norrows 10) | replace pos pos+rownum*10\n" +
            "| select chromx,pos,ref,alt,bucket,values\n" +
            "| rename chromx chrom\n" +
            "| sort genome;\n" +
            "\n" +
            "create #markers_nor# = nor <(gor [#gt#] | select 1-alt | distinct | top 4);\n" +
            "create #markers_gor# = gor [#gt#] | select 1-alt | distinct | top 4;\n" +
            "\n";

    @Test
    public void columnNamesUsableInCalc() {
        String query = "gor [#gt#] | skip 1 | gttranspose [#buckets#] <(nor [#buckets#] | top 10 | select pn) [#markers_nor#] -vs 1 -cols | calc x chr1_12_A_T | replace 4- 'x'+#rc\n";
        String[] result = TestUtils.runGorPipeLines(prologue + query);
        String header = result[0];
        String expected = "CHROM\tPOS\tPN\tchr1_1_A_T\tchr1_2_A_T\tchr1_11_A_T\tchr1_12_A_T\tx\n";
        Assert.assertEquals(expected, header);
    }

    @Test
    public void markersFromGorzFile() {
        // The query uses random so the best we can do is ensure the query runs without errors.
        String query = "gor [#gt#] | skip 1 | gttranspose [#buckets#] <(nor [#buckets#] | top 10 | select pn) [#markers_gor#] -vs 1 -cols";
        TestUtils.runGorPipe(prologue + query);
    }

    @Test
    public void markersFromNorFile() {
        // The query uses random so the best we can do is ensure the query runs without errors.
        String query = "gor [#gt#] | skip 1 | gttranspose [#buckets#] <(nor [#buckets#] | top 10 | select pn) [#markers_nor#] -vs 1 -cols";
        TestUtils.runGorPipe(prologue + query);
    }

    @Test
    public void markersFromNestedNorQuery() {
        // The query uses random so the best we can do is ensure the query runs without errors.
        String query = "gor [#gt#] | skip 1 | gttranspose [#buckets#] <(nor [#buckets#] | top 10 | select pn) <(nor [#markers_gor#]) -vs 1 -cols";
        TestUtils.runGorPipe(prologue + query);
    }

    @Test
    public void markerInInputStream() {
        // The query uses random so the best we can do is ensure the query runs without errors.
        String query = "gor [#gt#] | skip 1 | gttranspose [#buckets#] <(nor [#buckets#] | top 10 | select pn) <(nor [#markers_nor#]) -vs 1 | calc x len(values) | replace values 'x'+values+'x'";
        TestUtils.runGorPipe(prologue + query);
    }
}
