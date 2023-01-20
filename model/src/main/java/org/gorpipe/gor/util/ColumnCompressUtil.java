package org.gorpipe.gor.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ColumnCompressUtil {

    public final static String MAGIC = "zip::";

    public static String deflate(String data, int minimumSize) {
        if (isCompressed(data) || data.length() < minimumSize) {
            return data;
        }

        var d = new Deflater();
        var dataBytes = data.getBytes(StandardCharsets.UTF_8);
        d.setInput(dataBytes);
        d.finish();
        byte output[] = new byte[dataBytes.length + 100];
        var byteCount = d.deflate(output);

        return MAGIC + dataBytes.length + "::" + Base64.getEncoder().encodeToString(Arrays.copyOfRange(output, 0, byteCount));
    }

    public static String inflate(String data) throws DataFormatException {
        if (isCompressed(data)) {
            var first = data.indexOf("::");
            var second = data.indexOf("::", first+1);

            if (second < 0) return data;

            var size = data.substring(first+2, second);
            var dataToDeflate = data.substring(second+2);
            var decodedBytes = Base64.getDecoder().decode(dataToDeflate);
            var i = new Inflater();
            i.setInput(decodedBytes);
            var a = i.getTotalIn();
            var b = i.getTotalOut();
            var outbytes = new byte[Integer.parseInt(size)];
            var bytesInflated = i.inflate(outbytes);

            return new String(outbytes, 0, bytesInflated, StandardCharsets.UTF_8);
        } else {
            return data;
        }
    }

    public static boolean isCompressed(String data) {
        return data.startsWith(MAGIC);
    }

}
