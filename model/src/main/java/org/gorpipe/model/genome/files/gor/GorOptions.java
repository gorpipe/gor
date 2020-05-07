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

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.GorContext;
import org.gorpipe.gor.GorSession;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.table.Dictionary;
import org.gorpipe.model.gor.iterators.RowSource;
import org.gorpipe.model.util.Util;
import org.gorpipe.util.string.StringUtil;
import gorsat.Commands.CommandArguments;
import gorsat.Commands.CommandParseUtilities;
import gorsat.Commands.GenomicRange;
import gorsat.DynIterator;
import gorsat.gorsatGorIterator.MapAndListUtilities;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gorsat.Commands.CommandParseUtilities.stringValueOfOption;
import static gorsat.Commands.CommandParseUtilities.stringValueOfOptionWithDefault;

/**
 * Various options that can be entered with Gor
 *
 * @version $Id$
 */
public class GorOptions {

    private static final String ERROR_INITIALIZING_QUERY = "Error Initializing Query: ";
    private static final String IS_NOT_FOUND = " is not found!";

    private static final Logger log = LoggerFactory.getLogger(GorOptions.class);

    public GorSession getSession() {
        return session;
    }
    /**
     * True if we cache all the parsed lines of a dictionary once we have read it, and make a hash map from pn's to lines.
     */
    private boolean useDictionaryCache = Boolean.valueOf(System.getProperty("gor.dictionary.cache.active", "true"));
    /**
     * The gorPipeSession
     */
    private GorSession session;
    private GorContext context;
    /**
     * List of files to process
     */
    public final ArrayList<SourceRef> files = new ArrayList<>();
    /**
     * Chromsome number to start with
     */
    public final int chromo;
    /**
     * basepair position to start (inclusive) with, requires chromosome
     */
    public final int begin;
    /**
     * basepair position to end with (exclusive), requires chromosome,
     */
    public final int end;
    /**
     * Chromosome name to start with
     */
    public final String chrname;
    /**
     * True to insert source name as column
     */
    public final boolean insertSource;
    /**
     * Index file path
     */
    private final String idxFile;
    /**
     * Reference file path
     */
    public String refFile;
    /**
     * The name of the output column with the source field
     */
    public final String sourceColName;
    /**
     * The common root that was prepended to each source name
     */
    public final String commonRoot;
    /**
     * The number of blocks to run in parallel
     */
    public final int parallelBlocks;
    /**
     * Map of column nr to a list of tags to filter the column on
     */
    public Set<String> columnTags;
    /**
     * Silent tag filtering.  If silent we get don't error even if not all tags in filter are seen in the data.
     */
    public final boolean isSilentTagFilter;
    /**
     * Don't use tags for line filtering, only for dictionary filtering.
     */
    public final boolean isNoLineFilter;
    /**
     * The resource monitor to report resources usage to, or null if none is attached
     */
    public final ResourceMonitor monitor;
    private boolean enforceResourceRelativeToRoot = false; // Flag that all local resources should be validated as being relative to root, i.e. must be at or below root in the hierarchy and no abolute refs.

    public boolean hasLocalDictonaryFile = false; // Flag that a local dictonary file has been found
    private boolean isDictionaryWithBuckets = false; // source col from dictionary files can be hiden if no buckets and no -f filters

    private String sourceName;

    /**
     * Chromosome Name to Id Cache
     */
    public final ChromoCache chrcache = new ChromoCache();

    /**
     * Chromsome name and ordering scheme to be used for output data
     */
    public final ChrDataScheme dataOutputScheme;
    
    public static class ProjectContext {
        public final String securityKey;
        public final String commonRoot;

        ProjectContext(String securityKey, String commonRoot) {
            this.securityKey = securityKey;
            this.commonRoot = commonRoot;
        }
    }

    /**
     * @param files
     * @param resmon
     */
    public GorOptions(List<SourceRef> files, ResourceMonitor resmon) {
        this(-1, 0, Integer.MAX_VALUE, false, 0, null, false, false, null, files,
                null, resmon, ChrDataScheme.ChrLexico);
    }

    /**
     * @param files
     */
    public GorOptions(List<SourceRef> files) {
        this(-1, 0, Integer.MAX_VALUE, false, 0, null, false, false, null,
                 files, null,  null, ChrDataScheme.ChrLexico);
    }

