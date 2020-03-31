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

package org.gorpipe.model.genome.files.binsearch;

import org.gorpipe.RangedNumberFormatter;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.util.collection.ByteArray;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.zip.InflaterOutputStream;

public class UTestGorZipLexOutputStream {

    @Rule
    public final TemporaryFolder tf = new TemporaryFolder();

    public static final String[] CHROMOSOMES = IntStream.rangeClosed(1, 22).mapToObj(idx -> "chr" + idx).sorted().toArray(String[]::new);

    @Test
    public void testWriteSimpleFile() throws IOException {
        final String path = tf.newFile("simpleFile.gorz").getAbsolutePath();
        final GorZipLexOutputStream os = new GorZipLexOutputStream(path, false);
        final String header = "CHROM\tPOS\tREF\tALT\tCOL";
        os.setHeader(header);
        final String otherCols = "A\tC\tblablablablabla";
        final int posPerChr = 10;
        for (String chr : CHROMOSOMES) {
            for (int pos = 0; pos < posPerChr; ++pos) {
                final Row row = RowObj.apply(chr + "\t" + pos + "\t" + otherCols);
                os.write(row);
            }
        }
        os.close();
        validateNotSoBigFile(path, header, otherCols, CHROMOSOMES, posPerChr);
    }

    @Test
    public void testWriteNoChr() throws IOException {
        final String[] chromosomes = IntStream.rangeClosed(1, 3).mapToObj(String::valueOf).toArray(String[]::new);
        final String path = tf.newFile("noChr.gorz").getAbsolutePath();
        final GorZipLexOutputStream os = new GorZipLexOutputStream(path, false);
        final String header = "CHROM\tPOS\tREF\tALT\tCOL";
        os.setHeader(header);
        final String otherCols = "A\tC\tblablablablabla";
        final int posPerChr = 10;
        for (String chr : chromosomes) {
            for (int pos = 0; pos < posPerChr; ++pos) {
                final Row row = RowObj.apply(chr + "\t" + pos + "\t" + otherCols);
                os.write(row);
            }
        }
        os.close();
        validateNotSoBigFile(path, header, otherCols, chromosomes, posPerChr);
    }

    @Test
    public void testWriteNoOtherCols() throws IOException {
        final String path = tf.newFile("noOtherCols.gorz").getAbsolutePath();
        final GorZipLexOutputStream os = new GorZipLexOutputStream(path, false);
        final String header = "CHROM\tPOS";
        os.setHeader(header);
        final int posPerChr = 10;
        for (String chr : CHROMOSOMES) {
            for (int pos = 0; pos < posPerChr; ++pos) {
                final Row row = RowObj.apply(chr + "\t" + pos);
                os.write(row);
            }
        }
        os.close();
        validateNotSoBigFile(path, header, "", CHROMOSOMES, posPerChr);
    }

    @Test
    public void testWriteLongLines() throws IOException {
        final String path = tf.newFile("longLines.gorz").getAbsolutePath();
        final GorZipLexOutputStream os = new GorZipLexOutputStream(path, false);
        final String header = "CHROM\tPOS\tCOL";
        final byte[] otherColBytes = new byte[256 * 1024];
        Random random = new Random();
        for (int i = 0; i < otherColBytes.length; i++) {
            otherColBytes[i] = (byte) (random.nextInt(64)+33);
        }
        final String otherCol = new String(otherColBytes);
        os.setHeader(header);
        final int posPerChr = 2;
        for (String chr : CHROMOSOMES) {
            for (int pos = 0; pos < posPerChr; ++pos) {
                final Row row = RowObj.apply(chr + "\t" + pos+ "\t" + otherCol);
                os.write(row);
            }
        }
        os.close();
        validateNotSoBigFile(path, header, otherCol, CHROMOSOMES, posPerChr);
    }

    @Test
    public void testEmptyKey() throws IOException {
        final String path = tf.newFile("emptyKey.gorz").getAbsolutePath();
        final GorZipLexOutputStream os = new GorZipLexOutputStream(path, false);
        final String header = "CHROM\tPOS\tCOL";
        final String otherCol = "blablabla";
        os.setHeader(header);
        final int posPerChr = 2;
        final String[] chrs = new String[]{""};
        for (String chr : chrs) {
            for (int pos = 0; pos < posPerChr; ++pos) {
                final Row row = RowObj.apply(chr + "\t" + pos+ "\t" + otherCol);
                os.write(row);
            }
        }
        os.close();
        validateNotSoBigFile(path, header, otherCol, chrs, posPerChr);
    }

