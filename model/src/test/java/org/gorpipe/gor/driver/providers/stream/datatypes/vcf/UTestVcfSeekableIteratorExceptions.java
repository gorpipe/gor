package org.gorpipe.gor.driver.providers.stream.datatypes.vcf;

import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.Line;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class UTestVcfSeekableIteratorExceptions {

    @Test
    public void test_nextOnLine() throws IOException {
        final GenomicIterator it = getIterator("../tests/data/external/samtools/test.vcf");
        final Line line = new Line(3);
        boolean success = false;
        try {
            it.next(line);
        } catch (UnsupportedOperationException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    private GenomicIterator getIterator(String path) throws IOException {
        return new VcfIteratorFactory().createIterator(new StreamSourceFile(new FileSource(new SourceReference(path))));
    }
}