    /**
     * Construct GorOptions
     *
     * @param chromoOpt
     * @param beginOpt
     * @param endOpt
     * @param insertSourceOpt
     * @param columnTags
     * @param silentTagFilter
     * @param srcColName
     * @param files
     * @param commonRootOpt
     * @param monitor
     * @param outputScheme
     */
    public GorOptions(int chromoOpt, int beginOpt, int endOpt, boolean insertSourceOpt, int parallelBlocks,
                      final Set<String> columnTags, boolean silentTagFilter, boolean noLineFilter, String srcColName,
                      List<SourceRef> files, String commonRootOpt,
                      ResourceMonitor monitor, ChrDataScheme outputScheme) {
        this.chromo = chromoOpt;
        this.chrname = 0 <= chromoOpt && chromoOpt < outputScheme.id2chr.length ? outputScheme.id2chr[chromoOpt] : null;
        this.begin = beginOpt;
        this.end = endOpt;
        this.insertSource = insertSourceOpt;
        this.idxFile = null;
        this.refFile = null;
        this.parallelBlocks = parallelBlocks;
        this.columnTags = columnTags;
        this.isSilentTagFilter = silentTagFilter;
        this.isNoLineFilter = noLineFilter;
        this.sourceColName = srcColName;
        if (files != null) {
            this.files.addAll(files);
        }
        this.commonRoot = commonRootOpt;
        this.monitor = monitor;
        this.dataOutputScheme = outputScheme;
    }

    static void checkFileNameIsRelativeToRoot(final String fileName) {
        if (fileName.length() > 2 && (fileName.charAt(0) == '/' || fileName.charAt(0) == '\\' || fileName.charAt(1) == ':')) {
            log.warn("Absolute path: {}", fileName);
            throw new GorResourceException("Absolute paths for files are not allowed!", fileName);
        }
        if (!isPathConstraintWithInRoot(fileName)) {
            throw new GorResourceException("Filepaths are not allowed to reference parent folders!", fileName);
        }
    }

    static boolean isPathConstraintWithInRoot(String ipath) {
        final String path = ipath.replace('\\', '/');
        final ArrayList<String> parts = StringUtil.split(path, '/');
        final String[] outParts = new String[parts.size()];
        int last = 0;
        for (String part : parts) {
            if (part.equals(".")) {
                /* do nothing */
            } else if (part.equals("..")) {
                last -= 1;
                if (last < 0) { // dot dots have traversed above the root, not allowed
                    log.info("Path not constraint within root: {}", ipath);
                    return false;
                }
            } else {
                outParts[last] = part;
                last += 1;
            }
        }

        return true; // Path is constrained within the root
    }

    private static String[] extractQuotedFiles(String[] files) {
        List<String> processedFiles = new ArrayList<>();
        for (String file : files){
            if (file != null && file.length() > 0) {
                if (file.charAt(0) == '\"' || file.charAt(0) == '\'') {
                    final char lastChr = file.charAt(file.length() - 1);
                    final String name = file.substring(1, lastChr == '\"' || lastChr == '\'' ? file.length() - 1 : file.length());
                    processedFiles.add(name);
                } else {
                    processedFiles.add(file);
                }
            } else {
                throw new GorResourceException("Empty filename found when processing gor input", file);
            }
        }

        return processedFiles.stream().toArray(String[]::new);
    }

    public static GorOptions createGorOptions(String... args) {
        return createGorOptions(String.join(" ", args));
    }

    public static GorOptions createGorOptions(String query) {
        String[] arguments = CommandParseUtilities.quoteSafeSplit(query, ' ');
        return createGorOptions(null, arguments);
    }

    public static GorOptions createGorOptions(GorContext context, String[] arguments) {
        Tuple2<String[], String[]> result =  CommandParseUtilities.validateCommandArguments(arguments, new CommandArguments("-fs -nf -Y -P -stdin -n",
                "-f -ff -m -M -p -r -sc -ref -idx -Z -H -z -X -s -b -dict -parts -seek", -1, -1, false));

        String[] inputArguments = result._1;
        String[] illegalArguments = result._2;

        if (illegalArguments.length > 0) {
            // We have invalid arguments and need to throw exception
            throw new GorParsingException(String.format("Invalid arguments in gor query. Argument(s) %1$s not a part of this command", String.join(",", illegalArguments)));
        }

        return new GorOptions(context, extractQuotedFiles(inputArguments), arguments);
    }

