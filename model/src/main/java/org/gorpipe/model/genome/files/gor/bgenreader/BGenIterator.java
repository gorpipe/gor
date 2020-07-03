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

package org.gorpipe.model.genome.files.gor.bgenreader;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.providers.stream.datatypes.bgen.BGenFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.Line;
import org.gorpipe.model.genome.files.gor.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

import static org.gorpipe.model.genome.files.gor.bgenreader.Utils.ensureCapacity;
import static org.gorpipe.model.genome.files.gor.bgenreader.Utils.parseUnsignedInt;

public class BGenIterator extends GenomicIterator {
    private static final Logger log = LoggerFactory.getLogger(BGenIterator.class);
    private final Connection connection;
    private final VariantDataBlockParser parser;
    private ResultSet resultSet;
    private boolean hasNext = false;
    private PreparedStatement preparedStatement;
    private final StreamSourceSeekableFile source;
    private final Path indexFilePath;
    private byte[] buffer;

    public BGenIterator(BGenFile bGenFile) {
        this.source = new StreamSourceSeekableFile(bGenFile.getFileSource());
        this.indexFilePath = getIndexFilePath(bGenFile.getIndexSource());
        this.connection = createConnection();
        final HeaderInfo hi = readHeader();
        this.parser = DataBlockParserFactory.getParser(hi);
    }

    private Path getIndexFilePath(StreamSource indexSource) {
        try (final StreamSourceSeekableFile sssf = new StreamSourceSeekableFile(indexSource)) {
            final File indexFileLocal = File.createTempFile("bgenidxfile", ".bgen");
            try(final FileOutputStream fos = new FileOutputStream(indexFileLocal)) {
                final byte[] indexFileBuffer = new byte[32_768];
                int read;
                while ((read = sssf.read(indexFileBuffer)) > 0) {
                    fos.write(indexFileBuffer, 0, read);
                }
            }
            return indexFileLocal.toPath();
        } catch (IOException e) {
            throw new GorSystemException("Could not create temp file", e);
        }
    }

    private Connection createConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new GorSystemException("No database driver found for SQLite", e);
        }
        try {
            return DriverManager.getConnection("jdbc:sqlite:" + this.indexFilePath);
        } catch (SQLException e) {
            throw new GorResourceException("Could not create a connection to index file.", this.indexFilePath.toString(), e);
        }
    }

    private HeaderInfo readHeader() {
        try {
            this.source.seek(4);
            final byte[] headerLenBytes = new byte[4];
            this.source.read(headerLenBytes);
            final int headerLen = (int) parseUnsignedInt(headerLenBytes, 0) - 4; //The length field is included.
            final byte[] header = new byte[headerLen];
            readFully(this.source, header, 0, headerLen);
            return HeaderBlockParser.parse(header, 0, headerLen);
        } catch (IOException e) {
            throw new GorResourceException("Failed to read header from file.", "", e);
        }
    }

    @Override
    public String getHeader() {
        return "CHROM\tPOS\tREF\tALT\tRSID\tVARIANTID\tVALUES";
    }

    @Override
    public boolean seek(String chr, int pos) {
        try {
            final String queryChr = getQueryChr(chr);
            if (this.preparedStatement == null) {
                this.preparedStatement = this.connection.prepareStatement("SELECT " +
                        "case when length(chromosome) > 3 " +
                        "then case when substr(chromosome, 4, 1) = \"0\" then substr(chromosome, 5, length(chromosome) - 4) else substr(chromosome, 4, length(chromosome) - 3) end " +
                        "else chromosome end as gorchr," +
                        "position,file_start_position,size_in_bytes " +
                        "FROM Variant " +
                        "WHERE (gorchr > ?) OR (gorchr = ? AND position >= ?) " +
                        "ORDER BY gorchr,position");
            }
            this.preparedStatement.setString(1, queryChr);
            this.preparedStatement.setString(2, queryChr);
            this.preparedStatement.setInt(3, pos);
            this.resultSet = this.preparedStatement.executeQuery();
            return (this.hasNext = this.resultSet.next());
        } catch (SQLException e) {
            throw new GorResourceException("Could not execute database query.", this.indexFilePath.toString(), e);
        }
    }

    protected String getQueryChr(String chr) {
        return chr.startsWith("chr") ? chr.substring(3) : chr;
    }

    @SuppressWarnings("squid:S2095")  //Connection should not be closed here so try-with-resources is not applicable
    @Override
    public boolean hasNext() {
        try {
            if (this.resultSet == null) {
                this.resultSet = this.connection.createStatement().executeQuery("SELECT " +
                        "case when length(chromosome) > 3 " +
                        "then case when substr(chromosome, 4, 1) = \"0\" then substr(chromosome, 5, length(chromosome) - 4) else substr(chromosome, 4, length(chromosome) - 3) end " +
                        "else chromosome end as gorchr," +
                        "position,file_start_position,size_in_bytes " +
                        "FROM Variant " +
                        "ORDER BY gorchr,position");
            }
            if (!this.hasNext) {
                this.hasNext = this.resultSet.next();
            }
            return this.hasNext;
        } catch (SQLException e) {
            throw new GorResourceException("Could not execute query on the index file.",  this.indexFilePath.toString(), e);
        }
    }

    @Override
    public Row next() {
        try {
            final long offset = this.resultSet.getLong("file_start_position");
            final int len = this.resultSet.getInt("size_in_bytes");
            this.hasNext = this.resultSet.next();
            this.buffer = ensureCapacity(this.buffer, len);
            this.source.seek(offset);
            readFully(this.source, this.buffer, 0, len);
            return this.parser.parse(this.buffer, 0, len);
        } catch (SQLException | IOException e) {
            throw new GorResourceException("Could not read next line.", "", e);
        }
    }

    private static void readFully(StreamSourceSeekableFile source, byte[] array, int offset, int len) throws IOException {
        int total = 0;
        int read;
        while ((read = source.read(array, offset + total, len - total)) > 0) {
            total += read;
        }
        if (total != len) {
            throw new IllegalStateException("Could not read " + len + " bytes from file.");
        }
    }

    @Override
    public boolean next(Line line) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        try {
            if (this.resultSet != null) this.resultSet.close();
            if (this.preparedStatement != null) this.preparedStatement.close();
            if (this.connection != null) this.connection.close();
            if (Files.exists(this.indexFilePath)) {
                Files.delete(this.indexFilePath);
            }
        } catch (SQLException | IOException e) {
            log.warn(e.getMessage(), e);
        }
    }
}
