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

import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Created by gudrunh on 03/11/17
 */

public class UTestGorLiftover {

    private final static String mapstr =
            "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore\n" +
            "chr12\t154166\t154277\tchr12\t45000\t45111\t+\t12341034620\n" +
            "chr12\t154278\t154325\tchr12\t45112\t45159\t+\t12341034620\n" +
            "chr12\t154326\t154453\tchr12\t45160\t45287\t+\t12341034620\n" +
            "chr12\t154480\t6934516\tchr12\t45314\t6825350\t+\t12341034620\n" +
            "chr12\t6934516\t6938023\tchr12\t6825351\t6828858\t+\t12341034620\n" +
            "chr12\t6938023\t7029175\tchr12\t6828859\t6920011\t+\t12341034620\n" +
            "chr12\t7029175\t7080211\tchr12\t6920012\t6971048\t+\t12341034620\n" +
            "chr12\t7080212\t7090659\tchr12\t6971050\t6981497\t+\t12341034620\n" +
            "chr12\t7090659\t7094372\tchr12\t6981498\t6985211\t+\t12341034620\n" +
            "chr12\t7094372\t7094767\tchr12\t6985212\t6985607\t+\t12341034620\n" +
            "chr12\t7094768\t7094790\tchr12\t6985608\t6985630\t+\t12341034620\n" +
            "chr12\t7094790\t7103556\tchr12\t6986698\t6995464\t+\t12341034620\n" +
            "chr12\t7103563\t7108115\tchr12\t6995471\t7000023\t+\t12341034620\n" +
            "chr12\t7108115\t7118625\tchr12\t7000810\t7011320\t+\t12341034620\n" +
            "chr12\t7118625\t7189876\tchr12\t7011321\t7082572\t+\t12341034620\n" +
            "chr12\t7239876\t9994445\tchr12\t7087280\t9841849\t+\t12341034620\n" +
            "chr12\t9994448\t11331782\tchr12\t9841849\t11179183\t+\t12341034620\n";

    private static String mappath;
    private static Path tmpdir;

    @BeforeClass
    public static void setUp() throws IOException {
        tmpdir = Files.createTempDirectory("test-liftover-proof");
        tmpdir.toFile().deleteOnExit();

        File tmpfile = new File(tmpdir.toFile(), "hg19tohg38.gor");
        FileUtils.write(tmpfile, mapstr, StandardCharsets.UTF_8);
        mappath = tmpfile.getAbsolutePath().toString();
    }

    @Test
    public void testGorLiftoverFilterPInPath() {
        String query = "gor -p chr12:220000-1100000 ../tests/data/gor/genes.gorz | liftover " + mappath + " -seg -build hg38";
        int results = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(21, results);
    }

    @Test
    public void testGorLiftoverFilterPInPathSnp() {
        String query = "gor -p chr12:220000-1100000 ../tests/data/gor/genes.gorz | liftover " + mappath + " -snp -build hg38";
        int results = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(21, results);
    }

    @Test
    public void testGorLiftoverFilterPInPathVar() {
        String query = "gor -p chr12:220000-1100000 ../tests/data/gor/genes.gorz | liftover " + mappath + " -var -build hg38";
        int results = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(21, results);
    }

    @Test
    public void testGorLiftoverFilterPInPathInQuotes() {
        String query = "gor ../tests/data/gor/genes.gorz | liftover '" + mappath + "' -var -build hg38";
        int results = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(51776, results);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(tmpdir.toFile());
    }

}