    /**
     * Parse GorOptions from the option map and files list
     *
     * @param context             The associated context object, or null if not used
     * @param iargs             Input arguments
     * @param options
     */
    public GorOptions(GorContext context, String[] iargs, String[] options) {
        this.context = context;
        this.session = context != null ? context.getSession() : null;
        boolean insertSourceOpt = false;
        String srcColName = null;
        // Non null iff list of input files was read from a file
        String tagfile = null;
        ChrDataScheme outputScheme = ChrDataScheme.ChrLexico;


        boolean silentTagFilter = CommandParseUtilities.hasOption(options, "-fs");
        boolean noLineFilter = CommandParseUtilities.hasOption(options, "-nf");
        boolean allowBucketAccess = !CommandParseUtilities.hasOption(options, "-Y");
        enforceResourceRelativeToRoot = CommandParseUtilities.hasOption (options, "-P");
        String tmpIdxFile = CommandParseUtilities.stringValueOfOptionWithDefault(options, "-idx", null);
        String tmpRefFile = CommandParseUtilities.stringValueOfOptionWithDefault(options, "-ref", null);
        String monId = CommandParseUtilities.stringValueOfOptionWithDefault(options, "-H", null);
        String commonRootOpt = CommandParseUtilities.stringValueOfOptionWithDefault(options, "-r", null);
        String securityKey = CommandParseUtilities.replaceSingleQuotes(CommandParseUtilities.stringValueOfOptionWithDefault(options, "-Z", null));
        int tmpParallelBlocks = CommandParseUtilities.intValueOfOptionWithDefault(options, "-z", 0);

        if (CommandParseUtilities.hasOption(options, "-ff")) {
            tagfile = CommandParseUtilities.stringValueOfOption(options, "-ff");
            insertSourceOpt = true; // Always assume -s when using -f
        }

        if (CommandParseUtilities.hasOption(options, "-f")) {
            String taglist =  CommandParseUtilities.stringValueOfOption(options, "-f").replace("'", ""); // Incase ' is used to qoute strings, just remove it
            taglist = taglist.replace("\"", ""); // Incase " is used to escape list, just remove it
            final ArrayList<String> tags = StringUtil.split(taglist, 0, ',');
            this.columnTags = new HashSet<>();
            this.columnTags.addAll(tags);
            insertSourceOpt = true; // Always assume -s when using -f
        }

        if (CommandParseUtilities.hasOption(options, "-s")) {
            insertSourceOpt = true;
            try {
                srcColName = stringValueOfOptionWithDefault(options, "-s", null);
            } catch (GorParsingException gpe) {
                srcColName = null;
            }
        }

        if (CommandParseUtilities.hasOption(options, "-X")) {
            String scheme = stringValueOfOption(options, "-X");
            switch (scheme) {
                case "LEX":
                    outputScheme = ChrDataScheme.ChrLexico;
                    break;
                case "HG":
                    outputScheme = ChrDataScheme.HG;
                    break;
                default:
                    throw new GorParsingException(ERROR_INITIALIZING_QUERY + "Unknown output data scheme " + scheme, "-X", scheme);
            }
        }

        if (securityKey == null) {
            securityKey = System.getProperty("gor.security.context");
        }
        this.idxFile = tmpIdxFile;
        this.refFile = tmpRefFile;

        this.parallelBlocks = tmpParallelBlocks;
        this.monitor = monId != null ? ResourceMonitor.find(monId) : null;

        if (commonRootOpt == null) { // If not specified on command line, try the vm default
            commonRootOpt = System.getProperty("gor.common.root");
        }
        if (commonRootOpt != null) {
            if (commonRootOpt.trim().length() == 0) {
                commonRootOpt = null;
            } else if (commonRootOpt.length() > 2 && commonRootOpt.charAt(1) == ':' && !commonRootOpt.endsWith("\\")) { // windows path hack
                commonRootOpt = commonRootOpt + '\\';
            } else if (!commonRootOpt.endsWith("/")) {
                commonRootOpt = commonRootOpt + '/';
            }
        }

        commonRoot = commonRootOpt;

        loadTagFiles(context, iargs, tagfile);

        this.sourceColName = srcColName;

        if (CommandParseUtilities.hasOption(options, "-p")) {
            // Need to get the last range if multiple ranges are given
            String[] allRanges = CommandParseUtilities.stringArrayOfOption(options, "-p");
            String rangeSource = extractNonQuotedText(allRanges[allRanges.length - 1], 0);
            GenomicRange.Range range = CommandParseUtilities.parseRange(rangeSource);
            if(StringUtil.isAllDigit(range.chromosome())) {
                this.chromo = Integer.parseInt(range.chromosome());
            } else {
                this.chromo = chrcache.prefixToChromosomeIdOrUnknown(range.chromosome() + "\t", true);
            }
            this.begin = range.start();
            this.end = range.stop();
            this.chrname = range.chromosome();
        } else {
            this.chromo = -1;
            this.begin = 0;
            this.end = Integer.MAX_VALUE;
            this.chrname = null;
        }

        this.isSilentTagFilter = silentTagFilter;
        this.isNoLineFilter = noLineFilter;

        // Resolve sources, must be done after all fields are populated
        ProjectContext projectContext = new ProjectContext(securityKey, commonRoot);

        // Ensure input schemas are compatible if no output scheme is specified
        this.dataOutputScheme = outputScheme;

        final HashSet<String> alltags = new HashSet<>(); // Collect all tags in parsed dictionary files into one set
        resolveSources(insertSourceOpt, iargs, projectContext, allowBucketAccess, alltags);

        // Moved this assignment to last, so that dictionary discovery will correctly trigger insert source
        this.insertSource = hasLocalDictonaryFile && (isDictionaryWithBuckets || insertSourceOpt) || insertSourceOpt;
    }

