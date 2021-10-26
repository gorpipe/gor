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

import org.apache.commons.io.FileUtils;
import org.apache.parquet.Strings;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.PathUtils;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Default reader, useable by anyone
 */
public class DefaultFileReader extends FileReader {
    private static final Logger log = LoggerFactory.getLogger(DefaultFileReader.class);

    private final String securityContext;

    public DefaultFileReader(String securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    public String getSecurityContext() {
        return securityContext;
    }

    @Override
    protected void checkValidServerFileName(String fileName) {
        // Not used in this context
    }

    @Override
    public boolean exists(String file) {
        return Files.exists(PathUtils.toPath(file));
    }

    @Override
    public String createDirectory(String dir, FileAttribute<?>... attrs) throws IOException {
        return Files.createDirectory(PathUtils.toPath(dir), attrs).toString();
    }

    @Override
    public String createDirectories(String dir, FileAttribute<?>... attrs) throws IOException {
        return Files.createDirectories(PathUtils.toPath(dir), attrs).toString();
    }

    @Override
    public boolean isDirectory(String dir) {
        return Files.isDirectory(PathUtils.toPath(dir));
    }

    @Override
    public String move(String source, String dest) throws IOException {
        return Files.move(PathUtils.toPath(source), PathUtils.toPath(dest),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE).toString();
    }

    @Override
    public String copy(String source, String dest) throws IOException {
        return Files.copy(PathUtils.toPath(source), PathUtils.toPath(dest)).toString();
    }

    @Override
    public void delete(String file) throws IOException {
        Files.delete(PathUtils.toPath(file));
    }

    @Override
    public void deleteDirectory(String dir) throws IOException {
        FileUtils.deleteDirectory(new File(dir));
    }

    @Override
    public Stream<String> list(String dir) throws IOException {
        return Files.list(PathUtils.toPath(dir)).map(p -> p.toString());
    }


//    @Override
//    public void deleteDirectory(String dir) throws IOException {
//        Files.delete(dir)
//    }

    @Override
    public String[] readAll(String file) {
        try {
            file = checkLink(file);
            final List<String> lines = readlines(file);
            return lines.toArray(new String[lines.size()]);
        } catch (Exception ex) {
            throw new GorSystemException("Could not read file: " + file, ex);
        }
    }

    private List<String> readlines(String file) throws IOException {
        try (BufferedReader br = new BufferedReader(new BufferedReader(new java.io.FileReader(file)))) {
            ArrayList<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        }
    }

    @Override
    public String readHeaderLine(String file) {
        try {
            file = checkLink(file);
        } catch (IOException ex) {
            throw new GorSystemException("Could not read header line from link file: " + file, ex);
        }

        try (BufferedReader r = new BufferedReader(new java.io.FileReader(file))) {
            return r.readLine();
        } catch (IOException ex) {
            throw new GorSystemException("Could not read header line from file: " + file, ex);
        }
    }

    @Override
    public BufferedReader getReader(String file) throws IOException {
        try {
            file = checkLink(file);
        } catch (IOException ex) {
            throw new GorSystemException("Could not read link file: " + file, ex);
        }
        return new BufferedReader(new java.io.FileReader(file));
    }

    @Override
    public OutputStream getOutputStream(String file, boolean append) throws IOException {
        return new FileOutputStream(file, append);
    }

    @Override
    public InputStream getInputStream(String file) throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public Stream<String> iterateFile(String fileName, int maxDepth, boolean followLinks, boolean showModificationDate) throws IOException {
        final String file = resolveUrl(fileName, "", securityContext);

        if (file.startsWith("//db:")) {
            return DbSource.getDBLinkStream(file, new Object[]{});
        }

        File f = new File(file);
        if (f.isDirectory()) {
            Path path = f.toPath();
            Path root = Paths.get("");
            return getDirectoryStream(maxDepth, followLinks, showModificationDate, path, root);
        }

        BufferedReader bufferedReader = new BufferedReader(new java.io.FileReader(f));
        final Stream<String> lineStream = bufferedReader.lines();
        return lineStream.onClose(() -> {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                log.warn("Could not close file!", e);
            }
        });
    }

    static Stream<String> getDirectoryStream(int maxDepth, boolean followLinks, boolean showModificationDate, Path path, Path root) throws IOException {
        var pstream = followLinks ? Files.walk(path, maxDepth, FileVisitOption.FOLLOW_LINKS) : Files.walk(path, maxDepth);
        var stream = pstream.map(x -> {
            try {
                Path fileNamePath = x.getFileName();
                if (fileNamePath == null) {
                    throw new GorResourceException("Directory is not accessible", path.toString());
                }
                String filename = fileNamePath.toString();
                int li = filename.lastIndexOf('.');
                Path rel = root != null && !Strings.isNullOrEmpty(root.toString()) && x.isAbsolute() ? root.relativize(x) : x;
                String line = filename + "\t" + (Files.isSymbolicLink(x) ? 0 : Files.size(x)) + "\t" + Files.isDirectory(x) + "\t" + Files.isSymbolicLink(x) + "\t" + filename.substring(li == -1 ? filename.length() : li + 1) + "\t" + rel + "\t" + rel.toString().chars().filter(y -> y == '/').count();

                if (showModificationDate) {
                    line += "\t" + Files.getLastModifiedTime(x, LinkOption.NOFOLLOW_LINKS);
                }

                return line;
            } catch (IOException e) {
                throw new GorSystemException("Unable to get file size from " + x, e);
            }
        });
        String header = "#Filename\tFilesize\tIsDir\tIsSymbolic\tFiletype\tFilepath\tFiledepth" + (showModificationDate ? "\tModified" : "");
        return Stream.concat(Stream.of(header), stream);
    }

    @Override
    public RacFile openFile(String file) throws FileNotFoundException {
        return new GCRacFile(file);
    }

    @Override
    public Path toPath(String resource) {
        return Paths.get(resource);
    }

    @Override
    public BufferedReader getReader(Path path) throws IOException {
        try {
            path = Paths.get(checkLink(path));
        } catch (IOException ex) {
            throw new GorSystemException("Could not read link file: " + path, ex);
        }
        return Files.newBufferedReader(path);
    }

    public String getDictionarySignature(String dictionary, String[] tags) throws IOException {
        return new DictionaryTable.Builder<>(dictionary).securityContext(securityContext).build().getSignature(tags);
    }


    public String getFileSignature(String file) throws IOException {
        return GorOptions.getFileSignature(file, securityContext);
    }

    public static String checkLink(String file) throws IOException {
        return checkLink(Paths.get(file));
    }

    public static String checkLink(Path lp) throws IOException {
        if (lp.endsWith(".link")) {
            final List<String> lines = Files.readAllLines(lp);
            if (!lines.isEmpty()) {
                Path p = Paths.get(lines.get(0));
                if (!p.isAbsolute()) {
                    p = lp.getParent().resolve(p);
                }
                return p.toString();
            }
        }
        return lp.toString();
    }
}
