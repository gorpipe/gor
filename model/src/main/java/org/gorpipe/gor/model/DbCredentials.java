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

/**
 * Credentials for a single database source, supplied programmatically rather than through the
 * credentials file.
 *
 * This exists so that a host application can source credentials however it likes — from its own
 * configuration, a secret manager, or environment variables it owns the naming of — and hand them to
 * {@link DbConnectionCache#initializeDbSources(String, java.util.List)} without gor needing to know
 * where they came from.
 *
 * Credentials passed this way are installed before the credentials file is read, so a file row with
 * the same name takes precedence.
 *
 * @param name   the source name, e.g. "rda". Required.
 * @param url    the jdbc url. Required.
 * @param user   the database user. Required.
 * @param pwd    the password. May be null.
 * @param driver the jdbc driver class. May be null, in which case it is derived from the url prefix.
 */
public record DbCredentials(String name, String url, String user, String pwd, String driver) {

    /**
     * Credentials with the driver derived from the url prefix.
     */
    public DbCredentials(String name, String url, String user, String pwd) {
        this(name, url, user, pwd, null);
    }
}
