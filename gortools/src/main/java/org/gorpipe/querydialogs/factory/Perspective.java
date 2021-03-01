/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.templating.DialogArgumentWrapper;
import org.gorpipe.querydialogs.templating.NetworkTemplateLoader;
import org.gorpipe.querydialogs.templating.SkipFirstMethodModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a particular perspective onto data returned from a dialog query.
 *
 * @author arnie
 * @version $Id$
 */
public class Perspective {
    /**
     * The group name assigned to perspectives that don't define their own group names.
     */
    public static final String GLOBAL_GROUP = "[global]";
    private static final StringTemplateLoader TEMPLATE_LOADER;
    private static final Configuration TEMPLATE_CONFIG;
    private static final Logger logger = LoggerFactory.getLogger(Perspective.class);

    static {
        try {
            // Turn off freemarker library logging
            freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
        } catch (ClassNotFoundException e) {/* ignore */}

        TEMPLATE_CONFIG = new Configuration();
        TEMPLATE_LOADER = new StringTemplateLoader();
        TEMPLATE_CONFIG.setTemplateLoader(TEMPLATE_LOADER);

        TEMPLATE_CONFIG.setObjectWrapper(new DialogArgumentWrapper());
        TEMPLATE_CONFIG.setSharedVariable("skip", new SkipFirstMethodModel());
        TEMPLATE_CONFIG.setLocale(Locale.ENGLISH);
    }

