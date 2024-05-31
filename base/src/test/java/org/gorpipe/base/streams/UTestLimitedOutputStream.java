package org.gorpipe.base.streams;

import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

public class UTestLimitedOutputStream {

    @Test
    public void testWritingWithinLimit() throws IOException {
        long maxSize = 20;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = byteArrayOutputStream;
        try (LimitedOutputStream limitedOut = new LimitedOutputStream(outputStream, maxSize)) {
            byte[] data = "This should work".getBytes();
            limitedOut.write(data);
        }
        String result = byteArrayOutputStream.toString();
        assertEquals("This should work", result.trim());
    }

    @Test
    public void testWritingBeyondLimit(){
        long maxSize = 10;
        OutputStream outputStream = new ByteArrayOutputStream();
        var e  = Assert.assertThrows(IllegalStateException.class, () -> {
            LimitedOutputStream limitedOut = new LimitedOutputStream(outputStream, maxSize);
            byte[] data = "Exceeding limit".getBytes();
            limitedOut.write(data);
        });
        Assert.assertTrue(e.getMessage().equals("Output stream limit exceeded: 15 > 10"));
    }
}
