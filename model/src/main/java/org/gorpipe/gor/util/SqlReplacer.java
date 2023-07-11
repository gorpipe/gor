package org.gorpipe.gor.util;

import java.util.List;

public class SqlReplacer {
    final static String REGEX = "#\\{(\\S+?)\\}";

    public static List<String> replacementList(String content) {
        var matcher = java.util.regex.Pattern.compile(REGEX).matcher(content);

        // Collect keys and throw exception if there are no constants for all keys.
        var keys = new java.util.ArrayList<String>();

        while (matcher.find()) {
            keys.add(matcher.group(1));
        }

        return keys;
    }

    public static String replaceWithSqlParameter(String sql) {
        var matcher = java.util.regex.Pattern.compile(REGEX).matcher(sql);
        return matcher.replaceAll(matchResult -> "?");
    }
}
