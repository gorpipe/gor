/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.util.string;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of static string utility routines
 */
public class StringUtil {

    private StringUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Extract a substring from the text, that starts after the prefix and ends prior to the suffix	(wich is
     * the rest of the text string if the suffix is not found).
     *
     * @param text   The text to extract substring from
     * @param prefix The prefix text
     * @param suffix The suffix text
     * @return The substring
     */
    public static String substring(String text, String prefix, String suffix) {
        if (text != null) {
            final int prefixIdx = text.indexOf(prefix);
            if (prefixIdx >= 0) {
                final int suffixIdx = text.indexOf(suffix, prefixIdx);
                return text.substring(prefixIdx + prefix.length(), suffixIdx > 0 ? suffixIdx : text.length());
            }
        }

        return null;
    }

    /**
     * @param text The chars to look through
     * @param ch   The char to find
     * @return First index of the specified character after the specified startIndex in the provided char array
     */
    public static int indexOf(char[] text, char ch) {
        return indexOf(text, ch, 0);
    }

    /**
     * @param text       The chars to look through
     * @param ch         The char to find
     * @param startIndex The first position to read from
     * @return First index of the specified character after the specified startIndex in the provided char array
     */
    public static int indexOf(char[] text, char ch, int startIndex) {
        final int len = text.length;
        for (int i = startIndex; i < len; i++) {
            if (text[i] == ch) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check if a string holds a valid integer value
     *
     * @param s the string to check
     * @return true if the string can be converted to a int
     */
    public static final boolean isValidInt(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException n) {
            return false;
        }
        return true;
    }

    /**
     * Check if a string contains only digits
     *
     * @param s the string to check
     * @return true if the characters in the string are all digits
     */
    public final static boolean isAllDigit(String s) {
        for (int i = 0; i < s.length(); i++)
            if (!Character.isDigit(s.charAt(i)))
                return false;

        return true;
    }

    /**
     * Convert the byte text into an integer
     *
     * @param seq   The CharSequence to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @param end   The end position in the array for the value, exclusive
     * @return The integer value
     */
    private static int toInt(CharSequence seq, int begin, int end) {
        int nr = 0;
        final boolean isNegative = end > begin && seq.charAt(begin) == '-';

        for (int p = isNegative ? begin + 1 : begin; p < end; p++) {

            final char ch = seq.charAt(p);
            if (ch < '0' || ch > '9') {
                break;
            }
            nr = 10 * nr + (ch - '0');
        }

        return isNegative ? -nr : nr;
    }

    /**
     * Compares two strings, with regards to whether they include numbers.
     * When used instead of s1.compareTo(s2) when ordering strings, "String 9" will precede "String 10".
     *
     * @param s1 First String to compare
     * @param s2 Second String to compare
     * @return A number > 0 if s1 is precedes s2, less than 0 if s2 precedes s1, 0 if they are equal
     */
    public final static int compareNumStrings(String s1, String s2) {
        int lesserLength = Math.min(s1.length(), s2.length());
        int index = 0;
        while (index < lesserLength - 1 && Character.toUpperCase(s1.charAt(index)) == Character.toUpperCase(s2.charAt(index))) {
            int s1Index = index + 1;
            while (s1Index < s1.length() && Character.isDigit(s1.charAt(s1Index)) && s1Index - index < 9) {
                s1Index++;
            }
            int s2Index = index + 1;
            while (s2Index < s2.length() && Character.isDigit(s2.charAt(s2Index)) && s2Index - index < 9) {
                s2Index++;
            }
            if (s1Index - index > 1 || s2Index - index > 1) {
                final int s1Number = s1Index - index > 1 ? StringUtil.toInt(s1, index + 1, s1Index) : 0;
                final int s2Number = s2Index - index > 1 ? StringUtil.toInt(s2, index + 1, s2Index) : 0;

                if (s1Number != s2Number) {
                    return s1Number < s2Number ? -1 : 1;
                }
                index = Math.min(s1Index, s2Index);
            } else {
                index++;
            }
        }
        return s1.compareToIgnoreCase(s2);
    }

    /**
     * Query if the provided string is a valid double number
     * NOTE calling this method frequently is not optimal since it will using Double.parseDouble and catch exception is rather slow
     *
     * @param s The string to check
     * @return True if string is a valid double
     */
    public static final boolean isValidDouble(String s) {
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException n) {
            return false;
        }
        return true;
    }

