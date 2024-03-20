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

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.Row;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sigmar on 26/04/16.
 */
public class UTestGorMapMultimap {

    private File gorFile;
    private File mapJoin;
    private File mapJoinNoHeader;
    private File mapJoinNoHashInHeader;
    private File pnsTxt;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
        mapJoin = FileTestUtils.createTempFile(workDir.getRoot(), "mapjoin.txt",
                "#Gene_Symbol\tval\telvis\tfoo\n" +
                        "a\tval1\taron\t\n" +
                        "b\tval2\taron\t\n" +
                        "OR4F5\tval3\taron\t\n" +
                        "OR4F5\tval4\taron\t"
        );
        mapJoinNoHashInHeader = FileTestUtils.createTempFile(workDir.getRoot(), "mapjoin_nohashinheader.txt",
                "Gene_Symbol\tval\telvis\tfoo\n" +
                        "a\tval1\taron\t\n" +
                        "b\tval2\taron\t\n" +
                        "OR4F5\tval3\taron\t\n" +
                        "OR4F5\tval4\taron\t"
        );
        mapJoinNoHeader = FileTestUtils.createTempFile(workDir.getRoot(), "mapjoin_noheader.txt",
                        "a\tval1\taron\t\n" +
                        "b\tval2\taron\t\n" +
                        "OR4F5\tval3\taron\t\n" +
                        "OR4F5\tval4\taron\t"
        );


