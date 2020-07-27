package org.gorpipe.model.genome.files.binsearch;

import org.gorpipe.util.collection.ByteArray;
import org.gorpipe.util.collection.ByteArrayWrapper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Deflater;

public class TestFileGenerator {
    public final static List<String> CHROMOSOMES = IntStream.rangeClosed(1, 22).mapToObj(i -> "chr" + i).sorted().collect(Collectors.toList());

    private static final int SEED = 5;

    final String fileName;
    final int posPerChr;
    final int linesPerKey;
    final int maxColLen;
    final boolean skipNewLine;
    final byte[] header = "CHROM\tPOS\tNUMBER\tSOME_COL\n".getBytes();
    final int offset = header.length;
    long size;
    private File file;
    private final File workDir;
    private StringIntKey lastKey;
    private boolean vcf = false;

    public String path;

    public TestFileGenerator(String fileName, File workDir, int posPerChr, int linesPerKey, int maxColLen, boolean skipNewLine) {
        this.fileName = fileName;
        this.posPerChr = posPerChr;
        this.linesPerKey = linesPerKey;
        this.maxColLen = maxColLen;
        this.skipNewLine = skipNewLine;
        this.workDir = workDir;
    }

    public void writeVCF() throws IOException {
        this.vcf = true;
        final Random r = new Random(SEED);
        final byte[] buffer = new byte[maxColLen];
        fillBuffer(buffer);
        this.file = new File(this.workDir, this.fileName + ".vcf");
        final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        final Map<String, String> contigMap = getContigMap();
        final List<String> contigs = new ArrayList<>(contigMap.keySet());
        Collections.shuffle(contigs);

        this.lastKey = new StringIntKey(contigs.get(contigs.size() - 1), this.posPerChr);

        bos.write("##fileformat=VCFv4.2\n".getBytes());
        for (String contig : contigs) {
            bos.write(contigMap.get(contig).getBytes());
        }
        bos.write("#CHROM\tPOS\tNUMBER\tSOME_COL\n".getBytes());

        for (String contig : contigs) {
            for (int pos = 1; pos <= posPerChr; ++pos) {
                for (int num = 0; num < linesPerKey; ++num) {
                    bos.write((contig + "\t" + pos + "\t" + num + "\t").getBytes());
                    bos.write(buffer, 0, r.nextInt(maxColLen));
                    bos.write('\n');
                }
            }
        }
        bos.close();
        this.size = file.length();
        this.path = file.getAbsolutePath();
    }

    void writeFile(boolean compress) throws IOException {
        final Random r = new Random(SEED);
        if (this.skipNewLine) {
            this.lastKey = new StringIntKey("chrX", 1);
        } else {
            this.lastKey = new StringIntKey("chr22", this.posPerChr);
        }
        final byte[] buffer = new byte[maxColLen];
        fillBuffer(buffer);
        this.file = new File(this.workDir, this.fileName + (compress ? ".gorz" : ".gor"));
        final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        bos.write(header);
        if (compress) {
            writeCompressed(r, buffer, bos);
        } else {
            writeGorFile(r, buffer, bos);
        }
        bos.close();
        this.size = file.length();
        this.path = file.getAbsolutePath();
    }

    private void writeGorFile(Random r, byte[] buffer, BufferedOutputStream bos) throws IOException {
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
    }

    private void writeCompressed(Random r, byte[] buffer, BufferedOutputStream bos) throws IOException {
        final ByteArrayWrapper baw = new ByteArrayWrapper(Math.max(32 * 1024, maxColLen + 100));
        String lastPos = "";
        final Deflater deflater = new Deflater();
        for (String chr : CHROMOSOMES) {
            for (int pos = 1; pos <= posPerChr; ++pos) {
                lastPos = chr + "\t" + pos;
                for (int num = 0; num < linesPerKey; ++num) {
                    final byte[] lineBase = (chr + "\t" + pos + "\t" + num + "\t").getBytes();
                    final int colLen = r.nextInt(maxColLen);
                    if (lineBase.length + colLen + 1 >= 32 * 1024 && baw.size() > 0) {
                        flush(bos, baw, lastPos, deflater);
                    }
                    baw.write(lineBase);
                    baw.write(buffer, 0, colLen);
                    baw.write('\n');
                }
            }
            flush(bos, baw, lastPos, deflater);
        }
        if (skipNewLine) {
            lastPos = "chrX\t1";
            baw.write(("chrX\t1\t0\t").getBytes());
            baw.write(buffer, 0, r.nextInt(maxColLen));
            flush(bos, baw, lastPos, deflater);
        }
    }

