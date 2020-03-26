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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.providers.db.DbScope;
import org.gorpipe.model.util.IntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DbGenomicIterator allows iterating genomic data in the database. It allows subclassing to enable
 * static filtering on columns, for an example allowing project scoping in the RDA use case.
 *
 * @version $Id$
 */
public class DbGenomicIterator extends GenomicIterator {
    private static final Logger log = LoggerFactory.getLogger(DbGenomicIterator.class);
    private final String securityContext;
    private Connection conn;
    private PreparedStatement stmt;
    private ResultSet rs;
    private final GenomicIterator.ChromoLookup lookup;
    private final String sqlbase;
    private boolean hasNext;
    private final String chrColName;
    private final String posColName;
    private final int[] columns;
    private List<DbScope> dbScopes;
    private List<DbScope> dbScopesUsed;
    private boolean seekInitialized;

    public DbGenomicIterator(ChromoLookup lookup, String databaseSource, String tableName, String chrColName, String posColName, int[] columns, List<DbScope> dbScopes, String securityContext) {
        this.lookup = lookup;
        this.sqlbase = "select * from " + tableName;
        this.chrColName = chrColName;
        this.posColName = posColName;
        this.columns = columns;
        this.dbScopes = dbScopes;
        this.seekInitialized = false;
        this.securityContext = securityContext;

        final DbSource dbSource = DbSource.lookup(databaseSource);
        if (dbSource == null) {
            throw new GorSystemException("Unknown Database Source: " + databaseSource + " is not a registered source.", null);
        }

        //Validate database table name
        if (tableName != null) {
            if (!dbSource.queryTableExists(tableName)) {
                throw new GorDataException("Invalid database table! Unable to find db table with name " + tableName);
            }
        }

        try {
            this.conn = dbSource.getConnection();
            conn.setAutoCommit(false);
        } catch (SQLException ex) {
            throw new GorSystemException("Can't connect to Database. Error initializing database connection", ex);
        }

        initDbScopesUsed();
    }

    @Override
    public String getHeader() {
        try {
            if (rs == null) {
                stmt = conn.prepareStatement(sqlOrdered(scoping(sqlbase, true)));
                stmt.setFetchSize(2000);
                if (!dbScopesUsed.isEmpty()) {
                    int i = 1;
                    for (DbScope dbScope : dbScopesUsed) {
                        stmt.setObject(i, dbScope.getValue());
                        i++;
                    }
                }
                try {
                    rs = stmt.executeQuery();
                } catch (SQLException ex) {
                    if (ex.getErrorCode() == 904) { // Invalid identifier implies there is no such scoping column, try again assuming no scoping
                        log.info("Scoping column(s) {} not found in query {}, assume no scoping", DbScope.dbScopesColumnsToString(dbScopesUsed), sqlbase);
                        stmt = conn.prepareStatement(sqlOrdered(sqlbase));
                        stmt.setFetchSize(2000);
                        rs = stmt.executeQuery();
                    } else {
                        throw ex;
                    }
                }
                // Might need to add a try/catch here, in case we need a non scoping query on data that typically will be scoped.
                hasNext = rs.next();
            }
            return getHeaderFromResultSetMetaData(rs.getMetaData());
        } catch (SQLException ex) {
            throw new GorDataException("Error getting Header. " + ex.getMessage(), ex);
        }
    }

    private String getHeaderFromResultSetMetaData(ResultSetMetaData md) throws SQLException {
        final IntHashMap map = new IntHashMap();
        if (columns != null) { // Map source column to header array destination
            for (int i = 0; i < columns.length; i++) {
                map.put(columns[i], i);
            }
        } else { // Setup one to one mapping of source column to header array destination
            for (int i = 0; i < md.getColumnCount(); i++) { // Assume scoping column is always first in query
                final int headerPosition = (!dbScopesUsed.isEmpty()) ? (i == 0 ? -dbScopesUsed.size() : i - dbScopesUsed.size()) : i;
                if (headerPosition >= 0) {
                    map.put(i, headerPosition);
                }
            }
        }
        String[] header = new String[map.size()];
        int metaDataColumnCount = md.getColumnCount();
        for (int sourcePosition = 0; sourcePosition < metaDataColumnCount; sourcePosition++) {
            final int headerPosition = map.get(sourcePosition, -1);
            if (headerPosition >= 0) {
                header[headerPosition] = md.getColumnName(sourcePosition + 1);
            }
        }
        return String.join("\t",header);
    }

    private String sqlOrdered(String sql) {
        return sql + " order by " + chrColName + "," + posColName;
    }

    private String scoping(String sql, boolean firstCondition) {
        if (dbScopesUsed.isEmpty())
            return sql;
        Iterator<DbScope> dbScopeIterator = dbScopesUsed.iterator();
        StringBuilder result = new StringBuilder(sql)
            .append(firstCondition ? " where " : " and ").append(dbScopeIterator.next().getColumn()).append(" = ?");

        while (dbScopeIterator.hasNext()) {
            result.append(" and ").append(dbScopeIterator.next().getColumn()).append(" = ?");
        }
        return result.toString();
    }

