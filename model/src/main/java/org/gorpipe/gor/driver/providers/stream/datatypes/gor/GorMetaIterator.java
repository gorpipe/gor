package org.gorpipe.gor.driver.providers.stream.datatypes.gor;

import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.FileMetaIterator;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.model.GorMeta;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.gor.util.DynamicRowIterator;

import java.io.IOException;
import java.nio.file.Path;


public class GorMetaIterator extends DynamicRowIterator {

    final static String GOR_SOURCE = "GOR";

    public void initMeta(StreamSourceFile file) throws IOException {
        var fileIt = new FileMetaIterator();
        fileIt.initMeta(file);
        merge(fileIt);

        var gorMeta = GorMeta.createAndLoad(Path.of(DataUtil.toFile(file.getName(), DataType.META)));
        for (String k : gorMeta.getPropertyKeys()) {
            addRow(GOR_SOURCE, k, gorMeta.getProperty(k, ""));
        }
    }
}
