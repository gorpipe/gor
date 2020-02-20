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

package org.gorpipe.gor.driver.providers.stream;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import org.gorpipe.gor.driver.GorDriverModule;
import org.gorpipe.gor.driver.Plugin;
import org.gorpipe.gor.driver.providers.mem.MemSourceProvider;
import org.gorpipe.gor.driver.providers.stream.datatypes.bam.BamFileIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.datatypes.bgen.BGenFileIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.datatypes.cram.CramFileIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.datatypes.gorgz.GorGzIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.datatypes.gorz.GorzIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.datatypes.parquet.ParquetIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.datatypes.vcf.VcfIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSourceProvider;
import org.gorpipe.gor.driver.providers.stream.sources.http.HTTPSourceProvider;

public class StreamSourcePlugin implements Plugin {

    public StreamSourcePlugin() {
    }

    @Override
    public void configure(Binder binder) {
        GorDriverModule.bindSourceProvider(binder, FileSourceProvider.class);
        GorDriverModule.bindSourceProvider(binder, HTTPSourceProvider.class);
        bindStreamSourceIteratorFactory(binder, GorIteratorFactory.class);
        bindStreamSourceIteratorFactory(binder, GorzIteratorFactory.class);
        bindStreamSourceIteratorFactory(binder, ParquetIteratorFactory.class);
        bindStreamSourceIteratorFactory(binder, BamFileIteratorFactory.class);
        bindStreamSourceIteratorFactory(binder, BGenFileIteratorFactory.class);
        bindStreamSourceIteratorFactory(binder, CramFileIteratorFactory.class);
        bindStreamSourceIteratorFactory(binder, GorGzIteratorFactory.class);
        bindStreamSourceIteratorFactory(binder, VcfIteratorFactory.class);
        GorDriverModule.bindSourceProvider(binder, MemSourceProvider.class);
    }

    private static void bindStreamSourceIteratorFactory(Binder binder,
                                                        Class<? extends StreamSourceIteratorFactory> providerClass) {
        Multibinder<StreamSourceIteratorFactory> multiBinder = Multibinder.newSetBinder(binder, StreamSourceIteratorFactory.class);
        multiBinder.addBinding().to(providerClass);
    }
}
