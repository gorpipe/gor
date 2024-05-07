package org.gorpipe.gor.util;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.util.Pair;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Should be synced with CommandSubstitutions.
public class SqlReplacer {

    final static String REGEX = "#\\{(\\S+?)\\}";

    public static final String KEY_PROJECT_ID = "project_id";
    public static final String KEY_DB_PROJECT_ID = "project-id";
    public static final String KEY_ORGANIZATION_ID = "organization_id";
    public static final String KEY_DB_ORGANIZATION_ID = "organization-id";
    public static final String KEY_REQUEST_ID = "request_id";

    public static final String KEY_CHROM = "chrom";
    public static final String KEY_BPSTART = "bpstart";
    public static final String KEY_BPSTOP = "bpstop";
    public static final String KEY_TAGS = "tags";
    public static final String KEY_DATABASE = "database";

    public static Object[] replacementList(String sql, final Map<String, Object>constants) {
        var matcher = java.util.regex.Pattern.compile(REGEX).matcher(sql);

        // Collect keys and throw exception if there are no constants for all keys.
        var values = new java.util.ArrayList<>();

        while (matcher.find()) {
            var key = matcher.group(1).toLowerCase();
            if (!constants.containsKey(key)) {
                throw new GorResourceException("Unexpected constant in sql query: " + key, null);
            }
            var value = constants.get(key);
            if (value != null && value instanceof Collection<?>) {
                ((Collection<?>) value).stream().forEach(values::add);
            } else {
                values.add(value);
            }
        }

        return values.toArray();
    }

    public static String replaceWithSqlParameter(String sql, final Map<String, Object> constants) {
        var matcher = java.util.regex.Pattern.compile(REGEX).matcher(sql);
        return matcher.replaceAll(matchResult -> {
            var key = matchResult.group(1).toLowerCase();
            if (!constants.containsKey(key)) {
                throw new IllegalArgumentException("Missing constant for key: " + matcher.group(1));
            }
            var value = constants.get(key);
            if (value != null && value instanceof Collection<?>) {
                return ((Collection<?>) value).stream().map(v -> "?").collect(Collectors.joining(","));
            } else {
                return "?";
            }
        });
    }

    /**
     * Replace query constants using the VARS list of usual suspects and populate a bind array with
     * the associated values for use in prepareStatement.
     *
     * @param sql
     * @param constants
     * @return
     */
    // TODO: Clean up this code.
    public static Pair<String, Object[]> replaceConstants(final String sql, final Map<String, Object> constants) {
        var replacements = SqlReplacer.replacementList(sql, constants);
        var newSql = SqlReplacer.replaceWithSqlParameter(sql, constants);
        return new Pair<>(newSql, replacements);
    }
}
