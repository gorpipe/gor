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

package org.gorpipe.gor.db;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Integer.parseInt;

/**
 * Simple Database utilities class.
 */
public class Db {

    private final static Logger log = LoggerFactory.getLogger(Db.class);
    private static final Map<String, ConnectionPool> poolMap = new ConcurrentHashMap<>();
    private static final PrometheusMetricsTrackerFactory promFactory = new PrometheusMetricsTrackerFactory();
    /**
     * Create a ConnectionPool for the specified user, url and password and guaranty
     * there is only one pool created for each url, user combination.
     *
     * @param url  The URL for the database
     * @param user The username
     * @param pwd  The password
     * @return The connection pool as a ConnectionPool object
     */
    public static ConnectionPool getPool(String url, String user, String pwd) {
        // If pool exists then return a reference to it.
        String poolKey = ConnectionPool.composePoolKey(url, user);
        return poolMap.computeIfAbsent(poolKey, k -> new ConnectionPoolImpl(url, user, pwd));
    }

    private static class ConnectionPoolImpl implements ConnectionPool {
        private final String poolKey;
        private final HikariDataSource ds;
        private final Map<Connection, ConnectionInfo> connectionMap;
        private final int retryTimeout;


        /**
         * Create a ConnectionPool for the specified user, url and password
         *
         * @param url  The URL for the database
         * @param user The username
         * @param pwd  The password
         */
        ConnectionPoolImpl(String url, String user, String pwd) {
            this.poolKey = ConnectionPool.composePoolKey(url, user);
            this.ds = new HikariDataSource();
            this.connectionMap = new ConcurrentHashMap<>();

            if (url.startsWith("jdbc:postgresql:")) {
                ds.setDriverClassName("org.postgresql.Driver");
            } else if (url.startsWith("jdbc:oracle:")) {
                ds.setDriverClassName("oracle.jdbc.driver.OracleDriver");
            }
            ds.setJdbcUrl(url);
            ds.setUsername(user);
            ds.setPassword(pwd);
            ds.setMinimumIdle(parseInt(System.getProperty("gor.hikari.minimumidle", "1")));
            ds.setConnectionTimeout(parseInt(System.getProperty("gor.hikari.connectiontimeout", "5000")));
            retryTimeout = Integer.parseInt(System.getProperty("gor.hikari.connectionretrytimeout", "30000"));
            ds.setMaximumPoolSize(parseInt(System.getProperty("gor.hikari.poolsize", "20")));
            ds.setIdleTimeout(parseInt(System.getProperty("gor.hikari.idletimeout", "10000")));
            ds.setLeakDetectionThreshold(parseInt(System.getProperty("gor.hikari.leakdetectionthreashold", "30000")));
            ds.setAutoCommit(false);

            // The rest of the Hikari config has sane defaults but here are the options that can be adjusted:
            // https://github.com/brettwooldridge/HikariCP#frequently-used
            // We can also look into connecting Hikari with the Java Metrics and Health Checks library that we use:
            // https://github.com/brettwooldridge/HikariCP/wiki/Dropwizard-Metrics
            // https://github.com/brettwooldridge/HikariCP/wiki/Dropwizard-HealthChecks

            // Prometheus Metrics integration
            ds.setMetricsTrackerFactory(promFactory);

            ds.setPoolName(url.replace(":", "-") + "-" + user);
            ds.setRegisterMbeans(true);

            log.debug("Created Hikari Pool: " + ds.getPoolName() + " url:" + url + " u:" + user);
        }

        /**
         * Get a Connection from this pool.
         * TODO:
         *
         * @return Connection
         */
        public Connection getConnection() {
            Connection c = null;
            long startTime = System.currentTimeMillis();
            long endTime = startTime + retryTimeout;
            while (System.currentTimeMillis() < endTime && c == null) {
                try {
                    Connection connection = ds.getConnection();
                    ConnectionProxy proxy = new ConnectionProxy(connection, poolKey, connectionMap);
                    c = proxy.getProxy(Connection.class, connection);
                } catch (SQLException e) {
                    log.error("SQLException in DB.getConnection()", e);
                    System.gc();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        // Fashionably ignore.
                    }
                }
            }
            return c;
        }

        /**
         * Close this pool and remove from the parent poolMap so that it is not considered for
         * dispensing more Connections.
         *
         */
        public void close() {
            // This event has to be monitored very well as removal of a DataSource renders the
            // associated pool dysfunctional. Synchronized removal from poolMap to prevent a disaster.
            synchronized (poolMap) {
                poolMap.remove(poolKey);
                log.debug("Closing Hikari Pool: " + ds.getPoolName());
                ds.close();
            }
        }
    }
}
