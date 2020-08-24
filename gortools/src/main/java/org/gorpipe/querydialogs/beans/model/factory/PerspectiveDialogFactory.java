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
package org.gorpipe.querydialogs.beans.model.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateException;
import org.gorpipe.model.genome.files.gor.FileReader;
import org.gorpipe.model.genome.files.gor.QueryEvaluator;
import org.gorpipe.model.genome.files.gor.ReportCommand;
import org.gorpipe.model.genome.files.gor.RequiredColumn;
import org.gorpipe.querydialogs.beans.ColumnGroup;
import org.gorpipe.querydialogs.beans.model.*;
import org.gorpipe.querydialogs.beans.model.factory.builder.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author arnie
 * @version $Id$
 */
public class PerspectiveDialogFactory extends AbstractDialogFactory<PerspectiveDialog> {
    /**
     * Group used for dialogs where dialog group attribute is not defined
     */
    public static final String OTHER_DIALOG_CATEGORY = "Other";

    public PerspectiveDialogFactory(FileReader fileResolver, QueryEvaluator queryEvaluator) {
        super(fileResolver, queryEvaluator);
    }

    /**
     * Create a new perspective dialog factory.
     *
     * @return perspective dialog factory
     */
    public static PerspectiveDialogFactory create(FileReader fileResolver, QueryEvaluator queryEval) {
        final PerspectiveDialogFactory factory = new PerspectiveDialogFactory(fileResolver, queryEval);
        factory.registerArgumentBuilder(ArgumentType.STRING, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.NUMBER, new NumberArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.DATE, new DateArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.FILE, new FileArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.POSITION_RANGE, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.PN_LISTS, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.FILTERED_PN_LISTS, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.SLIDER, new NumberArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.VIRTUAL_PN_FILE, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.GOR_GRID, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.VALUE_GRID, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.PN_LISTS_ENTRIES, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.GENE_LIST, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.GRID, new GridArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.CHECK_ITEMS, new StringArgumentBuilder(fileResolver));
        factory.registerArgumentBuilder(ArgumentType.QUERY, new QueryArgumentBuilder(fileResolver, queryEval));
        return factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected PerspectiveDialog buildDialog(String name, Map<String, ? extends Object> attributes) throws TemplateException {
        final String category = attributes.containsKey("dialog_group") ? attributes.get("dialog_group").toString() : OTHER_DIALOG_CATEGORY;
        final String imageColor = attributes.containsKey("image_color") ? attributes.get("image_color").toString() : "";
        final PerspectiveDialogDisplayParams displayParams = new PerspectiveDialogDisplayParams(category, imageColor);

        String displayName = attributes.containsKey("name") ? attributes.get("name").toString() : name;
        String shortDescr = attributes.containsKey("short_description") ? attributes.get("short_description").toString() : "Missing short description";
        String detailedDescr = attributes.containsKey("long_description") ? attributes.get("long_description").toString() : "Missing long description";
        String helpLink = attributes.containsKey("help_link") ? attributes.get("help_link").toString() : name;
        final String gitSHA = attributes.containsKey("Git_SHASUM") ? attributes.get("Git_SHASUM").toString() : null;

        if (gitSHA == null) { // 2017-10-18 garpur Lets also handle old style report.yml
            displayName = name;
            detailedDescr = attributes.containsKey("description") ? attributes.get("description").toString() : null;
            shortDescr = attributes.containsKey("list_description") ? attributes.get("list_description").toString() : null;
        }
        final DialogDescription dialogDescription = new DialogDescription(displayName, detailedDescr, shortDescr, helpLink);

        // never used ?
        final String writePath = attributes.containsKey("write_path") ? attributes.get("write_path").toString() : "";

        DialogType type = attributes.containsKey("dialog_type") ? DialogType.valueOf(attributes.get("dialog_type").toString().toUpperCase().trim()) : DialogType.HTML_COL;
        Object oquery = attributes.get("query");
        String query = "";
        if( oquery != null ) {
            if( oquery instanceof Map ) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    query = objectMapper.writeValueAsString(oquery);
                } catch (JsonProcessingException e) {
                    query = oquery.toString();
                }
            } else query = oquery.toString();
        }

        String errorMsgTemplate = attributes.containsKey("error_message") ? attributes.get("error_message").toString() : null;
        String longRunningQueryTemplate = attributes.containsKey("query_type") ? attributes.get("query_type").toString() : null;

        String version = attributes.containsKey("Version_info") ? attributes.get("Version_info").toString() : null;
        String packageVersion = attributes.containsKey("Package_version") ? attributes.get("Package_version").toString() : null;

        List<Argument> arguments = new ArrayList<>();
        if (attributes.containsKey("arguments")) {
            List<Map<String, ? extends Object>> argumentAttributes = (List<Map<String, ? extends Object>>) attributes.get("arguments");

            for (Map<String, ? extends Object> entry : argumentAttributes) {
                try {
                    arguments.add(buildArgument((String) entry.get("name"), entry));
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Error in dialog " + name + "\n" + ex.getMessage());
                }
            }
        }