    @Test
    public void testWrite() throws IOException {
        final GorZipLexOutputStream os = new GorZipLexOutputStream(tf.newFile("dummy").getAbsolutePath(), false);
        boolean success = false;
        try {
            os.write(0);
        } catch (IOException e) {
            success = true;
        } catch (Exception e) {
            //Failed :(
        }
        Assert.assertTrue(success);
    }


    private void validateNotSoBigFile(String path, String header, String otherCols, String[] chromosomes, int posPerChr) throws IOException {
        final byte[] buffer = readAllFile(path);
        int begin = validateHeader(buffer, header);
        int count = 0;
        while (begin < buffer.length) {
            final int end = ArrayUtils.indexOf(buffer, (byte) '\n', begin);
            final String key = getKey(buffer, begin, end);
            int idx = begin + key.length() + 1;
            Assert.assertEquals(0, buffer[idx++]);
            final byte[] unzipped = unzipBlock(buffer, idx, end);
            count = validateBlock(unzipped, count, posPerChr, key, chromosomes, otherCols);
            begin = end + 1;
        }
    }

    private byte[] unzipBlock(byte[] buffer, int offset, int end) throws IOException {
        final int len = ByteArray.to8BitInplace(buffer, offset, end - offset);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InflaterOutputStream decOs = new InflaterOutputStream(baos);
        decOs.write(buffer, offset, len);
        decOs.flush();
        return baos.toByteArray();
    }

    private byte[] readAllFile(String path) throws IOException {
        final File file = new File(path);
        final int fileLen = (int) file.length(); //This is not a big file.
        final InputStream is = new FileInputStream(path);
        final byte[] buffer = new byte[fileLen];
        int bytesRead = 0;
        while (bytesRead < fileLen) {
            bytesRead += is.read(buffer);
        }
        return buffer;
    }

    private int endOfKey(byte[] buffer, int offset, int upTo) {
        int idx = offset;
        while (buffer[idx++] != '\t');
        while (idx < upTo) {
            if (buffer[idx] == '\t') return idx;
            ++idx;
        }
        return -1;
    }

    private int validateBlock(byte[] block, int count, int posPerChr, String key, String[] chromosomes, String otherCols) {
        int beginOfLine = 0;
        String actualKey = null;
        while (beginOfLine < block.length) {
            final int endOfLine = ArrayUtils.indexOf(block, (byte) '\n', beginOfLine);
            actualKey = validateKey(block, beginOfLine, endOfLine, count, posPerChr, chromosomes);
            validateOtherCols(block, beginOfLine + actualKey.length() + 1, endOfLine, otherCols);
            beginOfLine = endOfLine + 1;
            ++count;
        }
        Assert.assertEquals(key, actualKey);
        return count;
    }

    private int validateHeader(byte[] buffer, String header) {
        int end = ArrayUtils.indexOf(buffer, (byte) '\n');
        final String writtenHeader = new String(buffer, 0, end);
        Assert.assertEquals(header, writtenHeader);
        return end + 1;
    }

    private String validateKey(byte[] block, int beginOfLine, int endOfLine, int count, int posPerChr, String[] chromosomes) {
        final String actualKey = getKey(block, beginOfLine, endOfLine);
        final String wantedKey = chromosomes[count / posPerChr] + "\t" + (count % posPerChr);
        Assert.assertEquals(wantedKey, actualKey);
        return actualKey;
    }

    private String getKey(byte[] buffer, int offset, int upTo) {
        final int endOfKey = endOfKey(buffer, offset, upTo);
        return endOfKey == -1 ? new String(buffer, offset, upTo - offset) : new String(buffer, offset, endOfKey - offset);
    }

    private void validateOtherCols(byte[] buffer, int begin, int end, String otherCols) {
        final String actualOtherCols = begin > end ? "" : new String(buffer, begin, end - begin);
        Assert.assertEquals(otherCols, actualOtherCols);
    }
}
