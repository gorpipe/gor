package org.gorpipe.util;

public class StringUtils {

    /**
     * @param s
     * @return returns null if the String is null or empty after trimming. Otherwise returns same String.
     */
    public static String blankNull(String s) {
        if (s != null && s.trim().isEmpty()) {
            return null;
        }
        return s;
    }

}