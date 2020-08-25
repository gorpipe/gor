package org.gorpipe.gor.driver.bgenreader;

import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.datatypes.bgen.BGenFile;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.model.RowBase;
import org.gorpipe.gor.driver.bgen.BGenWriter;
import org.gorpipe.gor.driver.bgen.BGenWriterFactory;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

public class UTestBGenIterator {

    @ClassRule
    public static TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void test_basic() throws Exception {
        final File file = workDir.newFile("basic.bgen");
        final BGenWriter bGenWriter = BGenWriterFactory.getBGenWriter(file.getAbsolutePath(), false, false, 2, 3, 4, 5, 6);
        bGenWriter.write(new RowBase("chr1\t1\tA\tC\trsid1\tvarid1\t0123"));
        bGenWriter.write(new RowBase("chr1\t2\tA\tC\trsid2\tvarid2\t1230"));
        bGenWriter.write(new RowBase("chr1\t3\tA\tC\trsid3\tvarid3\t2301"));
        bGenWriter.write(new RowBase("chr1\t4\tA\tC\trsid4\tvarid4\t3012"));
        bGenWriter.close();

        final Iterator<String> results = Arrays.stream(("chr1\t1\tA\tC\trsid1\tvarid1\t~~!~~!  \n" +
                "chr1\t2\tA\tC\trsid2\tvarid2\t!~~!  ~!\n" +
                "chr1\t3\tA\tC\trsid3\tvarid3\t~!  ~!~~\n" +
                "chr1\t4\tA\tC\trsid4\tvarid4\t  ~!~~!~").split("\n")).iterator();

        final BGenFile bgenFile = new BGenFile(new FileSource(new SourceReference(file.getAbsolutePath())), new FileSource(new SourceReference(file.getAbsolutePath() + ".bgi")));
        final BGenIterator bGenIterator = new BGenIterator(bgenFile);

        bGenIterator.forEachRemaining(actualRow -> {
            Assert.assertTrue(results.hasNext());
            Assert.assertEquals(results.next(), actualRow.toString());
        });
        Assert.assertFalse(results.hasNext());
    }

    @Test
    public void test_seek() throws Exception {
        final File file = workDir.newFile("seek.bgen");
        final BGenWriter bGenWriter = BGenWriterFactory.getBGenWriter(file.getAbsolutePath(), false, false, 2, 3, 4, 5, 6);
        bGenWriter.write(new RowBase("chr1\t1\tA\tC\trsid1\tvarid1\t0123"));
        bGenWriter.write(new RowBase("chr1\t2\tA\tC\trsid2\tvarid2\t1230"));
        bGenWriter.write(new RowBase("chr1\t3\tA\tC\trsid3\tvarid3\t2301"));
        bGenWriter.write(new RowBase("chr1\t4\tA\tC\trsid4\tvarid4\t3012"));
        bGenWriter.close();

        final Iterator<String> results = Arrays.stream(("chr1\t3\tA\tC\trsid3\tvarid3\t~!  ~!~~\n" +
                "chr1\t4\tA\tC\trsid4\tvarid4\t  ~!~~!~").split("\n")).iterator();

        final BGenFile bgenFile = new BGenFile(new FileSource(new SourceReference(file.getAbsolutePath())), new FileSource(new SourceReference(file.getAbsolutePath() + ".bgi")));
        final BGenIterator bGenIterator = new BGenIterator(bgenFile);

        Assert.assertTrue(bGenIterator.seek("1", 3));

        bGenIterator.forEachRemaining(actualRow -> {
            Assert.assertTrue(results.hasNext());
            Assert.assertEquals(results.next(), actualRow.toString());
        });
        Assert.assertFalse(results.hasNext());
    }

    @Test
    public void test_layoutTypeOne() {
        final String path = "../tests/data/external/bgen/test_layout_type_1.bgen";
        final BGenFile bgenFile = new BGenFile(new FileSource(new SourceReference(path)), new FileSource(new SourceReference(path + ".bgi")));
        final BGenIterator bGenIterator = new BGenIterator(bgenFile);
        final String header = bGenIterator.getHeader();
        Assert.assertEquals(7, countCols(header));

        int count = 0;
        while (bGenIterator.hasNext()) {
            final String nextRow = bGenIterator.next().toString();
            Assert.assertEquals(7, countCols(nextRow));
            validateProbabilities(nextRow, 940);
            count++;
        }
        Assert.assertEquals(10, count);
    }

    private static int countCols(String row) {
        int count = 1;
        for (int i = 0; i < row.length(); ++i) {
            if (row.charAt(i) == '\t') count++;
        }
        return count;
    }

    private static void validateProbabilities(String row, int ns) {
        final int valueColOffset = row.lastIndexOf('\t') + 1;
        Assert.assertEquals(2 * ns, row.length() - valueColOffset);
        for (int i = 0; i < ns; ++i) {
            final char c0 = row.charAt(valueColOffset + 2 * i);
            final char c1 = row.charAt(valueColOffset + 2 * i + 1);
            if (c0 != ' ' || c1 != ' ') {
                final float p1 = 1f - (c0 - 33f) / 93f;
                final float p2 = 1f - (c1 - 33f) / 93f;
                Assert.assertTrue(p1 < 1.01);
                Assert.assertTrue(p2 < 1.01);
                Assert.assertTrue(p1 + p2 < 1.01);
            }
        }
    }
}
