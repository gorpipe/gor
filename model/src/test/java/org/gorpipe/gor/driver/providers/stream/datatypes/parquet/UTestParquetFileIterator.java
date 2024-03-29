package org.gorpipe.gor.driver.providers.stream.datatypes.parquet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import gorsat.Commands.Analysis;
import gorsat.ScalaTestUtils;
import gorsat.TestUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.binsearch.GorZipLexOutputStream;
import org.gorpipe.gor.model.ParquetLine;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.gor.session.SystemContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.Tuple2;
import scala.collection.mutable.ListBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UTestParquetFileIterator {

    static {
        //suppress excessive logging from apache libs
        LoggerContext logContext = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        logContext.getLogger("org.apache").setLevel(Level.WARN);
    }

    GorSession gorSession;
    GorSession norSession;

    @Before
    public void setup() {
        gorSession = new GorSession("dummy");
        norSession = new GorSession("dummy");
        DriverBackedFileReader fileReader = new DriverBackedFileReader("", Path.of(".").toAbsolutePath().toString());
        ProjectContext projectContext = new ProjectContext.Builder().setFileReader(fileReader).build();
        SystemContext systemContext = new SystemContext.Builder().build();
        gorSession.init(projectContext, systemContext, null);
        norSession.init(projectContext, systemContext, null);
        norSession.setNorContext(true);
    }

    @After
    public void close() {
        gorSession.close();
    }

    @Test
    public void shouldReadMultifileData() throws IOException {
        //This parquet data is a duplication of the first dataset - hence 96 rows.
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test2.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(gorSession);
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
    public void shouldReadParquetData() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(gorSession);
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
    public void shouldReadParquetNorData() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(norSession);
        int count = 0;
        Row lastRow = null;
        while (iterator.hasNext()) {
            lastRow = iterator.next();
            count++;
        }
        assertEquals(48, count);
        assertEquals(lastRow.chr,"chrN");
        String expected = "chrY\t10069\tT\tA\trs111065272";
        assertEquals(expected, lastRow.otherCols());
    }

    @Test(expected = GorSystemException.class)
    public void shouldFailOnFileNotExistsWhenGettingHeader() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parakeet/dbsnp_test.parakeet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(gorSession);
        iterator.getHeader();
    }

    @Test
    public void shouldReadParquetHeader() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(gorSession);
        String expectedHeader = "Chrom\tPOS\treference\tallele\tdifferentrsIDs";
        assertEquals(expectedHeader, iterator.getHeader());
    }

    @Test
    public void shouldReadParquetHeaderWithNor() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(norSession);
        String expectedHeader = "ChromNOR\tPosNOR\tChrom\tPOS\treference\tallele\tdifferentrsIDs";
        assertEquals(expectedHeader, iterator.getHeader());
    }

    @Test
    public void doesSupportSeek() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(gorSession);

        Assert.assertTrue(iterator.seek("chr22", 0));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("Gor line after seek not correct", "chr22\t16050036\tA\tC\trs374742143", iterator.next().toString());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("Gor line after seek not correct", "chr22\t16050527\tC\tA\trs587769434", iterator.next().toString());
    }

    @Test
    public void seekOnNoneExistingChrom() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(gorSession);

        Assert.assertTrue(iterator.seek("chr23", 0));
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void seekOnChromNotInFile() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(gorSession);

        Assert.assertTrue(iterator.seek("chrM", 0));
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldHandleSelectCommand() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(gorSession);
        iterator.getHeader();

        Tuple2<Analysis,ListBuffer<Row>> selectList = ScalaTestUtils.selectToList();
        Analysis select = selectList._1;
        ListBuffer<Row> buff = selectList._2;

        while (iterator.hasNext()) {
            Row row = iterator.next();
            select.process(row);
        }

        assertEquals(48, buff.size());
    }

    @Test
    public void shouldHandleGorZipLexOutputCommand() throws IOException {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        iterator.init(gorSession);
        iterator.getHeader();
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            GorZipLexOutputStream gorzip = new GorZipLexOutputStream(byteStream, false, false, null);

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
    public void testParquetBigInt() throws IOException {
        String parqB64 = "UEFSMRUAFSwVLiwVBBUAFQYVCBwYEAAAAAAAAAACYp9m4MUwAAAYEAAAAAAAAAACYp9m4MUwAAAWAigQAAAAAAAAAAJin2bgxTAAABgQAAAAAAAAAAJin2bgxTAAAAAAABYQAgAAAAMBBTAAAAAAAmKfZuDFMAAAFQIZLEgMc3Bhcmtfc2NoZW1hFQIAFQ4VIBUCGA92YWx1ZV9hc19udW1iZXIlChUkFUwAFgQZHBkcJggcFQ4ZNQAGCBkYD3ZhbHVlX2FzX251bWJlchUCFgQW5gEW6AEmCDwYEAAAAAAAAAACYp9m4MUwAAAYEAAAAAAAAAACYp9m4MUwAAAWAigQAAAAAAAAAAJin2bgxTAAABgQAAAAAAAAAAJin2bgxTAAAAAZHBUAFQAVAgAAABbmARYEABksGBhvcmcuYXBhY2hlLnNwYXJrLnZlcnNpb24YBTMuMS4yABgpb3JnLmFwYWNoZS5zcGFyay5zcWwucGFycXVldC5yb3cubWV0YWRhdGEYbXsidHlwZSI6InN0cnVjdCIsImZpZWxkcyI6W3sibmFtZSI6InZhbHVlX2FzX251bWJlciIsInR5cGUiOiJkZWNpbWFsKDM4LDE4KSIsIm51bGxhYmxlIjp0cnVlLCJtZXRhZGF0YSI6e319XX0AGEpwYXJxdWV0LW1yIHZlcnNpb24gMS4xMC4xIChidWlsZCBhODlkZjhmOTkzMmI2ZWY2NjMzZDA2MDY5ZTUwYzliNzk3MGJlYmQxKRkcHAAAAM8BAABQQVIx";
        var bb = Base64.getDecoder().decode(parqB64);
        var p = Paths.get("my.parquet");
        try {
            Files.write(p, bb);
            String res = TestUtils.runGorPipe("nor my.parquet");
            Assert.assertEquals("ChromNOR\tPosNOR\tvalue_as_number\n" +
                    "chrN\t0\t\n" +
                    "chrN\t0\t44.000000000000000000\n", res);
        } finally {
            Files.delete(p);
        }
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
            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write " + parquetWrite.toAbsolutePath());
            String rereadResult = TestUtils.runGorPipe("gor "+ parquetWrite.toAbsolutePath() +" | top 1");
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
            TestUtils.runGorPipe("nor ../tests/data/gor/dbsnp_test.gor | sort -c differentrsIDs | write " + parquetWrite.toAbsolutePath());
            String rereadResult = TestUtils.runGorPipe("gor "+ parquetWrite.toAbsolutePath() +" | top 1");
            Assert.assertEquals("Wrong content in written parquet file", "Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                    "chrY\t10069\tT\tA\trs111065272\n", rereadResult);
        } finally {
            if(Files.exists(parquetWrite)) Files.delete(parquetWrite);
        }
    }

    @Test
    public void testPartitionedParquet() throws IOException {
        Path tmpdir = null;
        try {
            tmpdir = Files.createTempDirectory("mu");
            Path tmprparquet = tmpdir.resolve("forkr.parquet");
            Path tmpparquet = tmpdir.resolve("fork.parquet");
            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write -r -f Chrom -d " + tmprparquet);
            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write -f Chrom -d " + tmpparquet);
            String resultr = TestUtils.runGorPipe("gor "+ tmprparquet);
            String result = TestUtils.runGorPipe("gor "+ tmpparquet);
            Assert.assertEquals("wrong result from partitioned parquet folder",resultr,result);
        } finally {
            if(tmpdir!=null) Files.walk(tmpdir).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch(Exception e) {

                }
            });
        }
    }

    @Test
    public void testPartitionedParquetCustomCol() throws IOException {
        Path tmpdir = null;
        try {
            tmpdir = Files.createTempDirectory("mu");
            Path tmprparquet = tmpdir.resolve("forkr.parquet");
            Path tmpparquet = tmpdir.resolve("fork.parquet");
            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write -r -f differentrsIDs -d " + tmprparquet);
            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write -f differentrsIDs -d " + tmpparquet);
            String resultr = TestUtils.runGorPipe("gor "+ tmprparquet);
            String result = TestUtils.runGorPipe("gor "+ tmpparquet);
            Assert.assertEquals("wrong result from partitioned parquet folder",resultr,result);
        } finally {
            if(tmpdir!=null) Files.walk(tmpdir).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch(Exception e) {

                }
            });
        }
    }

    @Test
    public void testPartitionedParquetNor() throws IOException {
        Path tmpdir = null;
        try {
            tmpdir = Files.createTempDirectory("mu");
            Path tmprparquet = tmpdir.resolve("forkr.parquet");
            Path tmpparquet = tmpdir.resolve("fork.parquet");
            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write -r -f differentrsIDs -d " + tmprparquet);
            TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write -f differentrsIDs -d " + tmpparquet);
            String resultr = TestUtils.runGorPipe("nor "+ tmprparquet);
            String result = TestUtils.runGorPipe("nor "+ tmpparquet);
            Assert.assertEquals("wrong result from partitioned parquet folder",resultr,result);
        } finally {
            if(tmpdir!=null) Files.walk(tmpdir).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch(Exception e) {
                    // Ignore
                }
            });
        }
    }


    private StreamSourceFile createStreamSourceFile(String fileUrl) {
        SourceReference sourceReference = new SourceReference(fileUrl);
        StreamSource fileSource = new FileSource(sourceReference);
        return new StreamSourceFile(fileSource);
    }

}
