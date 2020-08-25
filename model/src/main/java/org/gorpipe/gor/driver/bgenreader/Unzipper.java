package org.gorpipe.gor.driver.bgenreader;

import java.io.IOException;
import java.util.zip.DataFormatException;

abstract class Unzipper {

    abstract void setInput(byte[] in, int offset, int len) throws IOException;

    abstract int unzip(byte[] out, int offset, int len) throws DataFormatException, IOException;

    abstract void reset() throws IOException;
}