    public GenomicIterator getIterator() {
        return getIterator(null);
    }

    public GenomicIterator getIterator(GorMonitor gm) {
        List<GenomicIterator> genomicIterators = getIterators();

        GenomicIterator theIterator;
        if(genomicIterators.size() > 1 || insertSource) {
            theIterator = new MergeIterator(genomicIterators, this, gm);
        } else {
            theIterator = genomicIterators.get(0);
        }

        if (sourceName != null) {
            theIterator.setSourceName(sourceName);
        }

        if (chrname != null && !chrname.equals("")) {
            theIterator = new BoundedIterator(theIterator, chrname, begin, end);
        }

        theIterator.setContext(context);
        if (context != null) {
            context.iteratorCreated(toString());
        }

        return theIterator;
    }

    List<GenomicIterator> getIterators() {
        Stream<SourceRef> inRange = files.stream().filter(ref -> chrname == null || ref.isInRange(chrname, begin, end));
        Stream<SourceRef> withTag = inRange.filter( ref -> columnTags == null || ref.analyzeQueryTags(columnTags, insertSource) != SourceRef.NO_TAG);
        Stream<GenomicIterator> iteratorStream = withTag.parallel().map(this::createGenomicIteratorFromRef);

        List<GenomicIterator> genomicIterators = iteratorStream.collect(Collectors.toList());

        if (genomicIterators.isEmpty()) {
            // No iterator in range, add dummy one (that will not return any rows) as we must return at least one.
            SourceRef ref = files.get(0);
            genomicIterators.add(new BoundedIterator(createGenomicIteratorFromRef(ref), "", 0, "", 0));
        }
        return genomicIterators;
    }

    private GenomicIterator createGenomicIteratorFromRef(SourceRef ref) {
        GenomicIterator i;
        try {
            i = ref.iterate(new DefaultChromoLookup(), chrname, null);
        } catch (IOException e) {
            throw new GorResourceException("Couldn't open file", ref.getName(), e);
        }
        i.init(getSession());
        i.setSourceName(ref.getName());
        i.setTagStatus(columnTags == null ? SourceRef.NO_TAG : ref.analyzeQueryTags(columnTags, insertSource));
        if (!isNoLineFilter && i.getTagStatus() == SourceRef.POSSIBLE_TAG) {
            i.setTagFilter(new TagFilter(columnTags, ref.deletedTags, i.getHeader().split("\t").length - 1));
        }
        i.setSourceAlreadyInserted(ref.sourceAlreadyInserted);
        i.setColnum(i.getHeader().split("\t").length - 2);
        if (chrname != null && !chrname.equals("")) {
            i = new BoundedIterator(i, chrname, begin, end);
        }

        return i;
    }

