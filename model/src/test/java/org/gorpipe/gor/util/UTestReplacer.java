package org.gorpipe.gor.util;

import org.junit.Test;

import java.util.Map;

public class UTestReplacer {
    @Test
    public void testReplace() {
        var content = "select * from #{table} where #{COLUMN} = 1";
        var result = SqlReplacer.replaceWithSqlParameter(content, Map.of("table", "table_val", "column", "column_val"));
        assert result.equals("select * from ? where ? = 1");
    }

    @Test
    public void testReplacementList() {
        var content = "select * from #{table} where #{COLUMN} = 1";
        var result = SqlReplacer.replacementList(content, Map.of("table", "table_val", "column", "column_val"));
        assert result.length == 2;
        assert result[0].equals("table_val");
        assert result[1].equals("column_val");
    }
}