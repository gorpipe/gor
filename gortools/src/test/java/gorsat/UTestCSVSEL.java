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

import org.gorpipe.test.GorDictionarySetup;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sigmar on 19/12/2016.
 */
public class UTestCSVSEL {
    String[] BASIC_TAGBUCKET = {
            "#PN\tbucket",
            "1\tbucket1",
            "2\tbucket1",
            "3\tbucket1",
            "4\tbucket1",
            "5\tbucket2",
            "6\tbucket2",
            "7\tbucket2",
            "8\tbucket2",
    };

    String[] BASIC_TAGS = {
            "#PN",
            "3",
            "6"
    };

    @Test
    public void basic() {
        String[] contents = {
                "Chrom\tPos\tRef\tAlt\tBucket\tValues",
                "chr1\t1\tG\tA\tbucket1\ta,b,c,d",
                "chr1\t1\tG\tA\tbucket2\te,f,g,h"
        };

        String expected = "Chrom\tPos\tvalues\n" +
                "chr1\t1\tc,f\n";

        final String queryFormat = "gor %s | csvsel -s ',' %s %s";

        assertQuery(queryFormat, BASIC_TAGBUCKET, BASIC_TAGS, contents, expected);
    }

    @Test
    public void spaceSeparator() {
        String[] contents = {
                "Chrom\tPos\tRef\tAlt\tBucket\tValues",
                "chr1\t1\tG\tA\tbucket1\ta b c d",
                "chr1\t1\tG\tA\tbucket2\te f g h"
        };

        String expected = "Chrom\tPos\tvalues\n" +
                "chr1\t1\tc f\n";

        final String queryFormat = "gor %s | csvsel -s ' ' %s %s";

        assertQuery(queryFormat, BASIC_TAGBUCKET, BASIC_TAGS, contents, expected);
    }

    @Test
    public void fixedSizeValueSizeOne() {
        String[] contents = {
                "Chrom\tPos\tRef\tAlt\tBucket\tValues",
                "chr1\t1\tG\tA\tbucket1\tabcd",
                "chr1\t1\tG\tA\tbucket2\tefgh"
        };

        String expected = "Chrom\tPos\tvalues\n" +
                "chr1\t1\tcf\n";

        final String queryFormat = "gor %s | csvsel -vs 1 %s %s";

        assertQuery(queryFormat, BASIC_TAGBUCKET, BASIC_TAGS, contents, expected);
    }

    @Test
    public void fixedSizeValueSizeThree() {
        String[] contents = {
                "Chrom\tPos\tRef\tAlt\tBucket\tValues",
                "chr1\t1\tG\tA\tbucket1\taaabbbcccddd",
                "chr1\t1\tG\tA\tbucket2\teeefffggghhh"
        };

        String expected = "Chrom\tPos\tvalues\n" +
                "chr1\t1\tcccfff\n";

        final String queryFormat = "gor %s | csvsel -vs 3 %s %s";

        assertQuery(queryFormat, BASIC_TAGBUCKET, BASIC_TAGS, contents, expected);
    }