    private void loadTagFiles(GorContext context, String[] iargs, String tagfile) {
        if (tagfile != null) { // Parse the specified tag file for tag values to filter on
            try {
                String key = String.join("", iargs);
                Set<String> tags;

                if (CommandParseUtilities.isNestedCommand(tagfile)) {
                    String iteratorCommand = CommandParseUtilities.parseNestedCommand(tagfile);
                    key += iteratorCommand.hashCode();
                    Option<Set<String>> opt = MapAndListUtilities.syncGetSet(key, session);
                    if (opt.isDefined()) {
                        tags = opt.get();
                    } else {
                        tags = new LinkedHashSet<>();
                        loadTagsFromIterator(context, tags, iteratorCommand);
                        MapAndListUtilities.syncAddSet(key, tags, session);
                    }
                } else {
                    key += tagfile;
                    Option<Set<String>> opt = session != null ? MapAndListUtilities.syncGetSet(key, session) : Option.empty();
                    if (opt.isDefined()) {
                        tags = opt.get();
                    } else {
                        tags = readTags(commonRoot != null ? concatFolderFile(commonRoot, tagfile, tagfile, enforceResourceRelativeToRoot) : tagfile);
                        if (session != null) MapAndListUtilities.syncAddSet(key, tags, session);
                    }
                }
                this.columnTags = new HashSet<>();
                this.columnTags.addAll(tags);
            } catch (FileNotFoundException e) {
                throw new GorResourceException(ERROR_INITIALIZING_QUERY + "Tag file " + tagfile + IS_NOT_FOUND, tagfile);
            } catch (IOException e) {
                throw new GorSystemException(ERROR_INITIALIZING_QUERY + "Tag file " + tagfile + " can not be read!", e);
            }
        }
    }

    private String getDefaultReferencePath() {
        GorSession gps = this.getSession();

        if (gps == null) return null;

        String path = gps.getProjectContext().getReferenceBuild().getBuildPath();

        if (StringUtils.isEmpty(path)) {
            try {
                String baseFile = "config/gor_config.txt";
                String configPath = concatFolderFile(commonRoot, baseFile, baseFile, enforceResourceRelativeToRoot);
                path = MapAndListUtilities.getSingleHashMap(configPath, false, false, gps).getOrDefault("buildPath", null);
            } catch (Exception ex) { /* The reference can be emtpy, ok to return null from this method. */ }
        }

        return path;
    }

    private void loadTagsFromIterator(GorContext context, Set<String> tags, String iteratorCommand) {
        try (RowSource dSource = new DynIterator.DynamicRowSource(iteratorCommand, context, true)) {
            while (dSource.hasNext()) {
                final String line = dSource.next().toString();
                final int i = line.indexOf('\t', line.indexOf('\t') + 1);
                final int l = line.indexOf('\t', i + 1);
                String tag = line.substring(i + 1, l == -1 ? line.length() : l);
                tags.add(tag);
            }
        }
    }

    private String extractNonQuotedText(String option, int start) {
        if (option.charAt(start) == '\"') {
            return option.substring(start + 1, option.charAt(option.length() - 1) == '\"' ? option.length() - 1 : option.length());
        }
        return option.substring(start, option.charAt(option.length() - 1) == '\"' ? option.length() - 1 : option.length());
    }

    /**
     * Note must be called at the end of constructor when all fields have been set, or optionally from createFilter
     * as the second last thing in the constructor, and in that case the filter expression will be wrong
     *
     */
    private void resolveSources(boolean isInsertSource, String[] fileList, ProjectContext projectContext, boolean allowBucketAccess, Set<String> alltags) {
        sourceName = String.join(":", fileList);

        // Start by resolve simple filelists
        for (final String file : fileList) {
            createStandardSources(projectContext, allowBucketAccess, alltags, file);
        }

        // dictionary files will always insert sources
        if (isInsertSource || hasLocalDictonaryFile) {
            // Extract an unique file name part for the file. Used in source ref to get name for the file (used for example in getting sensible column names).
            // This is only used if alias is not defined.
            insertDictionarySource();
        }
    }

