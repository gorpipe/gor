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

import org.apache.parquet.Strings;
import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.PluggableGorDriver;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.adapters.StreamSourceRacFile;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.providers.rows.RowIteratorSource;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.StreamSourceProvider;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSourceType;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.gor.util.StringUtil;
import org.gorpipe.util.standalone.GorStandalone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

/**
 * Created by villi on 06/01/17.
 */
public class DriverBackedFileReader extends FileReader {

    private static final Logger log = LoggerFactory.getLogger(DriverBackedFileReader.class);

    private static final String DEFAULT_COMMON_ROOT = "./";

    private final boolean DEPENDENTS = System.getProperty("gor.filereader.dependents", "true").equalsIgnoreCase("true");

    private final String securityContext;
    protected final String commonRoot;

    public DriverBackedFileReader(String securityContext) {
        this(securityContext, null);
    }

    public DriverBackedFileReader(String securityContext, String commonRoot) {
        this.securityContext = securityContext;
        if ((commonRoot == null || commonRoot.length() < 1) && GorStandalone.isStandalone()) {
            this.commonRoot = PathUtils.markAsFolder(GorStandalone.getStandaloneRoot());
        } else {
            this.commonRoot = StringUtil.isEmpty(commonRoot) ? DEFAULT_COMMON_ROOT :  PathUtils.markAsFolder(commonRoot);
        }
    }

    @Override
    public String getCommonRoot() {
        return commonRoot;
    }

    @Override
    public SourceReference createSourceReference(String url, boolean writeable) {
        url = convertUrl(url);
        return new SourceReferenceBuilder(url).commonRoot(commonRoot).securityContext(securityContext).writeSource(writeable).build();
    }

    @Override
    public DataSource resolveUrl(String url, boolean writeable) {
        return resolveUrl(createSourceReference(url, writeable));
    }

    @Override
    public DataSource resolveUrl(SourceReference sourceReference) {
        DataSource dataSource =  GorDriverFactory.fromConfig().getDataSource(sourceReference);
        if (dataSource != null) {
            validateAccess(dataSource);
        } else {
            log.warn("No source found for {}", sourceReference.getUrl());
        }
        return dataSource;
    }

    /* Resolve without doing wrap and links
     */
    @Override
    public DataSource resolveDataSource(SourceReference sourceReference) throws IOException {
        DataSource dataSource =  GorDriverFactory.fromConfig().resolveDataSource(sourceReference);
        if (dataSource != null) {
            validateAccess(dataSource);
        } else {
            log.warn("No source found for {}", sourceReference.getUrl());
        }
        return dataSource;
    }

    @Override
    public String readLinkContent(String url) {
        try (DataSource source = resolveDataSource(createSourceReference(url, false))) {
            if (source == null) {
                throw new GorResourceException("Could not read link, invalid uri", url, null);
            } else {
                return GorDriverFactory.fromConfig().readLink(source);
            }
        } catch (IOException e) {
            throw new GorResourceException("Could not read link", url, e);
        }
    }

    @Override
    public String getSecurityContext() {
        return securityContext;
    }

    @Override
    protected void validateAccess(DataSource dataSource) {
        // This is not used for the standard reader
    }

    @Override
    public boolean exists(String file) {
        try {
            return resolveUrl(file).exists();
        } catch (GorResourceException gre) {
            log.warn("Exists:  Resolve got exception.  Assuming file does not exits.", gre);
            return false;
        }
    }

    @Override
    public String createDirectory(String dir, FileAttribute<?>... attrs) throws IOException {
        return resolveUrl(dir, true).createDirectory(attrs);
    }

    @Override
    public String createDirectoryIfNotExists(String dir, FileAttribute<?>... attrs) throws IOException {
        return resolveUrl(dir, true).createDirectoryIfNotExists(attrs);
    }

    @Override
    public String createDirectories(String dir, FileAttribute<?>... attrs) throws IOException {
        return resolveUrl(dir, true).createDirectories(attrs);
    }

    @Override
    public boolean isDirectory(String dir) {
        return dir.endsWith("/") || resolveUrl(dir).isDirectory();
    }

    @Override
    public String move(String source, String dest) throws IOException {
        DataSource sourceSource = resolveUrl(source);
        DataSource destSource = resolveUrl(dest, true);

        if (DEPENDENTS) {
            getDependentDestStream(source, dest, sourceSource).forEach(p -> p[0].move(p[1]));
        }

        String destPath = sourceSource.move(destSource);

        return destPath;
    }

