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

import htsjdk.samtools.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Created by sigmar on 25/06/15.
 */
public class UTestGorGenomeScan {

    /**
     * Test gor line count
     *
     * @throws Exception
     */
    @Test
    public void testGorGenomeCount() {
        String res = TestUtils.runGorPipeNoHeader("gor ../tests/data/external/samtools/serialization_test.bam | group chrom -count");
        String[] split = res.split("\n")[0].split("\t");
        Assert.assertEquals(1, Integer.parseInt(split[3].trim()));
    }

    /**
     * Test gor vcfiterator
     *
     * @throws Exception
     */
    @Test
    public void testGorVcfGenomeScan() throws IOException {
        String curdir = new File(".").getAbsolutePath();
        String[] args = new String[]{"gor " + curdir.substring(0, curdir.length() - 1) + "../tests/data/external/samtools/test.vcf.gz"};

        String bstr = TestUtils.runGorPipe(args);
        Stream<String> strstr = new BufferedReader(new StringReader(bstr)).lines();
        Path p = Paths.get(curdir.substring(0, curdir.length() - 1) + "../tests/data/external/samtools/test.out");
        Stream<String> cmpstr = new BufferedReader(new InputStreamReader(Files.newInputStream(p))).lines();
        Iterator<String> cmpitr = cmpstr.iterator();

        strstr.forEach(line -> {
            Assert.assertEquals(cmpitr.next(), line);
        });

        cmpstr.close();
        strstr.close();
    }

    /**
     * Test gor bamiterator
     *
     * @throws Exception
     */
    @Test
    public void testGorBamGenomeScan() throws IOException {
        String curdir = new File(".").getAbsolutePath();
        String[] args = new String[]{"gor " + curdir.substring(0, curdir.length() - 1) + "../tests/data/external/samtools/serialization_test.bam"};

        String bstr = TestUtils.runGorPipe(args);
        Stream<String> strstr = new BufferedReader(new StringReader(bstr)).lines();
        Path p = Paths.get(curdir.substring(0, curdir.length() - 1) + "../tests/data/external/samtools/serialization_test.out");
        Stream<String> cmpstr = new BufferedReader(new InputStreamReader(Files.newInputStream(p))).lines();
        Iterator<String> cmpitr = cmpstr.iterator();

        strstr.forEach(line -> {
            Assert.assertEquals(cmpitr.next(), line);
        });

        cmpstr.close();
        strstr.close();
    }