    private void createStandardSources(ProjectContext projectContext, boolean allowBucketAccess, Set<String> alltags, String file) {
        final ArrayList<String> parts = StringUtil.split(file);
        final int length = parts.size();
        if (length == 1) { // Simple file name
            if (file.equals("-")) {
                files.add(SourceRef.STANDARD_IN);
            } else {
                addSourceRef(file, projectContext, null, allowBucketAccess, alltags);
            }
        } else {
            hasLocalDictonaryFile = true;
            Dictionary.DictionaryLine sf = Dictionary.parseDictionaryLine(file, null);
            addSourceRef(sf.fileRef.physical, sf.fileRef.logical, sf.fileRef.isAcceptedAbsoluteRef, projectContext,
                    sf.alias, sf.startChr, sf.startPos, sf.stopChr, sf.stopPos, sf.tags, allowBucketAccess, alltags);
        }
    }

    private void insertDictionarySource() {
        // Keep and index array with the last checked position for /, from the right
        final int[] indices = new int[files.size()];
        for (int i = 0; i < files.size(); i++) {
            indices[i] = files.get(i).file.length() - 1; // Initialize at the end of the stream
        }
        final HashMap<String, SourceRef> map = new HashMap<>();
        boolean isUnique = false;
        while ( !isUnique ) {
            // Haven't found a unique part, advance index left to the next slash
            map.clear();
            boolean foundDuplicate = false;
            for (int i = 0; i < files.size(); i++) {
                final SourceRef source = files.get(i);
                if (source.alias == null) {
                    if (indices[i] != -1) {
                        int index = source.file.lastIndexOf('/', indices[i] - 1);
                        indices[i] = index != indices[i] ? index : -1;
                    }
                    String key = source.file.substring(indices[i] + 1);
                    String originalKey = key;
                    for (int k = 2; indices[i] == -1 && map.containsKey(key); k++) {
                        key = originalKey + "_" + k; // Filepath is not unique, lets suffix it with a unique number, making it unique
                    }
                    if (map.containsKey(key)) {
                        foundDuplicate = true;
                    }
                    map.put(key, source);
                }
            }
            isUnique = !foundDuplicate;
        }

        // The map contains the unique names to use for each SourceRef
        for (Map.Entry<String, SourceRef> entry : map.entrySet()) {
            entry.getValue().setUniqueFileNamePart(entry.getKey());
        }
    }


    public static Set<String> readTags(String filename) throws IOException {
        final boolean isNorFile = filename.toLowerCase().endsWith("nor");
        try (final BufferedReader br = new BufferedReader(new FileReader(filename))) {
            final Set<String> set = new LinkedHashSet<>();
            br.lines().filter(line -> !line.startsWith("#")).map(line -> {
                final int beginIdx = isNorFile ? line.indexOf('\t', line.indexOf('\t') + 1) : 0;
                final int endIdx = line.indexOf('\t', beginIdx);
                return endIdx == -1 ? line.substring(beginIdx) : line.substring(beginIdx, endIdx);
            }).map(String::trim).forEach(set::add);
            return Collections.unmodifiableSet(set);
        }
    }

    @Override
    public String toString() {
        // Convert the options into a command line string
        final StringBuilder text = new StringBuilder();

        // Specify source column tags, if any
        if (columnTags != null && !columnTags.isEmpty()) {
            appendWithSep(text, " ", "-f ");
            int cnt = 0;
            for (String tag : columnTags) {
                if (cnt++ != 0) {
                    text.append(",");
                }
                text.append(tag);
            }
        }

        // Extra filter flags
        if (isSilentTagFilter) {
            appendWithSep(text, " ", "-fs");
        }
        if (isNoLineFilter) {
            appendWithSep(text, " ", "-nf");
        }

        if (insertSource) {
            appendWithSep(text, " ", "-s ");
            if (sourceColName != null) {
                text.append(sourceColName);
            }
        }

        if (chromo != -1) {
            appendWithSep(text, " ", "-p " + chromo + ":" + begin + "-" + end);
        }

        if (commonRoot != null) {
            appendWithSep(text, " ", "-r " + commonRoot);
        }

        // Add files, including alias and tags
        for (SourceRef source : files) {
            if (text.length() > 0) {
                text.append(" ");
            }
            String file = source.file;
            if (commonRoot != null && file.startsWith(commonRoot)) {
                file = file.substring(commonRoot.length());
            }
            text.append(file);
        }
        return text.toString();
    }