    /**
     * Join a collection of strings (or toString() result of objects) using a
     * separator.
     *
     * @param separator Separator
     * @param items     Collection of items.
     * @return joined string.
     */
    public static String join(String separator, Iterable<?> items) {
        if (items == null) return null;
        StringBuilder result = new StringBuilder();
        join(separator, items, result);
        return result.toString();
    }

    /**
     * Join a collection of strings (or toString() result of objects) using a
     * seperator.
     *
     * @param seperator Separator
     * @param items     Collection of items.
     * @param result    Buffer to add joined string to.
     */
    public static void join(String seperator, Iterable<?> items, Appendable result) {
        try {
            if (items == null)
                return;
            Iterator<?> iter = items.iterator();
            for (int i = 0; iter.hasNext(); i++) {
                Object item = iter.next();
                if (i > 0)
                    result.append(seperator);
                if (item == null) item = "(null)";
                result.append(item.toString());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Join an array of strings (or toString() result of objects) using a
     * separator.
     *
     * @param seperator Separator
     * @param items     Array of items.
     * @return joined string.
     */
    public static String join(String seperator, Object... items) {
        if (items == null) return null;
        return join(seperator, Arrays.asList(items));
    }

    /**
     * Join an array of strings (or toString() result of objects) using a
     * separator.
     *
     * @param seperator Separator
     * @param items     Array of items.
     * @return joined string.
     */
    public static String join(String seperator, String... items) {
        if (items == null) return null;
        return join(seperator, Arrays.asList(items));
    }

    /**
     * Split the specified line into substring based on the specified separator
     *
     * @param line The line to split
     * @param sep  The separator to use
     * @return ArrayList of the substrings
     */
    public static ArrayList<String> split(String line, char sep) {
        return split(line, 0, sep);
    }

    /**
     * Split the specified line into substring based on the specified separator
     *
     * @param line     The line to split
     * @param startPos The starting position to split from
     * @param sep      The separator to use
     * @return ArrayList of the substrings
     */
    public static ArrayList<String> split(String line, int startPos, char sep) {
        final ArrayList<String> list = new ArrayList<String>();
        int lastPos = startPos;
        assert line != null;
        while (true) {
            final int pos = line.indexOf(sep, lastPos);
            if (pos > -1) {
                list.add(line.substring(lastPos, pos));
                lastPos = pos + 1;
            } else {
                list.add(line.substring(lastPos));
                return list;
            }
        }
    }

    /**
     * Split the specified line into substring based on the specified separator
     *
     * @param line     The line to split
     * @param startPos The starting position to split from
     * @param endPos   The end position to include
     * @param sep      The separator to use
     * @return ArrayList of the substrings
     */
    public static ArrayList<String> split(String line, int startPos, int endPos, char sep) {
        final ArrayList<String> list = new ArrayList<String>();
        int lastPos = startPos;
        assert line != null;
        while (lastPos < endPos) {
            final int pos = line.indexOf(sep, lastPos);
            if (pos > -1) {
                list.add(line.substring(lastPos, Math.min(pos, endPos)));
                lastPos = pos + 1;
            } else {
                list.add(line.substring(lastPos, endPos));
                break;
            }
        }
        return list;
    }

    /**
     * Split the specified line into substring assuming tab char as separator
     *
     * @param line The line to split
     * @return ArrayList of the substrings
     */
    public static ArrayList<String> split(String line) {
        return split(line, '\t');
    }

    /**
     * Split the specified line into substring assuming space is separating char, but keep quoted strings as one token.
     * i.e. "a 'a is a' b" is splitted into 3 elments, [a], [a is a], [b]
     *
     * @param line The line to split
     * @return ArrayList of the substrings
     */
    public static String[] splitReserveQuotesToArray(String line) {
        final ArrayList<String> list = splitReserveQuotes(line, 0);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Split the specified line into substring assuming space is separating char, but keep quoted strings as one token.
     * i.e. "a 'a is a' b" is splitted into 3 elments, [a], [a is a], [b]
     *
     * @param line     The line to split
     * @param startPos The starting position
     * @return ArrayList of the substrings
     */
    private static ArrayList<String> splitReserveQuotes(String line, int startPos) {
        final ArrayList<String> list = new ArrayList<String>();
        assert line != null;
        boolean inQuote = false, inDoubleQuote = false;
        final StringBuilder sb = new StringBuilder();
        for (int i = startPos; i < line.length(); i++) {
            if (inQuote && line.charAt(i) == '\'' || inDoubleQuote && line.charAt(i) == '\"') {
                inQuote = inDoubleQuote = false;
            } else {
                if (line.charAt(i) == '\'' && !inDoubleQuote) {
                    inQuote = true;
                } else if (line.charAt(i) == '\"' && !inQuote) {
                    inDoubleQuote = true;
                } else if (line.charAt(i) == ' ' && !inQuote && !inDoubleQuote) {
                    if (sb.length() > 0) {
                        list.add(sb.toString());
                        sb.setLength(0);
                    }
                    continue;
                }
            }
            sb.append(line.charAt(i));
        }
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list;
    }

    /**
     * Split the specified line into substring assuming tab char as separator
     *
     * @param line The line to split
     * @return Array of the substrings
     */
    public static String[] splitToArray(String line) {
        final ArrayList<String> list = split(line);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Split the specified line into substring assuming tab char as separator
     *
     * @param line The line to split
     * @param sep  The separator to use
     * @return Array of the substrings
     */
    public static String[] splitToArray(String line, char sep) {
        final ArrayList<String> list = split(line, sep);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Split the specified line into substring assuming tab char as separator
     *
     * @param line     The line to split
     * @param startPos The starting position in the line
     * @param sep      The separator to use
     * @return Array of the substrings
     */
    public static String[] splitToArray(String line, int startPos, char sep) {
        final ArrayList<String> list = split(line, startPos, sep);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Split the specified line into substring assuming space is separating char, but keep quoted strings and string in parenthesis as one token.
     * i.e. "a 'a is a' b" is splitted into 3 elments, [a], [a is a], [b]
     *
     * @param line The line to split
     * @return ArrayList of the substrings
     */
    public static String[] splitReserveQuotesAndParenthesesToArray(String line) {
        final List<String> list = splitReserveQuotesAndParentheses(line, 0);
        return list.toArray(new String[0]);
    }

    /**
     * Split the specified line into substring assuming space is separating char, but keep quoted strings as one token.
     * i.e. "a 'a is a' b" is splitted into 3 elments, [a], [a is a], [b]
     *
     * @param line     The line to split
     * @param startPos The starting position
     * @return ArrayList of the substrings
     */
    public static List<String> splitReserveQuotesAndParentheses(String line, int startPos) {
        final ArrayList<String> list = new ArrayList<>();
        boolean inQuote = false;
        boolean inDoubleQuote = false;
        int parCount = 0;
        final StringBuilder sb = new StringBuilder();
        for (int i = startPos; i < line.length(); i++) {
            if (inQuote && line.charAt(i) == '\'' || inDoubleQuote && line.charAt(i) == '\"') {
                inQuote = inDoubleQuote = false;
            } else if (line.charAt(i) == '(') parCount++;
            else if (line.charAt(i) == ')') parCount--;
            else {
                if (line.charAt(i) == '\'' && !inDoubleQuote) {
                    inQuote = true;
                } else if (line.charAt(i) == '\"' && !inQuote) {
                    inDoubleQuote = true;
                } else if (line.charAt(i) == ' ') {
                    if (!inQuote && !inDoubleQuote && parCount == 0) {
                        if (sb.length() > 0) {
                            list.add(sb.toString());
                            sb.setLength(0);
                        }
                        continue;
                    }
                }
            }
            sb.append(line.charAt(i));
        }
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list;
    }

    /**
     * Join an array of strings (or toString() result of objects) using a
     * seperator.
     *
     * @param seperator Separator
     * @param items     Array of items.
     * @param result    Buffer to add joined string to.
     */
    public static void join(String seperator, Object[] items, Appendable result) {
        if (items == null) return;
        join(seperator, Arrays.asList(items), result);
    }

    /**
     * @param o Object
     * @return true if toString for the object is empty
     */
    public static boolean isEmpty(Object o) {
        if (o == null || o.toString() == null) {
            return true;
        }
        return o.toString().trim().length() == 0;
    }

    /**
     * @param map map of objects
     * @return comma separated string of keys and values
     */
    public static String toString(Map<Object, Object> map) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (i > 0) {
                result.append(",");
            }
            result.append(entry.getKey() + "=" + entry.getValue());
            i++;
        }

        return result.toString();
    }

    /**
     * @param set collection of objects
     * @return comma separated string of values
     */
    public static <T extends Object> String toString(Collection<T> set) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (Object o : set.toArray()) {
            if (i > 0) result.append(",");
            result.append(o);
            i++;
        }
        return result.toString();
    }
}
