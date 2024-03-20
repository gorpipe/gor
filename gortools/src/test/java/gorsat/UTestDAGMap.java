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
public class UTestDAGMap {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
    }

    @Test
    public void testDAGMAP() throws Exception {
        String patientRelationsPath = createPatientRelationsFile();
        String patientsPath = createPatientsFile();
        String query = "nor -h " + patientsPath + " | dagmap -c patient_id " + patientRelationsPath;

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            int deepestDepth = 0; // cool name
            while (rs.hasNext()) {
                Row currentRow = rs.next();
                int relationshipDepth = currentRow.colAsInt(4);
                deepestDepth = Math.max(deepestDepth, relationshipDepth);
                Assert.assertEquals("Number of colmns per row in basic dagmap", 5, currentRow.toString().split("\t", -1).length);
                count++;
            }
            Assert.assertEquals("Gor map cartesian join failed", 16, count);
            Assert.assertEquals("Depth of relations not correct", 2, deepestDepth);
        }
    }

    @Test
    public void testDAGMAPWithPath() throws Exception {
        String patientRelationsPath = createPatientRelationsFile();
        String patientsPath = createPatientsFile();
        String query = "nor -h " + patientsPath + " | dagmap -c patient_id -dp " + patientRelationsPath;

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            int deepestDepth = 0; // cool name
            while (rs.hasNext()) {
                Row currentRow = rs.next();
                int relationshipDepth = currentRow.colAsInt(4);
                deepestDepth = Math.max(deepestDepth, relationshipDepth);
                Assert.assertEquals("Relationship path does not match dag_dist", relationshipDepth, currentRow.colAsString(5).toString().split("->", -1).length - 1);
                Assert.assertEquals("Number of colmns per row in basic dagmap", 6, currentRow.toString().split("\t", -1).length);
                count++;
            }
            Assert.assertEquals("Gor map cartesian join failed", 16, count);
            Assert.assertEquals("Depth of relations not correct", 2, deepestDepth);
        }
    }

    @Test
    public void testDAGMAPWithPathAndCustomSeparator() throws Exception {
        String patientRelationsPath = createPatientRelationsFile();
        String patientsPath = createPatientsFile();
        String query = "nor -h " + patientsPath + " | dagmap -c patient_id -dp -ps ';' " + patientRelationsPath;

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            while (rs.hasNext()) {
                Row currentRow = rs.next();
                int relationshipDepth = currentRow.colAsInt(4);
                Assert.assertEquals("Relationship dag_path does not match dag_dist", relationshipDepth, currentRow.colAsString(5).toString().split(";", -1).length - 1);
                Assert.assertEquals("Number of colmns per row in basic dagmap", 6, currentRow.toString().split("\t", -1).length);
                count++;
            }
            Assert.assertEquals("Gor map cartesian join failed", 16, count);
        }
    }

    @Test
    public void testDAGMAPWithPathAndLevelLimits() throws Exception {
        String patientRelationsPath = createPatientRelationsFile();
        String patientsPath = createPatientsFile();
        String query = "nor -h " + patientsPath + " | dagmap -c patient_id -dp -dl 1 " + patientRelationsPath;

        try (GenomicIterator rs = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            int deepestDepth = 0; // cool name
            while (rs.hasNext()) {
                Row currentRow = rs.next();
                deepestDepth = Math.max(deepestDepth, currentRow.colAsInt(4));
                int relationshipDepth = currentRow.colAsInt(4);
                Assert.assertEquals("Relationship path does not match dag_dist", relationshipDepth, currentRow.colAsString(5).toString().split("->", -1).length - 1);
                Assert.assertEquals("Number of colmns per row in basic dagmap", 6, currentRow.toString().split("\t", -1).length);
                count++;
            }
            Assert.assertEquals("Gor map cartesian join failed", 12, count);
            Assert.assertEquals("Depth of relations not correct", 1, deepestDepth);
        }
    }

    @Test // GOR-1219
    public void testDAGMapWithParsingErrorOnMaxLevel() throws Exception {
        String patientRelationsPath = createPatientRelationsFile();
        String patientsPath = createPatientsFile();
        String query = "nor -h " + patientsPath + " | dagmap " + patientRelationsPath + " -c patient_id -dp -dl 2'";

        try {
            TestUtils.runGorPipeLines(query);
        } catch (GorParsingException ex) {
            Assert.assertEquals("-dl", ex.getOption());
        }
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
