package org.gorpipe.util;

import java.util.regex.Pattern;

public class Strings {

    // One '@' with a non-empty local part and domain, no whitespace or '/' (the value may end up in a URL path).
    private static final Pattern EMAIL = Pattern.compile("[^\\s@/]+@[^\\s@/]+");

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

    /**
     * Loose check: a single '@' between a non-empty local part and domain, without whitespace or '/'.
     *
     * @param s string to check
     * @return returns true if the String looks like an email address, otherwise returns false.
     */
    public static boolean isEmail(String s) {
        return s != null && EMAIL.matcher(s).matches();
    }

}
