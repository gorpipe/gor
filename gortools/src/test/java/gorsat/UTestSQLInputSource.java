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

package gorsat;

import com.nextcode.gor.driver.utils.DatabaseHelper;
import org.gorpipe.gor.model.DbConnection;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by sigmar on 01/02/16.
 */
public class UTestSQLInputSource {

    private static String[] rdaPaths;
    private static String[] avasPaths;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @BeforeClass
    public static void initDb() throws IOException, ClassNotFoundException, SQLException {
        rdaPaths = DatabaseHelper.createRdaDatabase();
        avasPaths = DatabaseHelper.createAvasDatabase();

        List<String> credFileLines = Files.readAllLines(Path.of(rdaPaths[2]));
        credFileLines.add(Files.readAllLines(Path.of(avasPaths[2])).get(1));
        File credFile = FileTestUtils.createTempFile(new File(rdaPaths[0]), "gor.sql.credentials",
                credFileLines.stream().collect(Collectors.joining("\n")));
        System.setProperty("gor.sql.credentials", credFile.getAbsolutePath());
        DbConnection.initInConsoleApp();
    }

    @Test
    public void testNorSql() {
        String sqlcmd = "norsql {select * from v_variant_annotations}";
        var result = TestUtils.runGorPipe(sqlcmd).split("\n");

        Assert.assertEquals(11, result.length);
        Assert.assertEquals("chromnor\tposnor\tproject_id\tchromo\tpos\tpn\tfoo\tcomment", result[0].toLowerCase());
        Assert.assertEquals("chrN\t0\t10004\tchr1\t0\tfoo1\trda1\tcomment1", result[1]);
    }

    @Test
    public void testGorSql() {
        String sqlcmd = "gorsql {select chromo, pos, pn from v_variant_annotations}";
        var result = TestUtils.runGorPipe(sqlcmd).split("\n");
        Assert.assertEquals(11, result.length);
        Assert.assertEquals("chromo\tpos\tpn", result[0].toLowerCase());
        Assert.assertEquals("chr1\t2\tfoo3", result[3]);
    }

    @Test
    public void testNorSqlWithSource() {
        String sqlcmd = "norsql -db avas {select * from v_variant_annotations}";
        var result = TestUtils.runGorPipe(sqlcmd);
        Assert.assertEquals(11, result.split("\n").length);
        Assert.assertEquals("chrN\t0\t10004\tchr1\t0\tfoo1\tavas1\tcomment1", result.split("\n")[1]);
    }

    @Test
    public void testGorSqlWithSource() {
        String sqlcmd = "gorsql -db avas {select chromo, pos, pn, foo  from v_variant_annotations}";
        var result = TestUtils.runGorPipe(sqlcmd).split("\n");
        Assert.assertEquals(11, result.length);
        Assert.assertEquals("chromo\tpos\tpn\tfoo", result[0].toLowerCase());
        Assert.assertEquals("chr1\t2\tfoo3\tavas3", result[3]);
    }

    @Test
    public void testNorSqlSourceWithDbProjectId() {
        String sqlcmd = "norsql {select * from rda.v_variant_annotations where project_id = #{project-id}}";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";
        var result = TestUtils.runGorPipe(sqlcmd, false, securityContext);
        Assert.assertEquals(6, result.split("\n").length);
    }

    @Test
    public void testNorSqlSourceWithProjectId() {
        System.setProperty("gor.query.const.replacements", "true");
        String sqlcmd = "norsql {select project_id,chromo,pos,pn,comment, #{project_id} x from rda.v_variant_annotations where project_id = '#{project_id}'}";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";
        var result = TestUtils.runGorPipe(sqlcmd, false, securityContext);
        Assert.assertEquals(6, result.split("\n").length);
    }

    @Test
    public void testNorSqlInNested() throws IOException, ClassNotFoundException {
        String sqlcmd = "nor <(norsql {select * from v_variant_annotations})";
        Assert.assertEquals(10, TestUtils.runGorPipeCount(sqlcmd));
    }

    @Test
    public void testGorSqlWithSeek() {
        String sqlcmd = "gorsql -p chr1 {select chromo,pos, PN from v_variant_annotations where CHROMO=#{CHROM} order by chromo,pos}";
        Assert.assertEquals(10, TestUtils.runGorPipeCount(sqlcmd));
    }

    @Test
    public void testGorSqlWithSeekEmpty(){
        String sqlcmd = "gorsql -p chr2 {select chromo,pos, PN from v_variant_annotations where CHROMO=#{CHROM} order by chromo,pos}";
        Assert.assertEquals(0, TestUtils.runGorPipeCount(sqlcmd));
    }

    @Test
    public void testGorSqlWithSeekChromAndPos() {
        String sqlcmd = "gorsql -p chr1:3- {select chromo,pos, PN from v_variant_annotations where CHROMO=#{CHROM} and pos >= #{BPSTART} order by chromo,pos}";
        Assert.assertEquals(4, TestUtils.runGorPipeCount(sqlcmd));
    }

    @Test
    public void testGorSqlNestedWithSeek() {
        String gorcmd = "gor -p chr1:3- <(gorsql {select chromo,pos, PN from v_variant_annotations where CHROMO=#{CHROM} and pos >= #{BPSTART} order by chromo,pos})";
        Assert.assertEquals(4, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testNorSqlWithFilter() {
        String sqlcmd = "norsql -f foo1,bar2 {select * from v_variant_annotations where pn in (#{tags}) order by chromo,pos}";
        Assert.assertEquals(2, TestUtils.runGorPipeCount(sqlcmd));
    }

    @Test
    public void testGorSqlWithFilter() {
        String sqlcmd = "gorsql -f foo1,bar2 {select chromo,pos, PN from v_variant_annotations where pn in (#{tags}) order by chromo,pos}";
        Assert.assertEquals(2, TestUtils.runGorPipeCount(sqlcmd));
    }

    @Test
    public void testNorSqlNestedWithInsideFilter() {
        String gorcmd = "nor <(norsql -f foo1,bar2 {select * from v_variant_annotations where pn in (#{tags}) order by chromo,pos})";
        Assert.assertEquals(2, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testGorSqlNestedWithInsideFilter() {
        String gorcmd = "gor <(gorsql -f foo1,bar2 {select chromo,pos,PN from v_variant_annotations where pn in (#{tags}) order by chromo,pos})";
        Assert.assertEquals(2, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testNorSqlNestedWithOutsideFilter() {
        String gorcmd = "nor -f foo1,bar2 <(norsql {select * from v_variant_annotations where pn in (#{tags}) order by chromo,pos})";
        Assert.assertEquals(0, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testGorSqlNestedWithOutsideFilter() {
        String gorcmd = "gor -f foo1,bar2 <(gorsql {select chromo,pos,PN from v_variant_annotations where pn in (#{tags}) order by chromo,pos})";
        Assert.assertEquals(0, TestUtils.runGorPipeCount(gorcmd));
    }

    @Test
    public void testNorSqlInCreate() {
        String sqlcmd = "create xxx = norsql { select chromo,pos,pn from v_variant_annotations } | signature -timeres 1;\n" +
                "nor [xxx] ";
        var result = TestUtils.runGorPipe(sqlcmd).split("\n");

        Assert.assertEquals(11, result.length);
        Assert.assertEquals("chromnor\tposnor\tchromo\tpos\tpn", result[0].toLowerCase());
        Assert.assertEquals("chrN\t0\tchr1\t0\tfoo1", result[1]);
    }
}
