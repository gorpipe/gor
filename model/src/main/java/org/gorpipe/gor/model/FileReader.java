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

import com.google.common.base.Strings;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.utils.LinkFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.stream.Stream;

/**
 * FileReader allows reading of gor server managed files.
 */
public abstract class FileReader {

    private static final Logger log = LoggerFactory.getLogger(FileReader.class);

    /**
     * Tests whether a file exists.
     */
    public abstract boolean exists(String path);

    /**
     * Creates a new directory.
     * Returns: the directory
     */
    public abstract String createDirectory(String dir, FileAttribute<?>... attrs) throws IOException;

    public abstract String createDirectoryIfNotExists(String dir, FileAttribute<?>... attrs) throws IOException;
//        try {
//            log.trace("Creating directory {}", dir);
//            createDirectory(dir, attrs);
//        } catch (FileAlreadyExistsException faee) {
//            // Ignore, already created.
//            log.trace("Directory {} already exists", dir);
//        } catch (IOException e) {
//            if (e.getCause() != null && e.getCause() instanceof FileAlreadyExistsException) {
//                // Ignore, already created.
//                log.trace("Directory {} already exists", dir);
//            } else {
//                throw new GorSystemException("Could not create  directory: " + dir, e);
//            }
//
//        }
//        return dir;
//    }

    /**
     * Creates a new directory and its full path if needed.
     * Returns: the directory
     */
    public abstract String createDirectories(String dir, FileAttribute<?>... attrs) throws IOException;

    /**
     * Tests whether a file is a directory.
     */
    public abstract boolean isDirectory(String dir);

    public abstract String move(String source, String dest) throws IOException;

    public abstract String copy(String source, String dest) throws IOException;

    public abstract String streamMove(String source, String dest) throws IOException;

    public abstract String streamCopy(String source, String dest) throws IOException;

    public abstract void delete(String file) throws IOException;

    /**
     * Recursivly delete the given directory.
     * @param dir
     */
    public abstract void deleteDirectory(String dir) throws IOException;

    /**
     * Lists the contents of a directory.
     *
     * The elements of the stream are Strings that are obtained as if by resolving the name of the directory entry against dir.
     *
     * @param dir the directory to list
     * @return the contents of the directory.
     * @throws IOException
     */
    public abstract Stream<String> list(String dir) throws IOException;

    public abstract Stream<String> walk(String dir) throws IOException;

    //public abstract void deleteDirectory(String dir) throws IOException;



    /**
     * Read all content of the specified text file
     *
     * @param file The name of the file to read. This name is relative to the FileReader service, i.e. assume it is a root of a file system
     * @return A String array with each line of the file in consecutive order
     * @throws IOException If file is not found
     */
     public abstract String[] readAll(String file) throws IOException;

    /**
     * Open a RacFile access to the specified file.
     *
     * @param file The name of the file to read. This name is relative to the FileReader service, i.e. assume it is a root of a file system
     * @return RacFile object allowing random access to the underlying file
     * @throws IOException If file is not found
     */
    public abstract RacFile openFile(String file) throws IOException;

    /**
     * Get a Stream for the specified text file
     *
     * @param file The name of the file to read. This name is relative to the FileReader service, i.e. assume it is a root of a file system
     * @return A BufferedReader
     * @throws IOException If file is not found
     */
    public Stream<String> readFile(String file) throws IOException {
        BufferedReader br = getReader(file);
        Stream<String> lineStream = br.lines();
        lineStream.onClose(() -> {
            try {
                br.close();
            } catch (IOException e) {
                LoggerFactory.getLogger(FileReader.class).warn("Could not close file!", e);
            }
        });
        return lineStream;
    }

    /**
     * Get a path from resource
     *
     * @param resource
     * @return
     */
    public abstract Path toPath(String resource);

    public abstract Path toAbsolutePath(String resource);

    /**
     * Get a writer for the specified file
     * @param file
     * @return
     * @throws IOException
     */
    public OutputStream getOutputStream(String file) throws IOException {
        return getOutputStream(file, false);
    }

    /**
     * Get a writer for the specified file
     * @param file
     * @return
     * @throws IOException
     */
    public abstract OutputStream getOutputStream(String file, boolean append) throws IOException;

    /**
     * Get a Reader for the specified text file path
     *
     * @param path The path of the file to read. This name is relative to the FileReader service, i.e. assume it is a root of a file system
     * @return A BufferedReader
     * @throws IOException If file is not found
     */
    public abstract Reader getReader(Path path) throws IOException;

