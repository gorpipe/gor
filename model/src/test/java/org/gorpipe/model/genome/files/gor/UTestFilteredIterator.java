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

package org.gorpipe.model.genome.files.gor;

import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.model.genome.files.binsearch.GorSeekableIterator;
import org.gorpipe.model.genome.files.gor.filters.EqFilter;
import org.gorpipe.model.genome.files.gor.filters.RowFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

public class UTestFilteredIterator {

    public static File tmpDir;

    @BeforeClass
    public static void setup() throws IOException {
        tmpDir = Files.createTempDirectory("utestfileiterator").toFile();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.deleteDirectory(tmpDir);
    }

    public static String writeFile(int nPos, int nLabels) throws IOException {
        final File file = new File(tmpDir, "test.gor");
        final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write("CHROM\tPOS\tLABEL\n");
        final String[] chrs = IntStream.range(1, 22).mapToObj(i -> "chr" + i).sorted().toArray(String[]::new);
        for (final String chr : chrs) {
            for (int pos = 1; pos <= nPos; ++pos) {
                for (int label = 1; label <= nLabels; ++label) {
                    bw.write(chr + "\t" + pos + "\t" + label + "\n");
                }
            }
        }
        bw.close();
        return file.getAbsolutePath();
    }

    @Test
    public void testStream() throws IOException {
        final RowFilter rf = getModulusFilter(2);
        final GenomicIterator git = getIterator(writeFile(1, 2), rf);
        final String wanted = "chr1\t1\t1\n" +
                "chr10\t1\t1\n" +
                "chr11\t1\t1\n" +
                "chr12\t1\t1\n" +
                "chr13\t1\t1\n" +
                "chr14\t1\t1\n" +
                "chr15\t1\t1\n" +
                "chr16\t1\t1\n" +
                "chr17\t1\t1\n" +
                "chr18\t1\t1\n" +
                "chr19\t1\t1\n" +
                "chr2\t1\t1\n" +
                "chr20\t1\t1\n" +
                "chr21\t1\t1\n" +
                "chr3\t1\t1\n" +
                "chr4\t1\t1\n" +
                "chr5\t1\t1\n" +
                "chr6\t1\t1\n" +
                "chr7\t1\t1\n" +
                "chr8\t1\t1\n" +
                "chr9\t1\t1";
        final Iterator<String> expected = Arrays.stream(wanted.split("\n")).iterator();
        while (git.hasNext()) {
            Assert.assertTrue(expected.hasNext());
            Assert.assertEquals(expected.next(), git.next().toString());
        }
        Assert.assertFalse(expected.hasNext());
    }

    @Test
    public void testEmpty() throws IOException {
        final RowFilter rf = new EqFilter(2, "-1");
        final GenomicIterator git = getIterator(writeFile(1, 2), rf);
        Assert.assertFalse(git.hasNext());
    }

    @Test
    public void testSeek() throws IOException {
        final RowFilter rf = new EqFilter(2, "2");
        final GenomicIterator git = getIterator(writeFile(1, 2), rf);
        Assert.assertTrue(git.seek("chr9", 1));
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr9\t1\t2", git.next().toString());
        Assert.assertFalse(git.hasNext());
    }

    @Test
    public void testHasNextTwice() throws IOException {
        final RowFilter rf = new EqFilter(2, "2");
        final GenomicIterator git = getIterator(writeFile(1, 2), rf);
        Assert.assertTrue(git.hasNext());
        Assert.assertTrue(git.hasNext());
        Assert.assertEquals("chr1\t1\t2", git.next().toString());
    }

    public static GenomicIterator getIterator(String path, RowFilter rf) {
        final GenomicIterator git = new GorSeekableIterator(new StreamSourceSeekableFile(new FileSource(new SourceReference(path))));
        return new FilteredIterator(git, rf);
    }

    public static RowFilter getModulusFilter(int mod) {
        return new RowFilter() {
            int counter = 0;

            @Override
            public boolean test(Row row) {
                this.counter++;
                if (this.counter == mod) {
                    this.counter = 0;
                    return false;
                } else {
                    return true;
                }
            }
        };
    }
}

