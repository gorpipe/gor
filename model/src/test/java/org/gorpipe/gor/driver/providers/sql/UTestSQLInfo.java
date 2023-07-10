package org.gorpipe.gor.driver.providers.sql;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.providers.rows.sources.sql.SqlInfo;
import org.junit.Assert;
import org.junit.Test;

public class UTestSQLInfo {
    @Test
    public void parseSimpleSQLStatement() {
        var sql = "select * from table";
        var sqlInfo = SqlInfo.parse(sql);
        Assert.assertEquals(1, sqlInfo.columns().length);
        Assert.assertEquals("*", sqlInfo.columns()[0]);
        Assert.assertEquals(1, sqlInfo.tables().length);
        Assert.assertEquals("table", sqlInfo.tables()[0]);
        Assert.assertNull(sqlInfo.database());
    }

    @Test
    public void parseMultipleColumnsSQLStatement() {
        var sql = "select a,b,c,d from table";
        var sqlInfo = SqlInfo.parse(sql);
        Assert.assertEquals(4, sqlInfo.columns().length);
        Assert.assertEquals("a", sqlInfo.columns()[0]);
        Assert.assertEquals("b", sqlInfo.columns()[1]);
        Assert.assertEquals("c", sqlInfo.columns()[2]);
        Assert.assertEquals("d", sqlInfo.columns()[3]);
        Assert.assertEquals(1, sqlInfo.tables().length);
        Assert.assertEquals("table", sqlInfo.tables()[0]);
        Assert.assertNull(sqlInfo.database());
    }

    @Test
    public void parseColumnsWitAsSQLStatement() {
        var sql = "select a,b as foo,c from table";
        var sqlInfo = SqlInfo.parse(sql);
        Assert.assertEquals(3, sqlInfo.columns().length);
        Assert.assertEquals("a", sqlInfo.columns()[0]);
        Assert.assertEquals("foo", sqlInfo.columns()[1]);
        Assert.assertEquals("c", sqlInfo.columns()[2]);
        Assert.assertEquals(1, sqlInfo.tables().length);
        Assert.assertEquals("table", sqlInfo.tables()[0]);
        Assert.assertNull(sqlInfo.database());
    }

    @Test
    public void parseTableAndDatabaseSQLStatement() {
        var sql = "db:select * from foo.table";
        var sqlInfo = SqlInfo.parse(sql);
        Assert.assertEquals(1, sqlInfo.columns().length);
        Assert.assertEquals("*", sqlInfo.columns()[0]);
        Assert.assertEquals(1, sqlInfo.tables().length);
        Assert.assertEquals("foo.table", sqlInfo.tables()[0]);
        Assert.assertEquals("db", sqlInfo.database());
    }

    public void parseTableAndMultipleDatabasesSQLStatement() {
        var sql = "db:select * from foo.table bar.table";
        var sqlInfo = SqlInfo.parse(sql);
        Assert.assertEquals(1, sqlInfo.columns().length);
        Assert.assertEquals("*", sqlInfo.columns()[0]);
        Assert.assertEquals(2, sqlInfo.tables().length);
        Assert.assertEquals("foo.table", sqlInfo.tables()[0]);
        Assert.assertEquals("bar.table", sqlInfo.tables()[1]);
        Assert.assertEquals("db", sqlInfo.database());
    }

    @Test
    public void parseTableAndDatabaseSQLStatementWithWhere() {
        var sql = "db:select * from foo.table where foo=#{bar}";
        var sqlInfo = SqlInfo.parse(sql);
        Assert.assertEquals(1, sqlInfo.columns().length);
        Assert.assertEquals("*", sqlInfo.columns()[0]);
        Assert.assertEquals(1, sqlInfo.tables().length);
        Assert.assertEquals("foo.table", sqlInfo.tables()[0]);
        Assert.assertEquals("db", sqlInfo.database());
    }

    @Test
    public void parseBadSQLStatement() {
        var sql = "select *";
        Assert.assertThrows(GorResourceException.class, () -> SqlInfo.parse(sql)); ;
        var sql2 = "select a,b fro foo";
        Assert.assertThrows(GorResourceException.class, () -> SqlInfo.parse(sql2)); ;
    }

}
