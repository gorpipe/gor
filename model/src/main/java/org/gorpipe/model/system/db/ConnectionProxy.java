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

package org.gorpipe.model.system.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Wrapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper class to track connections that are leased out of the pool.
 */
class ConnectionProxy implements Wrapper {

    private final static Logger log = LoggerFactory.getLogger(ConnectionProxy.class);
    private final Connection connection;
    private final Map<Connection, ConnectionInfo> connectionMap;
    private final StackTraceElement[] stackArray;

    ConnectionProxy(
            Connection connection,
            String poolKey,
            Map<Connection, ConnectionInfo> connectionMap
    ) {
        this.connection = connection;
        this.connectionMap = connectionMap;
        connectionMap.put(connection, new ConnectionInfo(poolKey, Instant.now()));
        Exception e = new RuntimeException("");
        stackArray = e.getStackTrace();

        if (log.isDebugEnabled()) {
            log.debug("Constructing ConnectionProxy: " + consumerStackTrace());
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
        return (T) connection;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(Connection.class);
    }

    <T> T getProxy(Class<T> intf,
                   final T obj) {
        return (T)
                Proxy.newProxyInstance(obj.getClass().getClassLoader(),
                        new Class[]{intf},
                        (proxy, method, args) -> {
                            String methodName = method.getName();
                            switch (methodName) {
                                case "close":
                                    connectionMap.remove(connection);
                                    connection.close();
                                    break;
                            }

                            return method.invoke(obj, args);
                        });
    }

    protected void finalize() throws Throwable {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                log.error("Connection not closed before finalize: " + consumerStackTrace());
            }
        } finally {
            super.finalize();
        }
    }

    public String consumerStackTrace() {
        return Arrays.asList(stackArray).stream()
                .map(i -> i.toString())
                .collect(Collectors.joining("\n"));

    }


}
