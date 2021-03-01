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

import freemarker.template.TemplateException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.QueryEvaluator;
import org.gorpipe.gor.model.ReportCommand;
import org.gorpipe.querydialogs.*;
import org.gorpipe.querydialogs.Dialog;
import org.gorpipe.querydialogs.argument.StringArgument;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author arnie
 * @version $Id$
 */
@SuppressWarnings("javadoc")
public class PerspectiveDialog extends Dialog {
    /***/
    public static final String PROPERTY_HTML_TEMPLATE = "htmlTemplate";
    public final String writePath;
    private final List<Perspective> perspectives;
    private final List<ColumnGroup> columnGroups;
    private final List<ReportCommand> reportCommands;
    private final PerspectiveDialogDisplayParams displayParams;
    private Path dialogsFilePath;
    private String htmlTemplate;
    private Set<Object> initialColumns;
    private ReportViewer reportViewer = null; // Designated report viewer for reports created with this Report Form

    public PerspectiveDialog(Map<String, ? extends Object> attributes, FileReader fileResolver, QueryEvaluator queryEval, DialogDescription dialogDescription,
                             PerspectiveDialogDisplayParams displayParams, DialogType type, String query, String chartScript, String chartExec,
                             String chartColumns, ChartDataType chartDF, List<Argument> arguments, String htmlTemplate, List<? extends Object> initialColumns,
                             List<Perspective> perspectives, List<ColumnGroup> columnGroups, List<ReportCommand> reportCommands,
                             String errorMsgTemplate, String writePath, String longRunningQueryTemplate,
                             String version, String packageVersion, String gitSHA) throws TemplateException {
        super(attributes, fileResolver, queryEval, dialogDescription, type, query, chartScript, chartExec, chartColumns, chartDF,
                arguments, errorMsgTemplate, longRunningQueryTemplate, version, packageVersion, gitSHA);
        this.displayParams = displayParams;
        this.htmlTemplate = htmlTemplate;
        if (initialColumns != null && !initialColumns.isEmpty()) {
            this.initialColumns = new HashSet<>(initialColumns.size());
            for (Object o : initialColumns) {
                if (Number.class.isAssignableFrom(o.getClass())) {
                    this.initialColumns.add(((Number) o).intValue()); // indices are expected to be ints but yaml will parse whole numbers as longs
                } else {
                    this.initialColumns.add(o.toString().toLowerCase());
                }
            }
        }
        this.perspectives = Collections.unmodifiableList(perspectives);
        this.columnGroups = Collections.unmodifiableList(columnGroups);
        this.reportCommands = Collections.unmodifiableList(reportCommands);
        this.writePath = writePath;
        loadReportQueryToTemplateConfig();
        for (Perspective p : this.perspectives) {
            p.setArgumentMap(this.getArgumentMap());
        }
    }

    public PerspectiveDialog(final PerspectiveDialog dlg) throws TemplateException {
        this(dlg.attributes, dlg.getFileResolver(), dlg.getQueryEval(), dlg.dialogDescription, dlg.displayParams, dlg.getType(), dlg.getBaseQuery(),
                dlg.getBaseChartScript(), dlg.getBaseChartExec(), dlg.getBaseChartColumns(), dlg.getBaseChartDF(),
                dlg.copyArguments(), dlg.htmlTemplate, dlg.getInitialColumnsCopy(), dlg.getPerspectivesCopies(), dlg.columnGroups,
                dlg.reportCommands, dlg.errorMsgTemplate, dlg.writePath, dlg.longRunningQueryTemplate, dlg.version, dlg.packageVersion, dlg.gitSHA);
        setReportViewer(dlg.reportViewer);
        this.dialogsFilePath = dlg.dialogsFilePath;
    }

    /**
     * @deprecated Should define an html template for perspectives instead
     */
    @Deprecated
    public String getHtmlTemplate() {
        return htmlTemplate;
    }

    /**
     * @deprecated Should define an html template for perspectives instead
     */
    @Deprecated
    public void setHtmlTemplate(String htmlTemplate) {
        firePropertyChange(PROPERTY_HTML_TEMPLATE, this.htmlTemplate, this.htmlTemplate = htmlTemplate);
    }

    /**
     * @deprecated Should define initial columns for perspectives instead
     */
    @Deprecated
    public Set<Object> getInitialColumns() {
        return initialColumns;
    }

    public List<Perspective> getPerspectives() {
        return perspectives;
    }

    public List<ColumnGroup> getColumnGroups() {
        return columnGroups;
    }

    /**
     * Get copies of perspectives.
     *
     * @return list of perspectives
     */
    public List<Perspective> getPerspectivesCopies() {
        final List<Perspective> newPerspectives = new ArrayList<>();
        for (Perspective perspective : perspectives) {
            newPerspectives.add(new Perspective(perspective));
        }
        return newPerspectives;
    }

    /**
     * Verify dialog by getting the error message. If the error message
     * is empty the dialog is valid and the method returns {@code true}.
     * If the error message is not empty the dialog is invalid, and the error message
     * is displayed in an error dialog and the method returns {@code false}.
     *
     * @return {@code true} if dialog is valid, otherwise {@code false}
     */
    public boolean verify() {
        final String errorMsg = getErrorMessage();
        if (errorMsg != null && errorMsg.length() > 0) {
            throw new RuntimeException(errorMsg);
        }
        return true;
    }

