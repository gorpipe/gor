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

package org.gorpipe.gor.driver.providers.stream.datatypes.vcf;

import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.VcfGzGenomicIterator;

import java.io.IOException;

/**
 * Created by sigmar on 27/10/15.
 */
public class VcfFileIterator extends VcfGzGenomicIterator {
    public VcfFileIterator(StreamSourceFile file, GenomicIterator.ChromoLookup lookup, int[] columns, boolean compressed) throws IOException {
        super(lookup, file.getName(), columns, file.getFileSource(), compressed);
    }
}
