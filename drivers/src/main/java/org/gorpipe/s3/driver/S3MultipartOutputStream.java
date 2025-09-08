package org.gorpipe.s3.driver;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class S3MultipartOutputStream extends OutputStream {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(S3MultipartOutputStream.class);

    private static final int PART_SIZE = 5 * 1024 * 1024; // 5 MiB
    private static final int MAX_RETRIES = 3;
    private static final int MAX_CONCURRENT_UPLOADS = 4;
    private static final int RETRY_SLEEP_BASE_MS = 1000;

    private final String bucket;
    private final String key;
    private final List<CompletedPart> completedParts = new ArrayList<>();
    private final ByteBuffer buffer = ByteBuffer.allocate(PART_SIZE);
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_UPLOADS);

    private String uploadId;
    private int partNumber = 1;
    private boolean closed = false;

    public S3MultipartOutputStream(String bucket, String key) throws IOException {
        this.bucket = bucket;
        this.key = key;
    }

    abstract protected CreateMultipartUploadResponse sendCreateMultipartUploadRequest(CreateMultipartUploadRequest req) throws ExecutionException, InterruptedException;
    abstract protected UploadPartResponse sendUploadPartRequest(UploadPartRequest req, byte[] data) throws Exception;
    abstract protected CompleteMultipartUploadResponse sendCompleteMultipartUploadRequest(CompleteMultipartUploadRequest req) throws Exception;
    abstract protected AbortMultipartUploadResponse sendAbortMultipartUploadRequest(AbortMultipartUploadRequest req);


    private String initiateMultipartUpload() throws IOException {
        CreateMultipartUploadRequest req = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        try {
            return sendCreateMultipartUploadRequest(req).uploadId();
        } catch (Exception e) {
            throw new IOException("Failed to initiate multipart upload", e);
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (!buffer.hasRemaining()) {
            uploadPartAsync();
        }
        buffer.put((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int remaining = len;
        int offset = off;
        while (remaining > 0) {
            int toWrite = Math.min(buffer.remaining(), remaining);
            buffer.put(b, offset, toWrite);
            offset += toWrite;
            remaining -= toWrite;
            if (!buffer.hasRemaining()) {
                uploadPartAsync();
            }
        }
    }

    private void uploadPartAsync() throws IOException {
        buffer.flip();
        byte[] partData = new byte[buffer.limit()];
        buffer.get(partData);
        buffer.clear();

        int currentPart = partNumber++;

        if (uploadId == null) {
            this.uploadId = initiateMultipartUpload();
        }

        CompletableFuture<CompletedPart> future = CompletableFuture.supplyAsync(() -> {
            try {
                return uploadWithRetry(currentPart, partData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor);
        try {
            completedParts.add(future.get());
        } catch (Exception e) {
            abortMultipartUpload();
            throw new IOException("Failed to upload part", e);
        }
    }

    private CompletedPart uploadWithRetry(int partNum, byte[] data) throws IOException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                UploadPartRequest req = UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNum)
                        .contentLength((long) data.length)
                        .build();
                UploadPartResponse resp = sendUploadPartRequest(req, data);
                return CompletedPart.builder()
                        .partNumber(partNum)
                        .eTag(resp.eTag())
                        .build();
            } catch (Exception e) {
                if (attempt == MAX_RETRIES) throw new IOException(e);
                try {
                    Thread.sleep(RETRY_SLEEP_BASE_MS * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new IOException("Failed to upload part after retries");
    }

    @Override
    public void flush() throws IOException {
        // No-op: parts are uploaded when buffer is full or on close
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        if (buffer.position() > 0) {
            uploadPartAsync();
        }
        CompleteMultipartUploadRequest req = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build();
        try {
            sendCompleteMultipartUploadRequest(req);
        } catch (Exception e) {
            abortMultipartUpload();
            throw new IOException("Failed to complete multipart upload", e);
        } finally {
            executor.shutdown();
            closed = true;
        }
    }

    private void abortMultipartUpload() {
        try {
            AbortMultipartUploadRequest req = AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .build();
            sendAbortMultipartUploadRequest(req);
        } catch (Exception ignored) {
            logger.warn("Failed to abort multipart upload (ignoring exception)", ignored);
        }
    }
}