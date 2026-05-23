package org.gorpipe.gor.driver.providers.stream.datatypes.gord;

import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;

public class GordFile extends StreamSourceFile {

    public GordFile(StreamSource gordSource) {
        super(gordSource);
    }

    @Override
    public boolean supportsIndex() {
        return false;
    }
}
