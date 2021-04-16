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

package org.gorpipe.gor.binsearch;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.driver.GorDriver;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;

@SuppressWarnings("squid:S3457")
public class GorIndexFile implements AutoCloseable{
    private static final Logger log = LoggerFactory.getLogger(GorIndexFile.class);
    private static final String VERSION = "GORIv1";

    private final BufferedWriter out;
    private final GorIndexType indexType;

    private String lastChrom = "";
    private int lastPosInChrom = 0;
    private long lastFilePosInChrom = 0;

    private String lastChromWritten = "";
    private int lastPosWritten = 0;

    private boolean skippedLast = false;

    public GorIndexFile(File file, GorIndexType indexType) throws IOException {
        this(Files.newBufferedWriter(file.toPath()),indexType);
    }

    public GorIndexFile(OutputStream outputStream, GorIndexType indexType) throws IOException {
        this(new BufferedWriter(new OutputStreamWriter(outputStream)), indexType);
    }

    public GorIndexFile(BufferedWriter writer, GorIndexType indexType) throws IOException {
        out = writer;
        this.indexType = indexType;
        String versionString = String.format("## fileformat=%s\n", VERSION);
        out.write(versionString);
    }

    public static void load(InputStream inputStream, PositionCache pc) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            new Loader(reader, pc).invoke();
        } catch (IOException e) {
            log.warn("Error reading index file", e);
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public void generateForGorz(String filename) throws IOException {
        SourceReference sourceRef = new SourceReferenceBuilder(filename).build();
        GorDriver gorDriver = GorDriverFactory.fromConfig();
        try (
                StreamSource dataSource = (StreamSource) gorDriver.resolveDataSource(sourceRef);
                InputStream inputStream = dataSource.open()
        ) {
            new GorzReader(inputStream).invoke(this);
        }
    }

    public void writeLastEntry() throws IOException {
        if(skippedLast) writeEntry(lastChrom, lastPosInChrom, lastFilePosInChrom);
    }

    public void putFilePosition(String chr, int pos, long filePos) throws IOException {
        if (indexType == GorIndexType.CHROMINDEX) {
            if (chr.equals(lastChrom)) {
                lastPosInChrom = pos;
                lastFilePosInChrom = filePos;
                skippedLast = true;
                return;
            } else if(lastFilePosInChrom > 0) {
                // We store the last position for each chromosome. This is to allow the quickest
                // possible binary search for the beginning of the chromosome, without having
                // to tag the position as the start.
                writeEntry(lastChrom, lastPosInChrom, lastFilePosInChrom);
            }
        }
        lastChrom = chr;
        lastPosInChrom = pos;
        lastFilePosInChrom = filePos;

        if (filePos > 0) {
            writeEntry(chr, pos, filePos);
        }
    }

    private void writeEntry(String chr, int pos, long filePos) throws IOException {
        if (!lastChromWritten.equals(chr) || lastPosWritten != pos) {
            String line = String.format("%s\t%d\t%d\n", chr, pos, filePos);
            out.write(line);
            lastChromWritten = chr;
            lastPosWritten = pos;
            skippedLast = false;
        }
    }

    private static class Loader {
        private final BufferedReader reader;
        private final PositionCache pc;
        private String version;

        Loader(BufferedReader reader, PositionCache pc) {
            this.reader = reader;
            this.pc = pc;
            this.version = "";
        }

        void invoke() throws IOException {
            String line = processHeader();

            if (!version.equals(VERSION)) {
                throw new GorDataException("Invalid version of index file");
            }

            int lineCount = 0;
            while (line != null) {
                String[] split = line.split("\t");
                StringIntKey key = new StringIntKey(split[0], Integer.parseInt(split[1]));
                long pos = Long.parseLong(split[2]);
                pc.putFilePosition(key, pos);
                lineCount += 1;
                line = reader.readLine();
            }

            log.debug("{} lines read from index file", lineCount);
        }

        private String processHeader() throws IOException {
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("##")) {
                    String headerLine = line.substring(2).trim();
                    String[] parts = headerLine.split("=");
                    if (parts.length == 2) {
                        String key = parts[0];
                        String value = parts[1];

                        if (key.equals("fileformat")) {
                            version = value;
                        }
                    }
                    line = reader.readLine();
                } else {
                    break;
                }
            }
            return line;
        }
    }

    private static class GorzReader {
        private final InputStream inputStream;
        private long posInFile = 0;
        private boolean eof = false;
        private final byte[] inputBuffer = new byte[64*1024];
        private int posInBuffer = 0;
        private int bytesInBuffer = 0;

        GorzReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        void invoke(GorIndexFile out) throws IOException {
            fillBuffer();
            skipUntilNewline();

            while (!eof) {
                String chrom = readUntilTab();
                if (eof) {
                    break;
                }
                String posAsString = readUntilTab();
                int chromPos = Integer.parseInt(posAsString);
                skipUntilNewline();
                out.putFilePosition(chrom, chromPos, posInFile + posInBuffer);
            }
            // Dummy file position to trigger writing last position for last chrom
            out.putFilePosition("", 0, 0);

        }

        private String readUntilTab() throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            while (!eof) {
                byte c = inputBuffer[posInBuffer++];
                if (posInBuffer == bytesInBuffer) {
                    fillBuffer();
                }
                if (c == '\t') {
                    break;
                } else {
                    buffer.write(c);
                }
            }
            return buffer.toString();
        }

        private void skipUntilNewline() throws IOException {
            int p = posInBuffer;
            while (!eof) {
                while (p < bytesInBuffer && inputBuffer[p++] != '\n')
                    ;

                if (p == bytesInBuffer) {
                    posInBuffer = p;
                    fillBuffer();
                    p = 0;
                } else {
                    break;
                }
            }
            posInBuffer = p;
        }

        private void fillBuffer() throws IOException {
            posInFile += posInBuffer;
            bytesInBuffer = inputStream.read(inputBuffer, 0, inputBuffer.length);
            posInBuffer = 0;
            if (bytesInBuffer == -1) {
                eof = true;
            }
        }
    }
}
