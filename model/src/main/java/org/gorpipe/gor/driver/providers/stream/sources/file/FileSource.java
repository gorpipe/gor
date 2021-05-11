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

package org.gorpipe.gor.driver.providers.stream.sources.file;

import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSourceMetadata;
import org.gorpipe.gor.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Represents a data source accessed through file system.
 * <p>
 * Created by villi on 22/08/15.
 */
public class FileSource implements StreamSource {

    private static final Logger log = LoggerFactory.getLogger(FileSource.class);

    private final SourceReference sourceReference;
    private final Path file;
    private final boolean isSubset;
    private RandomAccessFile raf;

    // For debugging purposes, set the system property 'gor.filereader.stacktrace' to true
    // to keep track of the call stack when the 'raf' is opened. The call stack is then
    // logged out if the file is closed during finalize.
    private final boolean keepStackTraceForOpen = System.getProperty("gor.filereader.stacktrace", "false").equalsIgnoreCase("true");
    private Error rafOpenedStackTrace;

    /**
     * Name of file.  This should be the full path to the file.
     */
    public FileSource(String fileName, String subset) {
        this(new SourceReferenceBuilder(fileName).chrSubset(subset).build());
    }

    /**
     * Name of file.  This should be the full path to the file.
     */
    public FileSource(SourceReference sourceReference) {
        this(sourceReference, false);
    }

    /**
     * Name of file.  This should be the full path to the file.
     */
    public FileSource(SourceReference sourceReference, boolean isSubset) {
        String fileName = sourceReference.getUrl();
        if (fileName.startsWith("file://")) {
            fileName = fileName.substring(7);

            // Windows full path hack
            if (fileName.length() > 3 && fileName.charAt(2) == ':' && Util.isWindowsOS() ) {
                fileName = fileName.substring(1);
            }

            sourceReference = new SourceReference(fileName, sourceReference);
        }
        if (fileName.startsWith("file:")) {
            throw new IllegalArgumentException("Expected file url " + fileName + " to start with file://");
        }

        if (sourceReference.getCommonRoot() != null && !Paths.get(fileName).isAbsolute()) {
            fileName = Paths.get(sourceReference.getCommonRoot(), fileName).toString();
        }

        this.sourceReference = sourceReference;
        this.file = Paths.get(fileName);
        this.isSubset = isSubset;
    }

    @Override
    public InputStream open(long start) throws IOException {
        ensureOpen();
        raf.seek(start);
        return new FileSourceStream();
    }

    private void ensureOpen() throws IOException {
        if (raf == null) {
            if (keepStackTraceForOpen) {
                rafOpenedStackTrace = new Error();
            }
            raf = new RandomAccessFile(file.toFile(), "r");
        }
    }

    @Override
    public InputStream open(long start, long minLength) throws IOException {
        return open(start);
    }

    @Override
    public InputStream open() throws IOException {
        return open(0);
    }

    @Override
    public OutputStream getOutputStream(boolean append) throws IOException {
        var parent = file.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        return append ? Files.newOutputStream(file, StandardOpenOption.APPEND, StandardOpenOption.CREATE) : Files.newOutputStream(file);
    }

    @Override
    public boolean supportsWriting() {
        return true;
    }

    @Override
    public String getName() {
        return sourceReference.getUrl();
    }

    @Override
    public SourceType getSourceType() {
        return FileSourceType.FILE;
    }

    @Override
    public DataType getDataType() {
        return DataType.fromFileName(sourceReference.getUrl());
    }

    @Override
    public boolean exists() {
        return Files.exists(file);
    }

    @Override
    public StreamSourceMetadata getSourceMetadata() throws IOException {
        String uniqueId = null;
        // This is a performance hit on nfs, should find a way to make this more efficient
        /*File md5file = new File(file.getCanonicalPath() + ".md5");
        if( md5file.isFile() ){
            try (BufferedReader br = new BufferedReader(new FileReader(md5file))) {
                uniqueId = br.readLine();
            }
        }*/
        //return new StreamSourceMetadata(this, "file://" + file.toRealPath(), Files.getLastModifiedTime(file).toMillis(), Files.size(file), uniqueId, isSubset);
        File ffile = file.toFile();
        return new StreamSourceMetadata(this, "file://" + ffile.getCanonicalPath(), ffile.lastModified(), ffile.length(), uniqueId, isSubset);
    }

    @Override
    public SourceReference getSourceReference() {
        return sourceReference;
    }

    @Override
    public void close() throws IOException {
        if (raf != null) {
            raf.close();
            raf = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (raf != null) {
            String msg = "Datasource closed via finalize method: " + file.toAbsolutePath();
            if (rafOpenedStackTrace != null) {
                StringWriter sw = new StringWriter();
                rafOpenedStackTrace.printStackTrace(new PrintWriter(sw));
                log.warn("{}\nOpened at {}", msg, sw);
            } else {
                log.warn(msg);
            }
        }
        close();
    }

    /**
     * Implementation of an InputStream on top of a RandomAccessFile.
     * <p>
     * Closing the stream does not close the underlying RAF so it can be efficiently reused by
     * subsequent opens.
     */
    public class FileSourceStream extends InputStream {
        private long mark;

        @Override
        public int read() throws IOException {
            return raf.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return raf.read(b, off, len);
        }

        @Override
        public void close() {
            // No op
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(raf.length() - raf.getFilePointer(), Integer.MAX_VALUE);
        }

        @Override
        public long skip(long n) throws IOException {
            raf.seek(raf.getFilePointer() + n);
            return n;
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public synchronized void mark(int readlimit) {
            try {
                this.mark = raf.getFilePointer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized void reset() throws IOException {
            raf.seek(mark);
        }

        @Override
        public int read(byte[] b) throws IOException {
            return raf.read(b);
        }

        @Override
        public String toString() {
            return raf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public int hashCode() {
            return raf.hashCode();
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException(this.getClass().getName());
        }
    }
}
