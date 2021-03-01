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

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class UTestSegproj {

    String segmentFile1Path;
    String segmentFile2Path;
    String[] functions = new String[] {"min", "med", "max", "avg", "std", "sum"};

    @Before
    public void setUp() throws IOException {
            segmentFile1Path = createSegmentFile1();
            segmentFile2Path = createSegmentFile2();
    }

    @Test
    public void testSegprojBasics() {
        String[] lines = TestUtils.runGorPipeLines("gor " + segmentFile1Path + " | segproj -maxseg 10000");

        Integer[] expectedValues = new Integer[] {1,2,1,2,1,1,2,1,1,1};
        Integer counter = 0;

        for(String line : Iterables.skip(Arrays.asList(lines),1) ) {
             Integer value = Integer.parseInt(line.split("\t")[3].trim());
             Assert.assertEquals("Segproj count", expectedValues[counter++], value);
        }
    }

    @Test
    public void testSegprojWithSumColumn() {
        String[] lines = TestUtils.runGorPipeLines("gor " + segmentFile2Path + " | segproj -maxseg 10000 -sumcol 4");

        Integer[] expectedValues = new Integer[] {1,3,2,5,2,4,9,5,6,7};
        Integer counter = 0;

        for(String line : Iterables.skip(Arrays.asList(lines),1) ) {
            Integer value = Integer.parseInt(line.split("\t")[3].trim());
            Assert.assertEquals("Segproj count", expectedValues[counter++], value);
        }
    }

    @Test
    public void testSegProjWithGc() {
        String query = String.format("gor %s | segproj -maxseg 10000 -gc value", segmentFile2Path);
        String result = TestUtils.runGorPipe(query);
        String expected = "chr\tbpstart\tbpstop\tvalue\tsegCount\n" +
                "chr1\t500\t1000\t1\t1\n" +
                "chr1\t600\t2000\t2\t1\n" +
                "chr1\t1300\t1900\t3\t1\n" +
                "chr1\t2500\t4000\t4\t1\n" +
                "chr1\t3900\t6000\t5\t1\n" +
                "chr1\t6000\t7000\t6\t1\n" +
                "chr1\t32100\t32500\t7\t1\n";
        Assert.assertEquals(expected, result);
    }

    private static String createSegmentFile1() throws IOException {
        // Create parent relation file .tsv
        Path patientsPath = Files.createTempFile("segmentfile1", ".gor");
        File outputFile = patientsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("chr\tbpstart\tbpstop");
        outputWriter.println("chr1\t500\t1000");
        outputWriter.println("chr1\t600\t2000");
        outputWriter.println("chr1\t1300\t1900");
        outputWriter.println("chr1\t2500\t4000");
        outputWriter.println("chr1\t3900\t6000");
        outputWriter.println("chr1\t6000\t7000");
        outputWriter.println("chr1\t32100\t32500");
        outputWriter.close();

        /*Expected count
        500-600             1
        600-1000            2
        1000-1300           1
        1300-1900           2
        1900-2000           1
        2500-3900           1
        3900-4000           2
        4000-6000           1
        6000-7000           1
        32100-32500         1
        */

        return patientsPath.toString();
    }

    private static String createSegmentFile2() throws IOException {
        // Create parent relation file .tsv
        Path patientsPath = Files.createTempFile("segmentfile2", ".gor");
        File outputFile = patientsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("chr\tbpstart\tbpstop\tvalue");
        outputWriter.println("chr1\t500\t1000\t1");
        outputWriter.println("chr1\t600\t2000\t2");
        outputWriter.println("chr1\t1300\t1900\t3");
        outputWriter.println("chr1\t2500\t4000\t4");
        outputWriter.println("chr1\t3900\t6000\t5");
        outputWriter.println("chr1\t6000\t7000\t6");
        outputWriter.println("chr1\t32100\t32500\t7");
        outputWriter.close();

        /*Expected count
        500-600             1
        600-1000            3
        1000-1300           2
        1300-1900           5
        1900-2000           2
        2500-3900           4
        3900-4000           9
        4000-6000           5
        6000-7000           6
        32100-32500         7
        */

        return patientsPath.toString();
    }
}
