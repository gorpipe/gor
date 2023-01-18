/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor.driver.providers.stream.datatypes.gorgz;

import com.google.auto.service.AutoService;
import htsjdk.samtools.util.BlockCompressedInputStream;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.adapters.BlockCompressedSeekableFile;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorMetaIterator;
import org.gorpipe.gor.driver.providers.stream.datatypes.tabix.TabixIndexedFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.binsearch.GorSeekableIterator;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GenomicIteratorBase;
import org.gorpipe.gor.util.DynamicRowIterator;

import java.io.BufferedInputStream;
import java.io.IOException;

@AutoService(StreamSourceIteratorFactory.class)
public class GorGzIteratorFactory implements StreamSourceIteratorFactory {
    @Override
    public GenomicIterator createIterator(StreamSourceFile file) throws IOException {
        if (file.getIndexSource() != null && file.getIndexSource().exists()) {
            return new GorIndexedFileIterator((TabixIndexedFile) file);
        } else {
            StreamSource source = file.getFileSource();
            if (BlockCompressedInputStream.isValidFile(new BufferedInputStream(source.open()))) {
                BlockCompressedSeekableFile bcss = new BlockCompressedSeekableFile(source);
                return new GorSeekableIterator(bcss);
            }
            throw new GorSystemException("Invalid block compressed stream." + source.getName(), null);
        }
    }

    @Override
    public GenomicIteratorBase createMetaIterator(StreamSourceFile file) throws IOException {
        var it = new GorMetaIterator();
        it.initMeta(file);
        return it;
    }

    @Override
    public DataType[] getSupportedDataTypes() {
        return new DataType[]{DataType.GORGZ};
    }

    @Override
    public StreamSourceFile resolveFile(StreamSource source) {
        return new TabixIndexedFile(source);
    }
}
