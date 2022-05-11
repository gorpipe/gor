package org.gorpipe.util;

public class NumericUtils {
    /**
     * @param s
     * @return returns true if the String is an int number.
     */
    public static boolean isStringInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * @param s
     * @return returns true if the String is a long number.
     */
    public static boolean isStringLong(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

}