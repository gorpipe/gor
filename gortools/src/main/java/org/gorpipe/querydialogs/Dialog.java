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
package org.gorpipe.querydialogs;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.core.ParseException;
import freemarker.template.*;
import org.gorpipe.gor.model.Constants;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.QueryEvaluator;
import org.gorpipe.querydialogs.templating.DialogArgumentWrapper;
import org.gorpipe.querydialogs.templating.NetworkTemplateLoader;
import org.gorpipe.querydialogs.templating.QueryEvalMethodModel;
import org.gorpipe.querydialogs.templating.SkipFirstMethodModel;
import org.gorpipe.util.collection.extract.Extract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A basic dialog implementation
 * <p>
 * <p>Uses the freemarker library to interpolate the dialog's query with
 * supplied arguments.<br>
 * The provided query is processed such that all whitespace is
 * replaced with a single space character.<br>
 * Provides a couple of freemarker directives to query writers:
 * </p>
 * <dl>
 * <dt><code>skip(value[, scope])</code></dt>
 * <dd>will skip (i.e. not output) the first occurance of the given value within the given scope</dd>
 * <dt>br</dt>
 * <dd>inserts an explicit newline</dd>
 * </dl>
 * <p>
 * <p>All arguments are wrapped in a freemarker object wrapper such that when
 * referencing the argument's value the query writer uses {@code argname.val}
 * and when referencing the argument's operator the query writer uses {@code argname.op}.</p>
 * <p>
 * <p>Aside from being a bean model for a dialog this class provides a ListModel
 * interface on the dialog's arguments.</p>
 * <p>The interpolated query is defined as executable if all non-optional
 * arguments have received valid values.</p>
 *
 * @author arnie
 * @version $Id$
 */
@SuppressWarnings({"javadoc", "squid:S1948"}) // Gummi S. Added to remove critical sonar cube warning due to class not being serializable.
public class Dialog extends AbstractListBean {
    /**
     * The property name for the dialog's type.
     */
    public static final String PROPERTY_TYPE = "type";
    /**
     * The property name for the dialog's base query (not interpolated).
     */
    public static final String PROPERTY_BASE_QUERY = "baseQuery";
    /**
     * The property name for the dialog's base query (not interpolated).
     */
    public static final String PROPERTY_BASE_CHARTSCRIPT = "baseChartScript";
    /**
     * The property name for the dialog's base query (not interpolated).
     */
    public static final String PROPERTY_BASE_CHARTEXEC = "baseChartExec";
    /**
     * The property name for the dialog's base query (not interpolated).
     */
    public static final String PROPERTY_BASE_CHARTCOLUMNS = "baseChartColumns";
    /**
     * The property name for dialog's interpolated query (read only).
     */
    public static final String PROPERTY_BASE_CHARTDF = "baseChartDF";
    /**
     * The property name for dialog's interpolated query (read only).
     */
    public static final String PROPERTY_QUERY = "query";
    /**
     * The property name for dialog's interpolated query (read only).
     */
    public static final String PROPERTY_CHART = "chart";
    /**
     * The property name for the dialog's executable state.
     */
    public static final String PROPERTY_EXECUTABLE = "executable";
    private static final String ERROR_MSG_TEMPLATE_NAME_SUFFIX = "_error_message_";
    private static final String LONG_RUNNING_QUERY_TEMPLATE_NAME_SUFFIX = "_long_running_query_";
    private static final Logger logger = LoggerFactory.getLogger(Dialog.class);
    private static StringTemplateLoader DIALOG_TEMPLATE_LOADER;
    private static Configuration TEMPLATE_CONFIG;
    private static String projectName;

    static {
        try {
            // Turn off freemarker library logging
            freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
        } catch (ClassNotFoundException e) {
            /* ignore */
        }
    }

