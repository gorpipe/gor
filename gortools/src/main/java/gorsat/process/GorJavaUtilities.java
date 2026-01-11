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

package gorsat.process;

import org.apache.commons.io.FilenameUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.linkfile.LinkFileUtil;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.*;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.gor.table.TableFactory;
import org.gorpipe.gor.table.dictionary.DictionaryTableMeta;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryEntry;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTableMeta;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by sigmar on 21/12/15.
 */
public class GorJavaUtilities {
    // -ff option is handled in java class GorOptions from model which depends on DynamicGorIterator.
    // DynamicGorIterator needs PipeInstance from gortools. Suggestion: merge gortools and model modules.

    private static final Logger log = LoggerFactory.getLogger(GorJavaUtilities.class);

    public static final String GORZ_META = DataType.GORZ.suffix + DataType.META.suffix;
    public static final DecimalFormat fd3 = new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.ROOT));
    public static double[] pArray = IntStream.range(0, 128).mapToDouble(qual -> 1.0 - (qual - 33) / 93.0).toArray();
    public static String[] prArray = Arrays.stream(pArray).mapToObj(p -> fd3.format(p)).toArray(String[]::new);
    public static PRPRValue prprFunction = new PRPRValue();
    public static PRPRPRValue prprprFunction = new PRPRPRValue();

    public static class PRPRValue {
        String[][] prprArray;
        char sep;

        PRPRValue() {
            this(';');
        }

        PRPRValue(char sep) {
            this.sep = sep;
        }

        public String get(int c1, int c2) {
            if (prprArray == null)
                prprArray = IntStream.range(0, 128).mapToObj(i -> IntStream.range(0, 128).mapToObj(k -> prArray[i] + sep + prArray[k]).toArray(String[]::new)).toArray(String[][]::new);
            return prprArray[c1][c2];
        }
    }

    public static class PhenoInfo {
        public Phenotypes  phenotype;
        public String[] phenotypeNames;

        public PhenoInfo(Phenotypes phenotype, String[] phenotypeNames) {
            this.phenotype = phenotype;
            this.phenotypeNames = phenotypeNames;
        }
    }

    public enum Phenotypes {
        BINARY,
        QUANTITATIVE,
        MIXED
    }

    public static PhenoInfo getPhenotype(String phenoHeader, BufferedReader pheno) {
        Optional<String[]> ocommon = pheno.lines().skip(1).map(s -> s.split("\t")).map(s -> Arrays.copyOfRange(s, 1, s.length)).reduce((r1, r2) -> {
            for (int i = 0; i < r1.length; i++) {
                try {
                    if(!r1[i].equalsIgnoreCase("NA")) Integer.parseInt(r1[i]);
                    r1[i] = r2[i];
                } catch (NumberFormatException e) {
                    // Keep non integers for reduction
                }
            }
            return r1;
        });

        Phenotypes phenotypes = Phenotypes.BINARY;
        if(ocommon.isPresent()) {
            String[] common = ocommon.get();
            if (common.length>0) {
                Phenotypes pt = null;
                for (String s : common) {
                    try {
                        Integer.parseInt(s);
                        if (pt == null) pt = Phenotypes.BINARY;
                        else if (pt.equals(Phenotypes.QUANTITATIVE)) pt = Phenotypes.MIXED;
                    } catch (NumberFormatException e) {
                        if (pt == null) pt = Phenotypes.QUANTITATIVE;
                        else if (pt.equals(Phenotypes.BINARY)) pt = Phenotypes.MIXED;
                    }
                }
                phenotypes = pt;
            }
        }
        String[] phenoSplit = phenoHeader.split("\t");
        String[] phenoNames = Arrays.copyOfRange(phenoSplit, 1, phenoSplit.length);
        return new PhenoInfo(phenotypes, phenoNames);
    }

    public static class PRPRPRValue extends GorJavaUtilities.PRPRValue {
        PRPRPRValue() {
            super();
        }

        @Override
        public String get(int c1, int c2) {
            if (prprArray == null)
                prprArray = IntStream.range(0, 128).mapToObj(i -> IntStream.range(0, 128).mapToObj(k -> fd3.format(Math.abs(1.0 - pArray[i] - pArray[k])) + sep + prArray[i] + sep + prArray[k]).toArray(String[]::new)).toArray(String[][]::new);
            return prprArray[c1][c2];
        }
    }

    public static class VCFValue extends PRPRValue {
        double threshold;

        public VCFValue(double threshold) {
            super(',');
            this.threshold = threshold;
        }

        private String getGT(int c1, int c2) {
            double p1 = pArray[c1];
            double p2 = pArray[c2];
            double p0 = Math.abs(1.0 - p1 - p2);

            String gp = prprprFunction.get(c1, c2);
            if (p0 > threshold) return "\t0/0:" + gp;
            else if (p1 > threshold) return "\t0/1:" + gp;
            else if (p2 > threshold) return "\t1/1:" + gp;
            else return "\t./.:" + gp;
        }

        @Override
        public String get(int c1, int c2) {
            if (c1 == ' ') return "\t./.:0" + sep + "0" + sep + "0";
            else if (prprArray == null)
                prprArray = IntStream.range(0, 128).mapToObj(i -> IntStream.range(0, 128).mapToObj(k -> getGT(i, k)).toArray(String[]::new)).toArray(String[][]::new);
            return prprArray[c1][c2];
        }
    }

    public static String clearHints(String query) {
        int i = query.indexOf("/*+");
        if(i != -1) {
            return query.substring(0,i) + query.substring(query.indexOf("*/",i+3)+2);
        }
        return query;
    }

    public static GenomicIterator getDbIteratorSource(String sqlQuery, Map<String, Object> constants, final String source, boolean gortable, boolean scoping) {
        Supplier<Stream<String>> streamSupplier = () -> DbConnection.userConnections.getDBLinkStream( sqlQuery, constants, source);
        gorsat.Iterators.IteratorSource its;
        its = gortable ? new GorStreamIterator(streamSupplier, scoping) : new NorStreamIterator(streamSupplier);
        return new gorsat.Iterators.SingleIteratorSource(its, "dbit");
    }

    /**
     * Helper method for wrapping Object[] iterator to Stream.
     *
     * @param iterator
     * @return Stream where iterator elements are represented as string joined with tab delimiter
     */
    public static Stream<String> wrapObjectArrayIterator(Iterator<Object[]> iterator) {
        Iterator<String> theIterator = new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public String next() {
                Object[] data = iterator.next();
                return Arrays.stream(data).map(GorJavaUtilities::nullSafeToString).collect(Collectors.joining("\t"));
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(theIterator, Spliterator.IMMUTABLE), false);
    }

    private static String nullSafeToString(Object obj) {
        return (obj == null) ? "" : obj.toString();
    }


    public static class CmdParams {
        CmdParams(String[] cmdParams, String command) {
            this.cmdParams = cmdParams;
            this.command = command;
        }

        String[] cmdParams;
        String command;

        public String getAliasName() {
            return cmdParams[0];
        }

        public String getCmdPath() {
            return command.split("[ ]+")[0];
        }

        public String getCommand() {
            return command;
        }

        public Optional<String> getType() {
            return Arrays.stream(cmdParams).skip(1).filter(e -> e.startsWith("-s")).map(e -> e.substring(2)).findFirst();
        }

        public boolean isNor() {
            return Arrays.stream(cmdParams).skip(1).anyMatch(e -> e.equals("-n"));
        }

        public boolean isFormula() {
            return Arrays.stream(cmdParams).skip(1).anyMatch(e -> e.equals("-c"));
        }

        public boolean skipHeader() {
            return Arrays.stream(cmdParams).skip(1).anyMatch(e -> e.equals("-h"));
        }

        public boolean useProcessMethod() {
            return Arrays.stream(cmdParams).skip(1).anyMatch(e -> e.equals("-p"));
        }

        public boolean useHttpServer() {
            return Arrays.stream(cmdParams).skip(1).anyMatch(e -> e.equals("-u"));
        }

        public Optional<String> skipLines() {
            return Arrays.stream(cmdParams).skip(1).filter(p -> p.startsWith("-s")).findFirst().map(p -> p.substring(2));
        }

        public boolean allowError() {
            return Arrays.stream(cmdParams).skip(1).anyMatch(e -> e.equals("-e"));
        }
    }

    public static Map<String, CmdParams> readWhiteList(Path cmdConfigPath) {
        Map<String, CmdParams> allowedCmds = new HashMap<>();
        try {
            if (cmdConfigPath != null && Files.exists(cmdConfigPath)) {
                List<String> lines = Files.readAllLines(cmdConfigPath);
                for (String line : lines) {
                    String[] split = line.split("\t");
                    if (split.length == 1) {
                        int i = line.indexOf('[');
                        split = i > 0 ? new String[]{line.substring(0, i).trim(), line.substring(i)} : null;
                    }

                    if (split != null) {
                        String cmd = split[1].substring(1, split[1].length() - 1);
                        CmdParams cmdParams = new CmdParams(split[0].split("[ ]+"), cmd);
                        allowedCmds.put(cmdParams.getAliasName(), cmdParams);
                    }
                }
            }
        } catch(IOException ioe) {
            throw new GorSystemException("Failed to load white listed commands.", ioe);
        }
        return allowedCmds;
    }

    public static Map<String, CmdParams> readWhiteList(String cmdConfigFile) throws IOException {
        if (cmdConfigFile != null) {
            Path p = Paths.get(cmdConfigFile);
            return readWhiteList(p);
        }
        return new HashMap<>();
    }

    /**
     * Helper method to resolve whitelist file path (from config file) to full path.
     * <p>
     * If whitelist path is resolved as follows:
     * 1.  If input is absolute or empty then it is returned.
     * 2.  Search relative for the project directory.
     * 3.  If nothing is found we return the input.
     *
     * @param whiteListFile white list file path, can not be null or emtpy.
     * @param projectRoot   project root for the current project.
     * @return fully resolved path to the whitelist file.
     */
    public static Path resolveWhiteListFilePath(String whiteListFile, Path projectRoot) {
        if (whiteListFile == null || whiteListFile.isEmpty()) {
            throw new GorSystemException("Can not resolve empty white list file path", null);
        }

        Path whiteListPath = Paths.get(whiteListFile);

        if (whiteListPath.isAbsolute()) {
            return whiteListPath;
        }

        if (projectRoot != null) {
            return projectRoot.resolve(whiteListFile);
        }

        log.warn("Whitelist file {} is relative but no project root is defined!", whiteListFile);

        /*  Skipping searching the config path for now.  It we put this back in we should change the whitelist path to
        be relative to the "config" folder, i.e. for project folder case that would be <project root>/config instead
        of just <project root>.

        String configRootPath = ConfigManager.getConfigRootPath();
        if (configRootPath != null && !configRootPath.isEmpty()) {
            Path installWhiteListPath = Paths.get(ConfigManager.getConfigRootPath()).resolve(whiteListFile);
            if (Files.exists(installWhiteListPath)) {
                return installWhiteListPath;
            }
        }
        */

        return whiteListPath;
    }

    public static Optional<String> getIgnoreCase(Collection<String> c, String str) {
        return c.stream().filter(s -> s.equalsIgnoreCase(str)).findFirst();
    }

    public static String[] toUppercase(Collection<String> c) {
        return c.stream().map(String::toUpperCase).toArray(String[]::new);
    }

    public static String[] mergeArrays(String[] array1, String[] array2) {
        return Stream.concat(Arrays.stream(array1), Arrays.stream(array2)).toArray(String[]::new);
    }

    public static boolean isGorCmd(String cmd) {
        return cmd.toLowerCase().startsWith("gor ") || cmd.toLowerCase().startsWith("pgor ") || cmd.toLowerCase().startsWith("gorrow ") || cmd.toLowerCase().startsWith("gorrows ") ;
    }

    public static boolean isPGorCmd(String cmd) {
        return cmd.toLowerCase().startsWith("pgor ");
    }

    private static boolean isUUID(String filename) {
        return filename.indexOf('.')==36 && filename.charAt(8)=='-';
    }

    private static void md5Rename(String md5, Path p) throws IOException {
        Path dm = p.getParent().resolve(md5 + GORZ_META);
        if (!Files.exists(dm)) Files.move(p, dm);
        else if(Files.exists(p) && !Files.isSameFile(p,dm)) Files.delete(p);

        String fn = p.getFileName().toString();
        Path g = p.getParent().resolve(fn.substring(0, fn.length() - 5));
        Path d = p.getParent().resolve(DataUtil.toFile(md5, DataType.GORZ));
        if (!Files.exists(d)) Files.move(g, d);
        else if(Files.exists(g) && !Files.isSameFile(g,d)) Files.delete(g);
    }

    private static void writeDummyHeader(Writer dictionarypathwriter) throws IOException {
        var defheader = new String[] {"chrom","pos"};
        var tableheader = new DictionaryTableMeta();
        tableheader.setColumns(defheader);
        var header = tableheader.formatHeader();
        dictionarypathwriter.write(header);
    }

    private static void writeHeader(FileReader fileReader, Writer dictionarypath, String[] columns, boolean lineFilter) throws IOException {
        // TODO: handle nord
        var tableheader = new GorDictionaryTableMeta();

        if (columns != null) {
            tableheader.setColumns(columns);
        }

        if (!lineFilter) {
            tableheader.setProperty(DictionaryTableMeta.HEADER_LINE_FILTER_KEY, Boolean.toString(lineFilter));
        }
        dictionarypath.write(tableheader.formatHeader());
    }

    public static void createSymbolicLinkSafe(Path resultPath, Path cachePath) throws IOException {
        if (!Files.exists(resultPath, LinkOption.NOFOLLOW_LINKS)) {
            Files.createSymbolicLink(resultPath, cachePath.toAbsolutePath());
        } else if(Files.isSymbolicLink(resultPath)) {
            Files.delete(resultPath);
            Files.createSymbolicLink(resultPath, cachePath.toAbsolutePath());
        }
    }

    /*
     * Return the cachefile is valid, otherwise null.
     *
     * NOTE:  At first glance it would make sense to have this check in the FileCache, as it relates directly
     *        to the filecache integrity, but as the data pointed to might be on external source (s3, http)
     *        and we might not have access to data (to check timestamps) in the FileCache.
     */
    public static String verifyLinkFileLastModified2(ProjectContext projectContext, String cacheFile) {
        if (cacheFile != null && DataUtil.isLink(cacheFile)) {
            var invalidCacheFile = false;
            try {
                var ds = projectContext.getFileReader().resolveUrl(cacheFile);
                var linkLastModified = ds.getSourceMetadata().getLinkLastModified();
                var lastModified = ds.getSourceMetadata().getLastModified();
                if (linkLastModified != null && lastModified > linkLastModified) {
                    // Outdated link file.
                    invalidCacheFile = true;
                }
            } catch (Exception e) {
                // Can not resolve the file or other errors.
                invalidCacheFile = true;
            }

            if (invalidCacheFile) {
                log.debug("Link file {} is out of date and will be re-created.", cacheFile);
                try {
                    Files.delete(Paths.get(cacheFile));
                    cacheFile = null;
                } catch (IOException ioException) {
                    // Ignore
                }
            }
        }
        return cacheFile;
    }

    public static void writeDictionaryFromMeta(String commandToExecute, FileReader fileReader, String outfolderpath, String dictionarypath) throws IOException {
        FileReader localFileReader = fileReader;

        fileReader.updateFileSystemMetaData(outfolderpath);

        try (Stream<String> metapathstream = localFileReader.list(outfolderpath);
             Writer dictionarypathwriter = new OutputStreamWriter(localFileReader.getOutputStream(dictionarypath))) {
            var metaList = metapathstream.parallel().filter(p -> DataUtil.isMeta(p)).map(p -> GorMeta.createAndLoad(localFileReader, p)).collect(Collectors.toList());
            if (metaList.size() > 0) {
                writeHeader(localFileReader, dictionarypathwriter, metaList.get(0).getColumns(), false);
            }
            var ai = new AtomicInteger();
            var entries = metaList.parallelStream()
                .filter(meta -> !meta.containsProperty(GorMeta.HEADER_LINE_COUNT_KEY) || meta.getLineCount() > 0L)
                .map(meta -> {
                    var p = meta.getMetaPath();
                    // Assume we have all data in the root folder (note relatives does not work here for S3Shared).
                    var outfilename = PathUtils.getFileName(p);
                    var outfile = FilenameUtils.removeExtension(outfilename);
                    var builder = new GorDictionaryEntry.Builder(outfile, outfolderpath);
                    var i = ai.incrementAndGet();
                    builder.alias(Integer.toString(i));
                    builder.range(meta.getRange());
                    var tags = meta.getProperty(GorMeta.HEADER_TAGS_KEY, "");
                    if (!Strings.isNullOrEmpty(tags)) {
                        builder.tags(tags.split(","));
                    } else if (meta.containsProperty(GorMeta.HEADER_CARDCOL_KEY)) {
                        builder.tags(meta.getCordColTags());
                    }
                    return builder.build().formatEntry();
                }).collect(Collectors.toList());

            if (entries.size() > 0) {
                for (var entry : entries) {
                    dictionarypathwriter.write(entry);
                }
            } else writeDummyHeader(dictionarypathwriter);
        }

        localFileReader.writeLinkIfNeeded(dictionarypath);

        var linkOptions = LinkFileUtil.extractLinkOptionData(commandToExecute);
        if (!Strings.isNullOrEmpty(linkOptions)) {
            var linkMetaOption = LinkFileUtil.extractLinkMetaOptionData(commandToExecute);
            var linkData = LinkFileUtil.extractLink(fileReader, outfolderpath, linkOptions, linkMetaOption, null);
            LinkFileUtil.writeLinkFile(fileReader, linkData);
        }
    }

    public static Optional<String[]> parseDictionaryColumn(String[] dictList, FileReader fileReader) {
        return Arrays.stream(dictList).mapMulti((BiConsumer<String, Consumer<String[]>>) (df, consumer) -> {
            if (DataUtil.isDictionary(df.toLowerCase())) {
                var dt = TableFactory.getTable(df, fileReader);
                var cols = dt.getColumns();
                if (cols!=null) consumer.accept(cols);
            } else {
                try {
                    consumer.accept(fileReader.readHeaderLine(df).split("\t"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).findFirst();
    }
}