    /**
     * Test gor bamiterator naming system
     *
     * @throws Exception
     */
    @Test
    public void testGorBamWithChrNamingWithoutChr1() throws IOException {
        String[] sam = {
                "@HD\tVN:1.4\tGO:none\tSO:coordinate",
                "@SQ\tSN:chr21\tLN:48129895",
                "@SQ\tSN:chr22\tLN:51304566",
                "@RG\tID:WGC00Regional\tPL:illumina\tLB:WGC00Regional\tPI:400\tSM:WGC00Regional",

                "ST-E00211:59:H3HMWCCXX:7:1222:25136:45101\t69\tchr21\t9411901\t0\t*\t=\t9411901\t0\tATACCTTCCAGCACTACAAACTAGAAGACAAAAGAGACATGGATACCAAATTAAGTTCTTAATAGTGAAGAATAAAAGAGACTACCTTGCCTGCTCCAGTGGATCCAGCAACAGCCAACAACTGTCCTCTTTCTATCTTGGAATATCTTT\t?:=AA;-;/?@=?A>?AB?AA>=8?A<AABAAA<ABA@B@3??@A@>?AA=8AA@78;;<=ABA);)6?9??@AAAABABAABAABB?BAB>@=B.:?BABAAB>@B@ABAABBA:BAABAABB?>@B@B??ABABAB?BA==@?@8:><\tMC:Z:82M68S\tBD:Z:JJJJKJJJKKKKKJKJJJJCJKJKJKJJJJJCCJJJJJJJKKJJJJKKJCJJJJJKJJJJJJJJKKJKKJJKJJJCCJJJJJKJJKJJJKKJKKJJKKKKJKJJJKKKKKJJJKKKKJJJJJKKJJKJJJJDJJJJJJJJKJKJJJJJJD\tPG:Z:MarkDuplicates\tRG:Z:WGC00Regional\tBI:Z:KKKKKLKKKLKKLKKKKKKFKKKKKKKKKKKFFKKKKKKKLKKKKKKLKFLKKKKLKKKKKKLKKLJLKKKKLKKFFKKKKKKKKKLKLKKLKKKKKLKLJKKKKKLKKLKKKKKKLKKKKKKKJKKLKKKHKKKKKKKLKKKLKKKKKH\tAS:i:0\tXS:i:0",
                "ST-E00211:59:H3HMWCCXX:7:1222:25136:45101\t137\tchr21\t9411901\t60\t82M68S\t=\t9411901\t0\tTAAATAGCATTGAGTTATCAGTACTTTCATATCTTGATACATTTCTTCCTGAAAATGTTCATGCCTGCTGATTTGTCTGTTTTTTTTTTTTTTTTTTTTTTAATTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTGTTT\t>>??AABABB>BABA?ABABBAAAB??ABBABAB?BABAABB??AB?ABBBAAAABBA?ABBBABBBABBAB??BAABBA??????????????????<,,*,-48,<?<??????????<?????4<<?,<4????????5<,84*/--\tSA:Z:chr21,32879486,+,96S50M4S,0,0;chr22,36687721,-,40S31M79S,0,0;\tBD:Z:JJJCJJKKKJJJKJKJJJJJKKJJKJDJJJJJJJJJKJJJJJJDJJJJKJKKKCCJKJJJJJKKKJKKJKKJJDJJJJKJJDDDDDDDDDDDDDDDDDDDDJJJJDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDJJJD\tMD:Z:30G17T15T17\tPG:Z:MarkDuplicates\tRG:Z:WGC00Regional\tBI:Z:KKKFLKKKLKKLLKLKKKKKKLKKKKHKKKKKKKKLLKKKKKKHKKKKKLKLKFFLLJKKKKLKKLKKKKLKKHLJKKKJKHHHHHHHHHHHHHHHHHHHHKKLKHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHLJKH\tNM:i:3\tAS:i:67\tXS:i:0"
        };

        String samstring = String.join("\n", sam) + "\n";
        InputStream samstr = new ByteArrayInputStream(samstring.getBytes());

        Path tmpfile = Files.createTempFile("", ".bam");
        SamInputResource sir = SamInputResource.of(samstr);
        SamReader reader = SamReaderFactory.makeDefault().open(sir);
        SAMFileWriterFactory samf = new SAMFileWriterFactory();
        samf.setCreateIndex(true);
        SAMFileWriter sf = samf.makeBAMWriter(reader.getFileHeader(), true, tmpfile.toFile());
        reader.forEach(entry -> sf.addAlignment(entry));
        sf.close();

        String[] args = new String[]{"gor " + tmpfile.toAbsolutePath().toString()};

        String bstr = TestUtils.runGorPipe(args);
        Stream<String> strstr = new BufferedReader(new StringReader(bstr)).lines().skip(1);

        String[] res = {"chr21\t9411901\t9411982\tST-E00211:59:H3HMWCCXX:7:1222:25136:45101\t137\t60\t82M68S\t30G17T15T17\tchr21\t9411901\t0\tTAAATAGCATTGAGTTATCAGTACTTTCATATCTTGATACATTTCTTCCTGAAAATGTTCATGCCTGCTGATTTGTCTGTTTTTTTTTTTTTTTTTTTTTTAATTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTGTTT\t>>??AABABB>BABA?ABABBAAAB??ABBABAB?BABAABB??AB?ABBBAAAABBA?ABBBABBBABBAB??BAABBA??????????????????<,,*,-48,<?<??????????<?????4<<?,<4????????5<,84*/--\tSA=chr21,32879486,+,96S50M4S,0,0;chr22,36687721,-,40S31M79S,0,0; BD=JJJCJJKKKJJJKJKJJJJJKKJJKJDJJJJJJJJJKJJJJJJDJJJJKJKKKCCJKJJJJJKKKJKKJKKJJDJJJJKJJDDDDDDDDDDDDDDDDDDDDJJJJDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDJJJD PG=MarkDuplicates RG=WGC00Regional BI=KKKFLKKKLKKLLKLKKKKKKLKKKKHKKKKKKKKLLKKKKKKHKKKKKLKLKFFLLJKKKKLKKLKKKKLKKHLJKKKJKHHHHHHHHHHHHHHHHHHHHKKLKHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHLJKH NM=3 AS=67 XS=0 RB="};
        Stream<String> cmpstr = Arrays.stream(res);
        Iterator<String> cmpitr = cmpstr.iterator();

        strstr.forEach(line -> {
            Assert.assertEquals(cmpitr.next(), line);
        });

        cmpstr.close();
        strstr.close();
    }
}