    /**
     * Get a BufferedReader for the specified text file
     *
     * @param file The name of the file to read. This name is relative to the FileReader service, i.e. assume it is a root of a file system
     * @return A BufferedReader
     * @throws IOException If file is not found
     */
    public abstract BufferedReader getReader(String file) throws IOException;

    /**
     * Get a InputStream for the specified text file
     *
     * @param file The name of the file to read. This name is relative to the FileReader service, i.e. assume it is a root of a file system
     * @return A InputStream
     * @throws IOException If file is not found
     */
    public abstract InputStream getInputStream(String file) throws IOException;

    /**
     * Get a BufferedReader for the specified text file
     *
     * @param file The name of the file to read. This name is relative to the FileReader service, i.e. assume it is a root of a file system
     * @return An Iterator
     * @throws IOException If file is not found
     */
    public abstract Stream<String> iterateFile(String file, int maxDepth, boolean followLinks, boolean showModificationDate) throws IOException;

    /**
     * Read the first line (the header) of the specified file
     *
     * @param file The name of the file to read. This name is relative to the FileReader service, i.e. assume it is a root of a file system
     * @return A String with the first line
     * @throws FileNotFoundException If file is not found
     */
    public abstract String readHeaderLine(String file) throws IOException;

    /**
     * Calculate a MD5 digest for the specified dictionary file based on the fullname of all physical file and last modification date given
     * for the specified tags. Note that physical file, implies that link files and symbolic links are read for their content
     * and the actual physical file used.
     *
     * @param dictionary A dictionary file
     * @param tags       The tags from the dictionary file that will be touched
     * @return The MD5 digest that is the file signature
     * @throws IOException if the file does not exist or an I/O error occurs
     */
    public abstract String getDictionarySignature(String dictionary, String[] tags) throws IOException;


    /**
     * Calculate a MD5 digest for the specified file based on the fullname of the physical file and last modification date. Note
     * that physical file, implies that link files and symbolic links are read for their content and the actual physical file used.
     *
     * @param file The file to get the File Signature from
     * @return The MD5 digest that is the file signature
     * @throws IOException
     */
    public abstract String getFileSignature(String file) throws IOException;

    /**
     *
     * @return securityContext string if any
     */
    public String getSecurityContext() {
        return null;
    }

    String convertUrl(String file) {
        if (file != null && !file.isEmpty()) {
            if (file.charAt(0) == '\"' || file.charAt(0) == '\'') { // If given a file with quotes, must remove the quotes
                final char lastChr = file.charAt(file.length() - 1);
                file = file.substring(1, lastChr == '\"' || lastChr == '\'' ? file.length() - 1 : file.length());
            }

            file = file.replace('\\', '/'); // Allow either backslash or slash
        }
        return file;
    }

    protected abstract void validateAccess(final DataSource dataSource);

    public boolean allowsAbsolutePaths() {
        return true;
    }

    /**
     * Resolve the given url, this includes traversing .link files and do fallback to link files if the file does not exits.
     *
     * @param url the url to resolve.
     * @return the resolved url.
     */
    public DataSource resolveUrl(String url) {
        return resolveUrl(url, false);
    }

    /**
     * Resolve the given url, this includes traversing .link files and do fallback to link files if the file does not exits.
     *
     * @param url the url to resolve.
     * @return the resolved url.
     */
    public abstract DataSource resolveUrl(String url, boolean writeable);

    public DataSource resolveUrl(SourceReference sourceReference) {
        throw new GorSystemException(String.format("This file reader (%s) does not support creating data sources", this.getClass().getName()), null);
    }

    /**
     * Resolve datasource without resolving links.
     * @param sourceReference source to resolve.
     * @return the resolved data source.
     */
    public abstract DataSource resolveDataSource(SourceReference sourceReference) throws IOException ;

    /**
     * Read link file and return the underlying file, following all link files.
     * @param url   path to the link file
     * @return the underlying file
     */
    public abstract String readLink(String url);

    public abstract String getCommonRoot();

    public abstract SourceReference createSourceReference(String url, boolean writeable);

    //
    public void writeLinkIfNeeded(String url) throws IOException {
        if (Strings.isNullOrEmpty(url)) return;
        DataSource dataSource = resolveUrl(url, true);
        if (dataSource.forceLink()) {
            LinkFile.create((StreamSource)dataSource, dataSource.getProjectLinkFileContent())
                    .save(getOutputStream(dataSource.getProjectLinkFile()));
        }
    }

    /**
     * Get unsecure version of this file reader, with same paths and security context.
     */
    public FileReader unsecure() {
        // Default implementation just return self.
        return this;
    }

    public Stream<SourceRef> prepareSources(Stream<SourceRef> sources) {
        return sources;
    }
}
