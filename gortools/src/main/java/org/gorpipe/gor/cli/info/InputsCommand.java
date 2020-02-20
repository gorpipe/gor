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

package org.gorpipe.gor.cli.info;

import org.gorpipe.gor.cli.HelpOptions;
import gorsat.process.GorInputSources;
import picocli.CommandLine;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "inputs",
        header = "List gor input soures",
        description="List all inputs in gor and their options")
public class InputsCommand extends HelpOptions implements  Runnable{

    @Override
    public void run() {
        try {
            GorInputSources.register();
            System.out.print(GorInputSources.getInputSourceInfoTable());
        } catch (Exception e) {
            System.err.println("Error: \n");
            System.err.println(e.getMessage());
        }
    }
}
