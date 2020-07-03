package org.gorpipe.model.genome.files.gor.bgenreader;

import java.util.Arrays;

public class Utils {
    static long parseUnsignedInt(byte[] buffer, int offset) {
        return (buffer[offset] & 0xffL) | ((buffer[offset + 1] & 0xffL) << 8)
                | ((buffer[offset + 2] & 0xffL) << 16) | ((buffer[offset + 3] & 0xffL) << 24);
    }

    static int parseUnsignedShort(byte[] buffer, int offset) {
        return (buffer[offset] & 0xff) | ((buffer[offset + 1] & 0xff) << 8);
    }

    protected static byte[] ensureCapacity(byte[] array, int len) {
        if (array == null) {
            int newLen = 1;
            while (newLen < len) {
                newLen <<= 1;
            }
            return new byte[newLen];
        } else if (array.length < len) {
            int newLen = Math.max(array.length, 1);
            while (newLen < len) {
                newLen <<= 1;
            }
            return Arrays.copyOf(array, newLen);
        } else {
            return array;
        }
    }
}
