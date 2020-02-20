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

import org.gorpipe.model.genome.files.binsearch.GorZipLexOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestGorz {

    private static final int FIVE_HUNDRED_THOUSAND_LINES = 500000;

    @Test
    public void testColumnCompressionCreatesSmallerFiles() throws IOException {
        final File sourceFile = new File("../tests/data/gor/genes.gor");
        final Path tmpDir = Files.createTempDirectory("testColumnCompressionCreatesSmallerFiles");
        final File file = new File(tmpDir.toAbsolutePath() +  "/test_compressed.gorz");

        TestUtils.runGorPipe("gor " + sourceFile + " | write -c " + file.getAbsolutePath());
        assertCompressedFileSmaller(sourceFile, file);
        
        FileUtils.deleteDirectory(tmpDir.toFile());
    }

    @Test
    public void testGorzWithZStdAndZLib() throws IOException {
        final boolean systemUseZStd = Boolean.parseBoolean(System.getProperty("gor.compression.useZStd", "false"));

        System.setProperty("gor.compression.useZStd", "true");

        try {
            final Path tmpDir = Files.createTempDirectory("testGorzWithZStd");
            final String[] dbsnpTestLines = TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor").split("\n");
            final File file = new File(tmpDir.toAbsolutePath() + "/dbsnp_test_compressed.gorz");
            final File file2 = new File(tmpDir.toAbsolutePath() + "/dbsnp_test_compressed2.gorz");
            final int len = dbsnpTestLines.length;

            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write " + file.getAbsolutePath());
            String[] dbsnpTestAllegedLines = TestUtils.runGorPipe("gor " + file.getAbsolutePath()).split("\n");
            Assert.assertEquals(dbsnpTestLines.length, dbsnpTestAllegedLines.length);
            for (int i = 0; i < len; ++i) {
                Assert.assertEquals(dbsnpTestAllegedLines[i], dbsnpTestLines[i]);
            }

            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write -c " + file2.getAbsolutePath());
            dbsnpTestAllegedLines = TestUtils.runGorPipe("gor " + file2.getAbsolutePath()).split("\n");
            Assert.assertEquals(len, dbsnpTestAllegedLines.length);
            for (int i = 0; i < len; ++i) {
                Assert.assertEquals(dbsnpTestAllegedLines[i], dbsnpTestLines[i]);
            }

            System.setProperty("gor.compression.useZStd", "false");

            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write " + file.getAbsolutePath());
            dbsnpTestAllegedLines = TestUtils.runGorPipe("gor " + file.getAbsolutePath()).split("\n");
            Assert.assertEquals(len, dbsnpTestAllegedLines.length);
            for (int i = 0; i < len; ++i) {
                Assert.assertEquals(dbsnpTestAllegedLines[i], dbsnpTestLines[i]);
            }

            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write -c " + file2.getAbsolutePath());
            dbsnpTestAllegedLines = TestUtils.runGorPipe("gor " + file2.getAbsolutePath()).split("\n");
            Assert.assertEquals(len, dbsnpTestAllegedLines.length);
            for (int i = 0; i < len; ++i) {
                Assert.assertEquals(dbsnpTestAllegedLines[i], dbsnpTestLines[i]);
            }

            FileUtils.deleteDirectory(tmpDir.toFile());
        } finally {
            System.setProperty("gor.compression.useZStd", String.valueOf(systemUseZStd));
        }
    }

    private void assertCompressedFileSmaller(File sourceFile, File compressedFile) {
        Assert.assertTrue("Compressed file:" + compressedFile + " size:" + compressedFile.length() +
                        " should be smaller than source file:" + sourceFile + " size: " + sourceFile.length() +
                        " which is not compressed",
                compressedFile.length() < sourceFile.length());
    }

    @Test
    public void testGorzWithZStdAndZLib2() throws IOException {
        final boolean useZStd = Boolean.valueOf(System.getProperty("gor.compression.useZStd", "false"));

        System.setProperty("gor.compression.useZStd", "true");
        try {
            final Path tmpDir = Files.createTempDirectory("testGorzWithZStd");
            final File file = new File(tmpDir.toAbsolutePath() + "/genes_compressed.gorz");
            final File file2 = new File(tmpDir.toAbsolutePath() + "/genes_compressed2.gorz");
            final String[] genesLines = TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor").split("\n");
            final int len = genesLines.length;

            TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | write " + file.getAbsolutePath());
            String[] genesAllegedLines = TestUtils.runGorPipe("gor " + file.getAbsolutePath()).split("\n");
            assert genesAllegedLines.length == genesLines.length;
            for (int i = 0; i < len; ++i) {
                assert genesAllegedLines[i].equals(genesLines[i]);
            }

            TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | write -c " + file2.getAbsolutePath());
            genesAllegedLines = TestUtils.runGorPipe("gor " + file2.getAbsolutePath()).split("\n");
            assert genesAllegedLines.length == len;
            for (int i = 0; i < len; ++i) {
                assert genesAllegedLines[i].equals(genesLines[i]);
            }

            System.setProperty("gor.compression.useZStd", "false");

            TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | write " + file.getAbsolutePath());
            genesAllegedLines = TestUtils.runGorPipe("gor " + file.getAbsolutePath()).split("\n");
            assert genesAllegedLines.length == genesLines.length;
            for (int i = 0; i < len; ++i) {
                assert genesAllegedLines[i].equals(genesLines[i]);
            }

            TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | write -c " + file2.getAbsolutePath());
            genesAllegedLines = TestUtils.runGorPipe("gor " + file2.getAbsolutePath()).split("\n");
            assert genesAllegedLines.length == len;
            for (int i = 0; i < len; ++i) {
                assert genesAllegedLines[i].equals(genesLines[i]);
            }

            FileUtils.deleteDirectory(tmpDir.toFile());
        } finally {
            System.setProperty("gor.compression.useZStd", Boolean.toString(useZStd));
        }
    }

    @Test
    public void testZStdHugeLine() throws IOException {
        final Path tmpDir = Files.createTempDirectory("testGorzWithZStd");
        final boolean useZStd = Boolean.valueOf(System.getProperty("gor.compression.useZStd", "false"));
        System.setProperty("gor.compression.useZStd", "true");

        try {
            Path tmpfile = tmpDir.resolve("zstd.gorz");
            String tmpfileName = tmpfile.toString();
            try (GorZipLexOutputStream gorZipLexOutputStream = new GorZipLexOutputStream(tmpfileName, false)) {
                gorZipLexOutputStream.setHeader("chr\tpos\tval");
                StringBuilder line = new StringBuilder();
                line.append("chr1\t1\t");
                for (int i = 0; i < FIVE_HUNDRED_THOUSAND_LINES; i++) line.append('0');
                gorZipLexOutputStream.write(line.toString());
            }

            TestUtils.runGorPipe("gor " + tmpfileName);

            FileUtils.deleteDirectory(tmpDir.toFile());
        } finally {
            System.setProperty("gor.compression.useZStd", Boolean.toString(useZStd));
        }
    }

    @Test
    public void testReadingZLibCompressedGorzFile() {
        String query1 = "gor ../tests/data/gor/dbsnp_test.gor";
        String query2 = "gor ../tests/data/gor/dbsnp_test.gorz";
        String result1 = TestUtils.runGorPipeNoHeader(query1);
        String result2 = TestUtils.runGorPipeNoHeader(query2);
        Assert.assertEquals(result1, result2);

        TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/genes.gor", "gor ../tests/data/gor/genes.gorz");
    }
}
