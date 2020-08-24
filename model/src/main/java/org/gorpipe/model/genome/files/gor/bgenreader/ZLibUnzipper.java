package org.gorpipe.model.genome.files.gor.bgenreader;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ZLibUnzipper extends Unzipper {

    private final Inflater inf;

    ZLibUnzipper() {
        this.inf = new Inflater();
    }

    @Override
    void setInput(byte[] in, int offset, int len) {
        this.inf.setInput(in, offset, len);
    }

    @Override
    int unzip(byte[] out, int offset, int len) throws DataFormatException {
       return this.inf.inflate(out, offset, len);
    }

    @Override
    void reset() {
        this.inf.reset();
    }
}
