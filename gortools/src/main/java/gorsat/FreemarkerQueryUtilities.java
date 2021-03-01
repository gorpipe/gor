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

package gorsat;

import freemarker.template.TemplateException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.QueryEvaluator;
import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.ArgumentType;
import org.gorpipe.querydialogs.factory.ArgumentContent;
import org.gorpipe.querydialogs.factory.Perspective;
import org.gorpipe.querydialogs.factory.PerspectiveDialog;
import org.gorpipe.querydialogs.factory.PerspectiveDialogFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Helper functions to parse and create reports with the freemarker parsing engine.
 */
public class FreemarkerQueryUtilities {

    /**
     * Returns a gor query from the input resource file. The gor query is fully resolved and freemarker placeholders
     * are fully replaced by the input parameterMap.
     *
     * @param resource      Path to freemarker yml file
     * @param fileResolver  FileReader instance used for the current execution
     * @param queryEval     Query evaluator based on the current session, for using gor queries within freemarker syntax
     * @param reportName    Name of the report being generated
     * @param parameterMap  Map og parameters and their corresponding values.
     * @param cacheDir      Current cache directory
     * @return              Fully resolved gor query from the input yml file
     * @throws IOException          Yml file not found
     * @throws TemplateException    Internal freemarker error
     */
    public static Optional<String> requestQuery(String resource, FileReader fileResolver, QueryEvaluator queryEval, String reportName, Map<String, String> parameterMap, String cacheDir) throws IOException, TemplateException {
        List<PerspectiveDialog> perspectiveDialogs = PerspectiveDialogFactory.create(fileResolver, queryEval).buildDialogs(resource, cacheDir);
        Optional<PerspectiveDialog> optionalPerspective = getOptionalPerspective(reportName, perspectiveDialogs);

        if (!optionalPerspective.isPresent()) {
            return Optional.empty();
        }

        PerspectiveDialog perspectiveDialog = optionalPerspective.get();
        perspectiveDialog.setArgumentValues(getArgumentValues(parameterMap, fileResolver, perspectiveDialog));

        Optional<String> optional = parameterMap.entrySet().stream().filter(p -> p.getValue() == null).map(Map.Entry::getKey).findFirst();
        Optional<String> perspective = parameterMap.entrySet().stream().filter(p -> p.getKey().equalsIgnoreCase("perspective")).map(Map.Entry::getValue).findFirst();
        String query = perspective.map(s -> getPerspectiveQuery(perspectiveDialog, optional, s)).orElseGet(() -> getQuery(perspectiveDialog, optional));
        String newQuery = getNewQuery(query);

        return Optional.of(newQuery);
    }

    private static Optional<PerspectiveDialog> getOptionalPerspective(String reportName, List<PerspectiveDialog> perspectiveDialogs) {
        Optional<PerspectiveDialog> optionalPerspective;
        if (reportName != null) {
            optionalPerspective = perspectiveDialogs.stream().filter(p -> reportName.equalsIgnoreCase(p.getName())).findFirst();
            if (!optionalPerspective.isPresent()) optionalPerspective = perspectiveDialogs.stream().findFirst();
        } else optionalPerspective = perspectiveDialogs.stream().findFirst();
        return optionalPerspective;
    }

    private static String getPerspectiveQuery(PerspectiveDialog perspectiveDialog, Optional<String> optional, String perspective) {
        StringBuilder query = new StringBuilder(getQuery(perspectiveDialog, optional));
        perspectiveDialog.getPerspectives().stream().filter(pd -> pd.getName().equalsIgnoreCase(perspective)).map(Perspective::getFilterString).findFirst().ifPresent(f -> {
            query.append(" | where ").append(f);
        });
        return query.toString();
    }

    private static String getQuery(PerspectiveDialog perspectiveDialog, Optional<String> optional) {
        if (!optional.isPresent()) {
            return perspectiveDialog.getQuery();
        }
        String query = perspectiveDialog.getAttribute(optional.get()).toString();
        StringBuilder ret = new StringBuilder();
        int start = 0;
        int k = query.indexOf("${");
        while (k != -1) {
            ret.append(query, start, k);
            int n = query.indexOf('}', k + 6);
            if (n == -1) {
                ret.append(query.substring(k));
                break;
            } else if (perspectiveDialog.getArgumentMap().containsKey(query.substring(k + 2, n - 4))) {
                ret.append(perspectiveDialog.getArgumentMap().get(query.substring(k + 2, n - 4)));
            } else {
                ret.append(query, k, n + 1);
            }
            start = n + 1;
            k = query.indexOf("${", start);
        }
        ret.append(query.substring(start));
        query = ret.toString();
        return query;
    }

    private static String getNewQuery(String query) {
        String newQuery = Arrays.stream(query.split(";")).map(String::trim).collect(Collectors.joining(";"));
        String[] virtualFiles = {"[gorgrid:", "[grid:"};
        for (String virtualfile : virtualFiles) {
            int i = newQuery.indexOf(virtualfile);
            while (i != -1) {
                int e = newQuery.indexOf(']', i);
                int c = newQuery.indexOf('\'', i);
                if (c < e && c != -1) {
                    newQuery = newQuery.substring(0, i) + newQuery.substring(c + 1, newQuery.indexOf('\'', c + 1)).trim() + newQuery.substring(e + 1);
                } else {
                    newQuery = newQuery.substring(0, i) + newQuery.substring(i + virtualfile.length() + 1, e).trim() + newQuery.substring(e + 1);
                }
                i = newQuery.indexOf(virtualfile, i + 1);
            }
        }
        return newQuery;
    }

    private static String readPnListFromFile(String value, FileReader fileResolver) {
        String result = value;
        try {
            if (Files.exists(Paths.get(value))) {
                String[] lines = fileResolver.readAll(value);
                if (lines.length > 0) {
                    StringBuilder builder = new StringBuilder();

                    for (String line : lines) {
                        String quote = "'";
                        if (line.contains("'"))
                            quote = "\"";
                        builder.append(quote).append(line).append(quote).append(",");
                    }

                    result = builder.toString();
                    result = result.substring(0, result.length() - 1);
                }
            }
        } catch (Exception e) {
            // Do nothing
        }
        return result;
    }

    private static Map<String, ArgumentContent> getArgumentValues
            (Map<String, String> parameterMap, FileReader fileResolver, PerspectiveDialog perspectiveDialog) {
        return parameterMap.entrySet().stream().filter(p -> p.getValue() != null).collect(
                Collectors.toMap(Map.Entry::getKey, entry ->
                        {
                            Argument argument = perspectiveDialog.getArgument(entry.getKey());
                            String value;
                            if (argument != null && argument.getType() == ArgumentType.PN_LISTS_ENTRIES) {
                                //we need to load the pns from the file ans replace the original value
                                value = readPnListFromFile(entry.getValue(), fileResolver);
                            } else {
                                value = entry.getValue();
                            }
                            if (value.startsWith("[")) {
                                int end = value.indexOf(']');
                                if (value.substring(0, end).contains(",")) value = value.substring(1, value.length() - 1);
                            }
                            return new ArgumentContent(value);
                        }
                ));

    }

    private FreemarkerQueryUtilities() {}
}