    public final DialogDescription dialogDescription;
    protected final String errorMsgTemplate;
    protected final String longRunningQueryTemplate;
    protected final String version;
    protected final String packageVersion;
    protected final String gitSHA;
    protected final Map<String, Argument> argumentMap;
    private final PropertyChangeListener argumentListener;
    private final List<String> arguments;
    public Map<String, ? extends Object> attributes;
    protected boolean executable;
    private DialogType type;
    private String baseQuery;
    private String baseQueryMd5Digest;
    private String baseChartMd5Digest;
    private String baseChartExec;
    private String baseChartScript;
    private String baseChartColumns;
    private boolean baseChartDF;
    private String interpolatedQuery;
    private String interpolatedChart;
    private String[] chartColumns;
    private boolean advancedArgumentsVisible;
    private boolean hasErrorMsgTemplate = false;
    private boolean hasLongRunningQuery = false;
    private final FileReader fileResolver;
    private final QueryEvaluator queryEval;
    private boolean deferUpdates;

    public Dialog(Map<String, ? extends Object> attributes, FileReader fileResolver, QueryEvaluator queryEval, DialogDescription dialogDescription,
                  DialogType type, String query, String chartScript, String chartExec, String chartColumns, boolean chartDF,
                  List<Argument> arguments, String errorMsgTemplate, String longRunningQueryTemplate, String version, String packageVersion, String gitSHA)
            throws TemplateException {
        this.fileResolver = fileResolver;
        this.queryEval = queryEval;
        setConfig();
        this.attributes = attributes;
        this.dialogDescription = dialogDescription;
        this.arguments = new ArrayList<>();
        this.errorMsgTemplate = errorMsgTemplate;
        this.longRunningQueryTemplate = longRunningQueryTemplate;
        this.version = version;
        this.packageVersion = packageVersion;
        this.gitSHA = gitSHA;

        argumentMap = new HashMap<>();
        for (Argument a : arguments) {
            argumentMap.put(a.getName(), a);
            this.arguments.add(a.getName());
        }

        argumentListener = new ArgumentListener();

        for (Argument a : this.argumentMap.values()) {
            a.addPropertyChangeListener(argumentListener);
        }

        setType(type);
        setBaseQuery(query);
        setBaseChartScript(chartScript);
        setBaseChartExec(chartExec);
        setBaseChartColumns(chartColumns);
        setBaseChartDF(chartDF);
        loadErrorMessageTemplate();
        loadLongRunningQueryTemplate();
    }

