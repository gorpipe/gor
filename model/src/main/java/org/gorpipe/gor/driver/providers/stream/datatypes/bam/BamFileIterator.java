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

package org.gorpipe.gor.driver.providers.stream.datatypes.bam;

import org.gorpipe.gor.driver.adapters.SamtoolsAdapter;
import org.gorpipe.model.genome.files.gor.BamIterator;

/**
 * Extends BamIterator to provide an GenomicIterator using a Stream based Bamfile
 * <p>
 * Created by villi on 23/08/15.
 */
public class BamFileIterator extends BamIterator {
    /**
     * Implements the legacy interface required by SoruceReference
     */
    public BamFileIterator(BamFile file) {
        super(file.getFileSource().getSourceReference().getLookup(), SamtoolsAdapter.createReader(file.getFileSource(),
                file.getIndexSource()), file.getFileSource().getSourceReference().getColumns());
    }
}
