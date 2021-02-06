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

import freemarker.template.TemplateException;
import gorsat.Utilities.Utilities;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.QueryEvaluator;
import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.ArgumentType;
import org.gorpipe.querydialogs.Dialog;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
    private final Map<ArgumentType, ArgumentBuilder> argumentBuilders;
    protected String inputFileFirstReport;
    FileReader fileResolver;
    QueryEvaluator queryEval;

    /**
     * Constructs a factory with no registered argument builders
     */
    public AbstractDialogFactory(FileReader fr, QueryEvaluator queryEval) {
        this.fileResolver = fr;
        this.queryEval = queryEval;
        argumentBuilders = new HashMap<>();
    }

    /**
     * Registers an {@link ArgumentBuilder} to use for the given {@link ArgumentType}.
     *
     * @param type    - the type to register builder for
     * @param builder - the builder to use for given type
     * @return the previously registered builder for that type, or null if there was none
     */
    public ArgumentBuilder registerArgumentBuilder(ArgumentType type, ArgumentBuilder builder) {
        return argumentBuilders.put(type, builder);
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
        try (Reader br = fileResolver.getReader(resource)) {
            return buildDialogs(br, cacheDir);
        }
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
        Yaml yaml = new Yaml();
        Object o = yaml.load(reader);
        if (o == null) return new ArrayList<T>();
        if (!(o instanceof Map)) throw new RuntimeException("Invalid dialog configuration file");
        Map<String, Map<String, Object>> dialogMap = (Map<String, Map<String, Object>>) o;
        inputFileFirstReport = dialogMap.keySet().iterator().next();
        List<T> dialogs = new ArrayList<T>();

        if (cacheDir == null) {
            cacheDir = Files.createTempDirectory("dialogs").toAbsolutePath().toString();
        }

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
                            sval = sval.substring(0, i) + Utilities.makeTempFile(value, cacheDir) + sval.substring(u + 1);
                        }
                        i = sval.indexOf("${", i + 1);
                    }
                    if (found) val.put(skey, sval);
                }
            }
        }

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
