package org.gorpipe.util.http.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpUtils {
    public static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new LinkedHashMap<>();

        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }

        return result;
    }

    public static String constructQuery(Map<String,String> parameters) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,String> entry : parameters.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }
}
