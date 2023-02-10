package org.gorpipe.util;

public class Strings {

    /**
     * @param s string to check
     * @return returns true if the String is null or blank after trimming, otherwise returns false.
     */
    public static boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * @param s string to check
     * @return returns true if the String is null or empty after trimming, otherwise returns false.
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * @param s string to check
     * @return returns null if the String is null or empty after trimming, otherwise returns same String.
     */
    public static String blankNull(String s) {
        if (s != null && s.isBlank()) {
            return null;
        }
        return s;
    }

}