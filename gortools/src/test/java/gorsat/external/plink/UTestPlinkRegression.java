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

package gorsat.external.plink;

import gorsat.TestUtils;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UTestPlinkRegression {
    String vcfheader = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\ta\tb\tc\td\te\tf\tg\ti\tj\n";
    String regorheader = "Chrom\tPos\tid\tref\talt\tvalues\n";
    String pheno = "#IID\tpheno\n" +
            "a\t1\n"+
            "b\t2\n"+
            "c\t1\n"+
            "d\t2\n"+
            "e\t1\n"+
            "f\t2\n"+
            "g\t1\n"+
            "h\t2\n"+
            "i\t1\n"+
            "j\t2\n";
    String multipheno = "#IID\tpheno\tpheno2\n" +
            "a\t1\t1\n"+
            "b\t2\t1\n"+
            "c\t1\t1\n"+
            "d\t2\t1\n"+
            "e\t1\t1\n"+
            "f\t2\t2\n"+
            "g\t1\t2\n"+
            "h\t2\t2\n"+
            "i\t1\t2\n"+
            "j\t2\t2\n";
    String adjust = "CHROM\tPOS\tID\tREF\tALT\tP\tTEST\tPHENO\n" +
            "chr1\t1\tid\tA\tC\t0.00001\tADD\tpheno\n" +
            "chr2\t2\tid\tG\tT\t0.000001\tADD\tpheno\n";

    String oldPlinkExec;

    @Before
    public void init() {
        oldPlinkExec = System.getProperty("org.gorpipe.gor.driver.plink.executable");
        String wd = Paths.get(".").toAbsolutePath().toString();
        String tmpd = System.getProperty("java.io.tmpdir");
        String dockerPlink = "docker run --rm -i -v "+tmpd+":"+tmpd+" -v "+wd+":"+wd+" -w "+wd+" nextcode/gorplink2runner plink2";
        System.setProperty("org.gorpipe.gor.driver.plink.executable",dockerPlink);
    }

    @After
    public void clenaup() {
        if( oldPlinkExec != null ) System.setProperty("org.gorpipe.gor.driver.plink.executable",oldPlinkExec);
    }

    @Test
    public void testPlink2CoreDumpCleanup() throws Exception {
        Path projectPath = Paths.get(".");
        Path corePath = projectPath.resolve("core.1");
        try {
            Files.write(corePath, new byte[] {1});
            Assert.assertTrue(Files.exists(corePath));
            PlinkArguments args = new PlinkArguments("","",false,false,false,false,false,false,false,0.1f,0.1f, 0.1f);
            PlinkThread plinkThread = new PlinkThread(projectPath.toFile(), projectPath, new String[] {"bash","-c","exit 1"}, ".", "sample", true, null, args, true);
            try {
                Thread.sleep(1000);
                plinkThread.call();
            } catch(Exception e) {
                // Ignore error
            }
            Assert.assertFalse(Files.exists(corePath));
        } finally {
            Files.deleteIfExists(corePath);
        }
    }

    @Ignore("Needs plink2 installed")
    @Test
    public void testPlinkRegressionVcfMissingPheno() throws IOException {
        Path pg = Paths.get("vcf.gor");
        String regor = vcfheader + "chr1\t1\trs1\tA\tC\t.\t.\t.\tGT\t0/0\t0/1\t0/0\t0/1\t0/0\t0/1\t0/0\t0/1\t0/0\t0/1\n";
        try {
            Files.write(pg, regor.getBytes());
            String query = "gor vcf.gor | plinkregression missingpheno.txt";
            TestUtils.runGorPipe(query);
            Assert.fail("Plink should fail with missing phenotype file");
        } catch(Exception e) {
            // we want this
        } finally {
            Files.delete(pg);
        }
    }

    @Ignore("Needs plink2 installed")
    @Test
    public void testPlinkRegressionVcf() throws IOException {
        Path pg = Paths.get("vcf.gor");
        Path pp = Paths.get("pheno.txt");
        String regor = vcfheader + "chr1\t1\trs1\tA\tC\t.\t.\t.\tGT\t0/0\t0/1\t0/0\t0/1\t0/0\t0/1\t0/0\t0/1\t0/0\t0/1\n";
        try {
            Files.write(pg, regor.getBytes());
            Files.write(pp, pheno.getBytes());
            String query = "gor vcf.gor | plinkregression pheno.txt -vcf";
            String res = TestUtils.runGorPipe(query);
            Assert.assertEquals("Regression results not correct", "CHROM\tPOS\tID\tREF\tALT\tA1\tFIRTH\tTEST\tOBS_CT\tOR\tLOG_OR_SE\tZ_STAT\tP\tERRCODE\tPHENO\n" +
                    "chr1\t1\trs1\tA\tC\tC\tN\tADD\t9\t12\t1.60727\t1.54604\t0.122094\t.\tpheno\n", res);
        } finally {
            Files.delete(pg);
            Files.delete(pp);
        }
    }

    @Ignore("Needs plink2 installed")
    @Test
    public void testPlinkRegressionNorPheno() throws IOException {
        Path pg = Paths.get("reg.gor");
        Path pp = Paths.get("pheno.txt");
        String regor = regorheader + "chr1\t1\trs1\tA\tC\t0101010101\n";
        try {
            Files.write(pg, regor.getBytes());
            Files.write(pp, pheno.getBytes());
            String query = "create phn = nor pheno.txt; gor reg.gor | plinkregression [phn]";
            String results = TestUtils.runGorPipe(query);
            Assert.assertEquals("Wrong regression result", "Chrom\tPos\tid\tref\talt\tA1\tFIRTH\tTEST\tOBS_CT\tOR\tLOG_OR_SE\tZ_STAT\tP\tERRCODE\tPHENO\n" +
                    "chr1\t1\trs1\tA\tC\tC\tY\tADD\t10\t120.985\t2.28825\t2.09578\t0.0361018\t.\tpheno\n", results);
        } finally {
            Files.delete(pg);
            Files.delete(pp);
        }
    }


    @Ignore("Needs plink2 installed")
    @Test
    public void testPlinkRegression() throws IOException {
        Path pg = Paths.get("reg.gor");
        Path pp = Paths.get("pheno.txt");
        String regor = regorheader + "chr1\t1\trs1\tA\tC\t0101010101\n";
        try {
            Files.write(pg, regor.getBytes());
            Files.write(pp, pheno.getBytes());
            String query = "gor reg.gor | plinkregression pheno.txt";
            String results = TestUtils.runGorPipe(query);
            Assert.assertEquals("Wrong regression result", "Chrom\tPos\tid\tref\talt\tA1\tFIRTH\tTEST\tOBS_CT\tOR\tLOG_OR_SE\tZ_STAT\tP\tERRCODE\tPHENO\n" +
                    "chr1\t1\trs1\tA\tC\tC\tY\tADD\t10\t120.985\t2.28825\t2.09578\t0.0361018\t.\tpheno\n", results);
        } finally {
            Files.delete(pg);
            Files.delete(pp);
        }
    }

    @Ignore("Needs plink2 installed")
    @Test
    public void testMultiphenoPlinkRegression() throws IOException {
        Path pg = Paths.get("reg.gor");
        Path pp = Paths.get("pheno.txt");
        String regor = regorheader + "chr1\t1\trs1\tA\tC\t0101010101\n";
        try {
            Files.write(pg, regor.getBytes());
            Files.write(pp, multipheno.getBytes());
            String query = "gor reg.gor | plinkregression pheno.txt";
            String results = TestUtils.runGorPipe(query);
            Assert.assertEquals("Wrong regression result", "Chrom\tPos\tid\tref\talt\tA1\tFIRTH\tTEST\tOBS_CT\tOR\tLOG_OR_SE\tZ_STAT\tP\tERRCODE\tPHENO\n" +
                    "chr1\t1\trs1\tA\tC\tC\tY\tADD\t10\t120.985\t2.28825\t2.09578\t0.0361018\t.\tpheno\n" +
                    "chr1\t1\trs1\tA\tC\tC\tN\tADD\t10\t2.25\t1.29099\t0.628144\t0.52991\t.\tpheno2\n", results);
        } finally {
            Files.delete(pg);
            Files.delete(pp);
        }
    }

    @Ignore("Needs plink2 installed")
    @Test
    public void testPlinkRegressionPgenWrite() throws IOException {
        Path pg = Paths.get("reg.gor");
        Path pp = Paths.get("pheno.txt");
        String regor = regorheader + "chr1\t1\trs1\tA\tC\t010101010101\n" +
                "chr2\t2\trs2\tA\tC\t010101010101\n" +
                "chr3\t3\trs3\tA\tC\t010101010101\n" +
                "chr4\t4\trs4\tA\tC\t010101010101\n" +
                "chr5\t5\trs5\tA\tC\t010101010101\n" +
                "chr6\t6\trs6\tA\tC\t010101010101\n" +
                "chr7\t7\trs7\tA\tC\t010101010101\n" +
                "chr8\t8\trs8\tA\tC\t010101010101\n" +
                "chr9\t9\trs9\tA\tC\t010101010101\n";
        try {
            Files.write(pg, regor.getBytes());
            Files.write(pp, (pheno+"k\t1\nl\t2\n").getBytes());
            String query = "gor <(reg.gor | plinkregression pheno.txt)";
            String results = TestUtils.runGorPipe(query);
            Assert.assertEquals("Wrong regression result", "Chrom\tPos\tid\tref\talt\tA1\tFIRTH\tTEST\tOBS_CT\tOR\tLOG_OR_SE\tSE\tZ_STAT\tP\tERRCODE\tPHENO\n" +
                    "chr1\t1\trs1\tA\tC\tC\tY\tADD\t12\t168.997\t2.24178\t2.2883\t0.02212\t.\tpheno\n" +
                    "chr2\t2\trs2\tA\tC\tC\tY\tADD\t12\t168.997\t2.24178\t2.2883\t0.02212\t.\tpheno\n" +
                    "chr3\t3\trs3\tA\tC\tC\tY\tADD\t12\t168.997\t2.24178\t2.2883\t0.02212\t.\tpheno\n" +
                    "chr4\t4\trs4\tA\tC\tC\tY\tADD\t12\t168.997\t2.24178\t2.2883\t0.02212\t.\tpheno\n" +
                    "chr5\t5\trs5\tA\tC\tC\tY\tADD\t12\t168.997\t2.24178\t2.2883\t0.02212\t.\tpheno\n" +
                    "chr6\t6\trs6\tA\tC\tC\tY\tADD\t12\t168.997\t2.24178\t2.2883\t0.02212\t.\tpheno\n" +
                    "chr7\t7\trs7\tA\tC\tC\tY\tADD\t12\t168.997\t2.24178\t2.2883\t0.02212\t.\tpheno\n" +
                    "chr8\t8\trs8\tA\tC\tC\tY\tADD\t12\t168.997\t2.24178\t2.2883\t0.02212\t.\tpheno\n" +
                    "chr9\t9\trs9\tA\tC\tC\tY\tADD\t12\t168.997\t2.24178\t2.2883\t0.02212\t.\tpheno\n", results);
        } finally {
            Files.delete(pg);
            Files.delete(pp);
        }
    }

    @Ignore("Needs plink2 installed")
    @Test
    public void testPlinkRegressionJustHeader() throws IOException {
        Path pg = Paths.get("reg.gor");
        Path pp = Paths.get("pheno.txt");
        try {
            Files.write(pg, regorheader.getBytes());
            Files.write(pp, pheno.getBytes());
            String query = "gor reg.gor | plinkregression pheno.txt";
            String results = TestUtils.runGorPipe(query);
            Assert.assertEquals("Wrong regression result", "CHROM\tPOS\tID\tREF\tALT\tA1\tFIRTH\tTEST\tOBS_CT\tOR\tZ_STAT\tP\tPHENO\n", results);
        } finally {
            Files.delete(pg);
            Files.delete(pp);
        }
    }

    @Ignore("Needs plink2 installed")
    @Test
    public void testPlinkAdjustment() throws IOException {
        Path adj = Paths.get("adjust.gor");
        try {
            Files.write(adj, adjust.getBytes());
            String query1 = "gor adjust.gor | plinkadjustment -s";
            String results1 = TestUtils.runGorPipe(query1);

            String query2 = "gor adjust.gor | plinkadjustment | sort genome";
            String results2 = TestUtils.runGorPipe(query2);

            Assert.assertEquals("Wrong plink adjust result", "CHROM\tPOS\tID\tREF\tALT\tUNADJ\tGC\tQQ\tBONF\tHOLM\tSIDAKSS\tSIDAKSD\tFDRBH\tFDRBY\tTEST\tPHENO\n" +
                    "chr1\t1\tid\tA\tC\t1e-05\t0.522154\t0.75\t2e-05\t1e-05\t1.99999e-05\t1e-05\t1e-05\t1.5e-05\tADD\tpheno\n" +
                    "chr2\t2\tid\tG\tT\t1e-06\t0.478463\t0.25\t2e-06\t2e-06\t2e-06\t2e-06\t2e-06\t3e-06\tADD\tpheno\n", results1);
            Assert.assertEquals("Plink ordered results not the same", results1, results2);
        } finally {
            Files.delete(adj);
        }
    }

    @Test
    @Ignore("Needs plink2 installed")
    public void testPlink() {
        //This script is from Hakon
        //Want to keep it here for debugging purposes.
        final String query = "def #af# = 0.2;\n" +
                "    def #betagt# = ln(2.0);\n" +
                "    def #betacv1# = ln(1.1);\n" +
                "    def #betacv2# = ln(1.2);\n" +
                "   \n" +
                "    def #nummarkers# = 10;\n" +
                "    def #numpns# = 1000;\n" +
                "   \n" +
                "    create #base# = norrows #numpns#\n" +
                "    | calc PN rownum+1\n" +
                "    | select pn\n" +
                "    | sort -c pn\n" +
                "    | calc a1 if(random() < #af#,1,0)\n" +
                "    | calc a2 if(random() < #af#,1,0)\n" +
                "    | calc gt if(a1=1 and a2=1,2,if(a1=1 or a2=1,1,0))\n" +
                "    | calc covar1 invnormal(random())\n" +
                "    | calc covar2 invnormal(random())+covar1/10.0\n" +
                "    | calc pheno1 if(random()<1.0/(1.0+exp(-(#betagt# * gt ))),2,1)\n" +
                "    | calc pheno2 if(random()<1.0/(1.0+exp(-(ln(2*exp(#betagt#)) * gt + #betacv1# * covar1 ))),2,1)\n" +
                "    | calc pheno3 if(random()<1.0/(1.0+exp(-(ln(4*exp(#betagt#)) * gt + #betacv1# * covar1 + #betacv2# * covar2))),2,1)\n" +
                "    | select pn,pheno1-pheno3,covar1,covar2,gt;\n" +
                "    \n" +
                "    create #pheno# = nor [#base#] | select pn-pheno1;\n" +
                "   \n" +
                "    create #covars# = nor [#base#]\n" +
                "    | select pn,covar1,covar2  | calc covar3 sin(random());\n" +
                "   \n" +
                "    create #gts# = nor [#base#]\n" +
                "    | sort -c pn\n" +
                "    | group -lis -sc gt -s '' -len 1000\n" +
                "    | rename lis_gt values\n" +
                "    | calc m '0,1,2,3,4' | split m\n" +
                "    | replace values fsvmap(values,1,'if(random()<float(m)/10,\"3\",x)','')\n" +
                "    | select m,values;\n" +
                "   \n" +
                "    create ##pheno## = nor [#pheno#]\n" +
                "    | rename #1 IID\n" +
                "    | select IID,2-\n" +
                "    | distinct\n" +
                "    | sort -c IID;\n" +
                "   \n" +
                "    create ##PNs## = nor [##pheno##]\n" +
                "    | rename IID PN\n" +
                "    | select PN\n" +
                "    | distinct;\n" +
                "   \n" +
                "    create ##covar## = nor [#covars#]\n" +
                "    | rename #1 IID\n" +
                "    | select IID,2-\n" +
                "    | inset -c IID [##pheno##]\n" +
                "    | sort -c IID;\n" +
                "   \n" +
                "    create ##PNs## = nor [##pheno##]\n" +
                "    | rename IID PN\n" +
                "    | select PN\n" +
                "    | distinct;\n" +
                "   \n" +
                "    create #positions# = norrows #nummarkers#\n" +
                "    | calc Chrom 'chr1'\n" +
                "    | calc POS 1000000+rownum\n" +
                "    | calc ID 'rs00'+rownum\n" +
                "    | select chrom,pos,id;\n" +
                "   \n" +
                "    create ##glm## = gor [#positions#]\n" +
                "    | select chrom-\n" +
                "    | calc REF upper(refbase(chrom,pos))\n" +
                "    | calc ALT revcompl(ref)\n" +
                "    | select chrom,pos,id,ref,alt\n" +
                "    | calc m mod(pos,5)\n" +
                "    | multimap -c m -h [#gts#]\n" +
                "    | select chrom,pos,id,ref,alt,values\n" +
                "    | PLINKREGRESSION [##pheno##] -covar [##covar##];\n" +
                "   \n" +
                "    gor [##glm##]";
        final String results = TestUtils.runGorPipe(query);
        System.err.println(results);
    }
}
