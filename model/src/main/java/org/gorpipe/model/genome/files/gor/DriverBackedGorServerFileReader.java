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

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.ProjectContext;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.model.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

/**
 * Gor server file reader.
 * Extends Default file reader adding path restrictions and special handling of result cache directories.
 */
public class DriverBackedGorServerFileReader extends DriverBackedFileReader {
    private final boolean allowAbsolutePath;
    private static final String RESULT_CACHE_DIR = "cache/result_cache";

    /**
     * Create reader
     *
     * @param resolvedSessionRoot resolved session root
     * @param constants           The session constants available for file reader
     * @param allowAbsolutePath   allow absolute path
     */
    public DriverBackedGorServerFileReader(String resolvedSessionRoot, Object[] constants, boolean allowAbsolutePath, String securityContext) {
        super(securityContext, resolvedSessionRoot, constants);
        this.allowAbsolutePath = allowAbsolutePath;
    }

    @Override
    protected void checkValidServerFileName(final String fileName) {
        ProjectContext.validateServerFileName(fileName, allowAbsolutePath);
    }

    @Override
    public DataSource resolveUrl(String url) throws IOException {
        url = convertFileName2ServerPath(url);
        return super.resolveUrl(url);
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

    public static FileReader create(String resolvedSessionRoot, Object[] constants, boolean allowAbsolutePath, String securityContext) {
        if (GorFileReaderContext.fileReaderFallback()) {
            return new GorServerFileReader(resolvedSessionRoot, constants, allowAbsolutePath, securityContext);
        } else {
            return new DriverBackedGorServerFileReader(resolvedSessionRoot, constants, allowAbsolutePath, securityContext);
        }
    }
}
