package org.gorpipe.gor.driver.providers.stream.datatypes.gord;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.FileMetaIterator;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GenomicIteratorBase;

import java.io.IOException;

@AutoService(StreamSourceIteratorFactory.class)
public class GordIteratorFactory implements StreamSourceIteratorFactory {

    @Override
    public DataType[] getSupportedDataTypes() {
        return new DataType[] {DataType.GORD};
    }

    @Override
    public GenomicIterator createIterator(StreamSourceFile file) throws IOException {
        return new GordIterator(file.getFileSource());
    }

    @Override
    public GenomicIteratorBase createMetaIterator(StreamSourceFile file, FileReader reader) throws IOException {
        var fileIt = new FileMetaIterator();
        fileIt.initMeta(file);
        return fileIt;
    }

    @Override
    public StreamSourceFile resolveFile(StreamSource source) {
        return new GordFile(source);
    }
}
