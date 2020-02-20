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

package org.gorpipe.gor.driver.providers.gorserver;

import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.gor.driver.DataSource;

import java.io.IOException;

/**
 * Represents a data source that is capable of seeking to chromosome/position and returning parsed data lines.
 * Optional support for pushing down GOR filters.
 * <p>
 * Created by villi on 20/09/15.
 */
public interface GorSource extends DataSource {

    /**
     * Open iterator without filter
     */
    GenomicIterator open() throws IOException;

    /**
     * Open iterator and push down filter expression.
     *
     * @param filter Simple filter expression - use subset of GOR Where syntax (excluding the WHERE keyword) -
     *               only referring to columns or literal values with limited scalar function support.
     *               And and or are supported.
     * @return GenomicIterator filtering on query.
     */
    GenomicIterator open(String filter) throws IOException;

    /**
     * Check if this source supports filtering.
     */
    boolean supportsFiltering();

}
