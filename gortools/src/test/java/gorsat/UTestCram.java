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

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.test.utils.FileTestUtils;
import org.gorpipe.util.string.StringUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;

public class UTestCram {

    protected static File bamLinkFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void readCramWithFastaReference() {
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/external/samtools/cram_query_sorted.cram -ref ../tests/data/external/samtools/cram_query_sorted.fasta");
        Assert.assertEquals(8, lines.length);
    }

    @Test
    public void readCramFromBamLinkWithFastaReference() throws IOException {
        bamLinkFile = FileTestUtils.createTempFile(workDir.getRoot(), "cram_query_sorted.bam.link", Paths.get("../tests/data/external/samtools/cram_query_sorted.cram").toAbsolutePath().toString());
        String[] lines = TestUtils.runGorPipeLines("gor " + bamLinkFile.getCanonicalPath() + " -ref ../tests/data/external/samtools/cram_query_sorted.fasta");
        Assert.assertEquals(8, lines.length);
    }


    @Test
    public void readCramWithFastaReferenceFromProperty() {
        try {
            System.setProperty("gor.driver.cram.fastareferencesource", "../tests/data/external/samtools/cram_query_sorted.fasta");
            String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/external/samtools/cram_query_sorted.cram");
            Assert.assertEquals(8, lines.length);
        } finally {
            System.clearProperty("gor.driver.cram.fastareferencesource");
        }
    }

    @Test
    public void readCramWithFastaReferenceFromConfig() {
        System.clearProperty("gor.driver.cram.fastareferencesource");
        String[] args = new String[]{"gor ../tests/data/external/samtools/cram_query_sorted.cram", "-config", "../tests/config/gor_unittests_config.txt"};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals(7, count);
    }

    @Test
    public void readCramWithFastaReferenceAndGenerateMissingAttributes() {
        try {
            System.setProperty("gor.driver.cram.fastareferencesource", "../tests/data/external/samtools/cram_query_sorted.fasta");
            System.setProperty("gor.driver.cram.generatemissingattributes", "false");
            String[] linesWithoutMissingAttributes = TestUtils.runGorPipeLines("gor ../tests/data/external/samtools/cram_query_sorted.cram");
            System.setProperty("gor.driver.cram.generatemissingattributes", "true");
            String[] linesWithMissingAttributes = TestUtils.runGorPipeLines("gor ../tests/data/external/samtools/cram_query_sorted.cram");

            Assert.assertEquals(8, linesWithoutMissingAttributes.length);
            Assert.assertEquals(8, linesWithMissingAttributes.length);
            // See if we have the missing entry in the last column.
            Assert.assertFalse(linesWithoutMissingAttributes[1].contains("NM="));
            Assert.assertTrue(linesWithMissingAttributes[1].contains("NM="));

        } finally {
            System.clearProperty("gor.driver.cram.fastareferencesource");
            System.clearProperty("gor.driver.cram.generatemissingattributes");
        }

    }

    @Test(expected = GorResourceException.class)
    public void readCramWithNoReference() {
        TestUtils.runGorPipeLines("gor ../tests/data/external/samtools/cram_query_sorted.cram");
    }

    @Test(expected = GorResourceException.class)
    public void readCramWithReferenceFileButFileNotFound() {
        TestUtils.runGorPipeLines("gor ../tests/data/external/samtools/cram_query_sorted.cram -ref /foo/bar.fasta");
    }

    @Test(expected = GorSystemException.class)
    public void readCramWithIncompatibleFastaReference() {
        TestUtils.runGorPipeLines("gor ../tests/data/external/samtools/cram_query_sorted.cram -ref ../tests/data/external/samtools/ce.fa");
    }

    @Test
    public void readCramWithFastaReferenceInRefFile() throws IOException {

        String basePath = "../tests/data/external/samtools";
        String[] filesToCopy = {"cram_query_sorted.cram", "cram_query_sorted.cram.crai"};
        copyFiles(basePath, workDir.getRoot().toString(), filesToCopy);

        // Create ref file for the cramFile
        File referenceFile = workDir.newFile("cram_query_sorted.cram.ref");
        FileUtils.writeStringToFile(referenceFile, "../tests/data/external/samtools/cram_query_sorted.fasta", Charset.defaultCharset());

        String[] lines = TestUtils.runGorPipeLines(String.format("gor %1$s/cram_query_sorted.cram", workDir.getRoot()));
        Assert.assertEquals(8, lines.length);
    }

    @Test
    public void readCramWithChromseqReference() throws IOException {

        File chromseqDirectory = workDir.newFolder("chromseq");
        fastaFileToChromSeq("../tests/data/external/samtools/cram_query_sorted.fasta", chromseqDirectory.toString());

        File configFile = workDir.newFile("gor-config.txt");
        FileUtils.writeStringToFile(configFile, "buildPath\t" + chromseqDirectory, Charset.defaultCharset());

        String result = TestUtils.runGorPipe("gor ../tests/data/external/samtools/cram_query_sorted.cram",
                "-config", configFile.toString());
        String[] lines = result.split("\n");
        Assert.assertEquals(8, lines.length);
    }

    private void copyFiles(String baseDirectory, String destinationDirectory, String[] filesToCopy) throws IOException {
        for (String fileName : filesToCopy) {
            File sourceFile = new File(baseDirectory, fileName);
            File destinationFile = new File(destinationDirectory, fileName);
            FileUtils.copyFile(sourceFile, destinationFile);
        }
    }

    private void fastaFileToChromSeq(String fastaFile, String chromseqDirectory) throws IOException {
        List<String> lines = FileUtils.readLines(new File(fastaFile), Charset.defaultCharset());
        String chromosome = "";

        for (String line : lines) {
            if (line.startsWith(">")) {
                chromosome = line.substring(1);
            } else {
                if (!StringUtil.isEmpty(chromosome)) {
                    String fileName = chromosome + ".txt";
                    FileUtils.writeStringToFile(new File(chromseqDirectory, fileName), line, Charset.defaultCharset(), true);
                }
            }
        }
    }


}
