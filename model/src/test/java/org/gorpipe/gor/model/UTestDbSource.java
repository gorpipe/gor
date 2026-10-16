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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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

    private static List<DbCredentials> rdaCreds(String url, String username, String password) {
        return List.of(new DbCredentials("rda", url, username, password));
    }

    private static List<DbCredentials> rdaCreds(String url, String username, String password, String driver) {
        return List.of(new DbCredentials("rda", url, username, password, driver));
    }

    @Test
    public void credentialsBuildPartsInFileParserShape() {
        List<DbCredentials> creds = rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret");
        List<String[]> partsList = DbConnectionCache.toPartsForInstallation(creds);
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
    public void credentialsOmitPasswordFieldWhenPasswordUnset() {
        List<DbCredentials> creds = rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", null);
        List<String[]> partsList = DbConnectionCache.toPartsForInstallation(creds);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals(4, partsList.get(0).length);
    }

    @Test
    public void credentialsTreatBlankPasswordAsUnset() {
        List<DbCredentials> creds = rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "   ");
        List<String[]> partsList = DbConnectionCache.toPartsForInstallation(creds);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals("blank password should be omitted, not installed as an empty password",
                4, partsList.get(0).length);
    }

    @Test
    public void noCredentialsProducesNoParts() {
        Assert.assertTrue(DbConnectionCache.toPartsForInstallation(List.of()).isEmpty());
    }

    @Test
    public void credentialsSkippedWhenUsernameMissing() {
        List<DbCredentials> creds = rdaCreds("jdbc:postgresql://db:5432/csa", null, "secret");
        Assert.assertTrue(DbConnectionCache.toPartsForInstallation(creds).isEmpty());
    }

    @Test
    public void credentialsSkippedWhenUrlMissing() {
        List<DbCredentials> creds = rdaCreds(null, "gregor_reader", "secret");
        Assert.assertTrue(DbConnectionCache.toPartsForInstallation(creds).isEmpty());
    }

    @Test
    public void credentialsTreatBlankValuesAsUnset() {
        List<DbCredentials> creds = rdaCreds("jdbc:postgresql://db:5432/csa", "   ", "secret");
        Assert.assertTrue(DbConnectionCache.toPartsForInstallation(creds).isEmpty());
    }

    @Test
    public void credentialsDeriveOracleDriverFromUrlPrefix() {
        List<DbCredentials> creds = rdaCreds("jdbc:oracle:thin:@db:1521:XE", "gregor_reader", "secret");
        List<String[]> partsList = DbConnectionCache.toPartsForInstallation(creds);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals("oracle.jdbc.driver.OracleDriver", partsList.get(0)[1]);
    }

    @Test
    public void credentialsHonourExplicitDriverOverride() {
        List<DbCredentials> creds = rdaCreds("jdbc:whatever://db/x", "gregor_reader", "secret", "com.example.Driver");
        List<String[]> partsList = DbConnectionCache.toPartsForInstallation(creds);
        Assert.assertEquals(1, partsList.size());
        Assert.assertEquals("com.example.Driver", partsList.get(0)[1]);
    }

    @Test
    public void credentialsSkippedWhenDriverCannotBeDerived() {
        List<DbCredentials> creds = rdaCreds("jdbc:whatever://db/x", "gregor_reader", "secret");
        Assert.assertTrue(DbConnectionCache.toPartsForInstallation(creds).isEmpty());
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private String writeCredentialsFile(String... lines) throws Exception {
        File file = tempFolder.newFile("gor.db.credentials");
        Files.write(file.toPath(), String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        return file.getAbsolutePath();
    }

    @Test
    public void suppliedCredentialsOnlyInstallRdaSource() {
        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources(rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret"));

        DbConnection rda = cache.lookup("rda");
        Assert.assertNotNull("rda source should be installed from the supplied credentials", rda);
        Assert.assertEquals("jdbc:postgresql://db:5432/csa", rda.url);
        Assert.assertEquals("gregor_reader", rda.user);
        Assert.assertEquals("secret", rda.pwd);
    }

    @Test
    public void blankSuppliedPasswordInstallsSourceWithNullPassword() {
        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources(rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "   "));

        DbConnection rda = cache.lookup("rda");
        Assert.assertNotNull("source should still install, only the password is unset", rda);
        Assert.assertNull("whitespace password must not reach the connection as a real password", rda.pwd);
    }

    @Test
    public void partialCredentialsSkippedWithoutThrowing() {
        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources(rdaCreds("jdbc:postgresql://db:5432/csa", null, "secret"));

        Assert.assertNull(cache.lookup("rda"));
    }

    @Test
    public void suppliedCredentialsReplaceAnythingAlreadyInstalled() throws Exception {
        String credpath = writeCredentialsFile(
                "name\tdriver\turl\tuser\tpwd",
                "aux\torg.postgresql.Driver\tjdbc:postgresql://auxhost:5432/aux\tauxuser\tauxpwd");

        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources(credpath);
        Assert.assertNotNull(cache.lookup("aux"));

        // Each initializer clears the cache first; combining file and credentials is its own overload.
        cache.initializeDbSources(rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret"));
        Assert.assertNotNull(cache.lookup("rda"));
        Assert.assertNull("file sources should not survive a credentials load", cache.lookup("aux"));
    }

    @Test
    public void fileInstallsEveryRow() throws Exception {
        String credpath = writeCredentialsFile(
                "name\tdriver\turl\tuser\tpwd",
                "rda\torg.postgresql.Driver\tjdbc:postgresql://filehost:5432/csa\tfileuser\tfilepwd",
                "aux\torg.postgresql.Driver\tjdbc:postgresql://auxhost:5432/aux\tauxuser\tauxpwd");

        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources(credpath);

        Assert.assertEquals("fileuser", cache.lookup("rda").user);
        Assert.assertEquals("auxuser", cache.lookup("aux").user);
    }

    @Test(expected = FileNotFoundException.class)
    public void missingFileThrows() throws Exception {
        String missing = new File(tempFolder.getRoot(), "does-not-exist.credentials").getAbsolutePath();
        new DbConnectionCache().initializeDbSources(missing);
    }

    @Test
    public void noCredentialPathLeavesCacheEmpty() throws Exception {
        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources((String) null);

        Assert.assertNull(cache.lookup("rda"));
    }

    @Test
    public void fileAndSuppliedCredentialsCombine() throws Exception {
        String credpath = writeCredentialsFile(
                "name\tdriver\turl\tuser\tpwd",
                "aux\torg.postgresql.Driver\tjdbc:postgresql://auxhost:5432/aux\tauxuser\tauxpwd");

        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources(credpath, rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret"));

        Assert.assertEquals("auxuser", cache.lookup("aux").user);
        Assert.assertEquals("gregor_reader", cache.lookup("rda").user);
    }

    @Test
    public void suppliedCredentialsReplaceSameNamedFileRow() throws Exception {
        String credpath = writeCredentialsFile(
                "name\tdriver\turl\tuser\tpwd",
                "RDA\torg.postgresql.Driver\tjdbc:postgresql://filehost:5432/csa\tfileuser\tstale");

        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources(credpath, rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret"));

        DbConnection rda = cache.lookup("rda");
        Assert.assertEquals("gregor_reader", rda.user);
        Assert.assertEquals("secret", rda.pwd);
    }

    @Test
    public void incompleteSuppliedCredentialsKeepTheFileRow() throws Exception {
        String credpath = writeCredentialsFile(
                "name\tdriver\turl\tuser\tpwd",
                "rda\torg.postgresql.Driver\tjdbc:postgresql://filehost:5432/csa\tfileuser\tfilepwd");

        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources(credpath, rdaCreds("jdbc:postgresql://db:5432/csa", null, "secret"));

        Assert.assertEquals("fileuser", cache.lookup("rda").user);
    }

    @Test
    public void combinedLoadWithMissingFileThrowsAndKeepsTheCache() throws Exception {
        DbConnectionCache cache = new DbConnectionCache();
        cache.initializeDbSources(rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret"));
        String missing = new File(tempFolder.getRoot(), "does-not-exist.credentials").getAbsolutePath();

        Assert.assertThrows(FileNotFoundException.class, () -> cache.initializeDbSources(missing, List.of()));
        Assert.assertNotNull("a failed load should not clear what was installed", cache.lookup("rda"));
    }

    private void withServerCaches(String credpath, Runnable test) {
        String previous = System.getProperty("gor.db.credentials");
        try {
            if (credpath == null) {
                System.clearProperty("gor.db.credentials");
            } else {
                System.setProperty("gor.db.credentials", credpath);
            }
            test.run();
        } finally {
            if (previous == null) {
                System.clearProperty("gor.db.credentials");
            } else {
                System.setProperty("gor.db.credentials", previous);
            }
            DbConnection.systemConnections.clearDbSources();
            DbConnection.userConnections.clearDbSources();
        }
    }

    private static void initInServer(List<DbCredentials> systemCredentials) {
        try {
            DbConnection.initInServer(systemCredentials);
        } catch (Exception e) {
            throw new AssertionError("initInServer should log and continue, not throw", e);
        }
    }

    @Test
    public void initInServerGivesUserConnectionsTheFileAndTheSuppliedCredentials() throws Exception {
        // SQL/NORSQL/GORSQL and sql:// default to the rda source in the user connections, so a host that
        // supplies rda instead of keeping it in the file must have it installed there too.
        String credpath = writeCredentialsFile(
                "name\tdriver\turl\tuser\tpwd",
                "aux\torg.postgresql.Driver\tjdbc:postgresql://auxhost:5432/aux\tauxuser\tauxpwd");

        withServerCaches(credpath, () -> {
            initInServer(rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret"));

            Assert.assertEquals("gregor_reader", DbConnection.systemConnections.lookup("rda").user);
            Assert.assertNull("the file does not feed the system connections", DbConnection.systemConnections.lookup("aux"));
            Assert.assertEquals("gregor_reader", DbConnection.userConnections.lookup("rda").user);
            Assert.assertEquals("auxuser", DbConnection.userConnections.lookup("aux").user);
        });
    }

    @Test
    public void initInServerWithMissingFileStillInstallsTheSuppliedCredentials() {
        String missing = new File(tempFolder.getRoot(), "does-not-exist.credentials").getAbsolutePath();

        withServerCaches(missing, () -> {
            initInServer(rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret"));

            Assert.assertNotNull(DbConnection.systemConnections.lookup("rda"));
            Assert.assertNotNull(DbConnection.userConnections.lookup("rda"));
        });
    }

    @Test
    public void initInServerWithoutCredentialPathUsesTheSuppliedCredentials() {
        withServerCaches(null, () -> {
            initInServer(rdaCreds("jdbc:postgresql://db:5432/csa", "gregor_reader", "secret"));

            Assert.assertNotNull(DbConnection.userConnections.lookup("rda"));
        });
    }
}
