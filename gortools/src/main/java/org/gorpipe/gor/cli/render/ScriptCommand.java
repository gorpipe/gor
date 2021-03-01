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

package org.gorpipe.gor.cli.render;

import gorsat.Commands.CommandParseUtilities;
import gorsat.process.CLISessionFactory;
import gorsat.process.PipeOptions;
import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.session.GorSession;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "script",
        aliases = {"s"},
        header = "Renders gor script",
        description="Renders gor script to its final executable form")
public class ScriptCommand extends RenderOptions implements  Runnable{

    @Override
    public void run() {
        File scriptFile = new File(this.input);
        String query = this.input;

        if (scriptFile.exists()) {
            try {
                query = FileUtils.readFileToString(scriptFile, Charset.defaultCharset());
            } catch (IOException e) {
                System.err.println("Failed to load script file: " + this.input);
                System.exit(-1);
            }
        }

        query = CommandParseUtilities.cleanupQuery(query);

        PipeOptions options = PipeOptions.parseInputArguments(this.aliasFile != null ? new String[] {this.input,  "-alias", this.aliasFile.toString()} :
                new String[] {this.input});
        CLISessionFactory sessionFactory = new CLISessionFactory(options, "");
        GorSession session = sessionFactory.create();
        ReportCommand.renderQuery(session, query, this.pretty);
    }
}
