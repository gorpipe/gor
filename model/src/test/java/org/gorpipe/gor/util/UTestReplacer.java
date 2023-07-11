package org.gorpipe.gor.util;

import org.junit.Test;

public class UTestReplacer {
    @Test
    public void testReplace() {
        var content = "select * from #{table} where #{column} = 1";
        var result = SqlReplacer.replaceWithSqlParameter(content);
        assert result.equals("select * from ? where ? = 1");
    }

    @Test
    public void testReplacementList() {
        var content = "select * from #{table} where #{column} = 1";
        var result = SqlReplacer.replacementList(content);
        assert result.size() == 2;
        assert result.get(0).equals("table");
        assert result.get(1).equals("column");
    }
}