    private static StringBuilder appendWithSep(StringBuilder sb, String sep, String text) {
        if (sb.length() > 0) {
            sb.append(sep);
        }
        sb.append(text);
        return sb;
    }

    private void addSourceRef(String file, ProjectContext projectContext, String alias, boolean allowBucketAccess, Set<String> alltags) {
        // Add alias as tag on the file
        final Set<String> tags = new HashSet<>();
        if (alias != null) tags.add(alias);

        // A call to this can only take place at the end of a constructor, when all parameters have been set
        addSourceRef(file, file, false, projectContext, alias, null, -1, null, -1, tags, allowBucketAccess, alltags);
    }

    private static String toNominalForm(String file) {
        file = file.replace('\\', '/');
        if (file.length() > 1 && file.charAt(0) == '/' && file.charAt(1) != '/') {
            return '/' + file;
        }
        return file;
    }

    private void addSourceRef(String physicalFile, String logicalFile, boolean isAcceptedAbsoluteRef, ProjectContext projectContext, String alias,
                              String startChr, int startPos, String stopChr, int stopPos, Set<String> tags, boolean allowBucketAccess, Set<String> alltags) {
        final String lowerfile = physicalFile.toLowerCase();
        final boolean isDictionary = lowerfile.endsWith(".gord") && !lowerfile.startsWith("mem:");
        final boolean isBGenMultiFile = lowerfile.matches(".*#\\{.*\\}.*\\.bgen");

        // Windows full path hack
        if (physicalFile.length() > 2 && physicalFile.charAt(1) == ':' && Util.isWindowsOS() ) {
            physicalFile = "file://" + physicalFile;
        }

        final String fileName = getFullPath(physicalFile, logicalFile, isAcceptedAbsoluteRef);
        final String indexFileName = getFullPath(idxFile, idxFile, isAcceptedAbsoluteRef);
        final String referenceFileName = getFullPath(refFile, refFile, isAcceptedAbsoluteRef);

        if (isDictionary) {
            // Must decide how to treat range and tag information for dictionary files
            try {
                processDictionary(fileName, allowBucketAccess, projectContext, isSilentTagFilter, alltags);
            } catch (IOException e) {
                throw new GorSystemException(e);
            }
        } else if (isBGenMultiFile) {
            processBGenMultiFile(fileName, indexFileName, referenceFileName, alias, tags, projectContext);
        } else {
            files.add(new SourceRef(fileName, indexFileName, referenceFileName, alias, startChr, startPos, stopChr, stopPos, tags, false, projectContext.securityKey, projectContext.commonRoot));
        }
    }

    private String getFullPath(String physicalFile, String logicalFile, boolean isAcceptedAbsoluteRef) {

        // Test if input is empty
        if (StringUtil.isEmpty(physicalFile)) return physicalFile;

        final String lowerfile = physicalFile.toLowerCase();
        final boolean isFilePrefix = lowerfile.startsWith("file://");

        if (isFilePrefix) {
            physicalFile = physicalFile.substring(7);
        }
        physicalFile = toNominalForm(physicalFile);

        return !isAcceptedAbsoluteRef && commonRoot != null && (enforceResourceRelativeToRoot || (!isFilePrefix && !physicalFile.startsWith("//"))) ?
                concatFolderFile(commonRoot, physicalFile, logicalFile, enforceResourceRelativeToRoot) : physicalFile;
    }

