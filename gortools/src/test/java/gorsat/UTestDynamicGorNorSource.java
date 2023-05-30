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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.util.Util;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class UTestDynamicGorNorSource {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    public static final String DATA = "#data\n" +
            "1_10\n" +
            "1_9\n" +
            "1_8\n" +
            "1_7\n" +
            "1_6\n" +
            "1_5\n" +
            "1_4\n" +
            "1_3\n" +
            "1_2\n" +
            "1_1";

    @Test
    public void validGorNestedNor() throws IOException {
        String expected = "CHROM\tPOS\n" +
                "chr1\t1\t\n" +
                "chr1\t2\t\n" +
                "chr1\t3\t\n" +
                "chr1\t4\t\n" +
                "chr1\t5\t\n" +
                "chr1\t6\t\n" +
                "chr1\t7\t\n" +
                "chr1\t8\t\n" +
                "chr1\t9\t\n" +
                "chr1\t10\t\n";

        File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "data.txt", DATA);

        String query = "gor <(nor " + tempFile.getAbsolutePath() + " | colsplit #1 2 split -s '_' | replace split_1 'chr'+split_1 | rename split_1 CHROM | rename split_2 POS | select CHROM,POS) | sort genome";
        String results = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, results);
    }

    @Test
    public void invalidGorNestedNor() throws IOException {
        File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "data.txt", DATA);
        String query = "gor <(nor " + tempFile.getAbsolutePath() + ")";
        Assert.assertThrows(GorDataException.class, () -> TestUtils.runGorPipe(query));
    }

    @Test
    public void testQueryOptionRemove() {
        String query = "gor -p chr1:1-2 -f 'PO' genes.gor";
        String simpleQuery = Util.removeSeekFilterOptionsFromQuery(query);
        Assert.assertEquals("Remove options failed", "gor genes.gor", simpleQuery);

        query = "gor -p chr1:1-2 -ff <(nor stuff.tsv) genes.gor";
        simpleQuery = Util.removeSeekFilterOptionsFromQuery(query);
        Assert.assertEquals("Remove options failed", "gor genes.gor", simpleQuery);
    }
}
