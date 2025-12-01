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

package org.gorpipe.gor.cli.render;

import gorsat.Utilities.AnalysisUtilities;
import gorsat.Commands.CommandParseUtilities;
import gorsat.Utilities.MacroUtilities;
import gorsat.process.CLISessionFactory;
import gorsat.process.PipeOptions;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.util.DataUtil;
import picocli.CommandLine;

import java.util.Map;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "report",
        aliases = {"r"},
        header = "Renders gor report",
        description="Renders yml report to its final executable form.")
public class ReportCommand extends RenderOptions implements  Runnable{

    @Override
    public void run() {
        if (DataUtil.isYml(input)) {
            PipeOptions options = PipeOptions.parseInputArguments(this.aliasFile != null ? new String[] {this.input,  "-aliases", this.aliasFile.toString()} :
                    new String[] {this.input});
            CLISessionFactory sessionFactory = new CLISessionFactory(options, "");
            GorSession session = sessionFactory.create();
            String query = CommandParseUtilities.cleanupQuery(session.getSystemContext().getReportBuilder().parse(this.input));
            renderQuery(session, query, this.pretty);
        } else {
            System.err.println("Input is not a yml report.");
        }
    }

    static void renderQuery(GorSession session, String query, boolean pretty) {
        String[] commands = CommandParseUtilities.quoteSafeSplitAndTrim(query, ';');
        Map<String,String> defines = MacroUtilities.extractAliases(commands);
        commands = MacroUtilities.applyAliases(commands, defines);
        String finalQuery = CommandParseUtilities.cleanupQueryWithFormat(String.join(";", commands));

        if (session.getProjectContext().getGorAliasFile() != null) {
            Map<String, String> aliases = AnalysisUtilities.loadAliases(session.getProjectContext().getGorAliasFile(), session, "gor_aliases.txt");
            finalQuery = MacroUtilities.replaceAllAliases(finalQuery, aliases);
        }
        System.out.println(pretty ? CommandParseUtilities.cleanupQueryWithFormat(finalQuery) :
                CommandParseUtilities.cleanupQuery(finalQuery));
    }
}