    private static void initializeTemplateConfig(FileReader fileResolver, QueryEvaluator queryEval) {
        TEMPLATE_CONFIG = new Configuration();
        TEMPLATE_CONFIG.setLocalizedLookup(false);
        DIALOG_TEMPLATE_LOADER = new StringTemplateLoader();

        String macroPath = System.getProperty("dialog.macrodir", null);
        if (macroPath != null) {
            NetworkTemplateLoader netLoader = new NetworkTemplateLoader(macroPath, fileResolver);
            TEMPLATE_CONFIG.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{DIALOG_TEMPLATE_LOADER, netLoader}));
        } else {
            TEMPLATE_CONFIG.setTemplateLoader(DIALOG_TEMPLATE_LOADER);
        }

        TEMPLATE_CONFIG.setObjectWrapper(new DialogArgumentWrapper());
        TEMPLATE_CONFIG.setTemplateExceptionHandler(new DialogTemplateExceptionHandler());
        TEMPLATE_CONFIG.setSharedVariable("skip", new SkipFirstMethodModel());
        TEMPLATE_CONFIG.setSharedVariable("br", new SimpleScalar("\\n"));
        TEMPLATE_CONFIG.setLocale(java.util.Locale.ENGLISH);
        TEMPLATE_CONFIG.setSharedVariable("gor", new QueryEvalMethodModel(queryEval));
    }

    public FileReader getFileResolver() {
        return fileResolver;
    }

    public QueryEvaluator getQueryEval() {
        return this.queryEval;
    }

    public String getName() {
        return dialogDescription.name;
    }

    private String getBaseQueryNameAndDigest() {
        if (baseQueryMd5Digest == null || baseQueryMd5Digest.length() == 0) {
            baseQueryMd5Digest = Extract.md5(baseQuery);
        }
        return getName() + "_" + baseQueryMd5Digest;
    }

    private String getBaseChartNameAndDigest() {
        if (baseChartMd5Digest == null || baseChartMd5Digest.length() == 0) {
            baseChartMd5Digest = Extract.md5(baseChartExec);
        }
        return getName() + "_" + baseChartMd5Digest;
    }

    /**
     * @return The dialog name shortened so that it contains at most 2 capital letters (from right) and breaks on the right most underscore
     */
    public String getShortName() {
        final char[] name = getName().toCharArray();
        int upperCnt = 0;
        int i = name.length - 1;
        for (; i > 0 && upperCnt < 2 && name[i] != '_'; i--) {
            if (Character.isUpperCase(name[i]) && Character.isLowerCase(name[i - 1])) {
                upperCnt++;
            }
        }
        if (i > 0 || name[0] == '_') {
            i++;
        }
        return new String(name, i, name.length - i);
    }

    public String getDescription() {
        return dialogDescription.detailedDescr;
    }

    public String getHelpLink() {
        return dialogDescription.helpLink;
    }

    public String getListDescription() {
        return dialogDescription.shortDescr;
    }

    public DialogType getType() {
        return type;
    }

    public void setType(DialogType type) {
        DialogType oldType = this.type;
        this.type = type;
        firePropertyChange(PROPERTY_TYPE, oldType, type);
    }

    public String getBaseQuery() {
        return baseQuery;
    }

    public void setBaseQuery(String query) throws TemplateException {
        String oldQuery = this.baseQuery;
        this.baseQuery = query;
        baseQueryMd5Digest = Extract.md5(baseQuery);
        firePropertyChange(PROPERTY_BASE_QUERY, oldQuery, query);
        if (query == null || !query.equals(oldQuery)) {
            DIALOG_TEMPLATE_LOADER.putTemplate(getBaseQueryNameAndDigest(), query);
            updateInterpolatedQuery();
        }
    }

    public String getBaseChartExec() {
        return baseChartExec;
    }

    public void setBaseChartExec(String chartExec) throws TemplateException {
        if (chartExec != null) {
            String oldChartExec = this.baseChartExec;
            this.baseChartExec = chartExec;
            baseChartMd5Digest = Extract.md5(baseChartExec);
            firePropertyChange(PROPERTY_BASE_CHARTEXEC, oldChartExec, chartExec);
            if (chartExec == null || !chartExec.equals(oldChartExec)) {
                DIALOG_TEMPLATE_LOADER.putTemplate(getBaseChartNameAndDigest(), chartExec);
                updateInterpolatedChart();
            }
        }
    }

    public String getBaseChartScript() {
        return baseChartScript;
    }

    public void setBaseChartScript(String chartScript) {
        String oldChartScript = this.baseChartScript;
        this.baseChartScript = chartScript;
        firePropertyChange(PROPERTY_BASE_CHARTSCRIPT, oldChartScript, chartScript);
    }

    public boolean getBaseChartDF() {
        return baseChartDF;
    }

    public void setBaseChartDF(boolean chartDF) {
        boolean oldChartDF = this.baseChartDF;
        this.baseChartDF = chartDF;
        firePropertyChange(PROPERTY_BASE_CHARTDF, oldChartDF, chartDF);
    }

    public String getBaseChartColumns() {
        return baseChartColumns;
    }

    public void setBaseChartColumns(String chartColumns) {
        String oldChartColumns = this.baseChartColumns;
        if (chartColumns != null) {
            this.baseChartColumns = chartColumns;
            this.chartColumns = this.baseChartColumns.trim().split(",");
            firePropertyChange(PROPERTY_BASE_CHARTCOLUMNS, oldChartColumns, chartColumns);
        }
    }

    /**
     * Defer updates to interpolated query.
     * If set - query won't be evaluated on every change to input parameters but only when requested through
     * getInterpolatedQuery()
     */
    public void setDeferUpdates(boolean deferUpdates) {
        this.deferUpdates = deferUpdates;
    }

    /**
     * @return <code>true</code> if advanced arguments are visible, otherwise <code>false</code>
     */
    public boolean advancedArgumentsVisible() {
        return advancedArgumentsVisible;
    }

    /**
     * @param visible the visibility of advanced arguments to set
     */
    public void setAdvancedArgumentsVisible(final boolean visible) {
        this.advancedArgumentsVisible = visible;
    }

    protected void loadQuery(final String queryName, final String query) {
        DIALOG_TEMPLATE_LOADER.putTemplate(queryName, query);
    }

    protected String interpolateQuery(final String templateName) throws TemplateException, IOException {
        SkipFirstMethodModel sf = (SkipFirstMethodModel) TEMPLATE_CONFIG.getSharedVariable("skip");
        sf.reset();
        Template template = TEMPLATE_CONFIG.getTemplate(templateName);
        StringWriter writer = new StringWriter();
        template.process(argumentMap, writer);
        return writer.toString().trim().replaceAll("\\\\n", "\n");
    }

    protected String interpolateChart(final String templateName) throws TemplateException, IOException {
        SkipFirstMethodModel sf = (SkipFirstMethodModel) TEMPLATE_CONFIG.getSharedVariable("skip");
        sf.reset();
        Template template = TEMPLATE_CONFIG.getTemplate(templateName);
        StringWriter writer = new StringWriter();
        template.process(argumentMap, writer);
        return writer.toString().trim().replaceAll("\\\\n", "\n");
    }

    private void updateInterpolatedQuery() throws TemplateException {
        if (deferUpdates) {
            interpolatedQuery = null;
        } else {
            calcInterpolatedQuery();
        }
    }

    public void calcInterpolatedQuery() throws TemplateException {
        String oldQuery = interpolatedQuery;
        if (baseQuery == null) {
            interpolatedQuery = null;
            firePropertyChange(PROPERTY_QUERY, oldQuery, null);
            updateExecutable(false);
        } else {
            try {
                DialogTemplateExceptionHandler validator = (DialogTemplateExceptionHandler) TEMPLATE_CONFIG.getTemplateExceptionHandler();
                validator.reset(this);
                interpolatedQuery = interpolateQuery(getBaseQueryNameAndDigest());
                firePropertyChange(PROPERTY_QUERY, oldQuery, interpolatedQuery);
                boolean someMandatoryUnset = false;
                for (String key : this.argumentMap.keySet()) {
                    Argument a = this.argumentMap.get(key);
                    someMandatoryUnset = someMandatoryUnset || (!a.isOptional() && a.isEmpty());
                }

                boolean isExecutable = validator.isValid() && !someMandatoryUnset && !interpolatedQuery.isEmpty();

                updateExecutable(isExecutable);
            } catch (ParseException e) {
                throw new RuntimeException("Invalid query template", e);
            } catch (IOException e) {
                logger.error("Error on String(Reader/Writer) io", e);
            }
        }
    }

    private void updateInterpolatedChart() throws TemplateException {
        String oldChart = interpolatedChart;
        if (baseChartExec == null) {
            interpolatedChart = null;
            firePropertyChange(PROPERTY_CHART, oldChart, null);
            updateExecutable(false);
        } else {
            try {
                DialogTemplateExceptionHandler validator = (DialogTemplateExceptionHandler) TEMPLATE_CONFIG.getTemplateExceptionHandler();
                validator.reset(this);
                interpolatedChart = interpolateChart(getBaseChartNameAndDigest());
                firePropertyChange(PROPERTY_CHART, oldChart, interpolatedChart);
                updateExecutable(validator.isValid() && !interpolatedChart.isEmpty());
            } catch (ParseException e) {
                throw new RuntimeException("Invalid chart template", e);
            } catch (IOException e) {
                logger.error("Error on String(Reader/Writer) io", e);
            }
        }
    }

    public String getInterpolatedQuery() {
        if (interpolatedQuery == null) {
            try {
                calcInterpolatedQuery();
            } catch (TemplateException e) {
                throw new RuntimeException(e);
            }
        }
        return interpolatedQuery;
    }

    public String getInterpolatedChart() {
        return interpolatedChart;
    }

    public String getQuery() {
        return getInterpolatedQuery();
    }

    public String getChart() {
        return getInterpolatedChart();
    }

    public String getVersion() {
        return version;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public String getGitSha() {
        return gitSHA;
    }

    private void loadErrorMessageTemplate() {
        if (errorMsgTemplate != null && errorMsgTemplate.length() > 0) {
            loadQuery(getErrorMessageTemplateName(), errorMsgTemplate);
            hasErrorMsgTemplate = true;
        }
    }

    private void loadLongRunningQueryTemplate() {
        if (longRunningQueryTemplate != null && longRunningQueryTemplate.length() > 0) {
            loadQuery(getLongRunningQueryTemplateName(), longRunningQueryTemplate);
            hasLongRunningQuery = true;
        }
    }

    private String getLongRunningQueryTemplateName() {
        return getName() + LONG_RUNNING_QUERY_TEMPLATE_NAME_SUFFIX + Extract.md5(longRunningQueryTemplate);
    }

    private String getErrorMessageTemplateName() {
        return getName() + ERROR_MSG_TEMPLATE_NAME_SUFFIX + Extract.md5(errorMsgTemplate);
    }

    public String getErrorMessage() {
        String interpolatedErrorMsg = "";
        if (hasErrorMsgTemplate) {
            try {
                interpolatedErrorMsg = interpolateQuery(getErrorMessageTemplateName());
            } catch (TemplateException | IOException e) {
                logger.warn("Could not interpolate query template: " + e.getMessage());
            }
        }
        return interpolatedErrorMsg;
    }

    public String determineLongRunningQuery() {
        String longRunningQueryMsg = "";
        if (hasLongRunningQuery) {
            try {
                longRunningQueryMsg = interpolateQuery(getLongRunningQueryTemplateName());
            } catch (TemplateException | IOException e) {
                logger.warn("Could not interpolate long running query template: " + e.getMessage());
            }
        }
        return longRunningQueryMsg;
    }

    public List<Argument> copyArguments() {
        final List<Argument> argumentCopies = new ArrayList<Argument>();
        for (String argument : arguments) {
            argumentCopies.add(argumentMap.get(argument).copyArgument());
        }
        return argumentCopies;
    }

    private void updateExecutable(boolean isExecutable) {
        boolean wasExecutable = this.executable;
        this.executable = isExecutable;
        firePropertyChange(PROPERTY_EXECUTABLE, wasExecutable, isExecutable);
    }

    public boolean isExecutable() {
        return executable;
    }

    public Argument getArgument(String argumentName) {
        return argumentMap.get(argumentName);
    }

    public void setArgument(String argumentName, Argument arg) {
        argumentMap.put(argumentName, arg);
    }

    public Object getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }

    @Override
    public Object getElementAt(int index) {
        return argumentMap.get(arguments.get(index));
    }

    @Override
    public int getSize() {
        return arguments.size();
    }

    @Override
    public String toString() {
        return getName();
    }

    private void setConfig() {
        if (Constants.isSet()) {
            final String constantsProjectName = Constants.get().projectName();
            if (!constantsProjectName.equals(projectName)) {
                initializeTemplateConfig(fileResolver, queryEval);
                projectName = constantsProjectName;
            }
        } else if (TEMPLATE_CONFIG == null) {
            initializeTemplateConfig(fileResolver, queryEval);
        }
    }

    private static final class DialogTemplateExceptionHandler implements TemplateExceptionHandler {
        static final Pattern MESSAGE_PATTERN = Pattern.compile("^Expression (.+)\\.(.+) is undefined on.*");

        int count = 0;
        Dialog dialog;

        void reset(Dialog d) {
            count = 0;
            dialog = d;
        }

        boolean isValid() {
            return count == 0;
        }

        @Override
        public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
            try {
                Matcher m = MESSAGE_PATTERN.matcher(te.getMessage());

                if (m.matches() && m.group(2).startsWith("val")) {
                    if (!dialog.getArgument(m.group(1)).isOptional()) {
                        count++;
                        out.write("REQUIRED(" + m.group(1) + ")");
                    }
                } else {
                    throw te;
                }
            } catch (IOException e) {
                throw new TemplateException("Failed to write required argument. Cause: " + e, env);
            }
        }
    }

    private final class ArgumentListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(Argument.PROPERTY_VALUE) || evt.getPropertyName().equals(Argument.PROPERTY_OPERATOR))
                try {
                    updateInterpolatedQuery();
                } catch (TemplateException ex) {
                    logger.warn("Could not interpolate query template", ex);
                }
        }
    }
}
