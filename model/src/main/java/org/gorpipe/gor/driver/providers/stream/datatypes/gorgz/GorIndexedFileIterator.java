/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.gorpipe.gor.driver.providers.stream.datatypes.tabix.TabixIndexedFile;
import org.gorpipe.gor.model.GorGzGenomicIterator;

import java.io.IOException;

/**
 * Extends ChrPosBinIterator to provide an GenomicIterator using a Stream based GorFile
 * <p>
 * Created by villi on 23/08/15.
 */
public class GorIndexedFileIterator extends GorGzGenomicIterator {
    public GorIndexedFileIterator(TabixIndexedFile source) throws IOException {
        super(source.getFileSource().getSourceReference().getLookup(), source.getFileSource(),
                source.getIndexSource(), source.getFileSource().getSourceReference().getColumns());
    }
}