        pnsTxt = FileTestUtils.createPNTxtFile(workDir.getRoot());
    }


    @Test
    public void testMap() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + " | map -c Gene_Symbol " + mapJoin.getCanonicalPath() + "";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            while (rs.hasNext()) {
                Row r = rs.next();
                Assert.assertEquals("Wrong number of columns", 7, r.numCols());
                count++;
            }
            Assert.assertEquals("Gor map cartesian join failed", 1, count);
        }
    }

    @Test
    public void testMapNoHashHeader() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + " | map -c Gene_Symbol -h " + mapJoinNoHashInHeader.getCanonicalPath() + "";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            while (rs.hasNext()) {
                Row r = rs.next();
                Assert.assertEquals("Wrong number of columns", 7, r.numCols());
                count++;
            }
            Assert.assertEquals("Gor map cartesian join failed", 1, count);
        }
    }

    @Test
    public void testMapNoHashHeaderWithNOption() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + " | map -c Gene_Symbol -n elvis " + mapJoinNoHashInHeader.getCanonicalPath() + "";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            while (rs.hasNext()) {
                Row r = rs.next();
                Assert.assertEquals("Wrong number of columns", 5, r.numCols());
                count++;
            }
            Assert.assertEquals("Gor map cartesian join failed", 1, count);
        }
    }

    @Test
    public void testMapNoHeader() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + " | map -c Gene_Symbol " + mapJoinNoHeader.getCanonicalPath() + "";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            while (rs.hasNext()) {
                Row r = rs.next();
                Assert.assertEquals("Wrong number of columns", 7, r.numCols());
                count++;
            }
            Assert.assertEquals("Gor map cartesian join failed", 1, count);
        }
    }

    @Test
    public void testMapWithSingleColumnInputSource() {
        try {
            TestUtils.runGorPipeCount("gor " + gorFile.getCanonicalPath() + " | map -c Gene_Symbol " + pnsTxt.getCanonicalPath() + "");
            Assert.fail("Single column input source to MAP should throw an exception.");
        } catch (GorParsingException gpe) {
            Assert.assertTrue(gpe.getMessage().contains("No valid output column found"));
        } catch (Exception e) {
            Assert.fail("Unexpected exception when calling map with a single column file.");
        }
    }

    @Test
    public void testMapWithLink() throws IOException {
        File dataFile = new File("" + mapJoin.getCanonicalPath() + "");

        // Create link file
        Path linkFile = Files.createTempFile("mapjoin", DataType.LINK.suffix);
        PrintWriter writer = new PrintWriter(linkFile.toString());
        writer.println(dataFile.getAbsolutePath());
        writer.close();
        String[] lines = TestUtils.runGorPipeLines("gor " + gorFile.getCanonicalPath() + " | map -c Gene_Symbol " + linkFile.toString());
        Assert.assertEquals("Wrong number of lines when using map command", 1, Arrays.stream(lines).skip(1).count());
    }

    @Test
    public void testMapS() throws IOException {
        String[] lines = TestUtils.runGorPipeLines("gor " + gorFile.getCanonicalPath() + " | map -c Gene_Symbol -e " + mapJoin.getCanonicalPath() + "");
        Assert.assertEquals("Wrong number of lines when using map command", 1, Arrays.stream(lines).skip(1).map(String::trim).peek(c -> Assert.assertFalse("Column value should not end with a comma", c.endsWith(","))).count());
    }

    @Test
    public void testMapCis() throws Exception {
        String patientRelationsPath = createPatientRelationsFileWithMixedCaseData();
        String patientsPath = createPatientsFile();

        String caseSensitiveResults = "ChromNOR\tPosNOR\tpatient_id\tcol2\n" +
                "chrN\t0\tHB:000001\tHB:000003\n" +
                "chrN\t0\tHB:000002\tHB:000003\n" +
                "chrN\t0\tHB:000003\t\n" +
                "chrN\t0\tHB:000004\tHB:000006\n" +
                "chrN\t0\tHB:000006\t\n";

        String caseInsensitiveResults = "ChromNOR\tPosNOR\tpatient_id\tcol2\n" +
                "chrN\t0\tHB:000001\tHB:000003,hb:000004\n" +
                "chrN\t0\tHB:000002\tHB:000003,hb:000004\n" +
                "chrN\t0\tHB:000003\t\n" +
                "chrN\t0\tHB:000004\thb:000005,HB:000006\n" +
                "chrN\t0\tHB:000005\t\n" +
                "chrN\t0\tHB:000006\t\n";

        //Run with case sensitive column data lookup (default)
        String[] args = new String[]{"nor -h " + patientsPath + " | map -c patient_id " + patientRelationsPath};
        Assert.assertEquals("Nor map failed", 5, TestUtils.runGorPipeCount(args));
        Assert.assertEquals(caseSensitiveResults, TestUtils.runGorPipe(args));

        //Run with case insensitive column data lookup (with -cis)
        args = new String[]{"nor -h " + patientsPath + " | map -cis -c patient_id " + patientRelationsPath};
        Assert.assertEquals("Nor map with -cis failed", 6, TestUtils.runGorPipeCount(args));
        Assert.assertEquals(caseInsensitiveResults, TestUtils.runGorPipe(args));
    }

    @Test
    public void testMultiMap() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + " | multimap -c Gene_Symbol " + mapJoin.getCanonicalPath() + "";

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            while (rs.hasNext()) {
                rs.next();
                count++;
            }
            Assert.assertEquals("Gor map cartesian join failed", 2, count);
        }
    }

    @Test
    public void testMapLookupCacheWithSameSignature() {
        String query = "create xxx = gor ../tests/data/gor/genes.gor | top 1" +
                "| inset -c gene_symbol <(nor -h ../tests/data/gor/genes.gor | top 1 | select gene_symbol,1-) -b -cis " +
                "| map -c gene_symbol <(nor -h ../tests/data/gor/genes.gor | top 1 |select gene_symbol,1-) -m 0; gor [xxx] | select 1,2,Gene_Symbolx";

        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("Wrong result from inset/map query", res,
                "Chrom\tgene_start\tGene_Symbolx\n" +
                "chr1\t11868\tDDX11L1\n");
    }

    @Test
    public void testMapCartesianSingleColumn() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + " | multimap -cartesian <(nor " + mapJoin.getCanonicalPath() + " | select #1)";
        int count = TestUtils.runGorPipeCount(query);

        // There are only 3 distinct rows in the leftmost column
        Assert.assertEquals("Gor map cartesian join failed", 27, count);
    }

    @Test
    public void testMultimapCartesian() throws IOException {
        String query = "gor " + gorFile.getCanonicalPath() + " | multimap -cartesian " + mapJoin.getCanonicalPath() + "";
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals("GOR multimap cartesian join failed", 36, count);
    }

    @Test
    public void testMultimapCartesianWithLinkFile() throws IOException {
        Path linkFilePath = Files.createTempFile("mapjoin", DataType.LINK.suffix);
        File rf = linkFilePath.toFile();
        rf.deleteOnExit();
        File f = new File("" + mapJoin.getCanonicalPath() + "");
        Files.write(rf.toPath(), f.getAbsolutePath().getBytes());

        String query = "gor " + gorFile.getCanonicalPath() + " | multimap -cartesian " + linkFilePath;
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals("GOR multimap cartesian join failed", 36, count);
    }

    private static String createPatientRelationsFileWithMixedCaseData() throws IOException {
        String patientRelationsPath = createPatientRelationsFile();

        List<String> fileContent = new ArrayList<>(Files.readAllLines(Paths.get(patientRelationsPath)));
        for (int i = 0; i < fileContent.size(); i++) {
            if (i % 2 == 0) {
                fileContent.set(i, fileContent.get(i).toLowerCase());
            }
        }
        Files.write(Paths.get(patientRelationsPath), fileContent);

        return patientRelationsPath;
    }

    private static String createPatientRelationsFile() throws IOException {
        // Create parent relation file .tsv
        Path patientRelationsPath = Files.createTempFile("patient_relations", ".tsv");
        File outputFile = patientRelationsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("patient_id\trelation_to");
        outputWriter.println("HB:000001\tHB:000003");   // Index -> Father
        outputWriter.println("HB:000001\tHB:000004");   // Index -> Mother
        outputWriter.println("HB:000002\tHB:000003");   // Sibling -> Father
        outputWriter.println("HB:000002\tHB:000004");   // Sibling -> Mother
        outputWriter.println("HB:000003\t");            // Father
        outputWriter.println("HB:000004\tHB:000005");   // Mother->Grandfather
        outputWriter.println("HB:000004\tHB:000006");   // Mother->Grandmother
        outputWriter.println("HB:000005\t");            // Grandfather mother's side
        outputWriter.println("HB:000006\t");            // Grandmother mother's side
        outputWriter.close();

        return patientRelationsPath.toString();
    }

    private static String createPatientsFile() throws IOException {
        // Create parent relation file .tsv
        Path patientsPath = Files.createTempFile("patients", ".tsv");
        File outputFile = patientsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("patient_id");
        outputWriter.println("HB:000001"); // Index
        outputWriter.println("HB:000002"); // Sibling
        outputWriter.println("HB:000003"); // Father
        outputWriter.println("HB:000004"); // Mother
        outputWriter.println("HB:000005"); // Grandfather mother's side
        outputWriter.println("HB:000006"); // Grandmother mother's side
        outputWriter.close();

        return patientsPath.toString();
    }

}
