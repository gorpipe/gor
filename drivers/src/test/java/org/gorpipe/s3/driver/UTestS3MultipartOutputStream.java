package org.gorpipe.s3.driver;

import org.junit.Test;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class UTestS3MultipartOutputStream {

    /**
     * A test-only concrete implementation that captures uploaded parts in memory.
     */
    static class TestMultipartOutputStream extends S3MultipartOutputStream {
        final List<byte[]> uploadedParts = new ArrayList<>();
        final ByteArrayOutputStream allBytes = new ByteArrayOutputStream();
        boolean uploadAborted = false;
        boolean uploadCompleted = false;

        TestMultipartOutputStream(String bucket, String key) {
            super(bucket, key);
        }

        TestMultipartOutputStream(String bucket, String key, int partSize) {
            super(bucket, key);
            this.currentPartSize = partSize;
            try {
                Field bufferField = S3MultipartOutputStream.class.getDeclaredField("buffer");
                bufferField.setAccessible(true);
                bufferField.set(this, ByteBuffer.allocate(partSize));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected CreateMultipartUploadResponse sendCreateMultipartUploadRequest(CreateMultipartUploadRequest req) {
            return CreateMultipartUploadResponse.builder()
                    .uploadId("test-upload-id")
                    .build();
        }

        @Override
        protected UploadPartResponse sendUploadPartRequest(UploadPartRequest req, byte[] data) throws Exception {
            uploadedParts.add(data.clone());
            allBytes.write(data, 0, data.length);
            return UploadPartResponse.builder()
                    .eTag("etag-" + req.partNumber())
                    .build();
        }

        @Override
        protected CompleteMultipartUploadResponse sendCompleteMultipartUploadRequest(CompleteMultipartUploadRequest req) {
            uploadCompleted = true;
            return CompleteMultipartUploadResponse.builder().build();
        }

        @Override
        protected AbortMultipartUploadResponse sendAbortMultipartUploadRequest(AbortMultipartUploadRequest req) {
            uploadAborted = true;
            return AbortMultipartUploadResponse.builder().build();
        }
    }

    /**
     * Subclass that fails uploads to test abort behavior.
     */
    static class FailingMultipartOutputStream extends TestMultipartOutputStream {
        FailingMultipartOutputStream(String bucket, String key) throws IOException {
            super(bucket, key);
        }

        @Override
        protected UploadPartResponse sendUploadPartRequest(UploadPartRequest req, byte[] data) throws Exception {
            throw new RuntimeException("Simulated upload failure");
        }
    }

    @Test
    public void testSmallWrite() throws IOException {
        try (TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key")) {
            byte[] data = new byte[100];
            for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 127);
            out.write(data);
            assertEquals(0, out.getUploadPartStarted());
            assertEquals(0, out.getUploadPartDone());
        }
    }

    @Test
    public void testWriteExactlyOnePartSize() throws IOException {
        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key");
        byte[] data = new byte[S3MultipartOutputStream.INIT_PART_SIZE];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 256);
        out.write(data);
        out.close();

        assertEquals(1, out.getUploadPartDone());
        assertEquals(1, out.getUploadPartStarted());
        assertEquals(S3MultipartOutputStream.INIT_PART_SIZE, out.uploadedParts.get(0).length);
        assertTrue(out.uploadCompleted);
    }

    @Test
    public void testWriteMultipleParts() throws IOException {
        int partSize = S3MultipartOutputStream.INIT_PART_SIZE;
        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key");

        // Write 2.5 parts worth of data
        int totalSize = partSize * 2 + partSize / 2;
        byte[] data = new byte[totalSize];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 256);
        out.write(data);
        out.close();

        // Should produce 3 parts: 2 full + 1 partial
        assertEquals(3, out.getUploadPartDone());
        assertEquals(3, out.getUploadPartStarted());
        assertEquals(partSize, out.uploadedParts.get(0).length);
        assertEquals(partSize, out.uploadedParts.get(1).length);
        assertEquals(partSize / 2, out.uploadedParts.get(2).length);
        assertTrue(out.uploadCompleted);
    }

    @Test
    public void testDataIntegrity() throws IOException {
        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key");

        int totalSize = S3MultipartOutputStream.INIT_PART_SIZE * 3 + 1000;
        byte[] data = new byte[totalSize];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 256);
        out.write(data);
        out.close();

        byte[] result = out.allBytes.toByteArray();
        assertEquals(totalSize, result.length);
        assertArrayEquals(data, result);
    }

    @Test
    public void testSingleByteWrite() throws IOException {
        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key");
        out.write(42);
        out.close();

        assertEquals(1, out.getUploadPartDone());
        assertEquals(1, out.uploadedParts.get(0).length);
        assertEquals(42, out.uploadedParts.get(0)[0]);
        assertTrue(out.uploadCompleted);
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key");
        out.write(new byte[]{1, 2, 3});
        out.close();
        int partsAfterFirstClose = out.getUploadPartDone();
        out.close(); // should be no-op
        assertEquals(partsAfterFirstClose, out.getUploadPartDone());
    }

    @Test
    public void testFailedUploadAbortsMultipart() throws IOException {
        FailingMultipartOutputStream out = new FailingMultipartOutputStream("bucket", "key");
        byte[] data = new byte[S3MultipartOutputStream.INIT_PART_SIZE + 1]; // enough to trigger a part upload

        try {
            out.write(data);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Failed to upload part"));
        }
        assertTrue(out.uploadAborted);
    }

    @Test
    public void testInitialPartSize() throws IOException {
        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key");
        assertEquals(S3MultipartOutputStream.INIT_PART_SIZE, out.getCurrentPartSize());
        assertTrue(out.getCurrentPartSize() >= S3MultipartOutputStream.MIN_S3_PART_SIZE);
        out.close();
    }

    @Test
    public void testAdaptPartSize() throws Exception {
        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key");

        Method adaptMethod = S3MultipartOutputStream.class.getDeclaredMethod("adaptPartSize");
        adaptMethod.setAccessible(true);
        Field partNumberField = S3MultipartOutputStream.class.getDeclaredField("partNumber");
        partNumberField.setAccessible(true);

        int initialPartSize = out.getCurrentPartSize();

        // Set partNumber to 2500 (first trigger point)
        partNumberField.set(out, 2500);
        adaptMethod.invoke(out);
        assertEquals("Part size should quadruple at 2500 parts", initialPartSize * 4, out.getCurrentPartSize());

        // Set partNumber to 5000 (second trigger point)
        partNumberField.set(out, 5000);
        adaptMethod.invoke(out);
        assertEquals("Part size should quadruple again at 5000 parts", initialPartSize * 16, out.getCurrentPartSize());

        // Set partNumber to 7500 (third trigger point)
        partNumberField.set(out, 7500);
        adaptMethod.invoke(out);
        assertEquals("Part size should quadruple again at 7500 parts", initialPartSize * 64, out.getCurrentPartSize());

        out.close();
    }

    @Test
    public void testAdaptPartSizeDoesNotTriggerBetweenBoundaries() throws Exception {
        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key");

        Method adaptMethod = S3MultipartOutputStream.class.getDeclaredMethod("adaptPartSize");
        adaptMethod.setAccessible(true);
        Field partNumberField = S3MultipartOutputStream.class.getDeclaredField("partNumber");
        partNumberField.setAccessible(true);

        int initialPartSize = out.getCurrentPartSize();

        // Part numbers that should NOT trigger adaptation
        for (int pn : new int[]{1, 100, 1000, 2499, 2501, 3000}) {
            partNumberField.set(out, pn);
            adaptMethod.invoke(out);
            assertEquals("Part size should not change at partNumber " + pn, initialPartSize, out.getCurrentPartSize());
        }

        out.close();
    }

    @Test
    public void testAdaptPartSizeDoesNotTriggerAtOrAboveMaxParts() throws Exception {
        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key");

        Method adaptMethod = S3MultipartOutputStream.class.getDeclaredMethod("adaptPartSize");
        adaptMethod.setAccessible(true);
        Field partNumberField = S3MultipartOutputStream.class.getDeclaredField("partNumber");
        partNumberField.setAccessible(true);

        int initialPartSize = out.getCurrentPartSize();

        // At MAX_PARTS (10000), even though 10000 % 2500 == 0, should not trigger because partNumber >= MAX_PARTS
        partNumberField.set(out, S3MultipartOutputStream.MAX_PARTS);
        adaptMethod.invoke(out);
        assertEquals("Part size should not change at MAX_PARTS", initialPartSize, out.getCurrentPartSize());

        out.close();
    }

    @Test
    public void testLargeStreamWithAdaptivePartSizeMd5Integrity() throws Exception {
        int smallPartSize = 256; // 256 bytes

        TestMultipartOutputStream out = new TestMultipartOutputStream("bucket", "key", smallPartSize);

        // With 256-byte parts and adaptive sizing:
        //   Parts 1-2500:    256 bytes each  (adapt fires at partNumber 2500, data already extracted)
        //   Parts 2501-5000: 1KB each        (after first quadruple)
        //   Parts 5001-6000: 4KB each        (after second quadruple)
        // Total = 2500*256 + 2500*1024 + 1000*4096 = 7,296,000 bytes
        int totalBytes = 2500 * smallPartSize + 2500 * (smallPartSize * 4) + 1000 * (smallPartSize * 16);

        Random rng = new Random(42); // fixed seed for reproducibility
        MessageDigest inputMd5 = MessageDigest.getInstance("MD5");

        // Stream random data in variable-sized chunks
        int written = 0;
        byte[] chunk = new byte[8192];
        while (written < totalBytes) {
            int toWrite = Math.min(chunk.length, totalBytes - written);
            rng.nextBytes(chunk);
            inputMd5.update(chunk, 0, toWrite);
            out.write(chunk, 0, toWrite);
            written += toWrite;
        }
        out.close();

        assertEquals(6000, out.getUploadPartDone());
        assertTrue(out.uploadCompleted);

        // Verify adaptive sizing kicked in
        assertEquals(smallPartSize * 16, out.getCurrentPartSize());

        // Compare MD5 of input vs output
        byte[] inputDigest = inputMd5.digest();
        MessageDigest outputMd5 = MessageDigest.getInstance("MD5");
        outputMd5.update(out.allBytes.toByteArray());
        byte[] outputDigest = outputMd5.digest();

        assertArrayEquals("MD5 of input and output must match", inputDigest, outputDigest);
    }
}
