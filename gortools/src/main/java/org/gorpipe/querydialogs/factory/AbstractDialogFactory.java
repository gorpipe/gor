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
package org.gorpipe.querydialogs.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import freemarker.template.TemplateException;
import gorsat.Utilities.Utilities;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.QueryEvaluator;
import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.ArgumentType;
import org.gorpipe.querydialogs.Dialog;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A factory responsible for building {@link Dialog}s from a supplied YAML file.
 * <p>
 * Sample usage:
 * </p>
 * <pre>
 * AbstractDialogFactory&lt;Dialog&gt; f = new SomeDialogFactoryImplementation();
 * f.registerArgumentBuilder(ArgumentType.STRING, new StringArgumentBuilder());
 * List&lt;Dialog&gt; dialogs = f.buildDialogs(new File(&quot;/path/to/yaml/file&quot;));
 * </pre>
 *
 * @param <T> a Dialog type
 * @author arnie
 * @version $Id$
 */
public abstract class AbstractDialogFactory<T extends Dialog> {
    /**
     * Budget for the definition cache, in characters of yml source (the parsed map roughly doubles that in memory).
     */
    private static final long MAX_DEFINITION_CHARS = Long.getLong("gor.dialog.definition.cache.maxchars", 32L * 1024 * 1024);
    /**
     * Parsed dialog definitions, keyed on (resource, cacheDir). An entry is only used when the resource's current content
     * is identical to the content it was parsed from, so an edited file is always re-parsed.
     */
    private static final Cache<DefinitionKey, ParsedDefinition> DEFINITIONS = Caffeine.newBuilder()
            .maximumWeight(MAX_DEFINITION_CHARS)
            .weigher((DefinitionKey k, ParsedDefinition v) -> Math.max(1, v.content().length()))
            .build();
    private static final AtomicLong DEFINITION_PARSES = new AtomicLong();

    private record DefinitionKey(String resource, String cacheDir) {
    }

    /**
     * @param content      the yml content the definition was parsed from
     * @param dialogMap    the parsed yml, after embedded values have been replaced with file paths. Never handed out,
     *                     callers get a deep copy.
     * @param embeddedFiles map from embedded file path to its content
     */
    private record ParsedDefinition(String content, Map<String, Map<String, Object>> dialogMap, Map<String, String> embeddedFiles) {
    }

    private final Map<ArgumentType, ArgumentBuilder> argumentBuilders;
    protected String inputFileFirstReport;
    FileReader fileResolver;
    QueryEvaluator queryEval;
    boolean ignoreAllowedMismatch;
    boolean deferUpdates;

    /**
     * Constructs a factory with no registered argument builders
     */
    public AbstractDialogFactory(FileReader fr, QueryEvaluator queryEval, boolean ignoreAllowedMismatch) {
        this.fileResolver = fr;
        this.queryEval = queryEval;
        argumentBuilders = new HashMap<>();
        this.ignoreAllowedMismatch = ignoreAllowedMismatch;
    }

    /**
     * Registers an {@link ArgumentBuilder} to use for the given {@link ArgumentType}.
     *
     * @param type    - the type to register builder for
     * @param builder - the builder to use for given type
     * @return the previously registered builder for that type, or null if there was none
     */
    public ArgumentBuilder registerArgumentBuilder(ArgumentType type, ArgumentBuilder builder) {
        builder.setIgnoreAllowedMismatch(ignoreAllowedMismatch);
        return argumentBuilders.put(type, builder);
    }

    /**
     * If set, the dialogs built by this factory are created with deferred updates (see {@link Dialog#setDeferUpdates}):
     * the query is not rendered when the dialog is created, nor when its arguments are set, but only when it is
     * requested.
     *
     * @param deferUpdates whether dialogs should be created with deferred updates
     */
    public void setDeferUpdates(boolean deferUpdates) {
        this.deferUpdates = deferUpdates;
    }

    /**
     * Get the FileResolver for this factory
     *
     * @return the file resolver for this factory
     */
    public FileReader getFileReader() {
        return this.fileResolver;
    }

    public QueryEvaluator getQueryEval() {
        return this.queryEval;
    }

    /**
     * @param resource - the Yaml to read
     * @return a {@link List} of {@link Dialog}s
     */
    public List<T> buildDialogs(String resource, String cacheDir) throws IOException, TemplateException {
        if (cacheDir == null) {
            try (Reader br = fileResolver.getReader(resource)) {
                return buildDialogs(br, cacheDir);
            }
        }

        final String content;
        try (Reader br = fileResolver.getReader(resource)) {
            StringWriter writer = new StringWriter();
            br.transferTo(writer);
            content = writer.toString();
        }

        final DefinitionKey key = new DefinitionKey(resource, cacheDir);
        ParsedDefinition definition = DEFINITIONS.getIfPresent(key);
        if (definition == null || !definition.content().equals(content)) {
            definition = parseDefinition(content, cacheDir);
            DEFINITIONS.put(key, definition);
        } else {
            restoreEmbeddedFiles(definition, cacheDir);
        }

        if (definition.dialogMap() == null) return new ArrayList<>();
        return buildDialogs(deepCopy(definition.dialogMap()));
    }

    /**
     * @return number of times a yml dialog definition has been parsed, for tests and diagnostics
     */
    public static long definitionParseCount() {
        return DEFINITION_PARSES.get();
    }

    private ParsedDefinition parseDefinition(String content, String cacheDir) {
        Map<String, Map<String, Object>> dialogMap = parseDialogMap(new StringReader(content));
        Map<String, String> embeddedFiles = new HashMap<>();
        if (dialogMap != null) {
            replaceEmbeddedValuesWithFiles(dialogMap, cacheDir, embeddedFiles);
        }
        return new ParsedDefinition(content, dialogMap, embeddedFiles);
    }

