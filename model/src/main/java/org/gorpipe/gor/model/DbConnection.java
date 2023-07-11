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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * DbSource abstract access to database source that are configured on installation time.
 * Configuration is stored in a tab delimited formatted configuration file, specified by the java system key:
 * <p>
 * TODO:  This class needs some refactoring to handle the different databases more gracefully.  We should use some kind of
 * plugin mechanism (or Guice) instead of if/else statements.
 * Should also be renamed, as we have a DbSource class in the gor driver package.
 *
 * @version $Id$
 */
public class DbConnection {
    private static final Logger log = LoggerFactory.getLogger(DbConnection.class);
    public static final String DEFAULT_DBSOURCE = "rda";

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

    private static final ConcurrentHashMap<String, DbConnection> mapSources = new ConcurrentHashMap<>();
    private static GetPassword<DbConnection> pwdCallback = null;

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
        createConnectionPool();
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
        createConnectionPool();
        return pool != null ? pool.getConnection() : DriverManager.getConnection(url, user, pwd);
    }

    private void createConnectionPool() {
        pool = Db.getPool(url, user, pwd);
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
     * Lookup the specified source
     *
     * @param source The name of the source
     * @return The DbSource object
     */
    public static DbConnection lookup(String source) {
        return mapSources.get(source);
    }

    /**
     * Register a callback object to perform password prompting when needed
     *
     * @param callback
     */
    public static void setPasswordCallback(GetPassword<DbConnection> callback) {
        pwdCallback = callback;
    }

    /**
     * Read Database sources from configuration file
     *
     * @param credpath The path to the configuration file
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("WeakerAccess") // Used from gor-services
    public static void initializeDbSources(String credpath) throws ClassNotFoundException, IOException {
        clearDbSources();
        if (credpath != null && credpath.trim().length() > 0) {
            final Path path = Paths.get(credpath);
            if (Files.notExists(path)) {
                throw new FileNotFoundException("Specified db credentials file (" + credpath + ") is not found");
            }

            final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

            List<String[]> partsList = parseLinesForDbSourceInstallation(credpath, lines);

            for (String[] parts : partsList) {
                installDbSourceFromParts(parts);
            }
        } else {
            log.info("No db credential path specified");
        }
    }

    public static List<String[]> parseLinesForDbSourceInstallation(String credpath, List<String> lines) {
        int linecnt = 1;
        String partsSplitRegex = "\t";

        if (lines.size() == 1 && lines.get(0).contains("\\n")) {
            log.debug("Fixing up credentials data, since its in one line form with \\n and \\t");
            List<String> newLines = Arrays.asList(lines.get(0).split("\\\\n"));
            partsSplitRegex = "\\\\t";
            log.debug("New lines is {}", newLines);
            lines.clear();
            lines.addAll(newLines);
        }

        log.debug("Credentials data is {}, line count is {}", lines, lines.size());

        List<String[]> partsList = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // Name, Driver, URL, User [, Password]
            log.debug("Working with line {} of credentials data containing data {}", i, lines.get(i));
            if (lines.get(i).trim().startsWith("#")) {
                log.debug("Ignoring line {}", lines.get(i));
                continue;
            }

            String[] parts = lines.get(i).split(partsSplitRegex);
            if (parts.length >= 4) {
                partsList.add(parts);
            } else {
                log.error("Line {} in {} doesn't contain valid db source definition\n", linecnt, credpath);
            }
            linecnt++;
        }
        return partsList;
    }

    private static void installDbSourceFromParts(String[] parts) throws ClassNotFoundException {
        Class.forName(parts[1]); // Just load the driver once and for all
        final DbConnection source = new DbConnection(parts[0], parts[2], parts[3], parts.length > 4 ? parts[4] : null);
        install(source);
    }

    /**
     * Remove and disconnect all data sources previously loaded with initializeDbSources
     */
    public static void clearDbSources() {
        for (DbConnection src : mapSources.values()) {
            try {
                src.pool.close();
            } catch (SQLException e) {
                log.error("Exception when closing pool for db source: " + src.name, e);
            }
            mapSources.remove(src.name);
        }
        mapSources.clear();
    }


    /**
     * @param source The source to install as available
     */
    public static void install(final DbConnection source) {
        log.info("Installing DbSource with name {}, url {} and user {}", source.name, source.url, source.user);
        if (mapSources.containsKey(source.name)) {
            DbConnection existingSource = mapSources.get(source.name);
            log.warn("Installing over an existing source with name {}, url {} and user {}",
                    existingSource.name, existingSource.url, existingSource.user);
        }
        mapSources.put(source.name, source);
    }

    /**
     * Initialize DbSource to be used in a console app. i.e. search for gor.db.credentials file in the in user home dir
     * or be specified by gor.db.credentials system property and setup a console based login for missing db passwords
     *
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static void initInConsoleApp() throws ClassNotFoundException, IOException {
        File homeCredFile = new File(System.getProperty("user.home"), "gor.db.credentials");
        final String credpath = homeCredFile.exists() ? homeCredFile.getCanonicalPath() : System.getProperty("gor.db.credentials");
        initializeDbSources(credpath);
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
        final String credpath = System.getProperty("gor.db.credentials");
        if (credpath != null) {
            DbConnection.initializeDbSources(credpath);
        }
    }

    /**
     * Wrap a DbNorIterator into Stream as a proper source to db source link files out into the gor system.
     *
     * @param content
     * @param constants
     * @param source
     * @return Stream tsv formatted lines with header first and data lines following.
     */
    @SuppressWarnings("squid:S2095") //resource should not be closed since it being closed by the return object
    public static Stream<String> getDBLinkStream(String content, Map<String, Object> constants, String source) {
        final DbConnection dbsource = DbConnection.lookup(nullSafeSource(source));
        if (dbsource == null) {
            throw new GorResourceException("Error: Did not find database source named "+ nullSafeSource(source) +". ", content);
        }

        dbsource.createConnectionPool();
        DbNorIterator dbnor = new DbNorIterator(content, constants, dbsource.pool);
        Iterable<String> iterable = () -> dbnor;
        Stream<String> stream = StreamSupport.stream(iterable.spliterator(), false);
        stream.onClose(dbnor::close);
        return stream;
    }

    private static String nullSafeSource(String source) {
        return (source == null) ? DEFAULT_DBSOURCE : source;
    }
}
