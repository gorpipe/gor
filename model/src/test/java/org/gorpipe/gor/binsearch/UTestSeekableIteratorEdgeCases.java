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

package org.gorpipe.gor.binsearch;

import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.binsearch.SeekableIterator;
import org.gorpipe.gor.binsearch.StringIntKey;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class UTestSeekableIteratorEdgeCases {

    public static File workDir;
    public static File keysOnly;
    public static File windowsStyle;

    @BeforeClass
    public static void setup() throws IOException {
        workDir = Files.createTempDirectory("uTestSeekableIteratorEdgeCases").toFile();
        keysOnly = new File(workDir, "keysOnly.gor");
        windowsStyle = new File(workDir, "windowsStyle.gor");
        final BufferedWriter bw1 = new BufferedWriter(new FileWriter(keysOnly));
        final BufferedWriter bw2 = new BufferedWriter(new FileWriter(windowsStyle));
        bw1.write("CHROM\tPOS\n");
        bw2.write("CHROM\tPOS\r\n");
        IntStream.rangeClosed(1, 22).mapToObj(i -> "chr" + i).sorted()
                .flatMap(chr -> IntStream.rangeClosed(1, 10).mapToObj(i -> chr + "\t" + i))
                .forEach(line -> {
                    try {
                        bw1.write(line + "\n");
                        bw2.write(line + "\r\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        bw1.close();
        bw2.write("chrX\t1"); //No new line at end.
        bw2.close();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.deleteDirectory(workDir);
    }

    @Test
    public void test_emptyFile() throws IOException {
        final File file = new File(workDir, "empty.gor");
        final StreamSourceSeekableFile sssf = new StreamSourceSeekableFile(new FileSource(new SourceReference(file.getAbsolutePath())));
        final SeekableIterator si = new SeekableIterator(sssf, false);
        Assert.assertNull(si.getHeaderBytes());
        Assert.assertFalse(si.hasNext());

        final StreamSourceSeekableFile sssf2 = new StreamSourceSeekableFile(new FileSource(new SourceReference(file.getAbsolutePath())));
        final SeekableIterator si2 = new SeekableIterator(sssf2, true);
        Assert.assertEquals(0, si2.getHeaderBytes().length);
        Assert.assertFalse(si2.hasNext());
    }

    @Test
    public void test_noHeader() throws IOException {
        final File file = new File(workDir, "noHeader.gor");
        final String cont = "chr1\t1\t1\taaaaaaaaaaaaaaaa\n" +
                "chr1\t1\t2\tbbbbbbbbbbbbbbbb\n" +
                "chr1\t2\t1\tcccccccccccccccc\n"+
                "chr1\t3\t1\tdddddddddddddddd\n" +
                "chr1\t3\t2\teeeeeeeeeeeeeeee";
        final FileWriter fw = new FileWriter(file);
        fw.write(cont);
        fw.close();

        final StreamSourceSeekableFile sssf = new StreamSourceSeekableFile(new FileSource(new SourceReference(file.getAbsolutePath())));
        final SeekableIterator si = new SeekableIterator(sssf, false);

        Assert.assertNull(si.getHeaderBytes());
        Assert.assertNull(si.getHeader());

        si.seek(new StringIntKey("chr1", 1));
        Assert.assertTrue(si.hasNext());
        Assert.assertEquals("chr1\t1\t1\taaaaaaaaaaaaaaaa", si.getNextAsString());

        si.seek(new StringIntKey("chr1", 3));
        Assert.assertTrue(si.hasNext());
        Assert.assertEquals("chr1\t3\t1\tdddddddddddddddd", si.getNextAsString());

        si.seek(new StringIntKey("chr1", 2));
        Assert.assertTrue(si.hasNext());
        Assert.assertEquals("chr1\t2\t1\tcccccccccccccccc", si.getNextAsString());

        si.seek(new StringIntKey("zzzz", Integer.MAX_VALUE));
        Assert.assertFalse(si.hasNext());
    }

    @Test
    public void test_headerSizeEqualToBufferSize() throws IOException {
        final File file = new File(workDir,"file.gor");
        final FileOutputStream fos = new FileOutputStream(file);
        final byte[] headerBytes = new byte[64 * 1024 - 1];
        Arrays.fill(headerBytes, (byte) 'h');
        fos.write(headerBytes);
        final byte[] lineBytes = new byte[12];
        lineBytes[0] = '\n';
        lineBytes[11] = '\n';
        fos.write(lineBytes);
        fos.close();

        final StreamSourceSeekableFile sssf = new StreamSourceSeekableFile(new FileSource(new SourceReference(file.getAbsolutePath())));
        final SeekableIterator si = new SeekableIterator(sssf, true);
        Assert.assertEquals(headerBytes.length, si.getHeaderBytes().length);
        Assert.assertTrue(si.hasNext());
        Assert.assertEquals(10, si.getNextAsBytes().length);
    }

    @Test
    public void test_headerAndNothingElse() throws IOException {
        final File file = new File(workDir,"headerOnly.gor");
        final FileOutputStream fos = new FileOutputStream(file);
        final byte[] headerBytes = new byte[64 * 1024];
        Arrays.fill(headerBytes, (byte) 'h');
        fos.write(headerBytes);
        fos.close();

        final StreamSourceSeekableFile sssf = new StreamSourceSeekableFile(new FileSource(new SourceReference(file.getAbsolutePath())));
        final SeekableIterator si = new SeekableIterator(sssf, true);
        Assert.assertEquals(headerBytes.length, si.getHeaderBytes().length);
        Assert.assertFalse(si.hasNext());
    }

    @Test
    public void test_tooBigLine() throws IOException {
        final File file = new File(workDir, "tooBigLine.gor");
        final FileOutputStream fos = new FileOutputStream(file);
        final byte[] headerBytes = new byte[64 * 1024 * 1024 + 1];
        fos.write(headerBytes);
        fos.close();

        final StreamSourceSeekableFile sssf = new StreamSourceSeekableFile(new FileSource(new SourceReference(file.getAbsolutePath())));
        boolean success = false;
        try {
            new SeekableIterator(sssf, true);
        } catch (IllegalStateException e) {
            success = true;
        } catch (Exception e) {}
        Assert.assertTrue(success);
    }

    @Test
    public void testSeekAtRandomPositions() throws IOException {
        final Random r = new Random(5);
        final SeekableIterator si = new SeekableIterator(new StreamSourceSeekableFile(new FileSource(new SourceReference(keysOnly.getAbsolutePath()))), true);

        //Seek at random 1000 positions
        for (int i = 0; i < 1000; ++i) {
            final String chr = "chr" + (1 + r.nextInt(22));
            final int pos = 1 + r.nextInt(10);

            si.seek(new StringIntKey(chr, pos));
            Assert.assertTrue(si.hasNext());
            Assert.assertEquals(chr + "\t" + pos, si.getNextAsString());
        }
    }

    @Test
    public void test_WindowsStyleNewLine() throws IOException {
        final Random r = new Random(5);
        final SeekableIterator si = new SeekableIterator(new StreamSourceSeekableFile(new FileSource(new SourceReference(windowsStyle.getAbsolutePath()))), true);

        //Seek at random 1000 positions
        for (int i = 0; i < 1000; ++i) {
            final String chr = "chr" + (1 + r.nextInt(22));
            final int pos = 1 + r.nextInt(10);

            si.seek(new StringIntKey(chr, pos));
            Assert.assertTrue(si.hasNext());
            Assert.assertEquals(chr + "\t" + pos, si.getNextAsString());
        }

        si.seek(new StringIntKey("chrX", 1));
        Assert.assertTrue(si.hasNext());
        Assert.assertEquals("chrX\t1", si.getNextAsString());
    }
}
