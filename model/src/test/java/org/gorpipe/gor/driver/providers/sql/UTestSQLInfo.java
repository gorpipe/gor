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
        assert sqlInfo.columns().length == 1;
        assert sqlInfo.columns()[0].equals("*");
        assert sqlInfo.table().equals("table");
        assert sqlInfo.database() == null;
    }

    @Test
    public void parseMultipleColumnsSQLStatement() {
        var sql = "select a,b,c,d from table";
        var sqlInfo = SqlInfo.parse(sql);
        assert sqlInfo.columns().length == 4;
        assert sqlInfo.columns()[0].equals("a");
        assert sqlInfo.columns()[1].equals("b");
        assert sqlInfo.columns()[2].equals("c");
        assert sqlInfo.columns()[3].equals("d");
        assert sqlInfo.table().equals("table");
        assert sqlInfo.database() == null;
    }

    @Test
    public void parseColumnsWitAsSQLStatement() {
        var sql = "select a,b as foo,c from table";
        var sqlInfo = SqlInfo.parse(sql);
        assert sqlInfo.columns().length == 3;
        assert sqlInfo.columns()[0].equals("a");
        assert sqlInfo.columns()[1].equals("foo");
        assert sqlInfo.columns()[2].equals("c");
        assert sqlInfo.table().equals("table");
        assert sqlInfo.database() == null;
    }

    @Test
    public void parseTableAndDatabaseSQLStatement() {
        var sql = "db:select * from foo.table";
        var sqlInfo = SqlInfo.parse(sql);
        assert sqlInfo.columns().length == 1;
        assert sqlInfo.columns()[0].equals("*");
        assert sqlInfo.table().equals("foo.table");
        assert sqlInfo.database().equals("db");
    }

    @Test
    public void parseBadSQLStatement() {
        var sql = "select *";
        Assert.assertThrows(GorResourceException.class, () -> SqlInfo.parse(sql)); ;
        var sql2 = "select a,b fro foo";
        Assert.assertThrows(GorResourceException.class, () -> SqlInfo.parse(sql2)); ;
    }

}