    private void flush(BufferedOutputStream bos, ByteArrayWrapper baw, String lastPos, Deflater deflater) throws IOException {
        bos.write(lastPos.getBytes());
        bos.write('\t');
        bos.write(0);
        deflater.setInput(baw.getBuffer(), 0, baw.size());
        deflater.finish();
        byte[] compressed = new byte[32 * 1024];
        int len = 0;
        while (!deflater.finished()) {
            len += deflater.deflate(compressed, len, compressed.length - len);
            if (len == compressed.length) {
                compressed = Arrays.copyOf(compressed, 2 * compressed.length);
            }
        }
        bos.write(ByteArray.to7Bit(compressed, len));
        bos.write('\n');
        baw.reset();
        deflater.reset();
    }

    private void fillBuffer(byte[] buffer) {
        final Random r = new Random();
        for (int i = 0; i < buffer.length; ++i) {
            buffer[i] = (byte) (33 + r.nextInt(94));
        }
    }

    @Override
    public String toString() {
        return this.fileName;
    }

    public StringIntKey getLastKey() {
        return this.lastKey;
    }

    public List<String> getChromosomes() {
        if (this.vcf) {
            return Arrays.asList("chr1", "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16", "chr17", "chr18", "chr19", "chr2", "chr20", "chr21", "chr22", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9", "chrM", "chrX", "chrY");
        } else {
            return new ArrayList<>(CHROMOSOMES);
        }
    }

    private Map<String, String> getContigMap() {
        final Map<String, String> toReturn = new HashMap<>();
        toReturn.put("1", "##contig=<ID=1,length=249250621,assembly=unknown>\n");
        toReturn.put("2", "##contig=<ID=2,length=243199373,assembly=unknown>\n");
        toReturn.put("3", "##contig=<ID=3,length=198022430,assembly=unknown>\n");
        toReturn.put("4", "##contig=<ID=4,length=191154276,assembly=unknown>\n");
        toReturn.put("5", "##contig=<ID=5,length=180915260,assembly=unknown>\n");
        toReturn.put("6", "##contig=<ID=6,length=171115067,assembly=unknown>\n");
        toReturn.put("7", "##contig=<ID=7,length=159138663,assembly=unknown>\n");
        toReturn.put("8", "##contig=<ID=8,length=146364022,assembly=unknown>\n");
        toReturn.put("9", "##contig=<ID=9,length=141213431,assembly=unknown>\n");
        toReturn.put("10", "##contig=<ID=10,length=135534747,assembly=unknown>\n");
        toReturn.put("11", "##contig=<ID=11,length=135006516,assembly=unknown>\n");
        toReturn.put("12", "##contig=<ID=12,length=133851895,assembly=unknown>\n");
        toReturn.put("13", "##contig=<ID=13,length=115169878,assembly=unknown>\n");
        toReturn.put("14", "##contig=<ID=14,length=107349540,assembly=unknown>\n");
        toReturn.put("15", "##contig=<ID=15,length=102531392,assembly=unknown>\n");
        toReturn.put("16", "##contig=<ID=16,length=90354753,assembly=unknown>\n");
        toReturn.put("17", "##contig=<ID=17,length=81195210,assembly=unknown>\n");
        toReturn.put("18", "##contig=<ID=18,length=78077248,assembly=unknown>\n");
        toReturn.put("19", "##contig=<ID=19,length=59128983,assembly=unknown>\n");
        toReturn.put("20", "##contig=<ID=20,length=63025520,assembly=unknown>\n");
        toReturn.put("21", "##contig=<ID=21,length=48129895,assembly=unknown>\n");
        toReturn.put("22", "##contig=<ID=22,length=51304566,assembly=unknown>\n");
        toReturn.put("X", "##contig=<ID=X,length=155270560,assembly=unknown>\n");
        toReturn.put("Y", "##contig=<ID=Y,length=59373566,assembly=unknown>\n");
        toReturn.put("M", "##contig=<ID=M,length=16569,assembly=unknown>\n");
        return toReturn;
    }
}
