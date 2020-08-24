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

package org.gorpipe.gor.model;

import org.gorpipe.gor.model.DbSource;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UTestDbSource {

    @Test
    public void parseLinesForDbSourceInstallationWithDoubleSlashes() {
        List<String> lines = new ArrayList<>();
        lines.add("name\\tdriver\\turl\\tuser\\tpwd\\nrda\\torg.postgresql.Driver\\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\\trda\\tgislireyni22\\n");
        List<String[]> partsList = DbSource.parseLinesForDbSourceInstallation("irrelevant", lines);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals(5, partsList.get(0).length);
    }

    @Test
    public void parseLinesForDbSourceInstallationWithRealTabsAndNewlines() {
        List<String> lines = new ArrayList<>();
        lines.add("name\tdriver\turl\tuser\tpwd");
        lines.add("rda\torg.postgresql.Driver\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\trda\tgislireyni22\n");
        List<String[]> partsList = DbSource.parseLinesForDbSourceInstallation("irrelevant", lines);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals(5, partsList.get(0).length);
    }

    @Test
    public void parseLinesForDbSourceInstallationWithRealTabsAndNewlinesAndCommentedOutLine() {
        List<String> lines = new ArrayList<>();
        lines.add("name\tdriver\turl\tuser\tpwd");
        lines.add("rda\torg.postgresql.Driver\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\trda\tgislireyni22\n");
        lines.add("#rda2\torg.postgresql.Driver\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\trda\tgislireyni22\n");
        List<String[]> partsList = DbSource.parseLinesForDbSourceInstallation("irrelevant", lines);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals(5, partsList.get(0).length);
    }

    @Test
    public void parseLinesForDbSourceInstallationWithRealTabsAndNewlinesAndInvalidLine() {
        List<String> lines = new ArrayList<>();
        lines.add("name\tdriver\turl\tuser\tpwd");
        lines.add("rda\torg.postgresql.Driver\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\trda\tgislireyni22\n");
        // This line forgets the name and has no password, should log an error
        lines.add("org.postgresql.Driver\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\trda\n");
        List<String[]> partsList = DbSource.parseLinesForDbSourceInstallation("irrelevant", lines);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals(5, partsList.get(0).length);
    }
}