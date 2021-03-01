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

package gorsat;

import org.gorpipe.gor.driver.providers.stream.datatypes.bam.BamIterator;
import org.gorpipe.gor.model.DefaultChromoLookup;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.Line;
import org.gorpipe.gor.model.Row;
import org.junit.Assert;
import org.junit.Test;


/**
 * Created by sigmar on 23/12/15.
 */
public class UTestBamTagParsing {

    @Test
    public void testBamTagParse() {
        GenomicIterator.ChromoLookup lookup = new DefaultChromoLookup();
        BamIterator bamit = new BamIterator(lookup, "../tests/data/external/samtools/serialization_test.bam");

        while (bamit.hasNext()) {
            Row row = bamit.next();
            String tag = row.colAsString(13).toString();
            Assert.assertFalse(tag.startsWith(" "));
        }
    }

    @Test
    public void testBamNorContext() {
        String gorcmd = "gor ../tests/data/external/samtools/serialization_test.bam | where #3-#2 < 1000";
        String[] args = new String[]{gorcmd};

        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals("Incorrect group size", 1, count);
    }
}
