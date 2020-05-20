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

package org.gorpipe.model.util;

import java.util.ArrayList;
import java.util.List;

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
    private static List<String> splitReserveQuotesAndParentheses(String line, int startPos) {
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
     * @param o Object
     * @return true if toString for the object is empty
     */
    public static boolean isEmpty(Object o) {
        if (o == null || o.toString() == null) {
            return true;
        }
        return o.toString().trim().length() == 0;
    }
}
