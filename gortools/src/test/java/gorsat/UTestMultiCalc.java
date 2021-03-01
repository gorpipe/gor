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

import org.gorpipe.model.gor.iterators.RowSource;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by sigmar on 17/05/16.
 */
public class UTestMultiCalc {
    private static final Logger log = LoggerFactory.getLogger(UTestMultiCalc.class);

    private File gorFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
    }


    @Test
    public void testMultiCalc() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + " | calc a,b,c max(5,6),upper('y'),'simmi'";

        try (RowSource rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            String[] header = rs.getHeader().split("\t");
            count = getCount(rs, count, header);
            Assert.assertEquals("Gor multi column calc failed", 9, count);
        }
    }

    private int getCount(RowSource rs, int count, String[] header) {
        while (rs.hasNext()) {
            String line = rs.next().toString();
            String[] split = line.split("\t");
            Assert.assertEquals("Number of columns different than in header", header.length, split.length);
            count++;
        }
        return count;
    }

    @Test
    public void testMultiCalcLargeFile() {
        String query = "gor ../tests/data/gor/genes.gor | calc a,b,c max(5,6),upper('y'),'newcolumn'";

        try (RowSource rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            String header = rs.getHeader();
            String[] headersplit = header.split("\t");
            count = getCount(rs, count, headersplit);
            Assert.assertEquals("Gor multi column calc failed", 51776, count);
        }
    }

    @Test
    public void testCalcIfWoMulticalc() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + " | calc n if(gene_start==0, 'nm', '0')";

        try (RowSource rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            while (rs.hasNext()) {
                rs.next();
                count++;
            }
            Assert.assertEquals("Gor multi column calc failed", 9, count);
        }
    }
}
