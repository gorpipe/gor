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

package org.gorpipe.gor.driver.providers.stream.datatypes.cram;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.adapters.SamtoolsAdapter;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.FileMetaIterator;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.datatypes.bam.BamFileIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GenomicIteratorBase;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.util.DynamicRowIterator;

import java.io.IOException;

@AutoService(StreamSourceIteratorFactory.class)
public class CramFileIteratorFactory implements StreamSourceIteratorFactory {

    final static String CRAM_SOURCE = "BAM";

    @Override
    public GenomicIterator createIterator(StreamSourceFile file) throws IOException {
        return new CramFileIterator((CramFile) file);
    }

    @Override
    public GenomicIteratorBase createMetaIterator(StreamSourceFile file, FileReader reader) throws IOException {
        var fileIt = new FileMetaIterator();
        fileIt.initMeta(file);

        // Load the bam header and add it to the meta iterator
        try (var it = createIterator(file)) {
            ((CramFileIterator) it).init(file.getFileSource().getSourceReference().getLookup(), SamtoolsAdapter.createReader(file.getFileSource(),
                    file.getIndexSource()), true);
            var additonalInfo = it.getAdditionalInfo();

            for (var info : additonalInfo) {
                var infoRow = info.split("\t", 2);

                if (infoRow.length == 2) {
                    fileIt.addRow(CRAM_SOURCE, infoRow[0], infoRow[1].replace("\t", ","));
                }
            }
        }

        return fileIt;
    }

    @Override
    public DataType[] getSupportedDataTypes() {
        return new DataType[]{DataType.CRAM};
    }

    @Override
    public StreamSourceFile resolveFile(StreamSource source) {
        return new CramFile(source);
    }
}
