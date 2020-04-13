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

import org.apache.parquet.filter2.compat.FilterCompat;
import org.apache.parquet.filter2.predicate.FilterApi;
import org.apache.parquet.filter2.predicate.FilterPredicate;
import org.apache.parquet.filter2.predicate.Operators;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.PrimitiveType;
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
import org.gorpipe.model.gor.RowObj;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ParquetFileIterator extends GenomicIterator {
    private PriorityQueue<ParquetRowReader> mergeParquet = new PriorityQueue<>();
    private List<Path> parquetPaths = new ArrayList<>();
    private List<Path> parquetPathsForSeek = new ArrayList<>();
    private GenomicIterator.ChromoLookup lookup;
    private boolean nor = false;
    private java.nio.file.Path resultPath;
    private int[] sortCols;
    private Configuration configuration = new Configuration(true);
    private GroupReadSupport readSupport = new GroupReadSupport();
    private MessageType schema;
    private FilterPredicate filterPredicate;
    private FilterPredicate seekfilterPredicate;
    private FilterCompat.Filter filter;
    private String partitioningCol;
    private boolean partColPresent;

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
                parquetPathsForSeek = new ArrayList<>(parquetPaths);
            } catch (IOException e) {
                throw new GorSystemException("Init parquet", e);
            } finally {
                resultPath = null;
            }
        }
    }

    private String[] extractPartCol(String parquetPath) {
        java.nio.file.Path parentParquetPath = Paths.get(parquetPath).getParent();
        String parentFolder = parentParquetPath.getFileName().toString();
        String[] parentSplit = parentFolder.split("=");
        if (parentSplit.length == 2) return parentSplit;
        return null;
    }

    private List<Path> init(java.nio.file.Path parquetPath) throws IOException {
        Collection<java.nio.file.Path> pathCollection;
        if (Files.isDirectory(parquetPath)) {
            try(Stream<java.nio.file.Path> walk = Files.walk(parquetPath)) {
                pathCollection = walk
                        .filter(ParquetFileIterator::isParquetDataFile)
                        .collect(Collectors.toList());
            }

            Iterator<java.nio.file.Path> pathit = pathCollection.iterator();
            if (pathit.hasNext()) {
                java.nio.file.Path firstParquetPath = pathit.next();
                String[] partitioningColumn = extractPartCol(firstParquetPath.toString());
                if(partitioningColumn!=null) {
                    partitioningCol = partitioningColumn[0];
                    if (partitioningCol.toLowerCase().startsWith("chrom")) {
                        nor = true;
                        pathCollection = pathCollection.stream().sorted(Comparator.comparing(java.nio.file.Path::getParent)).collect(Collectors.toList());
                    }
                }
            }
        } else {
            pathCollection = Collections.singleton(parquetPath);
        }
        headerInit(pathCollection.stream().findFirst().get().toString(),partitioningCol);
        return pathCollection.stream().map(p -> new Path(p.toString())).collect(Collectors.toList());
    }

    private static boolean isParquetDataFile(java.nio.file.Path path) {
        return path.getFileName().toString().endsWith(".parquet")
                && !Files.isDirectory(path);
    }

    private void updateFilter() {
        if(filterPredicate!=null && seekfilterPredicate!=null) filter = FilterCompat.get(FilterApi.and(filterPredicate,seekfilterPredicate));
        else if(filterPredicate!=null) filter = FilterCompat.get(filterPredicate);
        else if(seekfilterPredicate!=null) filter = FilterCompat.get(seekfilterPredicate);
        else filter = null;
    }

    private ParquetRowReader initParquetReader(Path parquetFilePath) throws IOException {
        ParquetReader.Builder<Group> parquetBuilder = ParquetReader.builder(readSupport, parquetFilePath).withConf(configuration);
        if(filter!=null) parquetBuilder.withFilter(filter);
        ParquetReader<Group> reader = parquetBuilder.build();

        String[] partCol = extractPartCol(parquetFilePath.toString());
        String part = partCol != null ? partCol[1] : null;

        return nor ? new NorParquetRowReader(reader, sortCols, part) : new ParquetRowReader(reader, lookup, part);
    }

    class Path2ParquetReader implements Function<Path, ParquetRowReader> {
        IOException ioe;

        @Override
        public ParquetRowReader apply(Path path) {
            try {
                return initParquetReader(path);
            } catch (IOException e) {
                ioe = e;
            }
            return null;
        }

        public Optional<IOException> getError() {
            return Optional.ofNullable(ioe);
        }
    }

    private void subInit() throws IOException {
        if( nor && (sortCols == null || sortCols.length == 1) ) {
            if( parquetPaths.size() > 0 ) {
                ParquetRowReader parquetRowReader = initParquetReader(parquetPaths.remove(0));
                if (parquetRowReader.row != null) mergeParquet.add(parquetRowReader);
            }
        } else {
            Path2ParquetReader path2ParquetReader = new Path2ParquetReader();
            // Use takeWhile(Objects::nonNull) before collect when compiling with jdk9+
            List<ParquetRowReader> parquetRowReaders = parquetPaths.parallelStream().map(path2ParquetReader).collect(Collectors.toList());
            if(path2ParquetReader.getError().isPresent()) throw path2ParquetReader.getError().get();
            parquetRowReaders.stream().filter(p -> p.row != null).forEach(parquetRowReader -> mergeParquet.add(parquetRowReader));
            parquetPaths.clear();
        }
    }

    private void headerInit(String filePath, String partCol) throws IOException {
        Path parquetFilePath = new Path(filePath);
        InputFile inputFile = HadoopInputFile.fromPath(parquetFilePath, configuration);
        try(ParquetFileReader pfr = ParquetFileReader.open(inputFile)) {
            ParquetMetadata readFooter = pfr.getFooter();
            schema = readFooter.getFileMetaData().getSchema();
            String parquetHeader = schema.getFields().stream().map(this::getTypeName).collect(Collectors.joining("\t"));
            setHeader(parquetHeader);
            readSupport.init(configuration, null, schema);
        }

        if(partCol!=null) {
            String partColLower = partCol.toLowerCase();
            String[] cols = super.getHeader().toLowerCase().split("\t");
            partColPresent = Arrays.asList(cols).contains(partColLower);
            if(!partColPresent) {
                String newHeader = partColLower.equals("chrom") ? partCol+"\t"+super.getHeader() : super.getHeader()+"\t"+partCol;
                setHeader(newHeader);
            }
        }
    }

    private String getTypeName(Type type) {
        return type.getName();
    }

    @Override
    public void close() {
        mergeParquet.forEach(ParquetRowReader::close);
    }

    @Override
    public String getHeader() {
        init();
        return super.getHeader();
    }

    @Override
    public boolean seek(String chr, int pos) {
        Binary bin = Binary.fromString(chr);
        String[] header = getHeader().split("\t");
        if(pos<=1) {
            seekfilterPredicate = FilterApi.eq(FilterApi.binaryColumn(header[0]), bin);
        } else {
            seekfilterPredicate = FilterApi.and(FilterApi.eq(FilterApi.binaryColumn(header[0]), bin),FilterApi.gtEq(FilterApi.intColumn(header[1]), pos));
        }
        updateFilter();
        mergeParquet.forEach(ParquetRowReader::close);
        mergeParquet.clear();
        parquetPaths = new ArrayList<>(parquetPathsForSeek);
        return true;
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
            if(partitioningCol!=null&&!partColPresent) {
                if(partitioningCol.toLowerCase().equals("chrom")) {
                    row = RowObj.apply(parquetRowReader.getPart()+"\t"+row.toString());
                } else {
                    row = RowObj.apply(row.toString()+"\t"+parquetRowReader.getPart());
                }
            }
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

    private FilterPredicate intFilter(String valStr,String colName,char comp,boolean eq) {
        int ival = Integer.parseInt(valStr);
        Operators.IntColumn intColumn = FilterApi.intColumn(colName);
        if(comp=='=') return FilterApi.eq(intColumn, ival);
        else if(comp=='<') return eq?FilterApi.ltEq(intColumn, ival):FilterApi.lt(intColumn, ival);
        else if(eq) return FilterApi.gtEq(intColumn, ival);
        else return FilterApi.gt(intColumn, ival);
    }

    private FilterPredicate longFilter(String valStr,String colName,char comp,boolean eq) {
        long lval = Long.parseLong(valStr);
        Operators.LongColumn longColumn = FilterApi.longColumn(colName);
        if (comp == '=') return FilterApi.eq(longColumn, lval);
        else if (comp == '<') return eq ? FilterApi.ltEq(longColumn, lval) : FilterApi.lt(longColumn, lval);
        else if (eq) return FilterApi.gtEq(longColumn, lval);
        else return FilterApi.gt(longColumn, lval);
    }

    private FilterPredicate doubleFilter(String valStr,String colName,char comp,boolean eq) {
        double dval = Double.parseDouble(valStr);
        Operators.DoubleColumn doubleColumn = FilterApi.doubleColumn(colName);
        if(comp=='=') return FilterApi.eq(doubleColumn, dval);
        else if(comp=='<') return eq?FilterApi.ltEq(doubleColumn, dval):FilterApi.lt(doubleColumn, dval);
        else if(eq) return FilterApi.gtEq(doubleColumn, dval);
        else return FilterApi.gt(doubleColumn, dval);
    }

    private FilterPredicate stringFilter(String valStr,String colName,char comp,boolean eq) {
        Binary bin = Binary.fromString(valStr);
        Operators.BinaryColumn binaryColumn = FilterApi.binaryColumn(colName);
        if(comp=='=') return FilterApi.eq(binaryColumn, bin);
        else if(comp=='<') return eq?FilterApi.ltEq(binaryColumn, bin):FilterApi.lt(binaryColumn, bin);
        else if(eq) return FilterApi.gtEq(binaryColumn, bin);
        else return FilterApi.gt(binaryColumn, bin);
    }

    private FilterPredicate inFilter(String all,String colName) {
        String[] split = all.split(",");
        Operators.BinaryColumn binaryColumn = FilterApi.binaryColumn(colName);
        FilterPredicate newFilterPredicate = null;
        for(String val : split) {
            String valStr = val.trim().replace("'","");
            Binary bin = Binary.fromString(valStr);
            FilterPredicate tmpFilterPredicate = FilterApi.eq(binaryColumn, bin);
            if(newFilterPredicate==null) {
                newFilterPredicate = tmpFilterPredicate;
            } else {
                newFilterPredicate = FilterApi.or(newFilterPredicate, tmpFilterPredicate);
            }
        }
        return newFilterPredicate;
    }

    private FilterPredicate getFilterPredicate(PrimitiveType.PrimitiveTypeName primName, String filterStr, String valStr, String colName, char comp, char comp2, boolean eq) {
        if (primName.equals(PrimitiveType.PrimitiveTypeName.INT32)) {
            return intFilter(valStr,colName,comp,eq);
        } else if (primName.equals(PrimitiveType.PrimitiveTypeName.INT64)) {
            return longFilter(valStr,colName,comp,eq);
        } else if (primName.equals(PrimitiveType.PrimitiveTypeName.DOUBLE)) {
            return doubleFilter(valStr,colName,comp,eq);
        } else if(filterStr.endsWith("'")) {
            valStr = filterStr.substring(colName.length()+(eq?3:2),filterStr.length()-1);
            return stringFilter(valStr,colName,comp,eq);
        } else if(filterStr.endsWith(")")) {
            if(comp=='i' && comp2=='n' && filterStr.charAt(colName.length()+2)=='(') {
                String all = filterStr.substring(colName.length()+3,filterStr.length()-1);
                return inFilter(all,colName);
            }
        }
        return null;
    }

    private void mergeWithPreviousFilter(FilterPredicate newFilterPredicate) {
        if (filterPredicate == null) {
            filterPredicate = newFilterPredicate;
        } else {
            filterPredicate = FilterApi.and(filterPredicate, newFilterPredicate);
        }
        updateFilter();
    }

    @Override
    public boolean pushdownFilter(String origFilterStr) {
        String filterStr = origFilterStr.replace(" ","");
        String filterUpper = filterStr.toUpperCase();
        String[] header = getHeader().split("\t");
        if(filterUpper.startsWith("IN(")) {
            String all = filterStr.substring(3,filterStr.length()-1);
            FilterPredicate newFilterPredicate = inFilter(all,header[header.length-1]);
            mergeWithPreviousFilter(newFilterPredicate);
            return true;
        } else {
            int i = 0;
            for (; i < header.length; i++) {
                String col = header[i].toUpperCase();
                if (filterUpper.startsWith(col)) break;
            }
            if (i < header.length) {
                String col = header[i];
                char comp = filterStr.charAt(col.length());
                char comp2 = filterStr.charAt(col.length() + 1);
                if (comp == '<' || comp == '>' || comp == '=' || comp == 'i') {
                    boolean eq = (comp == '<' || comp == '>') && comp2 == '=';
                    String valStr = filterStr.substring(col.length() + (eq ? 2 : 1), filterStr.length() - 1);

                    PrimitiveType.PrimitiveTypeName primName = schema.getColumns().get(i).getPrimitiveType().getPrimitiveTypeName();
                    FilterPredicate newFilterPredicate = getFilterPredicate(primName, filterStr, valStr, col, comp, comp2, eq);

                    if (newFilterPredicate != null) {
                        mergeWithPreviousFilter(newFilterPredicate);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
