package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Arrays;

import static org.gorpipe.gor.driver.utils.TestUtils.md5;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by villi on 25/08/15.
 */
public class UTestStreamUtils {
    class TestableClosable implements Closeable {
        boolean closeCalled = false;

        @Override
        public void close() throws IOException {
            closeCalled = true;
            throw new IOException("I'm bad");
        }
    }

    private static File emptyFile;
    private static File lines1000File;
    private static File lines10000File;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpFiles() throws IOException {
        emptyFile = FileTestUtils.createEmptyFile(workDir.getRoot());
        lines1000File = FileTestUtils.createLinesFile(workDir.getRoot(), 1000);
        lines10000File = FileTestUtils.createLinesFile(workDir.getRoot(), 10000);
    }

    @Test
    public void testTryClose() {
        TestableClosable closable = new TestableClosable();
        assertTrue(!closable.closeCalled);

        StreamUtils.tryClose(closable);
        assertTrue(closable.closeCalled);
    }

    @Test
    public void testReadToBufferEmpty() throws IOException {
        InputStream stream;
        byte[] buf;

        buf = mkBuf(10, 'X');

        stream = new FileInputStream(emptyFile);
        assertEquals(0, StreamUtils.readToBuffer(stream, buf, 0, 0));
        assertEquals("XXXXXXXXXX", new String(buf));
        stream.close();

        stream = new FileInputStream(emptyFile);
        assertEquals(-1, StreamUtils.readToBuffer(stream, buf, 0, 10));
        assertEquals("XXXXXXXXXX", new String(buf));
        stream.close();

        stream = new FileInputStream(emptyFile);
        assertEquals(-1, StreamUtils.readToBuffer(stream, buf, 5, 5));
        assertEquals("XXXXXXXXXX", new String(buf));
        stream.close();
    }

    @Test
    public void testReadToBuffer() throws Exception {
        InputStream stream;
        byte[] buf;

        buf = mkBuf(10, 'X');
        stream = new FileInputStream(lines1000File);
        assertEquals(0, StreamUtils.readToBuffer(stream, buf, 0, 0));
        assertEquals("XXXXXXXXXX", new String(buf));
        stream.close();

        buf = mkBuf(10, 'X');
        stream = new FileInputStream(lines10000File);
        stream.skip(1234 * 5);
        assertEquals(4, StreamUtils.readToBuffer(stream, buf, 0, 4));
        assertEquals("1234XXXXXX", new String(buf));
        stream.close();

        buf = mkBuf(10, 'X');
        stream = new FileInputStream(lines10000File);
        stream.skip(1234 * 5);
        assertEquals(4, StreamUtils.readToBuffer(stream, buf, 1, 4));
        assertEquals("X1234XXXXX", new String(buf));
        stream.close();

        buf = mkBuf(10, 'X');
        stream = new FileInputStream(lines10000File);
        stream.skip(1234 * 5);
        assertEquals(4, StreamUtils.readToBuffer(stream, buf, 5, 4));
        assertEquals("XXXXX1234X", new String(buf));
        stream.close();

        buf = mkBuf(10, 'X');
        stream = new FileInputStream(lines10000File);
        stream.skip(1234 * 5);
        assertEquals(4, StreamUtils.readToBuffer(stream, buf, 6, 4));
        assertEquals("XXXXXX1234", new String(buf));
        stream.close();


        buf = mkBuf(5000, 'X');
        stream = new FileInputStream(lines1000File);
        assertEquals(4000, StreamUtils.readToBuffer(stream, buf, 0, 5000));
        assertEquals("XXXXXXXX", new String(buf, 4000, 8));
        assertEquals(md5(buf, 0, 4000), "c7f1529561a520a21b672b85e6accb18");
        stream.close();
    }

    @Test
    public void testReadRangeToStream() throws Exception {
        InputStream stream;
        OutputStream output;
        RequestRange range;

        // Test reading the first line (4 bytes)
        stream = new FileInputStream(lines1000File);
        output = new ByteArrayOutputStream();
        range = RequestRange.fromFirstLength(0, 4);
        StreamUtils.readRangeToStream(stream, range, output, 128 * 1024);
        stream.close();
        output.close();
        assertEquals(range.getLength(), output.toString().getBytes().length);
        assertEquals("000\n", output.toString());

        // Test a range shorter than buffer size
        stream = new FileInputStream(lines1000File);
        output = new ByteArrayOutputStream();
        range = RequestRange.fromFirstLength(100, 200);
        StreamUtils.readRangeToStream(stream, range, output, 128 * 1024);
        stream.close();
        output.close();
        assertEquals(range.getLength(), output.toString().getBytes().length);
        assertEquals("bc111d90e7890567dab95c4362985b73", md5(output.toString().getBytes()));

        // Test a range longer than buffer size
        stream = new FileInputStream(lines1000File);
        output = new ByteArrayOutputStream();
        range = RequestRange.fromFirstLength(100, 200);
        StreamUtils.readRangeToStream(stream, range, output, 128);
        stream.close();
        output.close();
        assertEquals(range.getLength(), output.toString().getBytes().length);
        assertEquals("bc111d90e7890567dab95c4362985b73", md5(output.toString().getBytes()));

        // Test the full file range
        stream = new FileInputStream(lines1000File);
        output = new ByteArrayOutputStream();
        range = RequestRange.fromFirstLength(0, lines1000File.length());
        StreamUtils.readRangeToStream(stream, range, output, 128 * 1024);
        stream.close();
        output.close();
        assertEquals(range.getLength(), lines1000File.length());
        assertEquals(lines1000File.length(), output.toString().getBytes().length);
        assertEquals("c7f1529561a520a21b672b85e6accb18", md5(output.toString().getBytes()));
    }

    private static byte[] mkBuf(int size, int fill) {
        byte[] buf = new byte[size];
        Arrays.fill(buf, (byte) fill);
        return buf;
    }

    private byte[] readFile(String file) throws IOException {
        File f = new File(file);
        int size = (int) f.length();
        byte[] b = new byte[size];

        FileInputStream fs = new FileInputStream(f);
        assertEquals(size, fs.read(b));
        return b;
    }

}
