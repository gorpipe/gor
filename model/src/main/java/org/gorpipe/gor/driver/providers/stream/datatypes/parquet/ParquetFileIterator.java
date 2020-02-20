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

package org.gorpipe.gor.driver.providers.stream.datatypes.parquet;

import org.gorpipe.gor.GorSession;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.Line;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryWrapper;
import org.gorpipe.util.gorutil.standalone.GorStandalone;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ParquetFileIterator extends GenomicIterator {
    private PriorityQueue<ParquetRowReader> mergeParquet = new PriorityQueue<>();
    private List<Path> parquetPaths = new ArrayList<>();
    private GenomicIterator.ChromoLookup lookup;
    private boolean nor = false;
    private String[] header;
    private java.nio.file.Path resultPath;
    private int[] sortCols;
    private Configuration configuration = new Configuration(true);
    private GroupReadSupport readSupport = new GroupReadSupport();

    public ParquetFileIterator(StreamSourceFile parquetFile) {
        this.lookup = parquetFile.getFileSource().getSourceReference().getLookup();
        this.resultPath = resolvePath(parquetFile);
    }

    private java.nio.file.Path resolvePath(StreamSourceFile parquetFile) {
        FileSource fileSource = resolveFileSource(parquetFile);
        SourceReference sourceReference = fileSource.getSourceReference();
        java.nio.file.Path path = Paths.get(sourceReference.url);
        // TODO there shouldn't be a reference to GorStandalone here.
        if (!path.isAbsolute() && GorStandalone.isStandalone()) {
            java.nio.file.Path root = Paths.get(sourceReference.commonRoot);
            path = root.resolve(path);
        }
        return path;
    }

    private FileSource resolveFileSource(StreamSourceFile parquetFile) {
        StreamSource ss = parquetFile.getFileSource();
        FileSource fs;
        if (ss instanceof RetryWrapper) {
            RetryWrapper rw = (RetryWrapper) ss;
            fs = (FileSource) rw.getWrapped();
        } else {
            fs = (FileSource) parquetFile.getFileSource();
        }
        return fs;
    }

    @Override
    public void init(GorSession gorSession) {
        nor = gorSession.getNorContext();
        gorSession.getGorContext().getSortCols().ifPresent(c -> this.sortCols = Arrays.stream(c.split(",")).mapToInt(Integer::parseInt).toArray());
        init();
    }

    private void init() {
        if (resultPath != null) {
            try {
                parquetPaths = init(resultPath);
            } catch (IOException e) {
                throw new GorSystemException("Init parquet", e);
            } finally {
                resultPath = null;
            }
        }
    }

    private List<Path> init(java.nio.file.Path parquetPath) throws IOException {
        Collection<java.nio.file.Path> pathCollection;
        if (Files.isDirectory(parquetPath)) {
            try(Stream<java.nio.file.Path> walk = Files.walk(parquetPath)) {
                pathCollection = walk
                        .filter(ParquetFileIterator::isParquetDataFile)
                        .collect(Collectors.toList());
            }
        } else {
            pathCollection = Collections.singleton(parquetPath);
        }
        headerInit(pathCollection.stream().findFirst().get().toString());
        return pathCollection.stream().map(p -> new Path(p.toString())).collect(Collectors.toList());
    }

    private static boolean isParquetDataFile(java.nio.file.Path path) {
        return path.getFileName().toString().endsWith(".parquet")
                && !Files.isDirectory(path);
    }

    private void initParquetReader(Path parquetFilePath) throws IOException {
        ParquetReader<Group> reader = ParquetReader.builder(readSupport, parquetFilePath)
                .withConf(configuration)
                .build();
        ParquetRowReader parquetRowReader = nor ? new NorParquetRowReader(reader, sortCols) : new ParquetRowReader(reader, lookup);
        if (parquetRowReader.row != null) mergeParquet.add(parquetRowReader);
    }

    private void subInit() throws IOException {
        if( nor && (sortCols == null || sortCols.length == 1) ) {
            if( parquetPaths.size() > 0 ) {
                initParquetReader(parquetPaths.remove(0));
            }
        } else {
            for (Path parquetFilePath : parquetPaths) {
                initParquetReader(parquetFilePath);
            }
            parquetPaths.clear();
        }
    }

    private void headerInit(String filePath) throws IOException {
        Path parquetFilePath = new Path(filePath);
        InputFile inputFile = HadoopInputFile.fromPath(parquetFilePath, configuration);
        try(ParquetFileReader pfr = ParquetFileReader.open(inputFile)) {
            ParquetMetadata readFooter = pfr.getFooter();
            MessageType schema = readFooter.getFileMetaData().getSchema();
            String[] parquetHeader = schema.getFields().stream().map(this::getTypeName).toArray(String[]::new);
            header = parquetHeader;
            readSupport.init(configuration, null, schema);
        }
    }

    private String getTypeName(Type type) {
        return type.getName();
    }

    @Override
    public void close() {
        // Iterator doesn't close resources
    }

    @Override
    public String[] getHeader() {
        init();
        return header;
    }

    @Override
    public boolean seek(String chr, int pos) {
        return false;
    }

    @Override
    public boolean next(Line line) {
        return false;
    }

    private Row row;

    @Override
    public boolean hasNext() {
        ParquetRowReader parquetRowReader = mergeParquet.poll();
        if (parquetRowReader != null) {
            row = parquetRowReader.next();
            if (parquetRowReader.hasNext()) {
                mergeParquet.add(parquetRowReader);
            }
            return true;
        } else if( parquetPaths.size() > 0 ) {
            try {
                subInit();
            } catch (IOException e) {
                throw new GorSystemException("Error while reading parquet file", e);
            }
            return hasNext();
        }
        return false;
    }

    @Override
    public Row next() {
        if(row==null) throw new NoSuchElementException();
        return row;
    }
}
