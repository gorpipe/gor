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
import java.util.Set;
import java.util.UUID;

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

    @Test
    public void testScanFolderIgnoresFastaWithoutIndexFiles() throws IOException {
        // Create FASTA file without index files
        File fastaFile = new File(testFolder, "no_index.fasta");
        try (FileWriter writer = new FileWriter(fastaFile)) {
            writer.write(">chr1\nACGTACGT\n");
        }
        String md5 = "engknow3778_" + UUID.randomUUID();
        createFastaFileWithIndexes("good.fasta", "chr2", "TGCA", md5);

        // Bad file is skipped (and logged), other files are still loaded.
        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        Assert.assertEquals(Set.of(new File(testFolder, "good.fasta").toPath()), referenceSource.getReferenceFiles());
        Assert.assertArrayEquals("TGCA".getBytes(), referenceSource.getReferenceBases(
                new SAMSequenceRecord("chr2", 4).setMd5(md5), false));
        Assert.assertTrue(referenceSource.containsReferenceFile(new File(testFolder, "good.fasta").toPath()));
        Assert.assertFalse(referenceSource.containsReferenceFile(fastaFile.toPath()));
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
                new SAMSequenceRecord("chr2", SAMSequenceRecord.UNKNOWN_SEQUENCE_LENGTH).setMd5("md5_2"), false));

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

    @Test
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
        String md5 = "engknow3778_" + UUID.randomUUID();
        createFastaFileWithIndexes("good.fasta", "chr2", "TGCA", md5);

        // Bad file is skipped (and logged), other files are still loaded.
        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        Assert.assertEquals(Set.of(new File(testFolder, "good.fasta").toPath()), referenceSource.getReferenceFiles());
        Assert.assertArrayEquals("TGCA".getBytes(), referenceSource.getReferenceBases(
                new SAMSequenceRecord("chr2", 4).setMd5(md5), false));
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

    @Test
    public void testCreatingAnotherSourceDoesNotBreakExistingSource() throws IOException {
        // ENGKNOW-3778: Opening a new reference source (e.g. by another CRAM iterator) must not affect
        // lookups in already open reference sources.
        String md5 = "engknow3778_" + UUID.randomUUID();
        createFastaFileWithIndexes("test.fasta", "chr3", "ACGTACGT", md5);
        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());

        File otherFolder = workDir.newFolder("other_ref_folder");
        createFastaFileWithIndexes(otherFolder, "other.fasta", "chr1", "TTTT", "engknow3778_" + UUID.randomUUID());
        try (FolderReferenceSource otherSource = new FolderReferenceSource(otherFolder.getAbsolutePath())) {
            SAMSequenceRecord record = new SAMSequenceRecord("chr3", 8).setMd5(md5);
            Assert.assertArrayEquals("GTAC".getBytes(), referenceSource.getReferenceBasesByRegion(record, 2, 4));
        }
    }

    @Test
    public void testFolderIndexIsSharedAndRescannedWhenStale() throws IOException {
        createFastaFileWithIndexes("ref1.fasta", "chr1", "ACGT", "engknow3778_" + UUID.randomUUID());
        referenceSource = new FolderReferenceSource(testFolder.getAbsolutePath());
        Assert.assertEquals(1, referenceSource.getReferenceFiles().size());

        // New file is not seen by new sources until the shared index is stale.
        String md5 = "engknow3778_" + UUID.randomUUID();
        createFastaFileWithIndexes("ref2.fasta", "chr2", "TGCA", md5);
        SAMSequenceRecord record = new SAMSequenceRecord("chr2", 4).setMd5(md5);
        try (FolderReferenceSource source = new FolderReferenceSource(testFolder.getAbsolutePath())) {
            Assert.assertEquals(1, source.getReferenceFiles().size());
            Assert.assertNull(source.getReferenceBases(record, false));
        }

        System.setProperty(Md5FolderIndex.KEY_RESCAN_INTERVAL, "0");
        try (FolderReferenceSource source = new FolderReferenceSource(testFolder.getAbsolutePath())) {
            Assert.assertEquals(2, source.getReferenceFiles().size());
            Assert.assertArrayEquals("TGCA".getBytes(), source.getReferenceBases(record, false));
            // Existing sources share the index.
            Assert.assertEquals(2, referenceSource.getReferenceFiles().size());
        } finally {
            System.clearProperty(Md5FolderIndex.KEY_RESCAN_INTERVAL);
        }
    }

    /**
     * Helper method to create a FASTA file with corresponding .dict and .fai files.
     * Note: This is a simplified version. In a real implementation, you would need
     * to create proper FASTA index files using samtools or similar tools.
     */
    private File createFastaFileWithIndexes(String fileName, String sequenceName,
                                            String sequence, String md5) throws IOException {
        return createFastaFileWithIndexes(testFolder, fileName, sequenceName, sequence, md5);
    }

    private File createFastaFileWithIndexes(File folder, String fileName, String sequenceName,
                                            String sequence, String md5) throws IOException {
        // Create FASTA file
        File fastaFile = new File(folder, fileName);
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
        File dictFile = new File(folder, baseName + ".dict");
        try (FileWriter writer = new FileWriter(dictFile)) {
            writer.write("@HD\tVN:1.0\n");
            writer.write("@SQ\tSN:" + sequenceName + "\tLN:" + sequence.length());
            if (md5 != null && !md5.isEmpty()) {
                writer.write("\tM5:" + md5);
            }
            writer.write("\n");
        }

        // Create .fai file (FASTA index - simplified)
        File faiFile = new File(folder, fileName + ".fai");
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
