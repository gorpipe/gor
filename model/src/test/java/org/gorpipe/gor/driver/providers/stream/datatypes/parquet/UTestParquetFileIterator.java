package org.gorpipe.gor.driver.providers.stream.datatypes.parquet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.gorpipe.gor.GorSession;
import org.gorpipe.model.genome.files.binsearch.GorZipLexOutputStream;
import org.gorpipe.model.genome.files.gor.NorParquetLine;
import org.gorpipe.model.genome.files.gor.ParquetLine;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import gorsat.Analysis.Select2;
import gorsat.Outputs.ToList;
import gorsat.TestUtils;
import org.aeonbits.owner.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import scala.collection.mutable.ListBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class UTestParquetFileIterator {

    static {
        //suppress excessive logging from apache libs
        LoggerContext logContext = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        logContext.getLogger("org.apache").setLevel(Level.WARN);
    }

    @Test
    public void shouldReadMultifileData() {
        //This parquet data is a duplication of the first dataset - hence 96 rows.
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test2.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.getHeader();
        int count = 0;
        Row lastRow = null;
        while (iterator.hasNext()) {
            lastRow = iterator.next();
            count++;
        }
        assertEquals(96, count);
        assertEquals(ParquetLine.class, lastRow.getClass());
        String expected = "chrY\t10069\tT\tA\trs111065272";
        assertEquals(expected, lastRow.getAllCols().toString());
    }



    @Test
    public void shouldReadParquetData() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.getHeader();
        int count = 0;
        Row lastRow = null;
        while (iterator.hasNext()) {
            lastRow = iterator.next();
            count++;
        }
        assertEquals(48, count);
        assertEquals(ParquetLine.class, lastRow.getClass());
        String expected = "chrY\t10069\tT\tA\trs111065272";
        assertEquals(expected, lastRow.getAllCols().toString());
    }

    @Test
    public void shouldReadParquetNorData() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        GorSession gorSession = new GorSession("dummy");
        gorSession.setNorContext(true);
        iterator.init(gorSession);
        int count = 0;
        Row lastRow = null;
        while (iterator.hasNext()) {
            lastRow = iterator.next();
            count++;
        }
        assertEquals(48, count);
        assertEquals(NorParquetLine.class, lastRow.getClass());
        String expected = "chrY\t10069\tT\tA\trs111065272";
        assertEquals(expected, lastRow.getAllCols().toString());
    }

    @Test(expected = GorSystemException.class)
    public void shouldFailOnFileNotExistsWhenGettingHeader() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parakeet/dbsnp_test.parakeet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.getHeader();
    }

    @Test
    public void shouldReadParquetHeader() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        String expectedHeader = "Chrom\tPOS\treference\tallele\tdifferentrsIDs";
        assertEquals(expectedHeader, iterator.getHeader());
    }

    @Test
    public void shouldReadParquetHeaderWithNor() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        GorSession gorSession = new GorSession("dummy");
        gorSession.setNorContext(true);
        String expectedHeader = "Chrom\tPOS\treference\tallele\tdifferentrsIDs";
        assertEquals(expectedHeader, iterator.getHeader());
    }

    @Test
    public void doesSupportSeek() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        GorSession gorSession = new GorSession("dummy");
        iterator.init(gorSession);

        Assert.assertTrue(iterator.seek("chr22", 0));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("Gor line after seek not correct", "chr22\t16050036\tA\tC\trs374742143", iterator.next().toString());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("Gor line after seek not correct", "chr22\t16050527\tC\tA\trs587769434", iterator.next().toString());
    }

    @Test
    public void seekOnNoneExistingChrom() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        GorSession gorSession = new GorSession("dummy");
        iterator.init(gorSession);

        Assert.assertTrue(iterator.seek("chr23", 0));
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void seekOnChromNotInFile() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        GorSession gorSession = new GorSession("dummy");
        iterator.init(gorSession);

        Assert.assertTrue(iterator.seek("chrM", 0));
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldHandleSelectCommand() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.getHeader();

        Select2 select = Select2.apply(scala.collection.JavaConverters.asScalaBuffer(Collections.list(1, 2, 3, 4)));
        ListBuffer<Row> buff = new ListBuffer();
        ToList toList = ToList.apply(buff);
        select.$bar(toList);

        while (iterator.hasNext()) {
            Row row = iterator.next();
            select.process(row);
        }

        assertEquals(48, scala.collection.JavaConverters.bufferAsJavaList(buff).size());
    }

    @Test
    public void shouldHandleGorZipLexOutputCommand() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.getHeader();
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            GorZipLexOutputStream gorzip = new GorZipLexOutputStream(byteStream, false, null);

            while (iterator.hasNext()) {
                Row row = iterator.next();
                gorzip.write(row);
            }
            gorzip.flush();

            //we expect 24 (not 48) since the parque data is generated from
            //       tests/data/gor/dbsnp_test.gorz
            assertEquals(24, byteStream.toString().split("\n").length);
        } catch (IOException e) {
            fail("IOException not expected on memory stream " + e.getMessage());
        }
    }

    @Test
    public void testPushdownWhere() {
        String result = TestUtils.runGorPipe("gor ../tests/data/parquet/dbsnp_test.parquet | where Chrom = 'chr12'");
        Assert.assertEquals("Wrong result from parquet pushdown query", "Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                "chr12\t60162\tC\tG\trs544101329\n" +
                "chr12\t60545\tA\tT\trs570991495\n", result);
    }

    @Test
    public void testPushdownWhereCustomColumn() {
        String result = TestUtils.runGorPipe("gor ../tests/data/parquet/dbsnp_test.parquet | where Chrom = 'chrX' | where differentrsIDs > 'rs6'");
        Assert.assertEquals("Wrong result from parquet pushdown query", "Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                "chrX\t2699625\tA\tG\trs6655038\n", result);
    }

    @Test
    public void testPushdownWhereNumeric() {
        String result = TestUtils.runGorPipe("gor ../tests/data/parquet/dbsnp_test.parquet | where pos < 10000");
        Assert.assertEquals("Wrong result from parquet pushdown query", "Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                "chr17\t186\tG\tA\trs547289895\n" +
                "chr17\t460\tG\tA\trs554808397\n", result);
    }

    @Test
    public void testPushdownWhereInSet() {
        String result = TestUtils.runGorPipe("gor ../tests/data/parquet/dbsnp_test.parquet | where differentrsIDs in ('rs547289895','rs554808397')");
        Assert.assertEquals("Wrong result from parquet pushdown query", "Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                "chr17\t186\tG\tA\trs547289895\n" +
                "chr17\t460\tG\tA\trs554808397\n", result);
    }

    @Test
    public void testParquetFiltering() {
        String result = TestUtils.runGorPipe("gor -f 'rs547289895' ../tests/data/parquet/dbsnp_test.parquet");
        Assert.assertEquals("Wrong result from parquet pushdown query", "Chrom\tPOS\treference\tallele\tdifferentrsIDs\tSource\n" +
                "chr17\t186\tG\tA\trs547289895\tdbsnp_test.parquet\n", result);
    }

    @Test
    public void testParquetDictionaryFiltering() throws IOException {
        Path p = Paths.get("../tests/data/parquet/dbsnp_test.gord");
        try {
            Files.write(p, ("dbsnp_test.parquet\tdummy\tchr1\t0\tchr1\t1000000000\trs367896724,rs199706086\n" +
                    "dbsnp_test.parquet\tdummy\tchr2\t0\tchr2\t1000000000\trs536478188,rs370414480\n").getBytes());
            String result = TestUtils.runGorPipe("gor -p chr1 -f 'rs199706086','rs370414480' ../tests/data/parquet/dbsnp_test.gord");
            Assert.assertEquals("Wrong result from parquet pushdown query", "Chrom\tPOS\treference\tallele\tdifferentrsIDs\tSource\n" +
                    "chr1\t10250\tA\tC\trs199706086\tdummy\n", result);
        } finally {
            if (Files.exists(p)) Files.delete(p);
        }
    }

    @Test
    public void testJoinWithParquetFiles() {
        String expectedResult = TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | join -snpsnp ../tests/data/gor/dbsnp_test.gor");
        String result = TestUtils.runGorPipe("gor ../tests/data/parquet/dbsnp_test.parquet | join -snpsnp ../tests/data/parquet/dbsnp_test.parquet");
        Assert.assertEquals("Join results not the same",expectedResult,result);
    }

    @Test
    public void testJoinWithNestedParquetFiles() {
        String expectedResult = TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | join -snpsnp ../tests/data/gor/dbsnp_test.gor");
        String result = TestUtils.runGorPipe("gor ../tests/data/parquet/dbsnp_test.parquet | join -snpsnp <(gor ../tests/data/parquet/dbsnp_test.parquet)");
        Assert.assertEquals("Join results not the same",expectedResult,result);
    }

    @Test
    public void testMergeWithParuet() {
        TestUtils.runGorPipe("gor ../tests/data/parquet/dbsnp_test.parquet ../tests/data/gor/dbsnp_test.gor | top 10");
    }

    @Test
    public void testParquetWrite() throws IOException {
        Path parquetWrite = Files.createTempFile("write",".parquet");
        try {
            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write " + parquetWrite.toAbsolutePath().toString());
            String rereadResult = TestUtils.runGorPipe("gor "+parquetWrite.toAbsolutePath().toString()+" | top 1");
            Assert.assertEquals("Wrong content in written parquet file", "Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                    "chr1\t10179\tC\tCC\trs367896724\n", rereadResult);
        } finally {
            if(Files.exists(parquetWrite)) Files.delete(parquetWrite);
        }
    }

    @Test
    public void testNorParquetWrite() throws IOException {
        Path parquetWrite = Files.createTempFile("write",".parquet");
        try {
            TestUtils.runGorPipe("nor ../tests/data/gor/dbsnp_test.gor | sort -c differentrsIDs | write " + parquetWrite.toAbsolutePath().toString());
            String rereadResult = TestUtils.runGorPipe("gor "+parquetWrite.toAbsolutePath().toString()+" | top 1");
            Assert.assertEquals("Wrong content in written parquet file", "Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                    "chrY\t10069\tT\tA\trs111065272\n", rereadResult);
        } finally {
            if(Files.exists(parquetWrite)) Files.delete(parquetWrite);
        }
    }

    private StreamSourceFile createStreamSourceFile(String fileUrl) {
        SourceReference sourceReference = new SourceReference(fileUrl);
        StreamSource fileSource = new FileSource(sourceReference);
        return new StreamSourceFile(fileSource);
    }

}