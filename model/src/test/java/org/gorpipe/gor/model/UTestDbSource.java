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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UTestDbSource {

    @Test
    public void parseLinesForDbSourceInstallationWithDoubleSlashes() {
        List<String> lines = new ArrayList<>();
        lines.add("name\\tdriver\\turl\\tuser\\tpwd\\nrda\\torg.postgresql.Driver\\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\\trda\\tgislireyni22\\n");
        List<String[]> partsList = DbConnectionCache.parseLinesForDbSourceInstallation("irrelevant", lines);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals(5, partsList.get(0).length);
    }

    @Test
    public void parseLinesForDbSourceInstallationWithRealTabsAndNewlines() {
        List<String> lines = new ArrayList<>();
        lines.add("name\tdriver\turl\tuser\tpwd");
        lines.add("rda\torg.postgresql.Driver\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\trda\tgislireyni22\n");
        List<String[]> partsList = DbConnectionCache.parseLinesForDbSourceInstallation("irrelevant", lines);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals(5, partsList.get(0).length);
    }

    @Test
    public void parseLinesForDbSourceInstallationWithRealTabsAndNewlinesAndCommentedOutLine() {
        List<String> lines = new ArrayList<>();
        lines.add("name\tdriver\turl\tuser\tpwd");
        lines.add("rda\torg.postgresql.Driver\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\trda\tgislireyni22\n");
        lines.add("#rda2\torg.postgresql.Driver\tjdbc:postgresql://gor-dev.cqi71y09rnsb.us-east-1.rds.amazonaws.com:5432/csa\trda\tgislireyni22\n");
        List<String[]> partsList = DbConnectionCache.parseLinesForDbSourceInstallation("irrelevant", lines);
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
        List<String[]> partsList = DbConnectionCache.parseLinesForDbSourceInstallation("irrelevant", lines);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals(5, partsList.get(0).length);
    }

    private static Map<String, String> rdaEnv(String url, String username, String password) {
        Map<String, String> env = new HashMap<>();
        if (url != null) env.put("APPSERVER_RDA_URL", url);
        if (username != null) env.put("APPSERVER_RDA_USERNAME", username);
        if (password != null) env.put("APPSERVER_RDA_PASSWORD", password);
        return env;
    }

    @Test
    public void parseEnvBuildsRdaPartsInFileParserShape() {
        Map<String, String> env = rdaEnv("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret");
        List<String[]> partsList = DbConnectionCache.parseEnvForDbSourceInstallation(env);
        Assert.assertEquals(1, partsList.size());
        String[] parts = partsList.get(0);
        Assert.assertEquals(5, parts.length);
        Assert.assertEquals("rda", parts[0]);
        Assert.assertEquals("org.postgresql.Driver", parts[1]);
        Assert.assertEquals("jdbc:postgresql://db:5432/csa", parts[2]);
        Assert.assertEquals("gregor_reader", parts[3]);
        Assert.assertEquals("secret", parts[4]);
    }

    @Test
    public void parseEnvOmitsPasswordFieldWhenPasswordUnset() {
        Map<String, String> env = rdaEnv("jdbc:postgresql://db:5432/csa", "gregor_reader", null);
        List<String[]> partsList = DbConnectionCache.parseEnvForDbSourceInstallation(env);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals(4, partsList.get(0).length);
    }

    @Test
    public void parseEnvReturnsNothingWhenNoRdaVarsPresent() {
        Assert.assertTrue(DbConnectionCache.parseEnvForDbSourceInstallation(new HashMap<>()).isEmpty());
    }

    @Test
    public void parseEnvSkipsSourceWhenUsernameMissing() {
        Map<String, String> env = rdaEnv("jdbc:postgresql://db:5432/csa", null, "secret");
        Assert.assertTrue(DbConnectionCache.parseEnvForDbSourceInstallation(env).isEmpty());
    }

    @Test
    public void parseEnvSkipsSourceWhenUrlMissing() {
        Map<String, String> env = rdaEnv(null, "gregor_reader", "secret");
        Assert.assertTrue(DbConnectionCache.parseEnvForDbSourceInstallation(env).isEmpty());
    }

    @Test
    public void parseEnvTreatsBlankValuesAsUnset() {
        Map<String, String> env = rdaEnv("jdbc:postgresql://db:5432/csa", "   ", "secret");
        Assert.assertTrue(DbConnectionCache.parseEnvForDbSourceInstallation(env).isEmpty());
    }

    @Test
    public void parseEnvDerivesOracleDriverFromUrlPrefix() {
        Map<String, String> env = rdaEnv("jdbc:oracle:thin:@db:1521:XE", "gregor_reader", "secret");
        List<String[]> partsList = DbConnectionCache.parseEnvForDbSourceInstallation(env);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals("oracle.jdbc.driver.OracleDriver", partsList.get(0)[1]);
    }

    @Test
    public void parseEnvHonoursExplicitDriverOverride() {
        Map<String, String> env = rdaEnv("jdbc:whatever://db/x", "gregor_reader", "secret");
        env.put("APPSERVER_RDA_DRIVER", "com.example.Driver");
        List<String[]> partsList = DbConnectionCache.parseEnvForDbSourceInstallation(env);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals("com.example.Driver", partsList.get(0)[1]);
    }

    @Test
    public void parseEnvSkipsSourceWhenDriverCannotBeDerived() {
        Map<String, String> env = rdaEnv("jdbc:whatever://db/x", "gregor_reader", "secret");
        Assert.assertTrue(DbConnectionCache.parseEnvForDbSourceInstallation(env).isEmpty());
    }
}