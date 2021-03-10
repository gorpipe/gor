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

package org.gorpipe.util.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Simple Wrapping interface for accessing connection pool implementation
 */
public interface ConnectionPool {
    /**
     * @return A Connection to use
     * @throws SQLException
     */
    Connection getConnection() throws SQLException;

    /**
     * Close the connection pool, cleanup up resources
     *
     * @throws SQLException
     */
    void close() throws SQLException;

    /**
     * Static Interface method to make sure all are using the same method to compose
     * the pool key.
     *
     * @param dbUrl
     * @param user
     * @return
     */
    static String composePoolKey(String dbUrl, String user) {
        return dbUrl + "-" + user;
    }
}
