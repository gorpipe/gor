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

package org.gorpipe.gor.driver.meta;

import java.util.Arrays;

/**
 * Defines types of sources
 */
public abstract class SourceType {
    /**
     * FILE(false,"","file:"),
     * HTTP(true,"http:","https:"),
     * <p>
     * TCP(true,"tcp:"),
     * S3(true,"s3:"),
     * AZURE(true,"az:"),
     * DX(true,"dx:"),
     * DB(false,"db:") {
     * public boolean isSupported() {
     * return false;
     * }
     * },
     * MEM(false,"mem:"),
     * TEST(false,"test:");
     **/

    private final boolean isRemote;
    private final String[] protocols;
    private final String name;

    public SourceType(String name, boolean isRemote, String... protocols) {
        this.name = name.toUpperCase();
        this.isRemote = isRemote;
        this.protocols = protocols.clone();
    }

    public boolean isRemote() {
        return isRemote;
    }

    public boolean isSupported() {
        return true;
    }

    public boolean supportsPreparation() {
        return false;
    }

    // Default priority is 10000, 0 is highest priority.
    public int getPriority() {
        return 10000;
    }

    /**
     * @return true if the given path is absolute, else false.
     */
    public boolean isAbsolutePath(String path) {
        return path.contains(":") || path.startsWith("/");
    }

    public String[] getProtocols() {
        return protocols;
    }

    public String getName() {
        return name;
    }

    /**
     * Check if {@code file} matches the source type.
     *
     * @param file file to check.
     * @return true if {@code file} matches the source type, otherwise false.
     */
    public boolean match(String file) {
        // Should match our cases, which are normal protocols and //db:.
        for (String protocol : protocols) {
            if (file.startsWith(protocol)) return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SourceType other) {
            return name.equals(other.name) && isRemote == other.isRemote && Arrays.equals(protocols, other.protocols);
        }
        return false;
    }
}
