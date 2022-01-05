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
import org.gorpipe.gor.driver.PluggableGorDriver;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Gor server file reader.
 * Extends Default file reader adding path restrictions and special handling of result cache directories.
 */
public class DriverBackedGorServerFileReader extends DriverBackedFileReader {
    private final boolean allowAbsolutePath;
    private static final String RESULT_CACHE_DIR = "cache/result_cache";

    private List<String> writeLocations;

    /**
     * Create reader
     *
     * @param commonRoot resolved session root
     * @param constants           The session constants available for file reader
     * @param allowAbsolutePath   allow absolute path
     */
    public DriverBackedGorServerFileReader(String commonRoot, Object[] constants, boolean allowAbsolutePath,
                                           String securityContext, List<String> writeLocations) {
        super(securityContext, commonRoot, constants);
        this.allowAbsolutePath = allowAbsolutePath;
        this.writeLocations = writeLocations != null ? new ArrayList<>(writeLocations) : new ArrayList<>();
        this.writeLocations.add("result_cache");
    }

    @Override
    public String getDictionarySignature(String dictionary, String[] tags) throws IOException {
        if (dictionary.startsWith(RESULT_CACHE_DIR)) { // Files in Result Cache can be assumed to never change
            return Util.md5(dictionary);
        }
        return super.getDictionarySignature(dictionary, tags);
    }

    @Override
    Stream<String> directDbUrl(String resolvedUrl) {
        throw new GorSystemException("Direct queries on db urls not allowed on server. Trying to open " + resolvedUrl, null);
    }

    @Override
    public String getFileSignature(String file) throws IOException {
        // Notes:
        // 1. Handling of symbolic links:  We don't need any special handling of symbolic links as each file is resolved to its canonical form before getting
        //    the signature.  This is simpler and better for the caching (as two different queries using different links but the same actual files will
        //    hit the same cache).

        // Use md5 signature if it is available
        File md5file = new File(commonRoot + file + ".md5");
        if (md5file.exists()) {
            List<String> lines = java.nio.file.Files.readAllLines(md5file.toPath());
            if (!lines.isEmpty()) {
                return lines.get(0).trim();
            }
        }

        // Old cache fallback
        if (file.startsWith(RESULT_CACHE_DIR)) { // Files in Result Cache can be assumed to never change
            return Util.md5(file);
        }

        // Standard fallback
        return super.getFileSignature(file);
    }

    public boolean allowsAbsolutePaths() {
        return false;
    }

    @Override
    public void validateAccess(final DataSource dataSource) {
        if (dataSource.getSourceReference().isWriteSource()) {
            validateWriteAccess(dataSource);
        } else {
            validateReadAccess(dataSource);
        }
    }

    private void validateReadAccess(DataSource source) throws GorResourceException {
        validateServerFileName(source.getAccessValidationPath(), commonRoot, allowAbsolutePath);
    }

    void validateWriteAccess(DataSource source) throws GorResourceException {
        validateServerFileName(source.getAccessValidationPath(), commonRoot, allowAbsolutePath);
        isWithinAllowedFolders(source, writeLocations, commonRoot);

        
        if (source.getAccessValidationPath().toLowerCase().endsWith(".link")) {
            throw new GorResourceException("Writing link files is not allowed", null);
        }
    }

    public static void isWithinAllowedFolders(DataSource dataSource, List<String> writeLocations, String commonRoot) {
        for (String location : writeLocations) {
            if (PathUtils.resolve(Path.of(commonRoot), dataSource.getAccessValidationPath())
                    .startsWith(PathUtils.resolve(commonRoot, location))) {
                return;
            }
        }
        String message = String.format("Invalid File Path: File path not within folders allowed! Path given: %s. " +
                "Write locations are %s", dataSource.getAccessValidationPath(), Arrays.toString(writeLocations.toArray()));
        throw new GorResourceException(message, dataSource.getAccessValidationPath());
    }

    public static void validateServerFileName(String filename, String projectRoot, boolean allowAbsolutePath) throws GorResourceException {
        if (PathUtils.isLocal(filename) && !allowAbsolutePath) {
            Path filePath = Paths.get(filename);
            var realProjectRoot = Paths.get(projectRoot);
            if (!filePath.isAbsolute()) {
                filePath = realProjectRoot.resolve(filePath);
            }
            filePath = PathUtils.relativize(realProjectRoot, filePath);
            if (filePath.isAbsolute() || !filePath.normalize().equals(filePath)) {
                String message = String.format("Invalid File Path: File paths must be within project scope! Path given: %s, Project root is: %s", filename, projectRoot);
                throw new GorResourceException(message, filename);
            }
        }
    }
}