    public static void initializeTempleConfig(FileReader fileResolver) {
        String macroPath = System.getProperty("dialog.macrodir", null);
        if (macroPath != null) {
            NetworkTemplateLoader netLoader = new NetworkTemplateLoader(macroPath, fileResolver);
            TEMPLATE_CONFIG.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{TEMPLATE_LOADER, netLoader}));
        }
    }

    private final String namePrefix;
    private final String name;
    private final String groupName;
    private final boolean isDefault;
    private final String filterTemplate;
    private final String viewTemplate;
    private final Set<Object> viewTemplateColumns;
    private final Set<Object> initialColumns;
    private Map<String, ? extends Object> argumentMap;

    /**
     */
    public Perspective(String namePrefix, String name, String groupName, Boolean isDefault, String filterTemplate, String viewTemplate,
                       List<Object> viewTemplateColumns, List<Object> initialColumns) {
        this(namePrefix, name, groupName, isDefault, filterTemplate, viewTemplate, viewTemplateColumns, initialColumns, true);
    }

    /**
     * Constructor that copies the input perspective.
     *
     * @param persp the perspective to copy
     */
    public Perspective(final Perspective persp) {
        this(persp.namePrefix, persp.name, persp.groupName, persp.isDefault, persp.filterTemplate, persp.viewTemplate,
                persp.getViewTemplateColumnsList(), persp.getInitialColumnsList(), false);
        setArgumentMap(persp.copyArgumentMap());
    }

    private Perspective(String namePrefix, String name, String groupName, Boolean isDefault, String filterTemplate, String viewTemplate,
                        List<Object> viewTemplateColumns, List<Object> initialColumns, boolean loadTemplates) {
        this.namePrefix = namePrefix;
        this.name = name;
        this.groupName = groupName == null ? GLOBAL_GROUP : groupName;
        this.isDefault = isDefault != null && isDefault.booleanValue();
        this.filterTemplate = filterTemplate;
        this.viewTemplate = viewTemplate;
        this.viewTemplateColumns = makeColumnSet(viewTemplateColumns);
        this.initialColumns = makeColumnSet(initialColumns);
        if (loadTemplates) {
            initialize();
        }
    }

    private void initialize() {
        if (viewTemplate != null)
            TEMPLATE_LOADER.putTemplate(getViewTemplateName(), viewTemplate);
        if (filterTemplate != null)
            TEMPLATE_LOADER.putTemplate(getFilterTemplateName(), "<@compress>" + filterTemplate + "</@compress>");
    }

    /**
     * @return the name of this perspective
     */
    public String getName() {
        return name;
    }

    /**
     * @return the group name if one has been defined, GLOBAL_GROUP otherwise.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Note: no enforcement of only one default perspective
     *
     * @return true if this is the default perspective
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * @return The type of view that should be selected by default when this perspective is activated
     */
    public VIEW_TYPE getViewType() {
        if (viewTemplate != null && viewTemplate.length() > 0) {
            return VIEW_TYPE.HTML;
        }
        return VIEW_TYPE.TABLE;
    }

    /**
     * @return The template used to describe the perspective's HTML (record) view. Null if the template defines table view.
     */
    public String getViewTemplate() {
        return viewTemplate;
    }

    /**
     * Set the perspective argument map.
     *
     * @param argumentMap the argument map to set
     */
    public void setArgumentMap(final Map<String, ? extends Object> argumentMap) {
        this.argumentMap = argumentMap;
    }

    private Map<String, ? extends Object> copyArgumentMap() {
        Map<String, Object> newArgumentMap = null;
        if (argumentMap != null) {
            newArgumentMap = new HashMap<String, Object>();
            for (Entry<String, ? extends Object> entry : argumentMap.entrySet()) {
                final Argument dialogArg = (Argument) entry.getValue();
                newArgumentMap.put(entry.getKey(), dialogArg.copyArgument());
            }
        }
        return newArgumentMap;
    }

    /**
     * @return a String representing a filter that should be applied to the data for this perspective
     */
    public String getFilterString() {
        if (argumentMap != null) {
            String filterString = interpolate(getFilterTemplateName(), argumentMap);
            return filterString == null ? "" : filterString;
        }
        return filterTemplate == null ? "" : filterTemplate;
    }

    /**
     * Processes this perspectives view template with the given data.
     * Adds access to the the originating dialog's arguments via a mapping to "dialog_args".
     *
     * @param data the data to interpolate, containing column to value mappings
     * @return a String representing this perspective's view on the given data or null if no view specified
     */
    public String getViewString(Map<String, ? extends Object> data) {
        Map<String, Object> withDialogArguments = new HashMap<>(data);
        if (argumentMap != null) {
            withDialogArguments.put("dialog_args", argumentMap);
        }
        return interpolate(getViewTemplateName(), withDialogArguments);
    }

    /**
     * @return the Set of data columns used by this perspectives view, null if no requirements. Columns can be either names or indices
     */
    public Set<Object> getViewTemplateColumns() {
        return viewTemplateColumns;
    }

    /**
     * @return the Set of columns pertinent to this perspective, or null if none specified. Columns can be either names or indices
     */
    public Set<Object> getInitialColumns() {
        return initialColumns;
    }

    private List<Object> getViewTemplateColumnsList() {
        return viewTemplateColumns != null ? new ArrayList<>(viewTemplateColumns) : null;
    }

    private List<Object> getInitialColumnsList() {
        return initialColumns != null ? new ArrayList<>(initialColumns) : null;
    }

    private String interpolate(String templateName, Map<String, ? extends Object> arguments) {
        try {
            SkipFirstMethodModel sf = (SkipFirstMethodModel) TEMPLATE_CONFIG.getSharedVariable("skip");
            sf.reset();
            Template template = TEMPLATE_CONFIG.getTemplate(templateName);
            template.setTemplateExceptionHandler(new PerspectiveTemplateExceptionHandler());
            StringWriter writer = new StringWriter();
            template.process(arguments, writer);
            return writer.toString().trim();
        } catch (FileNotFoundException fnfe) {
            // Indicates there is no template defined, which is just fine
        } catch (ParseException pe) {
            throw new RuntimeException("Invalid template", pe);
        } catch (IOException ioe) {
            logger.error("Error on String(Reader/Writer) io", ioe);
        } catch (TemplateException te) {
            logger.warn("Could not interpolate query template", te);
            return te.getMessage();
        }
        return null;
    }

    private String getViewTemplateName() {
        return namePrefix + "." + getName() + ".view";
    }

    private String getFilterTemplateName() {
        return namePrefix + "." + getName() + ".filter";
    }

    private Set<Object> makeColumnSet(List<Object> columns) {
        if (columns != null && !columns.isEmpty()) {
            Set<Object> result = new HashSet<Object>(columns.size());
            for (Object o : columns) {
                if (Number.class.isAssignableFrom(o.getClass())) {
                    result.add(((Number) o).intValue()); // indices are expected to be ints but yaml will parse whole numbers as longs
                } else {
                    result.add(o.toString().toLowerCase());
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public String toString() {
        return getName() + " " + getViewType();
    }

    /**
     * Defines the type of view that should be selected by default when the perspective is activated.
     */
    public enum VIEW_TYPE {
        /**
         * Data is displayed with one record per line.
         */
        HTML,
        /**
         * Data is displayed as a table.
         */
        TABLE
    }

    private static final class PerspectiveTemplateExceptionHandler implements TemplateExceptionHandler {
        private static final Pattern MESSAGE_PATTERN = Pattern.compile("Expression [\\p{Graph}]* is undefined on.*");

        @Override
        public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
            try {
                Matcher m = MESSAGE_PATTERN.matcher(te.getMessage());
                if (m.matches()) {
                    final int colNameStart = te.getMessage().indexOf("Expression ") + 11;
                    final int colNameEnd = te.getMessage().indexOf(" is undefined on");
                    out.write("[Unknown column name: " + te.getMessage().substring(colNameStart, colNameEnd).trim() + "]");
                } else {
                    throw te;
                }
            } catch (IOException e) {
                throw new TemplateException("Failed to write required argument. Cause: " + e, env);
            }
        }
    }
}
