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
import org.gorpipe.gor.GorSession;
import org.gorpipe.gor.GorSessionFactory;
import org.gorpipe.test.utils.FileTestUtils;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
import gorsat.process.GenericSessionFactory;
import gorsat.process.PipeInstance;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sigmar on 05/12/2016.
 */
public class UTestTableFunction {

    private File rsIDsFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        rsIDsFile = FileTestUtils.createTempFile(workDir.getRoot(), "rsIDsFile.txt",
                "rs544101329\n" +
                        "rs28970552"
        );
    }

    @Before
    public void setupTest() {
        Path currentPath = Paths.get("../tests/data/reports");
        String macroPath = currentPath.toAbsolutePath().normalize().toString();
        System.setProperty("dialog.macrodir", macroPath);
    }

    @AfterClass
    public static void cleanup() {
        System.gc();
    }

    @Test
    public void testTablefunctionFreemarkerMacros() {
        String query = "create tf = tablefunction ../tests/data/reports/testmacro.yml() | signature -timeres 1; gor [tf]";
        Assert.assertEquals(5, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testFreemarkerMacros() {
        String query = "gor ../tests/data/reports/testmacro.yml()";
        Assert.assertEquals(5, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTablefunctionWithWrite() {
        try {
            File tmpfile = File.createTempFile("test", ".gor");
            tmpfile.deleteOnExit();

            String query = "gor ../tests/data/reports/test2.yml() | write " + tmpfile.toString();
            Assert.assertEquals(0, TestUtils.runGorPipeCount(query));

            query = "gor " + tmpfile.toString();
            Assert.assertEquals(10, TestUtils.runGorPipeCount(query));
        } catch (IOException ex) {
            Assert.fail();
        }
    }

    @Test
    public void testStepFunction() {
        int expected = 10;
        String query = "gor ../tests/data/reports/lib.yml(default_query) | pipesteps ../tests/data/reports/lib.yml(mystep)";
        GorSessionFactory factory = new GenericSessionFactory();

        PipeInstance pi = PipeInstance.createGorIterator(factory.create().getGorContext());
        pi.init(query, false, "");
        int count = 0;
        while (pi.hasNext()) {
            pi.next();
            count++;
        }
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testStepFunctionWithArguments() {
        int expected = 11;
        String query = "gor ../tests/data/reports/lib.yml(default_query) | pipesteps ../tests/data/reports/lib.yml(mystep,top=" + expected + ")";
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunction() {
        String query = "create tf = tablefunction ../tests/data/reports/test2.yml(query2) | signature -timeres 1; gor [tf]";
        Assert.assertEquals(48, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionWithColon() {
        String query = "gor ../tests/data/reports/test.yml(top=10,bleh=':')";
        Assert.assertEquals(10, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionWithDefaultValues() {
        String query = "gor ../tests/data/reports/test.yml(top=10,some='thing')";
        Assert.assertEquals(10, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionWithPerspective() {
        String query = "gor ../tests/data/reports/testperspective.yml(perspective=filter)";
        Assert.assertEquals(59, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionPgor() {
        int expected = 11;
        String query = "create tf = tablefunction ../tests/data/reports/test2.yml(top = " + expected + "); pgor [tf]";
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionPnListFuncStyle() {
        int expected = 2;
        String query = "create tf = tablefunction ../tests/data/reports/test3.yml::TestReport(top = " + expected + ", CASEs = ['rs544101329','rs28970552']) | signature -timeres 1; gor [tf]";
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionPnListFuncParams() {
        String query = "create tf = tablefunction ../tests/data/reports/test3.yml::TestReport(query2) -f 'rs544101329','rs28970552' -s rsIDs | top 2 | signature -timeres 1; gor [tf]";
        Assert.assertEquals(2, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionWithPipesteps() {
        String query = "create tf = tablefunction ../tests/data/reports/test3.yml::TestReport(top = 10, CASEs = ['rs544101329','rs28970552']) | calc a 'test' | top 1 | signature -timeres 1; gor [tf]";
        Assert.assertEquals(1, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionPnListFuncStyleWithPipesteps() {
        String query = "create tf = tablefunction ../tests/data/reports/test2.yml::TestReport(query2, top = 10, CASEs = ['rs544101329','rs28970552']) | calc a 'test' | top 1 | signature -timeres 1; gor [tf]";
        Assert.assertEquals(1, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionPnList() {
        int expected = 2;
        String query1 = "create tf = tablefunction ../tests/data/reports/test3.yml::TestReport(top = " + expected + ", CASEs = ['rs544101329','rs28970552']) | signature -timeres 1; gor [tf]";
        String query2 = "create tf = tablefunction ../tests/data/reports/test3.yml::TestReport(top = " + expected + ", CASEs = ['rs544101329','rs28970552','rs28970553']) | signature -timeres 1; gor [tf]";
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query1));
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query2));
    }

    @Test
    public void testTableFunctionPnVirtualFile() throws IOException {
        String query = "create PNs = nor " + rsIDsFile.getCanonicalPath() + "; create tf = tablefunction ../tests/data/reports/test4.yml::TestReport(top = 2, CASEs=[PNs]) | multimap -cartesian [PNs] | signature -timeres 1; gor [tf]";
        Assert.assertEquals(4, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testYamlPnList() throws IOException {
        String query = "create PNs = nor " + rsIDsFile.getCanonicalPath() + "; gor ../tests/data/reports/test3.yml::TestReport(top = 2, CASEs=['rs544101329','rs28970552']) | multimap -cartesian [PNs]";
        Assert.assertEquals(4, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCreateYamlPnList() throws IOException {
        String query = "create PNs = nor " + rsIDsFile.getCanonicalPath() + "; create res = gor ../tests/data/reports/test3.yml::TestReport(top = 2, CASEs=['rs544101329','rs28970552']) | multimap -cartesian [PNs]; gor [res]";
        Assert.assertEquals(4, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCreateNorYaml() {
        String query = "nor ../tests/data/reports/test3.yml::TestReport(query2)";
        Assert.assertEquals(48, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCreateNorCreateYaml() {
        String query = "create xxx = nor ../tests/data/reports/test3.yml::TestReport(query2) | signature -timeres 1; nor [xxx]";
        Assert.assertEquals(48, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCreateNorYamlPnList() throws IOException {
        String query = "create PNs = nor " + rsIDsFile.getCanonicalPath() + "; create res = nor ../tests/data/reports/test3.yml::TestReport(query2) | multimap -cartesian [PNs] | signature -timeres 1; nor [res]";
        Assert.assertEquals(96, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCreatePgorYamlPnList() throws IOException {
        String query = "create PNs = nor " + rsIDsFile.getCanonicalPath() + "; create res = pgor ../tests/data/reports/test3.yml::TestReport(top = 2, CASEs=['rs544101329','rs28970552']) | multimap -cartesian [PNs] | signature -timeres 1; gor [res]";
        Assert.assertEquals(4, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCreatePgorParamsYamlPnList() throws IOException {
        String query = "create PNs = nor " + rsIDsFile.getCanonicalPath() + "; create res = pgor ../tests/data/reports/test3.yml::TestReport(query2) -f 'rs544101329','rs28970552' -s rsIDs | top 2 | multimap -cartesian [PNs] | signature -timeres 1; gor [res]";
        Assert.assertEquals(4, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testUrlParameters() throws IOException {
        String query = "create PNs = nor " + rsIDsFile.getCanonicalPath() + " | signature -timeres 1; gor ../tests/data/reports/test4.yml?TestReport&top=2&CASEs=[PNs] | multimap -cartesian [PNs]";
        Assert.assertEquals(4, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testYamlPnVirtualFile() throws IOException {
        String query = "create PNs = nor " + rsIDsFile.getCanonicalPath() + " | signature -timeres 1; gor ../tests/data/reports/test4.yml::TestReport(top = 2, CASEs=[PNs]) | multimap -cartesian [PNs]";
        Assert.assertEquals(4, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionPnFile() throws IOException {
        int expected = 2;
        String query = "create tf = tablefunction ../tests/data/reports/test3.yml::TestReport(top = " + expected + ", CASEs=" + rsIDsFile.getCanonicalPath() + ") | signature -timeres 1; gor [tf]";
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testYmlWithPnFile() throws IOException {
        int expected = 2;
        String query = "create tf = gor ../tests/data/reports/test4.yml::TestReport(top = " + expected + ", CASEs='" + rsIDsFile.getCanonicalPath() + "') | signature -timeres 1; gor [tf]";
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testTableFunctionPnFileInput() throws IOException {
        int expected = 2;
        String query = "create tf = tablefunction ../tests/data/reports/test4.yml::TestReport(top = " + expected + ", CASEs='" + rsIDsFile.getCanonicalPath() + "') | signature -timeres 1; gor [tf]";
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCustomEntryNestedFunction() {
        int expected = 12;
        String query = "gor ../tests/data/gor/dbsnp_test.gorz | cmd {../tests/data/reports/test.yml(passthrough)} | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCustomEntryCmd() {
        int expected = 2;
        String query = "cmd {../tests/data/reports/test.yml::TestReport(echo)} | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));

    }

    @Test
    public void testCustomYamlEntryFunctionSyntaxCmd() {
        int expected = 2;
        String query = "cmd {../tests/data/reports/test.yml(echo)} | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCustomYamlEntryFunctionSyntaxDefaultCmd() {
        int expected = 2;
        String query = "cmd {../tests/data/reports/test5.yml} | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCustomYamlEntryFunctionParamSyntaxCmd() {
        int expected = 2;
        String query = "cmd {../tests/data/reports/test.yml::TestReport(echo,stuff=stuff)} | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testCustomYamlEntryURLParamSyntaxCmd() {
        int expected = 2;
        String query = "cmd {../tests/data/reports/test.yml?TestReport&query=echo&stuff=stuff} | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testYamlFile() {
        int expected = 10;
        String query = "gor ../tests/data/reports/test.yml(top=50) | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testYamlFileNoParams() {
        String query = "gor ../tests/data/reports/test2.yml";
        Assert.assertEquals(10, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testYamlFileBracketSyntax() {
        int expected = 10;
        String query = "gor ../tests/data/reports/test.yml(top=50) | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testYamlFileBracketSyntaxWithFunction() {
        int expected = 10;
        String query = "gor ../tests/data/reports/test.yml::TestReport(top=50) | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testYamlGridParsing() {
        String query1 = "gor ../tests/data/reports/test_report.yml::TestReport(query,GOR_report=../tests/data/gor/dbsnp_test.gorz)";
        String query2 = "gor ../tests/data/reports/test_report.yml::TestReport(query2,GOR_report=../tests/data/gor/dbsnp_test.gorz)";

        //Test [:gorgrid]
        Assert.assertEquals(2, TestUtils.runGorPipeCount(query1));
        //Test [:grid]
        Assert.assertEquals(5, TestUtils.runGorPipeCount(query2));
    }

    @Test
    public void testNestedQueryWithYamlContainingCreates() {
        int expected = 10;
        String query = "gor <(gor ../tests/data/reports/test2.yml::TestReport(top=50)) | top " + expected;
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    @Ignore
    public void testQueryService() {
        int expected = 11;
        String query = "create tf = tablefunction demo(top = " + expected + ") | signature -timeres 1; gor [tf]";
        Assert.assertEquals(expected, TestUtils.runGorPipeCount(query));
    }

    @Test
    public void testReportBuilderReplaceErrorForContains() {
        GorSessionFactory factory = new GenericSessionFactory();
        GorSession gorPipeSession = factory.create();

        String qr = ReportUtilities.parseYaml("../tests/data/reports/test6.yml(pathways=REACTOME||Interleukin-1_processing)", gorPipeSession);

        Assert.assertTrue(qr.contains("where Pathway in ('REACTOME||Interleukin-1_processing')"));
    }

    @Test
    public void testReportBuilderReplaceErrorForContainsWithComma() {
        GorSessionFactory factory = new GenericSessionFactory();
        GorSession gorPipeSession = factory.create();

        String query1 = ReportUtilities.parseYaml("../tests/data/reports/test7.yml(pathways=REACTOME||mRNA_3'-end_processing)", gorPipeSession);
        String query2 = ReportUtilities.parseYaml("../tests/data/reports/test7.yml(pathways=REACTOME||mRNA_3-end_processing')", gorPipeSession);
        String query3 = ReportUtilities.parseYaml("../tests/data/reports/test7.yml(pathways='REACTOME||mRNA_3-end_processing)", gorPipeSession);
        String query4 = ReportUtilities.parseYaml("../tests/data/reports/test7.yml(pathways='REACTOME||mRNA_3'-end_processing')", gorPipeSession);

        String expectedQuery1 = "create ##dummy## = gor ##genes## | top 1;def ##ref## = ref;def ##genes## = ##ref##/genes.gorz;def ##gene2pathway## = ##ref##/ensgenes/ensgenes_gene2pathway.mmap;create ##genePathway## = nor -h ##gene2pathway##;create ##pathwayGene## = nor -h ##gene2pathway## | rename Gene_symbol Gene_in_pathway | columnsort pathway,Gene_in_pathway;gor ##genes## | select 1-3,gene_symbol | multimap [##genePathway##] -c gene_symbol -n Pathway\n| where Pathway in ('REACTOME||mRNA_3\\'-end_processing') | multimap [##pathwayGene##] -c pathway -n Gene_in_pathway\n\n | group 1 -gc 3,gene_symbol -sc pathway,Gene_in_pathway -set -len 1000 | rename set_pathway pathways | rename set_Gene_in_pathway Genes_in_pathway";
        String expectedQuery2 = "create ##dummy## = gor ##genes## | top 1;def ##ref## = ref;def ##genes## = ##ref##/genes.gorz;def ##gene2pathway## = ##ref##/ensgenes/ensgenes_gene2pathway.mmap;create ##genePathway## = nor -h ##gene2pathway##;create ##pathwayGene## = nor -h ##gene2pathway## | rename Gene_symbol Gene_in_pathway | columnsort pathway,Gene_in_pathway;gor ##genes## | select 1-3,gene_symbol | multimap [##genePathway##] -c gene_symbol -n Pathway\n| where Pathway in ('REACTOME||mRNA_3-end_processing\\'') | multimap [##pathwayGene##] -c pathway -n Gene_in_pathway\n\n | group 1 -gc 3,gene_symbol -sc pathway,Gene_in_pathway -set -len 1000 | rename set_pathway pathways | rename set_Gene_in_pathway Genes_in_pathway";
        String expectedQuery3 = "create ##dummy## = gor ##genes## | top 1;def ##ref## = ref;def ##genes## = ##ref##/genes.gorz;def ##gene2pathway## = ##ref##/ensgenes/ensgenes_gene2pathway.mmap;create ##genePathway## = nor -h ##gene2pathway##;create ##pathwayGene## = nor -h ##gene2pathway## | rename Gene_symbol Gene_in_pathway | columnsort pathway,Gene_in_pathway;gor ##genes## | select 1-3,gene_symbol | multimap [##genePathway##] -c gene_symbol -n Pathway\n| where Pathway in ('\\'REACTOME||mRNA_3-end_processing') | multimap [##pathwayGene##] -c pathway -n Gene_in_pathway\n\n | group 1 -gc 3,gene_symbol -sc pathway,Gene_in_pathway -set -len 1000 | rename set_pathway pathways | rename set_Gene_in_pathway Genes_in_pathway";
        String expectedQuery4 = "create ##dummy## = gor ##genes## | top 1;def ##ref## = ref;def ##genes## = ##ref##/genes.gorz;def ##gene2pathway## = ##ref##/ensgenes/ensgenes_gene2pathway.mmap;create ##genePathway## = nor -h ##gene2pathway##;create ##pathwayGene## = nor -h ##gene2pathway## | rename Gene_symbol Gene_in_pathway | columnsort pathway,Gene_in_pathway;gor ##genes## | select 1-3,gene_symbol | multimap [##genePathway##] -c gene_symbol -n Pathway\n| where Pathway in ('REACTOME||mRNA_3\\'-end_processing') | multimap [##pathwayGene##] -c pathway -n Gene_in_pathway\n\n | group 1 -gc 3,gene_symbol -sc pathway,Gene_in_pathway -set -len 1000 | rename set_pathway pathways | rename set_Gene_in_pathway Genes_in_pathway";

        Assert.assertEquals(expectedQuery1, query1);
        Assert.assertEquals(expectedQuery2, query2);
        Assert.assertEquals(expectedQuery3, query3);
        Assert.assertEquals(expectedQuery4, query4);
    }

    @Test
    public void testReportBuilderReplaceErrorForContainsWithCommaAtEnd() {
        GorSessionFactory factory = new GenericSessionFactory();
        GorSession gorPipeSession = factory.create();


        String qr = ReportUtilities.parseYaml("../tests/data/reports/test7.yml(pathways=REACTOME||mRNA_3')", gorPipeSession);
        String expectedQuery = "create ##dummy## = gor ##genes## | top 1;def ##ref## = ref;def ##genes## = ##ref##/genes.gorz;def ##gene2pathway## = ##ref##/ensgenes/ensgenes_gene2pathway.mmap;create ##genePathway## = nor -h ##gene2pathway##;create ##pathwayGene## = nor -h ##gene2pathway## | rename Gene_symbol Gene_in_pathway | columnsort pathway,Gene_in_pathway;gor ##genes## | select 1-3,gene_symbol | multimap [##genePathway##] -c gene_symbol -n Pathway\n| where Pathway in ('REACTOME||mRNA_3\\'') | multimap [##pathwayGene##] -c pathway -n Gene_in_pathway\n\n | group 1 -gc 3,gene_symbol -sc pathway,Gene_in_pathway -set -len 1000 | rename set_pathway pathways | rename set_Gene_in_pathway Genes_in_pathway";
        Assert.assertTrue(qr.contains("where Pathway in ('REACTOME||mRNA_3\\'')"));
        Assert.assertEquals(qr, expectedQuery);
    }

    @Test
    public void testRefSeqAccessThroughYaml() throws IOException {
        File f = File.createTempFile("refseq", ".yml");
        f.deleteOnExit();
        FileUtils.writeStringToFile(f, "TestReport:\n" +
                "    query:\n" +
                "        gor -p chr1 ../tests/data/gor/dbsnp_test.gor | calc x refbases(chrom,pos,pos+2)", (Charset) null);

        // Create the dummy reference to go with this test and notify the gorpipe session where the reference is.
        File d = Files.createTempDir();
        d.deleteOnExit();

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < 1000; i++) {
            builder.append("NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN");
        }

        FileUtils.writeStringToFile(new File(d, "chr1.txt"), builder.toString(), Charset.defaultCharset());
        File configFile = new File(d, "config.txt");
        FileUtils.writeStringToFile(configFile, "buildPath\t" + d.getAbsolutePath(), Charset.defaultCharset());

        String query = "gor " + f.getAbsolutePath();
        GenericSessionFactory factory = new GenericSessionFactory();
        factory.setConfigFile(configFile.getAbsolutePath());

        PipeInstance pi = PipeInstance.createGorIterator(factory.create().getGorContext());
        pi.init(query, false, "");
        int count = 0;
        while (pi.hasNext()) {
            pi.next();
            count++;
        }
        pi.close();

        Assert.assertEquals(2, count);

    }

    @Test
    public void testReportWithGorCallbackResultingInString() throws IOException {
        File path = createTestYml("    <#assign x = gor(\"norrows 1 -offset 100\")>\n    gor ../tests/data/gor/genes.gor | top ${x}\n");
        int count = TestUtils.runGorPipeCount("gor " + path.getAbsolutePath());

        Assert.assertEquals(100, count);
    }

    @Test
    public void testReportWithGorCallbackResultingInNumber() throws IOException {

        String query = "    <#assign x = gor(\"norrows 1 -offset 100\", \"number\")>\n" +
                "    <#if x gt 10>\n" +
                "    gor ../tests/data/gor/genes.gor | top ${x}\n" +
                "    <#else>\n" +
                "    gor ../tests/data/gor/genes.gor | top ${x}\n" +
                "    </#if>\n";

        File path = createTestYml(query);
        int count = TestUtils.runGorPipeCount("gor " + path.getAbsolutePath());

        Assert.assertEquals(100, count);

    }

    @Test(expected = TemplateException.class)
    public void testReportWithGorCallbackResultingInNumberComparedToStringError() throws IOException {

        String query = "    <#assign x = gor(\"norrows 1 -offset 100\")>\n" +
                "    <#if x gt 10>\n" +
                "    gor ../tests/data/gor/genes.gor | top ${x}\n" +
                "    <#else>\n" +
                "    gor ../tests/data/gor/genes.gor | top ${x}\n" +
                "    </#if>\n";

        File path = createTestYml(query);
        TestUtils.runGorPipeCount("gor " + path.getAbsolutePath());

        Assert.fail("We are comparing number to string in freemarker which should fail.");
    }

    @Test
    public void testReportWithGorCallbackResultingInSequence() throws IOException {
        File path = createTestYml("    <#assign x = gor(\"norrows 55 -offset 1000\", \"list\")>\n    gor ../tests/data/gor/genes.gor | top ${x?size}\n");
        int count = TestUtils.runGorPipeCount("gor " + path.getAbsolutePath());

        Assert.assertEquals(55, count);
    }

    @Test
    public void testReportWithGorCallbackResultingInIterator() throws IOException {
        String query = "    <#assign x = gor(\"norrows 100\", \"iterator\")>\n" +
                "    <#list x as xxx>\n" +
                "    create foo_${xxx} = gor ../tests/data/gor/genes.gor | top ${xxx};\n" +
                "    </#list>\n" +
                "    gor [foo_55]\n";

        File path = createTestYml(query);
        int count = TestUtils.runGorPipeCount("gor " + path.getAbsolutePath());

        Assert.assertEquals(55, count);
    }

    @Test(expected = TemplateModelException.class)
    public void testReportWithGorCallbackNoInput() throws IOException {
        File path = createTestYml("    <#assign x = gor()>\n    gor ../tests/data/gor/genes.gor | top ${x}\n");
        TestUtils.runGorPipeCount("gor " + path.getAbsolutePath());

        Assert.fail("Gor freemarker method requires at least a query as input.");
    }

    @Test(expected = TemplateModelException.class)
    public void testReportWithGorCallbackUnknownDataFormatError() throws IOException {
        File path = createTestYml("    <#assign x = gor(\"norrows 1 -offset 100\", \"foo\")>\n    gor ../tests/data/gor/genes.gor | top ${x}\n");
        TestUtils.runGorPipeCount("gor " + path.getAbsolutePath());

        Assert.fail("There is not data type called foo in gor freemarker method");
    }

    private File createTestYml(String query) throws IOException {
        File tmpFile = File.createTempFile("ymltest", ".yml");
        tmpFile.deleteOnExit();

        StringBuilder builder = new StringBuilder();

        builder.append("Test Report:\n");
        builder.append("  query:\n");
        builder.append(query);

        FileUtils.writeStringToFile(tmpFile, builder.toString(), Charset.defaultCharset());

        return tmpFile;
    }
}
