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
 * Created by sigmar on 02/05/16.
 */
public class UTestGorMerge {
    private File gorFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
    }

    private static final Logger log = LoggerFactory.getLogger(UTestGorMerge.class);

    @Test
    public void testMergeWithDifferentColumnNames() {
        String query = "gor ../tests/data/gor/dbsnp_test.gorz ../tests/data/gor/dbsnp_test_different_colnames.gor";

        boolean failed = false;

        try {
            TestUtils.runGorPipeCount(query);
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertTrue("Merging gor files should fail on files with different column names", failed);
    }

    @Test
    public void testMergeWithNor() {
        String query = "nor ../tests/data/gor/dbsnp_test.gorz | merge ../tests/data/gor/dbsnp_test.gorz";

        try (RowSource pi = TestUtils.runGorPipeIterator(query)) {
            while (pi.hasNext()) {
                String next = pi.next().toString();
                Assert.assertEquals("Merging with nor failed", 7, next.split("\t").length);
            }
        }
    }

    @Test
    public void testMergeWithNorNonGorFile() throws IOException {
        String query = "nor " + gorFile.getCanonicalPath() + " | merge " + gorFile.getCanonicalPath() + "";

        try (RowSource pi = TestUtils.runGorPipeIterator(query)) {
            while (pi.hasNext()) {
                String next = pi.next().toString();
                Assert.assertEquals("Merging with nor failed", 6, next.split("\t").length);
            }
        }
    }
}