    private void initDbScopesUsed() {
        dbScopesUsed = new ArrayList<>();
        if (!dbScopes.isEmpty()) {
            List<String> resultSetColumns = getResultsSetColumnsGracefully();
            collectDbScopesUsed(resultSetColumns);
        }
        if (dbScopesUsed.isEmpty() && !dbScopes.isEmpty()) {
            throw new GorDataException("Could not find any dbscope columns: " + DbScope.dbScopesColumnsToString(dbScopes) + ", in for sql query " + sqlbase);
        }
    }

    private void collectDbScopesUsed(List<String> resultSetColumns) {
        for (DbScope dbScope : dbScopes) {
            String a = dbScope.getColumn().toUpperCase();
            for (String resultSetColumn : resultSetColumns) {
                String b = resultSetColumn.toUpperCase();
                if (a.equals(b)) {
                    dbScopesUsed.add(dbScope);
                    break;
                }
            }
        }
    }

    private List<String> getResultsSetColumnsGracefully() {
        List<String> resultSetColumns;
        try {
            resultSetColumns = fetchResultSetMetaData();
        } catch (Exception e) {
            try {
                this.conn.close();
            } catch (SQLException sqle) {
                log.warn("Error closing connection during exception handling", sqle);
            }
            throw e;
        }
        return resultSetColumns;
    }

    private List<String> fetchResultSetMetaData() {
        List<String> resultSetColumns = new ArrayList<>();
        try (PreparedStatement preparedStatement = conn.prepareStatement(sqlbase)) {
            preparedStatement.setFetchSize(1);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                int columnCount = resultSetMetaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    resultSetColumns.add(resultSetMetaData.getColumnName(i));
                }
            } catch (SQLException ex) {
                throw new GorDataException("Error result metadata set. " + ex.getMessage(), ex);
            }

        } catch (SQLException ex) {
            throw new GorDataException("Error in prepare statement after getting result set with " + resultSetColumns.size() + " columns. " + ex.getMessage(), ex);
        }
        return resultSetColumns;
    }

    @Override
    public boolean seek(String chr, int pos) {
        try {
            if (rs != null) rs.close();
            if (!seekInitialized) {
                if (stmt != null) stmt.close();
                stmt = conn.prepareStatement(sqlOrdered(scoping(sqlbase + " where " + chrColName + " = ? and " + posColName + " >= ?", false)));
                stmt.setFetchSize(2000);
                seekInitialized = true;
            }
            stmt.setString(1, chr);
            stmt.setInt(2, pos);
            if (!dbScopesUsed.isEmpty()) {
                int i = 3;
                for (DbScope dbScope : dbScopesUsed) {
                    stmt.setObject(i, dbScope.getValue());
                    i++;
                }
            }
            rs = stmt.executeQuery();
            hasNext = rs.next();
            return hasNext;
        } catch (SQLException ex) {
            throw new GorDataException("Error seeking. " + ex.getMessage(), ex);
        }
    }

    String removeInvalidCharacters(String val) {
        if (val != null) {
            val = val.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '); // tabs and newlines will make gor parsing go haywire
        }
        return val;
    }

    @Override
    public boolean next(Line line) {
        if (!hasNext) {
            return false;
        }
        try {
            assert rs != null;
            if (columns == null) {
                // We need to find where chrom and pos columns are.
                int idx = dbScopesUsed.size() + 1;
                // Get chrom and pos columns
                line.chr = rs.getString(idx++);
                line.chrIdx = lookup.chrToId(line.chr);
                line.pos = rs.getInt(idx++);
                // Loop through remainder of columns after chrom and pos
                for (int i = 0; i <= line.cols.length - dbScopesUsed.size(); i++) {
                    line.cols[i].setUTF8(removeInvalidCharacters(rs.getString(idx + i)));
                }
            } else {
                line.chr = rs.getString(columns[0] + 1);
                line.chrIdx = lookup.chrToId(line.chr);
                line.pos = rs.getInt(columns[1] + 1);
                for (int i = 2; i < columns.length; i++) {
                    line.cols[i - 2].setUTF8(removeInvalidCharacters(rs.getString(columns[i] + 1)));
                }
            }
            hasNext = rs.next();
            return true;
        } catch (SQLException ex) {
            throw new GorDataException("Error reading Db - " + DbScope.dbScopesToString(dbScopes) + " securityContext: " + securityContext + ex.getMessage(), ex);
        }
    }

    @Override
    public void close() {
        try {
            try {
                try {
                    if (rs != null) rs.close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                if (stmt != null) stmt.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (conn != null) conn.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            rs = null;
            stmt = null;
            conn = null;
        }
    }
}
