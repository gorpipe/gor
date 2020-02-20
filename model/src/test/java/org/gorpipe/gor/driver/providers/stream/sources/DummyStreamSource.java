package org.gorpipe.gor.driver.providers.stream.sources;

import java.io.IOException;
import java.io.InputStream;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;

/**
 * Dummy boilerplate stream source for testing.
 * Created by villi on 15/09/15.
 */
public class DummyStreamSource implements StreamSource {
    @Override
    public StreamSourceMetadata getSourceMetadata() {
        return null;
    }

    @Override
    public SourceReference getSourceReference() {
        return null;
    }

    @Override
    public InputStream open() throws IOException {
        return null;
    }

    @Override
    public InputStream open(long start) throws IOException {
        return null;
    }

    @Override
    public InputStream open(long start, long minLength) throws IOException {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public SourceType getSourceType() {
        return null;
    }

    @Override
    public DataType getDataType() {
        return null;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public void close() {

    }
}