    private void processBGenMultiFile(String fileName, String indexFileName, String referenceFileName, String alias, Set<String> tags, ProjectContext projectContext) {
        final String[] chromos = {"1", "10", "11", "12",
                "13", "14", "15", "16", "17", "18", "19", "2", "20", "21", "22", "3", "4", "5", "6", "7", "8", "9", "M", "X", "Y"};
        final Matcher matcher = Pattern.compile("#\\{.*\\}").matcher(fileName);
        matcher.find();
        final int internalBegin = matcher.start();
        final int internalEnd = matcher.end();
        assert !matcher.find();

        final String prefix = fileName.substring(0, internalBegin);
        final String suffix = fileName.substring(internalEnd);
        for (String chr : chromos) {
            final String path = prefix + chr + suffix;
            if (Files.isRegularFile(Paths.get(path))) {
                this.files.add(new SourceRef(path, indexFileName, referenceFileName, alias, "chr" + chr, 0, "chr" + chr, 1000000000, tags, false, projectContext.securityKey, projectContext.commonRoot));
            }
        }
    }

    private void processDictionary(String fileName, boolean allowBucketAccess, ProjectContext projectContext, boolean isSilentTagFilter, Set<String> alltags) throws IOException {
        final boolean hasTags = !(this.columnTags == null || this.columnTags.isEmpty());
        String fileSignature = useDictionaryCache ? getFileSignature(fileName, null) : "";
        fileSignature += "_" +  allowBucketAccess;
        Dictionary gord = new Dictionary(fileName, allowBucketAccess, this.columnTags, commonRoot, fileSignature, hasTags, isSilentTagFilter, useDictionaryCache);
        this.hasLocalDictonaryFile = true;
        this.isDictionaryWithBuckets = gord.isDictionaryWithBuckets();
        final Dictionary.DictionaryLine[] fileList = gord.getFiles().length == 0 && hasTags ? gord.getFallbackLinesForHeader() : gord.getFiles();
        if (!hasTags && gord.getAnyBucketHasDeletedFile()) {
            if (this.columnTags == null) this.columnTags = new HashSet<>(gord.getValidTags());
            else this.columnTags.addAll(gord.getValidTags());
        }
        for (Dictionary.DictionaryLine line : fileList) {
            subProcessOfProcessDictionary(line, allowBucketAccess, projectContext, alltags,
                    gord.getBucketDeletedFiles(Paths.get(line.fileRef.logical).getFileName().toString()));
        }
    }

    private void subProcessOfProcessDictionary(Dictionary.DictionaryLine dictionaryLine, boolean allowBucketAccess, ProjectContext projectContext, Set<String> alltags, Collection<String> deletedTags) {
        if (dictionaryLine.sourceInserted) {
            files.add(new SourceRef(dictionaryLine.fileRef.logical, dictionaryLine.alias, null, null,
                    dictionaryLine.startChr, dictionaryLine.startPos, dictionaryLine.stopChr, dictionaryLine.stopPos,
                    dictionaryLine.tags, deletedTags, dictionaryLine.sourceInserted,
                    projectContext.securityKey, projectContext.commonRoot));

        } else {
            addSourceRef(dictionaryLine.fileRef.physical, dictionaryLine.fileRef.logical, dictionaryLine.fileRef.isAcceptedAbsoluteRef, projectContext,
                    dictionaryLine.alias, dictionaryLine.startChr, dictionaryLine.startPos, dictionaryLine.stopChr, dictionaryLine.stopPos, dictionaryLine.tags, allowBucketAccess, alltags);
        }
    }

    public static String getFileSignature(String fileName, String securityContext) throws IOException {
        //TODO: This method should really take in SourceReference or better yet be removed and replaced with calls to datasource.getUniqueId
        DataSource ds = GorDriverFactory.fromConfig().getDataSource(new SourceReferenceBuilder(fileName).securityContext(securityContext).build());
        return getFileSignature(ds);
    }

    public static String getFileSignature(DataSource source) throws IOException {
        if (source != null) {
            return source.getSourceMetadata().getUniqueId();
        } else {
            return Util.md5(Long.toString(System.currentTimeMillis()));
        }
    }


    private static String concatFolderFile(String folder, String physical, String logical, boolean checkRelativeToRoot) {
        if (physical.contains("://")) { // The file is not a filesystem reference, so do not apply common file system root
            return physical;
        }
        if (checkRelativeToRoot) { // Need to ensure that the file doesn't refer above the root in the file hierarchy
            checkFileNameIsRelativeToRoot(logical);
        }
        return folder.endsWith("/") || folder.endsWith("\\") ? folder + physical : folder + '/' + physical;
    }
}
