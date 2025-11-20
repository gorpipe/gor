package org.gorpipe.gor.driver.providers.mdr;

import gorsat.TestUtils;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.providers.stream.sources.mdr.MdrConfiguration;
import org.gorpipe.gor.driver.providers.stream.sources.mdr.MdrServer;
import org.gorpipe.test.IntegrationTests;
import org.gorpipe.utils.DriverUtils;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Category(IntegrationTests.class)
//@Ignore("Can not access Keycloak from the the Gitlab build servers.")
public class UTestMDR {

    private static String S3_REGION = "us-ashburn-1";
    private static final String S3_BUCKET = "mdr-genomic-data-dev";

    private static MdrConfiguration config;
    private static String securityContext;

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();


    @BeforeClass
    public static void setupClass() throws IOException {
        Properties props = TestUtils.loadSecrets();
        var secret = props.getProperty("GOR_KEYCLOAK_CLIENT_SECRET");
        if (secret != null) {
            System.setProperty("GOR_KEYCLOAK_CLIENT_SECRET", secret);
        }

        securityContext = DriverUtils.createSecurityContext(
                "s3",
                S3_BUCKET,
                Credentials.OwnerType.System, "",
                props.getProperty("MDR_S3_KEY"),
                props.getProperty("MDR_S3_SECRET"),
                props.getProperty("MDR_S3_ENDPOINT"),
                "");

        config = ConfigManager.createPrefixConfig("gor.mdr", MdrConfiguration.class);

        Path credFile = Files.createTempFile("gor.mdr.credentials", ".tmp");
        credFile.toFile().deleteOnExit();
        Files.writeString(credFile, """
                #name\tMdrUrl\tKeycloakUrl\tKeycloakClientId\tKeycloakClientSecret
                dev\t%s\t%s\t%s\t%s
                """.formatted(config.mdrServer(), config.keycloakAuthServer(), config.keycloakClientId(), secret));
        System.setProperty("gor.mdr.credentials", credFile.toString());
        MdrServer.loadMdrServers(config);
    }

    @Test
    public void testStdUrl() {
        Assert.assertEquals("default", MdrServer.extractMdrEnvName(URI.create("mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3")));
        Assert.assertEquals("ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3", MdrServer.extractDocumentId(URI.create("mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3")));
        Assert.assertEquals("default", MdrServer.extractMdrEnvName(URI.create("mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3/test.txt")));
        Assert.assertEquals("ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3", MdrServer.extractDocumentId(URI.create("mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3/test.txt")));
    }

    @Test
    public void tesServerUrl() {
        Assert.assertEquals("dev", MdrServer.extractMdrEnvName(URI.create("mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3?env=dev")));
        Assert.assertEquals("ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3", MdrServer.extractDocumentId(URI.create("mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3?env=dev")));
        Assert.assertEquals("dev", MdrServer.extractMdrEnvName(URI.create("mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3/test.txt?env=dev")));
        Assert.assertEquals("ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3", MdrServer.extractDocumentId(URI.create("mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3/test.txt?env=dev")));
    }

    @Test
    public void testReadDocumentStd() {
        var result = TestUtils.runGorPipe("nor mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3 | top 1", true, securityContext).split("\n");

        Assert.assertEquals(2, result.length);
        Assert.assertEquals("ChromNOR\tPosNOR\tcol1", result[0]);
        Assert.assertEquals("chrN\t0\tVARIANT CALLER SUMMARY,,Number of samples,1", result[1]);
    }

    @Test
    public void testReadDocumentServer() {
        var result = TestUtils.runGorPipe("nor mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3?env=dev | top 1", true, securityContext).split("\n");

        Assert.assertEquals(2, result.length);
        Assert.assertEquals("ChromNOR\tPosNOR\tcol1", result[0]);
        Assert.assertEquals("chrN\t0\tVARIANT CALLER SUMMARY,,Number of samples,1", result[1]);
    }

    @Test
    public void testReadDocumentThroughLinkFile() throws IOException {

        Path tempFile = Files.createTempFile("document_", ".nor.link");
        Files.write(tempFile, "mdr://ff8e31e0-a9ae-41eb-bb8d-21854a47d8b3".getBytes());

        var result = TestUtils.runGorPipe("nor " + tempFile + " | top 1", true, securityContext).split("\n");

        Assert.assertEquals(2, result.length);
        Assert.assertEquals("ChromNOR\tPosNOR\tcol1", result[0]);
        Assert.assertEquals("chrN\t0\tVARIANT CALLER SUMMARY,,Number of samples,1", result[1]);
    }

    @Test
    public void testNonExistingDocumentId() {
        var ex = Assert.assertThrows(GorResourceException.class, () -> {
            TestUtils.runGorPipe("gor mdr://f3658d3a-5220-4094-b7b1-311804df3db8", true, securityContext);
        });

        Assert.assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    @Ignore("Need find new test documents.")
    public void testReadDocuments() {
        var result = TestUtils.runGorPipe("gor mdr://2806e2ec-30f0-41f1-bdf1-ce3b2def078a mdr://191b3d28-4db9-4aa3-aa6b-cbb2968885a5 mdr://ee4d7e36-e6dc-42d8-8f78-714242a8cf6d | top 10000", true, securityContext).split("\n");

        Assert.assertEquals(10001, result.length);
        Assert.assertEquals(10001, result.length);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\n", result[0]);
        Assert.assertEquals("chr1\t11868\t14412\tDDX11L1\n", result[1]);
        Assert.assertEquals("chr1\t35519068\t35524872\tRP11-248I9.2\n", result[3000]);
    }

    @Test
    @Ignore
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
            TestUtils.runGorPipe(query, true, securityContext);
        });

        Assert.assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    @Ignore
    public void test1000EntryDictionary() {
        var result = TestUtils.runGorPipe("gor ../tests/data/mdr/genes_mdr_1000.gord | top 10000", true, securityContext).split("\n");
        Assert.assertEquals(10001, result.length);
    }
}
