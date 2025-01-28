package org.gorpipe.gor.driver.providers.rows.sources.sql;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.util.StringUtil;

import java.util.ArrayList;

public record SqlInfo(String[] columns, String[] tables, String database, String statement) {

    private static final String SELECT = "select ";
    private static final String DISTINCT = "distinct ";
    private static final String FROM = "from ";
    private static final String WHERE = "where ";
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
        final int idxDistinct = sql.indexOf(DISTINCT);
        final int idxFrom = sql.indexOf(FROM);
        int idxWhere = sql.indexOf(WHERE);

        if (idxWhere < 0) {
            idxWhere = sql.length();
        }

        if (idxSelect < 0 || idxFrom < 0) { // Must find columns
            throw new GorResourceException("Invalid sql query, must include SELECT and FROM", sql);
        }

        final ArrayList<String> fields = StringUtil.split(sql,  (idxDistinct < 0 ? idxSelect + SELECT.length() : idxDistinct + DISTINCT.length()), idxFrom, ',');
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

        var tables = sql.substring(idxFrom + FROM.length(), idxWhere).trim().split(" ");
        var tableNames = new ArrayList<String>();
        for (String t : tables) {
            if (!t.isBlank()) {
                tableNames.add(t.trim());
            }
        }

        return new SqlInfo(columns.toArray(String[]::new), tableNames.toArray(String[]::new), databaseName, sql);
    }
}