    /**
     * Determine whether the query should be run as a long query which implies that the results of the
     * query will not be streamed to the SM user but instead the results are saved to a directory of
     * the user´s choice.
     *
     * @return {@code true} if the query is a long running query, otherwise {@code false}
     */
    public boolean checkIfLongRunningQuery() {
        final String longRunningMsg = determineLongRunningQuery();
        return longRunningMsg != null && longRunningMsg.length() > 0;
    }

    public void setReportViewer(ReportViewer container) {
        reportViewer = container;
    }

    public void addNewReportToViewer() {
        if (reportViewer == null) {
            throw new RuntimeException("A report viewer must be set prior to adding new report to it");
        }
        reportViewer.addReportToViewer(this);
    }

    /**
     * Get the report command list.
     *
     * @return the report command list
     */
    public List<ReportCommand> getReportCommands() {
        final List<ReportCommand> repCmds = new ArrayList<>();
        for (ReportCommand reportCommand : reportCommands) {
            try {
                final String interpolateQuery = interpolateQuery(reportCommand.getCommandNameAndDigest());
                repCmds.add(new ReportCommand(reportCommand.name, reportCommand.category, interpolateQuery, reportCommand.getRequiredReportColumns()));
            } catch (Exception ex) {
                throw new RuntimeException("Error in drill in report query: " + reportCommand.name + "\n" + ex.getMessage(), ex);
            }
        }
        return repCmds;
    }

    /**
     * Get the dialog category the dialog belongs to.
     *
     * @return the dialog category
     */
    public String getDialogCategory() {
        return displayParams.category;
    }

    /**
     * Get the dialog image color.
     *
     * @return the dialog image color
     */
    public Color getImageColor() {
        return displayParams.getImageColor();
    }

    /**
     * Get the dialog fade in image color.
     *
     * @return the dialog fade in image color
     */
    public Color getFadeInImageColor() {
        return displayParams.getFadeInImageColor();
    }

    /**
     * Get argument map.
     *
     * @return the argument map
     */
    public Map<String, Argument> getArgumentMap() {
        return argumentMap;
    }

    /**
     * Set values for arguments.
     *
     * @param argName2Content argument name to value map
     */
    public void setArgumentValues(final Map<String, ArgumentContent> argName2Content) {
        for (Entry<String, ArgumentContent> argName2ContentEntry : argName2Content.entrySet()) {
            final Argument argument = argumentMap.get(argName2ContentEntry.getKey());
            if (argument != null) {
                final ArgumentContent.Format format = argName2ContentEntry.getValue().format;
                if (format != null && argument instanceof StringArgument) {
                    ((StringArgument) argument).setFormat(format.valueFormat);
                }
                argument.setValue(argName2ContentEntry.getValue().value);
            }
        }
    }

    /**
     * Read argument values from file and set.
     *
     * @param path thepath of the file to read argument values from
     * @throws IOException
     */
    public void setArgumentValues(final Path path) throws IOException {
        final Map<String, ArgumentContent> argName2Content = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(path, Charset.forName("UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitValues = line.split("\t");
                argName2Content.put(splitValues[0], ArgumentContent.parseContent(splitValues[1]));
            }
        }
        setArgumentValues(argName2Content);
    }

    public void saveArgumentValues(final Path selFilePath) {
        try (BufferedWriter bw = Files.newBufferedWriter(selFilePath, Charset.forName("UTF-8"))) {
            bw.write("report_yaml\t" + getDialogsFilePath() + "\n");
            bw.write("report\t" + getName() + "\n");
            for (Entry<String, Argument> entry : argumentMap.entrySet()) {
                final Argument argument = entry.getValue();
                final Object argValue = argument.getValue();
                if (argValue != null && argValue.toString().length() > 0) {
                    String formattedArgValue = formatArgumentForStore(argument);
                    final String argLine = entry.getKey() + "\t" + formattedArgValue + "\n";
                    bw.write(argLine);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error saving argument values", ex);
        }
    }

    public boolean storingAllowed() {
        return writePath != null && writePath.length() > 0;
    }

    private String formatArgumentForStore(final Argument arg) {
        String argValue = arg.getValue().toString();
        if (arg instanceof StringArgument) {
            final String valueFormat = ((StringArgument) arg).getFormat();
            final ArgumentContent.Format argContentFormat = ArgumentContent.Format.getFormatByValueFormat(valueFormat);
            if (argContentFormat != null) {
                argValue = argContentFormat.formatForStore(argValue);
            }
        }
        return argValue;
    }

    public Path getDialogsFilePath() {
        return dialogsFilePath;
    }

    public void setDialogsFilePath(final Path dialogsFilePath) {
        this.dialogsFilePath = dialogsFilePath;
    }

    private void loadReportQueryToTemplateConfig() {
        for (ReportCommand reportCommand : reportCommands) {
            // Use dialog query name as prefix to distinguish between the same report command in different dialog queries
            loadQuery(reportCommand.getCommandNameAndDigest(), reportCommand.command);
        }
    }

    private List<Object> getInitialColumnsCopy() {
        final List<Object> initialColumnsCopyList = new ArrayList<>();
        if (initialColumns != null) {
            initialColumnsCopyList.addAll(initialColumns);
        }
        return initialColumnsCopyList;
    }
}
