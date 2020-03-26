package org.gorpipe.gor.driver.providers.stream.datatypes.gor;

import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.Row;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class UTestGorSeekableIterator {
    private final static Logger log = LoggerFactory.getLogger(UTestGorSeekableIterator.class);

    private final String GOR_FILE = "../tests/data/gor/genes.gor";
    private final int GOR_FILE_LINECOUNT = 51776;

    private final String GORZ_FILE = "../tests/data/gor/genes.gorz";

    private static String[] CHROMOSOMES = IntStream.rangeClosed(1, 22).mapToObj(i -> "chr" + i).sorted().toArray(String[]::new);
    private static String BASIC_GOR_FILE;
    private static String GOR_FILE_WITH_QUITE_LONG_LINES;
    private static String GOR_FILE_WITH_LONG_LINES;
    private static String GOR_FILE_WITH_MANY_LINES;
    private static String GOR_FILE_WITH_MANY_AND_QUITE_LONG_LINES;
    private static String PATHOLOGICAL_GOR_FILE;

    private final static int NOT_SO_BIG_NUMBER = 1_000;
    private final static int QUITE_BIG_NUMBER = 25_000;
    private final static int BIG_NUMBER = 100_000;

    private final static int SEED = 5;

    @ClassRule
    public static TemporaryFolder tf = new TemporaryFolder();

    @BeforeClass
    public static void writeFiles() throws IOException {
        BASIC_GOR_FILE = writeFile("basic.gor", 10, 1, NOT_SO_BIG_NUMBER, false);
        GOR_FILE_WITH_QUITE_LONG_LINES = writeFile("quite_long_lines.gor", 10, 1, QUITE_BIG_NUMBER, false);
        GOR_FILE_WITH_LONG_LINES = writeFile("long_lines.gor",10, 1, BIG_NUMBER, false);
        GOR_FILE_WITH_MANY_LINES = writeFile("many_lines.gor", 5, 50, NOT_SO_BIG_NUMBER, false);
        GOR_FILE_WITH_MANY_AND_QUITE_LONG_LINES = writeFile("many_and_quite_long_lines.gor", 3, 30, QUITE_BIG_NUMBER, false);
        PATHOLOGICAL_GOR_FILE = writeFile("pathological.gor",5, 10, BIG_NUMBER, true);
    }

    private static String writeFile(String fileName, int posPerChr, int linesPerKey, int maxColLen, boolean skipNewLine) throws IOException {
        final Random r = new Random(SEED);
        final byte[] buffer = new byte[maxColLen];
        Arrays.fill(buffer, (byte) 'a');
        final File file = tf.newFile(fileName);
        final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        bos.write("CHROM\tPOS\tNUMBER\tSOME_COL\n".getBytes());
        for (String chr : CHROMOSOMES) {
            for (int pos = 1; pos <= posPerChr; ++pos) {
                for (int num = 0; num < linesPerKey; ++num) {
                    bos.write((chr + "\t" + pos + "\t" + num + "\t").getBytes());
                    bos.write(buffer, 0, r.nextInt(maxColLen));
                    bos.write('\n');
                }
            }
        }
        if (skipNewLine) {
            bos.write(("chrX\t1\t0\t").getBytes());
            bos.write(buffer, 0, r.nextInt(maxColLen));
        }
        bos.close();
        return file.getAbsolutePath();
    }

    private GenomicIterator getGenomicIterator(String filename) {
        Path path = Paths.get(filename);
        String absolutePath = path.toAbsolutePath().toString();
        SourceReference sourceReference = new SourceReference(absolutePath);

        GenomicIterator iterator = null;
        try {
            iterator = GorDriverFactory.fromConfig().createIterator(sourceReference);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertNotNull(iterator);

        iterator.init(null);

        // This is done in LineSource - next doesn't work correctly without this
        iterator.setColnum(iterator.getHeader().split("\t").length - 2);

        return iterator;
    }

    @Test
    public void getHeader_WhenFileIsGor() {
        GenomicIterator iterator = getGenomicIterator(GOR_FILE);
        String[] header = iterator.getHeader().split("\t");
        assertEquals(4, header.length);
        assertEquals("Chrom", header[0]);
        assertEquals("gene_start", header[1]);
        assertEquals("gene_end", header[2]);
        assertEquals("Gene_Symbol", header[3]);
    }

    @Test
    public void getHeader_WhenFileIsGorz() {
        GenomicIterator iterator = getGenomicIterator(GORZ_FILE);
        String[] header = iterator.getHeader().split("\t");
        assertEquals(4, header.length);
        assertEquals("Chrom", header[0]);
        assertEquals("gene_start", header[1]);
        assertEquals("gene_end", header[2]);
        assertEquals("Gene_Symbol", header[3]);
    }

    @Test
    public void next_ReturningRow_WhenFileIsGor_GetFirstLineOnly() {
        GenomicIterator iterator = getGenomicIterator(GOR_FILE);

        // next won't work properly if hasNext isn't called
        assertTrue(iterator.hasNext());

        Row r = iterator.next();
        assertNotNull(r);
        assertEquals("chr1\t11868\t14412\tDDX11L1", r.getAllCols().toString());
    }


    @Test
    public void next_ReturningRow_WhenFileIsGorz_GetFirstLineOnly() {
        GenomicIterator iterator = getGenomicIterator(GORZ_FILE);

        // next won't work properly if hasNext isn't called
        assertTrue(iterator.hasNext());

        Row r = iterator.next();
        assertNotNull(r);
        assertEquals("chr1\t11868\t14412\tDDX11L1", r.getAllCols().toString());
    }

    @Test
    public void hasNext_WhenFileIsGor_ShouldReturnTrueAtStart() {
        GenomicIterator iterator = getGenomicIterator(GOR_FILE);
        assertTrue(iterator.hasNext());
    }

    @Test
    public void hasNext_WhenFileIsGorz_ShouldReturnTrueAtStart() {
        GenomicIterator iterator = getGenomicIterator(GORZ_FILE);
        assertTrue(iterator.hasNext());
    }

    @Test
    public void hasNext_WhenFileIsGor_ShouldReturnFalseWhenFileIsExhausted() {
        GenomicIterator iterator = getGenomicIterator(GOR_FILE);
        int count = 0;
        while(iterator.hasNext()) {
            count++;
            if(count > GOR_FILE_LINECOUNT) {
                break;
            }
            iterator.next();
        }
        assertFalse(iterator.hasNext());
        assertEquals(GOR_FILE_LINECOUNT, count);
    }

    @Test
    public void hasNext_WhenFileIsGorz_ShouldReturnFalseWhenFileIsExhausted() {
        GenomicIterator iterator = getGenomicIterator(GORZ_FILE);
        int count = 0;
        while(iterator.hasNext()) {
            count++;
            if(count > GOR_FILE_LINECOUNT) {
                break;
            }
            iterator.next();
        }
        assertFalse(iterator.hasNext());
        assertEquals(GOR_FILE_LINECOUNT, count);
    }

    @Test
    public void seek_WhenFileIsGor_SeekToMiddleReturnsTrueWhenPositionExists() {
        GenomicIterator iterator = getGenomicIterator(GOR_FILE);
        boolean result = iterator.seek("chr16", 56463044);
        assertTrue(result);
    }

    @Test
    public void seek_WhenFileIsGor_NextReturningRowWorksAfterSeekToMiddle() {
        GenomicIterator iterator = getGenomicIterator(GOR_FILE);
        iterator.seek("chr16", 56463044);

        // next won't work properly if hasNext isn't called
        assertTrue(iterator.hasNext());

        Row r = iterator.next();
        assertNotNull(r);
        assertEquals("chr16\t56463044\t56486111\tNUDT21", r.getAllCols().toString());
    }


    @Test
    public void seek_WhenFileIsGor_NextReturningRowWorksAfterSeekToLine() {
        GenomicIterator iterator = getGenomicIterator(GOR_FILE);
        iterator.seek("chrY", 59001390);

        // next won't work properly if hasNext isn't called
        assertTrue(iterator.hasNext());

        Row r = iterator.next();
        assertNotNull(r);
        assertEquals("chrY\t59001390\t59001635\tCTBP2P1", r.getAllCols().toString());
    }

    @Test
    public void seek_WhenFileIsGorz_SeekToMiddleReturnsTrueWhenPositionExists() {
        GenomicIterator iterator = getGenomicIterator(GORZ_FILE);
        boolean result = iterator.seek("chr16", 56463044);
        assertTrue(result);
    }

    @Test
    public void seek_WhenFileIsGorz_NextReturningRowWorksAfterSeekToMiddle() {
        GenomicIterator iterator = getGenomicIterator(GORZ_FILE);
        iterator.seek("chr16", 56463044);

        // next won't work properly if hasNext isn't called
        assertTrue(iterator.hasNext());

        Row r = iterator.next();
        assertNotNull(r);
        assertEquals("chr16\t56463044\t56486111\tNUDT21", r.getAllCols().toString());
    }

    @Test
    public void seek_WhenFileIsGorz_NextReturningRowWorksAfterSeekToLine() {
        GenomicIterator iterator = getGenomicIterator(GORZ_FILE);
        iterator.seek("chrY", 59001390);

        // next won't work properly if hasNext isn't called
        assertTrue(iterator.hasNext());

        Row r = iterator.next();
        assertNotNull(r);
        assertEquals("chrY\t59001390\t59001635\tCTBP2P1", r.getAllCols().toString());
    }

    @Test
    public void streamAllFile_basicGorFile() throws IOException {
        streamAllFile(BASIC_GOR_FILE);
    }

    @Test
    public void streamAllFile_gorFileWithQuiteLongLines() throws IOException {
        streamAllFile(GOR_FILE_WITH_QUITE_LONG_LINES);
    }

    @Test
    public void streamAllFile_gorFileWithLongLines() throws IOException {
        streamAllFile(GOR_FILE_WITH_LONG_LINES);
    }

    @Test
    public void streamAllFile_gorFileWithManyLines() throws IOException {
        streamAllFile(GOR_FILE_WITH_MANY_LINES);
    }

    @Test
    public void streamAllFile_gorFileWithManyAndQuiteLongLines() throws IOException {
        streamAllFile(GOR_FILE_WITH_MANY_AND_QUITE_LONG_LINES);
    }

    @Test
    public void streamAllFile_pathologicalGorFile() throws IOException {
        streamAllFile(PATHOLOGICAL_GOR_FILE);
    }

    @Test
    public void seekAtBeginning_basicGorFile() {
        seekAtBeginning(BASIC_GOR_FILE);
    }

    @Test
    public void seekAtBeginning_gorFileWithQuiteLongLines() {
        seekAtBeginning(GOR_FILE_WITH_QUITE_LONG_LINES);
    }

    @Test
    public void seekAtBeginning_gorFileWithLongLines() {
        seekAtBeginning(GOR_FILE_WITH_LONG_LINES);
    }

    @Test
    public void seekAtBeginning_gorFileWithManyLines() {
        seekAtBeginning(GOR_FILE_WITH_MANY_LINES);
    }

    @Test
    public void seekAtBeginning_gorFileWithManyAndQuiteLongLines() {
        seekAtBeginning(GOR_FILE_WITH_MANY_AND_QUITE_LONG_LINES);
    }

    @Test
    public void seekAtBeginning_pathologicalGorFile() {
        seekAtBeginning(PATHOLOGICAL_GOR_FILE);
    }

    @Test
    public void seekBeyondEnd_basicGorFile() {
        seekBeyondEnd(BASIC_GOR_FILE);
    }

    @Test
    public void seekBeyondEnd_gorFileWithQuiteLongLines() {
        seekBeyondEnd(GOR_FILE_WITH_QUITE_LONG_LINES);
    }

    @Test
    public void seekBeyondEnd_gorFileWithLongLines() {
        seekBeyondEnd(GOR_FILE_WITH_LONG_LINES);
    }

    @Test
    public void seekBeyondEnd_gorFileWithManyLines() {
        seekBeyondEnd(GOR_FILE_WITH_MANY_LINES);
    }

    @Test
    public void seekBeyondEnd_gorFileWithManyAndQuiteLongLines() {
        seekBeyondEnd(GOR_FILE_WITH_MANY_AND_QUITE_LONG_LINES);
    }

    @Test
    public void seekBeyondEnd_pathologicalGorFile() {
        seekBeyondEnd(PATHOLOGICAL_GOR_FILE);
    }

    @Test
    public void seekToExistingPositions_basicGorFile() {
        seekToExistingPositions(BASIC_GOR_FILE, 10);
    }

    @Test
    public void seekToExistingPositions_gorFileWithQuiteLongLines() {
        seekToExistingPositions(GOR_FILE_WITH_QUITE_LONG_LINES, 10);
    }

    @Test
    public void seekToExistingPositions_gorFileWithLongLines() {
        seekToExistingPositions(GOR_FILE_WITH_LONG_LINES, 10);
    }

    @Test
    public void seekToExistingPositions_gorFileWithManyLines() {
        seekToExistingPositions(GOR_FILE_WITH_MANY_LINES, 5);
    }

    @Test
    public void seekToExistingPositions_gorFileWithManyAndQuiteLongLines() {
        seekToExistingPositions(GOR_FILE_WITH_MANY_AND_QUITE_LONG_LINES, 3);
    }

    @Test
    public void seekToExistingPositions_pathologicalGorFile() {
        seekToExistingPositions(PATHOLOGICAL_GOR_FILE, 5);
    }

    @Test
    public void seekEdgeCases_basicGorFile() {
        seekEdgeCases(BASIC_GOR_FILE, 10);
    }

    @Test
    public void seekEdgeCases_gorFileWithQuiteLongLines() {
        seekEdgeCases(GOR_FILE_WITH_QUITE_LONG_LINES, 10);
    }

    @Test
    public void seekEdgeCases_gorFileWithLongLines() {
        seekEdgeCases(GOR_FILE_WITH_LONG_LINES, 10);
    }

    @Test
    public void seekEdgeCases_gorFileWithManyLines() {
        seekEdgeCases(GOR_FILE_WITH_MANY_LINES, 5);
    }

    @Test
    public void seekEdgeCases_gorFileWithManyAndQuiteLongLines() {
        seekEdgeCases(GOR_FILE_WITH_MANY_AND_QUITE_LONG_LINES, 5);
    }

    @Test
    public void seekEdgeCases_pathologicalGorFile() {
        seekEdgeCases(PATHOLOGICAL_GOR_FILE, 5);
    }

    @Test
    public void seekAtLastPos_Pathological() {
        final GenomicIterator it = getGenomicIterator(PATHOLOGICAL_GOR_FILE);
        Assert.assertTrue(it.seek("chrX", 1));
        Assert.assertTrue(it.hasNext());
    }

    private void streamAllFile(String fileName) throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader(fileName));
        final GenomicIterator it = getGenomicIterator(fileName);

        final String wantedHeader = br.readLine();
        final String actualHeader = String.join("\t", it.getHeader());
        Assert.assertEquals(wantedHeader, actualHeader);

        br.lines().forEach(wantedLine -> {
            Assert.assertTrue(it.hasNext());
            final String actualLine = it.next().toString();
            Assert.assertEquals(wantedLine, actualLine);
        });

        Assert.assertFalse(it.hasNext());
        br.close();
        it.close();
    }

    public void seekAtBeginning(String fileName) {
        final GenomicIterator it = getGenomicIterator(fileName);
        Assert.assertTrue(it.seek("chr1", 1));
        Assert.assertTrue(it.hasNext());
        final String nextAsString = it.next().toString();
        Assert.assertTrue(nextAsString.substring(0, Math.min(nextAsString.length(), 100)), nextAsString.startsWith("chr1\t1\t0\t"));
    }

    private void seekBeyondEnd(String fileName) {
        final GenomicIterator it = getGenomicIterator(fileName);
        Assert.assertFalse(it.seek("chrY", 1_000_000_000));
    }

    private void seekToExistingPositions(String filePath, int keysPerChr) {

        final GenomicIterator it = getGenomicIterator(filePath);
        for (String chr : CHROMOSOMES) {
            for (int pos = 1; pos <= keysPerChr; ++pos) {
                log.debug("Seeking to {} {}", chr, pos);
                Assert.assertTrue(it.seek(chr, pos));
                Assert.assertTrue(it.hasNext());
                Assert.assertEquals(0, it.next().colAsInt(2));
            }
        }
        it.close();
    }

    private void seekEdgeCases(String filePath, int keysPerChr) {
        GenomicIterator it;

        final int firstPosOnChromosome = 1;

        it = getGenomicIterator(filePath);
        for (String chr : CHROMOSOMES) {
            log.debug("Seeking to {} {}", chr, 0);
            Assert.assertTrue(it.seek(chr, 0));
            Assert.assertTrue(it.hasNext());
            final Row next = it.next();
            Assert.assertEquals(chr, next.chr);
            Assert.assertEquals(firstPosOnChromosome, next.pos);
            Assert.assertEquals(0, next.colAsInt(2));
        }
        it.close();

        it = getGenomicIterator(filePath);
        for (int i = 0; i < CHROMOSOMES.length - 1; ++i) {
            final String chr = CHROMOSOMES[i];
            final int pos = keysPerChr + 1;
            log.debug("Seeking to {} {}", chr, pos);
            Assert.assertTrue(it.seek(chr, pos));
            Assert.assertTrue(it.hasNext());
            final Row next = it.next();
            Assert.assertEquals(CHROMOSOMES[i + 1], next.chr);
            Assert.assertEquals(firstPosOnChromosome, next.pos);
            Assert.assertEquals(0, next.colAsInt(2));
        }
        it.close();
    }
}