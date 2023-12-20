package org.gorpipe.gor.driver.providers.mdr;

import gorsat.TestUtils;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.test.IntegrationTests;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Category(IntegrationTests.class)
public class UTestMDR {

    @BeforeClass
    public static void setupClass() {
        Properties props = TestUtils.loadSecrets();
        var secret = props.getProperty("GOR_KEYCLOAK_CLIENT_SECRET");
        if (secret != null) {
            System.setProperty("GOR_KEYCLOAK_CLIENT_SECRET", secret);
        }

    }

    @Test
    public void testReadDocument() {
        var result = TestUtils.runGorPipeLines("gor mdr://2806e2ec-30f0-41f1-bdf1-ce3b2def078a | top 10000");

        Assert.assertEquals(10001, result.length);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\n", result[0]);
        Assert.assertEquals("chr1\t11868\t14412\tDDX11L1\n", result[1]);
        Assert.assertEquals("chr1\t35899090\t36023551\tKIAA0319L\n", result[1009]);

    }

    @Test
    public void testReadDocumentWithNor() {
        var result = TestUtils.runGorPipeLines("nor -h mdr://2806e2ec-30f0-41f1-bdf1-ce3b2def078a | top 10000");

        Assert.assertEquals(10001, result.length);
        Assert.assertEquals("ChromNOR\tPosNOR\tChrom\tgene_start\tgene_end\tGene_Symbol\n", result[0]);
        Assert.assertEquals("chrN\t0\tchr1\t11868\t14412\tDDX11L1\n", result[1]);
        Assert.assertEquals("chrN\t0\tchr1\t35899090\t36023551\tKIAA0319L\n", result[1009]);
    }

    @Test
    public void testReadDocuments() {
        var result = TestUtils.runGorPipeLines("gor mdr://2806e2ec-30f0-41f1-bdf1-ce3b2def078a mdr://191b3d28-4db9-4aa3-aa6b-cbb2968885a5 mdr://ee4d7e36-e6dc-42d8-8f78-714242a8cf6d | top 10000");

        Assert.assertEquals(10001, result.length);
        Assert.assertEquals(10001, result.length);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\n", result[0]);
        Assert.assertEquals("chr1\t11868\t14412\tDDX11L1\n", result[1]);
        Assert.assertEquals("chr1\t35519068\t35524872\tRP11-248I9.2\n", result[3000]);
    }

    @Test
    public void testReadDocumentThroughLinkFile() throws IOException {
        Path tempFile = Files.createTempFile("document_", ".gor.link");
        Files.write(tempFile, "mdr://2806e2ec-30f0-41f1-bdf1-ce3b2def078a".getBytes());

        var result = TestUtils.runGorPipeLines("gor " + tempFile + " | top 10000");

        Assert.assertEquals(10001, result.length);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\n", result[0]);
        Assert.assertEquals("chr1\t11868\t14412\tDDX11L1\n", result[1]);
        Assert.assertEquals("chr1\t35899090\t36023551\tKIAA0319L\n", result[1009]);
    }

    @Test
    public void testNonExistingDocumentId() {
        var ex = Assert.assertThrows(GorResourceException.class, () -> {
            TestUtils.runGorPipeLines("gor mdr://f3658d3a-5220-4094-b7b1-311804df3db8");
        });

        Assert.assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    public void testDictionaryWithInvalidEntry() throws IOException {
        // Copy test file to a new file
        Path tempFile = Files.createTempFile("genes_", ".gord");
        Files.copy(Path.of("../tests/data/mdr/genes_mdr_1000.gord"), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Append new line
        var writer = new BufferedWriter(new FileWriter(tempFile.toFile(), true));
        writer.write("mdr://f3658d3a-5220-4094-b7b1-311804df3db8\tppp\n");
        writer.close();

        var ex = Assert.assertThrows(GorResourceException.class, () -> {
            var query = "gor " + tempFile + " | top 10000";
            TestUtils.runGorPipeLines(query);
        });

        Assert.assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    public void test1000EntryDictionary() {
        var result = TestUtils.runGorPipeLines("gor ../tests/data/mdr/genes_mdr_1000.gord | top 10000");

        Assert.assertEquals(10001, result.length);
    }
}
