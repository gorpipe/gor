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

package org.gorpipe.gor.driver;

import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.model.genome.files.gor.GenomicIterator;

import java.io.IOException;

public interface GorDriver extends SourceProvider {

    // TODO: change the subset parameters into a more generic "query hint" mechanism and remove subset from public interface

    /**
     * Create genomic iterator from source using default chromosome lookup and
     * columns
     */
    GenomicIterator createIterator(SourceReference reference) throws IOException;

    /**
     * Get data source from source reference.
     */
    DataSource getDataSource(SourceReference reference);

    /**
     * Get current gor driver configuration.
     */
    GorDriverConfig config();

}
