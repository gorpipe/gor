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

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class UTestGtGenAndLd {

    String testSetupGORQL =
            "create #dummy# = nor <(norrows 1000 | sort -c rownum:n);" +
                    "create #buckets# = nor [#dummy#] | rename #1 PN | calc bucket 'b_'+str(div(PN,50)+1);" +
                    "create #loci# = gorrow chr1,1,2 | multimap -cartesian -h <(norrows 20000) | calc Npos #2+RowNum | select 1,Npos | sort genome | rename #2 Pos | calc ref 'G' |  calc alt 'C';" +
                    "create #gt# = gor [#loci#] | multimap -cartesian -h [#buckets#] | distloc 20000 | hide bucket | calc gt mod(PN,4) | where random()<0.05;" +
                    "create #cov# = gorrow chr1,0,3000 | multimap -cartesian -h [#buckets#] | where not(bucket='b_2');";

    String basicGtGen = "gor [#gt#] | replace gt 0 | gtgen -gc ref,alt [#buckets#] [#cov#] | top 1000";
    String basicGtLd = " | replace values fsvmap(values, 1, 'if(sin(pos)>0.5, \"2\", \"0\")', '') | gtld -sum -f 100";

    
    @Test
    public void testGtGen() {
        String query = basicGtGen +
                " | csvsel -gc ref,alt -u 3 -vs 1 [#buckets#] <(nor [#buckets#] | select #1) " +
                " | gtld -sum -f 100 -calc" +
                " | merge <(" + basicGtGen + " | gtld -sum -f 100 -calc) " +
                " | group 1 -gc 3- -count| throwif allcount != 2 | where 2=3";

        TestUtils.runGorPipe(testSetupGORQL + query );
    }

    @Test
    public void testGtGen2() {
        String query = "create yyy = " + basicGtGen + basicGtLd + ";" +
                "gor [yyy] | gtld -calc" +
                " | merge <(" + basicGtGen + basicGtLd + " -calc) " +
                " | group 1 -gc 3- -count | throwif allcount != 2 | where 2=3";

        TestUtils.runGorPipe(testSetupGORQL + query);
    }

    @Test
    public void test_unknownPnInRightSource() throws IOException {
        final File tmpDir = Files.createTempDir();
        final File leftSource = new File(tmpDir, "leftFile.gor");
        final BufferedWriter leftSourceWriter = new BufferedWriter(new FileWriter(leftSource));
        leftSourceWriter.write("CHROM\tPOS\tPN\tGT\nchr1\t1\tPN1\t2\n");
        leftSourceWriter.close();
        final File rightSource = new File(tmpDir, "rightFile.gor");
        final BufferedWriter rightSourceWriter = new BufferedWriter(new FileWriter(rightSource));
        rightSourceWriter.write("CHROM\tPOS\tEND\tPN\nchr1\t1\t1\tPN2\n");
        rightSourceWriter.close();
        final File tbSource = new File(tmpDir, "tb.tsv");
        final BufferedWriter tbWriter = new BufferedWriter(new FileWriter(tbSource));
        tbWriter.write("PN1\tBUCKET1\n");
        tbWriter.close();
        final String query = "gor " + leftSource.getAbsolutePath() + " | gtgen " + tbSource.getAbsolutePath() + " " + rightSource.getAbsolutePath();
        final String results = TestUtils.runGorPipe(query);
        final String wanted = "CHROM\tPOS\tBucket\tValues\n" +
                "chr1\t1\tBUCKET1\t2\n";
        Assert.assertEquals(wanted, results);
        FileUtils.deleteDirectory(tmpDir);
    }

    @Test
    public void GTLDWithBuckets() {
        String common = "def #pns# = 100000;\n" +
                "def #p# = 0.3;\n" +
                "def #q# = 0.02;\n" +
                "def #D# = 0.1; /* Should ideally be smaller than all of: p*q, (1-p)*(1-q), (1-p)*q, p*(1-q) */\n" +
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
                ")\n" +
                "| select chrom,pos,ref,alt,values\n" +
                ";\n";

        String query1 = "nor <(gor [#gt#] | bucketsplit values 5000 -vs 1 | gtld -sum -f 100)\n";
        String query2 = "nor <(gor [#gt#] | gtld -sum -f 100)\n";

        String result1 = TestUtils.runGorPipe(common + query1);
        String result2 = TestUtils.runGorPipe(common + query2);

        Assert.assertEquals(result1, result2);
    }
}
