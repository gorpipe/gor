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
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.util.StringUtil;
import org.gorpipe.gor.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Gor server file reader
 *
 * @TODO: Remove once use of DriverBackedGorServerFileReader is proven.
 */
public class GorServerFileReader extends FileReader {
    private static final Logger log = LoggerFactory.getLogger(GorServerFileReader.class);

    public static final String RESULT_CACHE_DIR = "cache/result_cache";
    private final String resolvedSessionRoot;
    private final boolean allowAbsolutePath;
    private final Object[] constants;
    private final String securityContext;

    /**
     * Create reader
     *
     * @param resolvedSessionRoot resolved session root
     * @param constants           The session constants available for file reader
     * @param allowAbsolutePath   allow absolute path
     */
    public GorServerFileReader(String resolvedSessionRoot, Object[] constants, boolean allowAbsolutePath, String securityContext) {
        this.resolvedSessionRoot = resolvedSessionRoot;
        this.constants = constants;
        this.allowAbsolutePath = allowAbsolutePath;
        this.securityContext = securityContext;
    }

    @Override
    protected void checkValidServerFileName(final String fileName) {
        if (!allowAbsolutePath && fileName.length() > 2 && (fileName.charAt(0) == '/' || fileName.charAt(1) == ':')) {
            throw new GorResourceException("Invalid File Path: Absolute paths for files are not allowed!", fileName);
        }
        if (!allowAbsolutePath && (fileName.equals("..") || fileName.contains("../"))) {
            throw new GorResourceException("Invalid File Path: Filepaths are not allowed to reference parent folders!", fileName);
        }
    }

    /**
     * Resolve the given url, this includes traversing .link files and do fallback to link files if the file does not exits.
     *
     * @param url the url to resolve.
     * @return the resolved url.
     */
    private String resolveUrl(String url) throws IOException {
        url = convertFileName2ServerPath(url);
        return resolveUrl(url, resolvedSessionRoot, securityContext);
    }

    @Override
    public String getSecurityContext() {
        return securityContext;
    }

    @Override
    public String getDictionarySignature(String dictionary, String[] tags) throws IOException {
        if (dictionary.startsWith(RESULT_CACHE_DIR)) { // Files in Result Cache can be assumed to never change
            return Util.md5(dictionary);
        }

        final String fileName = resolveUrl(dictionary);
        return new DictionaryTable.Builder<>(Paths.get(fileName)).securityContext(securityContext).build().getSignature(true, resolvedSessionRoot, tags);
    }

    @Override
    public String getFileSignature(String file) throws IOException {
        // Notes:
        // 1. Handling of symbolic links:  We don't need any special handling of symbolic links as each file is resolved to its canonical form before getting
        //    the signature.  The is simpler and better for the caching (as two different queries using different links but the same actual files will
        //    hit the same cache).

        // Use md5 signature if it is available
        File md5file = new File(file + ".md5");
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
        String fileName = resolveUrl(file);
        return GorOptions.getFileSignature(fileName, securityContext);
    }

    @Override
    public String[] readAll(String file) throws IOException {
        try (Stream<String> s = readFile(file)) {
            return s.toArray(size -> new String[size]);
        }
    }

    @Override
    public String readHeaderLine(String file) throws IOException {
        final String url = resolveUrl(file);

        if (url.startsWith("//db:")) {
            final int idxSelect = url.indexOf("select ");
            final int idxFrom = url.indexOf(" from ");
            if (idxSelect < 0 || idxFrom < 0) { // Must find columns
                return null;
            }
            final ArrayList<String> fields = StringUtil.split(url, idxSelect + 7, idxFrom, ',');
            final StringBuilder header = new StringBuilder(200);
            for (String f : fields) {
                if (header.length() > 0) {
                    header.append('\t');
                } else {
                    header.append('#');
                }
                final int idxAs = f.indexOf(" as ");
                if (idxAs > 0) {
                    header.append(f.substring(idxAs + 4).trim());
                } else {
                    final int idxPoint = f.indexOf('.');
                    header.append(f.substring(idxPoint > 0 ? idxPoint + 1 : 0).trim());
                }
            }
            return header.toString();
        }

        try (BufferedReader r = new BufferedReader(new java.io.FileReader(url))) {
            return r.readLine();
        }
    }

    @Override
    public RacFile openFile(String file) throws IOException {
        String resolvedUrl = resolveUrl(file);
        return new GCRacFile(resolvedUrl);
    }

    @SuppressWarnings("squid:S2095") //resource is closed by return object
    @Override
    public Stream<String> readFile(String file) throws IOException {
        String resolvedUrl = resolveUrl(file);

        if (resolvedUrl.startsWith("//db:")) {
            return DbSource.getDBLinkStream(resolvedUrl, constants);
        }

        Stream<String> lineStream;
        BufferedReader br = new BufferedReader(new java.io.FileReader(resolvedUrl));
        lineStream = br.lines();
        lineStream.onClose(() -> {
            try {
                br.close();
            } catch (IOException e) {
                log.warn("Could not close file!", e);
            }
        });

        return lineStream;
    }

    @Override
    public Path toPath(String resource) {
        return Paths.get(resource);
    }

    @Override
    public Reader getReader(Path path) throws IOException {
        return getReader(path.toString());
    }

    @Override
    public BufferedReader getReader(String file) throws IOException {
        String resolvedUrl = resolveUrl(file);
        return new BufferedReader(new java.io.FileReader(resolvedUrl));
    }

    @Override
    public Stream<String> iterateFile(String file, int maxDepth, boolean showModificationDate) throws IOException {
        String resolvedUrl = resolveUrl(file);

        if (resolvedUrl.startsWith("//db:")) {
            return DbSource.getDBLinkStream(resolvedUrl, constants);
        }

        File f = new File(resolvedUrl);
        if (f.isDirectory()) {
            Path path = f.toPath();
            Path root = Paths.get(resolvedSessionRoot);
            return DefaultFileReader.getDirectoryStream(maxDepth, showModificationDate, path, root);
        }

        try (BufferedReader bufferedReader = new BufferedReader(new java.io.FileReader(f))) {
            return bufferedReader.lines();
        }
    }
}
