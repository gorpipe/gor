package org.gorpipe.gor.driver.providers.stream.sources;

import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.driver.utils.TestUtils;

import java.io.IOException;

public class UTestFilesFileSource extends CommonFilesTests {
    @Override
    protected String getDataName(String name) {
        return TestUtils.getTestFile("bvl_min/" + name);
    }

    @Override
    protected StreamSource createSource(String name) throws IOException {
        return new FileSource(new SourceReference(name));
    }
}
