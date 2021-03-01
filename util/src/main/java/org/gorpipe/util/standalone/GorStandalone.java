/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.util.standalone;

import java.io.File;

public class GorStandalone {
    public static final String STANDALONE_PROPERTY_NAME = "sm.standalone";
    public static final String STANDALONE_FILE_CACHE_USE_SUBFOLDERS = "sm.standalone.filecache.usesubfolders";
    public static final String STANDALONE_FILE_CACHE_SUBFOLDER_SIZE = "sm.standalone.filecache.subfoldersize";

    private GorStandalone() {}

    public static String getStandaloneRoot() {
        return System.getProperty(STANDALONE_PROPERTY_NAME);
    }

    public static boolean getResultCacheUseSubfolders() {
        return Boolean.parseBoolean(System.getProperty(STANDALONE_FILE_CACHE_USE_SUBFOLDERS, "true"));
    }

    public static Integer getResultCacheSubfolderSize() {
        return Integer.parseInt(System.getProperty(STANDALONE_FILE_CACHE_SUBFOLDER_SIZE, "2"));
    }

    public static boolean isStandalone() {
        String standalone = getStandaloneRoot();
        return standalone != null && standalone.length() > 0;
    }

    public static String getStandaloneRootName() {
        String projectPath = getStandaloneRoot();
        return projectPath.substring(projectPath.lastIndexOf('/') + 1);
    }

    public static boolean isURL(String leafPath) {
        return leafPath.matches("^[A-Za-z0-9]+://.*$") || leafPath.startsWith("//db:");
    }

    public static String getRootPrefixed(String leafPath) {
        if (isAbsolutePath(leafPath)) {
            String fullPath = (getStandaloneRoot() + File.separator + leafPath).replaceAll("//", "/");
            File existsInProjectFile = new File(fullPath);
            if (existsInProjectFile.exists()) {
                leafPath = leafPath.substring(1);
            }
        }

        if (isStandalone() && !isAbsolutePath(leafPath) && !leafPath.startsWith(getStandaloneRoot())) {
            leafPath = (getStandaloneRoot() + File.separator + leafPath).replaceAll("//", "/");
        }

        return leafPath;
    }

    //TODO: tolli 2018-04-07 Might want to consider file: as special case and perform prefix on path.
    public static String getRootPrefixedExclUrl(String leafPath) {
        if (!isURL(leafPath)) {
            leafPath =  getRootPrefixed(leafPath);
        }
        return leafPath;
    }

    private static boolean isAbsolutePath(String leafPath) {
        return (leafPath.length() >= 2 && leafPath.charAt(1) == ':') || (leafPath.length() >= 1 && leafPath.charAt(0) == '/');
    }
}