        String htmlTemplate = attributes.containsKey("html_template") ? attributes.get("html_template").toString() : null;
        List<?> initialColumns = attributes.containsKey("initial_columns") ? (List<?>) attributes.get("initial_columns") : null;

        List<Perspective> perspectives = new ArrayList<>();
        if (attributes.containsKey("perspectives")) {
            List<Map<String, ? extends Object>> perspectiveAttributes = (List<Map<String, ? extends Object>>) attributes.get("perspectives");

            for (Map<String, ? extends Object> entry : perspectiveAttributes) {
                perspectives.add(new Perspective(name, (String) entry.get("name"), (String) entry.get("group"),
                        (Boolean) entry.get("default"), (String) entry.get("filter"), (String) entry.get("view_template"),
                        (List<Object>) entry.get("view_columns"), (List<Object>) entry.get("initial_columns")));
            }
        }

        List<ColumnGroup> columnGroups = new ArrayList<ColumnGroup>();
        if (attributes.containsKey("column_groups")) {
            List<Map<String, ? extends Object>> groupAttributes = (List<Map<String, ? extends Object>>) attributes.get("column_groups");

            int order = 1000;
            for (Map<String, ? extends Object> entry : groupAttributes) {
                columnGroups.add(new ColumnGroup((String) entry.get("name"), (String) entry.get("description"), (String) entry.get("columns"), true, order--));
            }
        }

        String chartScript = attributes.containsKey("chartScript") ? attributes.get("chartScript").toString() : null;
        String chartExec = attributes.containsKey("chart") ? attributes.get("chart").toString() : null;
        String chartColumns = attributes.containsKey("chartColumns") ? attributes.get("chartColumns").toString() : "";
        boolean chartDF = attributes.containsKey("chartDF") && Boolean.parseBoolean(attributes.get("chartDF").toString());

        final List<ReportCommand> reportCommands = getReportCommands(attributes);
        return new PerspectiveDialog(attributes, fileResolver, queryEval, dialogDescription, displayParams, type, query, chartScript,
                chartExec, chartColumns, chartDF, arguments, htmlTemplate, initialColumns, perspectives, columnGroups,
                reportCommands, errorMsgTemplate, writePath, longRunningQueryTemplate, version, packageVersion, gitSHA);
    }

    @Override
    public List<PerspectiveDialog> buildDialogs(final Path resource) throws IOException, TemplateException {
        final List<PerspectiveDialog> dialogs = super.buildDialogs(resource);
        for (PerspectiveDialog dialog : dialogs) {
            dialog.setDialogsFilePath(resource);
        }
        return dialogs;
    }

    @Override
    public List<PerspectiveDialog> buildDialogs(final String resource, final String cacheDir) throws IOException, TemplateException {
        final List<PerspectiveDialog> dialogs = super.buildDialogs(resource, cacheDir);
        for (PerspectiveDialog dialog : dialogs) {
            dialog.setDialogsFilePath(getFileReader().toPath(resource));
        }
        return dialogs;
    }

    @Override
    public List<PerspectiveDialog> buildDialogs(final String resource) throws IOException, TemplateException {
        final List<PerspectiveDialog> dialogs = super.buildDialogs(resource);
        for (PerspectiveDialog dialog : dialogs) {
            dialog.setDialogsFilePath(getFileReader().toPath(resource));
        }
        return dialogs;
    }

    public List<PerspectiveDialog> buildDialogs(final String resource, final byte[] ymlPayload) throws IOException, TemplateException {
        final List<PerspectiveDialog> dialogs = super.buildDialogs(new StringReader(new String(ymlPayload)), null);
        for (PerspectiveDialog dialog : dialogs) {
            dialog.setDialogsFilePath(getFileReader().toPath(resource));
        }
        return dialogs;
    }

    @SuppressWarnings("unchecked")
    private List<ReportCommand> getReportCommands(final Map<String, ? extends Object> attributes) {
        final List<ReportCommand> reportCommands = new ArrayList<>();
        if (attributes.containsKey("drill_in_reports")) {
            List<Map<String, ? extends Object>> reportCommandsAttributes = (List<Map<String, ? extends Object>>) attributes
                    .get("drill_in_reports");

            for (Map<String, ? extends Object> entry : reportCommandsAttributes) {
                final List<RequiredColumn> requiredColumns = new ArrayList<>();
                for (String reqCol : (List<String>) entry.get("required_columns")) {
                    requiredColumns.add(new RequiredColumn(reqCol));
                }
                try {
                    String category = entry.get("category") != null ? (String) entry.get("category") : "";
                    reportCommands.add(new ReportCommand((String) entry.get("name"), category, (String) entry.get("query"), requiredColumns));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return reportCommands;
    }
}
