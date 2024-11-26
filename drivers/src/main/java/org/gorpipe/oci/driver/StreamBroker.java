package org.gorpipe.oci.driver;

import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple alternative to JDK {@link java.io.PipedInputStream}; queue input stream provides what's written in queue
 * output stream.
 *
 * </p>
 *
 */
public class StreamBroker {

    /**
     * Internal buffer, i.e. byte array with position of last used byte
     *
     * NOTE: Making this class static hurts performance.
     */
    public class Buffer {
        byte[] buf;
        int end;
        int pos;
        boolean custom = false;

        Buffer(int size) {
            buf = new byte[size];
            reset();
        }

        Buffer(byte[] bytes) {
            buf = bytes;
            end = bytes.length;
            pos = 0;
            custom = true;
        }

        public void reset() {
            end = 0;
            pos = 0;
        }
    }

    public class BrokerInputStream extends InputStream {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BrokerInputStream.class);
        private Future future;
        private Buffer buffer = null;

        /**
         * Reads and returns a single byte.
         *
         * @return either the byte read or {@code -1} if the end of the stream has been reached
         */
        @Override
        public int read() {
            ensureBuffer();
            if (buffer == null) return -1;
            return ((0xFF) & buffer.buf[buffer.pos++]);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ensureBuffer();
            if (buffer == null) return -1;
            int count = Math.min(len, buffer.end - buffer.pos);
            System.arraycopy(buffer.buf, buffer.pos, b, off, count);
            buffer.pos += count;
            return count;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = 0;
            while (skipped < n) {
                ensureBuffer();
                if (buffer == null) return -1;

                long count = Math.min(n - skipped, (buffer.end - buffer.pos));
                buffer.pos += (int)count;
                skipped += count;
            }
            return skipped;
        }

        @Override
        public int available() {
            return StreamBroker.this.available();
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (future != null) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GorSystemException(e);
                } catch (ExecutionException e) {
                    throw new GorResourceException("Writing ", "Unknown stream", e.getCause());
                }
            }
        }

        /**
         * Sets the future the determines if the other end of the stream is success
         *
         * @param future future to be waited for
         */
        public void setFuture(Future future) {
            this.future = future;
        }

        /**
         * Ensures that the buffer is not null and has at least one byte available to read.
         * If there is no more data, return null.
         */
        private void ensureBuffer() {
            if (buffer == null || buffer.end == buffer.pos) {
                if (buffer != null) {
                    StreamBroker.this.returnBuffer(buffer);
                }
                buffer = StreamBroker.this.takeBuffer();
            }
        }
    }

    public class BrokerOutputStream extends OutputStream {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BrokerOutputStream.class);

        private Future<PutObjectResponse> future;
        private Buffer buffer = null;

        public BrokerOutputStream() {
            expected.set(1);
        }

        @Override
        public void write(final int b) {
            ensureBuffer();
            buffer.buf[buffer.end++] = (byte) b;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            while (len > 0) {
                ensureBuffer();
                var count = Math.min(len, buffer.buf.length - buffer.end);
                System.arraycopy(b, off, buffer.buf, buffer.end, count);
                len -= count;
                buffer.end += count;
            }
        }

        @Override
        public void flush() throws IOException {
            if (buffer != null) {
                putBuffer(buffer);
                buffer = null;
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            flush();
            expected.set(0);
            if (future != null) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GorSystemException(e);
                } catch (ExecutionException e) {
                    throw new GorResourceException("Writing ", "Unknown stream", e.getCause());
                }
            }
        }

        /**
         * Sets the future the determines if the other end of the stream is success
         *
         * @param future future to be waited for
         */
        public void setFuture(Future<PutObjectResponse> future) {
            this.future = future;
        }

        /**
         * Ensures that the buffer is not null and has space for at least one byte.
         */
        private void ensureBuffer() {
            if (buffer == null || buffer.end == buffer.buf.length) {
                if (buffer != null) {
                    putBuffer(buffer);
                }
                buffer = getEmtpyBuffer();
            }
        }
    }

    // Params for the custom buffering for gor lines.
    private static final int BUFFER_SIZE = 8 * 1024;
    private static final int BUFFER_COUNT = 2;
    final ArrayBlockingQueue<Buffer> bufferQueue = new ArrayBlockingQueue<>(BUFFER_COUNT); // Buffers available for read thread

    private final BlockingQueue<Buffer> blockingQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger expected = new AtomicInteger();

    private final BrokerInputStream inputStream = new BrokerInputStream();
    private final BrokerOutputStream outputStream = new BrokerOutputStream();

    public StreamBroker() {
        for (int i = 0; i < BUFFER_COUNT; i++) {
            bufferQueue.add(new Buffer(BUFFER_SIZE));
        }
    }

    public BrokerInputStream getInputStream() {
        return inputStream;
    }

    public BrokerOutputStream getOutputStream() {
        return outputStream;
    }

    private int available() {
        return Math.max(expected.get(), blockingQueue.size());
    }

    private Buffer getEmtpyBuffer() {
        try {
            return bufferQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException(e);
        }
    }

    private void putBuffer(Buffer buffer) {
        try {
            blockingQueue.put(buffer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException(e);
        }
    }

    private Buffer takeBuffer() {
        Buffer buff = null;
        while (buff == null && available() > 0) {
            try {
                buff = blockingQueue.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GorSystemException(e);
            }
        }
        return buff;
    }

    private void returnBuffer(Buffer buffer) {
        buffer.reset();
        try {
            bufferQueue.put(buffer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException(e);
        }
    }
}