    /**
     * The embedded files are written to the cache directory when the definition is parsed. Re-create any that have
     * been removed or changed since, as the dialog queries refer to them by path.
     */
    private static void restoreEmbeddedFiles(ParsedDefinition definition, String cacheDir) {
        for (Map.Entry<String, String> embedded : definition.embeddedFiles().entrySet()) {
            if (!hasContent(Paths.get(embedded.getKey()), embedded.getValue())) {
                Utilities.makeTempFile(embedded.getValue(), cacheDir);
            }
        }
    }

    private static boolean hasContent(Path file, String content) {
        try {
            // Same encoding as Utilities.makeTempFile writes with.
            return Arrays.equals(Files.readAllBytes(file), content.getBytes());
        } catch (IOException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static <V> V deepCopy(V value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(k, deepCopy(v)));
            return (V) copy;
        } else if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(v -> copy.add(deepCopy(v)));
            return (V) copy;
        } else if (value instanceof Set<?> set) {
            Set<Object> copy = new LinkedHashSet<>();
            set.forEach(v -> copy.add(deepCopy(v)));
            return (V) copy;
        } else if (value instanceof Date date) {
            return (V) date.clone();
        } else if (value instanceof byte[] bytes) {
            return (V) bytes.clone();
        }
        // Strings, numbers, booleans and null are immutable.
        return value;
    }

    /**
     * @param resource - the Yaml to read
     * @return a {@link List} of {@link Dialog}s
     */
    public List<T> buildDialogs(String resource) throws IOException, TemplateException {
        try (Reader br = fileResolver.getReader(resource)) {
            return buildDialogs(br, null);
        }
    }

    /**
     * @param resource - the Yaml to read
     * @return a {@link List} of {@link Dialog}s
     */
    public List<T> buildDialogs(Path resource) throws IOException, TemplateException {
        try (Reader br = fileResolver.getReader(resource)) {
            return buildDialogs(br, null);
        }
    }

    /**
     * Attempts to read and parse the given file as a YAML file and convert the resulting data structure to a list of {@link Dialog}s
     *
     * @param reader - reader of the yaml content
     * @return a {@link List} of {@link Dialog}s
     **/
    @SuppressWarnings("unchecked")
    public List<T> buildDialogs(Reader reader, String cacheDir) throws TemplateException, IOException {
        Map<String, Map<String, Object>> dialogMap = parseDialogMap(reader);
        if (dialogMap == null) return new ArrayList<T>();

        if (cacheDir == null) {
            cacheDir = Files.createTempDirectory("dialogs").toAbsolutePath().toString();
        }

        replaceEmbeddedValuesWithFiles(dialogMap, cacheDir, null);
        return buildDialogs(dialogMap);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> parseDialogMap(Reader reader) {
        DEFINITION_PARSES.incrementAndGet();
        // Only standard yml types; never construct arbitrary java classes from tags.
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object o = yaml.load(reader);
        if (o == null) return null;
        if (!(o instanceof Map)) throw new RuntimeException("Invalid dialog configuration file");
        return (Map<String, Map<String, Object>>) o;
    }

    /**
     * Replaces ${key} references to other values of the same dialog with the path of a file holding that value.
     *
     * @param embeddedFiles if not null, receives the written files (path to content)
     */
    private static void replaceEmbeddedValuesWithFiles(Map<String, Map<String, Object>> dialogMap, String cacheDir, Map<String, String> embeddedFiles) {
        for (String key : dialogMap.keySet()) {
            Map<String, Object> val = dialogMap.get(key);
            for (String skey : val.keySet()) {
                Object oval = val.get(skey);
                if (oval instanceof String) {
                    String sval = oval.toString();
                    int i = sval.indexOf("${");
                    boolean found = i != -1;
                    while (i != -1) {
                        int u = sval.indexOf("}", i + 1);
                        String entry = sval.substring(i + 2, u);
                        if (val.containsKey(entry)) {
                            String value = val.get(entry).toString();
                            String file = Utilities.makeTempFile(value, cacheDir);
                            if (embeddedFiles != null) embeddedFiles.put(file, value);
                            sval = sval.substring(0, i) + file + sval.substring(u + 1);
                        }
                        i = sval.indexOf("${", i + 1);
                    }
                    if (found) val.put(skey, sval);
                }
            }
        }
    }

    private List<T> buildDialogs(Map<String, Map<String, Object>> dialogMap) throws TemplateException {
        inputFileFirstReport = dialogMap.keySet().iterator().next();
        List<T> dialogs = new ArrayList<T>();
        TreeSet<String> sortedKeys = new TreeSet<>(dialogMap.keySet());
        for (String key : sortedKeys) {
            dialogs.add(buildDialog(key, dialogMap.get(key)));
        }
        return dialogs;
    }

    /**
     * Get the name of the first report in the input file.
     *
     * @return the name of the first report in the input file
     */
    public String getInputFileFirstReport() {
        return inputFileFirstReport;
    }

    protected abstract T buildDialog(String name, Map<String, ? extends Object> attributes) throws TemplateException;

    protected Argument buildArgument(String name, Map<String, ? extends Object> attributes) {
        ArgumentType argType = attributes.containsKey("type") ? ArgumentType.valueOf(attributes.get("type").toString().trim().toUpperCase()) : ArgumentType.STRING;
        return argumentBuilders.get(argType).build(name, attributes);
    }
}
