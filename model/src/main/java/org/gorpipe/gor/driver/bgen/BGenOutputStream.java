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

package org.gorpipe.gor.driver.bgen;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.util.collection.ByteArray;

import java.io.*;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * See specification at: https://www.well.ox.ac.uk/~gav/bgen_format/spec/latest.html
 *
 * @author hjaltii
 */
class BGenOutputStream implements AutoCloseable {
    private static final int HEADER_BLOCK_LEN = 24;

    private OutputStream os;
    private final byte[] buffer = new byte[1024 * 1024];
    private int bufferIdx;
    private int variantCount = 0;
    private int sampleCount = 0;
    private boolean first = true;
    private final String fileName;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private String idxFileName;
    private long filePos = HEADER_BLOCK_LEN;

    public BGenOutputStream(String fileName) {
        this.fileName = fileName;
    }

    void write(VariantDataBlock db) throws IOException, SQLException {
        if (this.first) {
            this.sampleCount = db.numberOfSamples;
            initialize();
            this.first = false;
        }
        int totalWritten = 0;
        int written;
        while ((written = db.write(this.buffer, this.bufferIdx, this.buffer.length - this.bufferIdx)) > 0) {
            this.bufferIdx += written;
            totalWritten += written;
            if (this.bufferIdx == this.buffer.length) {
                this.os.write(this.buffer);
                this.bufferIdx = 0;
            }
        }
        this.bufferIdx -= written;
        totalWritten -= written;

        preparedStatement.setString(1, db.chr.toString());
        preparedStatement.setInt(2, db.pos);
        preparedStatement.setString(3, db.rsId.toString());
        preparedStatement.setInt(4, db.alleles.length);
        preparedStatement.setString(5, db.alleles[0].toString());
        preparedStatement.setString(6, getAltAlleleString(db.alleles));
        preparedStatement.setLong(7, this.filePos);
        preparedStatement.setLong(8, totalWritten);
        this.filePos += totalWritten;
        preparedStatement.execute();
        this.variantCount++;
    }

    private void initialize() {
        try {
            this.os = new FileOutputStream(fileName);
            this.bufferIdx = HEADER_BLOCK_LEN;
        } catch (IOException e) {
            throw new GorSystemException("Could not create file " + fileName, e);
        }
        prepareIdxTable();
    }

    @Override
    public void close() {
        if (!this.first) {
            try {
                if (this.bufferIdx > 0) {
                    this.os.write(this.buffer, 0, this.bufferIdx);
                }
                this.os.close();
                writeHeaderBlock();
                this.connection.close();
            } catch (IOException e) {
                throw new GorSystemException("Could not close file " + fileName, e);
            } catch (SQLException e) {
                throw new GorSystemException("Could not close data base connection.", e);
            }
        }
    }

    static String getAltAlleleString(CharSequence[] alleles) {
        if (alleles.length == 2) {
            return alleles[1].toString();
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append(alleles[1]);
            for (int i = 2; i < alleles.length; ++i) {
                sb.append(',');
                sb.append(alleles[i]);
            }
            return sb.toString();
        }
    }

    private void prepareIdxTable() {
        try {
            this.idxFileName = fileName + ".bgi";
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + idxFileName);
            final String create = "CREATE TABLE Variant ( " +
                    "chromosome TEXT NOT NULL, " +
                    "position INT NOT NULL, " +
                    "rsid TEXT NOT NULL, " +
                    "number_of_alleles INT NOT NULL, " +
                    "reference_allele TEXT NOT NULL, " +
                    "alternative_alleles TEXT NULL, " +
                    "file_start_position INT NOT NULL, " +
                    "size_in_bytes INT NOT NULL, " +
                    "PRIMARY KEY (chromosome, position, rsid, reference_allele, alternative_alleles, file_start_position ) " +
                    ") WITHOUT ROWID;";
            this.connection.createStatement().execute(create);
            final String statementToPrepare = "INSERT INTO Variant (chromosome, position, rsid, number_of_alleles, reference_allele, alternative_alleles, file_start_position, size_in_bytes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
            this.preparedStatement = this.connection.prepareStatement(statementToPrepare);
        } catch (SQLException e) {
            throw new GorResourceException(e.getMessage(), fileName);
        }
    }

    private void writeHeaderBlock() {
        final byte[] headerBlock = new byte[HEADER_BLOCK_LEN];
        ByteArray.writeInt(headerBlock, 0, ByteOrder.LITTLE_ENDIAN, 20); //Offset relative to fifth byte of the first variant data block.
        ByteArray.writeInt(headerBlock, 4, ByteOrder.LITTLE_ENDIAN, 20); //Header block length.
        ByteArray.writeInt(headerBlock, 8, ByteOrder.LITTLE_ENDIAN, variantCount);
        ByteArray.writeInt(headerBlock, 12, ByteOrder.LITTLE_ENDIAN, sampleCount);
        headerBlock[16] = 'b';
        headerBlock[17] = 'g';
        headerBlock[18] = 'e';
        headerBlock[19] = 'n';
        headerBlock[20] = 9; //Use zlib compression and layout type 2. (00001001)
        try (RandomAccessFile raf = new RandomAccessFile(new File(fileName), "rw")) {
            raf.seek(0);
            raf.write(headerBlock);
        } catch (IOException e) {
            throw new GorSystemException("Could not write header block to file " + fileName, e);
        }
    }
}

