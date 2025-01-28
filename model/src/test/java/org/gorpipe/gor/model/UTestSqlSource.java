package org.gorpipe.gor.model;

import com.nextcode.gor.driver.utils.DatabaseHelper;
import gorsat.TestUtils;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSecurityException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;

public class UTestSqlSource {

    private static String[] paths;

    // Create a workdir rule
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @BeforeClass
    public static void setup() throws IOException, ClassNotFoundException, SQLException {
        paths = DatabaseHelper.createRdaDatabase();
        System.setProperty("gor.db.credentials", paths[2]);
        System.setProperty("gor.sql.credentials", paths[2]);
        DbConnection.initInConsoleApp();
    }

    @Test
    public void testThatSQLSourceIsCreateFromLink_Link_Secured() throws IOException {
        // Create a link to a file in the workdir
        // Containing the sql query
        var linkFile = workDir.newFile("sqlquery.sql.link");
        Files.writeString(linkFile.toPath(), "sql://select * from rda.v_variant_annotations where project_id = #{project-id}");

        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(workDir.getRoot().getAbsolutePath(), securityContext,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        var data = reader.readAll("sqlquery.sql.link");
        Assert.assertEquals(6, data.length);
    }

    @Test
    public void testThatSQLSourceIsCreateFromLink_NoLink_Secured()  {
        // Create a link to a file in the workdir
        // Containing the sql query
        var query = "sql://select * from rda.v_variant_annotations where project_id = #{project-id}";

        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(workDir.getRoot().getAbsolutePath(), securityContext,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        Assert.assertThrows(GorSecurityException.class, () -> reader.readAll(query));
    }

    @Test
    public void testReadingDbDataDirectMultipleSources_Secured() throws Exception {
        String sqlqueryRda = "sql://select * from rda.v_variant_annotations";
        String sqlqueryAvas = "sql://avas:select * from avas.v_variant_annotations";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        var linkFile1 = workDir.newFile("query1.link");
        Files.writeString(linkFile1.toPath(), sqlqueryRda);

        var linkFile2 = workDir.newFile("query2.link");
        Files.writeString(linkFile2.toPath(), sqlqueryAvas);

        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(".", securityContext,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        DbConnection.userConnections.install(new DbConnection("rda", "jdbc:derby:" + paths[1], "rda", "beta3"));

        // Create avas database
        var avasPaths = DatabaseHelper.createAvasDatabase();
        DbConnection.userConnections.install(new DbConnection("avas", "jdbc:derby:" + avasPaths[1], "avas", "beta3"));

        // 1.  Fails, i.e. it succeeds when it should not.
        var dataRda = reader.readAll(linkFile1.getAbsolutePath());
        var dataAvas = reader.readAll(linkFile2.getAbsolutePath());

        Assert.assertTrue(dataRda[1].contains("rda1"));
        Assert.assertTrue(dataAvas[1].contains("avas1"));
    }

    @Test
    public void testReadingDbDataWithProjectAndOrgScope_Secured() throws Exception {
        String sqlqueryWithScopeAndOrg = "sql://select * from rda.v_variant_annotations where project_id = #{project-id} and organization_id = #{organization-id}";
        String securityContext = "dbscope=project_id#int#10005,organization_id#int#1|||extrastuff=other";

        var linkFile = workDir.newFile("query.link");
        Files.writeString(linkFile.toPath(), sqlqueryWithScopeAndOrg);

        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(".", securityContext,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        var paths = DatabaseHelper.createRdaDatabaseWithOrg();
        DbConnection.userConnections.install(new DbConnection("rda", "jdbc:derby:" + paths[1], "rda", "beta3"));


        var data = reader.readAll(linkFile.getAbsolutePath());

        Assert.assertEquals(4, data.length);
    }

    @Test
    public void testReadingDbDataThroughLinkFile_Secured() throws IOException {
        String sqlqueryWithScope = "//db:select * from rda.v_variant_annotations variant_annotations\nwhere variant_annotations.project_id = #{project-id}\norder by chromo, pos desc\n";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        // Write link file
        var linkFile = workDir.newFile("query.link");
        Files.writeString(linkFile.toPath(), sqlqueryWithScope);

        // Test reading the link file
        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(".", securityContext,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        var data = reader.readAll(linkFile.getAbsolutePath());
        Assert.assertEquals(6, data.length);
    }

    @Test
    public void testReadingSqlDataWithInvalidReplacement_Secured() throws IOException {
        String sqlqueryWithScope = "//db:select * from rda.v_variant_annotations variant_annotations\nwhere variant_annotations.project_id = #{projects-id}\norder by chromo, pos desc\n";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        // Write link file
        var linkFile = workDir.newFile("query.link");
        Files.writeString(linkFile.toPath(), sqlqueryWithScope);

        // Test reading the link file
        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(".", securityContext,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        var e = Assert.assertThrows(GorResourceException.class, () -> reader.readAll(linkFile.getAbsolutePath()));
        Assert.assertTrue(e.getMessage().contains("sql query: projects-id"));
    }

    @Test
    public void testReadingDbDataThroughLinkFile_Unsecured() throws IOException {
        String sqlqueryWithScope = "//db:select * from rda.v_variant_annotations variant_annotations\nwhere variant_annotations.project_id = #{project-id}\norder by chromo, pos desc\n";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        // Write link file
        var linkFile = workDir.newFile("query.link");
        Files.writeString(linkFile.toPath(), sqlqueryWithScope);

        // Test reading the link file
        var reader = new DriverBackedFileReader(securityContext, ".");

        var data = reader.readAll(linkFile.getAbsolutePath());
        Assert.assertEquals(6, data.length);
    }

    @Test
    public void testReadingDbDataThroughQuery_Unsecured() throws IOException {
        String sqlqueryWithScope = "//db:select * from rda.v_variant_annotations variant_annotations\nwhere variant_annotations.project_id = #{project-id}\norder by chromo, pos desc\n";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        // Test reading the link file
        var reader = new DriverBackedFileReader(securityContext, ".");

        var data = reader.readAll(sqlqueryWithScope);
        Assert.assertEquals(6, data.length);
    }

    @Test
    public void testThatCorrectHeaderIsShownWithWildCard() {
        String query = "nor 'sql://select * from variant_annotations where project_id = #{project-id}'";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        var result = TestUtils.runGorPipe(query, false, securityContext).split("\n");

        Assert.assertEquals("chromnor\tposnor\tproject_id\tchromo\tpos\tpn\tfoo\tcomment", result[0].toLowerCase());
    }

    @Test
    public void testThatCorrectHeaderIsShownWithList() {
        String query = "nor 'sql://select chromo,pos,comment from variant_annotations where project_id = #{project-id}'";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        var result = TestUtils.runGorPipe(query, false, securityContext).split("\n");

        Assert.assertEquals("chromnor\tposnor\tchromo\tpos\tcomment", result[0].toLowerCase());
    }

    @Test
    public void testThatCorrectHandlingOfDistinct() {
        String query = "nor 'sql://select distinct chromo,pos,pn,foo,comment from rda.variant_annotations where project_id = #{project-id}'";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        var result = TestUtils.runGorPipe(query, false, securityContext).split("\n");

        Assert.assertEquals("chromnor\tposnor\tchromo\tpos\tpn\tfoo\tcomment", result[0].toLowerCase());
    }

    @Test
    public void testThatCorrectHandlingOfColumns() {
        String query = "nor 'sql://select chromo,pos,pn,foo,comment from variant_annotations where project_id = #{project-id}'";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        var result = TestUtils.runGorPipe(query, false, securityContext).split("\n");

        Assert.assertEquals("chromnor\tposnor\tchromo\tpos\tpn\tfoo\tcomment", result[0].toLowerCase());
    }

    @Test
    public void testReadingWithMacroreplacement() throws IOException {
        // Create whitelist file
        var whitelistFile = workDir.newFile("whitelist.txt");
        Files.writeString(whitelistFile.toPath(), "variant_annotations -c [sql select * from v_variant_annotations where project_id = '#{projectid}' #(F:and pn in (filter)) order by Chromo,Pos]");

        String query = "variant_annotations -f 'GDX_12000001_10'";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        TestUtils.runGorPipeCountWithWhitelist(query, whitelistFile.toPath(), securityContext);
    }
}
