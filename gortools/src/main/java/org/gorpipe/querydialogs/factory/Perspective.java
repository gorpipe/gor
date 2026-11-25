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

import freemarker.cache.StringTemplateLoader;
import freemarker.core.Environment;
import freemarker.core.InvalidReferenceException;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.templating.DialogArgumentWrapper;
import org.gorpipe.querydialogs.templating.SkipFirstMethodModel;
import org.gorpipe.querydialogs.templating.TemplateCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.Map.Entry;


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
    /**
     * Always empty. The perspective's own templates are compiled from their source through {@link TemplateCache}, so
     * nothing is written to this shared loader, and an include or import in a perspective template fails with
     * "template not found".
     */
    private static final StringTemplateLoader TEMPLATE_LOADER;
    /**
     * Shared, read-only after configuration. Per-render state (the skip method, the exception handler) is set on each
     * render's Environment.
     */
    private static final Configuration TEMPLATE_CONFIG;
    private static final Logger logger = LoggerFactory.getLogger(Perspective.class);

    static {
        TEMPLATE_CONFIG = new Configuration(Configuration.VERSION_2_3_34);
        TEMPLATE_LOADER = new StringTemplateLoader();
        TEMPLATE_CONFIG.setTemplateLoader(TEMPLATE_LOADER);

        TEMPLATE_CONFIG.setObjectWrapper(new DialogArgumentWrapper());
        TEMPLATE_CONFIG.setLocale(Locale.ENGLISH);
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
        this.namePrefix = namePrefix;
        this.name = name;
        this.groupName = groupName == null ? GLOBAL_GROUP : groupName;
        this.isDefault = isDefault != null && isDefault.booleanValue();
        this.filterTemplate = filterTemplate;
        this.viewTemplate = viewTemplate;
        this.viewTemplateColumns = makeColumnSet(viewTemplateColumns);
        this.initialColumns = makeColumnSet(initialColumns);
    }

    /**
     * Constructor that copies the input perspective.
     *
     * @param persp the perspective to copy
     */
    public Perspective(final Perspective persp) {
        this(persp.namePrefix, persp.name, persp.groupName, persp.isDefault, persp.filterTemplate, persp.viewTemplate,
                persp.getViewTemplateColumnsList(), persp.getInitialColumnsList());
        setArgumentMap(persp.copyArgumentMap());
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
            String filterString = interpolate(getFilterTemplateName(), getFilterTemplateSource(), argumentMap);
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
        return interpolate(getViewTemplateName(), viewTemplate, withDialogArguments);
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

    private String interpolate(String templateName, String templateSource, Map<String, ? extends Object> arguments) {
        if (templateSource == null) {
            // No template defined, which is just fine
            return null;
        }
        try {
            Template template = TemplateCache.get(TEMPLATE_CONFIG, templateName, templateSource);
            StringWriter writer = new StringWriter();
            Environment env = template.createProcessingEnvironment(arguments, writer);
            // Arguments in the data model take precedence, as they did when skip was a shared variable.
            if (!arguments.containsKey("skip")) {
                env.setGlobalVariable("skip", new SkipFirstMethodModel());
            }
            env.setTemplateExceptionHandler(new PerspectiveTemplateExceptionHandler());
            env.process();
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

    private String getFilterTemplateSource() {
        return filterTemplate == null ? null : "<#compress>" + filterTemplate + "</#compress>";
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
        @Override
        public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
            try {
                if (te instanceof InvalidReferenceException ire) {
                    String expr = ire.getBlamedExpressionString();
                    if (expr != null) {
                        out.write("[Unknown column name: " + expr + "]");
                        return;
                    }
                }
                throw te;
            } catch (IOException e) {
                throw new TemplateException("Failed to write required argument. Cause: " + e, env);
            }
        }
    }
}
