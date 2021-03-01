package org.gorpipe.gor.driver.bgenreader;

import com.github.luben.zstd.ZstdInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ZStdUnzipper extends Unzipper {
    private ZstdInputStream zstdIs;

    @Override
    void setInput(byte[] in, int offset, int len) throws IOException {
        this.zstdIs = new ZstdInputStream(new ByteArrayInputStream(in, offset, len));
    }

    @Override
    int unzip(byte[] out, int offset, int len) throws IOException {
        return this.zstdIs.read(out, offset, len);
    }

    @Override
    void reset() throws IOException {
        this.zstdIs.close();
        this.zstdIs = null;
    }
}
