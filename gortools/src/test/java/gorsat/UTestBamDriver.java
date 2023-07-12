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

import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class UTestBamDriver {

    private File bamGord;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        File bamDir = new File("../tests/data/external/samtools/");

        bamGord = FileTestUtils.createTempFile(workDir.getRoot(), "bam.gord",
                bamDir.getCanonicalPath() + "/index_test.bam\tindex\n" +
                        bamDir.getCanonicalPath() + "/serialization_test.bam\tserialization\n"
        );

    }

    @Test
    public void testBamWithMinusPOnDictionary1() throws IOException {
        TestUtils.assertTwoGorpipeResults("gor -p chr1 " + bamGord.getCanonicalPath() + " -f index | top 10 | validatecolumns 0", "gor " + bamGord.getCanonicalPath() + " -f index | top 10");
    }

    @Test
    public void testBamWithMinusPOnDictionary2() throws IOException {
        TestUtils.assertTwoGorpipeResults("gor -p chr1 ../tests/data/external/samtools/index_test.bam | top 1 | validatecolumns 0", "gor ../tests/data/external/samtools/index_test.bam | top 1");
    }

    @Test
    public void testBamWithMinusPOnDictionary3() throws IOException {
        TestUtils.assertTwoGorpipeResults("gor -p chr1 ../tests/data/external/samtools/serialization_test.bam | top 1 | validatecolumns 0", "gor ../tests/data/external/samtools/serialization_test.bam  | top 1");
    }

    @Test
    public void testBamMeta() {
        var result = TestUtils.runGorPipe("META ../tests/data/external/samtools/index_test.bam");
        Assert.assertTrue(result.contains("BAM\t@"));

    }
}
