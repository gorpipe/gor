package org.gorpipe.base.streams;

import org.gorpipe.exceptions.GorRetryException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

public class UTestFullRetryOutputStream {

    @Test
    public void testWritingWithoutError() throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStream out = new FullRetryOutputStream(outputStream)) {
            byte[] data = "This should work".getBytes();
            out.write(data);
        }
        String result = outputStream.toString();
        assertEquals("This should work", result.trim());
    }

    @Test
    public void testWritingWithError() throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream() {
            @Override
            public void write(byte[] bd, int off, int size) {
                super.write(bd, off, size);
                throw new RuntimeException("Write error");
            }
        };
        try (OutputStream out = new FullRetryOutputStream(outputStream)) {
            out.write( "Some data".getBytes());
            Assert.fail("Should throw exception");
        } catch (GorRetryException e) {
            Assert.assertTrue(e.isFullRetry());
        }
    }
}