    @Override
    public String copy(String source, String dest) throws IOException {
        DataSource sourceSource = resolveUrl(source);
        DataSource destSource = resolveUrl(dest, true);

        if (DEPENDENTS) {
            getDependentDestStream(source, dest, sourceSource).forEach(p -> p[0].copy(p[1]));
        }

        String destPath = sourceSource.copy(destSource);

        return destPath;
    }

    @Override
    public String streamMove(String source, String dest) throws IOException {
        if (!source.equals(dest)) {
            streamCopy(source, dest);
            delete(source);
        }
        return dest;
    }

    @Override
    public String streamCopy(String source, String dest) throws IOException {
        if (!source.equals(dest)) {
            try (InputStream inputStream = getInputStream(source);
                 OutputStream outputStream = getOutputStream(dest)) {
                inputStream.transferTo(outputStream);
            }
        }
        return dest;
    }

    @Override
    public void delete(String file) throws IOException {
        // Resolve as writeable (will not resolve links).
        DataSource source = resolveUrl(file, true);
        if (DEPENDENTS) {
            List<DataSource> dependents = getValidatedDependents(source);
            for (DataSource dependent : dependents) {
                dependent.delete();
            }
        }
        source.delete();
    }

    @Override
    public void deleteDirectory(String dir) throws IOException {
        dir = PathUtils.markAsFolder(dir);
        resolveUrl(dir, true).deleteDirectory();
    }

    @Override
    public Stream<String> list(String dir) throws IOException {
        return resolveUrl(dir).list();
    }

    @Override
    public Stream<String> walk(String dir) throws IOException {
        return resolveUrl(dir).walk();
    }

    @Override
    public String[] readAll(String file) throws IOException {
        try (Stream<String> s = readFile(file)) {
            return s.toArray(String[]::new);
        }
    }

    @Override
    public String readHeaderLine(String file) throws IOException {
        final DataSource source = resolveUrl(file);

        try(var it = GorDriverFactory.fromConfig().createIterator(source)) {
            if (it!=null) {
                return it.getHeader();
            } else {
                return loadRawHeader(source);
            }
        } catch (GorException e) {
            return loadRawHeader(source);
        } finally {
            source.close();
        }
    }

    private String loadRawHeader(DataSource source) throws IOException {
        try (InputStream str = ((StreamSource) source).open()) {
            BufferedReader r = new BufferedReader(new InputStreamReader(str));
            return r.readLine();
        }
    }

    @Override
    public Stream<String> readFile(String file) throws IOException {
        return readAndClose(resolveUrl(file));
    }

    @Override
    public Path toPath(String resource) {
        return Path.of(resource);
    }

    @Override
    public Path toAbsolutePath(String resource) {
        return commonRoot!=null&&commonRoot.length()>0 ? Path.of(commonRoot).resolve(resource) : Path.of(resource);
    }

    @Override
    public BufferedReader getReader(Path path) throws IOException {
        return getReader(path.toString());
    }

    @Override
    public BufferedReader getReader(String resource) throws IOException {
        DataSource source = resolveUrl(resource);
        String resolvedUrl = PathUtils.fixFileSchema(source.getName());
        return readSource(source, resolvedUrl);
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
        DataSource source = resolveUrl(resource);
        return ((StreamSource)source).openClosable();
    }

    @Override
    public OutputStream getOutputStream(String resource, boolean append) throws IOException {
        StreamSource source = (StreamSource) resolveUrl(resource, true);
        return source.getOutputStream(append);
    }

    @Override
    public Stream<String> iterateFile(String file, int maxDepth, boolean followLinks, boolean showModificationDate) throws IOException {
        var source = resolveUrl(file);

        if (source.getSourceType() == FileSourceType.FILE) {
            if (source.isDirectory()) {
                var root = commonRoot != null ? Paths.get(commonRoot) : null;
                if (!DataUtil.isGord(source.getFullPath())) {
                    return getDirectoryStream(maxDepth, followLinks, showModificationDate,source.getPath(), root);
                } else if (Files.exists(source.getPath().resolve(source.getPath().getFileName()))) {
                    source = resolveUrl(source.getPath().resolve(source.getPath().getFileName()).toString());
                } else if (Files.exists(source.getPath().resolve(GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME))) {
                    source = resolveUrl(source.getPath().resolve(GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME).toString());
                } else {
                    return getDirectoryStream(maxDepth, followLinks, showModificationDate, source.getPath(), root);
                }
            }
        }
        return readAndClose(source);
    }

