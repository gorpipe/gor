package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import htsjdk.samtools.SAMSequenceRecord;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for FolderReferenceSource class.
 * Tests folder scanning, MD5 mapping, and reference base retrieval.
 */
public class UTestEBIReferenceSource {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private File testFolder;
    private EBIReferenceSource referenceSource;

    @Before
    public void setUp() throws IOException {
        testFolder = workDir.newFolder("ref_folder");
        System.setProperty(EBIReferenceSource.KEY_USE_CRAM_REF_DOWNLOAD, "true");
    }

    @After
    public void tearDown() throws IOException {
        if (referenceSource != null) {
            referenceSource.close();
        }
    }

    @Test
    public void testConstructorWithEmptyFolder() throws IOException {
        // Test with empty folder
        referenceSource = new EBIReferenceSource(testFolder.getAbsolutePath());

        // Should not throw exception, but map should be empty
        Assert.assertNotNull(referenceSource);
        Assert.assertEquals(0, referenceSource.getRefbasesFiles().size());
    }

    @Test(expected = GorResourceException.class)
    public void testConstructorWithNonExistentFolder() {
        // Test with non-existent folder
        referenceSource = new EBIReferenceSource("/non/existent/path");
    }

    @Test
    public void testConcurrentAccess() throws IOException, InterruptedException {
        createFastaFileWithIndexes("test.fasta", "chr1", "ACGT", "md5_hash");

        referenceSource = new EBIReferenceSource(testFolder.getAbsolutePath());

        SAMSequenceRecord record = new SAMSequenceRecord("chr1", 4);
        record.setAttribute("M5", "md5_hash");

        // Test concurrent access
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                byte[] bases = referenceSource.getReferenceBases(record, false);
                Assert.assertNotNull(bases);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    public void testLoadFromRefbasesFile() throws Exception {
        String md5 = "refbases_md5";
        byte[] seq = "ACGTREF".getBytes();
        this.createRefbasesFile(md5, seq);

        referenceSource = new EBIReferenceSource(testFolder.getAbsolutePath());

        SAMSequenceRecord record = new SAMSequenceRecord("chr1", seq.length).setMd5(md5);

        byte[] bases = referenceSource.getReferenceBases(record, false);

        Assert.assertNotNull(bases);
        Assert.assertArrayEquals(seq, bases);
        Assert.assertEquals(1, referenceSource.getRefbasesFiles().size());
    }

    @Test
    public void testDownloadFromEBIReal() throws Exception {
        String md5 = "d2ed829b8a1628d16cbeee88e88e39eb"; // hg19 chrM

        referenceSource = new EBIReferenceSource(testFolder.getAbsolutePath());

        SAMSequenceRecord record = new SAMSequenceRecord("chrM").setMd5(md5);

        byte[] bases = referenceSource.getReferenceBases(record, false);

        Assert.assertNotNull(bases);
        Assert.assertEquals(16571, bases.length);

        // The download should have persisted a refbases file
        Assert.assertEquals(1, referenceSource.getRefbasesFiles().size());
        Path stored = referenceSource.getRefbasesFiles().iterator().next();
        Assert.assertEquals("md5_" + md5 + ".txt", stored.getFileName().toString());
        Assert.assertTrue(Files.exists(stored));
        Assert.assertArrayEquals(bases, Files.readAllBytes(stored));
    }

    /*
    @Test
    public void testDownloadFromEBIFake() throws Exception {
        String md5 = "download_md5";
        // Start simple HTTP server that returns "SEQUENCE_<md5>" for path /<md5>
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();
                String req = path.startsWith("/") && path.length() > 1 ? path.substring(1) : "";
                byte[] resp = ("SEQUENCE_" + req).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
                exchange.sendResponseHeaders(200, resp.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            } finally {
                exchange.close();
            }
        });
        server.start();

        // Save and override Defaults for the test
        String oldMask = htsjdk.samtools.Defaults.EBI_REFERENCE_SERVICE_URL_MASK;
        boolean oldUse = htsjdk.samtools.Defaults.USE_CRAM_REF_DOWNLOAD;
        try {
            int port = server.getAddress().getPort();
            htsjdk.samtools.Defaults.EBI_REFERENCE_SERVICE_URL_MASK = "http://localhost:" + port + "/%s";
            htsjdk.samtools.Defaults.USE_CRAM_REF_DOWNLOAD = true;

            referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

            SAMSequenceRecord record = new SAMSequenceRecord("chr1", 100).setMd5(md5);

            byte[] bases = referenceSource.getReferenceBases(record, false);

            Assert.assertNotNull(bases);
            Assert.assertEquals("SEQUENCE_" + md5, new String(bases, java.nio.charset.StandardCharsets.US_ASCII));

            // The download should have persisted a refbases file
            Assert.assertEquals(1, referenceSource.getRefbasesFiles().size());
            Path stored = referenceSource.getRefbasesFiles().iterator().next();
            Assert.assertTrue(Files.exists(stored));
            Assert.assertArrayEquals(bases, Files.readAllBytes(stored));
        } finally {
            // restore defaults and stop server
            htsjdk.samtools.Defaults.EBI_REFERENCE_SERVICE_URL_MASK = oldMask;
            htsjdk.samtools.Defaults.USE_CRAM_REF_DOWNLOAD = oldUse;
            server.stop(0);
        }
    }

     */

    /**
     * Helper method to create a FASTA file with corresponding .dict and .fai files.
     * Note: This is a simplified version. In a real implementation, you would need
     * to create proper FASTA index files using samtools or similar tools.
     */
    private File createFastaFileWithIndexes(String fileName, String sequenceName,
                                            String sequence, String md5) throws IOException {
        // Create FASTA file
        File fastaFile = new File(testFolder, fileName);
        var header = "";
        try (FileWriter writer = new FileWriter(fastaFile)) {
            header = ">" + sequenceName;
            if (md5 != null && !md5.isEmpty()) {
                header += " MD5:" + md5;
            }
            header += "\n";
            writer.write(header);

            // Write sequence in 60 char lines (FASTA convention)
            int lineLength = 60;
            for (int i = 0; i < sequence.length(); i += lineLength) {
                int end = Math.min(i + lineLength, sequence.length());
                writer.write(sequence.substring(i, end));
                writer.write("\n");
            }
        }

        // Create .dict file (simplified - real dict files have specific format)
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        File dictFile = new File(testFolder, baseName + ".dict");
        try (FileWriter writer = new FileWriter(dictFile)) {
            writer.write("@HD\tVN:1.0\n");
            writer.write("@SQ\tSN:" + sequenceName + "\tLN:" + sequence.length());
            if (md5 != null && !md5.isEmpty()) {
                writer.write("\tM5:" + md5);
            }
            writer.write("\n");
        }

        // Create .fai file (FASTA index - simplified)
        File faiFile = new File(testFolder, fileName + ".fai");
        try (FileWriter writer = new FileWriter(faiFile)) {
            // Format: sequence_name, length, offset, linebases, linewidth
            // This is simplified - real .fai files need proper calculation
            writer.write(sequenceName + "\t" + sequence.length() + "\t" + header.length() + "\t60\t61\n");
        }

        return fastaFile;
    }

    private File createRefbasesFile(String md5, byte[] bytes) throws IOException {
        Path refbases = testFolder.toPath().resolve("md5_" + md5 + ".txt");
        Files.write(refbases, bytes);
        return refbases.toFile();
    }

    /**
     * Helper method to calculate MD5 hash of a string.
     * This is a simplified version - in reality, MD5 should be calculated
     * from the actual sequence bytes in the same way htsjdk does it.
     */
    private String calculateMD5(String sequence) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sequence.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "test_md5_hash";
        }
    }
}
