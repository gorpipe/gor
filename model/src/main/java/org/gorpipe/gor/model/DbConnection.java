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

package org.gorpipe.gor.model;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.util.db.ConnectionPool;
import org.gorpipe.util.db.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.*;

/**
 * DbConnection abstract access to database source that are configured on installation time.
 *<p>
 * The class abstracts the connections to a single database.
 *<p>
 * The static part creates a cache of DbConnection objects that can be used to access all databases defined in the
 * database configuration files.  This part is located here for backward compatibility reasons.
 *<p>
 * We have two different configuration files:
 *
 * <li> gor.db.credentials - contains the system databases, which are used by the system internally (e.g. session management),
 *                         and by operation where we have strict access controls (db://, //db:).  These credentials
 *                         typically would grant full access to the database.
 * <li> gor.sql.credentials - contains the user databases, which are used by the user available commands/sources (SQL,
 *                          GORSQL, NORSQL, sql://).  These credentials typically would grant limited/read-only access to
 *                          the database.
 * <p><br>
 * The format for these files is:
 * <pre>
 *    name\tdriver\turl\tuser\tpwd
 *    rda\torg.postgresql.Driver\tjdbc:postgresql://myurl.com:5432/csa\trda\tmypass
 *    ...
 * </pre>
 *<p>
 * The location of the files defaults to the config directory but can be specified by the system properties
 * gor.db.credentials and gor.sql.credentials.
 * <p><br>
 * TODO:  This class needs some refactoring to handle the different databases more gracefully.  We should use some kind of
 * plugin mechanism (or Guice) instead of if/else statements.
 *
 * @version $Id$
 */
public class DbConnection {
    private static final Logger log = LoggerFactory.getLogger(DbConnection.class);

    public static DbConnectionCache systemConnections = new DbConnectionCache();
    public static DbConnectionCache userConnections = new DbConnectionCache();

    private static DbConnection.GetPassword<DbConnection> pwdCallback = null;

    /**
     * The name of the datasource.
     */
    public final String name;
    /**
     * The URL to use to connect the datasource.
     */
    public final String url;
    /**
     * The username to login to.
     */
    public final String user;
    /**
     * The password to use.
     */
    public String pwd;
    /**
     * The connection pool.
     */
    private ConnectionPool pool = null; // Connection pool to use, if available


    /**
     * Get a password property
     *
     * @param <T> The Object type
     */
    public interface GetPassword<T> {
        /**
         * @param value The object to get string property from
         * @return The string property
         */
        String get(T value);
    }

    /**
     * Construct a DbSource
     *
     * @param name The name of the source
     * @param url  The url to the source
     * @param user The user to login as
     * @param pwd  The password for the user
     */
    public DbConnection(String name, String url, String user, String pwd) {
        this.name = name;
        this.url = url;
        this.user = user;
        this.pwd = pwd;
        getConnectionPool();
    }

