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

package org.gorpipe.model.genome.files.gor;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;

public class BGenIterator extends GenomicIterator {
    private static final Logger log = LoggerFactory.getLogger(BGenIterator.class);
    final private Connection connection;
    final IndexedBGenFileDriver driver;
    private ResultSet resultSet;
    boolean hasNext = false;
    final private String indexFilePath;
    private PreparedStatement preparedStatement;

    public BGenIterator(String path) {
        this.driver = new IndexedBGenFileDriver(path);
        final File bgenIdxFile = new File(path + ".bgi");
        this.indexFilePath = bgenIdxFile.getAbsolutePath();
        if (bgenIdxFile.isFile()) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new GorSystemException("No database driver found for SQLite", e);
            }
            try {
                this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.indexFilePath);
            } catch (SQLException e) {
                throw new GorResourceException("Could not create a connection to index file.", this.indexFilePath, e);
            }
        } else {
            throw new GorResourceException("An index file " + this.indexFilePath + " GOR only supports reading of .bgen files which have corresponding index files in the same folder.", path);
        }
    }

    @Override
    public String getHeader() {
        return String.join("\t",new String[] {"CHROM", "POS", "REF", "ALT", "RSID", "VARIANTID", "VALUES"});
    }

    @Override
    public boolean seek(String chr, int pos) {
        try {
            final String queryChr = chr.length() == 4 ? "0" + chr.substring(3) : chr.substring(3);
            if (this.preparedStatement == null) {
                this.preparedStatement = this.connection.prepareStatement("SELECT chromosome,position,file_start_position,size_in_bytes FROM Variant WHERE (chromosome > ?) OR (chromosome = ? AND position >= ?)");
            }
            this.preparedStatement.setString(1, queryChr);
            this.preparedStatement.setString(2, queryChr);
            this.preparedStatement.setInt(3, pos);
            this.resultSet = this.preparedStatement.executeQuery();
            return (this.hasNext = this.resultSet.next());
        } catch (SQLException e) {
            throw new GorResourceException("Could not execute database query.", this.indexFilePath, e);
        }
    }

    @SuppressWarnings("squid:S2095")  //Connection should not be closed here so try-with-resources is not applicable
    @Override
    public boolean hasNext() {
        try {
            if (this.resultSet == null) {
                this.resultSet = this.connection.createStatement().executeQuery("SELECT chromosome,position,file_start_position,size_in_bytes FROM Variant ORDER BY chromosome, position");
            }
            return this.hasNext || (this.hasNext = this.resultSet.next());
        } catch (SQLException e) {
            throw new GorResourceException("Could not execute query on the index file.",  this.indexFilePath, e);
        }
    }

    @Override
    public Row next() {
        final Line toReturn = new Line(5);
        this.next(toReturn);
        return toReturn;
    }

    @Override
    public boolean next(Line line) {
        try {
            if (this.hasNext()) {
                this.hasNext = false;
                return this.driver.writeNextLine(line, this.resultSet.getLong("file_start_position"), this.resultSet.getInt("size_in_bytes"));
            } else return false;
        } catch (SQLException e) {
            throw new GorResourceException("Could not parse result set from query on index file.", this.indexFilePath, e);
        }
    }

    @Override
    public void close() {
        try {
            if (this.resultSet != null) this.resultSet.close();
            if (this.preparedStatement != null) this.preparedStatement.close();
            if (this.connection != null) this.connection.close();
        } catch (SQLException e) {
            log.warn(e.getMessage(), e);
        }
    }
}
