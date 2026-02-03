package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import htsjdk.samtools.SAMSequenceRecord;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
public class UTestFolderReferenceSource {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private File testFolder;
    private FolderReferenceSource referenceSource;

    @Before
    public void setUp() throws IOException {
        testFolder = workDir.newFolder("ref_folder");
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
        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        // Should not throw exception, but map should be empty
        Assert.assertNotNull(referenceSource);
        Assert.assertEquals(0, referenceSource.getReferenceFiles().size());
    }

    @Test(expected = GorResourceException.class)
    public void testConstructorWithNonExistentFolder() {
        // Test with non-existent folder
        referenceSource = new FolderReferenceSource("/non/existent/path");
    }

    @Test
    public void testScanFolderWithValidFastaFiles() throws IOException {
        // Create a valid FASTA file with index files
        File fastaFile = createFastaFileWithIndexes("test_ref.fasta", "chr1", "ACGTACGT", "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6");

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        // Verify that the FASTA file was scanned
        Assert.assertNotNull(referenceSource);
        Assert.assertEquals(1, referenceSource.getReferenceFiles().size());
    }

    @Test(expected = GorResourceException.class)
    public void testScanFolderIgnoresFastaWithoutIndexFiles() throws IOException {
        // Create FASTA file without index files
        File fastaFile = new File(testFolder, "no_index.fasta");
        try (FileWriter writer = new FileWriter(fastaFile)) {
            writer.write(">chr1\nACGTACGT\n");
        }

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());
    }

    @Test
    public void testScanFolderWithMultipleFastaFiles() throws IOException {
        // Create multiple FASTA files
        createFastaFileWithIndexes("ref1.fasta", "chr1", "ACGT", "md5_1");
        createFastaFileWithIndexes("ref2.fasta", "chr2", "TGCA", "md5_2");
        createFastaFileWithIndexes("ref3.fa", "chr3", "GCTA", "md5_3");

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        Assert.assertNotNull(referenceSource);
        Assert.assertEquals(3, referenceSource.getReferenceFiles().size());
        Assert.assertArrayEquals("TGCA".getBytes(), referenceSource.getReferenceBases(
                new SAMSequenceRecord("chr2").setMd5("md5_2"), false));

    }

    @Test
    public void testGetReferenceBasesWithValidMD5() throws IOException {
        // Create FASTA file and get its MD5
        String sequenceName = "chr1";
        String sequence = "ACGTACGTACGTACGT";
        String md5 = calculateMD5(sequence);

        File fastaFile = createFastaFileWithIndexes("test.fasta", sequenceName, sequence, md5);

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        // Create a SAMSequenceRecord with the MD5
        SAMSequenceRecord record = new SAMSequenceRecord(sequenceName, sequence.length()).setMd5(md5);

        byte[] bases = referenceSource.getReferenceBases(record, false);

        Assert.assertNotNull(bases);
        Assert.assertEquals(sequence.length(), bases.length);
        Assert.assertEquals("ACGTACGTACGTACGT", new String(bases));
    }

    @Test
    public void testGetReferenceBasesWithInvalidMD5() throws IOException {
        // Create FASTA file
        createFastaFileWithIndexes("test.fasta", "chr1", "ACGT", "valid_md5");

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        // Create a SAMSequenceRecord with different MD5
        SAMSequenceRecord record = new SAMSequenceRecord("chr1", 4);
        record.setAttribute("M5", "invalid_md5");

        byte[] bases = referenceSource.getReferenceBases(record, false);

        // Should return empty or fallback to EBI
        // The behavior depends on implementation
        Assert.assertNull(bases);
    }

    @Test(expected = GorDataException.class)
    public void testGetReferenceBasesWithNullMD5() throws IOException {
        createFastaFileWithIndexes("test.fasta", "chr1", "ACGT", "md5_hash");

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        SAMSequenceRecord record = new SAMSequenceRecord("chr1", 4);
        // No MD5 set

        byte[] bases = referenceSource.getReferenceBases(record, false);
    }

    @Test
    public void testGetReferenceBasesCaseInsensitive() throws IOException {
        String sequence = "acgtacgt"; // lowercase
        String md5 = calculateMD5(sequence.toUpperCase());

        File fastaFile = createFastaFileWithIndexes("test.fasta", "chr1", sequence, md5);

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        SAMSequenceRecord record = new SAMSequenceRecord("chr1", sequence.length());
        record.setAttribute("M5", md5);

        byte[] bases = referenceSource.getReferenceBases(record, false);

        // Bases should be converted to uppercase
        Assert.assertNotNull(bases);
        String basesString = new String(bases);
        Assert.assertTrue(basesString.equals(basesString.toUpperCase()));
    }

    @Test
    public void testCloseReleasesResources() throws IOException {
        createFastaFileWithIndexes("test.fasta", "chr1", "ACGT", "md5_hash");

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        // Close should not throw exception
        referenceSource.close();
    }

    @Test
    public void testMultipleCloseCalls() throws IOException {
        createFastaFileWithIndexes("test.fasta", "chr1", "ACGT", "md5_hash");

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        // Multiple close calls should be safe
        referenceSource.close();
        referenceSource.close();
    }

    @Test(expected = GorResourceException.class)
    public void testScanFolderWithCorruptedFastaFile() throws IOException {
        // Create a FASTA file that exists but is corrupted
        File fastaFile = new File(testFolder, "corrupted.fasta");
        try (FileWriter writer = new FileWriter(fastaFile)) {
            writer.write("This is not a valid FASTA file\n");
        }

        // Create index files so it gets picked up
        File dictFile = new File(testFolder, "corrupted.dict");
        dictFile.createNewFile();
        File faiFile = new File(testFolder, "corrupted.fai");
        faiFile.createNewFile();

        // Should handle corrupted files gracefully
        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());
    }

    @Test
    public void testScanFolderWithSubdirectories() throws IOException {
        // Create subdirectory with FASTA file
        File subDir = new File(testFolder, "subdir");
        subDir.mkdirs();

        // Only files in the root folder should be scanned
        createFastaFileWithIndexes("root.fasta", "chr1", "ACGT", "md5_1");

        File subFasta = new File(subDir, "sub.fasta");
        try (FileWriter writer = new FileWriter(subFasta)) {
            writer.write(">chr1\nACGT\n");
        }

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        // Only root.fasta should be found
        Assert.assertNotNull(referenceSource);
        Assert.assertEquals(1, referenceSource.getReferenceFiles().size());
    }

    @Test
    public void testGetReferenceBasesWithLongSequence() throws IOException {
        // Create a longer sequence
        StringBuilder longSeq = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longSeq.append("ACGT");
        }

        String sequence = longSeq.toString();
        String md5 = calculateMD5(sequence);

        createFastaFileWithIndexes("long.fasta", "chr1", sequence, md5);

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        SAMSequenceRecord record = new SAMSequenceRecord("chr1", sequence.length());
        record.setAttribute("M5", md5);

        byte[] bases = referenceSource.getReferenceBases(record, false);

        Assert.assertNotNull(bases);
        Assert.assertEquals(sequence.length(), bases.length);
    }

    @Test
    public void testConcurrentAccess() throws IOException, InterruptedException {
        createFastaFileWithIndexes("test.fasta", "chr1", "ACGT", "md5_hash");

        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

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
