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

package org.gorpipe.gor.function;

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.util.DataUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * report(file.yml[::ReportName] [, key=value | entry]...) renders a yml report builder, a local file or a url such as
 * template://org/category/name/version.yml. Arguments are the same as for file.yml(...): key=value pairs set report
 * arguments and an argument without a value selects the query entry to render. It can also be used as the argument of
 * PIPESTEPS.
 */
public class ReportFunctionSource implements FunctionSource {

    private static final String YML_SUFFIX = ".yml";
    private static final String REPORT_NAME_SEPARATOR = "::";

    @Override
    public String name() {
        return "report";
    }

    @Override
    public String query(String[] args, GorSession session) {
        if (args.length == 0) {
            throw new GorParsingException("report requires a yml report builder file or url as its first argument, " +
                    "e.g. report(file.yml, top=10)");
        }

        String resource = args[0];
        String reportName = null;
        int i = resource.toLowerCase().indexOf(YML_SUFFIX + REPORT_NAME_SEPARATOR);
        if (i != -1) {
            reportName = resource.substring(i + YML_SUFFIX.length() + REPORT_NAME_SEPARATOR.length()).trim();
            resource = resource.substring(0, i + YML_SUFFIX.length());
        }
        if (!DataUtil.isYml(resource)) {
            throw new GorParsingException("report requires a yml report builder file or url, got: " + resource);
        }

        Map<String, String> parameters = new LinkedHashMap<>();
        for (int a = 1; a < args.length; a++) {
            String arg = args[a];
            int eq = arg.indexOf('=');
            if (eq == -1) {
                parameters.put(arg, null);
            } else {
                parameters.put(arg.substring(0, eq).trim(), unquote(arg.substring(eq + 1).trim()));
            }
        }

        return session.getSystemContext().getReportBuilder().renderQuery(resource, reportName, parameters);
    }

    // Pipe steps are rendered as a query, with the leading input source removed as for file.yml(...)
    @Override
    public String pipeSteps(String[] args, GorSession session) {
        String steps = query(args, session);
        String upper = steps.toUpperCase();
        return upper.startsWith("GOR ") || upper.startsWith("NOR ") ? steps.substring(4).trim() : steps;
    }

    private static String unquote(String value) {
        if (value.length() > 1 && value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
