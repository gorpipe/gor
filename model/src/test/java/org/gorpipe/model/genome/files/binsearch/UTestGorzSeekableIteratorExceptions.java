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

package org.gorpipe.model.genome.files.binsearch;

import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.Line;
import org.gorpipe.model.genome.files.binsearch.GorzSeekableIterator;
import org.junit.Assert;
import org.junit.Test;

public class UTestGorzSeekableIteratorExceptions {

    @Test
    public void test_nextOnLine() {
        final GenomicIterator it = getIterator("../tests/data/gor/dbsnp_test.gorz");
        final Line line = new Line(3);
        boolean success = false;
        try {
            it.next(line);
        } catch (UnsupportedOperationException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    private GenomicIterator getIterator(String path) {
        return new GorzSeekableIterator(new StreamSourceSeekableFile(new FileSource(new SourceReference(path))));
    }
}
