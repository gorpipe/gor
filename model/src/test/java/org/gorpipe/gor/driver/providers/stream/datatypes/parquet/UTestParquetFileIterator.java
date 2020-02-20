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
import org.junit.Test;
import scala.collection.mutable.ListBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;


public class UTestParquetFileIterator {

    static {
        //suppres excessive from apache libs
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
        String[] expectedHeaders = Arrays.asList("Chrom", "POS", "reference", "allele", "differentrsIDs").toArray(new String[5]);
        assertEquals(expectedHeaders, iterator.getHeader());
    }

    @Test
    public void shouldReadParquetHeaderWithNor() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        GorSession gorSession = new GorSession("dummy");
        gorSession.setNorContext(true);
        String[] expectedHeaders = Arrays.asList("Chrom", "POS", "reference", "allele", "differentrsIDs").toArray(new String[5]);
        assertEquals(expectedHeaders, iterator.getHeader());
    }

    @Test
    public void doesNotSupportSeek() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        assertFalse(iterator.seek("chr22", 0));
    }

    @Test
    public void shouldHandleSelectCommand() {
        StreamSourceFile file = createStreamSourceFile("../tests/data/parquet/dbsnp_test.parquet");
        ParquetFileIterator iterator = new ParquetFileIterator(file);
        String[] header = iterator.getHeader();

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
        String[] header = iterator.getHeader();
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
    public void testMergeWithParuet() {
        TestUtils.runGorPipe("gor ../tests/data/parquet/dbsnp_test.parquet ../tests/data/gor/dbsnp_test.gor | top 10");
    }

    private StreamSourceFile createStreamSourceFile(String fileUrl) {
        SourceReference sourceReference = new SourceReference(fileUrl);
        StreamSource fileSource = new FileSource(sourceReference);
        return new StreamSourceFile(fileSource);
    }

}