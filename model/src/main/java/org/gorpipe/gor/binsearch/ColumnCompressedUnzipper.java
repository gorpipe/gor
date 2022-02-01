package org.gorpipe.gor.binsearch;

import com.github.luben.zstd.ZstdInputStream;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.util.collection.ByteArray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.InflaterOutputStream;

public class ColumnCompressedUnzipper extends Unzipper {
    private byte[] lookupBytesCompressed7Bit;
    private final Map<Integer, Map<Integer, byte[]>> mapExtTable;
    private boolean lookupTableParsed = false;

    ColumnCompressedUnzipper(byte[] lookupBytesCompressed7Bit) {
        super();
        this.mapExtTable = new HashMap<>();
        this.lookupBytesCompressed7Bit = lookupBytesCompressed7Bit;
    }

    private byte[] getLookupTable() {
        final byte[] lookupBytesCompressed = ByteArray.to8Bit(lookupBytesCompressed7Bit);
        final byte[] toReturn;
        try {
            toReturn = inflate(lookupBytesCompressed);
        } catch (IOException e) {
            throw new GorDataException("Could not uncompress the lookup table", e);
        }
        return toReturn;
    }

    private byte[] inflate(byte[] lookupBytesCompressed) throws IOException {
        if (this.type == CompressionType.ZLIB) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final InflaterOutputStream infOS = new InflaterOutputStream(baos)) {
                infOS.write(lookupBytesCompressed);
            }
            return baos.toByteArray();
        } else {
            final ByteArrayInputStream baos = new ByteArrayInputStream(lookupBytesCompressed);
            final byte[] toReturn;
            try (final ZstdInputStream zstdInputStream = new ZstdInputStream(baos)) {
                byte[] array = new byte[16];
                int read;
                int totalRead = 0;
                while ((read = zstdInputStream.read(array, totalRead, array.length - totalRead)) > 0) {
                    totalRead += read;
                    if (totalRead == array.length) {
                        array = Arrays.copyOf(array, 2 * array.length);
                    }
                }
                toReturn = Arrays.copyOfRange(array, 0, totalRead);
            }
            return toReturn;
        }
    }

    @Override
    public int decompress(int offset, int len) throws DataFormatException, IOException {
        if (!this.lookupTableParsed) {
            final byte[] lookupTable = getLookupTable();
            BlockPacker.lookupMapFromBytes(this.mapExtTable, lookupTable);
            this.lookupTableParsed = true;
            this.lookupBytesCompressed7Bit = null;
        }
        if (this.done) {
            return 0;
        } else {
            if(rawDataHolder.capacity() < outBuffer.capacity()) {
                rawDataHolder = ByteBuffer.allocate(outBuffer.capacity());
            }
            super.decompress(0, outBuffer.limit());
            int ret = BlockPacker.decode(outBuffer.array(), 0, rawDataHolder.array(), offset, this.mapExtTable);
            ByteBuffer oout = this.outBuffer;
            this.outBuffer = rawDataHolder;
            this.rawDataHolder = oout;
            return ret;
        }
    }
}
