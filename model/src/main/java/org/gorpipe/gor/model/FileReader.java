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

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * FileReader allows reading of gor server managed files.
 */
public abstract class FileReader {
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
    public abstract Stream<String> iterateFile(String file, int maxDepth, boolean showModificationDate) throws IOException;

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

    String convertFileName2ServerPath(String file) {
        if (file.charAt(0) == '\"' || file.charAt(0) == '\'') { // If given a file with quotes, must remove the quotes
            final char lastChr = file.charAt(file.length() - 1);
            file = file.substring(1, lastChr == '\"' || lastChr == '\'' ? file.length() - 1 : file.length());
        }

        file = file.replace('\\', '/'); // Allow either backslash or slash

        checkValidServerFileName(file);

        return file;
    }

    String resolveUrl(String url, String resolvedSessionRoot, String securityContext) throws IOException {
        SourceReference sourceReference = new SourceReferenceBuilder(url).commonRoot(resolvedSessionRoot).securityContext(securityContext).build();

        DataSource ds = GorDriverFactory.fromConfig().getDataSource(sourceReference);

        String cannonicalName = ds.getSourceMetadata().getCanonicalName();
        return cannonicalName.startsWith("file://") ? cannonicalName.substring(7) : cannonicalName;
    }

    protected abstract void checkValidServerFileName(final String fileName);
}