    @Override
    public RacFile openFile(String file) {
        DataSource source = resolveUrl(file);
        return new StreamSourceRacFile((StreamSource) source);
    }

    @Override
    public String getDictionarySignature(String dictionary, String[] tags) throws IOException {
        final DataSource source = resolveUrl(dictionary);
        String dictpath = PathUtils.fixFileSchema(source.getName());
        return new GorDictionaryTable.Builder<>(dictpath).fileReader(this).build()
                .getSignature(true, source.getSourceReference().commonRoot, tags);
    }

    @Override
    public String getFileSignature(String file) throws IOException {
        DataSource source = resolveUrl(file);
        return GorOptions.getFileSignature(source);
    }

    private BufferedReader readSource(DataSource source, String resolvedUrl) {
        try {
            return new SourceReader(source);
        } catch (IOException e) {
            var name = source.getName();
            throw ExceptionUtilities.mapGorResourceException(name, resolvedUrl, e);
        }
    }

    private static class SourceReader extends BufferedReader {
        private final DataSource source;

        SourceReader(DataSource source) throws IOException {
            super(DataUtil.isGZip(source.getName()) ?
                    new BufferedReader(new InputStreamReader(new GZIPInputStream(((StreamSource) source).open()))) :
                    new InputStreamReader(((StreamSource) source).open()));
            this.source = source;
        }

        @Override
        public void close() throws IOException {
            super.close();
            source.close();
        }
    }

    private Stream<String> readAndClose(DataSource source) throws IOException {
        String resolvedUrl = PathUtils.fixDbSchema(PathUtils.fixFileSchema(source.getName()));

        if (source instanceof RowIteratorSource rowSource) {
            var iterator = rowSource.open();

            var header = iterator.getHeader();
            var dataStream = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                    .map(Row::toString).onClose(iterator::close);

            if (header != null) {
                return Stream.concat(Stream.of(header), dataStream);
            } else {
                return dataStream;
            }
        }


        BufferedReader br = readSource(source, resolvedUrl);
        Stream<String> lineStream = br.lines();
        return lineStream.onClose(() -> {
            try {
                br.close();
                source.close();
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
                Path rel = root != null && !Strings.isNullOrEmpty(root.toString()) && x.isAbsolute() ? root.toAbsolutePath().relativize(x) : x;
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
    public Stream<SourceRef> prepareSources(Stream<SourceRef> sources) {
        if (sources == null) {
            return sources;
        }

        var gorDriverFactory = (PluggableGorDriver)GorDriverFactory.fromConfig();
        var sourceTypes =  gorDriverFactory.getSupportedSourceTypes();

        var resultStream = sources;

        for (var sourceType : sourceTypes) {
            if (!sourceType.supportsPreparation()) {
                continue;
            }

            var provider =gorDriverFactory.getSourceProvider(sourceType);
            resultStream = provider.prepareSources(resultStream);
        }

        return resultStream;
    }

    private Stream<DataSource[]> getDependentDestStream(String source, String dest, DataSource sourceSource) throws IOException {
        return getValidatedDependents(sourceSource).stream()
                .filter(dependent -> dependent.getName().startsWith(source)) // Only dependencies that are simply derived from the source.
                .map(dependent -> {
                    DataSource dependentDest = resolveUrl(dest + dependent.getName().substring(source.length()));
                    return new DataSource[]{dependent, dependentDest};
                });
    }

    private List<DataSource> getValidatedDependents(DataSource source) throws IOException {
        if (source.getDataType() != null && !source.getDataType().hasDependents) {
            return List.of();
        }

        List<DataSource> dependents = new ArrayList<>();
        DataSource metaSource = resolveUrl(source.getName() + DataType.META.suffix, true);
        if (metaSource != null && metaSource.exists()) {
            dependents.add(metaSource);
        }

        DataSource indexSource = findIndexSource(source);
        if (indexSource != null) {
            dependents.add(indexSource);
        }
        return dependents;
    }

    private static StreamSource findIndexSource(DataSource source) throws IOException {
        SourceProvider provider = PluggableGorDriver.instance().getSourceProvider(source.getSourceType());
        if (provider != null && provider instanceof StreamSourceProvider) {
            StreamSourceProvider streamProvider = (StreamSourceProvider) provider;
            StreamSourceFile sourceFile = streamProvider.getSourceFile(source);

            if (sourceFile != null && sourceFile.supportsIndex()) {
                return streamProvider.findIndexFileFromFileDriver(sourceFile, source.getSourceReference());
            }
        }
        return null;
    }
}