    /**
     * Reserve a connection from this source
     *
     * @return The reserved connection, one must call close on it to release it back to the pool
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        if ((pwd == null || pwd.length() == 0) && pwdCallback != null) {
            pwd = pwdCallback.get(this);
        }
        getConnectionPool();
        return pool != null ? pool.getConnection() : DriverManager.getConnection(url, user, pwd);
    }

    ConnectionPool getConnectionPool() {
        pool = Db.getPool(url, user, pwd);
        return pool;
    }

    public void close() {
        if (pool != null) {
            try {
                pool.close();
            } catch (SQLException e) {
                log.error("Exception when closing pool for db source: " + name, e);
            }
        }
    }

    private boolean isOracle() {
        return url.startsWith("jdbc:oracle:");
    }

    /**
     * Query for the timestamp of last change associated with the specified table name
     *
     * @param tableName Name of table or view
     * @return timestamp of last table change. Returns currentTimeMillis if error or no value found.
     */
    @java.lang.SuppressWarnings({"squid:S2077", "squid:S3923"})  //S2077:table name is validated, S3923:ignore.
    public long queryDefaultTableChange(String tableName) {
        //Must validated table name
        if (!isOracle() || !queryTableExists(tableName)) {
            return System.currentTimeMillis();
        }

        // TODO:  Would be better to use auditing here (http://www.jlcomp.demon.co.uk/faq/table_update.html).
        try (Connection conn = getConnection()) {
            String sql = "select SCN_TO_TIMESTAMP(MAX(ora_rowscn)) from " + tableName;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getTimestamp(1).getTime() : System.currentTimeMillis();
                }
            }
        } catch (SQLException ex) {
            if (ex.getMessage().startsWith("ORA-08181")) {
                // ORA-08181: The max ora_rowscn number is too old or not a scn number.
                return System.currentTimeMillis();
            } else if (ex.getMessage().startsWith("ORA-00904")) {
                // ORA=00904: Invalid identifier, ora_rowscn only exists for tables.
                return System.currentTimeMillis();
            } else if (ex.getMessage().startsWith("ORA-01405")) {
                // ORA=014050: NULL value fed to SCN_TO_TIMESTAMP, the table was empty.
                return System.currentTimeMillis();
            }
            throw new GorSystemException(ex);
        }
    }

    /**
     * Check if table/view exists
     *
     * @param tableName Name of table or view
     * @return true if the table/view exists, otherwise false.
     */
    public boolean queryTableExists(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            throw new GorResourceException("Error checking if table exists. Table is null or empty.", tableName);
        }

        //Check if schema in table name
        final String[] types = new String[]{"TABLE", "VIEW"};
        final int dotIdx = tableName.indexOf('.');
        String schema = "";
        String table = tableName;
        if (dotIdx > 0) {
            table = tableName.substring(dotIdx + 1);
            schema = tableName.substring(0, dotIdx);
        }
        //Validate table exists
        try (Connection conn = getConnection()) {
            DatabaseMetaData dbm = conn.getMetaData();
            //We do not know in which case table names are stored in the db
            return dbm.getTables(null, schema.isEmpty() ? null : schema, table, types).next() ||
                    dbm.getTables(null, schema.isEmpty() ? null : schema.toUpperCase(), table.toUpperCase(), types).next() ||
                    dbm.getTables(null, schema.isEmpty() ? null : schema.toLowerCase(), table.toLowerCase(), types).next();

        } catch (Exception e) {
            if (e instanceof UndeclaredThrowableException ue) {
                if (ue.getUndeclaredThrowable().getCause() instanceof SQLSyntaxErrorException) {
                    return false;
                }
            }
            throw new GorResourceException("Error checking if table exists. Testing table/view: ", url + " - "+ tableName, e);
        }
    }

    /**
     * Initialize DbConnections to be used in a console app. i.e. search for config files in the in user home dir
     * or be specified by system properties and set up a console based login for missing db passwords
     *
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static void initInConsoleApp() throws ClassNotFoundException, IOException {
        File homeDbCredFile = new File(System.getProperty("user.home"), "gor.db.credentials");
        final String dbCredpath = homeDbCredFile.exists() ? homeDbCredFile.getCanonicalPath() : System.getProperty("gor.db.credentials");
        systemConnections.initializeDbSources(dbCredpath);

        File homeSqlCredFile = new File(System.getProperty("user.home"), "gor.sql.credentials");
        final String sqlCredpath = homeSqlCredFile.exists() ? homeSqlCredFile.getCanonicalPath() : System.getProperty("gor.sql.credentials");
        userConnections.initializeDbSources(sqlCredpath);

        setPasswordCallback(s -> {
            final Console console = System.console();
            return (console != null) ? new String(console.readPassword("Password for " + s.user + "@" + s.name + ": ")) : null;
        });
    }

    /**
     * Initialize DbSource to be used in a server.
     *
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("unused") // Called from GorQueryTask in gor-services
    public static void initInServer() throws ClassNotFoundException, IOException {
        final String dbCredpath = System.getProperty("gor.db.credentials");
        if (dbCredpath != null) {
            systemConnections.initializeDbSources(dbCredpath);
        }

        final String sqlCredpath = System.getProperty("gor.sql.credentials");
        if (sqlCredpath != null) {
            userConnections.initializeDbSources(sqlCredpath);
        }
    }

    /**
     * Register a callback object to perform password prompting when needed
     *
     * @param callback
     */
    public static void setPasswordCallback(DbConnection.GetPassword<DbConnection> callback) {
        pwdCallback = callback;
    }

}