    private void assertQuery(String queryFormat, String[] tagbucket, String[] tags, String[] contents, String expected) {
        final File tagBucketFile = TestUtils.createTsvFile("UTestCsvSel", tagbucket);
        final File tagsFile = TestUtils.createTsvFile("UTestCsvSel", tags);
        final File dataFile = TestUtils.createGorFile("UTestCsvSel", contents);
        final String query = String.format(queryFormat, dataFile.getAbsolutePath(), tagBucketFile.getAbsolutePath(), tagsFile.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testCvssel() {
        String[] args = new String[]{"create #dummy# = gorrow chr1,1,2 | signature -timeres 1 | calc x '0,1,2,3,4,5,6,7,8,9' | calc y x | calc z x | split x | split y | split z | calc rownum int(x)+10*int(y)+100*int(z) | sort 1 -c rownum:n;\n" +
                "\n" +
                "    create #pnbuck# = nor [#dummy#] | select rownum | calc bucket 'bucket'+str(1+div(rownum,100)) | rename rownum PN | select PN,bucket | top 352 | sort -c bucket,pn;\n" +
                "\n" +
                "    gorrow chr1,1,2 | calc x '1,2,3' | split x | select 1,x | calc ref 'G' | calc alt 'A'  | select 1-4 | distinct | top 100 | multimap -cartesian -h [#pnbuck#] | sort 1 -c #3,#4,bucket,PN\n" +
                "    | group 1 -gc #3,#4,bucket -lis -sc pn | rename lis_PN values\n" +
                "    | csvsel -s ',' -gc #3,#4 -tag pn [#pnbuck#] <( nor [#pnbuck#]  | select #1 | where random()<0.5 )\n" +
                "    | where pn != value\n"};

        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals(0, count);
    }

    @Test
    public void testCsvselTruncatedLines() {
        String[] args = new String[]{"create #dummy# = gorrow chr1,1,2 | signature -timeres 1 | calc x '0,1,2,3,4,5,6,7,8,9' | calc y x | calc z x | split x | split y | split z | calc rownum int(x)+10*int(y)+100*int(z) | sort 1 -c rownum:n | signature -timeres 1;" +
                "create #pnbuck# = nor [#dummy#] | select rownum | calc bucket 'bucket'+str(1+div(rownum,10)) | rename rownum PN | select PN,bucket | sort -c bucket,pn;" +
                "gorrow chr1,1,2 | calc x '0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31' | split x" +
                " | calc y '0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31' | split y | calc z '0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31' | split z" +
                " | calc pos #2+32*32*int(x)+32*int(y)+int(z) | select 1,pos | calc ref 'G' | calc alt 'A'  | select 1-4 | calc bucket 'bucket1'" +
                " | calc values '                                ' | csvsel -gc #3,#4 [#pnbuck#] <( nor [#pnbuck#] | top 16 | select #1) -u '  ' -vs 2" +
                " | replace values fsvmap(values,2,'chars2dose(x)',',') | top 1"};

        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals(1, count);
    }

    @Test
    public void testCvsselHide() {
        String query = "create #dummy# = gorrow chr1,1,2 | calc x '0,1,2,3,4,5,6,7,8,9' | split x | calc rownum x | sort 1 -c rownum:n | signature -timeres 1;\n" +
                "    create #pnbuck# = nor [#dummy#] | select rownum | calc bucket 'bucket'+str(1+div(rownum,100)) | rename rownum PN | select PN,bucket | top 352 | sort -c bucket,pn;\n" +
                "    gorrow chr1,1,2 | calc x '1,2,3' | split x | select 1,x | calc ref 'G' | calc alt 'A'  | select 1-4 | distinct | top 100 | multimap -cartesian -h [#pnbuck#] | sort 1 -c #3,#4,bucket,PN\n" +
                "    | group 1 -gc #3,#4,bucket -lis -sc pn | rename lis_PN values" +
                "    | csvsel -s ',' -gc #3,#4 -tag pn -hide '0','1','2','3' [#pnbuck#] <(nor [#pnbuck#]  | select #1)" +
                "    | hide 3-5";

        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        int count = 0;

        for (String line : lines) {
            final String value = line.split("\t")[2];
            if (value.equals("0") || value.equals("1") || value.equals("2") || value.equals("3")) count++;
        }

        Assert.assertEquals(0, count);
    }

    @Test
    public void testCvsselVcfGTGPOut() throws IOException {
        String gorcontent = "CHROM\tPOS\tID\tREF\tALT\tbucket\tvalues\n"+
                "chr1\t1\trs1\tA\tC\t1\t~~~~\n";
        String bucketcontent = "#PN\tbucket\n"+
                "A\t1\n"+
                "B\t1\n";

        Path pg = Paths.get("cont.gor");
        Path bp = Paths.get("buckets.tsv");
        try {
            Files.write(pg, gorcontent.getBytes());
            Files.write(bp, bucketcontent.getBytes());
            String query = "gor cont.gor | CSVSEL buckets.tsv <(nor buckets.tsv | select #1) -u ' ' -gc 3,4,5 -vs 2 -vcf -threshold 0.9";

            String result = TestUtils.runGorPipe(query);
            String expct = "CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tA\tB\n" +
                    "chr1\t1\trs1\tA\tC\t.\t.\t.\tGT:GP\t0/0:1;0;0\t0/0:1;0;0\n";
            Assert.assertEquals(expct,result);
        } finally {
            Files.delete(pg);
            Files.delete(bp);
        }
    }

    @Test
    public void testCvsselVcfOut() throws IOException {
        String gorcontent = "CHROM\tPOS\tID\tREF\tALT\tbucket\tvalues\n"+
                "chr1\t1\trs1\tA\tC\t1\t01\n";
        String bucketcontent = "#PN\tbucket\n"+
                "A\t1\n"+
                "B\t1\n";

        Path pg = Paths.get("cont.gor");
        Path bp = Paths.get("buckets.tsv");
        try {
            Files.write(pg, gorcontent.getBytes());
            Files.write(bp, bucketcontent.getBytes());
            String query = "gor cont.gor | CSVSEL buckets.tsv <(nor buckets.tsv | select #1) -u 3 -gc 3,4,5 -vs 1 -vcf";

            String result = TestUtils.runGorPipe(query);
            String expct = "CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tA\tB\n" +
                    "chr1\t1\trs1\tA\tC\t.\t.\t.\tGT\t0/0\t0/1\n";

            Assert.assertEquals(expct,result);
        } finally {
            Files.delete(pg);
            Files.delete(bp);
        }
    }

    @Test
    public void testCVSSELLargeDataSetWithPartGor() throws IOException {
        String name = "testCVSSELLargeDataSetWithPartGor";
        Path dictDir = Files.createTempDirectory(name + "_");
        dictDir.toFile().deleteOnExit();
        GorDictionarySetup.createHorizontalDictionary(dictDir.toString(), name, 30, 5, 2);

        String query = "def ##hvarSource## = " + Paths.get(dictDir.toAbsolutePath().toString(), name, name + ".gord") + ";\n" +
                "def ##buckets## = " + Paths.get(dictDir.toAbsolutePath().toString(), name, name + "_buckets.tsv") + ";\n" +
                "partgor -dict ##hvarSource##  <(gor ##hvarSource## \n" +
                "   | csvsel ##buckets##  \n" +
                "       <(nor ##buckets## | select #1 | inset -c #1 <(norrows 1 | calc pn '#{tags}' | split pn | select pn) | top 1)  \n" +
                "       -u ' ' -vs 2 -gc 4,5,3 -tag PN \n" +
                "   | top 20\n" +
                ")";

        String result = TestUtils.runGorPipe(query);
        String expct = "Chrom\tPos\tRef\tAlt\tId\tPN\tvalue\n" +
                "chr1\t1\tG\tT\trs48075\tPN76\t6Q\n" +
                "chr1\t1\tG\tT\trs48075\tPN96\t4d\n" +
                "chr1\t1\tG\tT\trs48075\tPN66\t*C\n" +
                "chr1\t1\tG\tT\trs48075\tPN86\ttQ\n" +
                "chr1\t1\tG\tT\trs48075\tPN36\tj0\n" +
                "chr1\t1\tG\tT\trs48075\tPN11\thh\n" +
                "chr1\t1\tG\tT\trs48075\tPN141\tJk\n" +
                "chr1\t1\tG\tT\trs48075\tPN56\toE\n" +
                "chr1\t1\tG\tT\trs48075\tPN106\tkS\n" +
                "chr1\t1\tG\tT\trs48075\tPN31\tOM\n" +
                "chr1\t1\tG\tT\trs48075\tPN121\tl7\n" +
                "chr1\t1\tG\tT\trs48075\tPN116\tmd\n" +
                "chr1\t1\tG\tT\trs48075\tPN91\t6r\n" +
                "chr1\t1\tG\tT\trs48075\tPN21\t[p\n" +
                "chr1\t1\tG\tT\trs48075\tPN81\t43\n" +
                "chr1\t1\tG\tT\trs48075\tPN16\tBa\n" +
                "chr1\t1\tG\tT\trs48075\tPN101\tTe\n" +
                "chr1\t1\tG\tT\trs48075\tPN146\t4K\n" +
                "chr1\t1\tG\tT\trs48075\tPN136\t~8\n" +
                "chr1\t1\tG\tT\trs48075\tPN26\tiq\n" +
                "chr1\t1\tG\tT\trs48075\tPN51\tTe\n" +
                "chr1\t1\tG\tT\trs48075\tPN61\tp2\n" +
                "chr1\t1\tG\tT\trs48075\tPN6\tQ[\n" +
                "chr1\t1\tG\tT\trs48075\tPN111\tIO\n" +
                "chr1\t1\tG\tT\trs48075\tPN131\tAj\n" +
                "chr1\t1\tG\tT\trs48075\tPN46\tf4\n" +
                "chr1\t1\tG\tT\trs48075\tPN126\tLH\n" +
                "chr1\t1\tG\tT\trs48075\tPN71\tEr\n" +
                "chr1\t1\tG\tT\trs48075\tPN1\t~A\n" +
                "chr1\t1\tG\tT\trs48075\tPN41\t*2\n" +
                "chr1\t2\tA\tT\trs8576\tPN76\t~6\n" +
                "chr1\t2\tA\tT\trs8576\tPN96\tlf\n" +
                "chr1\t2\tA\tT\trs8576\tPN66\tG6\n" +
                "chr1\t2\tA\tT\trs8576\tPN86\tO}\n" +
                "chr1\t2\tA\tT\trs8576\tPN36\toH\n" +
                "chr1\t2\tA\tT\trs8576\tPN11\tTd\n" +
                "chr1\t2\tA\tT\trs8576\tPN141\ts{\n" +
                "chr1\t2\tA\tT\trs8576\tPN56\tIe\n" +
                "chr1\t2\tA\tT\trs8576\tPN106\tJ9\n" +
                "chr1\t2\tA\tT\trs8576\tPN31\te4\n" +
                "chr1\t2\tA\tT\trs8576\tPN121\t8]\n" +
                "chr1\t2\tA\tT\trs8576\tPN116\tAK\n" +
                "chr1\t2\tA\tT\trs8576\tPN91\tt3\n" +
                "chr1\t2\tA\tT\trs8576\tPN21\tF1\n" +
                "chr1\t2\tA\tT\trs8576\tPN81\tfE\n" +
                "chr1\t2\tA\tT\trs8576\tPN16\t7M\n" +
                "chr1\t2\tA\tT\trs8576\tPN101\t3]\n" +
                "chr1\t2\tA\tT\trs8576\tPN146\tD6\n" +
                "chr1\t2\tA\tT\trs8576\tPN136\t~2\n" +
                "chr1\t2\tA\tT\trs8576\tPN26\tFL\n" +
                "chr1\t2\tA\tT\trs8576\tPN51\tIK\n" +
                "chr1\t2\tA\tT\trs8576\tPN61\tP~\n" +
                "chr1\t2\tA\tT\trs8576\tPN6\tt2\n" +
                "chr1\t2\tA\tT\trs8576\tPN111\tL4\n" +
                "chr1\t2\tA\tT\trs8576\tPN131\tkO\n" +
                "chr1\t2\tA\tT\trs8576\tPN46\tt1\n" +
                "chr1\t2\tA\tT\trs8576\tPN126\tl5\n" +
                "chr1\t2\tA\tT\trs8576\tPN71\tA0\n" +
                "chr1\t2\tA\tT\trs8576\tPN1\tht\n" +
                "chr1\t2\tA\tT\trs8576\tPN41\tI5\n";

        Assert.assertEquals(expct,result);
    }
}
