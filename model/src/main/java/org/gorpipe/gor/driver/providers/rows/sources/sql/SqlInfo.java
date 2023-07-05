package org.gorpipe.gor.driver.providers.rows.sources.sql;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.util.StringUtil;

import java.util.ArrayList;

public record SqlInfo(String[] columns, String table, String database, String statement) {

    private static final String SELECT = "select ";
    private static final String FROM = "from ";
    private static final String AS = " as ";

    public boolean hasHeader() {
        return columns.length > 1 && !columns[0].equals("*");
    }

    public static SqlInfo parse(String sql) {

        String databaseName = null;

        final int databaseIdx = sql.indexOf(":");

        if (databaseIdx > 0) {
            databaseName = sql.substring(0, databaseIdx);
            sql = sql.substring(databaseIdx + 1);
        }

        sql = sql.toLowerCase().replace("\n", " ").replace("\r", " ");
        final int idxSelect = sql.indexOf(SELECT);
        final int idxFrom = sql.indexOf(FROM);

        if (idxSelect < 0 || idxFrom < 0) { // Must find columns
            throw new GorResourceException("Invalid sql query, must include SELECT and FROM", sql);
        }

        final ArrayList<String> fields = StringUtil.split(sql, idxSelect + SELECT.length(), idxFrom, ',');
        var columns = new ArrayList<String>();
        for (String f : fields) {
            final int idxAs = f.indexOf(AS);
            if (idxAs > 0) {
                columns.add(f.substring(idxAs + AS.length()).trim());
            } else {
                final int idxPoint = f.indexOf('.');
                columns.add(f.substring(idxPoint > 0 ? idxPoint + 1 : 0).trim());
            }
        }

        var tableName = sql.substring(idxFrom + FROM.length()).trim();

        return new SqlInfo(columns.toArray(String[]::new), tableName, databaseName, sql);
    }
}
